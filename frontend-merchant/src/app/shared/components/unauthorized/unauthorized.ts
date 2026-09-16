import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth-service';

@Component({
  imports: [RouterLink],
  selector: 'app-unauthorized',
  styleUrl: './unauthorized.css',
  templateUrl: './unauthorized.html',
})
export class Unauthorized {
  private readonly authService = inject(AuthService);

  readonly isLogged = this.authService.isAuthenticated;

  handleLogout(): void {
    this.authService.logout();
  }
}
