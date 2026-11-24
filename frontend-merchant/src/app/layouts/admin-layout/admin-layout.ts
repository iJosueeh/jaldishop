import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AdminHeader } from './components/admin-header/admin-header';
import { AdminSidebar } from './components/admin-sidebar/admin-sidebar';
import { AdminCommandPalette } from './components/admin-command-palette/admin-command-palette';
import { AdminService } from '../../core/services/admin.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  imports: [AdminHeader, AdminSidebar, AdminCommandPalette, RouterOutlet],
  selector: 'app-admin-layout',
  styleUrl: './admin-layout.css',
  templateUrl: './admin-layout.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLayout implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly profileService = inject(ProfileService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isMobileMenuOpen = signal<boolean>(false);
  readonly isMobileOverlayRendered = signal<boolean>(false);
  readonly isMobileOverlayClosing = signal<boolean>(false);
  readonly isCommandPaletteOpen = signal<boolean>(false);
  private overlayTimeoutId?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.overlayTimeoutId) {
        clearTimeout(this.overlayTimeoutId);
      }
    });
  }

  ngOnInit(): void {
    if (!this.profileService.currentProfile()) {
      this.profileService.getMyProfile().subscribe({
        error: (err) => console.error('Error al cargar perfil admin: ', err),
      });
    }

    this.adminService.loadUsers().subscribe();
    this.adminService.loadStores().subscribe();
  }

  toggleMobileMenu(): void {
    if (this.isMobileMenuOpen()) {
      this.closeMobileMenu();
    } else {
      if (this.overlayTimeoutId) {
        clearTimeout(this.overlayTimeoutId);
        this.overlayTimeoutId = undefined;
      }
      this.isMobileOverlayRendered.set(true);
      this.isMobileOverlayClosing.set(false);
      this.isMobileMenuOpen.set(true);
    }
  }

  closeMobileMenu(): void {
    if (!this.isMobileMenuOpen()) return;
    this.isMobileMenuOpen.set(false);
    this.isMobileOverlayClosing.set(true);
    if (this.overlayTimeoutId) {
      clearTimeout(this.overlayTimeoutId);
    }
    this.overlayTimeoutId = setTimeout(() => {
      this.isMobileOverlayRendered.set(false);
      this.isMobileOverlayClosing.set(false);
      this.overlayTimeoutId = undefined;
    }, 180);
  }

  openCommandPalette(): void {
    this.isCommandPaletteOpen.set(true);
  }

  closeCommandPalette(): void {
    this.isCommandPaletteOpen.set(false);
  }
}
