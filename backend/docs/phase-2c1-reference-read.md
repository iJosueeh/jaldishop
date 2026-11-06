# Fase 2C.1 — lectura de external_reference

Base: 98bb62b, rama feature/mercadopago-orders-api. La emisión sigue usando
paymentId; no se cambia el cuerpo de creación ni se habilitan reintentos POST.
No requiere migraciones.

## Consumidores reales y preparación

| Consumidor | Preparación de lectura |
| --- | --- |
| ExternalPaymentReference | Parser estricto UUID histórico o pa1_UUID; rechaza espacios, prefijos/versiones desconocidos y UUID abreviados. |
| ProcessMercadoPagoWebhookService | Usa el parser; pa1_ hace una lectura sin bloqueo para obtener el Payment propietario. La persistencia revalida la pertenencia. Verifica identidad de la respuesta HTTP. |
| PaymentWebhookPersistenceService | pa1_ selecciona exclusivamente el intento indicado, de Mercado Pago y ORDERS. Comprueba propietario, providerOrderId y providerPaymentId disponibles; sin fallback a otro intento. UUID histórico conserva selección por identificadores remotos y política Preferences. |
| Cancelación de órdenes | Recibe la referencia remota y la valida contra el intento de la orden antes de atajos por estados terminales. La firma histórica delega con paymentId. |
| PaymentOrderReconciliationProcessor | Valida referencia contra el intento del claim, pertenencia, orden consultada y número de transacciones antes de cambios financieros, incluso en cancelaciones/órdenes abiertas. Consolidación bloquea Payment antes de PaymentAttempt. |
| ManualOrderInvestigationService | Snapshot y respuesta remota deben coincidir literalmente; el parser exige paymentId propietario o pa1_ del intento investigado. Conserva controles de importe, moneda, cuenta, entorno y propietarios remotos. No produce transiciones financieras. |
| MercadoPagoRestClientAdapter, MpOrderResponse, MpPaymentResponse, DTO de consulta | Transportan la referencia sin reinterpretarla; no requieren cambios de lectura. Pruebas HTTP usan servidor simulado. |
| CheckoutCreationRequest / snapshot de solicitud | Conserva el cuerpo original. Investigación ahora puede leer ambos formatos; no se reescriben snapshots históricos. |
| Consultas locales de pago/intento | Se consultan por sus IDs locales, no convierten external_reference; no requieren cambios. |

Los HTTP permanecen antes de la transacción de persistencia/investigación/conciliación.
La resolución inicial de pa1_ no toma bloqueo de intento. La revalidación financiera
ocurre tras bloquear Payment y después PaymentAttempt. Una contradicción puede
registrar evidencia de correlación, pero no cambia estados financieros.

## Política conservadora

Una orden recibida debe coincidir con providerOrderId ya persistido. pa1_ no autoriza
adoptar automáticamente una orden desconocida ni recuperar un ID ausente. La
investigación manual mantiene su exigencia de ID remoto y snapshot inequívocos.
Un webhook de orden abierta sin pagos no aplica estados financieros.

## Validación local

Desde backend, sin cargar archivos .env:

```bash
./mvnw -o -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
  -Dtest=ExternalPaymentReferenceTest,OrdersWebhookCorrelationTest,ProcessMercadoPagoWebhookServiceTest,ManualOrderInvestigationServiceTest,OrderReferenceReadTest,PaymentReconciliationProcessorTest,MercadoPagoRestClientAdapterTest test
```

Resultado: 105 pruebas ejecutadas, cero fallos, errores u omisiones en las siete
clases del comando anterior. `git diff --check -- backend` también pasó.
La suite adicional SingleAttemptIsolatedReconciliationTest fue omitida por su
condición de entorno; no se cuenta como validación PostgreSQL.

Cobertura nueva: intento explícito válido, inexistente, malformado, de otro pago,
contradictorio con la orden, cancelación contradictoria con Payment aprobado,
snapshot pa1_ válido/cruzado y conciliación bajo bloqueos ordenados. La cobertura
histórica del parser, webhooks, investigación y adaptador se ejecuta también.

## PostgreSQL en Fedora (opcional; no ejecutado en esta fase)

Para una instancia local nueva, omitir initdb si ya fue inicializada:

```bash
sudo dnf install postgresql-server postgresql-contrib
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo -u postgres createuser --pwprompt jaldishop_test
sudo -u postgres createdb --owner=jaldishop_test jaldishop_integration_test
export DB_URL=jdbc:postgresql://127.0.0.1:5432/jaldishop_integration_test
export DB_USERNAME=jaldishop_test
read -rs -p 'Clave local: ' DB_PASSWORD
export DB_PASSWORD
./mvnw -o -Dtest=ManualOrderInvestigationPostgresIntegrationTest,OrderReconciliationPostgresIntegrationTest test
unset DB_PASSWORD
```

La autenticación TCP local debe permitir ese usuario. TestPostgresConfig acepta
exclusivamente loopback y base dedicada; no usar Neon ni cargar credenciales reales.
Estas suites comprueban persistencia/concurrencia. La corrección de fixtures
descrita a continuación añade referencias pa1_ a escenarios PostgreSQL existentes;
su ejecución queda a cargo del entorno Fedora, fuera del sandbox.

## Corrección de las seis incidencias PostgreSQL

Diagnóstico por inspección de fixtures y del flujo de producción; no se ejecutó
PostgreSQL desde el sandbox. No se identificó una regresión de producción en
estas seis incidencias y no se modificó código de producción en esta corrección.

En 98bb62b (2B.3), processOrderWebhook ya rechazaba más de una transacción.
2C.1 extendió la política conservadora a conciliación y exige correlación antes
de cancelaciones, órdenes abiertas y atajos por Payment APPROVED. Un estado
terminal local no convierte una respuesta remota inválida en evidencia válida.

| Prueba (nombre original) | Clasificación y causa concreta | Corrección |
| --- | --- | --- |
| shouldKeepOpenCreatedOrderWithoutTransactions | Fixture desactualizado: ORD-RECON-002 coincide con el intento, pero falta external_reference. | Añadir UUID del Payment; conservar PENDING/PROCESSING y liberación de lease. |
| shouldMarkAttemptRejectedOnCanceledOrderWithoutTransactions | Fixture desactualizado: ORD-RECON-004B coincide, pero falta external_reference. | Añadir pa1_ del intento exacto; conservar REJECTED, ORDER_CANCELED y Payment PENDING. |
| shouldMarkAttemptRejectedOnCancelledOrderWithoutTransactions | Fixture desactualizado: ORD-RECON-003 coincide, pero falta external_reference. | Añadir pa1_ del intento exacto; conservar REJECTED y liberación de lease. |
| shouldMarkAttemptRejectedOnExpiredOrderWithoutTransactions | Fixture desactualizado: ORD-RECON-004 coincide, pero falta external_reference; el rechazo de correlación explica que siga PROCESSING. | Añadir UUID del Payment; conservar REJECTED y ORDER_CANCELED. |
| shouldSelectAccreditedTransactionWhenMultipleTransactionsReturned | Comportamiento deliberadamente rechazado por seguridad: referencia histórica y ORD-RECON-011 válidos, pero pagos remotos 11111 y 22222 hacen ambigua la asociación. | Renombrar a shouldRejectAmbiguousOrderWhenMultipleTransactionsReturned; exigir TECHNICAL_FAILURE/IDENTIFIER_CONTRADICTION, Payment PENDING, intento PROCESSING sin providerPaymentId y sin pedido. |
| shouldAvoidDuplicateConfirmationWhenWebhookConcurrentlyApproves | Fixture desactualizado: tras adquirir claim de ORD-RECON-006 pasa remoteOrder=null; no representa una consulta remota exitosa ni permite validar identificadores antes del atajo. | Proporcionar ORD-RECON-006 con pa1_ del intento exacto; conservar SUCCESS_ALREADY_APPROVED y liberación de lease. |

Las cuatro órdenes sin transacciones carecen legítimamente de providerPaymentId;
no se inventa uno. Sus providerOrderId locales/remotos coinciden. La prueba de
múltiples transacciones conserva ambos IDs remotos como caso negativo: no se
elimina una transacción para hacerla aprobar.

OrderReferenceReadTest cubre ambos formatos históricos/pa1_ en los cuatro estados,
referencias ausentes, vacías, malformadas y cruzadas, orden contradictoria,
respuesta nula y ambigüedad incluso con Payment APPROVED. Verifica ausencia de
mutaciones y el orden Payment → PaymentAttempt. Las pruebas anteriores de
webhooks conservan los rechazos por providerPaymentId contradictorio.

Validación ejecutada: las siete clases del comando local anterior suman ahora
122 pruebas (18 de OrderReferenceReadTest), sin fallos, errores ni omisiones.
También se compilan las suites PostgreSQL, sin ejecutarlas. No se cambian el
parser, la emisión, las validaciones, Preferences, HTTP ni los bloqueos.

Para repetir exactamente las 29 pruebas PostgreSQL (14 de investigación y 15 de
conciliación), desde backend en Fedora y con PostgreSQL local ya preparado:

```bash
export DB_URL=jdbc:postgresql://127.0.0.1:5432/jaldishop_integration_test
export DB_USERNAME=jaldishop_test
read -rs -p 'Clave local: ' DB_PASSWORD
export DB_PASSWORD
./mvnw -o -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
  -Dtest=ManualOrderInvestigationPostgresIntegrationTest,OrderReconciliationPostgresIntegrationTest test
unset DB_PASSWORD
```

Usar exclusivamente la base dedicada: las suites limpian sus tablas de prueba.
La ruta de rechazo ambiguo revierte consolidación y conserva el lease previamente
adquirido hasta su expiración; la prueba lo refleja, sin cambiar su gestión.
La clasificación actual expone ese rechazo como TECHNICAL_FAILURE; no implica
que se deba aplicar automáticamente una transición financiera.

## Cierre de 2C.1

El usuario confirmó la ejecución en Fedora de las 29 pruebas PostgreSQL:
0 fallos, 0 errores, 0 omitidas y BUILD SUCCESS. Esta validación se suma a
las pruebas focalizadas reportadas. Durante el cierre no se repitieron pruebas.
La corrección final afectó únicamente fixtures, expectativas, cobertura negativa
y documentación; no añadió cambios de producción.

## Pendientes antes de 2C.2

- Cambiar deliberadamente el emisor Orders y probar que crea snapshots pa1_ con el
  mismo intento; mantener Preferences histórico. No está activado aquí.
- Validar en PostgreSQL dedicado los escenarios nuevos de pa1_ y concurrencia real.
- Decidir explícitamente si se permitirá recuperar una orden cuyo ID local falta;
  actualmente se rechaza, aunque la referencia pa1_ sea válida.
- Cualquier herramienta externa que interprete external_reference como UUID de
  Payment debe adoptar el parser/tipo antes de emitir pa1_. No se modificaron
  frontend, Postman ni herramientas externas.
