import { Component, computed, inject, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matAccountBalanceOutline,
  matPaymentsOutline,
  matSaveOutline,
  matInfoOutline,
  matSyncOutline,
  matWandStarsOutline,
} from '@ng-icons/material-symbols/outline';
import { BillingService } from '../../../../core/services/billing.service';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { UpdatePayoutDestinationRequest } from '../../../../core/models/billing.models';
import { extractApiErrorMessage } from '../../../../core/utils/error.utils';

@Component({
  selector: 'app-payout-destination-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matAccountBalanceOutline,
      matPaymentsOutline,
      matSaveOutline,
      matInfoOutline,
      matSyncOutline,
      matWandStarsOutline,
    }),
  ],
  templateUrl: './payout-destination-modal.html',
  styleUrl: './payout-destination-modal.css',
})
export class PayoutDestinationModal {
  private readonly billingService = inject(BillingService);
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);

  readonly isOpen = input<boolean>(false);
  readonly closeModal = output<void>();

  readonly channel = signal<'BANK_TRANSFER_CCI' | 'YAPE_PHONE'>('BANK_TRANSFER_CCI');
  readonly bankName = signal<string>('BCP');
  readonly accountNumber = signal<string>('');
  readonly cci = signal<string>('');
  readonly phoneNumber = signal<string>('');
  readonly accountHolderName = signal<string>('');
  readonly docType = signal<'DNI' | 'RUC' | 'CE'>('DNI');
  readonly docNumber = signal<string>('');

  readonly isSubmitting = signal<boolean>(false);
  readonly generalError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});

  // 1. Titular sugerido: Persona Jurídica (RUC 20) -> Razón Social; Persona Natural -> Nombre completo del comerciante
  readonly suggestedHolderName = computed(() => {
    const store = this.storeService.currentStore();
    const profile = this.profileService.currentProfile();
    const personalName = profile ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim() : '';

    if (store?.taxIdType === 'RUC_20' && store?.legalBusinessName?.trim()) {
      return store.legalBusinessName.trim();
    }

    if (personalName && personalName !== 'Usuario') {
      return personalName;
    }

    if (store?.legalBusinessName?.trim() && store.legalBusinessName.trim().toLowerCase() !== store.name?.trim().toLowerCase()) {
      return store.legalBusinessName.trim();
    }

    return personalName || '';
  });

  // 2. Documento sugerido
  readonly suggestedDoc = computed(() => {
    const store = this.storeService.currentStore();
    if (!store?.taxIdNumber) return null;

    const num = store.taxIdNumber.trim();
    let type: 'DNI' | 'RUC' | 'CE' = 'DNI';
    if (store.taxIdType === 'RUC_10' || store.taxIdType === 'RUC_20' || num.length === 11) {
      type = 'RUC';
    } else if (store.taxIdType === 'DNI' || num.length === 8) {
      type = 'DNI';
    }
    return { type, number: num };
  });

  // 3. Celular sugerido para Yape/Plin
  readonly suggestedPhone = computed(() => {
    const profile = this.profileService.currentProfile();
    const store = this.storeService.currentStore();
    const raw = profile?.phone || store?.contactPhone || store?.whatsappNumber;
    if (!raw) return null;
    const clean = raw.replace(/\D/g, '');
    if (clean.length === 9 && clean.startsWith('9')) {
      return clean;
    }
    return null;
  });

  readonly hasAutofillData = computed(() => {
    return !!(this.suggestedHolderName() || this.suggestedDoc()?.number);
  });

  readonly autofillSummary = computed(() => {
    const name = this.suggestedHolderName();
    const doc = this.suggestedDoc();
    const docText = doc ? `${doc.type}: ${doc.number}` : '';
    return { name, doc: docText };
  });

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        if (!this.profileService.currentProfile()) {
          this.profileService.getMyProfile().subscribe({
            error: () => {},
          });
        }

        const dest = this.billingService.payoutDestination();
        if (dest) {
          this.channel.set(dest.payoutChannel === 'YAPE_PHONE' ? 'YAPE_PHONE' : 'BANK_TRANSFER_CCI');
          this.bankName.set(dest.bankName || 'BCP');
          this.accountNumber.set(dest.accountNumber || '');
          this.cci.set(dest.accountCci || '');
          this.phoneNumber.set(dest.phoneNumber || '');
          this.accountHolderName.set(dest.accountHolderName || '');
          this.docType.set((dest.accountHolderDocType as 'DNI' | 'RUC' | 'CE') || 'DNI');
          this.docNumber.set(dest.accountHolderDocNumber || '');
        }
        this.generalError.set(null);
        this.fieldErrors.set({});
      }
    });
  }

  autofillFromProfile(): void {
    const name = this.suggestedHolderName();
    if (name) {
      this.accountHolderName.set(name);
    }

    const doc = this.suggestedDoc();
    if (doc) {
      this.docType.set(doc.type);
      this.docNumber.set(doc.number);
    }

    if (this.channel() === 'YAPE_PHONE' && !this.phoneNumber()) {
      const phone = this.suggestedPhone();
      if (phone) {
        this.phoneNumber.set(phone);
      }
    }

    // Limpiar errores previos de titular
    this.fieldErrors.update((f) => {
      const copy = { ...f };
      delete copy['accountHolderName'];
      delete copy['accountHolderDocNumber'];
      delete copy['accountHolderDocType'];
      delete copy['phoneNumber'];
      return copy;
    });
  }

  onSave(): void {
    this.generalError.set(null);
    const errors: Record<string, string> = {};

    if (!this.accountHolderName().trim()) {
      errors['accountHolderName'] = 'El nombre del titular es obligatorio.';
    }

    const cleanDoc = this.docNumber().trim();
    if (!cleanDoc) {
      errors['accountHolderDocNumber'] = 'El documento del titular es obligatorio.';
    } else if (this.docType() === 'DNI' && cleanDoc.length !== 8) {
      errors['accountHolderDocNumber'] = 'El DNI debe tener 8 dígitos.';
    } else if (this.docType() === 'RUC' && cleanDoc.length !== 11) {
      errors['accountHolderDocNumber'] = 'El RUC debe tener 11 dígitos.';
    }

    if (this.channel() === 'BANK_TRANSFER_CCI') {
      const cleanCci = this.cci().replace(/\D/g, '');
      if (!cleanCci) {
        errors['accountCci'] = 'El CCI es obligatorio para transferencias.';
      } else if (cleanCci.length !== 20) {
        errors['accountCci'] = 'El CCI debe tener exactamente 20 dígitos numéricos.';
      }
    } else {
      const cleanPhone = this.phoneNumber().replace(/\D/g, '');
      if (!cleanPhone) {
        errors['phoneNumber'] = 'El número de celular es obligatorio.';
      } else if (cleanPhone.length !== 9 || !cleanPhone.startsWith('9')) {
        errors['phoneNumber'] = 'El número de Yape/Plin debe tener 9 dígitos y empezar con 9.';
      }
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      this.generalError.set('Por favor, revisa los campos señalados antes de guardar.');
      return;
    }

    const payload: UpdatePayoutDestinationRequest = {
      payoutChannel: this.channel(),
      bankName: this.channel() === 'BANK_TRANSFER_CCI' ? this.bankName() : undefined,
      accountNumber: this.channel() === 'BANK_TRANSFER_CCI' ? (this.accountNumber().trim() || undefined) : undefined,
      accountCci: this.channel() === 'BANK_TRANSFER_CCI' ? this.cci().replace(/\D/g, '') : undefined,
      phoneNumber: this.channel() === 'YAPE_PHONE' ? this.phoneNumber().replace(/\D/g, '') : undefined,
      accountHolderName: this.accountHolderName().trim(),
      accountHolderDocType: this.docType(),
      accountHolderDocNumber: this.docNumber().trim(),
    };

    this.isSubmitting.set(true);
    this.billingService.updatePayoutDestination(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeModal.emit();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const { summary, fieldErrors } = extractApiErrorMessage(err, 'Error al guardar la cuenta de liquidación.');
        this.generalError.set(summary);
        this.fieldErrors.set(fieldErrors);
      },
    });
  }

  onClose(): void {
    if (!this.isSubmitting()) {
      this.closeModal.emit();
    }
  }
}
