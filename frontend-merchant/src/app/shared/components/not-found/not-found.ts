import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth-service';

@Component({
  imports: [RouterLink],
  selector: 'app-not-found',
  styleUrl: './not-found.css',
  templateUrl: './not-found.html',
})
export class NotFound {
  private readonly authService = inject(AuthService);

  readonly isAuthenticated = computed(() => this.authService.isAuthenticated());
}
