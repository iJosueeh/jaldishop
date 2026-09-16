import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-store-identity-card',
  styleUrl: './store-identity-card.css',
  templateUrl: './store-identity-card.html',
})
export class StoreIdentityCard {
  readonly form = input<FormGroup>(new FormGroup({}));
  readonly slug = input<string>('');
}
