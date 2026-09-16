import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-store-delivery-card',
  styleUrl: './store-delivery-card.css',
  templateUrl: './store-delivery-card.html',
})
export class StoreDeliveryCard {
  readonly form = input<FormGroup>(new FormGroup({}));
}
