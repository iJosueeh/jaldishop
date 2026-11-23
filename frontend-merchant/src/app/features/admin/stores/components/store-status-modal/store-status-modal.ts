import { Component, input, output, signal } from '@angular/core';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matWarningOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matWarningOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-store-status-modal',
  styleUrl: './store-status-modal.css',
  templateUrl: './store-status-modal.html',
})
export class StoreStatusModal {
  readonly isOpen = input<boolean>(false);
  readonly store = input<AdminStoreSummary | null>(null);
  readonly isClosing = signal<boolean>(false);

  readonly confirm = output<AdminStoreSummary>();
  readonly cancel = output<void>();

  handleCancel(): void {
    if (this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.cancel.emit();
    }, 200);
  }
}
