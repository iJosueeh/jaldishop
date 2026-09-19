import { Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-profile-info-card',
  styleUrl: './profile-info-card.css',
  templateUrl: './profile-info-card.html',
})
export class ProfileInfoCard {
  readonly form = input<FormGroup>(new FormGroup({}));
  readonly isLoading = input<boolean>(false);
  readonly save = output<void>();

  onSubmit(): void {
    this.save.emit();
  }
}
