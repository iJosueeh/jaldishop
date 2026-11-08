> Arranque y unitarias actuales: [guía del backend](../../README.md). Los comandos de sandbox y resultados anteriores se conservan como historial; no son requisitos del equipo.

Auditoría y corrección de búsqueda pública — 2026-11-08

Estado: implementación terminada y SQL validado sobre PostgreSQL real. La ejecución integral HTTP → servicio → adaptador → JPA → PostgreSQL sigue pendiente: el sandbox impide crear sockets (`SocketException: Operation not permitted`). No presentar las pruebas HTTP con dobles como sustituto de esa validación.

Causa y alcance

Se revisaron StoreController, SearchPublicStoresService, StoreRepository, StoreRepositoryAdapter, StoreJpaRepository, entidades, tests, pom.xml y migraciones. La rama es feature/mercadopago-orders-api.

El controlador valida q (máximo 100 caracteres) y limit (1–50, predeterminado 5). El adaptador recorta espacios y convierte null/blanco a null. El servicio público fija ACTIVE. La consulta compartida busca nombre o slug, ordena por createdAt DESC y carga categorías mediante EntityGraph. El límite se aplica después, en memoria. Nada de esto se cambió.

LOWER conservaba las tildes. `pasteleria` coincidía con el slug pasteleria-delicia, pero no con el nombre de dulce-deleite. Los parámetros ya estaban enlazados; no había concatenación de SQL ejecutable. Sin embargo, concatenar `%` alrededor del parámetro lo convertía en un patrón LIKE: `%` y `_` ampliaban las coincidencias. El adaptador tampoco escapaba `\`; el SQL PostgreSQL sin ESCAPE explícito utiliza la barra como escape. No se ejecutó el endpoint antiguo contra JDBC en este entorno; no se atribuye al endpoint una prueba SQL directa del tratamiento de la barra.

Las utilidades existentes (SlugUtils, CreateStoreService y UbigeoCatalogService) normalizan para otros fines y pueden eliminar símbolos o cambiar espacios. Normalizar solo el término tampoco corrige los nombres almacenados. No se reutilizaron.

Decisión

Usar LOWER(unaccent(...)) tanto para el término como para nombre y slug, con LOCATE (subcadena literal; PostgreSQL POSITION). Los CAST permiten que Hibernate tipifique el resultado de la función. No hay patrones ni escape especial: %, _ y \ son texto. Los parámetros siguen enlazados. No se busca descripción, categorías ni productos, y no se modifican datos almacenados.

La corrección se hace en la consulta compartida: el listado administrativo recibe también equivalencia de acentos y símbolos literales, conservando su filtro de estado opcional. La búsqueda pública conserva exclusivamente ACTIVE; CLOSED, INACTIVE y SUSPENDED quedan fuera. /featured mantiene su consulta vacía y su límite predeterminado 6.

Se descartaron un mapa manual con translate (cobertura parcial y mantenimiento de caracteres), normalización solo Java (asimétrica), columnas normalizadas (sincronización/backfill innecesarios) e índices nuevos (sin evidencia de necesidad). No se agregaron dependencias; el proyecto ya tiene Spring Boot 4.1.1, Hibernate 7.4.5, JDBC PostgreSQL y Flyway PostgreSQL. No tiene Testcontainers.

Migración nueva: src/main/resources/db/migration/V26__enable_unaccent_for_store_search.sql. Instala unaccent en public, sin alterar migraciones anteriores. PostgreSQL local requiere tener instalado el módulo contrib y permisos CREATE para esta extensión trusted. La configuración presupone public.unaccent; una extensión previamente instalada en otro esquema requiere revisar esa configuración antes de aplicar esta migración. No se mueve automáticamente una extensión existente.

Referencias oficiales: [PostgreSQL unaccent](https://www.postgresql.org/docs/current/unaccent.html), [soporte de Neon](https://neon.com/docs/extensions/unaccent). Solo se consultó documentación pública; no hubo conexión a cuentas, consolas ni bases de Neon/Render.

Rendimiento y límites

La búsqueda anterior `%término%` sobre LOWER ya carecía de acceso útil por B-tree para subcadenas. POSITION y unaccent conservan ese coste de recorrido y añaden normalización por fila. Se ejecutó EXPLAIN (ANALYZE, BUFFERS): recorrido secuencial y ordenamiento sobre 10 fixtures, 2 resultados. Esto no es una medición de capacidad productiva. No se añadieron índices, wrappers IMMUTABLE ni columnas. Si el volumen lo exige, medir con datos representativos antes de evaluar trigramas y paginación en SQL. El límite en memoria sigue cargando todas las coincidencias y sus categorías; es una limitación previa.

unaccent aplica su diccionario: además de vocales acentuadas, puede equiparar ñ/n y expandir ligaduras. No ofrece tolerancia a errores tipográficos ni búsqueda lingüística. Se validó PostgreSQL 18.6, no la versión ni la configuración de servidores remotos.

Pruebas y evidencia

- StoreControllerTest: 10 pruebas aprobadas; incluye limit=0, limit=51, q excesivo, [] con 200 y limit=1.
- GetAdminStoresServiceTest: 3 pruebas aprobadas.
- StoreSearchQueryTest: 1 prueba aprobada. Hibernate compila el HQL real y acepta parámetros tipados; no conecta a ninguna base.
- verify-postgres.py: clúster nuevo PostgreSQL 18.6, modo monousuario, V1–V26 aplicadas sin errores, reproducción del fallo anterior y 21 verificaciones SQL aprobadas. Incluye acentos precompuestos/descompuestos, mayúsculas, panadería, dulce, slug, estados, ausencia de resultados, símbolos literales, texto parecido a SQL, consulta null/vacía, límite SQL y filtro administrativo. Los fixtures se crean en una tabla temporal que oculta la tabla stores del clúster nuevo. El clúster se elimina al finalizar. La migración V26 se ejecutó como SQL; no se ejecutó el motor Flyway contra JDBC.
- StoreSearchPostgresIntegrationTest: prueba sin repositorios simulados para HTTP/servicio/adaptador/JPA y PostgreSQL desechable. Intento habilitado: falla al crear ServerSocket, antes de iniciar el servidor. Sigue pendiente su ejecución en un entorno con sockets habilitados. Sin variable de entorno se omite explícitamente (8 métodos); esas omisiones no cuentan como aprobaciones.
- Bruno: 21 casos preparados, no ejecutados; no hay CLI Bruno ni servidor autorizado accesible en este sandbox.
- Diff de estos cambios sin errores de espacios. El diff global reporta una línea en blanco final preexistente en frontend-merchant/src/app/core/models/order.models.ts; no se tocó.
- Las 49 modificaciones rastreadas heredadas se compararon byte a byte con su diff inicial y permanecen intactas, incluidas direcciones y Mercado Pago. No se hizo commit, push, merge ni deploy.

Repetir desde backend, con una instalación PostgreSQL local completa y desechable:

```bash
STORE_SEARCH_TEST_PG_BIN=/ruta/a/postgresql/bin python3 qa/public-store-search/verify-postgres.py
STORE_SEARCH_TEST_PG_BIN=/ruta/a/postgresql/bin mvn -Dtest=StoreSearchPostgresIntegrationTest test
mvn -Dtest=StoreControllerTest,GetAdminStoresServiceTest,StoreSearchQueryTest test
```

No usar DB_URL, perfiles de aplicación ni bases existentes. El test crea su propio directorio, puerto y clúster. Para los tests Mockito en este sandbox fue necesario cargar su agente al arrancar:

```bash
mvn -DargLine=-javaagent:/home/ijosueeh/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dtest=StoreControllerTest,GetAdminStoresServiceTest,StoreSearchQueryTest,StoreSearchPostgresIntegrationTest test
```

Casos de Bruno

Abrir esta carpeta como colección, seleccionar Local disposable y apuntar baseUrl únicamente a una instancia local desechable preparada para este caso. Las solicitudes son GET públicos y codifican los símbolos en la URL (%25, %5C). No escriben datos.

Las aserciones exactas requieren este dataset de prueba, equivalente al de StoreSearchPostgresIntegrationTest. Prepararlo únicamente en una instancia desechable, sin modificar tiendas existentes. No ejecutar la colección contra Neon o Render. Si hay otras tiendas, adaptar las expectativas antes de interpretar resultados.

| Nombre | Slug | Estado |
| --- | --- | --- |
| Pastelería Delicia | pasteleria-delicia | ACTIVE |
| Pastelería Dulce Deleite | dulce-deleite | ACTIVE |
| Panadería Norte | pan-del-norte | ACTIVE |
| PANADERÍA Sur | horno-sur | ACTIVE |
| Oferta 50% real | oferta-porcentaje | ACTIVE |
| Tienda_A | guion-bajo | ACTIVE |
| Ruta \ centro | ruta-central | ACTIVE |
| Pastelería Oculta | pasteleria-inactiva | INACTIVE |
| Pastelería Suspendida | pasteleria-suspendida | SUSPENDED |
| Pastelería Cerrada | pasteleria-cerrada | CLOSED |

Todas tienen descripción `busquedasecreta` (debe devolver []). La fecha de creación de Dulce Deleite es posterior a Delicia para verificar limit=1. q vacío con limit=50 devuelve las siete activas; sin q y con limit=5 devuelve cinco. Los casos 01–03 y 20 exigen las dos pastelerías; 04–05 las dos panaderías; 06–07 filtran nombre/slug; 08 devuelve []; 09–12 tratan símbolos literalmente; 13 excluye descripción; 14–15 conservan blanco; 16–17 exigen 400 VALIDATION_ERROR; 18 limita a uno; 19 prueba q omitido; 21 trata texto parecido a SQL como literal.
