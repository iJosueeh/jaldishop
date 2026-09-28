import { Component, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matCheckOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matCheckOutline })],
  selector: 'app-admin-settings-header',
  styleUrl: './admin-settings-header.css',
  templateUrl: './admin-settings-header.html',
})
export class AdminSettingsHeader {
  readonly save = output<void>();
}
