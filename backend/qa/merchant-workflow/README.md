# Flujo Merchant en Postman

Esta coleccion automatiza el recorrido de demostracion Merchant:

```text
health -> registrar merchant/tienda -> actualizar tienda -> categoria -> producto
-> variante -> inventario -> capacidad -> listar pedidos
```

## Seguridad y alcance

- Ejecutar solo con `baseUrl` local (`127.0.0.1` o `localhost`). La coleccion lo bloquea en cualquier otro host.
- Usa una base PostgreSQL local desechable. Cada ejecucion crea un usuario Merchant, tienda, categoria, producto, variante y configuracion de capacidad.
- No contiene credenciales reales. El email y el nombre de tienda se generan en cada ejecucion y el token se guarda solo en el entorno de Postman.
- No borra los datos creados: elimina la base de desarrollo completa solo si es una base local desechable y necesitas reiniciar el escenario.

## Ejecucion

1. Levantar PostgreSQL local y el backend desde `backend/` con `./mvnw spring-boot:run` o `./mvnw.cmd spring-boot:run`.
2. En Postman, importar `jaldishop-merchant-workflow.postman_collection.json`.
3. Importar `jaldishop-merchant-workflow.local.env.json` y seleccionarlo como entorno activo.
4. Abrir la coleccion, pulsar **Run collection** y ejecutar las carpetas en su orden numerico.
5. Verificar que todos los tests pasen. El request final de cambio de estado se muestra como omitido si `existingOrderId` esta vacio.

## Pedido opcional

Una tienda creada en esta coleccion no tiene pedidos: el pedido se crea solo tras el flujo Customer + checkout + pago aprobado.

Para probar la transicion de estado, prepara un pedido `CONFIRMED` que pertenezca a la tienda creada, asigna su UUID a `existingOrderId` y ejecuta solo `04 - Pedidos/02 - Cambiar estado de pedido existente (opcional)`. El valor inicial `IN_PREPARATION` de `nextOrderStatus` es valido solo desde `CONFIRMED`.

No uses IDs de una tienda ajena ni un pedido con una transicion invalida: el backend debe rechazarlo.
