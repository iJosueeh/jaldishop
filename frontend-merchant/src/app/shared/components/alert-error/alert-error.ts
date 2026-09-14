import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-alert-error',
  styleUrl: './alert-error.css',
  templateUrl: './alert-error.html',
})
export class AlertError {
  readonly message = input<string | null>(null);
}
