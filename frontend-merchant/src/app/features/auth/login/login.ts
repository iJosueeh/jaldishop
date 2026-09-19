import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth-service';
import { Router } from '@angular/router';
import { BrandPanel } from './components/brand-panel/brand-panel';
import { FormPanel } from './components/form-panel/form-panel';
import { LoginRequest } from '../../../core/models/auth.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  imports: [BrandPanel, FormPanel],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  onLogin(credentials: LoginRequest): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.toastService.success('¡Bienvenido a tu panel de JaldiShop!', 'Sesión iniciada');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Correo o contraseña incorrectos.');
        } else if (err.status === 403) {
          this.errorMessage.set('Tu cuenta no tiene permisos de comerciante.');
        } else {
          this.errorMessage.set(err.error?.message || 'Error de conexión con el servidor.');
        }
      },
    });
  }
}
