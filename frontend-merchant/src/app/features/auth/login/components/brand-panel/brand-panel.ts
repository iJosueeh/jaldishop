import { Component } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matCheckCircleOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-brand-panel',
  styleUrl: './brand-panel.css',
  templateUrl: './brand-panel.html',
})
export class BrandPanel {}

