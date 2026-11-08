# Colecciones funcionales locales

Este directorio conserva las colecciones Bruno de negocio:

- [Direcciones duplicadas](customer-address-duplicates/README.md).
- [Clientes del comerciante](merchant-customers/README.md).
- [Búsqueda pública de tiendas](public-store-search/README.md).

El componente QA extraordinario fue retirado. No existe un perfil `qa`, preparador,
allowlist ni requisito de Fedora/Podman. El arranque habitual está en
[la guía del backend](../README.md).

Seleccionar el entorno local de cada colección y configurar sus variables privadas.
Las colecciones necesitan sus fixtures específicos en una base local desechable;
no suponer que las migraciones crean todos esos datos. Las pruebas Java que crean
clústeres temporales no preparan la base utilizada por la API de Bruno.
Los archivos `.bru` y sus entornos se conservan sin cambios.

Los resultados y comandos de sandbox en las guías de cada corrección son históricos.
Para las unitarias actuales, usar la guía del backend. Las pruebas PostgreSQL que
requieren `*_TEST_PG_BIN` siguen siendo opcionales y su ejecución en Windows no está
verificada.
