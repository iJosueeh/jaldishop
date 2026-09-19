import { Component, signal } from '@angular/core';
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
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitted = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly sentEmail = signal<string | null>('');

  readonly forgotForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
  });

  handleSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    const email = this.forgotForm.getRawValue().email.trim();
    this.isLoading.set(true);
    this.errorMessage.set(null);

    setTimeout(() => {
      this.isLoading.set(false);
      this.sentEmail.set(email);
      this.isSubmitted.set(true);
    }, 600);
  }

  resetForm(): void {
    this.forgotForm.reset();
    this.errorMessage.set(null);
    this.isSubmitted.set(false);
  }

}
