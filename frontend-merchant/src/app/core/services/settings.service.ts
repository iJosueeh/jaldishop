import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MerchantSettings } from '../models/settings.models';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { environment } from '../../../environments/environment';
import { catchError, of, tap } from 'rxjs';

export const DEFAULT_SETTINGS: MerchantSettings = {
  cancellationPolicy: 'Puedes solicitar la cancelación gratuita de tu pedido mientras se encuentre en estado Confirmado antes de entrar a preparación.',
  refundPolicy: 'Conforme a la normativa de INDECOPI, ante fallas o inconsistencias comprobadas se procederá con el reenvío o reembolso del producto dentro de las 24 horas.',
  requireCustomerDni: false,
  allowOrderNotes: true,
  onlinePaymentsEnabled: true,
  cashOnDeliveryEnabled: true,
  acceptsPosOnDelivery: false,
  minDeliveryAmount: null,
  estimatedPrepTimeMinutes: 30,
  orderSoundAlert: true,
  soundVolume: 80,
  lowStockVisualAlert: true,
};

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly toastService = inject(ToastService);
  private readonly invalidationBus = inject(InvalidationBusService);

  private readonly apiUrl = `${environment.apiUrl}/api/v1/stores/me/settings`;

  readonly settings = signal<MerchantSettings>(DEFAULT_SETTINGS);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);

  loadSettings(): void {
    this.isLoading.set(true);
    this.http.get<MerchantSettings>(this.apiUrl).pipe(
      tap((data) => {
        this.settings.set(data);
        this.isLoading.set(false);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe();
  }

  saveSettings(newSettings: MerchantSettings, storeId?: string): void {
    const previous = this.settings();
    // Rollback optimista: asignamos inmediatamente en memoria
    this.settings.set(newSettings);
    this.isSaving.set(true);

    this.http.put<MerchantSettings>(this.apiUrl, newSettings).pipe(
      tap((saved) => {
        this.settings.set(saved);
        this.isSaving.set(false);
        this.toastService.success('Configuración guardada correctamente.');

        if (storeId) {
          this.invalidationBus.dispatchCacheInvalidated({ storeId, module: 'STORE' });
        }
      }),
      catchError((err) => {
        // Rollback a estado anterior en caso de falla
        this.settings.set(previous);
        this.isSaving.set(false);
        this.toastService.error('Error al guardar la configuración. Se restauraron los cambios.');
        return of(null);
      })
    ).subscribe();
  }

  updateSettingsLocally(partial: Partial<MerchantSettings>): void {
    this.settings.update((current) => ({ ...current, ...partial }));
  }

  playNotificationSound(): void {
    const prefs = this.settings();
    if (!prefs.orderSoundAlert) return;

    try {
      const audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();

      const volume = (prefs.soundVolume ?? 80) / 100;
      gainNode.gain.setValueAtTime(volume * 0.3, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.001, audioContext.currentTime + 0.8);

      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(587.33, audioContext.currentTime); // D5
      oscillator.frequency.exponentialRampToValueAtTime(880.0, audioContext.currentTime + 0.3); // A5

      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);

      oscillator.start();
      oscillator.stop(audioContext.currentTime + 0.8);
    } catch {
      // AudioContext no disponible o bloqueado por autoplay policy
    }
  }
}
