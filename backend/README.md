# JaldiShop Backend: desarrollo local

El backend usa **Java 21**, **Spring Boot 4.1.1** (`pom.xml`), **Maven Wrapper
3.9.16**, **PostgreSQL 16** y **Flyway**. El flujo es igual para los tres integrantes:
crear una base local, copiar `.env.template` a `.env` y ejecutar Maven Wrapper.
No requiere un perfil Spring adicional, Fedora, Podman ni credenciales de proveedores.

## 1. Requisitos

- JDK 21 y `JAVA_HOME` apuntando a esa instalación; comprobar `java -version`.
- PostgreSQL 16, cliente `psql` y extensiones `btree_gist` y `unaccent` disponibles.
  En algunas distribuciones Linux las extensiones requieren el paquete contrib de PostgreSQL 16.
- Git. No hace falta instalar Maven globalmente: usar `mvnw` o `mvnw.cmd`.
- Internet en el primer uso del Wrapper para descargar Maven y las dependencias no cacheadas.

Ejecutar los comandos siguientes desde `backend/`, dentro del repositorio clonado.

## 2. PostgreSQL local

Elegir **una** de estas opciones. La aplicación utiliza por defecto
`127.0.0.1:5433`, base `jaldishop_dev`, usuario `jaldishop`.

### Opción A: PostgreSQL 16 instalado en el equipo

Iniciar el servicio PostgreSQL instalado (en Servicios de Windows o mediante el
gestor de servicios de Linux). No hay un nombre de servicio universal entre instaladores.
Conectar con una cuenta administradora local usando el puerto de esa instalación:

```text
psql -h 127.0.0.1 -p 5432 -U postgres -d postgres
```

Solo para una **base nueva** y un usuario que todavía no existan:

```sql
CREATE USER jaldishop WITH PASSWORD 'solo_desarrollo_local';
CREATE DATABASE jaldishop_dev OWNER jaldishop;
```

Salir con `\q`. Ajustar `DB_URL` en `.env` al puerto real, por ejemplo 5432.
Si los nombres ya existen, revisar su uso antes de hacer cambios: no borrar,
reutilizar ni ejecutar pruebas destructivas sobre una base compartida.

Flyway crea `btree_gist` en V14 y `unaccent` en V26. El usuario local debe ser
propietario de la base y tener permiso para crear estas extensiones. Si el instalador
lo restringe, el administrador local puede habilitarlas en **esa base nueva**:

```text
psql -h 127.0.0.1 -p 5432 -U postgres -d jaldishop_dev
```

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;
```

La aplicación no necesita ejecutarse con el superusuario `postgres`.

### Opción B: Docker, si ya está disponible

El mismo comando funciona en PowerShell y Bash. Crea un contenedor y volumen
nuevos exclusivos para desarrollo; comprobar que esos nombres no estén en uso:

```text
docker run --name jaldishop-dev-db -e POSTGRES_DB=jaldishop_dev -e POSTGRES_USER=jaldishop -e POSTGRES_PASSWORD=solo_desarrollo_local -p 127.0.0.1:5433:5432 -v jaldishop-dev-data:/var/lib/postgresql/data -d postgres:16
```

En siguientes sesiones basta con:

```text
docker start jaldishop-dev-db
```

Esta alternativa usa la cuenta inicial de una instancia aislada. La contraseña
mostrada es pública y solo sirve como ejemplo local. No publicar el puerto en la red.
Cambiarla tanto en la base como en `.env` si se elige otra contraseña.

## 3. Crear `.env` e iniciar

La copia es **solo para un clon nuevo sin `.env`**. Si ya existe, conservarlo y
revisar manualmente sus valores: no sobrescribirlo ni asumir que apunta a localhost.
El archivo es privado y está ignorado por Git.

### Windows PowerShell

Desde la raíz del repositorio:

```powershell
cd backend
java -version
if (-not (Test-Path .env)) { Copy-Item .env.template .env }
notepad .env
.\mvnw.cmd --version
.\mvnw.cmd spring-boot:run
```

### Linux

Desde la raíz del repositorio:

```bash
cd backend
java -version
if [ ! -e .env ]; then cp .env.template .env; fi
nano .env
./mvnw --version
./mvnw spring-boot:run
```

Revisar DB_URL, DB_USERNAME y DB_PASSWORD. Reemplazar JWT_SECRET por un secreto
aleatorio propio; JwtService usa sus bytes UTF-8 y necesita al menos 32 bytes.
Recomendamos 64 caracteres hexadecimales. Generarlo con:

PowerShell:

```powershell
([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
```

Linux:

```bash
od -An -N32 -tx1 /dev/urandom | tr -d ' \n'
```

Pegar el resultado en JWT_SECRET. La clave de la plantilla es un ejemplo público
que permite iniciar localmente, no un secreto para compartir sesiones.
La duración JWT ya está fijada en el código de configuración a una hora; no hace
falta inventar otra variable para autenticación.

Usar líneas simples `CLAVE=valor`, sin `export` ni comillas; el archivo también se
importa como Java properties. Para contraseñas locales evitar barras inversas y
escapes que puedan interpretarse de forma diferente por ambos lectores.
`BackendApplication` carga `.env`; las variables ya definidas en el proceso tienen
prioridad sobre los valores dotenv. Revisar/eliminar de la terminal cualquier
configuración heredada de base, proveedor o perfil antes de iniciar.
No activar `SPRING_PROFILES_ACTIVE=qa`.

La API escucha en `http://127.0.0.1:8080`; comprobar
`http://127.0.0.1:8080/actuator/health`. Detener con Ctrl+C.
Para acceder a los frontends locales desde el navegador, usar `localhost`, que está
permitido en CORS. El backend requiere PostgreSQL activo antes de iniciar.

## 4. Migraciones

Flyway aplica automáticamente los **25 scripts existentes hasta V26** al iniciar.
No hay V9 en el árbol actual; no renumerar ni crear una migración para llenar el hueco.
Se conserva `ddl-auto=none`: Hibernate no crea ni actualiza el esquema.
La plantilla activa validación de checksums. Ante un error de checksum, investigar
la diferencia; no editar migraciones aplicadas ni usar `repair` para ocultarla.
Las migraciones incluyen datos iniciales, pero no todos los fixtures de Bruno/E2E.
No ejecutar seeds adicionales para el arranque normal.

## 5. Pruebas opcionales

Para compilar sin ejecutar pruebas:

```text
# PowerShell
.\mvnw.cmd -DskipTests compile
# Linux
./mvnw -DskipTests compile
```

Estas unitarias focalizadas no requieren JDBC ni servicios externos:

```powershell
.\mvnw.cmd "-Dtest=LocalDevelopmentConfigurationTest,CustomerAddressTest,CreateCustomerAddressServiceTest,UpdateCustomerAddressServiceTest,TestPostgresConfigTest" test
```

```bash
./mvnw '-Dtest=LocalDevelopmentConfigurationTest,CustomerAddressTest,CreateCustomerAddressServiceTest,UpdateCustomerAddressServiceTest,TestPostgresConfigTest' test
```

Es un subconjunto, no toda la suite. **`mvn test` y `verify` también ejecutan
integraciones**, incluidas pruebas con fixtures destructivos; no usarlos como
sinónimo de unitarias.
Las integraciones generales necesitan variables del proceso (no basta `.env`):
DB_URL hacia `jaldishop_integration_test`, PHASE2A_DB_URL hacia
`jaldishop_fase2a_test`, DB_USERNAME y DB_PASSWORD. Ambas bases deben ser locales,
separadas de desarrollo y creadas expresamente para pruebas.
Se conserva `TestPostgresConfig` para impedir conexiones a otras bases.

Tres integraciones crean clústeres temporales con `initdb` y `postgres` mediante
ADDRESS_TEST_PG_BIN, STORE_CUSTOMERS_TEST_PG_BIN y STORE_SEARCH_TEST_PG_BIN.
Sin esas variables se omiten; su ejecución en Windows no está verificada.
El runner manual `SingleAttemptIsolatedReconciliationTest` usa configuración de
sandbox y requiere revisión independiente: no forma parte del desarrollo habitual.
No se ha eliminado ni habilitado automáticamente ninguna integración de negocio.

Si Mockito no puede adjuntarse a la JVM en un entorno restringido, eso es una
limitación de ese entorno; no requiere incorporar rutas personales de javaagent
al flujo del equipo. Usar un JDK 21 completo y revisar el mensaje del fallo.

## 6. Funciones externas opcionales

- **Mercado Pago:** token y secreto webhook vacíos son válidos para iniciar.
  La plantilla dirige el cliente a loopback y desactiva conciliación y reembolsos
  automáticos. Crear/consultar pagos y reembolsar requiere credenciales y no estará
  disponible con esta configuración. No existe un interruptor general ficticio
  `MERCADOPAGO_ENABLED`; no se están simulando cobros exitosos.
- **Cloudinary:** `demo` permite construir el bean. Subida y borrado de imágenes
  necesitan credenciales válidas y no deben invocarse con este entorno local.
- **Bruno/Postman:** se conservan las colecciones funcionales. Seleccionar un entorno
  local, configurar tokens/IDs privados y preparar los fixtures específicos por
  separado. No ejecutar colecciones contra una base compartida sin revisar sus escrituras.

## 7. Retiro de QA extraordinario

El arranque usa Maven Wrapper y `.env`. Se retiraron el componente `shared/config/qa`,
sus pruebas exclusivas, `application-qa.properties`, los SQL `db/qa`, los ejemplos de
allowlist y los dos lanzadores antiguos. Las colecciones de negocio bajo `qa/` siguen
[disponibles](qa/README.md). Los informes de auditoría existentes son históricos y no
son instrucciones vigentes para desarrollar.

El candidato aislado y su respaldo externo se conservan. En su directorio padre,
`plan-fedora-podman/`, `pg16-local/` y `fedora-it-*` quedan identificados como artefactos
para limpieza posterior; no son dependencias del proyecto. No ejecutar sus scripts
de preparación o limpieza como parte de este flujo.
