import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matBadgeOutline,
  matStorefrontOutline,
  matLockOutline,
  matChatOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [ReactiveFormsModule, NgIcon],
  providers: [
    provideIcons({
      matBadgeOutline,
      matStorefrontOutline,
      matLockOutline,
      matChatOutline,
    }),
  ],
  selector: 'app-store-identity-card',
  styleUrl: './store-identity-card.css',
  templateUrl: './store-identity-card.html',
})
export class StoreIdentityCard {
  readonly form = input<FormGroup>(new FormGroup({}));
  readonly slug = input<string>('');
}

