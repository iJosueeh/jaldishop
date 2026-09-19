import { Component, inject, signal } from '@angular/core';
import { RegisterBrandPanel } from './components/register-brand-panel/register-brand-panel';
import { RegisterFormPanel } from './components/register-form-panel/register-form-panel';
import { RegisterStep2Brand } from './components/register-step2-brand/register-step2-brand';
import { RegisterStep2Form } from './components/register-step2-form/register-step2-form';
import { RegisterStep3Brand } from './components/register-step3-brand/register-step3-brand';
import { RegisterStep3Form } from './components/register-step3-form/register-step3-form';
import {
  RegisterStep1Data,
  RegisterStep2Data,
  RegisterStep3Data,
} from '../../../core/models/register.models';
import { AuthService } from '../../../core/services/auth-service';
import { Router } from '@angular/router';
import { AuthResult, RegisterMerchantRequest } from '../../../core/models/auth.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  imports: [
    RegisterBrandPanel,
    RegisterFormPanel,
    RegisterStep2Brand,
    RegisterStep2Form,
    RegisterStep3Brand,
    RegisterStep3Form,
  ],
  selector: 'app-register',
  styleUrl: './register.css',
  templateUrl: './register.html',
})
export class Register {
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly currentStep = signal<1 | 2 | 3>(1);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly step1Data = signal<RegisterStep1Data | null>(null);
  readonly step2Data = signal<RegisterStep2Data | null>(null);
  readonly step3Data = signal<RegisterStep3Data | null>(null);

  onStep1Submit(data: RegisterStep1Data): void {
    this.step1Data.set(data);
    this.currentStep.set(2);
    this.errorMessage.set(null);
  }

  onStep2Back(): void {
    this.errorMessage.set(null);
    this.currentStep.set(1);
  }

  onStep2Submit(data: RegisterStep2Data): void {
    this.step2Data.set(data);
    this.errorMessage.set(null);
    this.currentStep.set(3);
  }

  onStep2Change(data: RegisterStep2Data): void {
    this.step2Data.set(data);
  }

  onStep2Skip(): void {
    if (!this.step2Data()) {
      this.step2Data.set({
        name: 'Mi negocio',
        businessType: 'Otro',
        contactPhone: '999999999',
        pickupEnabled: true,
        deliveryEnabled: true,
      });
    }
    this.errorMessage.set(null);
    this.currentStep.set(3);
  }

  onStep3Back(): void {
    this.errorMessage.set(null);
    this.currentStep.set(2);
  }

  onStep3Change(data: RegisterStep3Data): void {
    this.step3Data.set(data);
  }

  onStep3Submit(data: RegisterStep3Data): void {
    this.step3Data.set(data);

    if (!this.validatePrerequisites()) {
      return;
    }

    this.executeMerchantRegistration();
  }

  private validatePrerequisites(): boolean {
    if (!this.step1Data()) {
      this.currentStep.set(1);
      return false;
    }

    if (!this.step2Data()) {
      this.currentStep.set(2);
      return false;
    }
    return true;
  }

  private buildRegisterPayload(): RegisterMerchantRequest {
    const step1 = this.step1Data()!;
    const step2 = this.step2Data()!;

    return {
      firstName: step1.firstName.trim(),
      lastName: step1.lastName.trim(),
      email: step1.email.trim().toLowerCase(),
      password: step1.password,
      phone: step2.contactPhone?.trim(),
      storeName: step2.name.trim(),
      businessType: step2.businessType,
      storeContactPhone: step2.contactPhone?.trim(),
      address: step2.address?.trim() || undefined,
      pickupEnabled: step2.pickupEnabled,
      deliveryEnabled: step2.deliveryEnabled,
    };
  }

  private executeMerchantRegistration(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const payload = this.buildRegisterPayload();

    this.authService.registerMerchant(payload).subscribe({
      next: (result) => this.handleRegistrationSuccess(result),
      error: (err) => this.handleRegistrationError(err),
    });
  }

  private handleRegistrationSuccess(result: AuthResult): void {
    this.isLoading.set(false);
    this.toastService.success(
      'Tu cuenta y tienda han sido creadas con éxito. Inicia sesión para continuar.',
      '¡Registro exitoso!',
    );
    this.router.navigate(['/login']);
  }

  private handleRegistrationError(err: any): void {
    this.isLoading.set(false);
    const message =
      err?.error?.message ||
      'Ocurrió un error al registrar tu cuenta y negocio. Inténtalo nuevamente.';
    this.errorMessage.set(message);
  }
}
