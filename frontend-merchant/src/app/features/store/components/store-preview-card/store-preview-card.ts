import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPreviewOutline,
  matStorefrontOutline,
  matStoreOutline,
  matMopedOutline,
  matCheckOutline,
  matContentCopyOutline,
  matPowerSettingsNewOutline,
  matPauseCircleOutline,
  matChatOutline,
  matPinDropOutline,
  matReceiptOutline,
} from '@ng-icons/material-symbols/outline';
import { switchMap } from 'rxjs';
import { DecimalPipe } from '@angular/common';
import { ToastService } from '../../../../core/services/toast.service';
import { StoreResponse } from '../../../../core/models/store.models';

@Component({
  imports: [NgIcon, DecimalPipe],
  providers: [
    provideIcons({
      matPreviewOutline,
      matStorefrontOutline,
      matStoreOutline,
      matMopedOutline,
      matCheckOutline,
      matContentCopyOutline,
      matPowerSettingsNewOutline,
      matPauseCircleOutline,
      matChatOutline,
      matPinDropOutline,
      matReceiptOutline,
    }),
  ],
  selector: 'app-store-preview-card',
  styleUrl: './store-preview-card.css',
  templateUrl: './store-preview-card.html',
})
export class StorePreviewCard {
  private readonly toastService = inject(ToastService);

  readonly form = input<FormGroup>(new FormGroup({}));
  readonly store = input<StoreResponse | null>(null);

  private readonly form$ = toObservable(this.form);
  private readonly formValues = toSignal(
    this.form$.pipe(switchMap((f) => f.valueChanges)),
    { initialValue: null },
  );

  readonly isCopied = signal<boolean>(false);

  handlePauseStore(): void {
    this.toastService.info(
      'Para pausar temporalmente o configurar horarios, gestiona tus franjas en el módulo de Capacidad.',
    );
  }

  private getVal<T>(key: string, fallback: T): T {
    const liveVal = this.formValues()?.[key];
    if (liveVal !== undefined && liveVal !== null && liveVal !== '') {
      return liveVal;
    }
    const currentFormVal = this.form()?.get(key)?.value;
    if (currentFormVal !== undefined && currentFormVal !== null && currentFormVal !== '') {
      return currentFormVal;
    }
    return fallback;
  }

  readonly name = computed(() =>
    this.getVal('name', this.store()?.name || 'Nombre de tu Tienda'),
  );

  readonly description = computed(() =>
    this.getVal(
      'description',
      this.store()?.description ||
        'Productos preparados con la mejor dedicación. Realiza tu pedido con anticipación.',
    ),
  );

  readonly contactPhone = computed(() =>
    this.getVal('contactPhone', this.store()?.contactPhone || ''),
  );

  readonly address = computed(() =>
    this.getVal('address', this.store()?.address || ''),
  );

  readonly addressReference = computed(() =>
    this.getVal('addressReference', this.store()?.addressReference || ''),
  );

  readonly pickupEnabled = computed(() => {
    const live = this.formValues()?.pickupEnabled;
    if (live !== undefined && live !== null) return Boolean(live);
    const formVal = this.form()?.get('pickupEnabled')?.value;
    if (formVal !== undefined && formVal !== null) return Boolean(formVal);
    return this.store()?.pickupEnabled ?? true;
  });

  readonly deliveryEnabled = computed(() => {
    const live = this.formValues()?.deliveryEnabled;
    if (live !== undefined && live !== null) return Boolean(live);
    const formVal = this.form()?.get('deliveryEnabled')?.value;
    if (formVal !== undefined && formVal !== null) return Boolean(formVal);
    return this.store()?.deliveryEnabled ?? true;
  });

  readonly deliveryFeeAmount = computed(() => {
    const live = this.formValues()?.deliveryFeeAmount;
    if (live !== undefined && live !== null) return Number(live);
    const formVal = this.form()?.get('deliveryFeeAmount')?.value;
    if (formVal !== undefined && formVal !== null) return Number(formVal);
    return this.store()?.deliveryFeeAmount ?? 5.0;
  });

  readonly deliveryFeeCurrency = computed(() =>
    this.getVal('deliveryFeeCurrency', this.store()?.deliveryFeeCurrency || 'PEN'),
  );

  readonly taxApplies = computed(() => {
    const live = this.formValues()?.taxApplies;
    if (live !== undefined && live !== null) return Boolean(live);
    const formVal = this.form()?.get('taxApplies')?.value;
    if (formVal !== undefined && formVal !== null) return Boolean(formVal);
    return this.store()?.taxApplies ?? false;
  });

  readonly slug = computed(() => this.store()?.slug || 'mi-tienda');

  copyLink(): void {
    const url = `https://jaldishop.pe/tienda/${this.slug()}`;
    navigator.clipboard.writeText(url).then(() => {
      this.isCopied.set(true);
      setTimeout(() => this.isCopied.set(false), 2000);
    });
  }
}
