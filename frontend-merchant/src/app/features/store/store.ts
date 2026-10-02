import { Component, effect, inject, signal } from '@angular/core';
import { StoreIdentityCard } from './components/store-identity-card/store-identity-card';
import { StoreDeliveryCard } from './components/store-delivery-card/store-delivery-card';
import { StoreLocationCard } from './components/store-location-card/store-location-card';
import { StorePreviewCard } from './components/store-preview-card/store-preview-card';
import { NonNullableFormBuilder, Validators } from '@angular/forms';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
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
  private readonly toastService = inject(ToastService);
  readonly storeService = inject(StoreService);

  readonly saveSuccess = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.storeService.getStoreCategories().subscribe();
  }

  openPublicCatalog(): void {
    const slug = this.storeService.currentStore()?.slug;
    if (slug) {
      window.open(`https://jaldishop.pe/tienda/${slug}`, '_blank');
    } else {
      this.toastService.info('Configura y guarda el nombre de tu tienda para generar tu enlace público.');
    }
  }

  readonly storeForm = this.fb.group(
    {
      name: ['', [Validators.required, Validators.maxLength(160)]],
      description: [''],
      contactPhone: [''],
      address: [''],
      addressReference: [''],
      latitude: [null as number | null, [Validators.min(-90), Validators.max(90)]],
      longitude: [null as number | null, [Validators.min(-180), Validators.max(180)]],
      pickupEnabled: [true],
      deliveryEnabled: [true],
      deliveryFeeAmount: [5.0, [Validators.min(0)]],
      deliveryFeeCurrency: ['PEN'],
      taxRate: [18.0, [Validators.min(0), Validators.max(100)]],
      logoUrl: [''],
      bannerUrl: [''],
      instagramUrl: [''],
      facebookUrl: [''],
      whatsappNumber: [''],
      categoryIds: [[] as string[]],
    },
    {
      validators: [
        (control) => {
          const pickup = control.get('pickupEnabled')?.value;
          const delivery = control.get('deliveryEnabled')?.value;
          return !pickup && !delivery ? { requireAtLeastOneFulfillment: true } : null;
        },
        (control) => {
          const lat = control.get('latitude')?.value;
          const lng = control.get('longitude')?.value;
          const hasLat = lat !== null && lat !== undefined && lat !== '';
          const hasLng = lng !== null && lng !== undefined && lng !== '';
          return hasLat !== hasLng ? { coordinatesParityMismatch: true } : null;
        },
      ],
    }
  );

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
      contactPhone: store.contactPhone ? store.contactPhone.replace(/^\+51\s*/, '') : '',
      address: store.address ?? '',
      addressReference: store.addressReference ?? '',
      latitude: store.latitude ?? null,
      longitude: store.longitude ?? null,
      pickupEnabled: store.pickupEnabled,
      deliveryEnabled: store.deliveryEnabled,
      deliveryFeeAmount: store.deliveryFeeAmount ?? 0,
      deliveryFeeCurrency: store.deliveryFeeCurrency ?? 'PEN',
      taxRate: store.taxRate ?? 18.0,
      logoUrl: store.logoUrl ?? '',
      bannerUrl: store.bannerUrl ?? '',
      instagramUrl: store.instagramUrl ?? '',
      facebookUrl: store.facebookUrl ?? '',
      whatsappNumber: store.whatsappNumber ? store.whatsappNumber.replace(/^\+51\s*/, '') : '',
      categoryIds: store.categoryIds ? Array.from(store.categoryIds) : [],
    });
  }

  private buildUpdateRequest(): UpdateStoreRequest {
    const value = this.storeForm.getRawValue();
    const rawPhone = value.contactPhone?.trim();
    const cleanPhone = rawPhone ? rawPhone.replace(/\D/g, '') : '';
    const normalizedPhone = cleanPhone
      ? (cleanPhone.startsWith('51') ? `+${cleanPhone}` : `+51${cleanPhone}`)
      : undefined;

    const rawWhatsapp = value.whatsappNumber?.trim();
    const cleanWhatsapp = rawWhatsapp ? rawWhatsapp.replace(/\D/g, '') : '';
    const normalizedWhatsapp = cleanWhatsapp
      ? (cleanWhatsapp.startsWith('51') ? `+${cleanWhatsapp}` : `+51${cleanWhatsapp}`)
      : undefined;

    const latVal = value.latitude !== null && value.latitude !== undefined && value.latitude !== ('' as any)
      ? Number(value.latitude)
      : undefined;
    const lngVal = value.longitude !== null && value.longitude !== undefined && value.longitude !== ('' as any)
      ? Number(value.longitude)
      : undefined;

    return {
      name: value.name.trim(),
      description: value.description.trim() || undefined,
      contactPhone: normalizedPhone,
      address: value.address.trim() || undefined,
      addressReference: value.addressReference.trim() || undefined,
      latitude: latVal,
      longitude: lngVal,
      pickupEnabled: value.pickupEnabled,
      deliveryEnabled: value.deliveryEnabled,
      deliveryFeeAmount: value.deliveryFeeAmount ?? undefined,
      deliveryFeeCurrency: value.deliveryFeeCurrency || 'PEN',
      taxRate: value.taxRate ?? undefined,
      logoUrl: value.logoUrl.trim() || undefined,
      bannerUrl: value.bannerUrl.trim() || undefined,
      instagramUrl: value.instagramUrl.trim() || undefined,
      facebookUrl: value.facebookUrl.trim() || undefined,
      whatsappNumber: normalizedWhatsapp,
      categoryIds: value.categoryIds && value.categoryIds.length > 0 ? value.categoryIds : undefined,
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
