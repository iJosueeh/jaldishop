import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocationOnOutline,
  matPinDropOutline,
  matNearMeOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [ReactiveFormsModule, NgIcon],
  providers: [
    provideIcons({
      matLocationOnOutline,
      matPinDropOutline,
      matNearMeOutline,
      matWarningOutline,
    }),
  ],
  selector: 'app-store-location-card',
  styleUrl: './store-location-card.css',
  templateUrl: './store-location-card.html',
})
export class StoreLocationCard {
  readonly form = input<FormGroup>(new FormGroup({}));
}

