import { Component, computed, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCheckCircleOutline,
  matErrorOutline,
  matWarningOutline,
  matInfoOutline,
  matCloseOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCheckCircleOutline,
      matErrorOutline,
      matWarningOutline,
      matInfoOutline,
      matCloseOutline,
    }),
  ],
  selector: 'app-toast-container',
  styleUrl: './toast-container.css',
  templateUrl: './toast-container.html',
})
export class ToastContainer {
  private readonly toastService = inject(ToastService);

  readonly toasts = computed(() => this.toastService.toasts());

  dismiss(id: string): void {
    this.toastService.dismiss(id);
  }
}

