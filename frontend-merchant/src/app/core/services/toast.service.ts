import { Injectable, signal } from '@angular/core';
import { Toast, ToastType } from '../models/api-error.models';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  show(type: ToastType, message: string, title?: string, durationMs: number = 4000): string {
    const id = crypto.randomUUID();
    const newToast: Toast = { id, type, title, message, durationMs, dismissing: false };

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
    const target = this.toasts().find((t) => t.id === id);
    if (!target) return;
    if (target.dismissing) return;

    // Marcamos como en proceso de salida para disparar la animación en la UI
    this.toasts.update((current) =>
      current.map((t) => (t.id === id ? { ...t, dismissing: true } : t)),
    );

    // Se remueve de la lista al finalizar la animación
    setTimeout(() => {
      this.toasts.update((current) => current.filter((t) => t.id !== id));
    }, 240);
  }

  clear(): void {
    this.toasts.set([]);
  }
}
