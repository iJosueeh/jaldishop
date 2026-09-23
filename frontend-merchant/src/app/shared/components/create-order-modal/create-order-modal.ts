import { Component, computed, inject, input, output, signal } from '@angular/core';
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
import { MerchantOrder, OrderChannel, PaymentMethod } from '../../../core/models/order.models';

export interface CatalogProductItem {
  id: string;
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
  readonly Number = Number;

  isOpen = input<boolean>(false);
  closeModal = output<void>();
  orderCreated = output<MerchantOrder>();

  readonly activeMobileTab = signal<'DATA' | 'ITEMS'>('DATA');
  readonly productSearchQuery = signal<string>('');
  readonly isSearchDropdownOpen = signal<boolean>(false);

  readonly catalogProducts = signal<CatalogProductItem[]>([
    { id: 'p1', name: 'Torta de Chocolate (12 porc.)', price: 78.0, category: 'Tortas' },
    { id: 'p2', name: 'Caja Brownies Melcochosos x6', price: 36.0, category: 'Postres' },
    { id: 'p3', name: 'Galletas de Avena con Chispas x4', price: 22.0, category: 'Galletas' },
    { id: 'p4', name: 'Pie de Limón Artesanal Mediano', price: 65.0, category: 'Pies' },
    { id: 'p5', name: 'Cheesecake de Frutos Rojos', price: 72.0, category: 'Tortas' },
    { id: 'p6', name: 'Empanada de Carne Artesanal', price: 8.5, category: 'Salados' },
    { id: 'p7', name: 'Alfajores de Maicena x12', price: 28.0, category: 'Postres' },
    { id: 'p8', name: 'Cinnamon Rolls con Glaseado x4', price: 24.0, category: 'Postres' },
    { id: 'p9', name: 'Velita Dorada Especial', price: 6.0, category: 'Complementos' },
    { id: 'p10', name: 'Tarjeta de Dedicatoria', price: 4.0, category: 'Complementos' },
  ]);

  readonly filteredCatalogProducts = computed(() => {
    const q = this.productSearchQuery().trim().toLowerCase();
    if (!q) {
      return this.catalogProducts().slice(0, 4);
    }
    return this.catalogProducts()
      .filter((p) => p.name.toLowerCase().includes(q) || p.category.toLowerCase().includes(q))
      .slice(0, 4);
  });

  setMobileTab(tab: 'DATA' | 'ITEMS'): void {
    this.activeMobileTab.set(tab);
  }

  readonly orderForm: FormGroup = this.fb.group({
    customerName: ['', [Validators.required, Validators.minLength(3)]],
    customerPhone: ['', [Validators.required, Validators.pattern(/^[0-9+ ]{8,15}$/)]],
    channel: ['WHATSAPP' as OrderChannel, [Validators.required]],
    deliveryMode: ['DELIVERY', [Validators.required]],
    deliveryAddress: ['', [Validators.required]],
    deliveryReference: [''],
    deliveryFee: [5.0, [Validators.min(0)]],
    scheduledTime: [this.getDefaultScheduledTime(), [Validators.required]],
    paymentMethod: ['YAPE' as PaymentMethod, [Validators.required]],
    notes: [''],
    items: this.fb.array([
      this.createItemFormGroup('Torta de Chocolate (12 porc.)', 1, 78.0),
    ]),
  });

  get itemsFormArray(): FormArray {
    return this.orderForm.get('items') as FormArray;
  }

  createItemFormGroup(name = '', quantity = 1, unitPrice = 0): FormGroup {
    return this.fb.group({
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
      (ctrl) => ctrl.get('name')?.value?.trim().toLowerCase() === product.name.trim().toLowerCase()
    );

    if (existingIndex >= 0) {
      this.incrementQuantity(existingIndex);
    } else {
      this.itemsFormArray.push(this.createItemFormGroup(product.name, 1, product.price));
    }

    this.productSearchQuery.set('');
    this.isSearchDropdownOpen.set(false);
  }

  addCustomItem(): void {
    const query = this.productSearchQuery().trim().toLowerCase();
    if (!query) {
      const available = this.catalogProducts().find(
        (p) => !this.itemsFormArray.controls.some((ctrl) => ctrl.get('name')?.value?.toLowerCase() === p.name.toLowerCase())
      ) || this.catalogProducts()[1] || this.catalogProducts()[0];
      
      this.itemsFormArray.push(this.createItemFormGroup(available.name, 1, available.price));
      return;
    }

    const matchedProduct = this.catalogProducts().find(
      (p) => p.name.toLowerCase().includes(query) || p.category.toLowerCase().includes(query)
    );

    if (matchedProduct) {
      this.selectProductFromSearch(matchedProduct);
    } else {
      const fallback = this.filteredCatalogProducts()[0] || this.catalogProducts()[0];
      this.selectProductFromSearch(fallback);
    }
  }

  removeItem(index: number): void {
    if (this.itemsFormArray.length > 1) {
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

  setPaymentMethod(paymentMethod: PaymentMethod): void {
    this.orderForm.patchValue({ paymentMethod });
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onSubmit(): void {
    if (this.orderForm.invalid) {
      this.orderForm.markAllAsTouched();
      return;
    }

    const formVal = this.orderForm.value;
    const orderNum = `#PED-${Math.floor(1000 + Math.random() * 9000)}`;
    const items = (formVal.items as Array<{ name: string; quantity: number; unitPrice: number }>).map((it) => ({
      name: it.name,
      quantity: Number(it.quantity),
      unitPrice: Number(it.unitPrice),
      totalPrice: Number(it.quantity) * Number(it.unitPrice),
    }));

    const totalAmount = this.calculateGrandTotal();

    const channelMap: Record<OrderChannel, string> = {
      WHATSAPP: 'WhatsApp',
      COUNTER: 'Mostrador / Taller',
      INSTAGRAM: 'Instagram Direct',
      WEB_STORE: 'Tienda Web',
    };

    const paymentMap: Record<PaymentMethod, string> = {
      YAPE: 'Yape',
      PLIN: 'Plin',
      BCP: 'BCP Transferencia',
      EFECTIVO: 'Efectivo',
      TRANSFERENCIA: 'Transferencia Bancaria',
      TARJETA: 'Tarjeta POS',
    };

    const newOrder: MerchantOrder = {
      id: `ord-${Date.now()}`,
      orderNumber: orderNum,
      customerName: formVal.customerName,
      customerPhone: formVal.customerPhone,
      channel: formVal.channel,
      channelLabel: channelMap[formVal.channel as OrderChannel] || formVal.channel,
      deliveryMode: formVal.deliveryMode,
      deliveryAddress: formVal.deliveryMode === 'DELIVERY' ? formVal.deliveryAddress : 'Recojo en local / taller',
      deliveryReference: formVal.deliveryReference,
      scheduledTime: formVal.scheduledTime,
      deliveryTimeLabel: `Programado: ${formVal.scheduledTime}`,
      isUrgent: false,
      status: 'CONFIRMED',
      paymentMethod: formVal.paymentMethod,
      paymentLabel: paymentMap[formVal.paymentMethod as PaymentMethod] || formVal.paymentMethod,
      totalAmount,
      notes: formVal.notes,
      items,
      createdAt: new Date().toISOString(),
    };

    this.orderCreated.emit(newOrder);
    this.onClose();
  }
}
