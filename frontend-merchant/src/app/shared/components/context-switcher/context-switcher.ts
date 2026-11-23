import {
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth-service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matStorefrontOutline,
  matShieldOutline,
  matSwapHorizOutline,
  matCheckOutline,
  matKeyboardArrowDownOutline,
} from '@ng-icons/material-symbols/outline';

export type PortalContext = 'MERCHANT' | 'ADMIN';

@Component({
  selector: 'app-context-switcher',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matStorefrontOutline,
      matShieldOutline,
      matSwapHorizOutline,
      matCheckOutline,
      matKeyboardArrowDownOutline,
    }),
  ],
  templateUrl: './context-switcher.html',
  styleUrl: './context-switcher.css',
})
export class ContextSwitcher {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef);

  readonly isOpen = signal<boolean>(false);
  readonly currentUrl = signal<string>(this.router.url || '');

  constructor() {
    this.router.events
      ?.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      ?.subscribe((e) => {
        this.currentUrl.set(e.urlAfterRedirects || e.url);
      });
  }

  readonly currentContext = computed<PortalContext>(() => {
    const url = this.currentUrl();
    return url.startsWith('/admin') ? 'ADMIN' : 'MERCHANT';
  });

  readonly isDualRole = computed(() => this.authService.isDualRole());

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent): void {
    // Atajo accesible: Alt + S (o Alt + s) para alternar rápidamente el panel de contexto
    if (event.altKey && event.key.toLowerCase() === 's') {
      event.preventDefault();
      if (this.isDualRole()) {
        this.toggleDropdown();
      }
    } else if (event.key === 'Escape' && this.isOpen()) {
      this.isOpen.set(false);
    }
  }

  toggleDropdown(): void {
    this.isOpen.update((v) => !v);
  }

  switchContext(target: PortalContext): void {
    this.isOpen.set(false);
    if (target === this.currentContext()) {
      return;
    }

    if (target === 'ADMIN') {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
