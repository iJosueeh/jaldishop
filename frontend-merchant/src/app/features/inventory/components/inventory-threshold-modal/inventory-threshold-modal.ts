import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matNotificationsOutline,
  matCloseOutline,
  matErrorOutline,
  matCheckOutline,
  matRefreshOutline,
} from '@ng-icons/material-symbols/outline';
import { InventoryResponse } from '../../../../core/models/inventory.models';
import { InventoryService } from '../../../../core/services/inventory.service';

@Component({
  selector: 'app-inventory-threshold-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matNotificationsOutline,
      matCloseOutline,
      matErrorOutline,
      matCheckOutline,
      matRefreshOutline,
    }),
  ],
  templateUrl: './inventory-threshold-modal.html',
  styleUrl: './inventory-threshold-modal.css',
})
export class InventoryThresholdModal {
  readonly MAX_THRESHOLD = 9999;
  readonly PRESETS = [2, 5, 10, 15, 20];
  private readonly inventoryService = inject(InventoryService);

  readonly isOpen = input<boolean>(false);
  readonly variant = input<InventoryResponse | null>(null);

  readonly close = output<void>();
  readonly thresholdSaved = output<InventoryResponse>();

  readonly isEnabled = signal<boolean>(false);
  readonly thresholdValue = signal<number>(0);
  readonly isSaving = signal<boolean>(false);
  readonly isClosing = signal<boolean>(false);
  readonly validationError = signal<string | null>(null);
  readonly serverError = signal<string | null>(null);

  readonly isLowStockPreview = computed(() => {
    if (!this.isEnabled()) return false;
    const physical = this.variant()?.physicalQuantity ?? 0;
    return physical <= this.thresholdValue();
  });

  constructor() {
    effect(() => {
      const v = this.variant();
      if (v) {
        if (v.lowStockThreshold !== null) {
          this.isEnabled.set(true);
          this.thresholdValue.set(v.lowStockThreshold);
        } else {
          this.isEnabled.set(false);
          this.thresholdValue.set(0);
        }
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

  onToggleEnabled(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.isEnabled.set(checked);
    if (!checked) {
      this.validationError.set(null);
    } else {
      this.validate(this.thresholdValue());
    }
  }

  setPreset(val: number): void {
    this.isEnabled.set(true);
    this.validate(val);
  }

  onThresholdInput(event: Event): void {
    const rawVal = (event.target as HTMLInputElement).value.trim();
    if (rawVal === '') {
      this.validationError.set('Ingresa un valor para el umbral.');
      return;
    }

    const parsed = Number(rawVal);
    this.validate(parsed);
  }

  private validate(val: number): boolean {
    if (isNaN(val) || !Number.isInteger(val) || val < 0) {
      this.validationError.set('El umbral debe ser un número entero mayor o igual a 0.');
      return false;
    }
    if (val > this.MAX_THRESHOLD) {
      this.validationError.set(`El umbral máximo permitido es ${this.MAX_THRESHOLD.toLocaleString()} unidades.`);
      return false;
    }
    this.thresholdValue.set(val);
    this.validationError.set(null);
    return true;
  }

  onSubmit(): void {
    if (this.isSaving()) {
      return;
    }

    const currentVariant = this.variant();
    if (!currentVariant) {
      return;
    }

    let payloadValue: number | null = null;
    if (this.isEnabled()) {
      if (!this.validate(this.thresholdValue())) {
        return;
      }
      payloadValue = this.thresholdValue();
    }

    this.isSaving.set(true);
    this.serverError.set(null);

    this.inventoryService.updateThreshold(currentVariant.variantId, payloadValue).subscribe({
      next: (updated) => {
        this.isSaving.set(false);
        this.thresholdSaved.emit(updated);
        this.close.emit();
      },
      error: (err) => {
        this.isSaving.set(false);
        if (err?.error?.code === 'INVENTORY_NOT_CONFIGURED') {
          this.serverError.set(
            'No se puede configurar el umbral porque la variante no tiene stock inicializado.'
          );
        } else {
          this.serverError.set(err?.error?.message || err?.message || 'Error al guardar umbral.');
        }
      },
    });
  }
}
