import { Component, input } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matErrorOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matErrorOutline,
    }),
  ],
  selector: 'app-alert-error',
  styleUrl: './alert-error.css',
  templateUrl: './alert-error.html',
})
export class AlertError {
  readonly message = input<string | null>(null);
}

