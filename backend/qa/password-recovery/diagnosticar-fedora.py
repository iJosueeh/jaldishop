#!/usr/bin/env python3
"""Compare this process with a normal Fedora terminal; optionally probe a disposable local PG16 container.

Does not load .env, use existing databases, change policies, pull images, run Maven, or send mail.
"""
import argparse
import json
import os
from pathlib import Path
import pwd
import re
import secrets
import shutil
import socket
import stat
import subprocess
import tempfile
import time
import uuid

IMAGE = 'docker.io/library/postgres:16-alpine'
LABEL = 'com.jaldishop.password-recovery.diagnostic'
DATABASE = 'jaldishop_diagnostic_test'
USERNAME = 'jaldi_diagnostic'


def command(args, *, env=None, timeout=20):
    try:
        return subprocess.run(args, env=env, stdin=subprocess.DEVNULL, text=True,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise RuntimeError(f'Cannot execute {args[0]}: {type(error).__name__}') from None


def output_failure(result):
    # Called only for infrastructure commands, never commands carrying credentials.
    print((result.stderr or result.stdout).strip()[:2000])


def socket_probe():
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
            listener.settimeout(3)
            listener.bind(('127.0.0.1', 0))
            listener.listen(1)
            with socket.create_connection(listener.getsockname(), timeout=3) as client:
                with listener.accept()[0] as accepted:
                    accepted.settimeout(3)
                    client.sendall(b'jaldi-local-probe')
                    received = b''
                    while len(received) < len(b'jaldi-local-probe'):
                        fragment = accepted.recv(64)
                        if not fragment:
                            break
                        received += fragment
                    if received != b'jaldi-local-probe':
                        raise RuntimeError('Unexpected TCP probe response')
        print('TCP_LOOPBACK=OK (bind, listen, connect, accept and data)')
        return True
    except (OSError, RuntimeError) as error:
        print(f'TCP_LOOPBACK=FAILED: {type(error).__name__}: {error}')
        return False


def process_diagnostics():
    print('UID=' + str(os.getuid()))
    release = Path('/etc/fedora-release')
    print('FEDORA=' + (release.read_text().strip() if release.exists() else 'release file absent'))
    for filename in ['/proc/self/status', '/proc/self/uid_map',
                     '/proc/sys/user/max_user_namespaces', '/proc/sys/kernel/unprivileged_userns_clone']:
        path = Path(filename)
        if not path.exists():
            print(filename + '=not present')
            continue
        content = path.read_text().strip()
        if filename.endswith('/status'):
            content = '\n'.join(line for line in content.splitlines()
                                if line.startswith(('NoNewPrivs:', 'Seccomp:', 'Seccomp_filters:')))
        print(filename + ':\n' + content)
    username = pwd.getpwuid(os.getuid()).pw_name
    for name in ['subuid', 'subgid']:
        path = Path('/etc') / name
        try:
            entries = [line for line in path.read_text().splitlines()
                       if line.split(':')[0] in (username, str(os.getuid()))]
            print(name + '=' + ('; '.join(entries) if entries else 'no entry for current user'))
        except OSError:
            print(name + '=unavailable')
    for executable in ['podman', 'newuidmap', 'newgidmap', 'initdb', 'postgres', 'psql', 'java']:
        location = shutil.which(executable)
        print(executable + '=' + (location or 'not in PATH'))
        if location and executable in ['newuidmap', 'newgidmap']:
            print(executable + '_mode=' + stat.filemode(Path(location).stat().st_mode))
            if shutil.which('getcap'):
                capabilities = command(['getcap', location])
                print(executable + '_capabilities=' + (capabilities.stdout.strip() or 'none reported'))


def podman_probe():
    if not shutil.which('podman'):
        print('PODMAN_ROOTLESS=UNAVAILABLE')
        return False
    result = command(['podman', '--remote=false', 'info', '--format', '{{.Host.Security.Rootless}}'])
    if result.returncode != 0:
        print('PODMAN_ROOTLESS=FAILED')
        output_failure(result)
        return False
    rootless = result.stdout.strip() == 'true'
    print('PODMAN_ROOTLESS=' + ('OK' if rootless else 'FAILED: not rootless'))
    if not rootless:
        return False
    mapping = command(['podman', '--remote=false', 'unshare', 'cat', '/proc/self/uid_map'])
    if mapping.returncode != 0:
        print('PODMAN_USER_NAMESPACE=FAILED')
        output_failure(mapping)
        return False
    print('PODMAN_USER_NAMESPACE=OK\n' + mapping.stdout.strip())
    return True


def disposable_postgres_probe():
    if not shutil.which('psql'):
        raise RuntimeError('psql is required to verify a connection from the Fedora terminal; nothing started')
    available = command(['podman', '--remote=false', 'image', 'exists', IMAGE])
    if available.returncode != 0:
        raise RuntimeError('Image not present. No automatic download. If desired, run: '
                           'podman --remote=false pull ' + IMAGE + ' ; then repeat this diagnostic')
    marker = uuid.uuid4().hex
    name = 'jaldi-password-diagnostic-' + marker[:12]
    cid = None
    cleanup_ok = True
    with tempfile.TemporaryDirectory(prefix='jaldi-password-diagnostic-') as directory:
        envfile = Path(directory) / 'container.env'
        password = secrets.token_urlsafe(32)
        envfile.write_text('POSTGRES_USER=' + USERNAME + '\nPOSTGRES_DB=' + DATABASE
                           + '\nPOSTGRES_PASSWORD=' + password + '\n')
        envfile.chmod(0o600)
        try:
            start = command(['podman', '--remote=false', 'run', '--detach', '--pull=never',
                             '--name', name, '--label', LABEL + '=' + marker,
                             '--network=bridge', '--publish', '127.0.0.1::5432',
                             '--tmpfs', '/var/lib/postgresql/data:rw,nosuid,nodev,size=256m',
                             '--env-file', str(envfile), IMAGE], timeout=30)
            if start.returncode != 0:
                output_failure(start)
                raise RuntimeError('Disposable PostgreSQL container did not start')
            cid = start.stdout.strip()
            if not re.fullmatch(r'[0-9a-f]{64}', cid):
                raise RuntimeError('Unexpected container identifier; inspect the uniquely named container ' + name)
            print('DIAGNOSTIC_CONTAINER=' + name)
            ready = False
            for _ in range(40):
                status = command(['podman', '--remote=false', 'exec', cid, 'pg_isready',
                                  '-h', '127.0.0.1', '-p', '5432', '-U', USERNAME, '-d', DATABASE], timeout=5)
                if status.returncode == 0:
                    ready = True
                    break
                time.sleep(0.5)
            if not ready:
                raise RuntimeError('PostgreSQL did not become ready within the diagnostic interval')
            mapping = command(['podman', '--remote=false', 'port', cid, '5432/tcp'])
            matched = re.fullmatch(r'127\.0\.0\.1:([0-9]+)', mapping.stdout.strip())
            if mapping.returncode != 0 or not matched:
                raise RuntimeError('Expected a single published port on IPv4 loopback only')
            port = matched.group(1)
            # Remove inherited libpq service/host/options settings; use explicit loopback arguments.
            child_env = {key: value for key, value in os.environ.items() if not key.startswith('PG')}
            child_env.update(PGPASSWORD=password, PGCONNECT_TIMEOUT='5', PGSSLMODE='disable')
            query = command(['psql', '-X', '-w', '-h', '127.0.0.1', '-p', port, '-U', USERNAME,
                             '-d', DATABASE, '-At', '-v', 'ON_ERROR_STOP=1', '-c',
                             "SELECT current_database(), current_user, current_setting('server_version');"],
                            env=child_env, timeout=10)
            if query.returncode != 0:
                print((query.stderr or query.stdout).replace(password, '[REDACTED]')[:1000])
                raise RuntimeError('PostgreSQL is not accessible from the host through its loopback port')
            row = query.stdout.strip().split('|')
            if len(row) != 3 or row[:2] != [DATABASE, USERNAME]:
                raise RuntimeError('Unexpected diagnostic database identity')
            print('POSTGRES_HOST_CONNECTION=OK')
            print('POSTGRES_RESULT=' + json.dumps(dict(database=row[0], username=row[1], version=row[2])))
            print('POSTGRES_ADDRESS=127.0.0.1:' + port)
            print('No migrations or Maven tests executed; this is an infrastructure diagnostic only.')
        finally:
            # A failed `podman run` can leave a stopped container. Check the unique name too.
            candidate = cid if cid and re.fullmatch(r'[0-9a-f]{64}', cid) else name
            identity = command(['podman', '--remote=false', 'container', 'exists', candidate])
            if identity.returncode == 1:
                print('DIAGNOSTIC_CONTAINER_REMOVED=NONE_REMAINS')
            elif identity.returncode != 0:
                raise RuntimeError('Cannot confirm container cleanup; inspect ' + name)
            else:
                identity = command(['podman', '--remote=false', 'inspect', '--format',
                                    '{{ index .Config.Labels "' + LABEL + '" }}', candidate])
                if identity.returncode == 0 and identity.stdout.strip() == marker:
                    removed = command(['podman', '--remote=false', 'rm', '--force', candidate])
                    cleanup_ok = removed.returncode == 0
                else:
                    cleanup_ok = False
                print('DIAGNOSTIC_CONTAINER_REMOVED=' + ('OK' if cleanup_ok else 'FAILED: inspect ' + name))
                if not cleanup_ok:
                    raise RuntimeError('Cleanup not confirmed; no other containers or volumes were touched')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--postgres-container', action='store_true',
                        help='also test a new local PG16 container (existing image only), then remove it')
    args = parser.parse_args()
    os.umask(0o077)
    if os.getuid() == 0:
        raise RuntimeError('Run as the normal Fedora user, without sudo')
    process_diagnostics()
    tcp_ok = socket_probe()
    podman_ok = podman_probe()
    if args.postgres_container:
        if not tcp_ok or not podman_ok:
            raise RuntimeError('Socket/rootless prerequisite failed; no PostgreSQL container was created')
        disposable_postgres_probe()
    print('DIAGNOSTIC_FINISHED=' + ('OK' if tcp_ok and podman_ok else 'BLOCKED'))
    return 0 if tcp_ok and podman_ok else 1


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except (RuntimeError, OSError, KeyboardInterrupt) as failure:
        print('DIAGNOSTIC_STOPPED=' + str(failure))
        raise SystemExit(1)
