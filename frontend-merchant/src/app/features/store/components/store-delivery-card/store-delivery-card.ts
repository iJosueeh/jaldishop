import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocalShippingOutline,
  matStoreOutline,
  matMopedOutline,
  matReceiptOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [ReactiveFormsModule, NgIcon],
  providers: [
    provideIcons({
      matLocalShippingOutline,
      matStoreOutline,
      matMopedOutline,
      matReceiptOutline,
    }),
  ],
  selector: 'app-store-delivery-card',
  styleUrl: './store-delivery-card.css',
  templateUrl: './store-delivery-card.html',
})
export class StoreDeliveryCard {
  readonly form = input<FormGroup>(new FormGroup({}));
}

