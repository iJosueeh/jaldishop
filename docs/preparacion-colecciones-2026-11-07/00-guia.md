# Preparación del equipo — 7 de noviembre de 2026

**Postman preparado y validado estáticamente: 97 endpoints, 100 requests base conservados y 583 casos. Cero HTTP ejecutado. Bruno pendiente de ubicación canónica confirmada.**

## Fuente y respaldo

Fuente de rutas: inventario de la auditoría; responsables: matriz de integración. Colección canónica mantenida: `postman/jaldishop.postman_collection.json`. Respaldo del archivo anterior, privado y fuera de Git: `/tmp/jaldishop-collection-backup-20261107-111408/jaldishop.postman_collection.json` (directorio 0700, archivo 0600). No se sobrescribió el .bak previo ni ningún environment. El generador toma explícitamente ese respaldo, no la colección ya generada, para evitar duplicar escenarios.

Bruno: ambas copias documentadas existen en scratch externo. Una tiene bruno.json y otra no; ninguna está declarada fuente vigente del equipo. No se sobrescribió ni creó Bruno. [Comparación por firma](02-diferencias-bruno.md): faltantes, adicionales, duplicados y diferencias de body/headers/auth/variables frente al Postman previo. La elección de ubicación canónica sigue pendiente; después se debe generar/importar desde Postman y portar guards/aserciones, no mantener a mano dos contratos divergentes.

## Organización y cobertura

- Katherine: 44 endpoints y 274 casos. Merchant, tiendas, catálogo, inventario, capacidad y multimedia.
- Mia: 38 endpoints y 237 casos. Customer, autenticación/perfil, direcciones, favoritos, carrito, pedidos, reseñas, notificaciones y catálogo público.
- Josue: 15 endpoints y 72 casos. Administración, checkout, pagos y revisión transversal de seguridad/integridad.

Cada caso incluye ID estable para esta versión, módulo, endpoint, responsable, resultado esperado y riesgo. [Matriz de casos por endpoint](01-cobertura-casos.md); [índice estructurado](casos.json). Variantes de login y webhook son casos del mismo endpoint. Capacidad/inventario/checkout se marcan COMPARTIDO: Katherine prepara stock/cupo y opera; Mia aporta carrito/dirección; Josue observa reservas, rollback y concurrencia.

Los casos generados cubren éxito, no-auth en rutas protegidas, rol incorrecto cuando la anotación lo demuestra, UUID de path inválido, campos DTO obligatorios ausentes/nulos y máximos de String identificados. Incluyen variantes [null], campos internos nulos/vacíos, nombre 120/121, duplicados tras trim, lista vacía/nula; Merchant nombres 100/101, email 254/255, formato y password corto; firmas multimedia propias/ajenas, customer, visitante y branding temporal. Recursos ajenos/inexistentes tienen casos concretos de direcciones y multimedia, sin generalizar 403/404 a todas las rutas.

**No es cobertura negativa exhaustiva.** Conflictos de transición, concurrencia, expiración y repetición necesitan preparar estados concretos según el servicio; la matriz registra esos casos manuales. No se añadieron negativos de cuerpo a GET sin cuerpo ni 404 universales. HTTP esperado presupone todas las otras precondiciones correctas. Los scripts heredados permanecen en los casos base; los negativos no extraen IDs ni tokens.

## Variables y fixtures

Todos los valores de variables versionadas están vacíos salvo baseUrl=http://127.0.0.1:8080. Los cuerpos de login/registro usan variables de email/password. No se tocaron environments existentes. Usar un environment LOCAL PRIVADO distinto por integrante, con prefijo de fixture y fecha/ejecución en correos, nombres y SKU; no exportar su estado poblado ni versionar tokens. Los logins separan customerToken/merchantToken/adminToken; authToken heredado permanece para compatibilidad de scripts anteriores. merchantOnlyToken debe pertenecer a un usuario MERCHANT sin CUSTOMER para los negativos que exigen CUSTOMER.

Crear identidades A/B para cada rol, tiendas propias/ajenas, productos/variantes activos/inactivos, stock/cupo 1 y suficiente. Los IDs globales (storeId, productId, etc.) pertenecen al environment privado de cada ejecutor; no intercambiar uno poblado. otherStoreId/otherAddressId deben pertenecer a B. missingAddressId debe ser UUID válido confirmado ausente en el fixture. Crear un correo nuevo por alta válida; los casos de frontera también crean cuentas. serviceDate debe ser futura en Lima; configurar tiempos coherentes.

IDs de carrito, reserva, checkout, pedido e intento provienen del flujo real con fake, nunca de referencias de proveedor inventadas. Verificar también los IDs que extraen scripts heredados antes de avanzar. El generador no certifica automáticamente las relaciones entre fixtures.

## Ejecución manual

Requiere Postman moderno que soporte pm.execution.skipRequest. Importar colección y crear environment privado. Inspeccionar visualmente destino y body antes de Send. Elegir un ID de la matriz y definir runCase exactamente con ese ID. Configurar disposableFixturesReady=true solo después de demostrar que el backend local usa una base desechable. Esta declaración no puede comprobar la conexión interna del backend: se necesita la verificación de integración previa.

El guard exige HTTP loopback (localhost, 127.0.0.1 o ::1), variables usadas no vacías, selección exacta y fixtures declarados desechables. Redirecciones HTTP deshabilitadas. Con runCase vacío se omiten todas las solicitudes. No ejecutar Run Collection como flujo automático: las mutaciones requieren preparar/recrear fixtures entre casos; repetir el mismo runner puede volver a ejecutar el caso seleccionado. El guard no ofrece idempotencia.

1. Josue verifica destino, aislamiento y roles; Mia realiza logins y prepara customer A/B; Katherine registra merchant/tiendas. Reiniciar newKatherineEmail entre altas y fronteras. No usar los mismos SKU para todos los éxitos.
2. Katherine prepara categorías/productos/variantes, inventario y capacidad. Mia prueba lecturas públicas, direcciones/favoritos/carrito. Una respuesta 401/403 previa no demuestra validación de cuerpo.
3. Josue prueba checkout con dependencias locales y fake; Mia y Katherine verifican pedido customer/merchant. Anotar recuentos/postcondiciones antes y después de entradas inválidas por los medios locales autorizados; esta preparación no prueba ausencia de escrituras parciales.
4. Reseñas/notificaciones dependen de pedido elegible. Ejecutar aislamiento de recursos A/B y revocación con segundo administrador. Las pruebas STOMP de suscripción activa requieren cliente WebSocket separado; no están incluidas en los 97 endpoints REST.

Precondiciones especiales: REPETIR_SKU_EXISTENTE presupone variante ya creada con ese SKU y otro intento de creación sobre el mismo catálogo; MERCHANT_DUPLICADO usa existingMerchantEmail registrado como MERCHANT. ADMIN_REVOCADO_TOKEN_ANTERIOR requiere guardar el token de A en revokedAdminToken, retirar ADMIN con B y mantener A activo; ese caso solo lee usuarios y espera 403. No usar admin único para retirar roles.

## Operaciones sensibles

Todas las escrituras se marcan MANUAL. Pedidos/reservas/checkout pueden bloquear stock/cupos; recuperar o recrear sus fixtures. GET de reservas puede materializar expiración y escribir: no tratarlo como inocuo solo por el método. Multipart/uploads no se envían a Cloudinary; una firma válida tampoco autoriza subir archivos reales en esta fase.

Los cuatro endpoints payment y sus clones requieren además financialFakeReady=true, únicamente con proveedor fake demostrado y sin egress externo. Conciliar/investigar conservan su guard previo allowPaymentOperations=true. No habilitar estas variables por conveniencia: la colección no puede probar que el backend realmente usa fake. Webhook requiere firma local de fixture; no reutilizar secretos reales. Ni el guard ni las pruebas unitarias sustituyen supervisión manual de operaciones financieras.

## Validación realizada y límites

`node postman/tools/validar_equipo.cjs`: JSON parseable, 583 IDs únicos, 100 casos base, sintaxis de 1207 scripts, variables vacías y redirects deshabilitados. Guards probados en VM JavaScript sin HTTP: selección ausente, host externo, fixtures no confirmados y variables faltantes bloquean; localhost preparado permite el caso; financieros exigen fake. Inventario/matriz: 97 firmas presentes y reparto 44/38/15 comprobado.

No se ejecutó Postman/Newman/Bruno contra servidor ni se validó importación en sus interfaces. No se certifica funcionamiento de scripts heredados o contratos dinámicos hasta ejecución real. No se modificaron Java, Flyway, frontend ni archivos con secretos; no hubo conexión DB ni operaciones financieras.

## Variables declaradas (solo nombres)

`addressId`, `adminEmail`, `adminPassword`, `adminToken`, `attemptId`, `authPassword`, `authToken`, `baseUrl`, `capacityConfigId`, `capacityExceptionId`, `cartId`, `categoryId`, `checkoutReferenceId`, `customerEmail`, `customerPassword`, `customerToken`, `disposableFixturesReady`, `endTime`, `existingMerchantEmail`, `financialFakeReady`, `fixturePassword`, `merchantEmail`, `merchantOnlyToken`, `merchantPassword`, `merchantToken`, `missingAddressId`, `newJosueEmail`, `newKatherineEmail`, `newMiaEmail`, `notificationId`, `orderId`, `orderNumber`, `otherAddressId`, `otherStoreId`, `otherUserId`, `productId`, `providerOrderId`, `providerPaymentId`, `reservationId`, `revokedAdminToken`, `runCase`, `serviceDate`, `startTime`, `storeId`, `storeSearchQuery`, `storeSlug`, `userEmail`, `userId`, `variantId`, `webhookRequestId`, `webhookSignature`

## Archivos de esta fase

- Modificado: postman/jaldishop.postman_collection.json.
- Nuevos: postman/tools/preparar_equipo.py y validar_equipo.cjs; esta carpeta con guía, cobertura, comparación Bruno e índice JSON.
- Respaldo privado externo indicado arriba. Copias Bruno y environments conservados.
