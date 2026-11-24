import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matGavelOutline,
  matDescriptionOutline,
  matBadgeOutline,
  matEditNoteOutline,
  matSaveOutline,
  matWandStarsOutline,
} from '@ng-icons/material-symbols/outline';

const INDECOPI_CANCELLATION_TEMPLATE =
  'Puedes solicitar la cancelación gratuita de tu pedido mientras se encuentre en estado "Confirmado" antes de entrar a preparación o despacho.';

const INDECOPI_REFUND_TEMPLATE =
  'Conforme a la normativa de INDECOPI (Código de Protección y Defensa del Consumidor), ante fallas comprobadas en el producto, retraso no justificado o error en el despacho, se gestionará el reembolso inmediato o cambio de producto dentro de las 24 horas siguientes.';

@Component({
  selector: 'app-settings-policies-tab',
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matGavelOutline,
      matDescriptionOutline,
      matBadgeOutline,
      matEditNoteOutline,
      matSaveOutline,
      matWandStarsOutline,
    }),
  ],
  templateUrl: './settings-policies-tab.html',
  styleUrl: './settings-policies-tab.css',
})
export class SettingsPoliciesTab {
  readonly settingsService = inject(SettingsService);
  private readonly storeService = inject(StoreService);

  readonly cancellationPolicy = signal<string>(
    this.settingsService.settings().cancellationPolicy
  );
  readonly refundPolicy = signal<string>(
    this.settingsService.settings().refundPolicy
  );
  readonly requireDni = signal<boolean>(
    this.settingsService.settings().requireCustomerDni
  );
  readonly allowNotes = signal<boolean>(
    this.settingsService.settings().allowOrderNotes
  );

  constructor() {
    effect(() => {
      const s = this.settingsService.settings();
      this.cancellationPolicy.set(s.cancellationPolicy);
      this.refundPolicy.set(s.refundPolicy);
      this.requireDni.set(s.requireCustomerDni);
      this.allowNotes.set(s.allowOrderNotes);
    });
  }

  applyIndecopiCancellationTemplate(): void {
    this.cancellationPolicy.set(INDECOPI_CANCELLATION_TEMPLATE);
  }

  applyIndecopiRefundTemplate(): void {
    this.refundPolicy.set(INDECOPI_REFUND_TEMPLATE);
  }

  savePolicies(): void {
    const current = this.settingsService.settings();
    const storeId = this.storeService.currentStore()?.id;

    this.settingsService.saveSettings(
      {
        ...current,
        cancellationPolicy: this.cancellationPolicy().trim(),
        refundPolicy: this.refundPolicy().trim(),
        requireCustomerDni: this.requireDni(),
        allowOrderNotes: this.allowNotes(),
      },
      storeId
    );
  }
}
