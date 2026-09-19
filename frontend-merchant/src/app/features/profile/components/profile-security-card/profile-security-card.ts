import { Component, inject, input } from '@angular/core';
import { UserProfile } from '../../../../core/models/user-profile.models';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../../../core/services/auth-service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPhotoCameraOutline,
  matCheckCircleOutline,
  matChatOutline,
  matPhoneDisabledOutline,
  matLogoutOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [DatePipe, NgIcon],
  providers: [
    provideIcons({
      matPhotoCameraOutline,
      matCheckCircleOutline,
      matChatOutline,
      matPhoneDisabledOutline,
      matLogoutOutline,
    }),
  ],
  selector: 'app-profile-security-card',
  styleUrl: './profile-security-card.css',
  templateUrl: './profile-security-card.html',
})
export class ProfileSecurityCard {
  private readonly authService = inject(AuthService);

  readonly profile = input<UserProfile | null>(null);
  readonly storeName = input<string>('Mi tienda');
  readonly userInitials = input<string>('U');

  onLogout(): void {
    this.authService.logout();
  }
}

