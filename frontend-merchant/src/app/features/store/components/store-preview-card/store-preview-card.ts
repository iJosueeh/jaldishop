import { Component, input, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPreviewOutline,
  matStorefrontOutline,
  matStoreOutline,
  matMopedOutline,
  matCheckOutline,
  matContentCopyOutline,
  matPowerSettingsNewOutline,
  matPauseCircleOutline,
} from '@ng-icons/material-symbols/outline';
import { StoreResponse } from '../../../../core/models/store.models';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matPreviewOutline,
      matStorefrontOutline,
      matStoreOutline,
      matMopedOutline,
      matCheckOutline,
      matContentCopyOutline,
      matPowerSettingsNewOutline,
      matPauseCircleOutline,
    }),
  ],
  selector: 'app-store-preview-card',
  styleUrl: './store-preview-card.css',
  templateUrl: './store-preview-card.html',
})
export class StorePreviewCard {
  readonly form = input<FormGroup>(new FormGroup({}));
  readonly store = input<StoreResponse | null>(null);

  readonly isCopied = signal<boolean>(false);

  copyLink(): void {
    const slug = this.store()?.slug || 'mi-tienda';
    const url = `https://jaldishop.pe/tienda/${slug}`;
    navigator.clipboard.writeText(url).then(() => {
      this.isCopied.set(true);
      setTimeout(() => this.isCopied.set(false), 2000);
    });
  }
}

