import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, HostListener, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matContentCopyOutline,
  matCheckOutline,
  matEditOutline,
  matDeleteOutline,
  matCloseOutline,
  matSellOutline,
  matCampaignOutline,
} from '@ng-icons/material-symbols/outline';
import { PromoCodeService } from '../../../core/services/promo-code.service';
import {
  DiscountType,
  PromoCode,
  PromoStatus,
  resolvePromoStatus,
  UpsertPromoCodePayload,
} from '../../../core/models/promo-code.models';

@Component({
  selector: 'app-promo-codes-manager',
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matAddOutline,
      matContentCopyOutline,
      matCheckOutline,
      matEditOutline,
      matDeleteOutline,
      matCloseOutline,
      matSellOutline,
      matCampaignOutline,
    }),
  ],
  templateUrl: './promo-codes-manager.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromoCodesManager {
  private readonly promoService = inject(PromoCodeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly mode = input.required<'ADMIN' | 'MERCHANT'>();
  readonly storeId = input<string | null>(null);

  readonly promos = signal<PromoCode[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly copiedId = signal<string | null>(null);

  // Modal state
  readonly isModalOpen = signal<boolean>(false);
  readonly isModalClosing = signal<boolean>(false);
  private modalCloseTimeoutId?: ReturnType<typeof setTimeout>;
  readonly editingPromo = signal<PromoCode | null>(null);
  readonly isSubmitting = signal<boolean>(false);
  readonly formError = signal<string | null>(null);

  // Form fields
  readonly formCode = signal<string>('');
  readonly formDiscountType = signal<DiscountType>('PERCENTAGE');
  readonly formDiscountValue = signal<string>('10');
  readonly formMinOrder = signal<string>('');
  readonly formMaxDiscount = signal<string>('');
  readonly formUsageLimit = signal<string>('');
  readonly formStartsAt = signal<string>('');
  readonly formEndsAt = signal<string>('');
  readonly formActive = signal<boolean>(true);
  readonly formShowInBanner = signal<boolean>(false);
  readonly formBannerText = signal<string>('');

  readonly isAdmin = computed(() => this.mode() === 'ADMIN');
  readonly pageTitle = computed(() =>
    this.isAdmin() ? 'Promociones del Marketplace' : 'Cupones de Descuento',
  );
  readonly pageSubtitle = computed(() =>
    this.isAdmin()
      ? 'Códigos globales financiados por JaldiShop y banner superior del storefront.'
      : 'Cupones exclusivos de tu tienda. Se aplican sobre el subtotal de tus productos.',
  );

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.modalCloseTimeoutId) {
        clearTimeout(this.modalCloseTimeoutId);
      }
    });

    effect(() => {
      const m = this.mode();
      const sId = this.storeId();
      if (m === 'ADMIN' || (m === 'MERCHANT' && sId)) {
        this.loadPromos();
      }
    });
  }

  loadPromos(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const req$ =
      this.mode() === 'ADMIN'
        ? this.promoService.listGlobal()
        : this.promoService.listByStore(this.storeId()!);

    req$.subscribe({
      next: (list) => {
        this.promos.set(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'No se pudieron cargar los códigos promocionales.');
        this.isLoading.set(false);
      },
    });
  }

  getStatus(promo: PromoCode): PromoStatus {
    return resolvePromoStatus(promo);
  }

  getStatusLabel(status: PromoStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'Activo';
      case 'SCHEDULED':
        return 'Programado';
      case 'EXPIRED':
        return 'Expirado';
      case 'EXHAUSTED':
        return 'Agotado';
      case 'INACTIVE':
        return 'Inactivo';
    }
  }

  getStatusBadgeClass(status: PromoStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-emerald-50 text-emerald-800 border-emerald-200';
      case 'SCHEDULED':
        return 'bg-sky-50 text-sky-800 border-sky-200';
      case 'EXPIRED':
        return 'bg-amber-50 text-amber-800 border-amber-200';
      case 'EXHAUSTED':
        return 'bg-rose-50 text-rose-800 border-rose-200';
      case 'INACTIVE':
        return 'bg-stone-100 text-stone-700 border-stone-200';
    }
  }

  copyCode(promo: PromoCode): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      void navigator.clipboard.writeText(promo.code);
    }
    this.copiedId.set(promo.id);
    setTimeout(() => {
      if (this.copiedId() === promo.id) {
        this.copiedId.set(null);
      }
    }, 1800);
  }

  openCreateModal(): void {
    this.cancelPendingModalClose();
    this.editingPromo.set(null);
    this.formCode.set('');
    this.formDiscountType.set('PERCENTAGE');
    this.formDiscountValue.set('10');
    this.formMinOrder.set('');
    this.formMaxDiscount.set('');
    this.formUsageLimit.set('');
    this.formStartsAt.set(this.toLocalInput(new Date().toISOString()));
    this.formEndsAt.set('');
    this.formActive.set(true);
    this.formShowInBanner.set(false);
    this.formBannerText.set('');
    this.formError.set(null);
    this.isModalClosing.set(false);
    this.isModalOpen.set(true);
  }

  openEditModal(promo: PromoCode): void {
    this.cancelPendingModalClose();
    this.editingPromo.set(promo);
    this.formCode.set(promo.code);
    this.formDiscountType.set(promo.discountType);
    this.formDiscountValue.set(String(promo.discountValue));
    this.formMinOrder.set(promo.minOrderAmount !== null ? String(promo.minOrderAmount) : '');
    this.formMaxDiscount.set(promo.maxDiscountAmount !== null ? String(promo.maxDiscountAmount) : '');
    this.formUsageLimit.set(promo.usageLimit !== null ? String(promo.usageLimit) : '');
    this.formStartsAt.set(this.toLocalInput(promo.startsAt));
    this.formEndsAt.set(this.toLocalInput(promo.endsAt));
    this.formActive.set(promo.active);
    this.formShowInBanner.set(promo.showInBanner);
    this.formBannerText.set(promo.bannerText ?? '');
    this.formError.set(null);
    this.isModalClosing.set(false);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    if (!this.isModalOpen() || this.isModalClosing()) return;
    this.isModalClosing.set(true);
    this.modalCloseTimeoutId = setTimeout(() => {
      this.isModalOpen.set(false);
      this.isModalClosing.set(false);
      this.editingPromo.set(null);
      this.formError.set(null);
      this.modalCloseTimeoutId = undefined;
    }, 180);
  }

  onModalBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isModalOpen()) {
      this.closeModal();
    }
  }

  private cancelPendingModalClose(): void {
    if (this.modalCloseTimeoutId) {
      clearTimeout(this.modalCloseTimeoutId);
      this.modalCloseTimeoutId = undefined;
    }
  }

  onActiveToggleChange(checked: boolean): void {
    this.formActive.set(checked);
    if (checked && !this.formStartsAt().trim()) {
      this.formStartsAt.set(this.toLocalInput(new Date().toISOString()));
    }
  }

  submitForm(): void {
    this.formError.set(null);
    const code = this.formCode().trim().toUpperCase();
    if (!/^[A-Z0-9_-]{3,40}$/.test(code)) {
      this.formError.set('El código debe tener entre 3 y 40 caracteres (A-Z, 0-9, - o _), sin espacios.');
      return;
    }

    const discountValue = Number(this.formDiscountValue());
    if (!Number.isFinite(discountValue) || discountValue <= 0) {
      this.formError.set('Ingresa un valor de descuento mayor a 0.');
      return;
    }
    if (this.formDiscountType() === 'PERCENTAGE' && discountValue > 100) {
      this.formError.set('El porcentaje de descuento no puede superar 100%.');
      return;
    }

    const minOrderAmount = this.formMinOrder().trim() ? Number(this.formMinOrder()) : null;
    const maxDiscountAmount =
      this.formDiscountType() === 'PERCENTAGE' && this.formMaxDiscount().trim()
        ? Number(this.formMaxDiscount())
        : null;
    const usageLimit = this.formUsageLimit().trim() ? Math.floor(Number(this.formUsageLimit())) : null;
    const startsAt = this.formStartsAt().trim() ? new Date(this.formStartsAt()).toISOString() : null;
    const endsAt = this.formEndsAt().trim() ? new Date(this.formEndsAt()).toISOString() : null;

    if (startsAt && endsAt && new Date(endsAt) <= new Date(startsAt)) {
      this.formError.set('La fecha de fin debe ser posterior a la fecha de inicio.');
      return;
    }

    const showInBanner = this.isAdmin() ? this.formShowInBanner() : false;
    const bannerText = this.isAdmin() && this.formBannerText().trim() ? this.formBannerText().trim() : null;
    if (showInBanner && !bannerText) {
      this.formError.set('Ingresa el texto que se mostrará en el banner del marketplace.');
      return;
    }

    const payload: UpsertPromoCodePayload = {
      code,
      discountType: this.formDiscountType(),
      discountValue,
      minOrderAmount,
      maxDiscountAmount,
      usageLimit,
      startsAt,
      endsAt,
      active: this.formActive(),
      showInBanner,
      bannerText,
    };

    this.isSubmitting.set(true);
    const editing = this.editingPromo();

    const req$ =
      this.mode() === 'ADMIN'
        ? editing
          ? this.promoService.updateGlobal(editing.id, payload)
          : this.promoService.createGlobal(payload)
        : editing
          ? this.promoService.updateForStore(this.storeId()!, editing.id, payload)
          : this.promoService.createForStore(this.storeId()!, payload);

    req$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeModal();
        this.loadPromos();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.formError.set(err?.error?.message || 'No se pudo guardar el código promocional.');
      },
    });
  }

  toggleActive(promo: PromoCode): void {
    const nextActive = !promo.active;
    const req$ =
      this.mode() === 'ADMIN'
        ? this.promoService.toggleGlobal(promo.id, nextActive)
        : this.promoService.toggleForStore(this.storeId()!, promo.id, nextActive);

    req$.subscribe({
      next: () => this.loadPromos(),
      error: (err) => this.errorMessage.set(err?.error?.message || 'No se pudo actualizar el estado.'),
    });
  }

  deletePromo(promo: PromoCode): void {
    const req$ =
      this.mode() === 'ADMIN'
        ? this.promoService.deleteGlobal(promo.id)
        : this.promoService.deleteForStore(this.storeId()!, promo.id);

    req$.subscribe({
      next: () => this.loadPromos(),
      error: (err) => this.errorMessage.set(err?.error?.message || 'No se pudo eliminar el código.'),
    });
  }

  private toLocalInput(iso: string | null): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
