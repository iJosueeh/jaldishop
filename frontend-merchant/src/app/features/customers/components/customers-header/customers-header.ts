import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matDownloadOutline, matGroupOutline } from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-customers-header',
  imports: [NgIcon],
  providers: [
    provideIcons({
      matDownloadOutline,
      matGroupOutline,
    }),
  ],
  templateUrl: './customers-header.html',
  styleUrl: './customers-header.css',
})
export class CustomersHeader {
  readonly totalCustomers = input<number>(0);
  readonly exportCsv = output<void>();

  onExportCsv(): void {
    this.exportCsv.emit();
  }
}
