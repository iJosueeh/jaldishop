import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
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
import {
  getAllowedDocTypesForRegime,
  getDefaultTaxRateForRegime,
  sunatDocumentValidator,
} from '../../../../core/utils/sunat-tax.utils';

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
    legalBusinessName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
    taxIdType: ['RUC_10' as TaxIdType, [Validators.required]],
    taxIdNumber: ['', [Validators.required, sunatDocumentValidator(() => this.taxForm?.get('taxIdType')?.value ?? 'RUC_10')]],
    taxRegime: ['RUS' as TaxRegime, [Validators.required]],
    canIssueInvoices: [{ value: false, disabled: true }],
    igvExempt: [{ value: true, disabled: true }],
    taxRate: [0],
  });

  private readonly formValues = toSignal(this.taxForm.valueChanges, {
    initialValue: this.taxForm.getRawValue(),
  });

  readonly allowedDocTypes = computed(() => {
    const regime = (this.formValues()?.taxRegime || 'RUS') as TaxRegime;
    return getAllowedDocTypesForRegime(regime);
  });

  constructor() {
    effect(() => {
      const store = this.storeService.currentStore();
      if (store) {
        const regime = store.taxRegime || 'RUS';
        const isRus = regime === 'RUS' || regime === 'NONE';
        const currentRate = store.taxRate ?? (isRus ? 0 : 18);
        const isExempt = isRus || currentRate === 0;
        this.isRusActive.set(isRus);

        this.taxForm.patchValue(
          {
            legalBusinessName: store.legalBusinessName || store.name || '',
            taxIdType: store.taxIdType || 'DNI',
            taxIdNumber: store.taxIdNumber || '',
            taxRegime: regime,
            canIssueInvoices: isRus ? false : !!store.canIssueInvoices,
            igvExempt: isExempt,
            taxRate: isRus ? 0 : currentRate,
          },
          { emitEvent: false }
        );

        this.applyRegimeControlState(isRus);
        this.taxForm.get('taxIdNumber')?.updateValueAndValidity({ emitEvent: false });
      }
    });

    this.taxForm.get('taxRegime')?.valueChanges.subscribe((regime: TaxRegime) => {
      const isRus = regime === 'RUS' || regime === 'NONE';
      this.isRusActive.set(isRus);

      const allowed = getAllowedDocTypesForRegime(regime);
      const currentType = this.taxForm.get('taxIdType')?.value as TaxIdType;
      if (!allowed.includes(currentType)) {
        this.taxForm.patchValue({ taxIdType: allowed[0] }, { emitEvent: false });
      }
      this.taxForm.get('taxIdNumber')?.updateValueAndValidity();

      this.applyRegimeControlState(isRus);
      if (isRus) {
        this.taxForm.patchValue({ canIssueInvoices: false, igvExempt: true, taxRate: 0 }, { emitEvent: false });
      } else {
        this.taxForm.patchValue({ igvExempt: false, taxRate: 18 }, { emitEvent: false });
      }
    });

    this.taxForm.get('taxIdType')?.valueChanges.subscribe(() => {
      this.taxForm.get('taxIdNumber')?.updateValueAndValidity();
    });

    this.taxForm.get('igvExempt')?.valueChanges.subscribe((exempt: boolean) => {
      const regime = (this.taxForm.get('taxRegime')?.value || 'RUS') as TaxRegime;
      this.taxForm.patchValue(
        { taxRate: getDefaultTaxRateForRegime(regime, !!exempt) },
        { emitEvent: false }
      );
    });
  }

  private applyRegimeControlState(isRus: boolean): void {
    const invoiceControl = this.taxForm.get('canIssueInvoices');
    const exemptControl = this.taxForm.get('igvExempt');
    if (isRus) {
      invoiceControl?.disable({ emitEvent: false });
      exemptControl?.disable({ emitEvent: false });
    } else {
      invoiceControl?.enable({ emitEvent: false });
      exemptControl?.enable({ emitEvent: false });
    }
  }

  onSave(): void {
    if (this.taxForm.invalid) {
      this.taxForm.markAllAsTouched();
      this.toastService.error('Revisa los datos fiscales ingresados.');
      return;
    }

    const raw = this.taxForm.getRawValue();
    this.isSaving.set(true);

    const isRus = raw.taxRegime === 'RUS' || raw.taxRegime === 'NONE';

    this.storeService
      .updateTaxProfile({
        legalBusinessName: raw.legalBusinessName?.trim(),
        taxIdType: raw.taxIdType,
        taxIdNumber: raw.taxIdNumber.trim(),
        taxRegime: raw.taxRegime,
        canIssueInvoices: isRus ? false : !!raw.canIssueInvoices,
        taxRate: isRus ? 0 : (raw.taxRate ?? 18),
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
