import { Component, computed, inject, input, OnInit, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { outputFromObservable, toSignal } from '@angular/core/rxjs-interop';
import { RegisterFooter } from '../../../../../shared/components/register-footer/register-footer';
import { RegisterStepHeader } from '../../../../../shared/components/register-step-header/register-step-header';
import { AlertError } from '../../../../../shared/components/alert-error/alert-error';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';
import { RegisterStep2Data } from '../../interface/register.models';

@Component({
  imports: [ReactiveFormsModule, RegisterFooter, RegisterStepHeader, AlertError, BusinessAvatar],
  selector: 'app-register-step2-form',
  styleUrl: './register-step2-form.css',
  templateUrl: './register-step2-form.html',
})
export class RegisterStep2Form implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);

  readonly initialData = input<RegisterStep2Data | null>(null);

  readonly back = output<void>();
  readonly step2Submit = output<RegisterStep2Data>();
  readonly skip = output<void>();

  readonly step2Form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    businessType: ['Repostería y pastelería', [Validators.required]],
    contactPhone: ['', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
    pickupEnabled: [true],
    deliveryEnabled: [true],
    address: [''],
  }, { validators: atLeastOneDeliveryMethodValidator });

  ngOnInit(): void {
    const data = this.initialData();
    if (data) {
      this.step2Form.patchValue(data);
    }
  }

  readonly formChange = outputFromObservable<RegisterStep2Data>(this.step2Form.valueChanges);

  readonly formValues = toSignal(this.step2Form.valueChanges, {
    initialValue: this.step2Form.getRawValue(),
  });

  onBack(): void {
    this.back.emit();
  }

  handleContinue(): void {
    if (this.step2Form.invalid || this.isLoading()) {
      this.step2Form.markAllAsTouched();
      return;
    }
    this.step2Submit.emit(this.step2Form.getRawValue());
  }

  handleSkip(): void {
    this.skip.emit();
  }
}

export const atLeastOneDeliveryMethodValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const pickup = control.get('pickupEnabled')?.value;
  const delivery = control.get('deliveryEnabled')?.value;
  return !pickup && !delivery ? { noDeliveryMethod: true } : null;
};
