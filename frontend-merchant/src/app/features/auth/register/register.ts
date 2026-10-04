import { Component, inject, OnInit, signal } from '@angular/core';
import { RegisterBrandPanel } from './components/register-brand-panel/register-brand-panel';
import { RegisterFormPanel } from './components/register-form-panel/register-form-panel';
import { RegisterStep2Brand } from './components/register-step2-brand/register-step2-brand';
import { RegisterStep2Form } from './components/register-step2-form/register-step2-form';
import { RegisterStep3LocationBrand } from './components/register-step3-location-brand/register-step3-location-brand';
import { RegisterStep3LocationForm } from './components/register-step3-location-form/register-step3-location-form';
import { RegisterStep4Brand } from './components/register-step4-brand/register-step4-brand';
import { RegisterStep4Form } from './components/register-step4-form/register-step4-form';
import {
  RegisterStep1Data,
  RegisterStep2Data,
  RegisterStep3Data,
  RegisterStep4Data,
} from '../../../core/models/register.models';
import { AuthService } from '../../../core/services/auth-service';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthResult, RegisterMerchantRequest } from '../../../core/models/auth.models';
import { CreateStoreRequest } from '../../../core/models/store.models';
import { StoreService } from '../../../core/services/store.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  imports: [
    RegisterBrandPanel,
    RegisterFormPanel,
    RegisterStep2Brand,
    RegisterStep2Form,
    RegisterStep3LocationBrand,
    RegisterStep3LocationForm,
    RegisterStep4Brand,
    RegisterStep4Form,
  ],
  selector: 'app-register',
  styleUrl: './register.css',
  templateUrl: './register.html',
})
export class Register implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly storeService = inject(StoreService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly currentStep = signal<1 | 2 | 3 | 4>(1);
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isExistingUser = signal<boolean>(false);

  readonly step1Data = signal<RegisterStep1Data | null>(null);
  readonly step2Data = signal<RegisterStep2Data | null>(null);
  readonly step3Data = signal<RegisterStep3Data | null>(null);
  readonly step4Data = signal<RegisterStep4Data | null>(null);

  ngOnInit(): void {
    const isAuth = this.authService.isAuthenticated();
    const isMerchant = this.authService.isMerchant();
    const flow = this.route.snapshot.queryParamMap.get('flow');

    if (isAuth && isMerchant) {
      this.router.navigate(['/dashboard'], { replaceUrl: true });
      return;
    }

    if (isAuth || flow === 'open-store') {
      if (isAuth) {
        this.isExistingUser.set(true);
        this.currentStep.set(2);
        // Verificar si la cuenta ya tiene una tienda activa para redirigir a dashboard
        this.authService.refreshToken().subscribe({
          next: (res) => {
            if (res.roles.includes('MERCHANT')) {
              this.router.navigate(['/dashboard'], { replaceUrl: true });
            }
          },
          error: () => {},
        });
      } else {
        this.router.navigate(['/login'], { replaceUrl: true });
      }
    }
  }

  onStep1Submit(data: RegisterStep1Data): void {
    this.step1Data.set(data);
    this.currentStep.set(2);
    this.errorMessage.set(null);
  }

  onStep2Back(): void {
    this.errorMessage.set(null);
    if (this.isExistingUser()) {
      this.authService.logout();
    } else {
      this.currentStep.set(1);
    }
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
        businessType: 'Restaurantes y Cafeterías',
        contactPhone: '999999999',
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
    this.errorMessage.set(null);
    this.currentStep.set(4);
  }

  onStep3Skip(): void {
    if (!this.step3Data()) {
      this.step3Data.set({
        pickupEnabled: true,
        deliveryEnabled: true,
        address: '',
        addressReference: '',
        latitude: null,
        longitude: null,
      });
    }
    this.errorMessage.set(null);
    this.currentStep.set(4);
  }

  onStep4Back(): void {
    this.errorMessage.set(null);
    this.currentStep.set(3);
  }

  onStep4Change(data: RegisterStep4Data): void {
    this.step4Data.set(data);
  }

  onStep4Submit(data: RegisterStep4Data): void {
    this.step4Data.set(data);

    if (this.isExistingUser()) {
      if (!this.step2Data()) {
        this.currentStep.set(2);
        return;
      }
      this.executeCreateStoreForExistingUser();
    } else {
      if (!this.validatePrerequisites()) {
        return;
      }
      this.executeMerchantRegistration();
    }
  }

  private buildCreateStorePayload(): CreateStoreRequest {
    const step2 = this.step2Data()!;
    const step3 = this.step3Data();

    const rawPhone = step2.contactPhone?.trim();
    const cleanPhone = rawPhone ? rawPhone.replace(/\D/g, '') : '';
    const normalizedPhone = cleanPhone
      ? (cleanPhone.startsWith('51') ? `+${cleanPhone}` : `+51${cleanPhone}`)
      : undefined;

    const payload: CreateStoreRequest = {
      name: step2.name.trim(),
      description: step2.businessType,
      contactPhone: normalizedPhone,
      address: step3?.address?.trim() || step2.address?.trim() || undefined,
      addressReference: step3?.addressReference?.trim() || undefined,
      latitude: step3?.latitude ?? undefined,
      longitude: step3?.longitude ?? undefined,
      pickupEnabled: step3?.pickupEnabled ?? step2.pickupEnabled ?? true,
      deliveryEnabled: step3?.deliveryEnabled ?? step2.deliveryEnabled ?? true,
    };

    if (step2.logoUrl && !step2.logoUrl.startsWith('data:')) {
      payload.logoUrl = step2.logoUrl;
    }
    if (step2.bannerUrl && !step2.bannerUrl.startsWith('data:')) {
      payload.bannerUrl = step2.bannerUrl;
    }
    if (step2.categoryIds && step2.categoryIds.length > 0) {
      payload.categoryIds = step2.categoryIds;
    }

    return payload;
  }

  private executeCreateStoreForExistingUser(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const payload = this.buildCreateStorePayload();

    this.storeService.createStore(payload).subscribe({
      next: () => {
        this.authService.refreshToken().subscribe({
          next: () => {
            this.isLoading.set(false);
            this.toastService.success(
              '¡Tu tienda ha sido creada y configurada con éxito!',
              '¡Bienvenido a tu panel!',
            );
            this.router.navigate(['/dashboard'], { replaceUrl: true });
          },
          error: () => {
            this.isLoading.set(false);
            this.toastService.info(
              '¡Tu tienda ha sido creada con éxito! Inicia sesión para ingresar a tu panel.',
              '¡Bienvenido!',
            );
            this.router.navigate(['/login'], { replaceUrl: true });
          },
        });
      },
      error: (err) => {
        this.isLoading.set(false);
        const code = err?.error?.code;
        if (code === 'MERCHANT_ALREADY_HAS_STORE') {
          this.authService.refreshToken().subscribe({
            next: (res) => {
              if (res.roles.includes('MERCHANT')) {
                this.toastService.info('Tu tienda ya se encuentra activa. Ingresando a tu panel...', 'Tienda activa');
                this.router.navigate(['/dashboard'], { replaceUrl: true });
              } else {
                this.router.navigate(['/login'], { replaceUrl: true });
              }
            },
            error: () => {
              this.router.navigate(['/login'], { replaceUrl: true });
            },
          });
          return;
        }

        const message =
          err?.error?.message ||
          'Ocurrió un error al registrar tu negocio. Inténtalo nuevamente.';
        this.errorMessage.set(message);
      },
    });
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
    const step3 = this.step3Data();

    const rawPhone = step2.contactPhone?.trim();
    const cleanPhone = rawPhone ? rawPhone.replace(/\D/g, '') : '';
    const normalizedPhone = cleanPhone
      ? (cleanPhone.startsWith('51') ? `+${cleanPhone}` : `+51${cleanPhone}`)
      : undefined;

    const payload: RegisterMerchantRequest = {
      firstName: step1.firstName.trim(),
      lastName: step1.lastName.trim(),
      email: step1.email.trim().toLowerCase(),
      password: step1.password,
      phone: normalizedPhone,
      storeName: step2.name.trim(),
      businessType: step2.businessType,
      storeContactPhone: normalizedPhone,
      address: step3?.address?.trim() || step2.address?.trim() || undefined,
      addressReference: step3?.addressReference?.trim() || undefined,
      latitude: step3?.latitude ?? null,
      longitude: step3?.longitude ?? null,
      pickupEnabled: step3?.pickupEnabled ?? step2.pickupEnabled ?? true,
      deliveryEnabled: step3?.deliveryEnabled ?? step2.deliveryEnabled ?? true,
    };

    if (step2.logoUrl && !step2.logoUrl.startsWith('data:')) {
      payload.logoUrl = step2.logoUrl;
    }
    if (step2.bannerUrl && !step2.bannerUrl.startsWith('data:')) {
      payload.bannerUrl = step2.bannerUrl;
    }
    if (step2.categoryIds && step2.categoryIds.length > 0) {
      payload.categoryIds = step2.categoryIds;
    }

    return payload;
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
      '¡Tu tienda ha sido creada y configurada con éxito!',
      '¡Bienvenido a tu panel!',
    );
    this.router.navigate(['/dashboard'], { replaceUrl: true });
  }

  private handleRegistrationError(err: any): void {
    this.isLoading.set(false);
    const message =
      err?.error?.message ||
      'Ocurrió un error al registrar tu cuenta y negocio. Inténtalo nuevamente.';
    this.errorMessage.set(message);
  }
}
