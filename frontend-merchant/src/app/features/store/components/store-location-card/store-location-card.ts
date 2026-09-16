import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-store-location-card',
  styleUrl: './store-location-card.css',
  templateUrl: './store-location-card.html',
})
export class StoreLocationCard {
  readonly form = input<FormGroup>(new FormGroup({}));
}
