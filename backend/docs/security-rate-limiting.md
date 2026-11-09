# Rate limiting y revisión SQL focalizada

## Alcance y revisión

No existía rate limiting o bloqueo de intentos local. AuthController ofrece login,
registro de clientes y comerciantes; login verifica BCrypt y emite JWT. Los
endpoints públicos están enumerados en SecurityConfig. El usuario confirmó Render Free, Docker, jaldishop-api.onrender.com y sin CDN
adicional. Free no admite más de una instancia según la documentación oficial.

Revisión SQL: las consultas @Query JPQL/nativas de los repositorios usan parámetros
nombrados o SQL constante. Los usos JdbcTemplate en CheckoutCreationRequestStore,
ManualOrderInvestigationService y PaymentWebhookPersistenceService usan `?` con
argumentos separados. No se encontraron createNativeQuery/createQuery ni SQL
construido con entradas externas en producción. Las concatenaciones de @Query
son constantes; concatenaciones MANUAL_/WEBHOOK_ son valores parametrizados.
No se identificaron consultas vulnerables ni casos pendientes por construcción
SQL dinámica. No se reescribieron repositorios.

StoreJpaRepository usa parámetros y LOCATE con public.unaccent: %, _ y barra
inversa siguen siendo literales. StoreSearchPostgresIntegrationTest ya cubre
acentos, comodines y `' OR 1=1 --`. Las páginas usan PageRequest y no interpolan
identificadores u ordenamiento SQL externos. La búsqueda pública valida limit.

## Implementación

Ventanas fijas desde la primera solicitud, sincronizadas, en memoria JVM:

| Política | Ruta | Valor inicial |
|---|---|---|
| login-account | POST /api/v1/auth/login | 5 / 60 s por email normalizado |
| login-ip | POST /api/v1/auth/login | 30 / 60 s por IP |
| register-ip | POST /api/v1/auth/register y /register/merchant | 3 / 600 s compartidos por IP |
| search-ip | GET /api/v1/stores/search y /featured | 60 / 60 s por IP |

Se cuentan solicitudes, incluso logins exitosos. No hay bloqueo permanente.
No se aplica límite general autenticado. La fase 2 añade únicamente las políticas
selectivas descritas al final de este documento. Webhooks y procesos internos quedan fuera.
429 conserva ApiError (code RATE_LIMIT_EXCEEDED, message, errors) y Retry-After
expresa segundos enteros hasta la expiración. No se añaden logs de credenciales.

Bucket4j es compatible con Java moderno, pero aún requeriría gestionar claves,
expiración y capacidad. Para estas cuatro políticas se eligió una ventana fija
sin dependencia adicional. Puede aceptar ráfagas en bordes de ventanas.

## Configuración y operación

En application.properties: app.security.rate-limit.<política>.limit y
.window-seconds. Variables RATE_LIMIT_LOGIN_ACCOUNT_LIMIT,
RATE_LIMIT_LOGIN_ACCOUNT_WINDOW_SECONDS, RATE_LIMIT_LOGIN_IP_LIMIT,
RATE_LIMIT_LOGIN_IP_WINDOW_SECONDS, RATE_LIMIT_REGISTER_IP_LIMIT,
RATE_LIMIT_REGISTER_IP_WINDOW_SECONDS, RATE_LIMIT_SEARCH_IP_LIMIT,
RATE_LIMIT_SEARCH_IP_WINDOW_SECONDS y RATE_LIMIT_MAX_ENTRIES.

Máximo inicial público: 10000 claves. La fase 2 usa otro almacén y otro bloqueo
para contadores autenticados, con capacidad independiente de 10000 claves,
configurable mediante RATE_LIMIT_AUTHENTICATED_MAX_ENTRIES. Al recibir solicitudes
se eliminan ventanas expiradas. En inactividad pueden quedar claves expiradas,
siempre acotadas. Al llenarse se rechazan nuevas claves hasta la primera
expiración; no se expulsan contadores activos que permitirían evadir límites.
Cada instancia tiene estado propio; reinicios, despliegues y cold starts lo
reinician. Antes de escalar a varias instancias reevaluar almacenamiento compartido.

### Identificación de IP: pendiente de habilitación en Render

**No está lista para producción.** El commit 0998718 activaba automáticamente
CF-Connecting-IP basándose en variables Render. Esa detección no autentica una
cabecera ni demuestra su conservación y saneamiento hasta el contenedor.

| Cabecera | Contrato documentado | Garantía que falta para JaldiShop |
|---|---|---|
| X-Forwarded-For | Render la recomienda. Cloudflare agrega la IP de quien conecta si ya había valores; puede conservar prefijos externos. | Cadena de proxies confiables, tratamiento de duplicados y valores aportados por el cliente; no hay cantidad fija de saltos documentada en las fuentes revisadas. |
| CF-Connecting-IP | Cloudflare entrega un valor al origen; lo recomienda por su formato de una IP. | Render no garantiza expresamente su conservación y saneamiento hasta el contenedor en las fuentes revisadas. No basta que sea un literal válido. |

Fuentes oficiales revisadas el 2026-11-09:
- [Render: DDoS](https://render.com/articles/how-render-handles-ddos-attacks):
  todo ingreso público atraviesa Cloudflare y balanceadores Render; recomienda
  XFF. No describe una política completa de confianza para estas cabeceras.
- [Render Free](https://render.com/docs/free): una instancia, sin ingreso privado.
  Reduce rutas de entrada pero no autentica una cabecera HTTP.
- [Cloudflare: cabeceras](https://developers.cloudflare.com/fundamentals/reference/http-headers/):
  XFF puede conservar valores previos. CF-Connecting-IP tiene excepciones para
  Workers: misma zona puede reflejar x-real-ip modificado por un Worker; entre
  zonas usa una IP de Worker fija. Transformaciones pueden retirar cabeceras y
  Pseudo IPv4 puede sustituir valores. No asumir conocer la configuración de Render.

No se cambia a XFF ni se toma la primera IP. Una futura resolución XFF requerirá
identificar al peer confiable y recorrer la cadena desde la derecha, descartando
sólo proxies explícitamente verificados; no confiar en todas las redes privadas,
ni fijar un número de saltos por observación de una sola solicitud.

### Comportamiento actual

server.forward-headers-strategy=none se mantiene.
- Local (RENDER distinto de true): límites por IP activos usando getRemoteAddr,
  ignorando CF-Connecting-IP, XFF, Forwarded y demás cabeceras de IP.
- Render (RENDER=true): políticas por IP **inactivas por defecto** mientras la
  verificación esté pendiente. No hay fallback a la IP del proxy, ni 503 por
  ausencia de cabecera en este modo. El límite login-account sigue activo.
  Registro y búsqueda quedan sin rate limiting por IP durante esta espera:
  es una protección incompleta, no un modo aprobado para producción.
- RATE_LIMIT_RENDER_IP_VERIFIED=false es el valor inicial, enlazado a
  app.security.rate-limit.render-ip-verified. Sólo después de verificar la frontera
  completa se podrá activar explícitamente. Esta marca es una declaración del
  operador, **no una validación automática**. Una cabecera HTTP no puede activarla.
- El candidato optativo CF-Connecting-IP se restringe al hostname de despliegue
  jaldishop-api.onrender.com en la variable de entorno, nunca al HTTP Host.
  Conserva validación de un único literal y normalización IPv4/IPv6, sin DNS.
  Las pruebas de este parser no demuestran autenticidad en Render.

Con el candidato activado, cabeceras ausentes, duplicadas o inválidas aún dan
503 CLIENT_IP_UNAVAILABLE sólo en rutas limitadas. Sí: si Render omite la cabecera
en todas las solicitudes, inutiliza login, ambos registros, búsqueda y destacados.
No afecta todas las rutas públicas. El modo pendiente evita ese fallo por defecto.
No se cambian los valores de límites ni se modifica el flujo de pagos.

### Verificación requerida antes de habilitar

1. Obtener confirmación de Render de la ruta completa hacia el contenedor Free,
   sin acceso alternativo que permita saltarse el saneamiento, y del contrato
   de sustitución/conservación de CF-Connecting-IP. Confirmar Workers,
   transformaciones, duplicados y comportamiento IPv6/Pseudo IPv4. Si Render sólo
   respalda XFF, obtener su cadena de confianza y implementar esa política antes
   de habilitar nada; esta marca no selecciona XFF.
2. En staging controlado del mismo plan/dominio de ingreso, comprobar solicitudes
   desde dos IP conocidas, sin cabeceras y con CF-Connecting-IP, XFF y Forwarded
   falsificados, duplicados y combinados. Debe observarse la identidad saneada,
   independiente de lo enviado por el cliente, y contadores separados por origen.
   Usar observación temporal sin tokens/contraseñas ni endpoint público permanente;
   retirar la instrumentación. No son pruebas de carga.
3. Verificar presencia consistente de la cabecera en solicitudes normales antes
   de aceptar el comportamiento 503 del modo activado. Si no está garantizada,
   mantener desactivado y resolver el contrato con Render. Unas pruebas puntuales
   no sustituyen la garantía de rutas y saneamiento del proveedor.
4. Registrar evidencia y política elegida; revisar de nuevo antes de cambiar
   plan, dominio, CDN, Workers o ingreso privado. No habilitar por el solo hecho
   de que RENDER=true o de que una cabecera tenga formato correcto.

No se hicieron solicitudes al servicio desplegado ni se modificó infraestructura.

## Verificación local

Revisión exclusiva de IP posterior a 0998718: 25 pruebas pasaron, sin fallos,
errores ni omitidas: ClientIpResolverTest (14), RateLimitsTest (6) y
AuthControllerTest (5). Incluyen modo Render pendiente por defecto, cabeceras HTTP
incapaces de habilitarlo, ausencia de 503 en modo pendiente, límite por cuenta
activo, límites locales activos e impacto 503 del candidato habilitado.
Se compiló mediante Maven test y git diff --check pasó. No se ejecutaron SQL,
bases externas ni pruebas contra Render en esta revisión.


Compilación y package con skipTests; suite focalizada ClientIpResolverTest, RateLimitsTest,
AuthControllerTest, JwtAuthenticationFilterTest, SecurityUserStatusWebTest y
StoreSearchQueryTest. Mockito requiere -javaagent con su jar local en este
entorno para evitar el bloqueo de auto-adjunción. Las pruebas nuevas cubren
429, Retry-After, normalización/independencia de cuentas, IP falsificada,
expiración, capacidad, concurrencia, registro compartido y exclusión del webhook.
Resultado previo (0998718): 53 pruebas focalizadas pasaron (12 del resolver, 6 de
rate limiting y 35 existentes), sin fallos ni errores; package y diff --check
pasaron. Se intentó StoreSearchPostgresIntegrationTest con PostgreSQL 18.6 local
y un clúster temporal: initdb funcionó, pero el sandbox impide abrir el socket
local (java.net.SocketException: Operation not permitted). La prueba no alcanzó
las consultas SQL. No se accedió a bases externas.

## Referencias Git

bf21bba se conserva como ancestro. origin/main local apunta a
c6d6528755118232d40bce4b5b40249fd1f0cfde y está contenido en la rama.
Los intentos de git fetch origin/main fallaron por resolución DNS de github.com;
no se puede afirmar sincronización con el main remoto actual ni integrar commits
remotos desconocidos. No hubo rebase, merge, push ni cambios de infraestructura.

## Fase 2: operaciones autenticadas selectivas

Base revisada: `0cee818`, rama `security/backend-rate-limiting`, árbol limpio.
La auditoría sigue correspondiendo al código de esa base. Implementación en
`security/authenticated-rate-limiting-phase2`; sin modificación de .env,
secretos, configuración de producción ni confianza de proxy.

| Política | Entradas | Inicial | Variable de límite / ventana |
|---|---|---|---|
| reservation-create-user | POST /api/v1/checkout y POST /api/v1/capacity/reservations | 12 / 60 s compartidos por userId | RATE_LIMIT_RESERVATION_CREATE_USER_LIMIT / RATE_LIMIT_RESERVATION_CREATE_USER_WINDOW_SECONDS |
| payment-create-user | Nuevo intento desde POST /api/v1/checkout/mercado-pago/preference | 6 / 60 s por userId | RATE_LIMIT_PAYMENT_CREATE_USER_LIMIT / RATE_LIMIT_PAYMENT_CREATE_USER_WINDOW_SECONDS |
| payment-admin-remote-user | POST /api/v1/admin/payments/attempts/{attemptId}/investigate-order y /reconcile-order | 10 / 60 s compartidos por administrador | RATE_LIMIT_PAYMENT_ADMIN_REMOTE_USER_LIMIT / RATE_LIMIT_PAYMENT_ADMIN_REMOTE_USER_WINDOW_SECONDS |

Son valores iniciales de admisión, no cifras derivadas de tráfico de producción:

- Reservas duran 10 minutos. Doce solicitudes por minuto dejan margen para
  cambios de franja/tienda, varias pestañas y reintentos, sin permitir un bucle
  ilimitado. La espera máxima de esta ventana es un minuto; cerca de expirar,
  incluso esa espera puede requerir seleccionar otra franja.
- Pagos ya serializa por reserva, reutiliza sesiones completas y rechaza
  operaciones en curso/resultados inciertos. Seis intentos nuevos por minuto
  permiten recuperarse de rechazos explícitos sin fomentar repetición remota.
  Recuperar sesiones existentes no consume cuota, aun con cuota agotada.
- Diez operaciones administrativas remotas por minuto admiten revisión manual
  de varios casos y frenan bucles/dobles clics. Un lease controla concurrencia,
  pero no sustituye el presupuesto entre distintos intentos. No se limita la
  suspensión/activación de usuarios o tiendas.

Las políticas leen `app.security.rate-limit.<política>.limit` y `.window-seconds`,
igual que las públicas. No dependen de la habilitación de IP de Render.
No hay nueva dependencia ni almacenamiento externo.

Checkout/reservas/admin reciben el `JwtPrincipal` establecido tras validar JWT
y consultar usuario/roles actuales. El comando de pagos se construye con ese
principal y se comprueba la propiedad de la reserva bloqueada antes de consultar
la cuota. No se usa un userId enviado en JSON, query o cabeceras libres, ni el
token como clave del contador.

Checkout y reserva directa consumen una vez en su controlador; el servicio
interno de reservas no consume. Se mantienen los roles actuales: checkout exige
autenticación sin rol adicional; reserva directa exige CUSTOMER. Admin conserva
ADMIN mediante seguridad HTTP y de método, antes de comprobar la cuota.

En pagos se retorna primero una sesión reutilizable. Para un nuevo pago se
validan carrito/precios/snapshot, se comprueba cuota y sólo después se protege
la reserva y se persisten Payment/PaymentAttempt. Tras un rechazo anterior,
la cuota se comprueba antes de crear/guardar otro intento. Un 429 se produce
antes de cualquier mutación o llamada remota. La admisión en memoria no se
revierte si después falla la transacción o el proveedor: se limita el trabajo
admitido, no sólo los pagos exitosos. Se mantienen bloqueos e idempotencia.

Cancelaciones, liberaciones, webhooks, compensaciones, reembolsos y ejecución
interna de conciliación no invocan estas políticas. Compartir userId con otra
política no comparte su cuota. La capacidad de cada almacén sí es global a
sus políticas: al llenarse, nuevas claves de ese almacén reciben 429.

HTTP conserva `429`, `RATE_LIMIT_EXCEEDED` y `Retry-After`. El BFF de checkout y
`customerProxy` propagan la cabecera sin cambiar su valor; los clientes directos
pueden leerla mediante `Access-Control-Expose-Headers: Retry-After`. No existen
actualmente proxies de marketplace para preferencia o reserva directa que haya
que añadir para esta fase.

Riesgos pendientes: el presupuesto no es una cuota de reservas activas por
usuario ni corrige la aceptación de subintervalos con contadores por igualdad
exacta de franja. No se cambia ese diseño de capacidad. Tampoco se ofrece
exclusión mutua de toda investigación manual: se limita frecuencia y se
conservan los controles de concurrencia existentes. Persisten reinicio de
contadores al reiniciar JVM, ráfagas en bordes de ventanas y coste de limpiar
mapas en cada admisión. La validación JWT/usuario en BD ocurre antes de limitar;
esto no protege de toda saturación del backend.

Para calibrar posteriormente: observar 429 por política, tiempo de espera,
reintentos y expiraciones legítimas sin registrar tokens/datos personales;
ajustar los valores en un despliegue controlado. No se hicieron pruebas de carga
contra Render ni operaciones de pago reales.

### Verificación de fase 2

- Suite focalizada: RateLimitsTest, ClientIpResolverTest,
  AuthenticatedRateLimitWebTest, CheckoutControllerTest,
  CapacityReservationControllerTest, CreatePaymentPreferenceServiceTest y
  AdminOrderInvestigationSecurityTest. Sin fallos, errores ni omisiones.
- Suite ampliada sin los contextos que requieren BD: 1249 pruebas pasaron,
  cero fallos/errores. Otras 14 pruebas de integración fueron omitidas por sus
  condiciones existentes. Las integraciones completas PostgreSQL/pagos no se
  ejecutaron: no se habilitó una BD de prueba dedicada en esta ejecución.
- Los tests HTTP usan SecurityConfig/filtro reales con JWT/usuario simulados,
  incluyendo JWT inválido/expirado, roles insuficientes, IDs falsificados,
  cambio de token, políticas independientes, concurrencia, CORS y 429. Un caso
  ejecuta checkout y creación de reserva reales sobre repositorios simulados
  para demostrar que el servicio interno no duplica la admisión.
- En pagos se comprueba que un 429 no muta la reserva, no llama a save de
  reserva/pago/intento ni al gateway, para primer intento y tras rechazo.
  Se recuperan sesiones existentes Preferences y Orders con cuota agotada.
  Es prueba de rechazo previo a escrituras, no una integración de rollback SQL.
- Se añadieron pruebas Vitest para ambos proxies. npm ci no pudo completarse:
  caché incompleta y EAI_AGAIN al resolver registry.npmjs.org. Vitest/build de
  marketplace quedan pendientes. Una comprobación aislada con Node 24 ejecutó
  los TS reales con Web Response y adaptadores mínimos de Next: seis casos
  pasaron (429, código, Retry-After numérico/fecha y ausencia de cabecera en 200).
  No sustituye una ejecución con Next instalado ni una prueba de navegador.
- Mockito necesitó su agente explícito en esta JVM, mediante
  `-DargLine=-javaagent:/ruta/mockito-core-5.23.0.jar`. No se cambió la dependencia.
- `git diff --check` pasó. No hubo solicitudes al despliegue ni pagos reales.
