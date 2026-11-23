import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matEditOutline,
  matCloseOutline,
  matWarningOutline,
  matErrorOutline,
  matCheckOutline,
  matRefreshOutline,
  matAddOutline,
  matRemoveOutline,
} from '@ng-icons/material-symbols/outline';
import { InventoryResponse } from '../../../../core/models/inventory.models';
import { InventoryService } from '../../../../core/services/inventory.service';

@Component({
  selector: 'app-inventory-stock-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matEditOutline,
      matCloseOutline,
      matWarningOutline,
      matErrorOutline,
      matCheckOutline,
      matRefreshOutline,
      matAddOutline,
      matRemoveOutline,
    }),
  ],
  templateUrl: './inventory-stock-drawer.html',
  styleUrl: './inventory-stock-drawer.css',
})
export class InventoryStockDrawer {
  readonly MAX_STOCK = 99999;
  private readonly inventoryService = inject(InventoryService);

  readonly isOpen = input<boolean>(false);
  readonly variant = input<InventoryResponse | null>(null);

  readonly close = output<void>();
  readonly stockSaved = output<InventoryResponse>();

  readonly quantity = signal<number>(0);
  readonly isSaving = signal<boolean>(false);
  readonly isClosing = signal<boolean>(false);
  readonly validationError = signal<string | null>(null);
  readonly serverError = signal<string | null>(null);

  readonly availablePreview = computed(() => {
    const reserved = this.variant()?.reservedQuantity ?? 0;
    const currentQ = this.quantity();
    if (isNaN(currentQ)) return 0;
    return Math.max(0, currentQ - reserved);
  });

  constructor() {
    effect(() => {
      const v = this.variant();
      if (v) {
        this.quantity.set(v.physicalQuantity ?? 0);
        this.validationError.set(null);
        this.serverError.set(null);
      }
    });
  }

  handleClose(): void {
    if (this.isSaving() || this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.close.emit();
    }, 200);
  }

  onBackdropClick(): void {
    this.handleClose();
  }

  applyDelta(delta: number): void {
    const reserved = this.variant()?.reservedQuantity ?? 0;
    const current = isNaN(this.quantity()) ? 0 : this.quantity();
    const next = Math.min(this.MAX_STOCK, Math.max(reserved, current + delta));
    this.quantity.set(next);
    this.validationError.set(null);
    this.serverError.set(null);
  }

  onQuantityChange(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    const rawVal = inputElement.value.trim();

    if (rawVal === '') {
      this.validationError.set('La cantidad física es obligatoria.');
      return;
    }

    const parsed = Number(rawVal);
    if (isNaN(parsed) || !Number.isInteger(parsed) || parsed < 0) {
      this.validationError.set('La cantidad física debe ser un número entero mayor o igual a 0.');
      return;
    }

    if (parsed > this.MAX_STOCK) {
      this.validationError.set(`La cantidad física máxima permitida es ${this.MAX_STOCK.toLocaleString()} unidades.`);
      return;
    }

    const reserved = this.variant()?.reservedQuantity ?? 0;
    if (parsed < reserved) {
      this.validationError.set(
        `No puedes reducir el stock físico por debajo de las reservas activas (mínimo ${reserved}).`
      );
      this.quantity.set(parsed);
      return;
    }

    this.quantity.set(parsed);
    this.validationError.set(null);
    this.serverError.set(null);
  }

  onSubmit(): void {
    if (this.isSaving()) {
      return;
    }

    const currentVariant = this.variant();
    if (!currentVariant) {
      return;
    }

    const q = this.quantity();
    if (q < 0 || isNaN(q) || !Number.isInteger(q)) {
      this.validationError.set('Ingresa un valor entero no negativo válido.');
      return;
    }

    if (q > this.MAX_STOCK) {
      this.validationError.set(`La cantidad física no puede exceder ${this.MAX_STOCK.toLocaleString()} unidades.`);
      return;
    }

    const reserved = currentVariant.reservedQuantity ?? 0;
    if (q < reserved) {
      this.validationError.set(
        `No se puede ajustar la cantidad a ${q} porque existen ${reserved} unidades reservadas activas.`
      );
      return;
    }

    this.isSaving.set(true);
    this.serverError.set(null);

    this.inventoryService.updateStock(currentVariant.variantId, q).subscribe({
      next: (updated) => {
        this.isSaving.set(false);
        this.stockSaved.emit(updated);
        this.close.emit();
      },
      error: (err) => {
        this.isSaving.set(false);
        if (err?.error?.code === 'CANNOT_REDUCE_BELOW_RESERVED' || err?.code === 'CANNOT_REDUCE_BELOW_RESERVED') {
          this.serverError.set(
            'Conflicto: No se puede reducir la cantidad física por debajo de las órdenes activas en curso.'
          );
        } else if (err?.status === 409) {
          this.serverError.set('Conflicto al actualizar stock. Verifica que la variante esté activa.');
        } else {
          this.serverError.set(err?.error?.message || err?.message || 'Error al guardar existencias.');
        }
      },
    });
  }
}
