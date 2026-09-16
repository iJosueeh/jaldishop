import { Component, computed, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  imports: [],
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
