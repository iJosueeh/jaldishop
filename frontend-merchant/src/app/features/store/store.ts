import { Component, effect, inject, signal } from '@angular/core';
import { StoreIdentityCard } from './components/store-identity-card/store-identity-card';
import { StoreDeliveryCard } from './components/store-delivery-card/store-delivery-card';
import { StoreLocationCard } from './components/store-location-card/store-location-card';
import { StorePreviewCard } from './components/store-preview-card/store-preview-card';
import { NonNullableFormBuilder, Validators } from '@angular/forms';
import { StoreService } from '../../core/services/store.service';
import { StoreResponse, UpdateStoreRequest } from '../../core/models/store.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matOpenInNewOutline,
  matSaveOutline,
  matCheckCircleOutline,
  matErrorOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [
    StoreIdentityCard,
    StoreDeliveryCard,
    StoreLocationCard,
    StorePreviewCard,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matOpenInNewOutline,
      matSaveOutline,
      matCheckCircleOutline,
      matErrorOutline,
    }),
  ],
  selector: 'app-store',
  styleUrl: './store.css',
  templateUrl: './store.html',
})
export class Store {
  private readonly fb = inject(NonNullableFormBuilder);
  readonly storeService = inject(StoreService);

  readonly saveSuccess = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly storeForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: [''],
    contactPhone: [''],
    address: [''],
    addressReference: [''],
    pickupEnabled: [true],
    deliveryEnabled: [true],
    deliveryFeeAmount: [5.0],
    deliveryFeeCurrency: ['PEN'],
    taxApplies: [false],
    taxRate: [18.0],
  });

  private readonly _syncStoreFormEffect = effect(() => {
    const store = this.storeService.currentStore();
    if (store) {
      this.populateForm(store);
    }
  });

  onSave(): void {
    if (this.storeForm.invalid) {
      this.storeForm.markAllAsTouched();
      return;
    }

    this.resetFeedbackState();
    const request = this.buildUpdateRequest();

    this.storeService.updateStore(request).subscribe({
      next: () => this.handleSaveSuccess(),
      error: (err) => this.handleSaveError(err),
    });
  }

  private populateForm(store: StoreResponse): void {
    this.storeForm.patchValue({
      name: store.name,
      description: store.description ?? '',
      contactPhone: store.contactPhone ?? '',
      address: store.address ?? '',
      addressReference: store.addressReference ?? '',
      pickupEnabled: store.pickupEnabled,
      deliveryEnabled: store.deliveryEnabled,
      deliveryFeeAmount: store.deliveryFeeAmount ?? 0,
      deliveryFeeCurrency: store.deliveryFeeCurrency ?? 'PEN',
      taxApplies: store.taxApplies,
      taxRate: store.taxRate ?? 18.0,
    });
  }

  private buildUpdateRequest(): UpdateStoreRequest {
    const value = this.storeForm.getRawValue();
    return {
      name: value.name.trim(),
      description: value.description.trim() || undefined,
      contactPhone: value.contactPhone.trim() || undefined,
      address: value.address.trim() || undefined,
      addressReference: value.addressReference.trim() || undefined,
      pickupEnabled: value.pickupEnabled,
      deliveryEnabled: value.deliveryEnabled,
      deliveryFeeAmount: value.deliveryFeeAmount ?? undefined,
      deliveryFeeCurrency: value.deliveryFeeCurrency,
      taxApplies: value.taxApplies,
      taxRate: value.taxRate ?? undefined,
    };
  }

  private resetFeedbackState(): void {
    this.saveSuccess.set(false);
    this.errorMessage.set(null);
  }

  private handleSaveSuccess(): void {
    this.saveSuccess.set(true);
    setTimeout(() => this.saveSuccess.set(false), 3000);
  }

  private handleSaveError(error: any): void {
    const message = error?.error?.message ?? 'No se pudo guardar los cambios de la tienda.';
    this.errorMessage.set(message);
  }
}
