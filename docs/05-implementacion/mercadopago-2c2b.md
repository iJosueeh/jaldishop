# Mercado Pago Orders — emisión 2C.2B

Rama: feature/mercadopago-orders-api. Base: 1320dab.

## Cambio y garantías

Único cambio productivo, en CreatePaymentPreferenceService.buildGatewayOrderCommand:

```diff
-                prep.payment().getId().toString(),
+                "pa1_" + prep.attempt().getId(),
```

Usa el UUID definitivo de savedAttempt devuelto por la preparación transaccional.
Solo afecta nuevas creaciones Orders. Preferences conserva Payment UUID.
No reconstruye snapshots existentes ni modifica las reglas de reenvío, estados o
idempotencia. El adaptador conserva la serialización única y envía los bytes releídos
del snapshot confirmado por saveBeforeSend. V24 mantiene la inmutabilidad.

## Pruebas

CheckoutRequestRetryTest verifica referencia por intento y estabilidad de referencia
y clave del mismo intento, además de preservar emisión Preferences.
MercadoPagoOrdersApiEndToEndIntegrationTest añade dos intentos reales persistidos del
mismo Payment: creación del primero y rechazo financiero persistido mediante
markRejected, seguido de creación explícita del segundo. El fixture original confundía
REJECTED_CONFIRMED (evidencia remota) con REJECTED (estado financiero); el error
de validación del POST produce ERROR y no habilita ese segundo intento. La corrección
solo modifica la prueba y conserva las reglas productivas. Comprueba UUID y clave distintos, referencia exacta en snapshot y HTTP,
primer snapshot intacto y reutilización del resultado sin tercer POST.
No añade reintentos automáticos.

Ejecución local focalizada: 114 pruebas aprobadas, cero fallos, errores u omisiones,
BUILD SUCCESS. Validación final comunicada por el usuario desde Fedora: 59 pruebas
PostgreSQL aprobadas, cero fallos, errores u omisiones y BUILD SUCCESS, incluida la
prueba corregida de dos intentos. No se repitieron pruebas durante el cierre.

Comando focalizado (Java 21; desde backend):

```bash
./mvnw -o   -Dtest=ExternalPaymentReferenceTest,CheckoutRequestRetryTest,CreatePaymentPreferenceServiceTest,CheckoutCreationRequestStoreTest,MercadoPagoRestClientAdapterTest,OrdersWebhookCorrelationTest,OrderReferenceReadTest,ManualOrderInvestigationServiceTest   -DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test
```

## PostgreSQL en Fedora — validado

Usar PostgreSQL local ya aprovisionado y BD desechable dedicada, Java 21 y rol con
permisos Flyway. Las suites limpian sus datos. No cargar .env de producción.
No usar Neon. Estos comandos no crean roles ni bases:

```bash
cd /ruta/jaldishop/backend
export DB_URL='jdbc:postgresql://127.0.0.1:5432/jaldishop_integration_test'
export PHASE2A_DB_URL='jdbc:postgresql://127.0.0.1:5432/jaldishop_fase2a_test'
export DB_USERNAME='jaldishop_test'
read -rsp 'Password de la BD local: ' DB_PASSWORD
export DB_PASSWORD
./mvnw -o   -Dtest=MercadoPagoOrdersApiEndToEndIntegrationTest,ManualOrderInvestigationPostgresIntegrationTest,OrderReconciliationPostgresIntegrationTest,PaymentReconciliationPostgresIntegrationTest   -DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test
unset DB_PASSWORD
```

Incluye creación, duplicados, concurrencia, snapshots, fallos de commit, webhook
duplicado/tardío, conciliación y contradicciones de referencia.

## Consumidores internos

Búsqueda focalizada de external_reference/externalReference y conversiones UUID en
backend/src/main: no aparecen consumidores fuera de payment.
ExternalPaymentReference mantiene lectura dual estricta.
ProcessMercadoPagoWebhookService resuelve Payment mediante el intento para pa1_.
PaymentWebhookPersistenceService cruza propietario, intento y providerOrderId.
PaymentOrderReconciliationProcessor usa validateReference para el intento reclamado,
incluidas cancelaciones. ManualOrderInvestigationService valida ambos formatos y
coincidencia literal con snapshot; sigue exigiendo providerOrderId.
DTOs, adaptador y store transportan String. La sobrecarga histórica de cancelación
construye Payment UUID; los caminos Orders remotos pasan su referencia explícita.
No se encontró incompatibilidad interna concreta que comprometa la emisión.

## Bloqueos para despliegue

- Confirmar con responsables de soporte, conciliación externa, exportaciones, BI,
  alertas y consumidores de notificaciones que admiten pa1_ y resuelven el Payment
  a través del intento. No se verificaron esos sistemas.
- Frontend y Postman quedan fuera del alcance solicitado; no se presume compatibilidad
  de sus consumidores externos.
- El formato de 40 caracteres cumple las restricciones oficiales informadas por el
  usuario; aceptación y devolución exacta del proveedor no se probaron contra Mercado
  Pago real. Su validación externa controlada queda pendiente antes de activar.

No hubo llamadas reales a Mercado Pago, migraciones nuevas, cambios financieros,
recuperación sin providerOrderId, push ni despliegue. Cierre autorizado con commit
feat(payment): emit attempt-scoped orders references.
La reversión del emisor para futuros intentos debe mantener lectura dual de los ya creados.
