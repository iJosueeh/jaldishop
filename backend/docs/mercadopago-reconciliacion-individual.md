# Conciliación individual de Mercado Pago Orders API

Implementación del backend en `feature/mercadopago-orders-api`, 2026-11-07. No se ejecutó la conciliación real del intento `cfede596-7383-4747-8fc6-4b289d454f7a`.

## Contrato y reutilización

`POST /api/v1/admin/payments/attempts/{attemptId}/reconcile-order`, protegido por `ADMIN` en el controlador y por la regla existente `/api/v1/admin/**`. La operación recibe exclusivamente un UUID de intento. No enlaza estados, importes ni moneda de un cuerpo enviado por el cliente.

El endpoint y el lote usan `PaymentReconciliationService.reconcileOrderAttempt`. La operación manual funciona con ambos flags de conciliación desactivados; no cambia ninguno de ellos. El endpoint de investigación de creación remota sigue siendo una acción distinta.

Se conserva un único flujo financiero: `PaymentOrderReconciliationProcessor` consulta la orden y delega en `PaymentWebhookPersistenceService.applyPaymentQueryResult`, igual que el webhook. Las referencias y el proveedor se validan bajo bloqueo; importe y moneda se contrastan allí con el pago local. `MercadoPagoResourceValidator` concentra identidad de orden, cardinalidad de transacciones, identidad de transacción y cuenta/entorno para ambas entradas. `collector_id` y `live_mode` mantienen el contrato existente: se exigen cuando se configuran `mercadopago.expected-collector-id` y `mercadopago.expected-live-mode`.

La clasificación financiera se aplica solo en persistencia. Se eliminaron las dos copias de la selección por acreditación porque las órdenes ambiguas, con varias transacciones, se rechazan antes de seleccionar la única transacción.

## Resultado financiero y comercial

La respuesta incluye `attemptId`, `paymentId`, `providerOrderId`, `status`, `detail`, `financialStatus` y `commercialStatus`. El estado financiero se relee de PostgreSQL al terminar; no se deduce del mensaje remoto.

| Resultado comercial | Interpretación |
| --- | --- |
| `CONFIRMED` | Existe un pedido comercial confirmado por el flujo existente. |
| `INCIDENCE_RECORDED` | Existe evidencia comercial persistida; la aprobación financiera no equivale a una compra completada. |
| `PENDING` | Pago aprobado cuya confirmación comercial requiere reintento o revisión, incluso si falla el registro de la incidencia. |
| `NOT_ATTEMPTED` | No se inició confirmación comercial para el estado financiero observado. |

Se retiró `SUCCESS_ALREADY_APPROVED`: una aprobación previa ya no basta para informar éxito comercial. Una repetición reutiliza la confirmación idempotente y consulta las incidencias existentes. Una transacción pendiente conserva su estado y se informa como abierta, en lugar de cancelada/expirada. Los fallos técnicos no se presentan como confirmación completa.

## Incidencias y reserva vencida

Orders reutiliza `PaymentReconciliationProcessor`, `PaymentConfirmationRunner` y `PaymentIncidenceRecorder`. El webhook también delega sus conflictos al mismo registrador. La reserva vencida conserva pago e intento en `APPROVED`, no crea un pedido y registra el código comercial correspondiente junto con `refundStatus=PENDING` en la misma transacción independiente. Se solicita un reembolso local; no se ejecuta un reembolso remoto.

El bloqueo de `Payment` serializa duplicados. Una repetición no crea otra incidencia terminal ni otra solicitud de reembolso. La consulta de la última incidencia incluye las terminales; no depende solo de las activas. Se usa la convención existente `RESOLVED` para cerrar los reintentos de confirmación de una incidencia comercial terminal: esto no significa que se haya recuperado la compra ni ejecutado el reembolso.

Si guardar la incidencia o confirmar su transacción falla, se propaga el error. No se captura una violación SQL para seguir trabajando en la misma transacción fallida. La respuesta no afirma que se registró una incidencia cuando no existe evidencia persistida. Un reembolso previo sin dicha evidencia produce resultado comercial pendiente de revisión.

## Leases y concurrencia

La lectura de enrutamiento se realiza antes de `REQUIRES_NEW`; su contexto vuelve a cargar el intento bajo bloqueo. Tanto el claim como la consolidación siguen el orden `Payment → PaymentAttempt`. El tiempo de elegibilidad y propiedad se obtiene después de adquirir los bloqueos, sin confiar en la hora anterior a HTTP ni en una hora pasada por el llamador.

HTTP se realiza fuera de la transacción financiera. La limpieza se intenta en `finally` y solo puede liberar el token de ese worker, aunque haya expirado. Nunca libera el token de un sucesor. Si PostgreSQL impide la limpieza, el lease conserva su recuperación por vencimiento; el endpoint no devuelve un éxito ficticio y el lote conserva el aislamiento de fallos entre candidatos.

## Pruebas y límites de verificación

**162 pruebas aprobadas en 16 clases, 0 fallos, 0 errores, 0 omitidas.** Maven offline finalizó con `BUILD SUCCESS`. Se compiló el backend y todas las clases de prueba.

Logs locales: [unitarias](/tmp/jaldishop-reconciliation-unit-final.log) e [integración bloqueada](/tmp/jaldishop-reconciliation-integration-final.log). Los informes JUnit están en `backend/target/surefire-reports/`.

| Escenario | Evidencia obtenida |
| --- | --- |
| Acreditación y confirmación comercial | Pruebas unitarias de webhook, persistencia, confirmación y dominio aprobadas; integración individual preparada. |
| Orden abierta / transacción pendiente | Pruebas unitarias aprobadas; escenarios HTTP/PostgreSQL preparados. |
| Repeticiones y aprobación previa sin pedido | Pruebas unitarias aprobadas; integración de duplicado sin modificar otro intento preparada. |
| Identidad cruzada, orden ambigua, importe/moneda incorrectos | Pruebas unitarias aprobadas; integración de orden ambigua existente conservada. |
| Cuenta y entorno incorrectos o ausentes cuando se esperan | Pruebas unitarias del validador compartido y del webhook aprobadas; verificada su utilización por conciliación. |
| Reserva vencida e incidencia idempotente | Pruebas unitarias aprobadas; integración verifica fila, código, ausencia de pedido y solicitud local de reembolso. |
| Error de almacenamiento de incidencia | Pruebas unitarias aprobadas; integración preparada para verificar rollback del reembolso y recuperación posterior sin otra consulta remota. |
| Lease obsoleto, token sucesor, liberación por error | Pruebas unitarias aprobadas; integración preparada para dos workers simultáneos, recuperación y rechazo de token obsoleto. |
| Timeout / HTTP 500 | Pruebas unitarias de orquestación aprobadas; integración con servidor local simulado preparada. |
| Permisos y entrada | ADMIN permitido, CUSTOMER/MERCHANT/no autenticado rechazados; UUID inválido devuelve 400; solo se delega el UUID, ignorando estados del cliente. |
| Aislamiento de base de datos | URL con nombre exacto y loopback validada antes del contexto; `current_database()` se comprueba antes de cualquier limpieza destructiva. Las pruebas negativas rechazan sandbox, otros nombres y destinos remotos. |

La integración seleccionó 16 métodos de conciliación y excluyó la regresión de reembolsos. Compiló, pero falló en `startMockHttpServer` con `java.net.SocketException: Operation not permitted`, antes de abrir PostgreSQL y antes de ejecutar los fixtures. Es un error de inicialización, no una verificación aprobada de esos escenarios. No hay prueba ejecutada del comportamiento transaccional, los bloqueos reales ni el arranque completo de Spring con estos cambios. No se calculó cobertura porcentual de código.

Los fallos unitarios intermedios por fixtures del registrador, mensajes de validación y construcción de un DTO inválido quedaron corregidos. `git diff --check -- backend` no encontró problemas de espacios.

## Reproducción aislada

Desde `backend/`, con Java 21 y las dependencias Maven ya disponibles. Mockito se carga como agente para evitar el autoattach restringido del sandbox:

```bash
./mvnw -o \
  -DargLine="-javaagent:${HOME}/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
  -Dtest=OrderReferenceReadTest,ManualOrderReconciliationTest,MercadoPagoResourceValidatorTest,PaymentReconciliationServiceTest,PaymentReconciliationProcessorTest,PaymentReconciliationSchedulerTest,PaymentIncidenceRecorderTest,PaymentConfirmationRunnerTest,AdminOrderInvestigationSecurityTest,ProcessMercadoPagoWebhookServiceTest,OrdersWebhookCorrelationTest,TestPostgresConfigTest,ConfirmPurchaseServiceTest,CommitCapacityReservationServiceTest,PaymentAttemptTest,PaymentIncidenceTest test
```

La integración requiere que `DB_USERNAME` y `DB_PASSWORD` correspondan exclusivamente al PostgreSQL local de integración, y un entorno que permita sockets locales. No usar el archivo de credenciales del sandbox ni sustituir la URL por `jaldishop_test`.

```bash
DB_URL=jdbc:postgresql://127.0.0.1:5433/jaldishop_integration_test \
APP_PAYMENT_RECONCILIATION_ENABLED=false \
APP_PAYMENT_RECONCILIATION_ORDERS_ENABLED=false \
PAYMENTS_MERCADOPAGO_REFUND_SCHEDULER_ENABLED=false \
./mvnw -o \
  -DargLine="-javaagent:${HOME}/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
  '-Dtest=OrderReconciliationPostgresIntegrationTest#manualApprovalAndDuplicateAffectOnlyTheSelectedAttemptWithSchedulersDisabled+failedIncidenceTransactionRollsBackRefundAndCanBeRetriedWithoutAnotherRemoteRead+shouldRecoverAccreditedOrderWithoutWebhookAndConfirmPurchase+shouldKeepOpenCreatedOrderWithoutTransactions+shouldMarkAttemptRejectedOnCancelledOrderWithoutTransactions+shouldMarkAttemptRejectedOnExpiredOrderWithoutTransactions+shouldMarkAttemptRejectedOnCanceledOrderWithoutTransactions+shouldNotDegradeAlreadyApprovedPaymentWhenOrderCanceledRemote+shouldRecordIncidenceWhenAccreditedAfterCapacityReservationExpired+shouldAvoidDuplicateConfirmationWhenWebhookConcurrentlyApproves+shouldAllowOnlyOneWorkerToClaimAttemptConcurrently+shouldAllowNewWorkerToClaimExpiredLease+shouldRejectConsolidationFromStaleWorkerWithInvalidToken+shouldPreserveStateAndReleaseLeaseOnTimeoutOrHttp500+shouldRejectAmbiguousOrderWhenMultipleTransactionsReturned+shouldRecoverAfterWorkerCrashAndLeaseExpiration' test
```

El contexto de integración desactiva la importación de `.env`, usa JWT y token sintéticos, fuerza JDBC y Flyway a la base autorizada y apunta Mercado Pago a HTTP simulado en loopback. El scheduler global y el de reembolsos permanecen desactivados; los tests llaman directamente a servicios de prueba. La bandera del lote puede variarse dentro del objeto de prueba sin activar ningún scheduler.

## Archivos afectados

Sin migraciones, dependencias nuevas ni modificaciones de `run-safe-sandbox.sh`. Cambios exclusivamente bajo `backend/`:

- [`docs/mercadopago-reconciliacion-individual.md`](../docs/mercadopago-reconciliacion-individual.md)
- [`src/main/java/com/jaldishop/backend/payment/application/gateway/MercadoPagoResourceValidator.java`](../src/main/java/com/jaldishop/backend/payment/application/gateway/MercadoPagoResourceValidator.java)
- [`src/main/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationBatchSummary.java`](../src/main/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationBatchSummary.java)
- [`src/main/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationCandidateResult.java`](../src/main/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationCandidateResult.java)
- [`src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentIncidenceRecorder.java`](../src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentIncidenceRecorder.java)
- [`src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentOrderReconciliationProcessor.java`](../src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentOrderReconciliationProcessor.java)
- [`src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationProcessor.java`](../src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationProcessor.java)
- [`src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationService.java`](../src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationService.java)
- [`src/main/java/com/jaldishop/backend/payment/application/webhook/PaymentWebhookPersistenceService.java`](../src/main/java/com/jaldishop/backend/payment/application/webhook/PaymentWebhookPersistenceService.java)
- [`src/main/java/com/jaldishop/backend/payment/application/webhook/ProcessMercadoPagoWebhookService.java`](../src/main/java/com/jaldishop/backend/payment/application/webhook/ProcessMercadoPagoWebhookService.java)
- [`src/main/java/com/jaldishop/backend/payment/domain/repository/PaymentIncidenceRepository.java`](../src/main/java/com/jaldishop/backend/payment/domain/repository/PaymentIncidenceRepository.java)
- [`src/main/java/com/jaldishop/backend/payment/infrastructure/persistence/adapter/JpaPaymentIncidenceRepositoryAdapter.java`](../src/main/java/com/jaldishop/backend/payment/infrastructure/persistence/adapter/JpaPaymentIncidenceRepositoryAdapter.java)
- [`src/main/java/com/jaldishop/backend/payment/infrastructure/persistence/repository/PaymentIncidenceJpaRepository.java`](../src/main/java/com/jaldishop/backend/payment/infrastructure/persistence/repository/PaymentIncidenceJpaRepository.java)
- [`src/main/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationController.java`](../src/main/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationController.java)
- [`src/test/java/com/jaldishop/backend/payment/application/gateway/MercadoPagoResourceValidatorTest.java`](../src/test/java/com/jaldishop/backend/payment/application/gateway/MercadoPagoResourceValidatorTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/reconciliation/ManualOrderReconciliationTest.java`](../src/test/java/com/jaldishop/backend/payment/application/reconciliation/ManualOrderReconciliationTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationPostgresIntegrationTest.java`](../src/test/java/com/jaldishop/backend/payment/application/reconciliation/OrderReconciliationPostgresIntegrationTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/reconciliation/OrderReferenceReadTest.java`](../src/test/java/com/jaldishop/backend/payment/application/reconciliation/OrderReferenceReadTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentIncidenceRecorderTest.java`](../src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentIncidenceRecorderTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationProcessorTest.java`](../src/test/java/com/jaldishop/backend/payment/application/reconciliation/PaymentReconciliationProcessorTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/webhook/OrdersWebhookCorrelationTest.java`](../src/test/java/com/jaldishop/backend/payment/application/webhook/OrdersWebhookCorrelationTest.java)
- [`src/test/java/com/jaldishop/backend/payment/application/webhook/ProcessMercadoPagoWebhookServiceTest.java`](../src/test/java/com/jaldishop/backend/payment/application/webhook/ProcessMercadoPagoWebhookServiceTest.java)
- [`src/test/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationSecurityTest.java`](../src/test/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationSecurityTest.java)
- [`src/test/java/com/jaldishop/backend/testutil/TestPostgresConfig.java`](../src/test/java/com/jaldishop/backend/testutil/TestPostgresConfig.java)
- [`src/test/java/com/jaldishop/backend/testutil/TestPostgresConfigTest.java`](../src/test/java/com/jaldishop/backend/testutil/TestPostgresConfigTest.java)

## Restricciones preservadas

No se modificaron frontend, Render, Neon, configuración del sandbox ni cambios ajenos sin commit. No se inició el backend real, no se crearon pagos, no se enviaron webhooks manuales, no se ejecutaron conciliaciones reales ni reembolsos. No se mostraron credenciales ni se alteraron estados de `jaldishop_test`. La operación real del intento indicado sigue pendiente de autorización separada.
