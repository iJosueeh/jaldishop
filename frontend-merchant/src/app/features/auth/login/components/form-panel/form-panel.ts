import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LoginRequest } from '../../../../../core/models/auth.models';
import { RouterLink } from '@angular/router';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-form-panel',
  styleUrl: './form-panel.css',
  templateUrl: './form-panel.html',
})
export class FormPanel {
  private readonly fb = inject(FormBuilder);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);

  readonly loginSubmit = output<LoginRequest>();
  
  readonly showPassword = signal(false);

  readonly loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  togglePassword(): void {
    this.showPassword.update((show) => !show);
  }

  handleSubmit(): void {
    if (this.loginForm.invalid || this.isLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.loginSubmit.emit(this.loginForm.getRawValue());
  }
}
