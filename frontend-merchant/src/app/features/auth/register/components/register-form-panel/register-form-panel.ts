import { Component, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

export interface RegisterStep1Data {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  passwordConfirm: string;
  terms: boolean
}

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-register-form-panel',
  styleUrl: './register-form-panel.css',
  templateUrl: './register-form-panel.html',
})
export class RegisterFormPanel {
  private readonly fb = inject(FormBuilder)
  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);
  readonly step1Submit = output<RegisterStep1Data>();

  readonly showPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);

  readonly registerForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    passwordConfirm: ['', [Validators.required]],
    terms: [false, [Validators.requiredTrue]],
  }, { validators: passwordMatchValidator });

  togglePassword() {
    this.showPassword.update((show) => !show);
  }

  toggleConfirmPassword(){
    this.showConfirmPassword.update((show) => !show)
  }

  handleContinue() {
    if (this.registerForm.invalid || this.isLoading()) {
      this.registerForm.markAllAsTouched();
      return;
    }
    this.step1Submit.emit(this.registerForm.getRawValue());
  }

}

export const passwordMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirm = control.get('passwordConfirm')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
};
