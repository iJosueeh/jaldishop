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
        if (this.authService.isAdmin() && !this.authService.isMerchant()) {
          this.toastService.success('¡Bienvenido a tu panel de administración!', 'Sesión iniciada');
          this.router.navigate(['/admin/dashboard']);
        } else if (this.authService.isMerchant()) {
          this.toastService.success('¡Bienvenido a tu panel de JaldiShop!', 'Sesión iniciada');
          this.router.navigate(['/dashboard']);
        } else {
          this.toastService.info(
            'Tu cuenta está lista. Vamos a dar de alta tu tienda en sencillos pasos.',
            '¡Bienvenido de vuelta!',
          );
          this.router.navigate(['/register'], { queryParams: { flow: 'open-store' } });
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        // 1. Error de rol / tienda eliminada / error local lanzado por AuthService
        if (err instanceof Error && !('status' in err)) {
          this.errorMessage.set(err.message);
          return;
        }

        // 2. Credenciales incorrectas
        if (err.status === 401) {
          this.errorMessage.set('Correo o contraseña incorrectos.');
          return;
        }

        // 3. Prohibido / sin permisos
        if (err.status === 403) {
          this.errorMessage.set('Tu cuenta no tiene permisos para acceder al portal de comerciantes.');
          return;
        }

        // 4. Servidor caído o sin conexión a internet
        if (err.status === 0) {
          this.errorMessage.set('No se pudo conectar con el servidor. Revisa tu conexión a internet o intenta en un momento.');
          return;
        }

        // 5. Error interno del servidor
        if (err.status >= 500) {
          this.errorMessage.set('El servidor experimentó un inconveniente temporal. Por favor, reintenta en breve.');
          return;
        }

        // 6. Mensaje explícito del backend o fallback
        this.errorMessage.set(err.error?.message || err.message || 'No fue posible iniciar sesión. Intenta nuevamente.');
      },
    });
  }
}
