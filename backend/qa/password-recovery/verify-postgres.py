#!/usr/bin/env python3
"""Validate all migrations and recovery SQL on a NEW cluster, without sockets or external DB URLs."""
import os
from pathlib import Path
import re
import subprocess
import tempfile

bin_dir = Path(os.environ['PASSWORD_RECOVERY_TEST_PG_BIN']).resolve()
share = os.environ.get('PASSWORD_RECOVERY_TEST_PG_SHARE')
lib = os.environ.get('PASSWORD_RECOVERY_TEST_PG_LIB')
migrations = Path(__file__).resolve().parents[2] / 'src/main/resources/db/migration'
with tempfile.TemporaryDirectory(prefix='jaldi-recovery-sql-') as temporary:
    data = Path(temporary) / 'data'
    init = [str(bin_dir / 'initdb'), '-D', str(data), '-U', 'recovery_test', '-A', 'trust',
            '--no-locale', '--encoding=UTF8']
    if share:
        init += ['-L', share]
    subprocess.run(init, check=True, stdout=subprocess.DEVNULL, timeout=30)
    statements = []
    for migration in sorted(migrations.glob('V*__*.sql'), key=lambda f: int(f.name.split('__')[0][1:])):
        if migration.name.startswith('V27__'):
            statements.append('CREATE TABLE recovery_before AS SELECT * FROM users;')
        statements.append('\n'.join(line for line in migration.read_text().splitlines() if line.strip()))
    statements.append(r"""
DO $checks$
DECLARE
 u UUID := gen_random_uuid();
 other UUID := gen_random_uuid();
 n integer;
BEGIN
 -- Migration does not alter any preexisting user fields, and all versions start at zero.
 IF EXISTS (SELECT 1 FROM recovery_before b FULL JOIN users existing_user ON b.id=existing_user.id
  WHERE b IS NULL OR existing_user IS NULL OR b.email IS DISTINCT FROM existing_user.email
  OR b.password_encoded IS DISTINCT FROM existing_user.password_encoded
  OR b.first_name IS DISTINCT FROM existing_user.first_name OR b.last_name IS DISTINCT FROM existing_user.last_name
  OR b.phone IS DISTINCT FROM existing_user.phone OR b.status IS DISTINCT FROM existing_user.status
  OR b.created_at IS DISTINCT FROM existing_user.created_at OR b.updated_at IS DISTINCT FROM existing_user.updated_at
  OR existing_user.credentials_version <> 0) THEN RAISE EXCEPTION 'Existing users changed'; END IF;
 INSERT INTO users(id,email,password_encoded,first_name,last_name,status,created_at,updated_at)
 VALUES (u,'recovery-sql@example.test','old-encoded','Test','User','ACTIVE',now(),now()),
        (other,'other-recovery-sql@example.test','old-encoded','Test','User','ACTIVE',now(),now());
 INSERT INTO password_reset_tokens VALUES(u, repeat('a',64), now() + interval '15 minutes');
 BEGIN
  INSERT INTO password_reset_tokens VALUES(other, repeat('a',64), now());
  RAISE EXCEPTION 'Duplicate token accepted';
 EXCEPTION WHEN unique_violation THEN NULL; END;
 BEGIN
  INSERT INTO password_reset_tokens VALUES(other, 'invalid', now());
  RAISE EXCEPTION 'Invalid hash accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 -- Exact expiry boundary and nonexistent hash never consume a token.
 DELETE FROM password_reset_tokens WHERE user_id=u AND token_hash=repeat('a',64) AND expires_at > now()+interval '15 minutes';
 GET DIAGNOSTICS n = ROW_COUNT;
 IF n <> 0 THEN RAISE EXCEPTION 'Expiry boundary accepted'; END IF;
 DELETE FROM password_reset_tokens WHERE user_id=u AND token_hash=repeat('b',64) AND expires_at > now();
 GET DIAGNOSTICS n = ROW_COUNT;
 IF n <> 0 THEN RAISE EXCEPTION 'Unknown hash accepted'; END IF;
 -- A failed password update rolls back token consumption in the same transaction/subtransaction.
 BEGIN
  DELETE FROM password_reset_tokens WHERE user_id=u AND token_hash=repeat('a',64) AND expires_at>now();
  UPDATE users SET credentials_version=-1 WHERE id=u;
  RAISE EXCEPTION 'Invalid credentials version accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 IF NOT EXISTS(SELECT 1 FROM password_reset_tokens WHERE user_id=u) THEN RAISE EXCEPTION 'Token lost after rollback'; END IF;
 -- Rotation leaves one outstanding hash.
 INSERT INTO password_reset_tokens VALUES(u, repeat('b',64), now()+interval '15 minutes')
 ON CONFLICT(user_id) DO UPDATE SET token_hash=EXCLUDED.token_hash, expires_at=EXCLUDED.expires_at;
 IF EXISTS(SELECT 1 FROM password_reset_tokens WHERE token_hash=repeat('a',64)) THEN RAISE EXCEPTION 'Old token survived rotation'; END IF;
 DELETE FROM password_reset_tokens WHERE user_id=u AND token_hash=repeat('b',64) AND expires_at>now();
 GET DIAGNOSTICS n = ROW_COUNT;
 IF n <> 1 THEN RAISE EXCEPTION 'First consumption failed'; END IF;
 UPDATE users SET password_encoded='new-encoded', credentials_version=credentials_version+1 WHERE id=u;
 DELETE FROM password_reset_tokens WHERE user_id=u AND token_hash=repeat('b',64) AND expires_at>now();
 GET DIAGNOSTICS n = ROW_COUNT;
 IF n <> 0 THEN RAISE EXCEPTION 'Reused token accepted'; END IF;
 IF NOT EXISTS(SELECT 1 FROM users WHERE id=u AND password_encoded='new-encoded' AND credentials_version=1)
 THEN RAISE EXCEPTION 'Credentials not updated'; END IF;
 INSERT INTO password_reset_tokens VALUES(u, repeat('c',64), now());
 DELETE FROM users WHERE id=u;
 IF EXISTS(SELECT 1 FROM password_reset_tokens WHERE user_id=u) THEN RAISE EXCEPTION 'Cascade failed'; END IF;
 RAISE NOTICE 'PASS: additive migration, data preservation, uniqueness, hash constraint, expiry, unknown hash, rollback, rotation, single use, version, cascade';
END
$checks$;
SELECT 'PASS: recovery SQL checks completed';
SELECT version();
""")
    command = [str(bin_dir / 'postgres'), '--single', '-j', '-D', str(data)]
    if share:
        command += ['-c', 'extension_control_path=' + share]
    if lib:
        command += ['-c', 'dynamic_library_path=' + lib]
    command += ['postgres']
    result = subprocess.run(command, input='\n\n'.join(statements) + '\n\n', text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
    print(result.stdout)
    if result.returncode or re.search(r'\b(ERROR|FATAL|PANIC):', result.stdout) or 'PASS:' not in result.stdout:
        raise SystemExit('PostgreSQL recovery validation FAILED')
