import { Component, computed, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matWarningOutline,
  matDeleteOutline,
  matInfoOutline,
  matCheckCircleOutline,
  matPauseCircleOutline,
} from '@ng-icons/material-symbols/outline';

export interface StoreStatusInfo {
  label: string;
  badgeClass: string;
  dotClass: string;
  description: string;
}

@Component({
  selector: 'app-store-danger-zone-card',
  standalone: true,
  imports: [NgIcon],
  providers: [
    provideIcons({
      matWarningOutline,
      matDeleteOutline,
      matInfoOutline,
      matCheckCircleOutline,
      matPauseCircleOutline,
    }),
  ],
  templateUrl: './store-danger-zone-card.html',
  styleUrl: './store-danger-zone-card.css',
})
export class StoreDangerZoneCard {
  readonly status = input<string>('ACTIVE');
  readonly isClosing = input<boolean>(false);
  readonly requestClose = output<void>();

  readonly statusInfo = computed<StoreStatusInfo>(() => {
    const raw = (this.status() || '').toUpperCase();
    switch (raw) {
      case 'ACTIVE':
        return {
          label: 'Activa y Operativa',
          badgeClass: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20',
          dotClass: 'bg-emerald-500 animate-pulse',
          description: 'Tu catálogo se encuentra publicado y abierto para recibir órdenes de tus clientes.',
        };
      case 'INACTIVE':
        return {
          label: 'Inactiva / Pausada',
          badgeClass: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20',
          dotClass: 'bg-amber-500',
          description: 'Tu tienda se encuentra temporalmente oculta al público. Los pedidos y registros están a salvo.',
        };
      case 'SUSPENDED':
        return {
          label: 'Suspendida por Moderación',
          badgeClass: 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border border-rose-500/20',
          dotClass: 'bg-rose-500',
          description: 'El acceso a la tienda ha sido restringido de forma temporal por el equipo de administración.',
        };
      case 'CLOSED':
        return {
          label: 'Clausurada',
          badgeClass: 'bg-slate-500/10 text-slate-700 dark:text-slate-400 border border-slate-500/20',
          dotClass: 'bg-slate-500',
          description: 'La tienda ha cesado sus operaciones de forma definitiva.',
        };
      default:
        return {
          label: 'En Configuración',
          badgeClass: 'bg-surface-container text-on-surface-variant border border-outline-variant/30',
          dotClass: 'bg-outline-variant',
          description: 'El estado del negocio se encuentra en proceso de validación o ajuste.',
        };
    }
  });

  onRequestClose(): void {
    if (!this.isClosing()) {
      this.requestClose.emit();
    }
  }
}
