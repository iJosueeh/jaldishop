import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AlertError } from '../../../shared/components/alert-error/alert-error';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLockResetOutline,
  matMailOutline,
  matArrowForwardOutline,
  matArrowBackOutline,
  matMarkEmailReadOutline,
  matLoginOutline,
} from '@ng-icons/material-symbols/outline';
import { AuthService } from '../../../core/services/auth-service';
import { timeout, TimeoutError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  imports: [ReactiveFormsModule, RouterLink, AlertError, NgIcon],
  providers: [
    provideIcons({
      matLockResetOutline,
      matMailOutline,
      matArrowForwardOutline,
      matArrowBackOutline,
      matMarkEmailReadOutline,
      matLoginOutline,
    }),
  ],
  selector: 'app-forgot-password',
  styleUrl: './forgot-password.css',
  templateUrl: './forgot-password.html',
})
export class ForgotPassword {
  private readonly authService = inject(AuthService);

  readonly isLoading = signal<boolean>(false);
  readonly isSubmitted = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly sentEmail = signal<string | null>('');

  readonly forgotForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
  });

  handleSubmit(): void {
    if (this.isLoading()) {
      return;
    }

    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    const email = this.forgotForm.getRawValue().email.trim();
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService
      .forgotPassword(email)
      .pipe(timeout(15000))
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.sentEmail.set(email);
          this.isSubmitted.set(true);
        },
        error: (err: unknown) => {
          this.isLoading.set(false);

          if (err instanceof TimeoutError || (err as any)?.name === 'TimeoutError') {
            this.errorMessage.set(
              'La solicitud al servidor excedió el tiempo límite. Revisa tu conexión o intenta en un momento.'
            );
            return;
          }

          if (err instanceof HttpErrorResponse) {
            if (err.status === 0) {
              this.errorMessage.set(
                'No se pudo conectar con el servidor. Revisa tu conexión a internet o intenta en un momento.'
              );
              return;
            }

            if (err.status === 429) {
              this.errorMessage.set(
                'Has alcanzado el límite de intentos de recuperación. Espera 15 minutos antes de intentar de nuevo.'
              );
              return;
            }

            if (err.status >= 500) {
              this.errorMessage.set(
                'El servidor experimentó un inconveniente temporal. Por favor, reintenta en breve.'
              );
              return;
            }

            if (err.status === 400) {
              this.errorMessage.set(
                err.error?.message || 'Ingresa un correo electrónico válido.'
              );
              return;
            }
          }

          this.errorMessage.set(
            'No fue posible procesar la solicitud de recuperación. Intenta nuevamente.'
          );
        },
      });
  }

  resetForm(): void {
    this.forgotForm.reset();
    this.errorMessage.set(null);
    this.isSubmitted.set(false);
  }
}
