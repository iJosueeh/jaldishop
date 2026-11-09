// Validación estática exhaustiva para Postman y Bruno; no envía HTTP ni ejecuta scripts de transporte.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '../..');
const postmanDir = path.join(root, 'postman');
const brunoDir = path.join(root, 'bruno/jaldishop');

function flat(items) {
  return items.flatMap(item => item.item ? flat(item.item) : [item]);
}

function files(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e =>
    e.isDirectory() ? files(path.join(dir, e.name)) : [path.join(dir, e.name)]
  );
}

console.log('--- 1. Validando Colección Principal Postman ---');
const mainColFile = path.join(postmanDir, 'jaldishop.postman_collection.json');
const mainCollection = JSON.parse(fs.readFileSync(mainColFile, 'utf8'));

// Validar las 8 carpetas requeridas
const expectedFolders = [
  '01. Autenticación',
  '02. Customer',
  '03. Tiendas y catálogo',
  '04. Carrito y checkout',
  '05. Pedidos y pagos',
  '06. Merchant',
  '07. Notificaciones',
  '08. Regresiones'
];
const actualFolders = mainCollection.item.map(i => i.name);
assert.deepEqual(actualFolders, expectedFolders, 'Las 8 carpetas principales no coinciden exactamente con la especificación');

const items = flat(mainCollection.item);
assert.equal(items.length, 111, `Se esperaban 111 peticiones en la colección principal, encontradas: ${items.length}`);

// Variables de colección y entornos
const variables = new Set(mainCollection.variable.map(v => v.key));
for (const envName of ['jaldishop.local.env.json', 'jaldishop.postman_environment.json']) {
  const env = JSON.parse(fs.readFileSync(path.join(postmanDir, envName), 'utf8'));
  const envKeys = new Set(env.values.map(v => v.key));
  for (const v of variables) {
    assert(envKeys.has(v), `Variable faltante en ${envName}: ${v}`);
  }
}

// Mapeos en controladores Spring Boot
const routes = [];
for (const file of files(path.join(root, 'backend/src/main/java'))) {
  if (!file.endsWith('.java')) continue;
  const source = fs.readFileSync(file, 'utf8');
  if (!source.includes('@RestController')) continue;
  const prefix = source.split('class ')[0].match(/@RequestMapping\(([^)]*)\)/)?.[1].match(/"([^"\n]*)"/)?.[1] || '';
  for (const m of source.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^)]*)\))?/g)) {
    const suffix = m[2]?.match(/"([^"\n]*)"/)?.[1] || '';
    const route = prefix + suffix;
    routes.push({
      method: m[1].toUpperCase(),
      route,
      pattern: new RegExp('^' + route.replace(/[.*+?^$()|[\]\\]/g, '\\$&').replace(/\{[^}]+\}/g, '[^/]+') + '$')
    });
  }
}

const coveredRoutes = new Set();
let postmanScripts = 0;

for (const item of items) {
  const rawUrl = typeof item.request.url === 'string' ? item.request.url : item.request.url.raw;
  assert(rawUrl.startsWith('{{baseUrl}}/api/v1/'), `URL inválida en ${item.name}: ${rawUrl}`);
  const route = rawUrl.replace('{{baseUrl}}', '').split('?')[0];
  const matches = routes.filter(r => r.method === item.request.method && r.pattern.test(route));

  if (item.name.includes('(R010)')) {
    assert(item.request.method === 'PATCH' && route === '/api/v1/customer/addresses');
  } else if (item.name.includes('(R011)')) {
    assert(route.endsWith('/missing'));
  } else {
    assert(matches.length > 0, `Petición sin contrato backend: ${item.name}`);
  }
  matches.forEach(r => coveredRoutes.add(r.method + ' ' + r.route));

  if (item.request.body?.raw) {
    JSON.parse(item.request.body.raw);
  }

  // Verificar sintaxis y seguridad de scripts
  for (const ev of item.event || []) {
    const code = ev.script.exec.join('\n');
    new vm.Script(code);
    postmanScripts++;
    assert(!/fetch\s*\(|\brequire\s*\(/.test(code), `Script con función no permitida en ${item.name}`);
  }

  // Prueba de guards en VM
  const preScript = item.event[0].script.exec.join('\n');
  function testGuard(targetUrl, envVars = {}) {
    let skipped = false, rejected = false;
    const pm = {
      request: { url: { toString: () => targetUrl } },
      variables: { replaceIn: str => str.replace('{{baseUrl}}', targetUrl) },
      environment: {
        get: k => envVars[k] !== undefined ? envVars[k] : '',
        set: (k, v) => { envVars[k] = v; }
      },
      execution: { skipRequest: () => { skipped = true; } }
    };
    try {
      vm.runInNewContext(preScript, { pm }, { timeout: 1000 });
    } catch (_) {
      rejected = true;
    }
    return { skipped, rejected };
  }

  // Debe rechazar URLs externas
  assert.equal(testGuard('https://external-api.evil.com/api/v1/health').rejected, true, `Debe rechazar host externo en ${item.name}`);
  
  // Debe permitir localhost sin exigir runCase
  if (preScript.includes('financialFakeReady')) {
    assert.equal(testGuard('http://127.0.0.1:8080/api/v1/checkout/mercado-pago/preference', { financialFakeReady: 'false' }).rejected, true);
    assert.deepEqual(testGuard('http://127.0.0.1:8080/api/v1/checkout/mercado-pago/preference', { financialFakeReady: 'true' }), { skipped: false, rejected: false });
  } else if (preScript.includes('allowDestructiveOperations')) {
    assert.equal(testGuard('http://127.0.0.1:8080/api/v1/stores/me', { allowDestructiveOperations: 'false' }).rejected, true);
    assert.deepEqual(testGuard('http://127.0.0.1:8080/api/v1/stores/me', { allowDestructiveOperations: 'true' }), { skipped: false, rejected: false });
  } else {
    assert.deepEqual(testGuard('http://127.0.0.1:8080/api/v1/example'), { skipped: false, rejected: false }, `Localhost bloqueado incorrectamente en ${item.name}`);
  }
}

assert.equal(coveredRoutes.size, routes.length, `Rutas backend cubiertas: ${coveredRoutes.size} de ${routes.length}`);
console.log(`✓ Postman principal válido: 111 requests, ${routes.length} rutas backend cubiertas al 100%.`);

console.log('--- 2. Validando Colección Secundaria (Regresiones y Negativos) ---');
const regColFile = path.join(postmanDir, 'jaldishop.regressions.postman_collection.json');
const regCollection = JSON.parse(fs.readFileSync(regColFile, 'utf8'));
const regItems = flat(regCollection.item);
assert(regItems.length >= 494, `Se esperaban al menos 494 peticiones en colección secundaria, encontradas: ${regItems.length}`);
console.log(`✓ Colección secundaria válida: ${regItems.length} escenarios negativos y de regresión separados.`);

console.log('--- 3. Validando Colecciones Bruno (.bru) ---');
const bruFiles = files(brunoDir).filter(f => f.endsWith('.bru'));
assert.equal(bruFiles.length, 112, `Se esperaban 111 peticiones + 1 environment en Bruno, encontradas: ${bruFiles.length}`);

let brunoScriptsCount = 0;
for (const bfile of bruFiles) {
  const content = fs.readFileSync(bfile, 'utf8');
  
  // VERIFICACIÓN CRÍTICA: CERO REFERENCIAS A PM
  assert(!/\bpm\./.test(content), `ERROR CRÍTICO: Referencia a 'pm.' encontrada en Bruno archivo ${bfile}`);
  assert(!/\bpm\[/.test(content), `ERROR CRÍTICO: Referencia a 'pm[' encontrada en Bruno archivo ${bfile}`);
  assert(!/\bpm\b/.test(content.replace(/\b(rpm|npm)\b/g, '')), `ERROR CRÍTICO: Referencia a 'pm' encontrada en Bruno archivo ${bfile}`);

  if (content.includes('script:pre-request')) {
    const match = content.match(/script:pre-request\s*\{([\s\S]*?)\n\}/);
    if (match) {
      new vm.Script(match[1]);
      brunoScriptsCount++;
    }
  }
  if (content.includes('script:post-response')) {
    const match = content.match(/script:post-response\s*\{([\s\S]*?)\n\}/);
    if (match) {
      new vm.Script(match[1]);
      brunoScriptsCount++;
    }
  }
  if (content.includes('tests {')) {
    const match = content.match(/tests\s*\{([\s\S]*?)\n\}/);
    if (match) {
      new vm.Script(match[1]);
      brunoScriptsCount++;
    }
  }
}
console.log(`✓ Colección Bruno validada: ${bruFiles.length} archivos .bru, ${brunoScriptsCount} scripts probados, CERO referencias a pm.`);

console.log('--- 4. Validando Entornos Bruno y Postman ---');
// Postman environment
const pmEnv = JSON.parse(fs.readFileSync(path.join(root, 'postman/jaldishop.local.env.json'), 'utf8'));
assert(Array.isArray(pmEnv.values), 'Postman environment must have values array');
assert.equal(pmEnv.values.length, 71, 'Postman environment must have 71 variables');

// Bruno JSON importable
const brunoJsonPath = path.join(root, 'bruno/jaldishop.local.environment.json');
assert(fs.existsSync(brunoJsonPath), 'Bruno JSON environment must exist');
const brunoJson = JSON.parse(fs.readFileSync(brunoJsonPath, 'utf8'));
assert(Array.isArray(brunoJson.variables), 'Invalid environment: missing or invalid variables array');
assert.equal(brunoJson.variables.length, 71, 'Bruno environment must have 71 variables');
for (const [idx, v] of brunoJson.variables.entries()) {
  assert(v && typeof v === 'object', `Invalid variable at index ${idx}: expected an object`);
  assert(typeof v.name === 'string' && v.name.length > 0, `Invalid variable at index ${idx}: missing or invalid name`);
}

// Bruno OpenCollection YAML
const brunoYamlPath = path.join(root, 'bruno/jaldishop/environments/JaldiShop Local.yml');
assert(fs.existsSync(brunoYamlPath), 'Bruno YAML environment must exist');
const brunoYamlContent = fs.readFileSync(brunoYamlPath, 'utf8');
assert(brunoYamlContent.includes('variables:'), 'Bruno YAML must contain variables: section');
assert(brunoYamlContent.includes('value: http://127.0.0.1:8080'), 'Bruno YAML must have correct baseUrl');
console.log('✓ Entornos Bruno y Postman validados: 71 variables cada uno, contratos respetados sin conflicto.');

console.log(JSON.stringify({
  status: 'ALL_CHECKS_PASS',
  mainCollectionRequests: items.length,
  secondaryCollectionRequests: regItems.length,
  backendRoutesCovered: coveredRoutes.size,
  brunoFiles: bruFiles.length,
  pmReferencesInBruno: 0,
  navigationFolders: actualFolders.length,
  httpExecuted: false
}, null, 2));
