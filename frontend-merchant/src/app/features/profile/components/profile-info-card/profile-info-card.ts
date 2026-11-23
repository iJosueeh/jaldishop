import { Component, inject, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPersonOutline,
  matMailOutline,
  matCallOutline,
  matNotificationsOutline,
  matShieldOutline,
  matSupportAgentOutline,
} from '@ng-icons/material-symbols/outline';
import { SettingsService } from '../../../../core/services/settings.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink, NgIcon],
  providers: [
    provideIcons({
      matPersonOutline,
      matMailOutline,
      matCallOutline,
      matNotificationsOutline,
      matShieldOutline,
      matSupportAgentOutline,
    }),
  ],
  selector: 'app-profile-info-card',
  styleUrl: './profile-info-card.css',
  templateUrl: './profile-info-card.html',
})
export class ProfileInfoCard {
  readonly settingsService = inject(SettingsService);

  readonly form = input<FormGroup>(
    new FormGroup({
      firstName: new FormControl(''),
      lastName: new FormControl(''),
      phone: new FormControl(''),
    }),
  );

  readonly email = input<string>('');
  readonly isLoading = input<boolean>(false);
  readonly save = output<void>();
  readonly discard = output<void>();

  onSubmit(): void {
    this.save.emit();
  }

  onToggleOrderSoundAlert(checked: boolean): void {
    this.settingsService.saveSettings({ ...this.settingsService.settings(), orderSoundAlert: checked });
  }

  onToggleCapacityAlert(checked: boolean): void {
    this.settingsService.saveSettings({ ...this.settingsService.settings(), lowStockVisualAlert: checked });
  }
}

