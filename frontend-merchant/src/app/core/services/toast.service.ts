import { Service, signal } from '@angular/core';
import { Toast, ToastType } from '../models/api-error.models';

@Service()
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  show(type: ToastType, message: string, title?: string, durationMs: number = 4000): string {
    const id = crypto.randomUUID();
    const newToast: Toast = { id, type, title, message, durationMs };

    this.toasts.update((current) => [...current, newToast]);

    if (durationMs > 0) {
      setTimeout(() => this.dismiss(id), durationMs);
    }

    return id;
  }

  success(message: string, title: string = '¡Operación exitosa!'): string {
    return this.show('success', message, title);
  }

  error(message: string, title: string = 'Ocurrió un error'): string {
    return this.show('error', message, title, 5000);
  }

  warning(message: string, title: string = 'Atención'): string {
    return this.show('warning', message, title);
  }

  info(message: string, title?: string): string {
    return this.show('info', message, title);
  }

  dismiss(id: string): void {
    this.toasts.update((current) => current.filter((t) => t.id !== id));
  }

  clear(): void {
    this.toasts.set([]);
  }
}
