# Mercado Pago Orders — preparación 2C.2A

Base revisada: `0171504`, rama `feature/mercadopago-orders-api`.
Esta entrega no activa emisión pa1_, no cambia código productivo ni requiere migración.

## Flujo y punto de activación

- `CreatePaymentPreferenceService.buildGatewayOrderCommand`: el primer argumento de
  `new CreateOrderCommand` es `prep.payment().getId().toString()`. Este es el punto
  único de construcción de la referencia Orders. Preferences tiene su constructor separado.
- `PaymentAttempt.create` asigna `UUID.randomUUID()` en el dominio. La preparación
  transaccional `PaymentPreferencePreparationService.prepareOrGetExistingPayment`
  guarda el intento y devuelve `savedAttempt` tanto para Payment nuevo como para
  nuevo intento de un Payment existente. El UUID no depende de un ID generado por la BD.
  Al retornar el proxy transaccional, el intento ya está confirmado antes del gateway.
- `CreatePaymentPreferenceService` pasa `prep.attempt().getId()` al gateway.
  `MercadoPagoRestClientAdapter.toOrderRequest` copia la referencia del comando.
  `createOrder` serializa una vez, llama `CheckoutCreationRequestStore.saveBeforeSend`
  y envía los bytes retornados, no reconstruye el cuerpo.
- El store usa `REQUIRES_NEW`: inserta snapshot con FK compuesta
  `(attemptId, mode, idempotencyKey)`, marca UNKNOWN y relee el cuerpo antes de
  confirmar. Un conflicto aborta antes del HTTP. No habilita un segundo POST.

## Inmutabilidad y límites

V24 impide UPDATE del snapshot y permite un solo registro por intento. Guarda texto,
no JSON normalizado; el adaptador envía los bytes UTF-8 releídos. No hay constraint
que interprete `external_reference`: la correspondencia semántica depende del emisor
(y de la validación de lectura). No es una garantía contra SQL arbitrario, DELETE
ni un comando construido incorrectamente por otro llamador del gateway.

V25 enlaza auditoría de creación mediante `payment_attempt_id`; registra estados y
códigos, no una copia de la referencia. Investigación manual exige coincidencia exacta
entre snapshot y respuesta y valida Payment o intento según el prefijo. Los eventos
son inmutables frente a UPDATE. El log de creación usa el comando, equivalente al
snapshot en el flujo actual; los logs no sustituyen evidencia durable.

## Consumidores internos revisados

- `ExternalPaymentReference`: UUID histórico o `pa1_<UUID>` estricto; no resuelve propietario.
- `ProcessMercadoPagoWebhookService`: procesa notificaciones payment/order, resuelve
  Payment por el intento explícito y conserva la referencia al persistir.
- `PaymentWebhookPersistenceService`: bloquea Payment antes del intento; cruza
  referencia, propietario de pago remoto y providerOrderId. No recurre a otro intento
  cuando la referencia explícita contradice los identificadores.
- `PaymentOrderReconciliationProcessor`: valida bajo bloqueos la referencia contra
  el intento reclamado, incluso para cancelaciones y Payment ya aprobado.
- `ManualOrderInvestigationService`: admite ambos formatos y exige igualdad con snapshot;
  solo consulta cuando existe providerOrderId inequívoco.
- DTOs Orders, serializador, gateway y store transportan String sin parsear UUID.
  Preferences sigue emitiendo Payment UUID y no se modifica. La búsqueda en
  `backend/src/main` no encontró otros usos de external_reference fuera de payment.
- Sobrecarga histórica `applyOrderCancellation` sin referencia construye Payment UUID;
  los caminos Orders con respuesta remota usan la sobrecarga que recibe referencia.

## Pruebas

Se amplía transporte HTTP simulado para ambos formatos: referencia exacta y envío
exacto de los bytes retornados por el store (incluido espacio agregado por el doble,
para distinguir el cuerpo persistido del objeto original). Se conserva cobertura UUID.
Webhook cubre selección de cualquiera de dos intentos del mismo Payment; conciliación
rechaza referencia del hermano aun con providerOrderId válido. Se conservan las
pruebas de 2C.1 de referencias cruzadas, pagos remotos contradictorios y compatibilidad.

En `ManualOrderInvestigationPostgresIntegrationTest` se añade carrera de dos conexiones
por el mismo snapshot pa1_: un ganador, duplicado rechazado, persistencia independiente
para otro intento del mismo Payment, UNKNOWN auditado una vez y rechazo de UPDATE.
También se parametriza webhook duplicado/tardío para ambos formatos. Estas pruebas
compilan con el resto de tests, pero requieren validación PostgreSQL externa.

## Validación fuera del sandbox (Fedora)

Usar Java 21 y PostgreSQL local ya aprovisionado, con BD de pruebas desechable y rol
con permisos para Flyway. No usar `.env` productivo ni Neon. Los tests verifican URL
loopback con puerto explícito y nombre exacto. Los comandos siguientes no crean roles
ni bases; configurar sus credenciales antes de ejecutarlos.

```bash
cd /ruta/jaldishop/backend
export DB_URL='jdbc:postgresql://127.0.0.1:5432/jaldishop_integration_test'
export DB_USERNAME='jaldishop_test'
read -rsp 'Password de la BD local: ' DB_PASSWORD
export DB_PASSWORD
./mvnw -Dtest=ManualOrderInvestigationPostgresIntegrationTest,OrderReconciliationPostgresIntegrationTest,PaymentReconciliationPostgresIntegrationTest test
unset DB_PASSWORD
```

Pruebas sin BD ni HTTP real:

```bash
./mvnw -o -Dtest=ExternalPaymentReferenceTest,CheckoutRequestRetryTest,CheckoutCreationRequestStoreTest,MercadoPagoRestClientAdapterTest,OrdersWebhookCorrelationTest,OrderReferenceReadTest,ManualOrderInvestigationServiceTest test
```

## Dependencias externas no verificadas

No se consultó Mercado Pago real. Deben confirmar responsables externos que Orders
acepta y devuelve sin transformación `pa1_` más UUID (40 caracteres), y que sus
herramientas de soporte, conciliación, exportación, BI, alertas y consumidores de
notificaciones no interpretan external_reference como Payment UUID. No se inspeccionaron
sistemas externos, frontend ni Postman; no se presume su compatibilidad. Las respuestas
simuladas prueban comportamiento local, no el contrato efectivo del proveedor.

## Cambio mínimo y autorización de 2C.2B

1. Aprobar reportes verdes de las pruebas focalizadas y PostgreSQL anteriores,
   incluyendo carrera de snapshots y pruebas existentes de webhook/conciliación.
2. Resolver explícitamente las dependencias externas anteriores; cualquier restricción
   de longitud/formato o consumidor UUID es bloqueo de activación.
3. En un cambio separado, sustituir SOLO el primer argumento del constructor Orders
   por `"pa1_" + prep.attempt().getId()`; actualizar la aserción de emisión Orders,
   conservando Preferences y fixtures UUID históricos. Probar dos intentos del mismo
   Payment desde preparación hasta snapshot/HTTP con referencias distintas y UUID
   exacto del intento persistido. No reconstruir snapshots existentes.
4. Autorizar explícitamente 2C.2B después de revisar ese diff y su prueba de emisión.
   Revertir el emisor para intentos futuros debe conservar lectura dual; UNKNOWN y
   snapshots existentes nunca se reenvían. No recuperar sin providerOrderId, no POST
   automático, no cambios financieros ni migraciones para habilitar la referencia.

## Resultado local de esta entrega

93 pruebas focalizadas aprobadas, sin fallos, errores ni omisiones. Maven compiló
los 219 archivos de pruebas, incluida la ampliación PostgreSQL; no ejecutó PostgreSQL.
El primer intento falló al inicializar Mockito por autoanexado restringido. La ejecución
verde usó el mismo comando focalizado con este argumento adicional (ajustar la ruta
según la instalación local):

```bash
-DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
```

No se ejecutaron Git de escritura, migraciones, frontend, Postman ni servicios reales.
Los cambios preexistentes del usuario fuera del alcance permanecen intactos.

## Cierre 2C.2A

Validación externa comunicada por el usuario: 93 pruebas focalizadas y 38 pruebas
PostgreSQL aprobadas, cero fallos, errores u omisiones y BUILD SUCCESS. No se
repitieron pruebas durante el cierre. Se autoriza el commit de pruebas y documentación;
la emisión permanece con paymentId. Las dependencias externas para 2C.2B siguen pendientes.
