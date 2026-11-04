# ☕ JaldiShop Backend API — Guía de Instalación y Ejecución Local

**Entregable 1 — Modelado de Datos + Backend API (Núcleo Transaccional)**

API RESTful desarrollada en **Java 21** con **Spring Boot 3.4**, **Spring Security 6 (JWT)**, **PostgreSQL 16+** y **Flyway** para versionamiento de esquema.

---

## 1. Requisitos Previos

* **Java Development Kit (JDK):** Versión 21 o superior (`java -version`).
* **Apache Maven:** Versión 3.9+ o el wrapper incluido (`./mvnw` o `mvnw.cmd`).
* **PostgreSQL:** Versión 16 o superior (o Docker instalado).
* **Cliente REST:** Postman o Insomnia para importar la colección oficial.

---

## 2. Configuración de Base de Datos & Variables de Entorno

Puedes configurar las credenciales mediante variables de entorno en tu sistema o en el archivo [`src/main/resources/application.properties`](./src/main/resources/application.properties):

```properties
# Configuración del Servidor
server.port=8080

# Conexión a Base de Datos PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/jaldishop
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# Control de Migraciones con Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# Seguridad & JWT
jwt.secret=9a78b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6
jwt.expiration-hours=24

# Logs y Debugging
logging.level.com.jaldishop.backend=DEBUG
```

---

## 3. Despliegue Rápido de PostgreSQL con Docker (Opcional)

Si cuentas con Docker instalado, puedes iniciar una instancia de PostgreSQL lista para usar:

```bash
docker run --name jaldishop-postgres \
  -e POSTGRES_DB=jaldishop \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16-alpine
```

---

## 4. Migraciones de Base de Datos y Seed Data

El esquema se versiona y aplica automáticamente al iniciar la aplicación mediante **Flyway** (migraciones `V1` a `V14`):

1. **`V1__initial_schema.sql`:** Creación del esquema relacional base (3FN), claves foráneas con políticas `ON DELETE RESTRICT/CASCADE`, restricciones `CHECK` e índices únicos parciales.
2. **`V2__seed_roles.sql`:** Catálogo de roles base (`CUSTOMER`, `MERCHANT`, `ADMIN`).
3. **`V3__add_store_customers.sql`:** Registro histórico de relación cliente-tienda (migrado posteriormente en V12).
4. **`V4__seed_complete_demo_data.sql`:** Semillas completas de demostración (usuarios, tiendas, catálogo y configuración de capacidad).
5. **`V5__remove_tax_applies_from_stores.sql`:** Limpieza y simplificación de configuración tributaria en tiendas.
6. **`V6__set_default_currency_and_normalize_phones.sql`:** Moneda por defecto `PEN` y normalización de teléfonos de contacto.
7. **`V7__strengthen_relational_integrity_and_multitenancy.sql`:** Índices únicos parciales para roles globales (`store_id IS NULL`) y contextuales (`store_id IS NOT NULL`).
8. **`V8__seed_store_categories_and_branding.sql`:** Catálogo de rubros comerciales y soporte de branding de tiendas.
9. **`V10__fix_user_roles_id_default_and_store_sync.sql`:** Soporte de `DEFAULT gen_random_uuid()` para claves surrogate en `user_roles`.
10. **`V11__refine_store_ownership.sql`:** Fortalecimiento de la propiedad de tiendas (`owner_user_id` NOT NULL con FK `ON DELETE RESTRICT`).
11. **`V12__migrate_store_customers_to_user_roles_and_check_scope.sql`:** Unificación de clientes de tienda dentro de `user_roles` (`CUSTOMER` contextual), eliminación segura de `store_customers` y validación estricta de alcance vía `CHECK (ck_user_roles_role_scope)`.
12. **`V13__strengthen_inventory_reservations_multitenancy.sql`:** Aislamiento multitenant de reservas de inventario (`store_id` obligatorio, FK compuesta `(capacity_reservation_id, store_id)` y clave candidata en `capacity_reservations`).
13. **`V14__prevent_capacity_slot_overlaps.sql`:** Extensión `btree_gist` y restricción de exclusión física `EXCLUDE USING gist` para evitar solapamientos de franjas horarias de capacidad activas.

Para verificar el estado de las migraciones sin levantar el servidor:
```bash
./mvnw flyway:info
```

### Seed Reproducible para Desarrollo y Pruebas E2E

Para sembrar un conjunto coherente, determinista y seguro de datos para demostración y validación end-to-end, se dispone del script reproducible [`seed-e2e.sh`](./seed-e2e.sh) y su archivo SQL canónico [`seed_e2e_demo_data.sql`](./src/main/resources/db/seed/seed_e2e_demo_data.sql).

**Características del Seed E2E:**
- **Idempotente y Seguro:** Emplea cláusulas `ON CONFLICT DO UPDATE / NOTHING` con UUIDs deterministas. Rechaza ejecución automática fuera de `localhost` o `127.0.0.1`.
- **Cero Secretos Reales:** Mocks locales exclusivamente; no contiene credenciales reales ni activa cobros externos.
- **Entidades sembradas:**
  - Usuarios: Cliente (`cliente.demo@jaldishop.com`), Comerciante (`merchant.demo@jaldishop.com`), Admin (`admin.demo@jaldishop.com`) con password común `Password123!`.
  - Tienda y Catálogo: Tienda `dulce-deleite` (Pastelería Dulce Deleite) con horarios, configuración de capacidad (10 cupos/franja), categorías y productos con variantes (`Torta de Chocolate Artesanal`, `Cheesecake de Frutos Rojos`, etc.) e inventarios sincronizados.
  - Libreta de Direcciones: Direcciones con códigos UBIGEO oficiales de Lima (`150122`, `150119`).
  - Carrito, Reservas y Pagos: Reservas de capacidad e inventario, pagos simulados (`APPROVED`, `REFUNDED`) e historial de auditoría de órdenes (`JAL-2026-000001`, `JAL-2026-000002`).

**Ejecución:**
```bash
# Parámetros explícitos obligatorios (restringido a bases de datos locales de prueba autorizadas)
DB_HOST=127.0.0.1 DB_PORT=5434 DB_NAME=jaldishop_test DB_USER=test_user PGPASSWORD=test_pass ./seed-e2e.sh
# o alternativamente con DB_URL:
DB_URL="jdbc:postgresql://127.0.0.1:5434/jaldishop_test" DB_USERNAME=test_user DB_PASSWORD=test_pass ./seed-e2e.sh
```

### Núcleo Transaccional & Pruebas de Concurrencia
El checkout integra de forma atómica:
* **Capacidad + Inventario en un Hold Atómico:** Bloqueos pesimistas `PESSIMISTIC_WRITE` (`FOR UPDATE`) sobre `capacity_configurations` e `inventories` con orden canónico determinista por variante para evitar deadlocks.
* **Suite de pruebas de concurrencia:** Verificada en [`CheckoutHoldConcurrencyIntegrationTest`](src/test/java/com/jaldishop/backend/checkout/application/CheckoutHoldConcurrencyIntegrationTest.java), con bootstrap automático de Flyway para CI y limpieza (`teardown`) garantizada antes y después de su ejecución.

---

## 5. Compilación y Ejecución del Servidor

### En Windows (PowerShell / CMD):
```powershell
# Compilar y ejecutar pruebas unitarias
.\mvnw.cmd clean test

# Iniciar la aplicación
.\mvnw.cmd spring-boot:run
```

### En Linux / macOS:
```bash
# Compilar y ejecutar pruebas
./mvnw clean test

# Iniciar la aplicación
./mvnw spring-boot:run
```

El servidor estará escuchando en `http://localhost:8080`.

---

## 6. Verificación de Salud del API

Ejecuta en tu terminal o navegador:
```bash
curl -X GET http://localhost:8080/api/v1/health
```

**Respuesta esperada (HTTP 200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2026-09-23T09:30:00Z"
}
```

---

## 7. Pruebas con Colección Postman

1. Importa en Postman o Bruno los 2 archivos unificados:
   - [`postman/jaldishop.postman_collection.json`](../postman/jaldishop.postman_collection.json)
   - [`postman/jaldishop.postman_environment.json`](../postman/jaldishop.postman_environment.json)
2. La colección incluye scripts automáticos para almacenar el JWT al hacer login:
   - `01. Identity > Register Merchant` -> Crea comerciante y tienda.
   - `02. Identity > Auth > Iniciar Sesión — Merchant` -> Auto-captura el `authToken` en el entorno.
   - Carpetas completas de `Stores`, `Catalog`, `Capacity`, `Cart`, `Checkout`, `Ordering` y `Notifications`.

Consulta la guía detallada en [`postman/README.md`](../postman/README.md).

---

## 8. Criterios de Evaluación y Seguridad

* **Normalización (3FN):** Estricta segregación de entidades, claves foráneas indexadas y snapshots históricos inmutables en pedidos (`orders`).
* **Seguridad de Passwords:** Hasheadas con algoritmo **BCrypt** de 10 rondas antes de persistirse.
* **Validación de Inputs:** Integración con **Jakarta Validation** (`@NotBlank`, `@NotNull`, `@Min`, `@Pattern`, `@Size`) y manejador centralizado `GlobalExceptionHandler`.
* **Control de Acceso:** Filtro `JwtAuthenticationFilter` sin estado con verificación de roles en cada endpoint (`@PreAuthorize` / `SecurityFilterChain`).
