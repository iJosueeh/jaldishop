import { Component, inject, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AlertError } from '../../../shared/components/alert-error/alert-error';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLockResetOutline,
  matLockOutline,
  matVisibilityOutline,
  matVisibilityOffOutline,
  matArrowForwardOutline,
  matArrowBackOutline,
  matCheckCircleOutline,
  matErrorOutline,
  matLoginOutline,
  matRefreshOutline,
} from '@ng-icons/material-symbols/outline';
import { AuthService } from '../../../core/services/auth-service';
import { timeout, TimeoutError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

export const RESET_TOKEN_REGEX = /^[A-Za-z0-9_-]{43}$/;

export const utf8MaxBytesValidator = (maxBytes: number): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const bytes = new TextEncoder().encode(control.value).length;
    return bytes > maxBytes ? { maxBytes: { max: maxBytes, actual: bytes } } : null;
  };
};

export const passwordConfirmMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const password = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
};

@Component({
  imports: [ReactiveFormsModule, RouterLink, AlertError, NgIcon],
  providers: [
    provideIcons({
      matLockResetOutline,
      matLockOutline,
      matVisibilityOutline,
      matVisibilityOffOutline,
      matArrowForwardOutline,
      matArrowBackOutline,
      matCheckCircleOutline,
      matErrorOutline,
      matLoginOutline,
      matRefreshOutline,
    }),
  ],
  selector: 'app-reset-password',
  styleUrl: './reset-password.css',
  templateUrl: './reset-password.html',
})
export class ResetPassword implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  readonly isLoading = signal<boolean>(false);
  readonly isSuccess = signal<boolean>(false);
  readonly isTokenInvalid = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);

  // Token is stored strictly in memory and never in storage or cookies
  readonly token = signal<string | null>(null);

  readonly resetForm = new FormGroup(
    {
      newPassword: new FormControl('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          utf8MaxBytesValidator(72),
        ],
      }),
      confirmPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    { validators: passwordConfirmMatchValidator },
  );

  ngOnInit(): void {
    const rawToken = this.route.snapshot.queryParamMap.get('token');

    if (!rawToken || !RESET_TOKEN_REGEX.test(rawToken.trim())) {
      this.isTokenInvalid.set(true);
      this.token.set(null);
    } else {
      this.token.set(rawToken.trim());
      this.isTokenInvalid.set(false);
    }

    // Clean token from address bar as soon as possible without refreshing or saving to history
    if (typeof window !== 'undefined' && window.history.replaceState) {
      if (window.location.search.includes('token') || rawToken) {
        window.history.replaceState({}, document.title, window.location.pathname);
      }
    }
  }

  togglePassword(): void {
    this.showPassword.update((val) => !val);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((val) => !val);
  }

  handleSubmit(): void {
    if (this.isLoading()) {
      return;
    }

    if (this.isTokenInvalid() || !this.token()) {
      this.isTokenInvalid.set(true);
      return;
    }

    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword } = this.resetForm.getRawValue();
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService
      .resetPassword({
        token: this.token()!,
        newPassword,
      })
      .pipe(timeout(15000))
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.isSuccess.set(true);
          // Purge token and passwords from memory upon successful completion
          this.token.set(null);
          this.resetForm.reset();
        },
        error: (err: unknown) => {
          this.isLoading.set(false);

          if (err instanceof TimeoutError || (err as any)?.name === 'TimeoutError') {
            this.errorMessage.set(
              'La solicitud al servidor excedió el tiempo límite. Revisa tu conexión o intenta en un momento.',
            );
            return;
          }

          if (err instanceof HttpErrorResponse) {
            if (err.status === 0) {
              this.errorMessage.set(
                'No se pudo conectar con el servidor. Revisa tu conexión a internet o intenta en un momento.',
              );
              return;
            }

            if (err.status === 422 || err.error?.code === 'INVALID_PASSWORD_RESET_TOKEN') {
              this.isTokenInvalid.set(true);
              this.token.set(null);
              return;
            }

            if (err.status === 400) {
              this.errorMessage.set(
                err.error?.message ||
                  'La contraseña ingresada no cumple con los requisitos del servidor.',
              );
              return;
            }

            if (err.status === 429) {
              this.errorMessage.set(
                'Has alcanzado el límite de intentos de restablecimiento. Por favor, espera unos minutos antes de reintentar.',
              );
              return;
            }

            if (err.status >= 500) {
              this.errorMessage.set(
                'El servidor experimentó un inconveniente temporal. Por favor, reintenta en breve.',
              );
              return;
            }
          }

          this.errorMessage.set('No fue posible restablecer la contraseña. Intenta nuevamente.');
        },
      });
  }
}
