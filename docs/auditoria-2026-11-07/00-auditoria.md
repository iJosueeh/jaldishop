# Auditoría JaldiShop — 7 de noviembre de 2026

## Decisiones y acciones prioritarias

No declarar el backend listo para el martes 10. Esta auditoría identifica contrato y mecanismos estáticos; **cero tests ejecutados y cero endpoints probados funcionalmente durante la auditoría**. No se conectó a PostgreSQL, Render, Neon ni Mercado Pago; no se ejecutó Maven, migraciones, webhooks, reembolsos ni HTTP.

1. Autorizar posteriormente correcciones de aislamiento multimedia y revocación de roles (P0).
2. Corregir entradas de variantes que pueden producir 500 (P1), antes de pruebas funcionales extensas.
3. Revisar límites compatibles con columnas SQL y exposición de diagnóstico (P1/P2).
4. Congelar un estado de trabajo reproducible: hay modificaciones previas locales en pagos, tests, frontend y Postman. Los hallazgos corresponden al **working tree**, incluyendo archivos nuevos, no exclusivamente a HEAD. Esta auditoría no modificó esos archivos.
5. Tras autorización, ejecutar pruebas aisladas y luego flujos funcionales con fixtures nuevos; conservar `jaldishop_test`. Incluso `jaldishop_integration_test` permaneció intacta en esta fase.

## Entregables

- [Inventario completo y matriz Postman](01-inventario.md): controlador, método, ruta, éxito y cantidad de requests por endpoint.
- [Evidencia de validaciones y SQL](02-evidencia-validaciones.md): índice de anotaciones DTO, guardas de dominio/servicio y restricciones de migraciones, con líneas.
- [Pruebas localizadas](03-pruebas.md): archivos y métodos candidatos por módulo, sin confundir nombres con aprobación.
- [Contrato por endpoint y asignación](04-equipo-y-casos.md): solicitudes, roles, respuestas y orden de ejecución.
- Este documento: hallazgos priorizados, matriz interpretada y etapas de corrección.

## Estado e inventario

32 controladores REST propios, 97 endpoints únicos. Postman: 100 requests, 97 firmas distintas; 97/97 firmas presentes, ninguna ruta obsoleta o faltante por método/ruta. Tres requests adicionales no son tres endpoints adicionales: ver duplicados en inventario. Esto no comprueba cuerpos, headers, variables, scripts ni cumplimiento del contrato.

La configuración real es Java 21 / Spring Boot **4.1.1** (`backend/pom.xml:8`), no Spring Boot 3. No se propone migrar para esta presentación. La versión importa para binding, validación MVC y ejecución de los slices.

Postman tiene 36 requests con script local de tipo test, pero solo 24 contienen `pm.test`; scripts de extracción de variables no equivalen a aserciones. No hay eventos raíz. Los scripts de carpetas deben revisarse antes de concluir qué requests carecen de aserciones heredadas.

Bruno: no encontrado dentro del repositorio. Sí hay artefactos relacionados accesibles:

- `/home/ijosueeh/.gemini/antigravity-cli/brain/078c7ee2-cbd6-4c2b-8c34-b7876eba0906/scratch/bruno_imported_test`: 86 archivos `.bru`, con `bruno.json`.
- Mismo directorio padre, `bruno_imported_phase_d`: 89 `.bru`, sin `bruno.json` localizado en esa carpeta.

Son copias externas; ni su vigencia ni su ejecución están certificadas. No deben reemplazar Postman automáticamente. Los fixtures Bruno de `.npm` no son colecciones JaldiShop.

## Hallazgos

Clasificación: VALIDACIÓN ESPERADA = rechazo deliberado documentado; DEFECTO FUNCIONAL/SEGURIDAD/INTEGRIDAD = mecanismo demostrado por fuente; RIESGO NO CONFIRMADO = consecuencia depende de ejecución o configuración; COMPORTAMIENTO NO VERIFICADO = sin evidencia suficiente. Prioridad mide impacto, no certeza. “Confirmado estáticamente” no significa reproducido por HTTP.

### F01 — P0 — DEFECTO DE SEGURIDAD: Firma multimedia sin aislamiento de tienda

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/media/application/service/GenerateUploadSignatureService.java:29`. Confirmación de impacto por ejecución: pendiente.

`execute` usa `explicitStoreId` directamente; solo consulta tienda del usuario cuando falta. `MediaController.generateUploadSignature` permite branding anónimo con storeId y permite PRODUCT_IMAGE a MERCHANT/ADMIN sin validar que esa tienda le pertenezca. `CloudinaryMediaStorageService.generateSignature/resolveFolder` firma la carpeta de ese UUID. Un merchant puede obtener firma para carpeta ajena; un visitante puede hacerlo para branding. No demuestra sobrescritura de un recurso existente, pero sí ruptura del aislamiento de subida y abuso de almacenamiento.

Esperado: permitir pre-registro en espacio temporal y autorizar pertenencia para carpeta de tienda existente. Corrección mínima: validar el contexto antes de firmar, preservando onboarding sin tienda. Prueba: visitante y merchant A piden storeId B → rechazo controlado y cero llamadas al firmador; A con A y branding temporal permitido → 200. El test existente `generateSignatureWithExplicitStoreId` prueba el comportamiento actual, no su seguridad.

### F02 — P0 — DEFECTO DE SEGURIDAD: Retirada de rol no invalida privilegios del token

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/identity/infrastructure/security/JwtAuthenticationFilter.java:75`. Confirmación de impacto por ejecución: pendiente.

`doFilterInternal` consulta al usuario y verifica `canAuthenticate()`, pero construye authorities desde `principal.roles()` del JWT; no desde los roles actuales que carga `UserRepositoryAdapter.findById`. `UpdateUserRolesService.execute` guarda cambios sin revocar tokens. Un ADMIN activo degradado por otro administrador conserva acceso a `/api/v1/admin/**` con su token anterior hasta expiración. La suspensión sí se consulta y bloquea; no confundir ambos casos.

Esperado: retirada de privilegios efectiva para peticiones posteriores según política de seguridad explícita. Corrección mínima: crear principal y authorities desde roles actuales o introducir versión de token; actualizar ambos, porque varios controladores consultan principal.roles directamente. Prueba: emitir token ADMIN, retirarlo con segundo admin, reutilizar token → 403 sin invocar servicio; token CUSTOMER no obtiene acceso admin. No se ejecutó la prueba.

### F03 — P1 — DEFECTO FUNCIONAL: Elemento null en attributes llega a NullPointerException

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java:62`. Confirmación de impacto por ejecución: pendiente.

`createVariant` y `updateVariant` recorren `attributes` y llaman `attr.name()`. Los DTO tienen `@Valid List<VariantAttributeDto>`, sin `@NotNull` sobre el elemento: cascada ignora null. Un cuerpo por lo demás válido con `attributes:[null]` supera esas restricciones y provoca NPE; el handler genérico lo traduce a 500. La lista null se admite deliberadamente y no es el problema.

Esperado: 400 VALIDATION_ERROR. Corrección mínima: restricción de elemento no nulo en ambos DTO. Prueba MockMvc con tienda propia y servicio mock: POST y PUT con elemento null deben dar 400 sin guardar; atributos válidos mantienen 201/200.

### F04 — P1 — DEFECTO FUNCIONAL: Longitudes de atributo no alineadas con PostgreSQL

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/catalog/web/dto/VariantAttributeDto.java:6`. Confirmación de impacto por ejecución: pendiente.

Nombre de atributo solo tiene NotBlank; `VariantAttribute` valida vacío pero no longitud; el mapper transmite el nombre. V1 crea `variant_attributes.name VARCHAR(120)` y PK `(variant_id,name)`. Un nombre no vacío de 121 caracteres supera validación y excede el almacenamiento. El handler de integridad solo convierte unique a 409; otras fallas van a 500 o handler genérico según traducción JDBC. El rechazo SQL depende de aplicar el esquema, no se conectó a la base.

Esperado: 400 antes de persistir, fundado en límite real de 120. Corrección mínima: Size(max=120) en DTO, revisar frontera de dominio para otros llamadores. Prueba de frontera 120/121 y persistencia PostgreSQL real aislada; comprobar rollback sin variantes parciales.

### F05 — P1 — DEFECTO FUNCIONAL: Alta merchant acepta nombres superiores a columna users

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/identity/web/dto/RegisterMerchantRequest.java:17`. Confirmación de impacto por ejecución: pendiente.

`firstName` y `lastName` solo NotBlank; `RegisterMerchantCommand` no restringe y `User.create` valida presencia, no longitud. V1 limita ambos a VARCHAR(100). Un nombre de 101 caracteres alcanza persistencia y puede producir 500. No es razón para imponer mínimos nuevos: el límite proviene de SQL.

Esperado: 400 de validación y ninguna alta parcial. Corrección mínima: alinear máximos 100; revisar también límites de email contra VARCHAR(254), sin asumir que @Email impone longitud. Prueba 100/101 para ambos nombres, integración de rollback merchant + tienda + roles. Test de rollback existente no demuestra este caso por existir.

### F06 — P2 — DEFECTO DE SEGURIDAD: Diagnóstico detallado de salud expuesto públicamente

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/identity/infrastructure/security/SecurityConfig.java:48`. Confirmación de impacto por ejecución: pendiente.

SecurityConfig permite Actuator anónimo. `application.properties:6-7` expone health/info y show-details=always. Permite ver detalles de contribuidores de salud según componentes activos; no se afirma exposición de secretos ni que se exponga env, que no está incluido.

Esperado: resumen público y diagnóstico restringido. Corrección mínima futura: limitar detalles por autorización y revisar allowlist sin alterar seguridad en esta fase. Prueba de configuración/perfil: anónimo solo status; admin ve lo permitido.

### R01 — P0 — RIESGO NO CONFIRMADO: Persistencia local falla después de crear sesión remota

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/payment/application/preference/CreatePaymentPreferenceService.java:134`. Confirmación de impacto por ejecución: pendiente.

Los métodos executeWithPreferencesApi/executeWithOrdersApi separan DB, gateway y DB: el último paso puede fallar después del éxito remoto. Hay idempotencia por intento, evidencia remota, leases y reconciliación en el proyecto; no se demuestra aquí doble cobro ni falta de recuperación. No resolver envolviendo HTTP en una transacción larga.

Esperado: reintento y recuperación convergen a una sesión/pedido; ningún aprobado sin seguimiento. Acción mínima: probar fallo inyectado después de respuesta remota y antes de guardar, con gateway fake. Incluir dos invocaciones concurrentes, reinicio, webhook duplicado y reconciliación simultánea. PostgreSQL real para locks/unique/leases; no llamar Mercado Pago en esa prueba.

### R02 — P2 — RIESGO NO CONFIRMADO: Catálogo público incluye variantes inactivas

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicProductController.java:74`. Confirmación de impacto por ejecución: pendiente.

fetchActiveProducts filtra productos ACTIVE pero pasa todas las variantes al mapper; este incluye todas y, si solo existe una variante, calcula precio aunque esté INACTIVE. No se ha establecido si el contrato público permite exhibir variantes inactivas. Checkout sí rechaza variante inactiva.

Acción: decidir con equipo si son visibles o deben excluirse; si se excluyen, filtrar en consulta pública y probar producto con variantes mixtas y única inactiva. No cambiar mapper compartido sin revisar merchant.

### R03 — P2 — COMPORTAMIENTO NO VERIFICADO: DiscountCode aceptado pero cálculo no aplica descuentos

Evidencia principal: `backend/src/main/java/com/jaldishop/backend/checkout/domain/model/CheckoutPricing.java:36`. Confirmación de impacto por ejecución: pendiente.

InitiateCheckoutRequest/command transportan discountCode, pero calculate fija descuento cero y código null. No hay controlador de descuentos en inventario. No afirmar descuento incorrecto sin regla aprobada ni presentar cupones como funcionalidad disponible.

Acción: definir si se rechaza código informado o se documenta fuera del alcance; test correspondiente. No implementar motor de descuentos arbitrario.

## Contrato de errores identificado

`shared/exception/GlobalExceptionHandler` y `ApiError` establecen `{code,message,errors}`; errors puede ser null u omitirse según serialización. No exigir timestamp/path porque no existen en record. El webhook devuelve éxito `{status,processed,message}` y errores ApiError; no es una inconsistencia por sí misma.

| Entrada o condición | Resultado por fuente | Clasificación |
|---|---|---|
| Bean Validation, JSON roto, UUID/enum no convertible, query obligatorio ausente | 400, códigos específicos de handler | VALIDACIÓN ESPERADA |
| IllegalArgumentException | 400 INVALID_ARGUMENT | VALIDACIÓN ESPERADA cuando guarda una entrada inválida |
| BusinessRuleException | 422, código de negocio | VALIDACIÓN ESPERADA; no exigir 400 para toda regla |
| ConflictException / unique SQL 23505 | 409, código de regla / RESOURCE_CONFLICT | VALIDACIÓN ESPERADA |
| Recurso ausente | 404 | VALIDACIÓN ESPERADA |
| Reserva de otro customer | 404 en Get/ReleaseCapacityReservationService | VALIDACIÓN ESPERADA; oculta existencia |
| Sin JWT válido en ruta protegida | 401 entry point | VALIDACIÓN ESPERADA |
| Usuario suspendido/inactivo/no encontrado con JWT | 401; consulta estado en cada request | VALIDACIÓN ESPERADA |
| Rol insuficiente / AccessDeniedException | 403 | VALIDACIÓN ESPERADA |
| JWT válido con fallo de infraestructura al consultar usuario | 503 SERVICE_UNAVAILABLE desde filtro | Error técnico controlado; no validación del cliente |
| Lock optimista/pesimista | 409 CONCURRENT_MODIFICATION | Conflicto controlado; comprobar postcondición |
| Método no soportado | 405 | VALIDACIÓN ESPERADA |
| MP timeout / rate limit / indisponible | 504 / 429 / 502 | Error de integración controlado |
| MP sin credenciales o no autorizado | 500 PAYMENT_GATEWAY_ERROR | Error de configuración controlado, no input inválido |
| MP parámetros incompatibles | 400 PAYMENT_GATEWAY_ERROR | Error de integración; no atribuir automáticamente culpa al usuario |
| Restricción DB no unique / excepción inesperada | 500 genérico | Clasificar causa, no convertir todo a validación |

Firma webhook ausente/inválida: 401 INVALID_WEBHOOK_SIGNATURE; parámetros malformados: 400 BAD_WEBHOOK_REQUEST; conflicto 409. HTTP 200 processed=false puede ser notificación ignorada, no prueba de confirmación de pago.

Refresh requiere JWT autenticado vigente: no hay endpoint público de refresh token independiente; probar el contrato real y no esperar renovar un JWT expirado.

## Matriz interpretada de reglas, pruebas y pendientes

El índice 02 contiene campos, máximos y guardas por archivo. Esta tabla resume caminos principales y límites de evidencia; el índice 03 localiza tests para cada módulo. Cobertura exacta de líneas/ramas no calculada; las coincidencias de nombres no certifican escenarios.

| Módulo | Validación implementada / contrato | Evidencia de tests existente | Negativos y autorización a cerrar antes de demo |
|---|---|---|---|
| identity | email/password/presencia; BCrypt; estado usuario; roles admin; autorretiro admin bloqueado | JwtAuthenticationFilterTest, SecurityUserStatusWebTest, RegisterMerchantRollbackIntegrationTest, RefreshTokenServiceTest | roles retirados con token previo; nombres 101; roles [null]; duplicado concurrente; registro parcial |
| store | nombre, geo emparejada/rangos, fee >=0, tax 0..100; StoreContextService para pertenencia | StoreControllerTest, StoreContextServiceTest, CloseMyStoreServiceTest y status | tienda ajena; tienda suspendida; cierre con pedidos/pagos pendientes; dependencias Cloudinary no atómicas |
| catalog | categoría/producto de misma tienda; nombres/slugs/SKU con límites; precio >=0.01, moneda tres mayúsculas; unique SKU tienda | controladores, servicios, modelos y PostgreSQL catalog | attributes null/121/duplicados; SKU simultáneo; precio excede NUMERIC(12,2); redondeo/escala; categoría ajena |
| inventory | cantidad no negativa; no reducir bajo reservado; flags tracksInventory; reservas/commit | ReserveInventoryServiceTest, InventoryRealConcurrencyPostgresIntegrationTest, InventoryPostgresIntegrationTest | última unidad en paralelo; expiración/commit/liberación simultáneos; rollback sin pérdida; tienda ajena |
| capacity | rangos horarios, configuración/excepción, solapes, reserva duplicada, capacidad agotada, expiry y ownership | pruebas disponibilidad/reserva/configuración y PostgreSQL | último cupo paralelo; cambio configuración mientras reserva; expiración/liberación/commit; fecha pasada según guarda real |
| address | campos DTO, teléfono/ubicación y ownership de customer | CustomerAddressControllerTest y servicios | ID ajeno, inexistente, default concurrente; referencia larga; geo incompleta |
| favorite | customer/producto y duplicados según servicio | CustomerFavoriteControllerTest, AddFavoriteProductServiceTest | producto inactivo/ajeno a tienda consultada; repetir add/remove; dos usuarios |
| cart | user+store; cantidades; pertenencia y disponibilidad de variante | CartControllerTest, CartModelTest y servicios | cantidad cero/negativa/extrema; actualización perdida; cambio de precio/estado antes del checkout; storeId ajeno |
| checkout | tienda ACTIVE, carrito no vacío, variantes activas y de tienda; reserva inventario/capacidad dentro de transacción | CheckoutControllerTest, CheckoutFulfillmentValidatorTest, InitiateCheckoutServiceTest, ConfirmPurchaseServiceTest | fallo entre ambas reservas; DTO inválido; reservas vencidas; snapshot alterado; descuento no verificado |
| ordering | customer propietario, merchant tienda esperada; transiciones/cancelación y compensación | controladores y servicios de pedidos/cancelación | cancelación vs aprobación/fulfillment; cancelación repetida; transición ilegal; usuario/tienda ajenos |
| review | compra/elegibilidad, rating, contenido y ownership | Create/Update/DeleteProductReviewServiceTest, CheckReviewEligibilityServiceTest, controladores | no comprador, compra cancelada, duplicado paralelo, rating límites, editar review ajena |
| payment | reserva/owner/expiry, intento idempotente, estado remoto, correlación, firma, reembolso/reconciliación | 43 archivos *Test.java, mocks y PostgreSQL, ver 03 | éxito remoto/fallo DB; webhook replay/concurrencia; monto/moneda/reference discordantes; refund doble; leases tras crash |
| notification | usuario destino, leído y dedup | NotificationControllerTest, OrderNotificationPostgresIntegrationTest | marcar ajena, evento duplicado, rollback de pedido no notifica; paginación si aplica |
| media | tipo y rol producto; branding público intencional | firma, Cloudinary y cleanup tests | F01; no filtrar apiSecret en respuesta; fallo externo durante cierre |
| realtime | autenticación STOMP y eventos transaccionales (fuera inventario REST) | StompAuthChannelInterceptorTest, WebSocketStompRealIntegrationTest, listeners | suscripción a otro usuario/tienda, token expirado, rollback y duplicados |
| shared/database/e2e | mapeo errores, restricciones y flujo transversal | GlobalExceptionHandlerTest, PostgreSqlArchitectureIntegrationTest, EndToEndBackendFlowValidationIntegrationTest | no-unique DB a 500; HTTP media type no soportado; fixture seguro; inspección no demuestra flujo operativo |

Frontend inspeccionado solo en lectura: merchant register exige password mínimo 8 (`register-form-panel.ts:49`) y backend merchant mínimo 6; frontend exige nombres mínimo 2 y backend presencia. Son divergencias demostrables, no motivo para imponer requisitos nuevos sin decisión de producto. `dailyOrderLimit` mínimo 1 y horarios en register-step4 no forman parte de RegisterMerchantRequest: comportamiento de onboarding/capacidad por verificar, no asumir que se guarda. Frontend no fue modificado. La búsqueda no localizó `frontend-customer/src`; la revisión de validaciones exclusivas de todas las interfaces no está certificada.

## Integridad y límites de la revisión

Hay mecanismos importantes: checkout transaccional, inventario con reservas y locks, capacidad bloqueada sobre configuración/excepción, restricciones multitenant V7/V13, exclusión de solapes V14, deduplicación notificaciones V16, intentos e incidencias de pago V17-V18, refunds V19/V22, identificador remoto único V21, leases V23 y solicitudes de creación/evidencia V24-V25. Su existencia reduce riesgos; no demuestra su aplicación en las bases ni ausencia de carreras.

GetCapacityReservationService hace expireIfDue y save desde GET. Por ello una lectura funcional también puede escribir: mantener prohibición HTTP en esta auditoría y no reutilizar jaldishop_test para “solo consultar” endpoints que materializan estados.

No se inspeccionaron valores de environments ni secretos. La respuesta de firma incluye apiKey pública de Cloudinary, no apiSecret; no clasificar apiKey pública como filtración de secreto. La ausencia de secretos en todos los DTO/logs no quedó demostrada exhaustivamente. Revisar mensajes de excepciones externas persistidos/logueados con datos de proveedor antes de compartir evidencia.

## Plan de correcciones y validación por etapas (pendiente de autorización)

1. **Congelar contrato y evidencia**: acordar Boot 4.1.1 real; identificar working tree que se presentará; conservar cambios existentes; acordar códigos para fallos por campo y ownership. No hacer commits desde esta auditoría.
2. **P0**: corregir F01/F02 y añadir regresiones de aislamiento y rol revocado. Validar recuperación R01 con gateway falso y PostgreSQL desechable. No activar conciliaciones ni reembolsos reales.
3. **P1**: F03/F04/F05 con fronteras derivadas de SQL; decidir escala/rango monetario a partir de NUMERIC(12,2); confirmar que errores de entrada llegan a 400/422/409 y no 500. No ocultar fallos técnicos convirtiéndolos indiscriminadamente a 400.
4. **P2 y contrato de producto**: restringir diagnóstico; resolver variantes públicas inactivas, descuento y divergencias frontend documentadas. Mantener fuera de alcance funciones sin contrato aprobado.
5. **Pruebas automáticas tras permiso**: primero unitarias/slices sin DB ni red; luego PostgreSQL real en jaldishop_integration_test con fixture explícitamente desechable y guardas; revisar TestPostgresConfig y cada test que borre/trunque. Sus guardas existen pero no certifican todos los fixtures. No usar jaldishop_test, Render ni Neon.
6. **Colecciones y funcionales tras permiso**: aplicar plan del documento 04, capturar resultado esperado/obtenido sin tokens, validar flujo por tres roles y dos tiendas. Probar integración externa solo en entorno de pruebas expresamente autorizado, después de pruebas con fake.
7. **Criterio de presentación**: ninguna vulneración de tienda/usuario, ningún duplicado financiero, stock/cupo no negativos ni excedidos, recuperación de errores de red demostrada, flujos críticos aprobados y evidencia por endpoint. Una colección con 100% de rutas no satisface este criterio.

Registro recomendado por caso: endpoint, fixture, rol, contrato identificado, regla implementada, test que realmente afirma la regla, ejecución/fecha/resultado, prueba funcional/resultado y evidencia sin datos sensibles. Todos los resultados nuevos de ejecución están pendientes.
