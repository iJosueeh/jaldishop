import { Component, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly toggleSidebar = output<void>();
  readonly operationRhythmMin = input<number>(0);
}
