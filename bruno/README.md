# JaldiShop API — Colecciones Bruno Nativas

Colecciones nativas en formato `.bru` para Bruno 4.3.0+, organizadas para desarrollo local y demostración de JaldiShop.

## Características

1. **APIs Nativas de Bruno:** Usa exclusivamente `bru`, `req`, `res`, `test` y `expect`. Cero referencias o dependencias de Postman (`pm`).
2. **Sin Errores en Ejecución:** Soluciona el error `ReferenceError: 'pm' is not defined`.
3. **Flujos Libres:** No exige `runCase` ni bloquea peticiones de lectura cotidianas.
4. **Seguridad Local:** Valida que las peticiones se dirijan a entornos locales (`localhost` / `127.0.0.1`), impidiendo llamadas accidentales a servicios externos como Mercado Pago o Cloudinary.
5. **Captura Automática de Tokens e IDs:**
   - `customerToken` y `customerUserId` en login/registro de cliente.
   - `merchantToken`, `merchantUserId`, `storeId`, `storeSlug` en login/registro y tienda del comerciante.
   - `adminToken` en login de administrador.
   - `categoryId`, `productId`, `variantId`, `cartId`, `orderNumber`, `reservationId` en los endpoints correspondientes.

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
   - `Documentos/Proyectos/jaldishop/bruno/jaldishop` (o la carpeta sincronizada en `Documentos/bruno/JaldiShop API`).
3. Selecciona el entorno `Local` en el selector superior derecho.
4. Ejecuta las peticiones en orden:
   - Registrar o Iniciar Sesión para capturar el token del rol deseado.
   - Consumir el catálogo, agregar al carrito y procesar pedidos.
