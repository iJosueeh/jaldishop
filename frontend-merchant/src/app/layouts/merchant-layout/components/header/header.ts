import { Component, inject, input, output } from '@angular/core';
import { ProfileService } from '../../../../core/services/profile.service';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly profileService = inject(ProfileService);

  readonly toggleSidebar = output<void>();
  readonly operationRhythmMin = input<number>(0);
}
