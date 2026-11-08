// Validación estática y guards en sandbox JS; no transportes HTTP.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const c = JSON.parse(fs.readFileSync(require('node:path').join(__dirname, '../jaldishop.postman_collection.json')));
function flat(items) { return items.flatMap(x => x.item ? flat(x.item) : [x]); }
const items = flat(c.item);
const cases = new Set();
let scripts = 0;
for (const item of items) {
 const id = item.name.split(' | ')[0];
 assert(!cases.has(id)); cases.add(id);
 assert.equal(item.protocolProfileBehavior.followRedirects, false);
 for (const event of item.event || []) { new vm.Script(event.script.exec.join('\n')); scripts++; }
 const guard = item.event[0].script.exec.join('\n');
 function check(url, selected, ready, financial, missing=false) {
  let skipped = false, rejected = false;
  const env = {runCase:selected, disposableFixturesReady:ready, financialFakeReady:financial};
  const pm = { request:{url:{toString:()=>url},body:{raw:'{{fixtureId}}'},auth:{}},
   variables:{replaceIn:x=>x,get:()=>missing ? '' : 'fixture'},environment:{get:k=>env[k]},
   execution:{skipRequest:()=>{skipped=true;}} };
  try {vm.runInNewContext(guard,{pm});} catch (_) {rejected=true;}
  return {skipped,rejected};
 }
 assert.deepEqual(check('https://example.invalid/api/v1/health',id,'true','true'),{skipped:true,rejected:true});
 assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health','', 'true','true'),{skipped:true,rejected:true});
 assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'','true'),{skipped:true,rejected:true});
 assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true','true',true),{skipped:true,rejected:true});
 assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true','true'),{skipped:false,rejected:false});
 if (guard.includes('!true ||')) assert.deepEqual(check('http://127.0.0.1:8080/api/v1/health',id,'true',''),{skipped:true,rejected:true});
}
for (const v of c.variable) assert.equal(v.value, v.key==='baseUrl'?'http://127.0.0.1:8080':'');
assert.equal(items.filter(x=>x.name.includes(' | VALIDO | ')).length,100);
console.log(JSON.stringify({cases:items.length,scripts,guardChecks:'PASS',variableValues:'PASS',originalRequests:100}));
