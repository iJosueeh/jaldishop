"""Sanea una exportación Postman y prepara ejemplos locales; nunca envía HTTP."""
import argparse
import copy
import json
import re
from pathlib import Path

POSTMAN = Path(__file__).resolve().parents[1]
BASE_URL = 'http://127.0.0.1:8080'
REGRESSIONS = 'Regresiones corregidas'
PERSONAL_FIELDS = {
    'firstName', 'lastName', 'recipientName', 'customerName', 'recipientPhone',
    'phone', 'customerPhone', 'streetAddress', 'address', 'deliveryAddress',
    'addressReference', 'deliveryReference', 'reference', 'imageUrl',
}
RENAMES = {'newKatherineEmail': 'newRegistrationEmail',
           'newMiaEmail': 'newRegistrationEmail', 'newJosueEmail': 'newRegistrationEmail'}


def walk(items):
    for item in items:
        if 'item' in item:
            yield from walk(item['item'])
        else:
            yield item


def fixture_key(field):
    return 'fixture' + field[0].upper() + field[1:]


def sanitize_body(value, label):
    if isinstance(value, list):
        return [sanitize_body(item, label) for item in value]
    if not isinstance(value, dict):
        return value
    result = {}
    for field, val in value.items():
        if isinstance(val, (dict, list)):
            val = sanitize_body(val, label)
        elif field.lower().endswith(('latitude', 'longitude')) and isinstance(val, (int, float)):
            limit = 90 if field.lower().endswith('latitude') else 180
            # Neutral synthetic coordinates; retain generated out-of-range validation cases.
            if abs(val) <= limit:
                val = 0.0
        elif isinstance(val, str) and val and '{{' not in val:
            if field == 'user_id':
                val = '{{providerUserId}}'
            elif field.lower() == 'password':
                val = '{{invalidShortPassword}}' if label == 'PASSWORD_CORTO' else '{{fixturePassword}}'
            elif field in {'email', 'customerEmail'}:
                boundary = re.fullmatch(r'email_(254|255)', label)
                if boundary:
                    size = int(boundary[1])
                    val = 'a' * 64 + '@' + 'b' * 63 + '.' + 'c' * 63 + '.' + 'd' * (size - 198) + '.test'
                    assert len(val) == size
                elif 'FORMATO' in label or 'EMAIL_FORMATO' == label:
                    val = 'invalid-email'
                else:
                    val = '{{fixtureEmail}}'
            elif field in PERSONAL_FIELDS:
                # Preserve generated boundary/format failures, which contain no personal data.
                if val != '!invalid!' and not (len(set(val)) == 1 and len(val) > 50):
                    val = '{{' + fixture_key(field) + '}}'
        result[field] = val
    return result


def add_regressions(collection):
    originals = {item['name'].split(' | ')[0]: item for item in walk(collection['item'])}
    cases = []

    def add(source, label, status, code=None, method=None, url=None, body=None, extra=()):
        item = copy.deepcopy(originals[source])
        case_id = f'R{len(cases) + 1:03d}'
        item['name'] = f'{case_id} | REGRESION | {label}'
        request = item['request']
        if method:
            request['method'] = method
        if url:
            request['url'] = url
        if body:
            data = json.loads(request['body']['raw'])
            data.update(body)
            request['body']['raw'] = json.dumps(data, ensure_ascii=False, indent=2)
        guard = copy.deepcopy(item['event'][0])
        guard['script']['exec'] = [line.replace(source, case_id) for line in guard['script']['exec']]
        tests = [f'pm.test("HTTP esperado", () => pm.response.to.have.status({status}));']
        if code:
            tests.append(f'pm.test("Código esperado", () => pm.expect(pm.response.json().code).to.eql("{code}"));')
        item['event'] = [guard, {'listen': 'test', 'script': {'type': 'text/javascript', 'exec': tests + list(extra)}}]
        request['description'] = f'{case_id}: {label}. Requiere los fixtures indicados en postman/README.md.'
        cases.append(item)

    add('M056', 'POST dirección duplicada', 409, 'ADDRESS_ALREADY_EXISTS',
        body={'streetAddress': '{{duplicateStreetAddress}}', 'ubigeoCode': '{{duplicateUbigeoCode}}'})
    add('M092', 'PUT hacia otra dirección existente', 409, 'ADDRESS_ALREADY_EXISTS',
        body={'streetAddress': '{{duplicateStreetAddress}}', 'ubigeoCode': '{{duplicateUbigeoCode}}'})
    add('M092', 'PUT conserva la ubicación propia', 200,
        body={'streetAddress': '{{existingStreetAddress}}', 'ubigeoCode': '{{existingUbigeoCode}}'})
    for query, label in [('pasteleria', 'Búsqueda sin tilde'), ('%25', 'Porcentaje literal'), ('_', 'Guion bajo literal')]:
        tests = [
            'pm.test("Coincidencias por nombre o slug", function () {',
            '  const rows = pm.response.json(); pm.expect(rows).to.be.an("array").that.is.not.empty;',
            '  const norm = s => s.normalize("NFD").replace(/[\\u0300-\\u036f]/g, "").toLowerCase();',
            f'  const q = norm(decodeURIComponent("{query}"));',
            '  rows.forEach(row => pm.expect(norm(row.name).includes(q) || norm(row.slug).includes(q)).to.eql(true));',
            '});',
        ]
        add('K002', label, 200, url=f'{{{{baseUrl}}}}/api/v1/stores/search?q={query}&limit=50', extra=tests)
    for limit in [0, 51]:
        add('K002', f'Límite inválido {limit}', 400, 'VALIDATION_ERROR',
            url=f'{{{{baseUrl}}}}/api/v1/stores/search?q=pasteleria&limit={limit}')
    add('K266', 'Fechas de clientes del comerciante', 200, extra=[
        'pm.test("Fechas ISO o nulos permitidos", function () {',
        '  const rows = pm.response.json(); pm.expect(rows).to.be.an("array").that.is.not.empty;',
        '  rows.forEach(row => [row.customerSince, row.lastOrderAt].forEach(value => {',
        '    if (value !== null) { pm.expect(value).to.be.a("string"); pm.expect(Number.isNaN(Date.parse(value))).to.eql(false); }',
        '  }));',
        '});',
    ])
    add('M056', 'Método no soportado conserva Allow', 405, 'METHOD_NOT_ALLOWED', method='PATCH', extra=[
        'pm.test("Allow presente", () => pm.expect(pm.response.headers.get("Allow")).to.include("GET"));',
    ])
    add('M053', 'Ruta inexistente devuelve error controlado', 404, 'RESOURCE_NOT_FOUND',
        url='{{baseUrl}}/api/v1/customer/addresses/{{addressId}}/missing')
    collection['item'].append({'name': REGRESSIONS, 'item': cases})


def prepare(source):
    collection = json.loads(source.read_text())
    collection['item'] = [item for item in collection['item'] if item.get('name') != REGRESSIONS]
    serialized = json.dumps(collection, ensure_ascii=False)
    for before, after in RENAMES.items():
        serialized = serialized.replace(before, after)
    collection = json.loads(serialized)
    for i, folder in enumerate(collection['item'], 1):
        folder['name'] = f'Grupo {i}'
    for item in walk(collection['item']):
        case_id, label, *_ = item['name'].split(' | ')
        item.pop('response', None)
        request = item['request']
        guard = item['event'][0]['script']['exec']
        item['event'][0]['script']['exec'] = [line.replace('JSON.stringify(pm.request.auth || {}))', 'JSON.stringify(pm.request.auth || {}) + JSON.stringify(pm.request.headers || {}))') for line in guard]
        request['description'] = f'Caso {case_id}; {label}. Ejecutar individualmente con datos locales desechables.'
        raw = request.get('body', {}).get('raw')
        if raw:
            try:
                body = json.loads(raw)
            except ValueError:
                raise ValueError(f'{case_id}: body raw debe ser JSON válido') from None
            request['body']['raw'] = json.dumps(sanitize_body(body, label), ensure_ascii=False, indent=2)
    add_regressions(collection)
    content = json.dumps(collection['item'], ensure_ascii=False)
    keys = set(re.findall(r'\{\{(\w+)\}\}', content))
    scripts = '\n'.join(line for item in walk(collection['item']) for event in item.get('event', []) for line in event['script'].get('exec', []))
    keys.update(re.findall(r'pm\.environment\.(?:get|set)\([\"\x27](\w+)[\"\x27]', scripts))
    keys.add('baseUrl')
    collection['variable'] = [{'key': key, 'value': BASE_URL if key == 'baseUrl' else '', 'type': 'string'} for key in sorted(keys)]
    collection['auth'] = {'type': 'noauth'}
    collection['info'] = {
        'name': 'JaldiShop API — desarrollo local',
        'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
        'description': 'Colección local con selección manual por runCase. Ver postman/README.md. Sin credenciales ni fixtures compartidos.',
    }
    environment = {
        'name': 'JaldiShop Local — completar valores privados',
        'values': [{'key': key, 'value': BASE_URL if key == 'baseUrl' else '',
                    'type': 'secret' if re.search(r'token|password|signature', key, re.I) else 'default', 'enabled': True}
                   for key in sorted(keys)],
        '_postman_variable_scope': 'environment',
    }
    serialized = json.dumps(collection, ensure_ascii=False)
    forbidden = r'APP_USR-|\bTEST-[A-Za-z0-9_-]{12,}|eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|-----BEGIN .*PRIVATE KEY|file://|/home/'
    if re.search(forbidden, serialized):
        raise ValueError('La exportación contiene un patrón sensible; revisar antes de compartirla.')
    return collection, environment


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', nargs='?', type=Path, default=POSTMAN / 'jaldishop.postman_collection.json')
    parser.add_argument('--output-dir', type=Path, default=POSTMAN)
    args = parser.parse_args()
    collection, environment = prepare(args.source)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for name, data in [('jaldishop.postman_collection.json', collection),
                       ('jaldishop.local.env.json', environment), ('jaldishop.postman_environment.json', environment)]:
        (args.output_dir / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'requests': len(list(walk(collection['item']))), 'variables': len(environment['values'])}))


if __name__ == '__main__':
    main()
