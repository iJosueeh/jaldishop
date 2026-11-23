import { describe, it, expect, beforeEach } from 'vitest';
import {
  InvalidationBusService,
  OrderCreatedBusEvent,
  OrderMutatedBusEvent,
  InventoryChangedBusEvent,
  CapacityUpdatedBusEvent,
  CacheInvalidatedBusEvent,
} from './invalidation-bus.service';

describe('InvalidationBusService', () => {
  let service: InvalidationBusService;

  beforeEach(() => {
    service = new InvalidationBusService();
  });

  it('debe crearse correctamente como servicio singleton', () => {
    expect(service).toBeTruthy();
  });

  it('debe emitir y recibir eventos de creación de pedido (orderCreated$)', () => {
    const emitted: OrderCreatedBusEvent[] = [];
    const sub = service.orderCreated$.subscribe((event) => emitted.push(event));

    const payload: OrderCreatedBusEvent = {
      storeId: 'store-123',
      orderId: 'ord-001',
      orderNumber: 'ORD-2026-0001',
      source: 'LOCAL_MERCHANT',
    };

    service.dispatchOrderCreated(payload);

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual(payload);
    sub.unsubscribe();
  });

  it('debe emitir y recibir eventos de mutación de pedido (orderMutated$)', () => {
    const emitted: OrderMutatedBusEvent[] = [];
    const sub = service.orderMutated$.subscribe((event) => emitted.push(event));

    const payload: OrderMutatedBusEvent = {
      storeId: 'store-123',
      orderId: 'ord-001',
      status: 'IN_PREPARATION',
      previousStatus: 'CONFIRMED',
      source: 'OPTIMISTIC',
    };

    service.dispatchOrderMutated(payload);

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual(payload);
    sub.unsubscribe();
  });

  it('debe emitir y recibir eventos de inventario (inventoryChanged$)', () => {
    const emitted: InventoryChangedBusEvent[] = [];
    const sub = service.inventoryChanged$.subscribe((event) => emitted.push(event));

    const payload: InventoryChangedBusEvent = {
      storeId: 'store-123',
      variantId: 'var-999',
      reason: 'RESTOCK',
      source: 'LOCAL_MUTATION',
    };

    service.dispatchInventoryChanged(payload);

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual(payload);
    sub.unsubscribe();
  });

  it('debe emitir y recibir eventos de capacidad (capacityUpdated$)', () => {
    const emitted: CapacityUpdatedBusEvent[] = [];
    const sub = service.capacityUpdated$.subscribe((event) => emitted.push(event));

    const payload: CapacityUpdatedBusEvent = {
      storeId: 'store-123',
      date: '2026-11-20',
      reason: 'ORDER_ASSIGNED',
      source: 'ORDER_MUTATION',
    };

    service.dispatchCapacityUpdated(payload);

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual(payload);
    sub.unsubscribe();
  });

  it('debe emitir y recibir eventos de reconexión WebSocket (reconnected$)', () => {
    const emitted: string[] = [];
    const sub = service.reconnected$.subscribe((event) => emitted.push(event.storeId));

    service.dispatchReconnected('store-123');

    expect(emitted).toEqual(['store-123']);
    sub.unsubscribe();
  });

  it('debe emitir y recibir eventos de invalidación selectiva de módulo (cacheInvalidated$)', () => {
    const emitted: CacheInvalidatedBusEvent[] = [];
    const sub = service.cacheInvalidated$.subscribe((event) => emitted.push(event));

    const payload: CacheInvalidatedBusEvent = {
      storeId: 'store-123',
      module: 'ANALYTICS',
    };

    service.dispatchCacheInvalidated(payload);

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual(payload);
    sub.unsubscribe();
  });

  it('debe permitir invalidar todos los módulos de forma broadcast', () => {
    const emitted: CacheInvalidatedBusEvent[] = [];
    const sub = service.cacheInvalidated$.subscribe((event) => emitted.push(event));

    service.invalidateAll('store-123');

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual({
      storeId: 'store-123',
      module: 'ALL',
    });
    sub.unsubscribe();
  });
});
