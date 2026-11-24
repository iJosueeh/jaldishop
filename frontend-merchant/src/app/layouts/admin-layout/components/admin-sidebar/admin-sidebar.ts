import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth-service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matDashboardOutline,
  matGroupOutline,
  matStorefrontOutline,
  matPersonOutline,
  matSettingsOutline,
  matCloseOutline,
  matLogoutOutline,
  matAnalyticsOutline,
  matBoltOutline,
  matChevronRightOutline,
  matHistoryEduOutline,
} from '@ng-icons/material-symbols/outline';

export interface AdminSidebarSections {
  supervision: boolean;
  ecosystem: boolean;
  finances: boolean;
  governance: boolean;
}

@Component({
  imports: [RouterLink, RouterLinkActive, NgIcon],
  providers: [
    provideIcons({
      matDashboardOutline,
      matGroupOutline,
      matStorefrontOutline,
      matPersonOutline,
      matSettingsOutline,
      matCloseOutline,
      matLogoutOutline,
      matAnalyticsOutline,
      matBoltOutline,
      matChevronRightOutline,
      matHistoryEduOutline,
    }),
  ],
  selector: 'app-admin-sidebar',
  styleUrl: './admin-sidebar.css',
  templateUrl: './admin-sidebar.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSidebar implements OnInit {
  readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isOpen = input<boolean>(false);
  readonly closeSidebar = output<void>();

  // Secciones Colapsables del Sidebar con interfaz tipada explícita
  readonly openSections = signal<AdminSidebarSections>({
    supervision: true,
    ecosystem: true,
    finances: true,
    governance: true,
  });

  toggleSection(sectionKey: keyof AdminSidebarSections): void {
    this.openSections.update((prev) => ({
      ...prev,
      [sectionKey]: !prev[sectionKey],
    }));
  }

  ngOnInit(): void {
    if (this.adminService.users().length === 0) {
      this.adminService.loadUsers().subscribe();
    }
    if (this.adminService.stores().length === 0) {
      this.adminService.loadStores().subscribe();
    }

    this.autoExpandActiveSection(this.router.url);
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.autoExpandActiveSection(event.urlAfterRedirects || event.url);
      });
  }

  private autoExpandActiveSection(url: string): void {
    if (url.includes('/admin/analytics') || url.includes('/admin/dashboard')) {
      this.openSections.update((s) => ({ ...s, supervision: true }));
    }
    if (url.includes('/admin/users') || url.includes('/admin/stores') || url.includes('/admin/merchants')) {
      this.openSections.update((s) => ({ ...s, ecosystem: true }));
    }
    if (url.includes('/admin/reconciliation')) {
      this.openSections.update((s) => ({ ...s, finances: true }));
    }
    if (url.includes('/admin/settings') || url.includes('/admin/audit-logs')) {
      this.openSections.update((s) => ({ ...s, governance: true }));
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
