import { Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-profile-info-card',
  styleUrl: './profile-info-card.css',
  templateUrl: './profile-info-card.html',
})
export class ProfileInfoCard {
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
}
