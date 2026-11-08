import { inject, Injectable, OnDestroy, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OrderConfirmedDomainEvent, StompConnectionState } from '../models/order.models';
import { TokenService } from './token-service';

/**
 * Convierte la URL base de la API HTTP REST en la URL correspondiente
 * del endpoint WebSocket STOMP nativo (/ws).
 */
export function getWebSocketUrl(apiUrl: string): string {
  const parsed = new URL(apiUrl);
  const protocol = parsed.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${parsed.host}/ws`;
}

/**
 * Servicio encargado de gestionar la conexión WebSocket/STOMP en tiempo real
 * con el backend de JaldiShop.
 *
 * Características:
 * - Utiliza WebSocket nativo (sin SockJS) sobre el endpoint /ws.
 * - Inyecta dinámicamente el token JWT en el frame STOMP CONNECT.
 * - Evita bucles infinitos de reconexión si el token es inválido o expiró (AUTH_ERROR).
 * - Suscribe exclusivamente al topic /topic/stores/{storeId}/orders.
 * - Emite eventos cuando un pedido es confirmado (ORDER_CONFIRMED).
 * - Notifica reconexiones para activar la reconciliación REST en OrderService.
 */
@Injectable({
  providedIn: 'root',
})
export class StompService implements OnDestroy {
  private readonly tokenService = inject(TokenService);

  private client: Client | null = null;
  private orderSubscription: StompSubscription | null = null;
  private subscribedStoreId: string | null = null;
  private isIntentionalDisconnect = false;
  private hadPreviousConnection = false;

  readonly connectionState = signal<StompConnectionState>('DISCONNECTED');
  readonly isSubscribed = signal<boolean>(false);
  readonly currentStoreId = signal<string | null>(null);

  private readonly orderConfirmedSubject = new Subject<OrderConfirmedDomainEvent>();
  readonly orderConfirmed$ = this.orderConfirmedSubject.asObservable();

  private readonly reconnectedSubject = new Subject<void>();
  readonly reconnected$ = this.reconnectedSubject.asObservable();

  /**
   * Permite inyectar o sustituir la instancia del cliente STOMP (útil para pruebas unitarias).
   */
  setCustomClient(customClient: Client | null): void {
    this.client = customClient;
  }

  getClient(): Client | null {
    return this.client;
  }

  /**
   * Inicia la conexión STOMP hacia el servidor y prepara la suscripción para storeId si se indica.
   */
  connect(storeId?: string): void {
    if (storeId) {
      this.subscribedStoreId = storeId;
      this.currentStoreId.set(storeId);
    }

    const token = this.tokenService.getToken();
    if (!token || this.tokenService.isTokenExpired(token)) {
      this.connectionState.set('AUTH_ERROR');
      return;
    }

    if (this.client?.active) {
      if (this.subscribedStoreId && (!this.orderSubscription || this.currentStoreId() !== this.subscribedStoreId)) {
        this.subscribeToStore(this.subscribedStoreId);
      }
      return;
    }

    this.isIntentionalDisconnect = false;
    this.connectionState.set('CONNECTING');

    const brokerURL = getWebSocketUrl(environment.apiUrl);

    this.client = new Client({
      brokerURL,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      beforeConnect: () => {
        const freshToken = this.tokenService.getToken();
        if (!freshToken || this.tokenService.isTokenExpired(freshToken)) {
          this.connectionState.set('AUTH_ERROR');
          this.client?.deactivate();
          return;
        }
        this.client!.connectHeaders = {
          Authorization: `Bearer ${freshToken}`,
        };
      },
      onConnect: () => {
        const wasReconnection = this.hadPreviousConnection;
        this.hadPreviousConnection = true;
        this.connectionState.set('CONNECTED');

        if (this.subscribedStoreId) {
          this.subscribeToStore(this.subscribedStoreId);
        }

        if (wasReconnection) {
          this.reconnectedSubject.next();
        }
      },
      onDisconnect: () => {
        this.isSubscribed.set(false);
        this.orderSubscription = null;
        if (this.isIntentionalDisconnect) {
          this.connectionState.set('DISCONNECTED');
        }
      },
      onStompError: (frame) => {
        const errorMsg = (frame.headers['message'] || frame.body || '').toLowerCase();
        if (
          errorMsg.includes('unauthorized') ||
          errorMsg.includes('credentials') ||
          errorMsg.includes('token') ||
          errorMsg.includes('accessdenied') ||
          errorMsg.includes('401') ||
          errorMsg.includes('403')
        ) {
          this.connectionState.set('AUTH_ERROR');
          this.disconnect();
        }
      },
      onWebSocketClose: () => {
        this.isSubscribed.set(false);
        this.orderSubscription = null;
        if (!this.isIntentionalDisconnect && this.connectionState() !== 'AUTH_ERROR') {
          this.connectionState.set('RECONNECTING');
        }
      },
    });

    this.client.activate();
  }

  /**
   * Suscribe al topic /topic/stores/{storeId}/orders.
   */
  subscribeToStore(storeId: string): void {
    this.subscribedStoreId = storeId;
    this.currentStoreId.set(storeId);

    if (!this.client || !this.client.connected) {
      return;
    }

    if (this.orderSubscription) {
      this.orderSubscription.unsubscribe();
      this.orderSubscription = null;
      this.isSubscribed.set(false);
    }

    const destination = `/topic/stores/${storeId}/orders`;
    this.orderSubscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const payload: OrderConfirmedDomainEvent = JSON.parse(message.body);
        if (payload.eventType === 'ORDER_CONFIRMED' && payload.storeId === this.subscribedStoreId) {
          this.orderConfirmedSubject.next(payload);
        }
      } catch {
        // Ignorar mensajes mal formateados
      }
    });

    this.isSubscribed.set(true);
  }

  /**
   * Cancela la suscripción al topic de órdenes de la tienda.
   */
  unsubscribe(): void {
    if (this.orderSubscription) {
      this.orderSubscription.unsubscribe();
      this.orderSubscription = null;
    }
    this.subscribedStoreId = null;
    this.currentStoreId.set(null);
    this.isSubscribed.set(false);
  }

  /**
   * Desconecta limpiamente el cliente STOMP y cancela suscripciones activas.
   */
  disconnect(): void {
    this.isIntentionalDisconnect = true;
    this.unsubscribe();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    if (this.connectionState() !== 'AUTH_ERROR') {
      this.connectionState.set('DISCONNECTED');
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
