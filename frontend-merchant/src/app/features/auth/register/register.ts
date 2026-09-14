import { Component, signal } from '@angular/core';
import { RegisterBrandPanel } from './components/register-brand-panel/register-brand-panel';
import {
  RegisterFormPanel,
  RegisterStep1Data,
} from './components/register-form-panel/register-form-panel';
import {
  RegisterStep2Data,
  RegisterStep2Brand,
} from './components/register-step2-brand/register-step2-brand';
import { RegisterStep2Form } from './components/register-step2-form/register-step2-form';
import { RegisterStep3Data, RegisterStep3Form } from './components/register-step3-form/register-step3-form';
import { RegisterStep3Brand } from './components/register-step3-brand/register-step3-brand';

@Component({
  imports: [RegisterBrandPanel, RegisterFormPanel, RegisterStep2Brand, RegisterStep2Form, RegisterStep3Brand, RegisterStep3Form],
  selector: 'app-register',
  styleUrl: './register.css',
  templateUrl: './register.html',
})
export class Register {
  readonly currentStep = signal<1 | 2 | 3>(1);

  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly step1Data = signal<RegisterStep1Data | null>(null);
  readonly step2Data = signal<RegisterStep2Data | null>(null);
  readonly step3Data = signal<RegisterStep3Data | null>(null);

  onStep1Submit(data: RegisterStep1Data): void {
    this.step1Data.set(data);
    this.currentStep.set(2);
  }

  onStep2Back(): void {
    this.currentStep.set(1);
  }

  onStep2Submit(data: RegisterStep2Data): void {
    this.step2Data.set(data);
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
    console.log('Paso 2 omitido, avanzando...');
    this.currentStep.set(3)
  }

  onStep3Back(): void {
    this.currentStep.set(2);
  }

  onStep3Change(data: RegisterStep3Data): void {
    this.step3Data.set(data);
  }

  onStep3Submit(data: RegisterStep3Data): void {
    this.step3Data.set(data);
    console.log('Todo listo para crear usuario y tienda:', {
      step1: this.step1Data(),
      step2: this.step2Data(),
      step3: this.step3Data(),
    });
  }
}
