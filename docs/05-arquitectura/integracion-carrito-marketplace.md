# Integración del carrito Customer en Marketplace

El navbar comparte el mismo componente de carrito en el landing y las tiendas. La selección ya no se reinicia al navegar al landing. `StorefrontCartProvider` mantiene la tienda activa; TanStack Query consulta el carrito real del cliente por `storeId`.

## Responsabilidades

- `features/storefront/cart/services/cartService.ts` contiene el contrato HTTP y los DTO de `/api/v1/cart/**`.
- `StorefrontCartProvider` coordina sesión, consultas y operaciones; los precios, cantidades y disponibilidad visibles proceden de `CartResponse`.
- `StorefrontCart` presenta el icono, resumen, acceso del cliente y estados de operación. El catálogo solo selecciona productos y variantes.
- Las rutas `app/api/customer` actúan como adaptador del servidor Next hacia Spring. El JWT se guarda en una cookie HttpOnly, SameSite=Lax y Secure en producción; nunca se devuelve al cliente ni se guarda en localStorage. Las mutaciones rechazan orígenes distintos del sitio.
- La tienda activa se guarda como metadatos públicos en localStorage. Productos, cantidades y precios se recuperan del backend; no existe un carrito local de respaldo.

## Contratos existentes reutilizados

La sesión utiliza `POST /api/v1/auth/login` y admite cuentas con rol CUSTOMER. No se ha añadido un carrito anónimo ni se ha cambiado la seguridad de Spring.

El carrito utiliza `GET /cart?storeId=...`, `POST /cart/items`, `PUT /cart/items/{variantId}`, `DELETE /cart/items/{variantId}?storeId=...` y `DELETE /cart?storeId=...`. El cliente envía identificadores de tienda y variante y cantidades; nunca envía precios calculados.

Cada carrito pertenece a una tienda. Al visitar otra tienda se consulta su carrito independiente, sin mezclar variantes. Volver al landing conserva la referencia de la última tienda, desde la que se puede continuar comprando.

Las operaciones se bloquean mientras hay una solicitud en curso. Un error no sustituye el carrito por productos simulados ni confirma cambios que el servidor rechazó. La sesión vencida solicita autenticación de nuevo.

## Límite del flujo

Añadir productos no reserva inventario ni capacidad. La acción del resumen conduce a las opciones de entrega; no ejecuta checkout ni pago. Los horarios siguen mostrando su estado real de disponibilidad. Esta integración cubre FE-STORE-02; checkout, pago y registro de nuevos clientes son flujos separados.

## Verificación

Las pruebas del servicio verifican los métodos, payloads y errores del contrato. Las pruebas de integración del proveedor verifican consulta, variantes, cantidades, eliminación, autenticación y conservación de la respuesta del servidor tras errores. Las pruebas del adaptador de sesión verifican el rol CUSTOMER y que el token solo se emite como cookie HttpOnly.
