import { Component, inject, input, output } from '@angular/core';
import { ProfileService } from '../../../../core/services/profile.service';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matMenuOutline,
  matSearchOutline,
  matSpeedOutline,
  matAddCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matMenuOutline,
      matSearchOutline,
      matSpeedOutline,
      matAddCircleOutline,
    }),
  ],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly profileService = inject(ProfileService);

  readonly toggleSidebar = output<void>();
  readonly operationRhythmMin = input<number>(0);
}
