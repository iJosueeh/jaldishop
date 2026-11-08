// Validación estática; no envía HTTP ni ejecuta scripts de transporte.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
const dir = path.join(root, 'postman');
const collection = JSON.parse(fs.readFileSync(path.join(dir, 'jaldishop.postman_collection.json'), 'utf8'));
function flat(items) { return items.flatMap(item => item.item ? flat(item.item) : [item]); }
function files(dir) { return fs.readdirSync(dir, {withFileTypes:true}).flatMap(e => e.isDirectory() ? files(path.join(dir,e.name)) : [path.join(dir,e.name)]); }
const items = flat(collection.item);
const variables = new Set(collection.variable.map(v => v.key));
assert.equal(variables.size, collection.variable.length);
for (const v of collection.variable) assert.equal(v.value, v.key === 'baseUrl' ? 'http://127.0.0.1:8080' : '');
for (const name of ['jaldishop.local.env.json','jaldishop.postman_environment.json']) {
  const env = JSON.parse(fs.readFileSync(path.join(dir,name),'utf8'));
  assert.deepEqual(env.values.map(v => v.key).sort(), [...variables].sort());
  for (const v of env.values) assert.equal(v.value, v.key === 'baseUrl' ? 'http://127.0.0.1:8080' : '');
}
const readme = fs.readFileSync(path.join(dir,'README.md'),'utf8');
for (const key of variables) assert(readme.includes('`'+key+'`'), 'Variable sin documentar: '+key);
// Mapeos presentes en los controladores actuales; esto no sustituye las pruebas HTTP.
const routes = [];
for (const file of files(path.join(root,'backend/src/main/java'))) {
  if (!file.endsWith('.java')) continue;
  const source = fs.readFileSync(file,'utf8');
  if (!source.includes('@RestController')) continue;
  const prefix = source.split('class ')[0].match(/@RequestMapping\(([^)]*)\)/)?.[1].match(/"([^"\n]*)"/)?.[1] || '';
  for (const m of source.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^)]*)\))?/g)) {
    const suffix = m[2]?.match(/"([^"\n]*)"/)?.[1] || '';
    const route = prefix + suffix;
    routes.push({method:m[1].toUpperCase(),route,pattern:new RegExp('^'+route.replace(/[.*+?^$()|[\]\\]/g,'\\$&').replace(/\{[^}]+\}/g,'[^/]+')+'$')});
  }
}
const ids = new Set(), covered = new Set();
let scripts = 0;
for (const item of items) {
  const id = item.name.split(' | ')[0];
  assert(!ids.has(id), 'ID repetido: '+id); ids.add(id);
  assert.equal(item.protocolProfileBehavior.followRedirects,false);
  assert.equal(item.response,undefined);
  const raw = typeof item.request.url === 'string' ? item.request.url : item.request.url.raw;
  assert(raw.startsWith('{{baseUrl}}/api/v1/'));
  const route = raw.replace('{{baseUrl}}','').split('?')[0];
  const matches = routes.filter(r => r.method === item.request.method && r.pattern.test(route));
  // Casos deliberados de error: método inexistente (405) y ruta inexistente (404).
  if (id === 'R010') assert(item.request.method === 'PATCH' && route === '/api/v1/customer/addresses');
  else if (id === 'R011') assert(route.endsWith('/missing'));
  else assert(matches.length > 0, 'Request sin contrato: '+id);
  matches.forEach(r => covered.add(r.method+' '+r.route));
  if (item.request.body?.raw) JSON.parse(item.request.body.raw);
  for (const match of JSON.stringify(item.request).matchAll(/\{\{(\w+)\}\}/g)) assert(variables.has(match[1]),'Variable faltante: '+match[1]);
  for (const event of item.event || []) {
    const code = event.script.exec.join('\n');
    new vm.Script(code); scripts++;
    assert(!/pm\.sendRequest|\bfetch\s*\(|\brequire\s*\(/.test(code));
    for (const m of code.matchAll(/pm\.environment\.(?:get|set)\(["'](\w+)["']/g)) assert(variables.has(m[1]),'Variable de script faltante: '+m[1]);
  }
  const guard = item.event[0].script.exec.join('\n');
  function check(url, selected, ready, financial, missing=false) {
    let skipped=false, rejected=false;
    const env={runCase:selected,disposableFixturesReady:ready,financialFakeReady:financial};
    const pm={request:{url:{toString:()=>url},body:{raw:'{{fixtureId}}'},headers:{signature:'{{headerFixture}}'},auth:{}},variables:{replaceIn:x=>x,get:key=>(missing===true || (missing==='header' && key==='headerFixture'))?'':'fixture'},environment:{get:k=>env[k]},execution:{skipRequest:()=>{skipped=true;}}};
    try { vm.runInNewContext(guard,{pm},{timeout:1000}); } catch (_) {rejected=true;}
    return {skipped,rejected};
  }
  for (const url of ['https://example.invalid/api/v1/health','http://localhost.evil.invalid/api/v1/health','http://127.0.0.1:8080@evil.invalid/api/v1/health']) assert.deepEqual(check(url,id,'true','true'),{skipped:true,rejected:true});
  assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health','', 'true','true'),{skipped:true,rejected:true});
  assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'','true'),{skipped:true,rejected:true});
  assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true','true',true),{skipped:true,rejected:true});
  assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true','true'),{skipped:false,rejected:false});
  assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true','true','header'),{skipped:true,rejected:true});
  if (guard.includes('!true ||')) assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true',''),{skipped:true,rejected:true});
}
assert.equal(covered.size,routes.length);
assert.equal(items.filter(i=>i.name.includes(' | VALIDO | ')).length,100);
assert.equal(items.filter(i=>i.name.includes(' | REGRESION | ')).length,11);
console.log(JSON.stringify({cases:items.length,scripts,routes:covered.size,variables:variables.size,regressions:11,guardChecks:'PASS',httpExecuted:false}));
