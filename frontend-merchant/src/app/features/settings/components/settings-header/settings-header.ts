import { Component } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matTuneOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matTuneOutline,
    }),
  ],
  selector: 'app-settings-header',
  styleUrl: './settings-header.css',
  templateUrl: './settings-header.html',
})
export class SettingsHeader {}

