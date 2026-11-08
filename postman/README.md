# JaldiShop API — Postman local

Colección Postman v2.1 para Spring Boot 4.1.1. Incluye **594 escenarios** sobre
**97 rutas HTTP**: 100 requests originales, sus variantes negativas y 11 regresiones
explícitas de las correcciones del backend. Los estados esperados presuponen fixtures
válidos y aislados; esta colección no prepara ni restaura datos automáticamente.

## Inicio

1. Iniciar el backend con [la guía local](../backend/README.md).
2. Importar [la colección](jaldishop.postman_collection.json) y
   [el entorno local](jaldishop.local.env.json) en Postman.
   `jaldishop.postman_environment.json` es un alias idéntico; importar solo uno.
3. Completar únicamente en el entorno privado los valores necesarios para el caso.
   No guardar tokens, contraseñas ni datos reales en los JSON compartidos.
4. Usar una API en localhost con una base **local desechable dedicada a pruebas**;
   no apuntar a la base personal de trabajo ni ejecutar toda la colección indiscriminadamente.
5. Asignar `runCase` al ID exacto de la petición, por ejemplo `R004`, y
   `disposableFixturesReady` a la cadena `true` solo después de preparar sus datos.
   Ejecutar ese request individualmente. Los flags comienzan vacíos y bloquean el envío.
6. Restaurar los fixtures entre casos que escriben. Un request válido repetido puede
   dejar de ser válido por duplicados, límites o cambios de estado.

La protección exige URL local, variables resueltas en URL/body/auth/headers y el ID
seleccionado. Se desactivan las redirecciones. Los scripts no envían requests secundarios.
Los logins capturan los tokens locales por rol; las demás capturas solo se conservan
para los requests originales exitosos. Los casos negativos/regresiones no sobrescriben
IDs o tokens. Revisar las capturas antes de encadenar un flujo.

## Variables

Todos los valores compartidos están vacíos salvo `baseUrl`. Se incluyen las mismas
variables en ambos entornos y en la colección; Postman resuelve primero los valores
privados del entorno seleccionado. No es necesario completar variables de otros casos.
Las variables `fixture*` son datos sintéticos elegidos por cada integrante, no datos
personales. Usar emails del dominio reservado `example.test` e imágenes de ejemplo
sin servicios privados. Los cuerpos negativos conservan cadenas inválidas sintéticas
y tamaños límite; `invalidShortPassword` debe contener menos de ocho caracteres.
Las coordenadas válidas de ejemplo son sintéticas (0,0); ajustar los fixtures locales
si el caso depende de distancias. Se conservan los valores fuera de rango de los negativos.

| Variable | Uso |
|---|---|
| `addressId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `adminEmail` | Email de la cuenta sintética local del rol indicado. |
| `adminPassword` | Contraseña de una cuenta sintética local; completar solo en el entorno privado. |
| `adminToken` | JWT local privado; completar o capturar mediante el login del rol correspondiente. |
| `attemptId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `authToken` | JWT local privado; completar o capturar mediante el login del rol correspondiente. |
| `baseUrl` | API local, por defecto `http://127.0.0.1:8080`. |
| `capacityConfigId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `capacityExceptionId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `categoryId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `checkoutReferenceId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `customerEmail` | Email de la cuenta sintética local del rol indicado. |
| `customerPassword` | Contraseña de una cuenta sintética local; completar solo en el entorno privado. |
| `customerToken` | JWT local privado; completar o capturar mediante el login del rol correspondiente. |
| `disposableFixturesReady` | Cadena `true` cuando los fixtures locales desechables estén preparados. |
| `duplicateStreetAddress` | Calle de una dirección ya guardada por el mismo usuario; R001/R002. |
| `duplicateUbigeoCode` | UBIGEO de esa dirección duplicada; R001/R002. |
| `endTime` | Fin de franja del fixture, HH:mm:ss. |
| `existingMerchantEmail` | Email sintético ya registrado para el escenario de duplicado. |
| `existingStreetAddress` | Calle actual de addressId; R003 conserva esa ubicación. |
| `existingUbigeoCode` | UBIGEO actual de addressId; R003. |
| `financialFakeReady` | Cadena `true` solo con un gateway fake local verificado; no habilita Mercado Pago por sí sola. |
| `fixtureAddress` | Valor sintético válido para el campo Address. |
| `fixtureAddressReference` | Valor sintético válido para el campo AddressReference. |
| `fixtureCustomerName` | Valor sintético válido para el campo CustomerName. |
| `fixtureCustomerPhone` | Valor sintético válido para el campo CustomerPhone. |
| `fixtureDeliveryAddress` | Valor sintético válido para el campo DeliveryAddress. |
| `fixtureDeliveryReference` | Valor sintético válido para el campo DeliveryReference. |
| `fixtureEmail` | Email sintético válido, por ejemplo con dominio example.test. |
| `fixtureFirstName` | Valor sintético válido para el campo FirstName. |
| `fixtureImageUrl` | URL pública de ejemplo sin credenciales; no apunta a Cloudinary real. |
| `fixtureLastName` | Valor sintético válido para el campo LastName. |
| `fixturePassword` | Contraseña de una cuenta sintética local; completar solo en el entorno privado. |
| `fixturePhone` | Valor sintético válido para el campo Phone. |
| `fixtureRecipientName` | Valor sintético válido para el campo RecipientName. |
| `fixtureRecipientPhone` | Valor sintético válido para el campo RecipientPhone. |
| `fixtureReference` | Valor sintético válido para el campo Reference. |
| `fixtureStreetAddress` | Valor sintético válido para el campo StreetAddress. |
| `invalidShortPassword` | Valor sintético corto para probar el rechazo de contraseña; completar privadamente. |
| `merchantEmail` | Email de la cuenta sintética local del rol indicado. |
| `merchantOnlyToken` | JWT local de un usuario con MERCHANT y sin CUSTOMER para negativos de autorización. |
| `merchantPassword` | Contraseña de una cuenta sintética local; completar solo en el entorno privado. |
| `merchantToken` | JWT local privado; completar o capturar mediante el login del rol correspondiente. |
| `missingAddressId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `newRegistrationEmail` | Email sintético aún no registrado para altas; cambiar/restaurar entre casos. |
| `notificationId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `orderId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `orderNumber` | Número público de pedido del fixture local. |
| `otherAddressId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `otherStoreId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `paymentId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `preferenceId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `productId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `providerOrderId` | ID sintético de orden del gateway fake; no usar identificadores reales. |
| `providerUserId` | Identificador sintético del usuario del gateway fake; no usar IDs reales. |
| `providerPaymentId` | ID sintético de pago del gateway fake. |
| `reservationId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `revokedAdminToken` | JWT local de un administrador revocado para el caso específico. |
| `runCase` | ID exacto del caso seleccionado; vacío impide enviarlo. |
| `serviceDate` | Fecha futura válida para los fixtures de capacidad, formato YYYY-MM-DD. |
| `startTime` | Inicio de franja del fixture, HH:mm:ss. |
| `storeId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `storeSearchQuery` | Consulta para búsqueda pública; el conjunto depende de los fixtures. |
| `storeSlug` | Slug de una tienda del fixture local. |
| `userId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `variantId` | Identificador del fixture local correspondiente; respetar propietario y relaciones. |
| `webhookRequestId` | Identificador sintético de la petición webhook local. |
| `webhookSignature` | Firma válida solo para el fixture/fake local; nunca un secreto del proveedor. |

## Regresiones explícitas

- **R001:** POST duplicado devuelve 409 / ADDRESS_ALREADY_EXISTS. El usuario debe tener
  menos de cinco direcciones y una con la calle/UBIGEO indicados.
- **R002:** PUT hacia otra dirección del mismo usuario devuelve 409. `addressId` debe
  señalar una dirección distinta de la pareja duplicateStreetAddress/duplicateUbigeoCode.
- **R003:** PUT que conserva la ubicación propia devuelve 200. Usar la ubicación actual
  de addressId en existingStreetAddress/existingUbigeoCode.
- **R004–R006:** búsquedas `pasteleria`, `%` y `_`. Preparar tiendas activas cuyos nombres
  o slugs contengan respectivamente esos textos (incluida una con tilde en Pastelería).
  Las pruebas exigen resultados no vacíos y comprueban coincidencias por nombre/slug.
- **R007–R008:** límites 0 y 51 devuelven 400 / VALIDATION_ERROR.
- **R009:** lista de clientes del comerciante con fechas ISO y lastOrderAt nulo permitido.
  El merchant debe tener tienda y al menos un cliente asociado en el fixture.
- **R010:** PATCH no soportado en addresses devuelve 405 y cabecera Allow.
- **R011:** una subruta inexistente devuelve 404 / RESOURCE_NOT_FOUND con JWT válido.

## Pagos e imágenes

La configuración local del backend no habilita pagos ni subida/borrado de imágenes.
No completar credenciales de Mercado Pago, Cloudinary u otros servicios para ejecutar
esta colección. Los casos financieros se mantienen bloqueados salvo que se haya
preparado un gateway fake local de forma independiente. Este repositorio no añade
infraestructura de fake ni un procedimiento QA avanzado. Para normalización interna
de Orders y concurrencia, usar las pruebas backend existentes.

## Validación y preparación de ejemplos

Desde la raíz del repositorio, sin servidores ni conexión de red:

```text
node postman/tools/validar_equipo.cjs
```

Comprueba JSON, variables/documentación, sintaxis JavaScript, protecciones de envío
y coincidencia de método/ruta con los controladores Java. **No ejecuta HTTP** ni
certifica el resultado de los 594 escenarios con una base de datos real.

El preparador es independiente de auditorías, respaldos y rutas personales:

```text
python3 postman/tools/preparar_equipo.py
```

En Windows puede usarse `py -3` en lugar de `python3`. El comando sanea la colección
incluida y reconstruye los dos entornos vacíos. No escribe en docs ni accede a servicios.
Para procesar otra exportación sin sobrescribir los archivos actuales:

```text
python3 postman/tools/preparar_equipo.py ruta/exportacion.json --output-dir ruta/salida
```

La exportación de entrada debe conservar los IDs de los requests originales utilizados
por las regresiones. El preparador no inventa contratos para una API distinta.

Para Bruno se conservan las [colecciones nativas](../backend/qa/README.md).
No se garantiza equivalencia automática de estos scripts Postman tras importarlos.
No se incluyen respaldos ni entornos remotos.
