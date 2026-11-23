import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matReceiptLongOutline,
  matBadgeOutline,
  matSaveOutline,
  matInfoOutline,
  matCheckCircleOutline,
  matSyncOutline,
} from '@ng-icons/material-symbols/outline';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TaxIdType, TaxRegime } from '../../../../core/models/store.models';

@Component({
  selector: 'app-store-tax-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIcon],
  providers: [
    provideIcons({
      matReceiptLongOutline,
      matBadgeOutline,
      matSaveOutline,
      matInfoOutline,
      matCheckCircleOutline,
      matSyncOutline,
    }),
  ],
  templateUrl: './store-tax-card.html',
})
export class StoreTaxCard {
  private readonly fb = inject(FormBuilder);
  private readonly storeService = inject(StoreService);
  private readonly toastService = inject(ToastService);

  readonly isSaving = signal<boolean>(false);
  readonly isRusActive = signal<boolean>(true);

  readonly taxForm: FormGroup = this.fb.group({
    legalBusinessName: ['', [Validators.maxLength(200)]],
    taxIdType: ['DNI' as TaxIdType, [Validators.required]],
    taxIdNumber: ['', [Validators.required]],
    taxRegime: ['RUS' as TaxRegime, [Validators.required]],
    canIssueInvoices: [{ value: false, disabled: true }],
  });

  constructor() {
    // Sincronizar desde currentStore
    effect(() => {
      const store = this.storeService.currentStore();
      if (store) {
        const regime = store.taxRegime || 'RUS';
        const isRus = regime === 'RUS' || regime === 'NONE';
        this.isRusActive.set(isRus);

        this.taxForm.patchValue(
          {
            legalBusinessName: store.legalBusinessName || '',
            taxIdType: store.taxIdType || 'DNI',
            taxIdNumber: store.taxIdNumber || '',
            taxRegime: regime,
            canIssueInvoices: isRus ? false : !!store.canIssueInvoices,
          },
          { emitEvent: false }
        );

        if (isRus) {
          this.taxForm.get('canIssueInvoices')?.disable({ emitEvent: false });
        } else {
          this.taxForm.get('canIssueInvoices')?.enable({ emitEvent: false });
        }
      }
    });

    // Reaccionar a cambios en taxRegime
    this.taxForm.get('taxRegime')?.valueChanges.subscribe((regime: TaxRegime) => {
      const isRus = regime === 'RUS' || regime === 'NONE';
      this.isRusActive.set(isRus);

      const invoiceControl = this.taxForm.get('canIssueInvoices');
      if (isRus) {
        invoiceControl?.setValue(false);
        invoiceControl?.disable();
      } else {
        invoiceControl?.enable();
      }
    });

    // Reaccionar a cambios en taxIdType para ajustar validaciones de longitud
    this.taxForm.get('taxIdType')?.valueChanges.subscribe((type: TaxIdType) => {
      const numberControl = this.taxForm.get('taxIdNumber');
      if (type === 'DNI') {
        numberControl?.setValidators([
          Validators.required,
          Validators.pattern('^\\d{8}$'),
        ]);
      } else {
        numberControl?.setValidators([
          Validators.required,
          Validators.pattern('^\\d{11}$'),
        ]);
      }
      numberControl?.updateValueAndValidity();
    });
  }

  onSave(): void {
    if (this.taxForm.invalid) {
      this.taxForm.markAllAsTouched();
      this.toastService.error('Revisa los datos fiscales ingresados.');
      return;
    }

    const raw = this.taxForm.getRawValue();
    this.isSaving.set(true);

    this.storeService
      .updateTaxProfile({
        legalBusinessName: raw.legalBusinessName?.trim() || undefined,
        taxIdType: raw.taxIdType,
        taxIdNumber: raw.taxIdNumber.trim(),
        taxRegime: raw.taxRegime,
        canIssueInvoices: raw.taxRegime === 'RUS' || raw.taxRegime === 'NONE' ? false : !!raw.canIssueInvoices,
      })
      .subscribe({
        next: () => {
          this.isSaving.set(false);
          this.toastService.success('Perfil fiscal SUNAT actualizado exitosamente.');
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.message || 'Error al guardar los datos fiscales.';
          this.toastService.error(msg);
        },
      });
  }
}
