> **CANCELADO — 8 de noviembre de 2026.** Este procedimiento Fedora/Podman no se ejecutará ni forma parte del flujo del equipo. Se conserva únicamente como historial. Usar [la guía local del backend](../../backend/README.md). No ejecutar sus comandos de creación, red o limpieza.

# PostgreSQL desechable y primera ronda HTTP — preparación, 7 de noviembre de 2026

**Estado: BLOCKED para ejecución. Solo diagnóstico y documentación realizados. Todos los comandos de creación, compilación, SQL, arranque, HTTP y limpieza que siguen son propuestas para una autorización posterior; ninguno fue ejecutado.**

Referencias: [resultados](../verificacion-integracion-2026-11-07/00-resultados.md), [guía Postman](../preparacion-colecciones-2026-11-07/00-guia.md), [matriz](../verificacion-integracion-2026-11-07/01-matriz-equipo.md).

## 1. Evidencia actual y condiciones de parada

- Podman está instalado, pero incluso --version/ps/images/network ls/volume ls fallan: `Failed to obtain podman configuration ... /run/user/1000/libpod: read-only file system`. No se pudo conocer versión operativa, contenedores, imágenes, redes o volúmenes. No se intentó reparar permisos.
- ss -ltn falla con `Cannot open netlink socket: Operation not permitted`. No hay evidencia de puertos disponibles. El resultado no significa ausencia de servicios.
- DB_URL, DB_USERNAME, DB_PASSWORD, SPRING_PROFILES_ACTIVE, SPRING_DATASOURCE_URL, SPRING_FLYWAY_URL, JWT_SECRET, MERCADOPAGO_ACCESS_TOKEN y CLOUDINARY_API_SECRET no están definidos en el proceso inspeccionado. No se publicaron valores.
- backend/.env existe. Spring importa .env y BackendApplication usa Dotenv: busca backend/.env o .env desde el directorio actual y transforma entradas en propiedades cuando no existe la variable de entorno. NO arrancar desde el repositorio con valores por defecto. No se cargó ese archivo en un proceso ni se mostraron sus secretos.
- Un solo application.properties localizado; no hay perfil de integración aislado. Java 21 / Spring Boot 4.1.1. Flyway habilitado, classpath:db/migration, validación deshabilitada por defecto. Hibernate ddl-auto=none.
- 24 archivos SQL en el directorio de migraciones, hasta V25. Flyway elige los pendientes según su historial, no según el último número solamente. Algunas migraciones incluyen datos demo. Revisar todos antes de autorizarlas sobre la instancia nueva.
- Mercado Pago tiene URL real por defecto. Cloudinary usa cliente externo. Los schedulers financieros tienen default false, sin que eso certifique valores efectivos de otro proceso. Una URL localhost que rechaza conexiones NO es un fake funcional.
- systemd-run, systemctl, Podman, psql y openssl están en PATH; bpftool no. La capacidad efectiva de filtrar egress no se ha verificado.
- Se conservó el working tree con cambios anteriores. Solo se añade este documento; no se ejecutaron DB, HTTP, Spring Boot, migraciones, Postman o pagos.

**Parar si falla cualquier inspección, hay duda sobre un recurso, no se demuestra el servidor o no se demuestra el aislamiento de red del backend.** No usar sudo para sortear restricciones de esta sesión. Las comprobaciones siguientes se ejecutarán posteriormente en una terminal Fedora autorizada y operativa.

## 2. Diseño

Contenedor nuevo con nombre aleatorio y label de ejecución; imagen PostgreSQL 16 local identificada por ID inmutable; puerto 55432 ligado solo a 127.0.0.1; PGDATA en tmpfs, sin volumen ni bind mount existente. Base jaldishop_integration_test y usuario jaldi_it_owner creados solo dentro de esa instancia. El rol bootstrap es superusuario dentro del contenedor aislado: no se reutiliza en servidores existentes. Para una ronda posterior más amplia, separar rol migrador y rol de aplicación.

Credenciales aleatorias en memoria y archivo privado de una ejecución en /tmp, nunca en Git ni environments compartidos. Usar directorio de ejecución vacío para backend y env -i; pin explícito de datasource y Flyway. El backend solo escucha 127.0.0.1:8080. Ningún recurso existente se reutiliza, elimina o limpia. tmpfs pierde los datos al detener el contenedor: recrear otra instancia para reiniciar fixtures, no reanudar esperando conservarlos.

No se crea una red adicional: se inspecciona la red efectiva asignada por Podman después del run y se registra. La publicación loopback y una base nueva aíslan el acceso/datos, pero NO bloquean salida a Internet del JVM. El filtro de egress del apartado 6 es una condición independiente.

## 3. Inspección previa, sin creación

```bash
cd /home/ijosueeh/Documentos/Proyectos/jaldishop
podman --version
podman ps -a --format '{{.Names}} {{.Image}} {{.Status}} {{.Ports}}'
podman images --format '{{.Repository}}:{{.Tag}} {{.ID}}'
podman network ls
podman volume ls
ss -ltn
```

Todos deben funcionar. No usar inspect completo: el JSON de contenedores puede contener secretos. Confirmar puertos 55432/8080 libres tanto en IPv4 como IPv6; conservar la salida como evidencia privada. No asumir que hay una imagen postgres:16 instalada. Comprobarla sin descargar:

```bash
podman image exists docker.io/library/postgres:16
podman image inspect docker.io/library/postgres:16 --format '{{.Id}} {{.RepoDigests}}'
```

Si falta, parar: adquisición de imagen queda como paso separado, autorizado y verificado; no cambiar silenciosamente de versión.

## 4. Creación futura, solamente después de autorización

Ejecutar los bloques de esta guía en una sesión Bash dedicada. Las variables se mantienen en esa sesión; si se pierde, recuperar solo desde el directorio privado de esta ejecución, no buscar credenciales en otros environments. No activar set -x.

```bash
set -euo pipefail
umask 077
jaldi_project=/home/ijosueeh/Documentos/Proyectos/jaldishop
jaldi_run_dir=$(mktemp -d /tmp/jaldi-disposable.XXXXXXXX)
jaldi_run_id=$(openssl rand -hex 8)
jaldi_container="jaldi-it-${jaldi_run_id}"
jaldi_unit="jaldi-it-${jaldi_run_id}"
jaldi_image=$(podman image inspect docker.io/library/postgres:16 --format '{{.Id}}')
jaldi_password=$(openssl rand -hex 32)
jaldi_jwt=$(openssl rand -hex 32)

# Una lista exitosa distingue ausencia de colisiones de error de Podman.
podman ps -a --format '{{.Names}}' > "$jaldi_run_dir/container-names.txt"
if grep -Fxq "$jaldi_container" "$jaldi_run_dir/container-names.txt"; then
  echo 'Colisión de nombre: detener'; exit 1
fi
# Comprobación activa sin HTTP. Debe conseguir bind en ambos puertos.
python3 - <<'PY'
import socket
sockets=[]
try:
    for port in (55432, 8080):
        s=socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sockets.append(s)
        s.bind(('127.0.0.1', port))
finally:
    for s in sockets: s.close()
PY
# Hay una carrera tras liberar el puerto: si run/arranque falla, no escoger
# otro servidor ni otro puerto automáticamente.
printf '%s\n' "$jaldi_run_id" > "$jaldi_run_dir/run-id"
printf '%s\n' "$jaldi_container" > "$jaldi_run_dir/container-name"
printf '%s\n' "$jaldi_unit" > "$jaldi_run_dir/unit-name"
printf '%s\n' "$jaldi_image" > "$jaldi_run_dir/image-id"
printf '%s\n' "$jaldi_password" > "$jaldi_run_dir/db-password"
printf '%s\n' "$jaldi_jwt" > "$jaldi_run_dir/jwt-secret"

POSTGRES_PASSWORD="$jaldi_password" podman run --detach --pull=never \
  --name "$jaldi_container" \
  --label "io.jaldishop.disposable-run=$jaldi_run_id" \
  --publish 127.0.0.1:55432:5432 \
  --tmpfs /var/lib/postgresql/data:rw,size=1g \
  --env POSTGRES_DB=jaldishop_integration_test \
  --env POSTGRES_USER=jaldi_it_owner \
  --env POSTGRES_PASSWORD \
  "$jaldi_image" > "$jaldi_run_dir/container-id"
```

No se solicita volumen nombrado; no existe colisión de volumen nuevo. El tmpfs tiene capacidad limitada: si la inicialización o Flyway agotan espacio, parar y crear otra ejecución autorizada. El password de bootstrap se conserva en metadata de este contenedor hasta eliminarlo; no publicar inspect/logs completos.

## 5. Identidad de servidor antes de migraciones

```bash
podman inspect "$jaldi_container" \
  --format '{{.Id}} {{.Image}} {{.State.Status}} {{index .Config.Labels "io.jaldishop.disposable-run"}}'
podman port "$jaldi_container"
podman inspect "$jaldi_container" --format '{{json .NetworkSettings.Networks}}'
podman inspect "$jaldi_container" --format '{{json .HostConfig.Tmpfs}}'
podman exec "$jaldi_container" pg_isready \
  -U jaldi_it_owner -d jaldishop_integration_test
```

Si aún inicia, repetir pg_isready manualmente; no avanzar con error. Verificar mismo ID/imagen/label, estado running, tmpfs esperado, y únicamente 127.0.0.1:55432. La siguiente consulta viaja por EL MISMO host/puerto que usará Spring; no basta consultar solo por socket dentro del contenedor:

```bash
PGPASSWORD="$jaldi_password" psql -X \
  -h 127.0.0.1 -p 55432 -U jaldi_it_owner \
  -d jaldishop_integration_test -v ON_ERROR_STOP=1 \
  -c "SELECT current_database(), current_user, inet_server_addr(),
             inet_server_port(), current_setting('server_version'),
             current_setting('data_directory'), pg_is_in_recovery();" \
  -c 'SELECT system_identifier FROM pg_control_system();' \
  -c "SELECT schemaname, tablename FROM pg_tables
      WHERE schemaname NOT IN ('pg_catalog','information_schema');" \
  > "$jaldi_run_dir/server-before.txt"

podman exec "$jaldi_container" psql -X -U jaldi_it_owner \
  -d jaldishop_integration_test -v ON_ERROR_STOP=1 \
  -c 'SELECT system_identifier FROM pg_control_system();' \
  > "$jaldi_run_dir/server-inside.txt"
```

Comparar ambos system_identifier y archivar: deben coincidir, current_database debe ser jaldishop_integration_test y current_user jaldi_it_owner. No debe haber tablas de aplicación ni historial Flyway antes del primer arranque. El puerto del servidor en SQL será 5432 (interno); el publicado es 55432. Ante tablas inesperadas o identidad distinta: detener, sin borrar datos.

Inventario exacto de migraciones a revisar:

```bash
find "$jaldi_project/backend/src/main/resources/db/migration" \
  -maxdepth 1 -type f -name '*.sql' -print | sort -V \
  > "$jaldi_run_dir/migrations.txt"
sha256sum "$jaldi_project"/backend/src/main/resources/db/migration/*.sql \
  > "$jaldi_run_dir/migrations.sha256"
```

Revisar SQL, incluidos seeds, antes de autorizar Flyway. Contra base nueva sin historial serán candidatas todas las migraciones de versión válidas del directorio. Confirmar luego los realmente aplicados; no afirmar que se ejecutaron por existir en el repositorio.

## 6. Aislamiento de integraciones: condición anterior al JVM

Para estos cuatro casos no se necesita Mercado Pago ni Cloudinary. Mantener endpoints financieros, webhooks, conciliación, reembolsos y multimedia fuera de ejecución. Deshabilitar explícitamente schedulers y eliminar credenciales efectivas. Además bloquear salida de red no-loopback del proceso Java.

Una opción en Fedora es systemd con IPAddressDeny=any/IPAddressAllow=localhost. Requiere soporte cgroup/BPF efectivo, que **NO está verificado aquí**. Una propiedad visible en systemctl NO prueba por sí sola que el kernel la aplica. Un administrador debe confirmar el filtro aplicado al cgroup y ausencia de avisos de soporte deshabilitado. bpftool no está disponible en PATH de esta sesión. Si no puede demostrar la restricción, NO iniciar el backend; preparar primero aislamiento de red verificable. No instalar herramientas ni alterar firewall durante esta fase.

No permitir acceso a Internet para “probar” si un proveedor funciona. No usar Render/Neon como comprobaciones. No equiparar URL 127.0.0.1:9 con fake: solo es un destino local de fallo, no implementa el contrato de Mercado Pago.

## 7. Construcción y arranque posteriores, con aislamiento demostrado

Construir el código actual, después de autorización; el JAR previo puede estar desactualizado. No activa Spring ni DB, pero sí modifica target:

```bash
(cd "$jaldi_project/backend" && mvn -o -DskipTests package)
mkdir "$jaldi_run_dir/app"
cp "$jaldi_project/backend/target/backend-0.0.1-SNAPSHOT.jar" \
  "$jaldi_run_dir/app/backend.jar"
sha256sum "$jaldi_run_dir/app/backend.jar" > "$jaldi_run_dir/backend.sha256"
```

El siguiente bloque **habilita Flyway y modifica solo la DB ya verificada**. Requiere autorización posterior y el control efectivo de egress del apartado 6. Se usa entorno limpio, directorio vacío y argumentos de mayor precedencia para impedir redirecciones mediante configuración heredada. No elegir un perfil ficticio como garantía de aislamiento.

```bash
(
  cd "$jaldi_run_dir/app"
  env -i PATH=/usr/bin:/bin XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:?}" \
    systemd-run --user --unit="$jaldi_unit" --collect \
    --property="WorkingDirectory=$jaldi_run_dir/app" \
    --property=IPAddressDeny=any --property=IPAddressAllow=localhost \
    /usr/bin/env -i PATH=/usr/bin:/bin \
    DB_URL=jdbc:postgresql://127.0.0.1:55432/jaldishop_integration_test \
    DB_USERNAME=jaldi_it_owner DB_PASSWORD="$jaldi_password" \
    JWT_SECRET="$jaldi_jwt" \
    /usr/lib/jvm/java-21-temurin-jdk/bin/java -jar ./backend.jar \
    --spring.config.import= \
    --spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/jaldishop_integration_test \
    --spring.datasource.username=jaldi_it_owner \
    '--spring.datasource.password=${DB_PASSWORD}' \
    --spring.datasource.hikari.jdbc-url=jdbc:postgresql://127.0.0.1:55432/jaldishop_integration_test \
    --spring.datasource.hikari.username=jaldi_it_owner \
    '--spring.datasource.hikari.password=${DB_PASSWORD}' \
    --spring.datasource.hikari.data-source-properties.ApplicationName="$jaldi_run_id" \
    --spring.flyway.url=jdbc:postgresql://127.0.0.1:55432/jaldishop_integration_test \
    --spring.flyway.user=jaldi_it_owner '--spring.flyway.password=${DB_PASSWORD}' \
    --spring.flyway.enabled=true --spring.flyway.validate-on-migrate=true \
    --spring.flyway.locations=classpath:db/migration \
    --spring.jpa.hibernate.ddl-auto=none \
    --server.address=127.0.0.1 --server.port=8080 \
    --app.payment.reconciliation.enabled=false \
    --app.payment.reconciliation.orders-enabled=false \
    --payments.mercadopago.refund-scheduler-enabled=false \
    --mercadopago.base-url=http://127.0.0.1:9 \
    --mercadopago.access-token= --mercadopago.webhook-secret= \
    --mercadopago.notification-url=http://127.0.0.1:8080/disabled \
    --mercadopago.back-url-success=http://127.0.0.1:8080/disabled \
    --mercadopago.back-url-failure=http://127.0.0.1:8080/disabled \
    --mercadopago.back-url-pending=http://127.0.0.1:8080/disabled \
    --cloudinary.cloud-name=demo --cloudinary.api-key=demo --cloudinary.api-secret=demo
)
```

Advertencia de privacidad del comando futuro: systemd registra argv del servicio; el entorno de `/usr/bin/env` aparece en argv e incluye secretos locales desechables. No publicar el estado/inspección completa del servicio o su D-Bus. Una instalación persistente debe emplear credenciales systemd/archivos protegidos y no este mecanismo temporal. Aquí no son secretos productivos y se eliminan al terminar.

Si systemd no permite filtros efectivos, no sustituir por java directo ni eliminar las propiedades para lograr el arranque. Evitar abrir Actuator env/configprops; no son necesarios para demostrar destino.

Verificación real después del arranque:

```bash
systemctl --user is-active "$jaldi_unit.service"
systemctl --user show "$jaldi_unit.service" \
  -p IPAddressDeny -p IPAddressAllow -p ControlGroup
# Consultar logs privadamente; no compartir cuerpos de seed o diagnósticos con secretos.
journalctl --user -u "$jaldi_unit.service" --no-pager
PGPASSWORD="$jaldi_password" psql -X -h 127.0.0.1 -p 55432 \
  -U jaldi_it_owner -d jaldishop_integration_test -v ON_ERROR_STOP=1 \
  -c "SELECT datname, usename, application_name, client_addr, state
      FROM pg_stat_activity WHERE application_name = '$jaldi_run_id';" \
  -c 'SELECT installed_rank, version, description, script, checksum, success
      FROM flyway_schema_history ORDER BY installed_rank;' \
  > "$jaldi_run_dir/server-after.txt"
```

Debe haber conexión Hikari con application_name de esta ejecución, DB/usuario correctos, migraciones esperadas y success=true, mismo system_identifier y solo escucha loopback. La configuración propuesta no equivale a conexión efectiva: estas evidencias son obligatorias.

## 8. HTTP/Postman posterior: cuatro casos, no colección completa

Solo después de autorización de HTTP y pasos anteriores:

```bash
curl --noproxy '*' --max-time 10 --fail-with-body \
  -D "$jaldi_run_dir/health.headers" \
  -o "$jaldi_run_dir/health.json" \
  http://127.0.0.1:8080/api/v1/health
```

Esta es una solicitud adicional al caso J001 si luego se repite desde Postman. No ejecutar ahora. Health de este controlador no prueba por sí solo DB: usar también SQL anterior.

Importar postman/jaldishop.postman_collection.json en Postman moderno con skipRequest. Crear environment PRIVADO nuevo, sin reutilizar los existentes. baseUrl=http://127.0.0.1:8080; disposableFixturesReady=true solo después de la evidencia de aislamiento; runCase exactamente un ID. financialFakeReady vacío; allowPaymentOperations vacío. Sin tokens para esta ronda. Confirmar proxies deshabilitados para loopback, redirects deshabilitados y URL resuelta local antes de Send. No Run Collection.

| ID | Caso | Esperado |
|---|---|---|
| J001 | GET /api/v1/health | 200 |
| K002 | GET /api/v1/stores/search | 200, lista puede estar vacía |
| M028 | GET /api/v1/users/me sin autenticación | 401 |
| J029 | GET /api/v1/admin/users sin autenticación | 401 |

K002 puede requerir storeSearchQuery por la plantilla; usar texto de fixture como jaldi, no un secreto. Las migraciones pueden crear datos demo: estos cuatro casos no requieren registrar usuarios. No ejecutar logins financieros ni crear pedidos/reservas.

Por cada solicitud registrar ID, fecha, SHA del JAR, ID contenedor/system_identifier, base/usuario, URL sin credenciales, HTTP esperado/obtenido, aserciones y evidencia. No exportar environments poblados. Éxito de esta ronda no certifica 97 endpoints ni 583 escenarios.

## 9. Limpieza exclusivamente por identidad de ejecución

Solo después de revisar y conservar resultados sin secretos. Estos comandos también requieren autorización posterior. Primero comprobar que las variables provienen del directorio privado de ESTA ejecución; si se perdieron, no seleccionar por prefijo ni por nombre de base.

```bash
: "${jaldi_run_dir:?}" "${jaldi_run_id:?}" "${jaldi_container:?}" "${jaldi_unit:?}"
test "$(cat "$jaldi_run_dir/run-id")" = "$jaldi_run_id"
test "$(cat "$jaldi_run_dir/container-name")" = "$jaldi_container"
test "$(cat "$jaldi_run_dir/unit-name")" = "$jaldi_unit"
test "$(podman inspect "$jaldi_container" --format '{{.Id}}')" \
  = "$(cat "$jaldi_run_dir/container-id")"
test "$(podman inspect "$jaldi_container" --format '{{index .Config.Labels "io.jaldishop.disposable-run"}}')" \
  = "$jaldi_run_id"

# Verificar la unidad registrada antes de detenerla; no usar pkill/java/puerto.
systemctl --user stop "$jaldi_unit.service"
podman stop "$jaldi_container"
podman rm "$jaldi_container"
unset jaldi_password jaldi_jwt
# Retirar únicamente los dos archivos de credenciales registrados.
rm -- "$jaldi_run_dir/db-password" "$jaldi_run_dir/jwt-secret"
```

Si el servicio nunca llegó a existir o ya se recolectó, comprobarlo manualmente antes de continuar; no sustituir el nombre. Si cualquier test de identidad falla, parar. No `podman prune`, `volume rm`, `rm -rf`, `DROP DATABASE`, `TRUNCATE` ni borrar por patrón. No existe volumen nombrado que limpiar. Detener tmpfs destruye los fixtures de esta instancia; no afecta a otras bases. Conservar el resto del directorio privado como evidencia y revisar su contenido antes de compartirlo.

## 10. Bloqueos pendientes

1. Terminal con Podman y sockets operativos; inventario de recursos y colisiones comprobado.
2. Imagen local PostgreSQL 16 revisada y fijada por ID.
3. Autorización para crear la instancia y aplicar SQL/Flyway exclusivamente sobre ella.
4. Filtro de egress del JVM aplicado y demostrado; la disponibilidad de systemd-run por sí sola no basta.
5. Autorización separada para arrancar backend y hacer las cuatro solicitudes HTTP.

Nada de esto se resolvió ejecutando comandos de mutación en esta fase. La guía es reproducible bajo sus precondiciones, pero no se declara validada en ejecución.
