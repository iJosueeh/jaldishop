"""Construye casos desde un respaldo explícito; no ejecuta HTTP ni lee environments."""
import collections,copy,json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
DOC=ROOT/'docs/preparacion-colecciones-2026-11-07'
source=Path(sys.argv[1])
c=json.loads(source.read_text())
def walk(items):
 for x in items:
  if 'item' in x: yield from walk(x['item'])
  elif 'request' in x: yield x

def signature(x):
 r=x['request'];u=r['url'];raw=u if isinstance(u,str) else u['raw']
 return r['method']+' '+re.sub(r'\{\{(\w+)\}\}',r'{\1}',raw.split('?',1)[0].replace('{{baseUrl}}',''))
def norm(s):return re.sub(r'\{[^}]+\}','{}',s)
endpoints={}
for line in (ROOT/'docs/auditoria-2026-11-07/01-inventario.md').read_text().splitlines():
 m=re.match(r'\| (\w+) \| `([^`]+)` \| \[([^]]+)\]\(([^)#]+)#L\d+\) \| (\d+) \|',line)
 if m:
  module,sig,method,file,status=m.groups();endpoints[norm(sig)]=(module,sig,method,ROOT/file.replace('../../',''),int(status))
owners={}
for line in (ROOT/'docs/verificacion-integracion-2026-11-07/01-matriz-equipo.md').read_text().splitlines():
 m=re.match(r'\| (Mia|Katherine|Josue) \| `([^`]+)`',line)
 if m:owners[norm(m[2])]=m[1]
assert len(endpoints)==len(owners)==97
original=list(walk(c['item']));assert len(original)==100
# Guard is request-level: it also protects execution of a single request.
guard=r'''const raw = pm.variables.replaceIn(pm.request.url.toString());
const allowed = /^http:\/\/(localhost|127\.0\.0\.1|\[::1\])(?::[0-9]{1,5})?\/api\/v1\//.test(raw);
const needed = (JSON.stringify(pm.request.url) + JSON.stringify(pm.request.body || {}) + JSON.stringify(pm.request.auth || {})).match(/\{\{(\w+)\}\}/g) || [];
const missing = needed.some(v => !pm.variables.get(v.slice(2, -2)));
const selected = pm.environment.get("runCase") === "CASE_ID";
const fakeReady = !FINANCIAL || pm.environment.get("financialFakeReady") === "true";
const fixturesReady = pm.environment.get("disposableFixturesReady") === "true";
if (missing || !allowed || !selected || !fakeReady || !fixturesReady) {
    if (pm.execution && pm.execution.skipRequest) pm.execution.skipRequest();
    throw new Error("BLOQUEADO: localhost, fixtures desechables, runCase exacto y fake financiero cuando corresponda son obligatorios");
}'''
vars={x['key'] for x in c.get('variable',[])}
for x in c.get('variable',[]):x['value']='http://127.0.0.1:8080' if x['key']=='baseUrl' else ''
folders={who:{} for who in ['Katherine','Mia','Josue']};case_rows=[];counter=collections.Counter()
def add(item,ep,label,status,code=None,changes=None):
 module,sig,method,path,success=ep;owner=owners[norm(sig)];counter[owner]+=1
 cid=owner[0].upper()+f'{counter[owner]:03d}'
 x=copy.deepcopy(item);r=x['request'];financial=module=='payment';write=r['method']!='GET' or '/capacity-reservations' in sig
 x['name']=f'{cid} | {label} | {item["name"]}'
 if changes:changes(r)
 tests=['pm.test("HTTP esperado", function () { pm.response.to.have.status('+str(status)+'); });']
 if status>=400:
  tests+=['pm.test("Error controlado", function () { const e=pm.response.json(); pm.expect(e.code).to.be.a("string"); pm.expect(e.message).to.be.a("string");'+(' pm.expect(e.code).to.eql("'+code+'");' if code else '')+' });']
  if code=='VALIDATION_ERROR':tests+=['pm.test("Errores de campos", function () { pm.expect(pm.response.json().errors).to.be.an("object"); });']
 # Keep old scripts only for successful originals: negative clones must not overwrite fixture IDs/tokens.
 old=copy.deepcopy(x.get('event',[])) if label=='VALIDO' else []
 x['event']=[{'listen':'prerequest','script':{'type':'text/javascript','exec':guard.replace('CASE_ID',cid).replace('FINANCIAL','true' if financial else 'false').splitlines()}}]+old+[{'listen':'test','script':{'type':'text/javascript','exec':tests}}]
 x['protocolProfileBehavior']={'followRedirects':False}
 x.pop('response',None) # saved examples may contain persistent tokens or personal data
 shared=module in {'capacity','inventory','checkout'}
 x['request']['description']=(str(x['request'].get('description',''))+'\n\n'+f'Caso {cid}; endpoint {sig}; responsable {owner}; estado NO EJECUTADO. '+('MODIFICA DATOS. ' if write else '')+('FINANCIERO: MANUAL, solo gateway fake verificado. ' if financial else '')+('COMPARTIDO: Katherine prepara stock/cupos; Mia carrito/dirección; Josue comprueba reservas/rollback. ' if shared else '')+'Elegir este ID en runCase; no usar Run Collection indiscriminado. Restaurar/recrear fixture entre casos. El HTTP esperado presupone los demás datos válidos.').strip()
 folders[owner].setdefault(module,[]).append(x)
 case_rows.append((cid,owner,module,sig,label,status,'MANUAL FINANCIERO/FAKE' if financial else 'ESCRITURA MANUAL' if write else 'LECTURA',shared))
 return x

def setbody(r,body):r['body']={'mode':'raw','raw':json.dumps(body,ensure_ascii=False,indent=2),'options':{'raw':{'language':'json'}}}
def public(sig):
 method,path=sig.split(' ',1)
 return path in ['/api/v1/health','/api/v1/auth/login','/api/v1/auth/register','/api/v1/auth/register/merchant','/api/v1/media/upload-signature','/api/v1/checkout/mercado-pago/webhook','/api/v1/store-categories','/api/v1/stores/search','/api/v1/stores/featured'] or path.startswith('/api/v1/stores/slug/') or (method=='GET' and re.fullmatch(r'/api/v1/(stores/[^/]+/(products|categories)|products/[^/]+/reviews)',path))
for item in original:
 ep=endpoints[norm(signature(item))];module,sig,method,path,status=ep
 r=item['request'];body=None
 try:body=json.loads(r.get('body',{}).get('raw',''))
 except (ValueError,TypeError):pass
 role='admin' if '/admin/' in sig else 'merchant' if owners[norm(sig)]=='Katherine' else 'customer'
 if module=='media':role='merchant'
 if sig.endswith('/auth/login'):
  role='admin' if 'Admin' in item['name'] else 'merchant' if 'Merchant' in item['name'] else 'customer'
  body={'email':'{{'+role+'Email}}','password':'{{'+role+'Password}}'}
 if body is not None:
  for key in list(body):
   if key=='password':body[key]='{{fixturePassword}}'
   elif key=='email' and not sig.endswith('/auth/login'):body[key]='{{new'+owners[norm(sig)]+'Email}}'
   elif key in {'storeId','productId','variantId','categoryId','userId','reservationId','checkoutReferenceId','attemptId','addressId'}:body[key]='{{'+key+'}}'
  if sig.endswith('/auth/login'):body['password']='{{'+role+'Password}}'
  setbody(r,body)
 r['auth']={'type':'noauth'} if public(sig) and module!='media' else {'type':'bearer','bearer':[{'key':'token','value':'{{'+role+'Token}}','type':'string'}]}
 if sig.endswith('/auth/login'):
  item.setdefault('event',[]).append({'listen':'test','script':{'type':'text/javascript','exec':['if (pm.response.code === 200) pm.environment.set("'+role+'Token", pm.response.json().token);']}})
 add(item,ep,'VALIDO',status)
 if not public(sig):
  add(item,ep,'SIN_AUTH',401,changes=lambda q:q.update(auth={'type':'noauth'}))
 # Only roles demonstrated by controller annotations/global admin matcher.
 text=path.read_text();roles=set(re.findall(r"hasRole\('([A-Z]+)'\)",text))
 if '/admin/' in sig:roles={'ADMIN'}
 if len(roles)==1 and not public(sig):
  wrong='customer' if next(iter(roles))!='CUSTOMER' else 'merchantOnly'
  add(item,ep,'ROL_INCORRECTO',403,changes=lambda q,w=wrong:q.update(auth={'type':'bearer','bearer':[{'key':'token','value':'{{'+w+'Token}}','type':'string'}]}))
 # Request DTO constraints only: inspect actual @Valid request type on the mapped method.
 match=re.search(r'\b'+method+r'\s*\((.*?)\)\s*\{',text,re.S)
 dto=re.search(r'@Valid\s+@RequestBody\s+(\w+)',match[1]) if match else None
 if match:
  uuid_params=re.findall(r'@PathVariable(?:\([^)]*\))?\s+UUID\s+(\w+)',match[1])
  canonical=sig.split(' ',1)[1].split('/')
  raw=r['url'] if isinstance(r['url'],str) else r['url']['raw']
  request_path=raw.split('?',1)[0].replace('{{baseUrl}}','').split('/')
  for param in uuid_params:
   if '{'+param+'}' not in canonical:continue
   idx=canonical.index('{'+param+'}')
   token=request_path[idx]
   def bad_uuid(q,token=token):
    q['url']=json.loads(json.dumps(q['url']).replace(token,'invalid-uuid'))
   add(item,ep,'UUID_INVALIDO_'+param,400,'INVALID_PARAMETER',bad_uuid)
 if module=='address' and r['method'] in {'PUT','DELETE'}:
  for label,value in [('RECURSO_AJENO','{{otherAddressId}}'),('INEXISTENTE','{{missingAddressId}}')]:
   add(item,ep,label,404,changes=lambda q,v=value:q.update(url=json.loads(json.dumps(q['url']).replace('{{addressId}}',v))))
 if body is not None and dto:
  files=list((ROOT/'backend/src/main/java').rglob(dto[1]+'.java'))
  if files:
   dto_text=files[0].read_text();record=re.search(r'public record \w+\((.*?)\)\s*(?:implements[^\{]+)?\{',dto_text,re.S)
   if record:
    for constraints,typ,field in re.findall(r'((?:\s*@\w+(?:\([^\n]*\))?\s*)+)\s*(String|UUID|BigDecimal|Integer|LocalDate|LocalTime)\s+(\w+)\s*[,\)]?',record[1]):
     if field not in body:continue
     if '@NotBlank' in constraints or '@NotNull' in constraints:
      for label,val,remove in [('AUSENTE',None,True),('NULO',None,False)]:
       b=copy.deepcopy(body)
       if remove:b.pop(field)
       else:b[field]=val
       add(item,ep,label+'_'+field,400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
     if '@NotBlank' in constraints and typ=='String':
      b=copy.deepcopy(body);b[field]='';add(item,ep,'VACIO_'+field,400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
     if '@Email' in constraints or '@Pattern' in constraints:
      b=copy.deepcopy(body);b[field]='!invalid!';add(item,ep,'FORMATO_'+field,400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
     for annotation,delta in [('DecimalMin',-1),('DecimalMax',1),('Min',-1),('Max',1)]:
      bound=re.search('@'+annotation+r'\(\s*(?:value\s*=\s*)?["\']?(-?[0-9]+(?:\.[0-9]+)?)',constraints)
      if bound:
       b=copy.deepcopy(body);b[field]=float(bound[1])+delta
       add(item,ep,'FUERA_'+annotation+'_'+field,400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
     size=re.search(r'@Size\([^)]*max\s*=\s*(\d+)',constraints)
     if size and typ=='String' and '@Email' not in constraints:
      b=copy.deepcopy(body);b[field]='x'*(int(size[1])+1)
      add(item,ep,'EXCEDE_'+field,400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
 if module=='catalog' and '/variants' in sig and r['method'] in {'POST','PUT'}:
  cases=[('ATTR_ELEMENTO_NULO',[None],400,'VALIDATION_ERROR'),('ATTR_NOMBRE_NULO',[{'name':None,'value':'v'}],400,'VALIDATION_ERROR'),('ATTR_VALOR_VACIO',[{'name':'Color','value':''}],400,'VALIDATION_ERROR'),('ATTR_NOMBRE_121',[{'name':'n'*121,'value':'v'}],400,'VALIDATION_ERROR'),('ATTR_DUPLICADO_TRIM',[{'name':'Color','value':'a'},{'name':' Color ','value':'b'}],400,'INVALID_ARGUMENT'),('ATTR_NOMBRE_120',[{'name':'n'*120,'value':'v'}],status,None),('ATTR_LISTA_VACIA',[],status,None),('ATTR_LISTA_NULA',None,status,None)]
  for label,attrs,http,code in cases:
   b=copy.deepcopy(body);b['attributes']=attrs;add(item,ep,label,http,code,lambda q,b=b:setbody(q,b))
 if module=='catalog' and '/variants' in sig and r['method']=='POST':
  add(item,ep,'REPETIR_SKU_EXISTENTE',409,changes=lambda q:setbody(q,body))
 if sig=='GET /api/v1/admin/users':
  add(item,ep,'ADMIN_REVOCADO_TOKEN_ANTERIOR',403,changes=lambda q:q.update(auth={'type':'bearer','bearer':[{'key':'token','value':'{{revokedAdminToken}}','type':'string'}]}))
 if sig.endswith('/auth/register/merchant'):
  b=copy.deepcopy(body);b['password']='12345';add(item,ep,'PASSWORD_CORTO',400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
  b=copy.deepcopy(body);b['email']='not-an-email';add(item,ep,'EMAIL_FORMATO',400,'VALIDATION_ERROR',lambda q,b=b:setbody(q,b))
  b=copy.deepcopy(body);b['email']='{{existingMerchantEmail}}';add(item,ep,'MERCHANT_DUPLICADO',409,changes=lambda q,b=b:setbody(q,b))
  for field,limit in [('firstName',100),('lastName',100),('email',254)]:
   for extra in [0,1]:
    b=copy.deepcopy(body);b[field]=('a'*64+'@'+'b'*63+'.'+'c'*63+'.'+'d'*(61+extra)) if field=='email' else 'n'*(limit+extra)
    add(item,ep,f'{field}_{limit+extra}',400 if extra else status,'VALIDATION_ERROR' if extra else None,lambda q,b=b:setbody(q,b))
 if module=='media':
  for label,b,http,auth in [('TIENDA_AJENA',{'targetType':'PRODUCT_IMAGE','storeId':'{{otherStoreId}}'},404,'merchant'),('PRODUCTO_SIN_AUTH',{'targetType':'PRODUCT_IMAGE','storeId':'{{storeId}}'},403,None),('CUSTOMER_PRODUCTO',{'targetType':'PRODUCT_IMAGE','storeId':'{{storeId}}'},403,'customer'),('BRANDING_TEMPORAL',{'targetType':'STORE_LOGO'},200,None),('BRANDING_AJENO_ANONIMO',{'targetType':'STORE_LOGO','storeId':'{{otherStoreId}}'},404,None)]:
   def change(q,b=b,a=auth):
    setbody(q,b);q['auth']={'type':'noauth'} if a is None else {'type':'bearer','bearer':[{'key':'token','value':'{{'+a+'Token}}','type':'string'}]}
   add(item,ep,label,http,changes=change)
c['item']=[{'name':who+' — '+str(sum(1 for k,v in owners.items() if v==who))+' endpoints','item':[{'name':module,'item':items} for module,items in sorted(groups.items())]} for who,groups in folders.items()]
c['auth']={'type':'noauth'}
raw=json.dumps(c);allvars=set(re.findall(r'\{\{([\w]+)\}\}',raw))|vars|{'merchantOnlyToken','otherStoreId','otherUserId','cartId','runCase','disposableFixturesReady','financialFakeReady','fixturePassword','newKatherineEmail','newMiaEmail','newJosueEmail'}
c['variable']=[{'key':key,'value':'http://127.0.0.1:8080' if key=='baseUrl' else '', 'type':'string'} for key in sorted(allvars)]
c['info']['description']='Colección canónica de preparación del equipo. 97 endpoints auditados; escenarios con fixtures desechables y selección manual por runCase. NO EJECUTADA. Ver docs/preparacion-colecciones-2026-11-07. Bruno externo pendiente de elección canónica.'
(ROOT/'postman/jaldishop.postman_collection.json').write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n')
DOC.mkdir(exist_ok=True)
(DOC/'casos.json').write_text(json.dumps([dict(zip(['id','responsable','modulo','endpoint','caso','http','riesgo','compartido'],row)) for row in case_rows],ensure_ascii=False,indent=2)+'\n')
print('Casos',len(case_rows),'por integrante',dict(counter))
