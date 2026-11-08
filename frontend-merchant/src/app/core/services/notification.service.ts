import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notification, UnreadCountResponse } from '../models/notification.models';
import { TokenService } from './token-service';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  private currentUserId: string | null = null;
  private requestSequence = 0;
  private unreadCountSequence = 0;
  private readonly inFlightPatchIds = signal<Set<string>>(new Set());

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = signal<number>(0);
  readonly isLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly hasUnread = computed(() => this.unreadCount() > 0);
  readonly unreadBadgeText = computed(() => {
    const count = this.unreadCount();
    if (count <= 0) return '';
    return count > 99 ? '99+' : `${count}`;
  });

  isMarkingRead(notificationId: string): boolean {
    return this.inFlightPatchIds().has(notificationId);
  }

  /**
   * Garantiza que el estado pertenezca estrictamente al usuario autenticado actual.
   * Si detecta que no hay token válido o que el usuario cambió respecto a la sesión anterior,
   * limpia de inmediato el caché y actualiza la referencia de identidad.
   */
  private ensureActiveSession(): string | null {
    const activeUserId = this.tokenService.getUserId();
    if (!activeUserId || !this.tokenService.hasValidToken()) {
      if (this.currentUserId !== null || this.notifications().length > 0 || this.unreadCount() > 0) {
        this.clearCache();
      }
      return null;
    }

    if (this.currentUserId !== null && this.currentUserId !== activeUserId) {
      this.clearCache();
    }

    this.currentUserId = activeUserId;
    return activeUserId;
  }

  getNotifications(page = 0, size = 20): Observable<Notification[]> {
    const activeUser = this.ensureActiveSession();
    if (!activeUser) {
      return of([]);
    }

    const currentSeq = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Notification[]>(this.baseUrl, { params }).pipe(
      tap({
        next: (items) => {
          if (currentSeq === this.requestSequence && this.tokenService.getUserId() === activeUser) {
            this.notifications.set(items || []);
            this.isLoading.set(false);
          }
        },
        error: (err: HttpErrorResponse) => {
          if (currentSeq === this.requestSequence && this.tokenService.getUserId() === activeUser) {
            const message = err?.error?.message || 'Error al cargar las notificaciones.';
            this.error.set(message);
            this.isLoading.set(false);
          }
        },
      }),
      catchError((err) => {
        if (currentSeq !== this.requestSequence || this.tokenService.getUserId() !== activeUser) {
          return of([]);
        }
        return throwError(() => err);
      }),
    );
  }

  getUnreadCount(): Observable<UnreadCountResponse> {
    const activeUser = this.ensureActiveSession();
    if (!activeUser) {
      return of({ unreadCount: 0 });
    }

    const currentSeq = ++this.unreadCountSequence;

    return this.http.get<UnreadCountResponse>(`${this.baseUrl}/unread/count`).pipe(
      tap({
        next: (res) => {
          if (currentSeq === this.unreadCountSequence && this.tokenService.getUserId() === activeUser) {
            if (res && typeof res.unreadCount === 'number') {
              this.unreadCount.set(Math.max(0, res.unreadCount));
            }
          }
        },
        error: () => {
          // Si falla en segundo plano no bloqueamos la interfaz
        },
      }),
      catchError((err) => {
        if (currentSeq !== this.unreadCountSequence || this.tokenService.getUserId() !== activeUser) {
          return of({ unreadCount: this.unreadCount() });
        }
        return throwError(() => err);
      }),
    );
  }

  markAsRead(notificationId: string): Observable<Notification> {
    if (!notificationId) {
      return throwError(() => new Error('Identificador de notificación inválido.'));
    }

    const activeUser = this.ensureActiveSession();
    if (!activeUser) {
      return throwError(() => new Error('Sesión no disponible para marcar notificación.'));
    }

    if (this.inFlightPatchIds().has(notificationId)) {
      const existing = this.notifications().find((n) => n.id === notificationId);
      if (existing) {
        return of(existing);
      }
      return of({
        id: notificationId,
        userId: activeUser,
        type: 'SYSTEM',
        title: '',
        message: '',
        status: 'READ',
        createdAt: new Date().toISOString(),
        readAt: new Date().toISOString(),
      });
    }

    this.inFlightPatchIds.update((ids) => {
      const next = new Set(ids);
      next.add(notificationId);
      return next;
    });

    return this.http.patch<Notification>(`${this.baseUrl}/${notificationId}/read`, {}).pipe(
      tap({
        next: (updated) => {
          if (this.tokenService.getUserId() === activeUser) {
            this.applyLocalReadStatus(notificationId, updated);
          }
        },
      }),
      catchError((err: HttpErrorResponse) => {
        // Manejo específico de NOTIFICATION_ALREADY_READ (422)
        if (err.status === 422 && err.error?.code === 'NOTIFICATION_ALREADY_READ') {
          const reconciled = this.reconcileAlreadyRead(notificationId);
          // Reconsulta autoritativa al backend: no decrementar a ciegas el contador local
          this.getUnreadCount().subscribe();
          return of(reconciled);
        }
        // No ocultar otros errores 422 (ej. VALIDATION_ERROR u otras reglas de negocio)
        return throwError(() => err);
      }),
      finalize(() => {
        this.inFlightPatchIds.update((ids) => {
          const next = new Set(ids);
          next.delete(notificationId);
          return next;
        });
      }),
    );
  }

  private applyLocalReadStatus(id: string, updatedFromServer: Notification): void {
    const list = this.notifications();
    const target = list.find((n) => n.id === id);
    const wasUnread = target ? target.status === 'UNREAD' : true;

    this.notifications.update((items) =>
      items.map((item) => (item.id === id ? updatedFromServer : item)),
    );

    if (wasUnread) {
      this.unreadCount.update((cnt) => Math.max(0, cnt - 1));
    }

    // Sincronización en segundo plano con el servidor
    this.getUnreadCount().subscribe();
  }

  private reconcileAlreadyRead(id: string): Notification {
    const list = this.notifications();
    const existing = list.find((n) => n.id === id);

    const nowIso = new Date().toISOString();
    const reconciled: Notification = existing
      ? { ...existing, status: 'READ', readAt: existing.readAt || nowIso }
      : {
          id,
          userId: this.currentUserId || '',
          type: 'SYSTEM',
          title: '',
          message: '',
          status: 'READ',
          createdAt: nowIso,
          readAt: nowIso,
        };

    this.notifications.update((items) =>
      items.map((item) => (item.id === id ? reconciled : item)),
    );

    // NOTA: No decrementamos unreadCount aquí; getUnreadCount() obtendrá el valor real del backend
    return reconciled;
  }

  clearCache(): void {
    this.requestSequence++;
    this.unreadCountSequence++;
    this.currentUserId = null;
    this.notifications.set([]);
    this.unreadCount.set(0);
    this.isLoading.set(false);
    this.error.set(null);
    this.inFlightPatchIds.set(new Set());
  }
}
