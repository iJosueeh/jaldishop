import { Component, computed, inject, input, output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { outputFromObservable, toSignal } from '@angular/core/rxjs-interop'
import { RegisterStep2Data } from '../register-step2-brand/register-step2-brand';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-register-step2-form',
  styleUrl: './register-step2-form.css',
  templateUrl: './register-step2-form.html',
})
export class RegisterStep2Form {
  private readonly fb = inject(FormBuilder);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);

  readonly back = output<void>();
  readonly step2Submit = output<RegisterStep2Data>();
  readonly skip = output<void>();

  readonly step2Form: FormGroup = this.fb.group({
    name: ['Dulce Clara', [Validators.required, Validators.minLength(2)]],
    businessType: ['Reposteria y pasteleria', [Validators.required]],
    contactPhone: ['999999999', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
    pickupEnabled: [true],
    deliveryEnabled: [true],
    address: ['Av. Primavera 452, Santiago de Surco'],
  });

  readonly formChange = outputFromObservable<RegisterStep2Data>(this.step2Form.valueChanges);

  private readonly formValues = toSignal(this.step2Form.valueChanges, {
    initialValue: this.step2Form.getRawValue(),
  });

  readonly previewInitial = computed(
    () => this.formValues().name?.trim()?.charAt(0)?.toUpperCase() || 'D',
  );

  onBack(): void {
    this.back.emit();
  }

  handleContinue(): void {
    if (this.step2Form.invalid ||  this.isLoading()) {
      this.step2Form.markAllAsTouched();
      return;
    }
    this.step2Submit.emit(this.step2Form.getRawValue());
  }

  handleSkip(): void {
    this.skip.emit();
  }

}
