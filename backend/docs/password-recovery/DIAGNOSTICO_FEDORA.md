# Diagnóstico agente / terminal Fedora — 2026-11-10

La restricción se reprodujo dentro del proceso del agente. **El estado de una terminal normal del anfitrión todavía no está comprobado.** La vista de `/proc`, los permisos remapeados y los montajes del agente no permiten concluir que Fedora tenga las mismas restricciones.

No se modificaron implementación, pruebas, permisos del sistema, sysctls, SELinux, seccomp ni configuración de Podman. No se reejecutó Maven: el requisito previo de abrir sockets sigue fallando. La certificación permanece pendiente y no se ha obtenido evidencia nueva para aprobar el PR.

## Comandos y resultados dentro del agente

| Comprobación | Resultado |
|---|---|
| Python `socket(AF_INET, SOCK_STREAM)` en `127.0.0.1` | `PermissionError(1, 'Operation not permitted')` al crear el socket |
| Python socket Unix | Mismo error |
| `/proc/self/status` | `NoNewPrivs: 1`, `Seccomp: 2`, un filtro |
| `/proc/self/uid_map` | Una entrada: `1000 0 1`; vista de usuario remapeada |
| `/proc/sys/user/max_user_namespaces` | `2147483647`; el límite visible no está desactivado |
| `/proc/sys/kernel/unprivileged_userns_clone` | Archivo ausente; eso por sí solo no demuestra que los namespaces estén deshabilitados |
| `podman info --format '{{.Host.Security.Rootless}}'` | No puede crear `/run/user/1000/libpod`: filesystem de solo lectura |
| Podman con almacenamiento y runtime nuevos en `/tmp` | `newuidmap: write to uid_map failed: Operation not permitted` |
| PostgreSQL 18.6 portable, cluster temporal nuevo | `initdb` termina con 0; el servidor termina con 1 al abrir `127.0.0.1:55439` |
| PostgreSQL aceptando conexiones en esta sesión | No disponible: `FATAL: could not create any TCP/IP sockets` |

El intento de Podman aislado usó únicamente rutas temporales:

```bash
jaldi_runtime_probe=$(mktemp -d /tmp/jaldi-userns-check-XXXXXXXX)
XDG_RUNTIME_DIR="$jaldi_runtime_probe" podman --remote=false \
  --root "$jaldi_runtime_probe/storage" \
  --runroot "$jaldi_runtime_probe/runtime" --storage-driver=vfs \
  info --format '{{.Host.Security.Rootless}}'
```

No se iniciaron contenedores ni se cambiaron rutas globales. La prueba PostgreSQL creó un cluster propio mediante `initdb -D <directorio temporal nuevo>` y luego intentó arrancar `postgres -D <ese directorio> -h 127.0.0.1 -p 55439`. No usó `DB_URL`, `.env`, Neon ni una instalación de desarrollo. La evidencia de ese intento está en `/tmp/jaldi-environment-diagnosis-8kwwa0wp/postgres.log`.

Se ejecutó además el diagnóstico reutilizable sin iniciar contenedores:

```bash
python qa/password-recovery/diagnosticar-fedora.py
```

Terminó con código 1 y `DIAGNOSTIC_FINISHED=BLOCKED`. La sintaxis, la ayuda y el camino bloqueado se comprobaron aquí. El ciclo opcional de contenedor se comprobó con simulaciones de éxito, creación parcial, etiqueta ajena e imagen ausente; **su ejecución real desde Fedora queda pendiente**.

## Comando para la terminal normal de Fedora

Ejecutar fuera de Codex y de contenedores, como el usuario habitual, **sin sudo**:

```bash
cd /home/ijosueeh/Documentos/Proyectos/jaldishop/backend
python3 qa/password-recovery/diagnosticar-fedora.py --postgres-container
```

El script está en [qa/password-recovery/diagnosticar-fedora.py](../../qa/password-recovery/diagnosticar-fedora.py). Requiere `python3`, Podman rootless y `psql`. No instala paquetes ni cambia configuraciones.

Si indica que falta la imagen, se puede descargar explícitamente y repetir el diagnóstico:

```bash
podman --remote=false pull docker.io/library/postgres:16-alpine
python3 qa/password-recovery/diagnosticar-fedora.py --postgres-container
```

La imagen coincide con la referencia PostgreSQL de CI. El script usa `--pull=never`: no descarga imágenes automáticamente.

## Qué hace el script

1. Muestra el ámbito del proceso, las entradas de subuid/subgid del usuario y la ubicación/modo/capacidades de los helpers. Un modo sin setuid no se interpreta automáticamente como fallo: también se muestran capacidades de archivo si `getcap` está disponible.
2. Prueba `bind`, `listen`, `connect`, `accept` y transferencia TCP real entre conexiones locales.
3. Consulta Podman local con `--remote=false` y comprueba el namespace rootless mediante `podman unshare cat /proc/self/uid_map`.
4. Solo si las comprobaciones anteriores pasan y se pidió `--postgres-container`, crea un contenedor PostgreSQL **nuevo**, con nombre y etiqueta únicos, sin montar carpetas del proyecto ni usar volúmenes existentes.
5. Sus datos viven en tmpfs de 256 MB. Publica un puerto dinámico únicamente en `127.0.0.1`; no usa el puerto habitual 5432 del anfitrión ni `--network=host`.
6. Genera una contraseña temporal, guardada en un archivo 0600, y conecta desde el `psql` de Fedora a la base nueva `jaldishop_diagnostic_test`. No lee ni reutiliza variables libpq, `.env` o credenciales del proyecto.
7. Verifica identidad de base, usuario y versión. Finalmente elimina solo el contenedor cuya etiqueta coincide con el identificador generado. También intenta limpiar una creación parcial. No ejecuta `prune`, no borra imágenes y no toca otros contenedores o volúmenes.

No ejecuta migraciones, Maven, correos ni funcionalidades de negocio. Es una comprobación de infraestructura previa a la certificación.

La salida normal no contiene contraseñas. Debe terminar con `POSTGRES_HOST_CONNECTION=OK`, `DIAGNOSTIC_CONTAINER_REMOVED=OK` y `DIAGNOSTIC_FINISHED=OK`. Si falla la limpieza, indica el nombre específico del contenedor para inspeccionarlo; no aplica limpiezas generales.

## Cómo interpretar la comparación

| Terminal normal | Conclusión permitida |
|---|---|
| TCP, Podman y PostgreSQL OK | El anfitrión puede proporcionar la infraestructura; la restricción observada queda acotada a la sesión del agente |
| TCP OK, Podman falla | El TCP del anfitrión funciona; queda un problema específico de rootless/helper/runtime que requiere su propia evidencia. Un PostgreSQL nativo aislado puede ser una alternativa en la siguiente fase |
| TCP falla también | La terminal usada también está restringida. No concluir automáticamente que todo Fedora está roto; verificar que realmente es una terminal fuera de contenedores/sandboxes |
| TCP y Podman OK, PostgreSQL falla | Revisar el error de imagen, red, arranque o conexión del contenedor; todavía no ejecutar certificación |

Una prueba de infraestructura satisfactoria **no aprueba Password Recovery**. Después de comparar la salida se podrá preparar y ejecutar la certificación sobre bases nuevas `jaldishop_integration_test` y `jaldishop_fase2a_test`, manteniendo correo deshabilitado e importación de `.env` deshabilitada.

Por instrucción del usuario, el trabajo se detiene aquí hasta disponer del resultado de esa terminal. Siguen pendientes JDBC con conexiones independientes, las carreras reales, el historial Flyway hasta V27, WebSocket TCP y Maven completo en un entorno funcional. No se hicieron commits, push, publicación ni despliegues.
