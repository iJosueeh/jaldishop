import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AdminHeader } from './components/admin-header/admin-header';
import { AdminSidebar } from './components/admin-sidebar/admin-sidebar';
import { AdminService } from '../../core/services/admin.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  imports: [AdminHeader, AdminSidebar, RouterOutlet],
  selector: 'app-admin-layout',
  styleUrl: './admin-layout.css',
  templateUrl: './admin-layout.html',
})
export class AdminLayout implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly profileService = inject(ProfileService);

  readonly isMobileMenuOpen = signal<boolean>(false);

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
    this.isMobileMenuOpen.update((open) => !open);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }
}
