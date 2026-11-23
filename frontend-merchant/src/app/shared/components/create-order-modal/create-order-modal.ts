import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matAddOutline,
  matRemoveOutline,
  matDeleteOutline,
  matPersonOutline,
  matCallOutline,
  matLocationOnOutline,
  matScheduleOutline,
  matReceiptLongOutline,
  matTwoWheelerOutline,
  matStorefrontOutline,
  matPaymentsOutline,
  matNotesOutline,
  matCheckOutline,
  matChatOutline,
  matShoppingBagOutline,
  matLocalShippingOutline,
  matSearchOutline,
  matBoltOutline,
  matTimerOutline,
} from '@ng-icons/material-symbols/outline';
import {
  CreateMerchantOrderItemRequest,
  CreateMerchantOrderRequest,
  MerchantOrder,
  OrderChannel,
  PaymentMethod,
} from '../../../core/models/order.models';
import { ProductService } from '../../../core/services/product.service';
import { StoreService } from '../../../core/services/store.service';
import { OrderService } from '../../../core/services/order.service';
import { ToastService } from '../../../core/services/toast.service';
import { mapDetailToMerchantOrder } from '../../../core/adapters/order.adapter';
import { getOperatingDateLima } from '../../../core/utils/date.utils';

export interface CatalogProductItem {
  id: string;
  variantId: string;
  productId: string;
  name: string;
  price: number;
  category: string;
}

@Component({
  selector: 'app-create-order-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIcon],
  viewProviders: [
    provideIcons({
      matCloseOutline,
      matAddOutline,
      matRemoveOutline,
      matDeleteOutline,
      matPersonOutline,
      matCallOutline,
      matLocationOnOutline,
      matScheduleOutline,
      matReceiptLongOutline,
      matTwoWheelerOutline,
      matStorefrontOutline,
      matPaymentsOutline,
      matNotesOutline,
      matCheckOutline,
      matChatOutline,
      matShoppingBagOutline,
      matLocalShippingOutline,
      matSearchOutline,
      matBoltOutline,
      matTimerOutline,
    }),
  ],
  templateUrl: './create-order-modal.html',
  styleUrl: './create-order-modal.css',
})
export class CreateOrderModal {
  private readonly fb = inject(FormBuilder);
  readonly productService = inject(ProductService);
  readonly storeService = inject(StoreService);
  readonly orderService = inject(OrderService);
  readonly toastService = inject(ToastService);
  readonly Number = Number;

  isOpen = input<boolean>(false);
  closeModal = output<void>();
  orderCreated = output<MerchantOrder>();

  readonly activeMobileTab = signal<'DATA' | 'ITEMS'>('DATA');
  readonly productSearchQuery = signal<string>('');
  readonly isSearchDropdownOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);

  constructor() {
    effect(() => {
      const open = this.isOpen();
      if (open) {
        const storeId = this.storeService.currentStore()?.id;
        if (storeId && this.productService.products().length === 0) {
          this.productService.loadProducts(storeId).subscribe();
        }
      }
    });
  }

  readonly catalogProducts = computed<CatalogProductItem[]>(() => {
    const products = this.productService.products();
    if (!products || products.length === 0) {
      return [];
    }
    const items: CatalogProductItem[] = [];
    for (const p of products) {
      if (p.status !== 'ACTIVE') continue;
      const categoryName = p.categoryName || 'General';

      if (p.variants && p.variants.length > 0) {
        const activeVariants = p.variants.filter((v) => v.status === 'ACTIVE');
        const targetVariants = activeVariants.length > 0 ? activeVariants : p.variants;
        for (const v of targetVariants) {
          items.push({
            id: v.id,
            variantId: v.id,
            productId: p.id,
            name: targetVariants.length > 1 && v.presentationName
              ? `${p.name} (${v.presentationName})`
              : p.name,
            price: v.priceAmount,
            category: categoryName,
          });
        }
      } else {
        items.push({
          id: p.id,
          variantId: p.id,
          productId: p.id,
          name: p.name,
          price: p.minPrice || 0,
          category: categoryName,
        });
      }
    }
    return items;
  });

  readonly filteredCatalogProducts = computed(() => {
    const q = this.productSearchQuery().trim().toLowerCase();
    const list = this.catalogProducts();
    if (!q) {
      return list.slice(0, 4);
    }
    return list
      .filter((p) => p.name.toLowerCase().includes(q) || p.category.toLowerCase().includes(q))
      .slice(0, 4);
  });

  setMobileTab(tab: 'DATA' | 'ITEMS'): void {
    this.activeMobileTab.set(tab);
  }

  readonly orderForm: FormGroup = this.fb.group({
    customerName: ['', [Validators.required, Validators.minLength(3)]],
    customerPhone: ['', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
    channel: ['WHATSAPP' as OrderChannel, [Validators.required]],
    deliveryMode: ['DELIVERY', [Validators.required]],
    deliveryAddress: ['', [Validators.required]],
    deliveryReference: [''],
    deliveryFee: [5.0, [Validators.min(0)]],
    scheduledTime: [this.getDefaultScheduledTime(), [Validators.required]],
    paymentMethod: ['YAPE' as PaymentMethod, [Validators.required]],
    notes: [''],
    items: this.fb.array([], (control) => {
      return (control as FormArray).length > 0 ? null : { required: true };
    }),
  });

  get itemsFormArray(): FormArray {
    return this.orderForm.get('items') as FormArray;
  }

  createItemFormGroup(name = '', quantity = 1, unitPrice = 0, variantId = ''): FormGroup {
    return this.fb.group({
      variantId: [variantId],
      name: [name, [Validators.required, Validators.minLength(2)]],
      quantity: [quantity, [Validators.required, Validators.min(1)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0)]],
    });
  }

  readonly scheduledTimeValue = toSignal(
    this.orderForm.get('scheduledTime')!.valueChanges,
    { initialValue: this.orderForm.get('scheduledTime')?.value }
  );

  readonly scheduledTimeRelativeText = computed(() => {
    const timeStr = this.scheduledTimeValue();
    if (!timeStr || typeof timeStr !== 'string' || !timeStr.includes(':')) {
      return '';
    }

    const [hours, minutes] = timeStr.split(':').map(Number);
    if (isNaN(hours) || isNaN(minutes)) return '';

    const now = new Date();
    const scheduledDate = new Date();
    scheduledDate.setHours(hours, minutes, 0, 0);

    const diffMinutes = Math.round((scheduledDate.getTime() - now.getTime()) / (60 * 1000));

    if (diffMinutes < -10) {
      return 'Hora anterior a la actual';
    }
    if (diffMinutes >= -10 && diffMinutes < 5) {
      return 'Inmediato (ahora)';
    }
    if (diffMinutes >= 5 && diffMinutes < 60) {
      return `En ~${diffMinutes} min`;
    }
    const h = Math.floor(diffMinutes / 60);
    const m = diffMinutes % 60;
    return m === 0 ? `En ~${h} h` : `En ~${h}h ${m}m`;
  });

  getDefaultScheduledTime(): string {
    const d = new Date(Date.now() + 30 * 60 * 1000);
    const hours = d.getHours().toString().padStart(2, '0');
    const minutes = d.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  setQuickTime(minutesFromNow: number): void {
    const d = new Date(Date.now() + minutesFromNow * 60 * 1000);
    const hours = d.getHours().toString().padStart(2, '0');
    const minutes = d.getMinutes().toString().padStart(2, '0');
    this.orderForm.patchValue({ scheduledTime: `${hours}:${minutes}` });
    this.orderForm.get('scheduledTime')?.markAsDirty();
  }

  adjustScheduledTime(deltaMinutes: number): void {
    const current = this.orderForm.get('scheduledTime')?.value || this.getDefaultScheduledTime();
    let [h, m] = current.split(':').map(Number);
    if (isNaN(h) || isNaN(m)) {
      const d = new Date();
      h = d.getHours();
      m = d.getMinutes();
    }
    const totalMinutes = h * 60 + m + deltaMinutes;
    // Normalizar a rango 24 horas (0 - 1439 minutos)
    const normalizedMinutes = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60);
    const newH = Math.floor(normalizedMinutes / 60).toString().padStart(2, '0');
    const newM = (normalizedMinutes % 60).toString().padStart(2, '0');
    this.orderForm.patchValue({ scheduledTime: `${newH}:${newM}` });
    this.orderForm.get('scheduledTime')?.markAsDirty();
  }

  onSearchFocus(): void {
    this.isSearchDropdownOpen.set(true);
  }

  onSearchBlur(): void {
    setTimeout(() => {
      this.isSearchDropdownOpen.set(false);
    }, 200);
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.productSearchQuery.set(value);
    this.isSearchDropdownOpen.set(true);
  }

  selectProductFromSearch(product: CatalogProductItem): void {
    const controls = this.itemsFormArray.controls;

    // Si ya existe en la lista, aumentamos su cantidad
    const existingIndex = controls.findIndex(
      (ctrl) =>
        (ctrl.get('variantId')?.value && ctrl.get('variantId')?.value === product.variantId) ||
        ctrl.get('name')?.value?.trim().toLowerCase() === product.name.trim().toLowerCase(),
    );

    if (existingIndex >= 0) {
      this.incrementQuantity(existingIndex);
    } else {
      this.itemsFormArray.push(
        this.createItemFormGroup(product.name, 1, product.price, product.variantId),
      );
    }

    this.productSearchQuery.set('');
    this.isSearchDropdownOpen.set(false);
  }

  addCustomItem(): void {
    const catalog = this.catalogProducts();
    if (catalog.length === 0) {
      this.toastService.warning('Tu tienda no tiene productos activos en el catálogo');
      return;
    }

    const query = this.productSearchQuery().trim().toLowerCase();
    if (!query) {
      const available =
        catalog.find(
          (p) =>
            !this.itemsFormArray.controls.some(
              (ctrl) => ctrl.get('variantId')?.value === p.variantId,
            ),
        ) || catalog[0];

      if (available) {
        this.selectProductFromSearch(available);
      }
      return;
    }

    const matchedProduct = catalog.find(
      (p) => p.name.toLowerCase().includes(query) || p.category.toLowerCase().includes(query),
    );

    if (matchedProduct) {
      this.selectProductFromSearch(matchedProduct);
    } else {
      this.toastService.warning('El producto no pertenece al catálogo de tu tienda');
    }
  }

  removeItem(index: number): void {
    if (this.itemsFormArray.length > 0) {
      this.itemsFormArray.removeAt(index);
    }
  }

  incrementQuantity(index: number): void {
    const control = this.itemsFormArray.at(index).get('quantity');
    if (control) {
      const curr = Number(control.value) || 1;
      control.setValue(curr + 1);
    }
  }

  decrementQuantity(index: number): void {
    const control = this.itemsFormArray.at(index).get('quantity');
    if (control) {
      const curr = Number(control.value) || 1;
      if (curr > 1) {
        control.setValue(curr - 1);
      }
    }
  }

  calculateTotalUnits(): number {
    const rawItems = this.itemsFormArray.value as Array<{ quantity: number }>;
    return rawItems.reduce((acc, curr) => acc + (Number(curr.quantity) || 0), 0);
  }

  calculateItemsSubtotal(): number {
    const rawItems = this.itemsFormArray.value as Array<{ quantity: number; unitPrice: number }>;
    return rawItems.reduce((acc, curr) => acc + (Number(curr.quantity || 0) * Number(curr.unitPrice || 0)), 0);
  }

  calculateGrandTotal(): number {
    const itemsSubtotal = this.calculateItemsSubtotal();
    const fee = Number(this.orderForm.get('deliveryFee')?.value || 0);
    return itemsSubtotal + (this.orderForm.get('deliveryMode')?.value === 'DELIVERY' ? fee : 0);
  }

  onDeliveryModeChange(mode: 'DELIVERY' | 'PICKUP'): void {
    this.orderForm.patchValue({ deliveryMode: mode });
    const addressControl = this.orderForm.get('deliveryAddress');
    if (mode === 'DELIVERY') {
      addressControl?.setValidators([Validators.required]);
      if (!this.orderForm.get('deliveryFee')?.value) {
        this.orderForm.patchValue({ deliveryFee: 5.0 });
      }
    } else {
      addressControl?.clearValidators();
      this.orderForm.patchValue({ deliveryFee: 0 });
    }
    addressControl?.updateValueAndValidity();
  }

  setChannel(channel: OrderChannel): void {
    this.orderForm.patchValue({ channel });
  }

  readonly isClosing = signal<boolean>(false);

  setPaymentMethod(paymentMethod: PaymentMethod): void {
    this.orderForm.patchValue({ paymentMethod });
  }

  handleClose(): void {
    if (this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.onClose();
    }, 200);
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onSubmit(): void {
    if (this.orderForm.invalid || this.itemsFormArray.length === 0 || this.isSubmitting()) {
      this.orderForm.markAllAsTouched();
      return;
    }

    const store = this.storeService.currentStore();
    if (!store?.id) {
      this.toastService.error('No se ha detectado una tienda activa');
      return;
    }

    const rawItems = this.itemsFormArray.value as Array<{
      variantId?: string;
      name: string;
      quantity: number;
      unitPrice: number;
    }>;

    const catalog = this.catalogProducts();
    const orderItems: CreateMerchantOrderItemRequest[] = [];

    for (const item of rawItems) {
      let vId = item.variantId;
      if (!vId) {
        const found = catalog.find((c) => c.name.toLowerCase() === item.name.trim().toLowerCase());
        vId = found?.variantId || found?.id;
      }
      if (!vId) {
        this.toastService.error(`El producto "${item.name}" no está vinculado a una variante activa del catálogo`);
        return;
      }
      orderItems.push({
        variantId: vId,
        quantity: Number(item.quantity) || 1,
        unitPriceAmount: Number(item.unitPrice) || 0,
      });
    }

    const formVal = this.orderForm.value;
    const timeStr = formVal.scheduledTime || this.getDefaultScheduledTime();
    let startTime: string | undefined;
    let endTime: string | undefined;

    if (timeStr && typeof timeStr === 'string' && timeStr.includes(':')) {
      const [h, m] = timeStr.split(':');
      startTime = `${h.padStart(2, '0')}:${m.padStart(2, '0')}:00`;
      const endTotal = (parseInt(h, 10) * 60 + parseInt(m, 10) + 30) % (24 * 60);
      const eh = Math.floor(endTotal / 60).toString().padStart(2, '0');
      const em = (endTotal % 60).toString().padStart(2, '0');
      endTime = `${eh}:${em}:00`;
    }

    const request: CreateMerchantOrderRequest = {
      deliveryMode: formVal.deliveryMode,
      customerName: formVal.customerName.trim(),
      customerPhone: formVal.customerPhone.trim(),
      customerEmail: undefined,
      deliveryAddress: formVal.deliveryMode === 'DELIVERY' ? formVal.deliveryAddress?.trim() : undefined,
      deliveryReference: formVal.deliveryMode === 'DELIVERY' ? formVal.deliveryReference?.trim() : undefined,
      deliveryFeeAmount: formVal.deliveryMode === 'DELIVERY' ? Number(formVal.deliveryFee) || 0 : 0,
      paymentMethod: formVal.paymentMethod,
      serviceDate: getOperatingDateLima(),
      serviceStartTime: startTime,
      serviceEndTime: endTime,
      notes: formVal.notes ? formVal.notes.trim() : undefined,
      items: orderItems,
    };

    this.isSubmitting.set(true);
    this.orderService.createMerchantOrder(store.id, request).subscribe({
      next: (createdDetail) => {
        this.isSubmitting.set(false);
        const mappedOrder = mapDetailToMerchantOrder(createdDetail);
        this.orderCreated.emit(mappedOrder);
        this.resetForm();
        this.closeModal.emit();
      },
      error: () => {
        this.isSubmitting.set(false);
      },
    });
  }

  resetForm(): void {
    this.orderForm.reset({
      customerName: '',
      customerPhone: '',
      channel: 'WHATSAPP',
      deliveryMode: 'DELIVERY',
      deliveryAddress: '',
      deliveryReference: '',
      deliveryFee: 5.0,
      scheduledTime: this.getDefaultScheduledTime(),
      paymentMethod: 'YAPE',
      notes: '',
    });
    this.itemsFormArray.clear();
  }
}
