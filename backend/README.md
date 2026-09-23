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

El esquema se aplica de forma automática al iniciar la aplicación mediante **Flyway**:

1. **`V1__initial_schema.sql`:** Creación de 19 tablas normalizadas (3FN), claves foráneas con políticas `ON DELETE RESTRICT/CASCADE`, restricciones `CHECK` e índices únicos parciales.
2. **`V2__seed_roles.sql`:** Inserción de roles base (`CUSTOMER`, `MERCHANT`, `ADMIN`).

Para verificar el estado de las migraciones sin levantar el servidor:
```bash
./mvnw flyway:info
```

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

1. Importa en Postman el archivo:
   - [`backend/jaldishop-api.postman_collection.json`](./jaldishop-api.postman_collection.json)
2. La colección incluye scripts automáticos para almacenar el JWT al hacer login:
   - `01. Register Merchant` -> Crea comerciante y tienda.
   - `03. Login Merchant` -> Auto-captura el `merchantToken`.
   - `02. Tiendas (Stores)` -> Configuración logística.
   - `04. Capacidad & Slots` -> Reglas de cupos semanales y excepciones.
   - `05. Comandas & Pedidos` -> Registro y avance de pedidos.

Consulta la guía detallada en [`docs/04-diseno/guia-pruebas-postman.md`](../docs/04-diseno/guia-pruebas-postman.md).

---

## 8. Criterios de Evaluación y Seguridad

* **Normalización (3FN):** Estricta segregación de entidades, claves foráneas indexadas y snapshots históricos inmutables en pedidos (`orders`).
* **Seguridad de Passwords:** Hasheadas con algoritmo **BCrypt** de 10 rondas antes de persistirse.
* **Validación de Inputs:** Integración con **Jakarta Validation** (`@NotBlank`, `@NotNull`, `@Min`, `@Pattern`, `@Size`) y manejador centralizado `GlobalExceptionHandler`.
* **Control de Acceso:** Filtro `JwtAuthenticationFilter` sin estado con verificación de roles en cada endpoint (`@PreAuthorize` / `SecurityFilterChain`).
