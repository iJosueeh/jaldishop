#!/usr/bin/env python3
"""Run SQL regression checks in a NEW PostgreSQL cluster, without sockets or external URLs."""
import os
from pathlib import Path
import re
import subprocess
import tempfile

bin_dir = Path(os.environ['STORE_SEARCH_TEST_PG_BIN']).resolve()
migrations = Path(__file__).resolve().parents[2] / 'src/main/resources/db/migration'
with tempfile.TemporaryDirectory(prefix='jaldi-store-search-') as temporary:
    data = Path(temporary) / 'data'
    subprocess.run([str(bin_dir / 'initdb'), '-D', str(data), '-U', 'store_search_test',
                    '-A', 'trust', '--no-locale', '--encoding=UTF8'], check=True,
                   stdout=subprocess.DEVNULL, timeout=30)
    statements = []
    for migration in sorted(migrations.glob('V*__*.sql'), key=lambda p: int(p.name.split('__')[0][1:])):
        # Single-user -j delimits batches by blank lines; retain SQL line comments.
        statements.append('\n'.join(line for line in migration.read_text().splitlines() if line.strip()))
    statements.append(r"""
CREATE TEMP TABLE stores (name text, slug text, status varchar, created_at int);
INSERT INTO stores VALUES
 ('Pastelería Delicia', 'pasteleria-delicia', 'ACTIVE', 1),
 ('Pastelería Dulce Deleite', 'dulce-deleite', 'ACTIVE', 2),
 ('Panadería Norte', 'pan-del-norte', 'ACTIVE', 3),
 ('PANADERÍA Sur', 'horno-sur', 'ACTIVE', 4),
 ('Oferta 50% real', 'oferta-porcentaje', 'ACTIVE', 5),
 ('Tienda_A', 'guion-bajo', 'ACTIVE', 6),
 ('Ruta \ centro', 'ruta-central', 'ACTIVE', 7),
 ('Pastelería Oculta', 'pasteleria-inactiva', 'INACTIVE', 8),
 ('Pastelería Suspendida', 'pasteleria-suspendida', 'SUSPENDED', 9),
 ('Pastelería Cerrada', 'pasteleria-cerrada', 'CLOSED', 10);
CREATE FUNCTION pg_temp.search_stores(q text, wanted varchar)
RETURNS TABLE (slug text, created_at int) LANGUAGE sql AS $search$
 SELECT s.slug, s.created_at FROM stores s
 WHERE (wanted IS NULL OR s.status = wanted)
 AND (q IS NULL OR q = ''
   OR position(lower(public.unaccent(q)) in lower(public.unaccent(s.name))) > 0
   OR position(lower(public.unaccent(q)) in lower(public.unaccent(s.slug))) > 0)
 ORDER BY s.created_at DESC
$search$;
DO $checks$
DECLARE
 q text;
 actual text[];
 expected text[];
 cases jsonb := '[
 ["pasteleria", ["dulce-deleite", "pasteleria-delicia"]],
 ["Pastelería", ["dulce-deleite", "pasteleria-delicia"]],
 ["PASTELERIA", ["dulce-deleite", "pasteleria-delicia"]],
 ["PASTELERÍA", ["dulce-deleite", "pasteleria-delicia"]],
 ["Pastelería", ["dulce-deleite", "pasteleria-delicia"]],
 ["panaderia", ["horno-sur", "pan-del-norte"]],
 ["PANADERÍA", ["horno-sur", "pan-del-norte"]],
 ["dulce", ["dulce-deleite"]],
 ["PAN-DEL-NORTE", ["pan-del-norte"]],
 ["zzzz-sin-coincidencias", []],
 ["%", ["oferta-porcentaje"]],
 ["_", ["guion-bajo"]],
 ["\\", ["ruta-central"]],
 ["50%", ["oferta-porcentaje"]],
 ["%_\\", []],
 ["'' OR 1=1 --", []]
 ]';
 item jsonb;
 n integer;
BEGIN
 -- Reproduce the accent bug and wildcard expansion in the old query first.
 SELECT count(*) INTO n FROM stores s WHERE s.status = 'ACTIVE' AND
   (lower(s.name) LIKE lower('%pasteleria%') OR lower(s.slug) LIKE lower('%pasteleria%'));
 IF n <> 1 THEN RAISE EXCEPTION 'Old accent reproduction: %', n; END IF;
 SELECT count(*) INTO n FROM stores s WHERE s.status = 'ACTIVE' AND
   (lower(s.name) LIKE lower('%%%') OR lower(s.slug) LIKE lower('%%%'));
 IF n <> 7 THEN RAISE EXCEPTION 'Old wildcard reproduction: %', n; END IF;
 FOR item IN SELECT jsonb_array_elements(cases) LOOP
   q := item->>0;
   SELECT coalesce(array_agg(value), ARRAY[]::text[]) INTO expected
     FROM jsonb_array_elements_text(item->1);
   SELECT coalesce(array_agg(s.slug ORDER BY s.created_at DESC), ARRAY[]::text[]) INTO actual
     FROM pg_temp.search_stores(q, 'ACTIVE') s;
   IF actual IS DISTINCT FROM expected THEN
     RAISE EXCEPTION 'Query %: expected %, actual %', q, expected, actual;
   END IF;
 END LOOP;
 SELECT count(*) INTO n FROM pg_temp.search_stores('', 'ACTIVE');
 IF n <> 7 THEN RAISE EXCEPTION 'Blank query: %', n; END IF;
 SELECT count(*) INTO n FROM pg_temp.search_stores(NULL, 'ACTIVE');
 IF n <> 7 THEN RAISE EXCEPTION 'Null query: %', n; END IF;
 SELECT count(*) INTO n FROM (SELECT * FROM pg_temp.search_stores('pasteleria', 'ACTIVE') LIMIT 1) limited;
 IF n <> 1 THEN RAISE EXCEPTION 'SQL limit: %', n; END IF;
 SELECT count(*) INTO n FROM pg_temp.search_stores('pasteleria', 'INACTIVE');
 IF n <> 1 THEN RAISE EXCEPTION 'Admin status: %', n; END IF;
 SELECT count(*) INTO n FROM pg_temp.search_stores('pasteleria', NULL);
 IF n <> 5 THEN RAISE EXCEPTION 'Admin all statuses: %', n; END IF;
 RAISE NOTICE 'SQL assertions passed';
END
$checks$;
SELECT 'PASS: old bug reproduced; 21 SQL regression cases on real PostgreSQL';
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM stores s WHERE s.status = 'ACTIVE' AND
 (position(lower(public.unaccent('pasteleria')) in lower(public.unaccent(s.name))) > 0 OR
  position(lower(public.unaccent('pasteleria')) in lower(public.unaccent(s.slug))) > 0)
 ORDER BY s.created_at DESC;
SELECT version();
""")
    result = subprocess.run([str(bin_dir / 'postgres'), '--single', '-j', '-D', str(data), 'postgres'],
                            input='\n\n'.join(statements) + '\n\n', text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
    print(result.stdout)
    if result.returncode or re.search(r'\b(ERROR|FATAL|PANIC):', result.stdout) or 'PASS:' not in result.stdout:
        raise SystemExit('PostgreSQL regression validation FAILED')
