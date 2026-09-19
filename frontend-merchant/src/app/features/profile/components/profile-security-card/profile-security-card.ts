import { Component, input } from '@angular/core';
import { UserProfile } from '../../../../core/models/user-profile.models';
import { DatePipe } from '@angular/common';

@Component({
  imports: [DatePipe],
  selector: 'app-profile-security-card',
  styleUrl: './profile-security-card.css',
  templateUrl: './profile-security-card.html',
})
export class ProfileSecurityCard {
  readonly profile = input<UserProfile | null>(null);
}
