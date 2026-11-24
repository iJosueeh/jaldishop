import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { OrderStatus } from '../models/order.models';

export interface OrderCreatedBusEvent {
  storeId: string;
  orderId?: string;
  orderNumber?: string;
  origin?: string;
  source: 'WEBSOCKET' | 'LOCAL_MERCHANT';
}

export interface OrderMutatedBusEvent {
  storeId: string;
  orderId: string;
  status: OrderStatus;
  previousStatus?: OrderStatus;
  source: 'WEBSOCKET' | 'LOCAL_MERCHANT' | 'OPTIMISTIC';
}

export interface InventoryChangedBusEvent {
  storeId: string;
  variantId?: string;
  reason?: string;
  source: 'WEBSOCKET' | 'LOCAL_MUTATION';
}

export interface CapacityUpdatedBusEvent {
  storeId: string;
  date?: string;
  reason?: string;
  source: 'WEBSOCKET' | 'ORDER_MUTATION';
}

export interface ReconnectedBusEvent {
  storeId: string;
}

export interface SettlementRequestedBusEvent {
  storeId: string;
  batchId: string;
  amount: number;
}

export interface StoreProfileMutatedBusEvent {
  storeId: string;
}

export type CacheModule = 'ORDERS' | 'INVENTORY' | 'CAPACITY' | 'ANALYTICS' | 'BILLING' | 'STORE' | 'SETTINGS' | 'AUDIT' | 'ALL';

export interface CacheInvalidatedBusEvent {
  storeId: string;
  module: CacheModule;
}

/**
 * Bus de eventos e invalidación reactiva singleton para sincronizar
 * OrderService, InventoryService, CapacityService, AnalyticsService y BillingService.
 */
@Injectable({
  providedIn: 'root',
})
export class InvalidationBusService {
  private readonly orderCreatedSubject = new Subject<OrderCreatedBusEvent>();
  readonly orderCreated$ = this.orderCreatedSubject.asObservable();

  private readonly orderMutatedSubject = new Subject<OrderMutatedBusEvent>();
  readonly orderMutated$ = this.orderMutatedSubject.asObservable();

  private readonly inventoryChangedSubject = new Subject<InventoryChangedBusEvent>();
  readonly inventoryChanged$ = this.inventoryChangedSubject.asObservable();

  private readonly capacityUpdatedSubject = new Subject<CapacityUpdatedBusEvent>();
  readonly capacityUpdated$ = this.capacityUpdatedSubject.asObservable();

  private readonly reconnectedSubject = new Subject<ReconnectedBusEvent>();
  readonly reconnected$ = this.reconnectedSubject.asObservable();

  private readonly settlementRequestedSubject = new Subject<SettlementRequestedBusEvent>();
  readonly settlementRequested$ = this.settlementRequestedSubject.asObservable();

  private readonly storeProfileMutatedSubject = new Subject<StoreProfileMutatedBusEvent>();
  readonly storeProfileMutated$ = this.storeProfileMutatedSubject.asObservable();

  private readonly cacheInvalidatedSubject = new Subject<CacheInvalidatedBusEvent>();
  readonly cacheInvalidated$ = this.cacheInvalidatedSubject.asObservable();

  dispatchOrderCreated(event: OrderCreatedBusEvent): void {
    this.orderCreatedSubject.next(event);
  }

  dispatchOrderMutated(event: OrderMutatedBusEvent): void {
    this.orderMutatedSubject.next(event);
  }

  dispatchInventoryChanged(event: InventoryChangedBusEvent): void {
    this.inventoryChangedSubject.next(event);
  }

  dispatchCapacityUpdated(event: CapacityUpdatedBusEvent): void {
    this.capacityUpdatedSubject.next(event);
  }

  dispatchReconnected(storeId: string): void {
    this.reconnectedSubject.next({ storeId });
  }

  dispatchSettlementRequested(event: SettlementRequestedBusEvent): void {
    this.settlementRequestedSubject.next(event);
  }

  dispatchStoreProfileMutated(event: StoreProfileMutatedBusEvent): void {
    this.storeProfileMutatedSubject.next(event);
  }

  dispatchCacheInvalidated(event: CacheInvalidatedBusEvent): void {
    this.cacheInvalidatedSubject.next(event);
  }

  invalidateAll(storeId: string): void {
    this.cacheInvalidatedSubject.next({
      storeId,
      module: 'ALL',
    });
  }
}
