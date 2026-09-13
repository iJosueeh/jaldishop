import { Component, signal } from '@angular/core';
import { RegisterBrandPanel } from './components/register-brand-panel/register-brand-panel';
import {
  RegisterFormPanel,
  RegisterStep1Data,
} from './components/register-form-panel/register-form-panel';
import { RegisterStep2Data, RegisterStep2Brand } from './components/register-step2-brand/register-step2-brand';
import { RegisterStep2Form } from './components/register-step2-form/register-step2-form';

@Component({
  imports: [RegisterBrandPanel, RegisterFormPanel, RegisterStep2Brand, RegisterStep2Form],
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

  onStep1Submit(data: RegisterStep1Data): void {
    this.step1Data.set(data);
    this.currentStep.set(2);
  }

  onStep2Back(): void {
    this.currentStep.set(1);
  }

  onStep2Submit(data: RegisterStep2Data): void {
    this.step2Data.set(data);
    console.log('Paso 1 listo:', this.step1Data());
    console.log('Paso 2 listo:', this.step2Data());
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
        deliveryEnabled: true
      })
    }
    console.log('Paso 2 omitido, avanzando...')
  }

}
