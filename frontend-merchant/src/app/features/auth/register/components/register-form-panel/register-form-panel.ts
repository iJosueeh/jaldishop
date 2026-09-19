import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RegisterFooter } from '../../../../../shared/components/register-footer/register-footer';
import { RegisterStepHeader } from '../../../../../shared/components/register-step-header/register-step-header';
import { AlertError } from '../../../../../shared/components/alert-error/alert-error';
import { RegisterStep1Data } from '../../../../../core/models/register.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matVisibilityOutline,
  matVisibilityOffOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    RegisterFooter,
    RegisterStepHeader,
    AlertError,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matVisibilityOutline,
      matVisibilityOffOutline,
    }),
  ],
  selector: 'app-register-form-panel',
  styleUrl: './register-form-panel.css',
  templateUrl: './register-form-panel.html',
})
export class RegisterFormPanel implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly initialData = input<RegisterStep1Data | null>();

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

  ngOnInit(): void {
    const data = this.initialData();
    if (data) {
      this.registerForm.patchValue(data);
    }
  }

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
