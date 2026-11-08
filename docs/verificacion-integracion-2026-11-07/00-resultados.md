# Verificación local — 7 de noviembre de 2026

**336 PASS, 0 FAIL, 0 ERROR, 0 SKIPPED; 55 clases seleccionadas. PostgreSQL y HTTP real BLOCKED. No se declara listo el backend.**

## Aislamiento y entorno

Se revisó el estado Git antes de ejecutar; existían cambios en seguridad, STOMP, pagos, frontend, colecciones y documentación. Esta fase solo añade esta carpeta de documentación. No se inició la aplicación, no se ejecutaron migraciones ni se accedió a ninguna base o proveedor.

Se localizó únicamente `backend/src/main/resources/application.properties` como configuración Spring application dentro de backend/src; no se encontraron configuraciones application por perfil. Datasource usa DB_URL/DB_USERNAME/DB_PASSWORD; Flyway está habilitado y usa db/migration. No existe aquí un perfil aislado listo para iniciar automáticamente. Los schedulers financieros tienen valores por defecto false, pero eso no demuestra los valores efectivos de un proceso futuro.

Las tres variables de base no están definidas en el proceso actual. TestPostgresConfig exige URL loopback, puerto explícito, sin parámetros y nombre jaldishop_integration_test para la ruta normal; la ruta phase2a es distinta y no se usó. La guarda rechaza jaldishop_test y hosts remotos. requireDestructiveIntegrationDatabase valida también el nombre real cuando sus llamadores lo usan; no afirmar que todas las pruebas la invocan. RegisterMerchantRollbackIntegrationTest sobrescribe datasource/Flyway, pero ejecuta migraciones y limpieza de fixtures: no se inició.

El usuario identifica jaldishop_integration_test como desechable; no se pudo comprobar su identidad real, servidor, contenido o permisos en esta sesión. Crear un socket local falla con PermissionError errno 1 incluso antes de bind. Por eso no hay conexión PostgreSQL ni servidor HTTP local; no se buscaron credenciales alternativas ni se cargaron environments con secretos.

Mercado Pago y Cloudinary son dependencias externas. Los tests seleccionados usan mocks para sus colaboradores; Spring MVC se ejecuta en memoria y STOMP usa broker/canales internos. La aplicación completa requerirá gateways fake y schedulers deshabilitados explícitamente antes de habilitar integración.

## Resultados por nivel de evidencia

| Verificación | Resultado | Alcance |
|---|---|---|
| Registro Merchant válido y longitudes | PASS interno / BLOCKED PostgreSQL | MockMvc y servicio con repositorios mock; nombres 100/101 y email 254/255 |
| Crear/actualizar variantes y atributos inválidos | PASS interno / BLOCKED PostgreSQL | POST/PUT MockMvc, null, vacío, nombre 120/121 y duplicados normalizados |
| Estructura y códigos de errores | PASS interno / BLOCKED HTTP real | 400 VALIDATION_ERROR, 400 INVALID_ARGUMENT para nombres repetidos; éxito 201/200 |
| Ausencia de llamadas a servicios con entradas inválidas | PASS interno | Aserciones de no interacción; no constituye prueba de rollback SQL |
| Persistencia, rollback y restricciones SQL aplicadas | BLOCKED | Ninguna afirmación de persistencia real ni esquema aplicado |
| REST, multimedia, roles revocados y estado de usuario | PASS interno | Filtro, MockMvc y servicios; no servidor HTTP real |
| STOMP revocación posterior a suscripción | PASS interno | Broker/outbound real en proceso y repositorios mock; no transporte WebSocket real |
| Regresión identity/catalog/media/capacity/checkout/realtime | PASS seleccionada | Checkout con mocks; no suite completa |

Comando ejecutado desde backend: `mvn -o -DargLine=-javaagent:/home/ijosueeh/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar -Dtest=<clases del anexo> test`. Log local: `/tmp/jaldi-integration-regression.log`; reportes Maven: backend/target/surefire-reports. Estos últimos son artefactos temporales que futuras ejecuciones pueden reemplazar.

## Continuación segura de integración

1. En un entorno que permita sockets, aportar variables de conexión exclusivamente a PostgreSQL loopback y jaldishop_integration_test. No usar perfiles/configuración por defecto con secretos de despliegue.
2. Antes de Flyway o fixtures, consultar current_database(), direcciones del servidor y configuración efectiva de datasource y Flyway sin imprimir credenciales. Confirmar que la instancia es desechable y que ninguna URL alternativa apunta a otra base. Si falla alguna comprobación, detener.
3. Usar gateways fake para Mercado Pago/Cloudinary y deshabilitar explícitamente conciliaciones/reembolsos programados. Revisar otros trabajos programados antes de arrancar el contexto completo.
4. Registro válido: comprobar usuario, roles y tienda en transacción completada. Para nombres 101/email 255, comprobar HTTP 400 y recuentos sin cambios. Probar conflicto de tienda para demostrar rollback después de guardar usuario.
5. Variantes: crear y actualizar con nombre de atributo de 120, releer en SQL y HTTP; rechazar [null], campos vacíos, nombre 121 y duplicados tras trim. Comparar variante y atributos antes/después de cada rechazo; verificar que ninguna fila parcial cambia.
6. Verificar restricciones SQL directamente dentro de transacciones desechables con rollback y fixtures UUID nuevos; mantener separada esta prueba del rechazo anticipado de DTO.
7. Arrancar HTTP solo en 127.0.0.1 y usar fixtures A/B. Ejecutar revocación REST, firmas de tienda ajena y transporte WebSocket con suscripción anterior a retirar rol/desactivar usuario. Registrar respuesta y postcondición.

## Defectos y riesgos pendientes

No se detectaron nuevos defectos en la regresión ejecutada. Los bloqueos de infraestructura no se clasifican como defectos del backend. Continúan sin demostrar persistencia/rollback reales, compatibilidad con el esquema efectivamente desplegado y HTTP/transporte WebSocket reales. STOMP consulta permisos por entrega; quedan el coste de consultas y mensajes ya en vuelo en una revocación concurrente. Bean Validation Size cuenta unidades UTF-16, más restrictivas que PostgreSQL para caracteres suplementarios. La suite seleccionada no certifica todos los módulos ni todos los endpoints.

## Equipo y colecciones

La [matriz definitiva](01-matriz-equipo.md) contiene los 97 endpoints, responsable, éxito HTTP auditado, permisos/evidencia, datos, dependencias, negativos, riesgos y estado. Todo endpoint permanece BLOCKED en HTTP local. Katherine mantiene 44 endpoints, Mia 38 y Josue 15; los pesos por operación no incluyen la carga transversal de Josue.

Para equilibrar ejecución: Josue asume en pareja con Katherine los casos de concurrencia, idempotencia y rollback de capacidad/inventario (reservar un bloque equivalente a 36 puntos de la estimación), además de seguridad transversal. Mia prepara identidades/fixtures comunes y cubre las cuatro lecturas públicas de catálogo ya asignadas. Katherine conserva configuración y operación comercial. Revisar tiempos tras la primera ronda.

Postman contiene 100 requests; la auditoría identifica 97 firmas únicas y tres duplicados adicionales. No se modificaron colecciones. Próxima actualización: añadir aserciones de estado, code/message/errors y postcondiciones; casos P0/P1 con dos tiendas/usuarios, fronteras y ausencia de escrituras parciales; separar escenarios financieros fake. Crear Bruno desde el contrato aprobado, comparar métodos/rutas y evitar sustituirlo por copias externas antiguas. Usar environment local sin secretos versionados y guardas contra hosts/base no autorizados. Sincronizar variables y scripts de extracción con dependencia entre fixtures; ejecutar solo después de resolver el aislamiento.

## Anexo: clases ejecutadas

| Clase | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| com.jaldishop.backend.capacity.application.availability.service.GetCapacityAvailabilityServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.availability.service.GetEffectiveCapacityServiceTest | 15 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.availability.service.ListStoreCapacitySlotsServiceTest | 14 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.configuration.service.CreateCapacityConfigurationServiceTest | 4 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.configuration.service.CreateCapacityExceptionServiceTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.configuration.service.UpdateCapacityExceptionServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.reservation.service.CreateCapacityReservationServiceTest | 7 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.reservation.service.GetCapacityReservationServiceTest | 4 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.application.reservation.service.ReleaseCapacityReservationServiceTest | 6 | 0 | 0 | 0 |
| ReleaseCommittedCapacityReservationService | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.web.controller.CapacityConfigurationControllerTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.web.controller.CapacityExceptionControllerTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.web.controller.CapacityReservationControllerTest | 9 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.web.controller.CapacitySlotsControllerTest | 9 | 0 | 0 | 0 |
| com.jaldishop.backend.capacity.web.controller.EffectiveCapacityControllerTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.CreateCategoryServiceTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.CreateProductServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.CreateProductVariantServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.GetCategoriesServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.GetProductVariantsServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.GetProductsServiceTest | 4 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.UpdateCategoryServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.UpdateProductServiceTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.application.service.UpdateProductVariantServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.web.controller.MerchantCategoryControllerTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.web.controller.MerchantProductControllerTest | 7 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.web.controller.MerchantProductVariantControllerTest | 9 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.web.controller.PublicCategoryControllerTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.catalog.web.controller.PublicProductControllerTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssemblerTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.checkout.application.service.ConfirmPurchaseServiceTest | 18 | 0 | 0 | 0 |
| com.jaldishop.backend.checkout.application.service.InitiateCheckoutServiceTest | 8 | 0 | 0 | 0 |
| com.jaldishop.backend.checkout.web.controller.CheckoutControllerTest | 4 | 0 | 0 | 0 |
| TestPostgresConfigTest | 14 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.application.service.ChangeUserStatusServiceTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.application.service.GetAdminUsersServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.application.service.RefreshTokenServiceTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.application.service.RegisterMerchantServiceTest | 6 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.application.service.UpdateUserRolesServiceTest | 4 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.infrastructure.security.JwtAuthenticationFilterTest | 15 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.infrastructure.security.SecurityUserStatusWebTest | 14 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.web.controller.AdminUserControllerTest | 8 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.web.controller.AuthControllerTest | 5 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.web.controller.UserControllerTest | 9 | 0 | 0 | 0 |
| com.jaldishop.backend.identity.web.mapper.AdminUserResponseMapperTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.media.application.service.GenerateUploadSignatureServiceTest | 8 | 0 | 0 | 0 |
| com.jaldishop.backend.media.application.service.StoreMediaCleanupAdapterTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.media.infrastructure.cloudinary.CloudinaryMediaStorageServiceTest | 3 | 0 | 0 | 0 |
| com.jaldishop.backend.media.web.controller.MediaControllerTest | 4 | 0 | 0 | 0 |
| com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisherTest | 2 | 0 | 0 | 0 |
| com.jaldishop.backend.realtime.infrastructure.listener.OrderDomainEventListenerTest | 4 | 0 | 0 | 0 |
| com.jaldishop.backend.realtime.infrastructure.websocket.StompAuthChannelInterceptorTest | 11 | 0 | 0 | 0 |
| com.jaldishop.backend.realtime.infrastructure.websocket.StompDeliveryAuthorizationBrokerTest | 5 | 0 | 0 | 0 |
| com.jaldishop.backend.realtime.infrastructure.websocket.StompDeliveryConfigurationTest | 1 | 0 | 0 | 0 |
| com.jaldishop.backend.shared.exception.GlobalExceptionHandlerTest | 22 | 0 | 0 | 0 |
