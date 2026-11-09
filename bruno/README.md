# JaldiShop API — Colecciones Bruno Nativas

Colecciones nativas en formato `.bru` para Bruno 4.3.0+, organizadas para desarrollo local y demostración de JaldiShop.

## Características

1. **APIs Nativas de Bruno:** Usa exclusivamente `bru`, `req`, `res`, `test` y `expect`. Cero referencias o dependencias de Postman (`pm`).
2. **Sin Errores en Ejecución:** Soluciona el error `ReferenceError: 'pm' is not defined`.
3. **Flujos Libres:** No exige `runCase` ni bloquea peticiones de lectura cotidianas.
4. **Flexibilidad de Entorno:** Permite ejecutar contra cualquier host configurado en `baseUrl` (local o producción/Render) sin bloqueos artificiales de URL, manteniendo únicamente los controles explícitos para pagos (`financialFakeReady`) y operaciones destructivas (`allowDestructiveOperations`).
5. **Token Unificado Dinámico (`authToken`):**
   - Todos los endpoints autenticados consumen directamente `{{authToken}}`.
   - Al registrarse o iniciar sesión (Customer, Merchant o Admin), el token emitido se captura automáticamente en `authToken` (y se replica en `customerToken`, `merchantToken` y `adminToken`).
   - Evita errores 401 por tokens cruzados o sesiones obsoletas al cambiar de usuario o rol en medio de una prueba.
   - Captura automática de `userId`, `storeId`, `storeSlug`, `categoryId`, `productId`, `variantId`, `cartId`, `orderNumber` y `reservationId`.

## Organización de Carpetas (Colección Principal)

1. `01.-Autenticacion`: Registro, login por rol, refresco de token y perfil.
2. `02.-Customer`: Direcciones, favoritos y reseñas.
3. `03.-Tiendas-y-catalogo`: Health check, tiendas destacadas, búsqueda y consulta de productos/categorías.
4. `04.-Carrito-y-checkout`: Gestión de carrito, franjas horarias (slots), disponibilidad y checkout.
5. `05.-Pedidos-y-pagos`: Pedidos del cliente, pagos locales Mercado Pago e investigación de pagos.
6. `06.-Merchant`: Configuración de tienda, catálogo, variantes, inventario, capacidad y pedidos. Incluye administración de plataforma.
7. `07.-Notificaciones`: Lista de notificaciones y conteo de no leídas.
8. `08.-Regresiones`: Casos R001 a R011 para verificación de correcciones del backend.

## Colección Secundaria (Pruebas Exhaustivas y Negativos)

Ubicada en `bruno/jaldishop-exhaustive`, contiene los más de 480 casos de validación de campos (nulos, vacíos, ausentes, formatos inválidos y errores de rol/autenticación).

## Cómo Usar en Bruno

1. Abre la aplicación **Bruno**.
2. Selecciona **Open Collection** y navega a:
   - `Documentos/Proyectos/jaldishop/bruno/jaldishop` (o la colección activa en tu workspace de Bruno).
3. Selecciona el entorno `Local` o `JaldiShop Local` en el selector superior derecho (ya está preinstalado en `environments/`).
4. Si necesitas importar un entorno manualmente desde archivo:
   - Selecciona `bruno/jaldishop.local.environment.json` (formato Bruno con `variables`).
   - **Nota:** No uses `jaldishop.local.env.json` en Bruno, ya que es formato Postman (`values`) y provocará el error `missing or invalid variables array`.
5. Ejecuta las peticiones en orden:
   - Registrar o Iniciar Sesión para capturar el token del rol deseado.
   - Consumir el catálogo, agregar al carrito y procesar pedidos.
