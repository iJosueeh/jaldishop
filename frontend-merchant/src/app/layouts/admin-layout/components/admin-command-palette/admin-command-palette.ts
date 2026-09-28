import {
  Component,
  computed,
  ElementRef,
  HostListener,
  inject,
  input,
  output,
  signal,
  ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth-service';
import { ToastService } from '../../../../core/services/toast.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matSearchOutline,
  matGroupOutline,
  matStorefrontOutline,
  matPersonOutline,
  matRefreshOutline,
  matLogoutOutline,
  matArrowForwardOutline,
  matCloseOutline,
  matBoltOutline,
} from '@ng-icons/material-symbols/outline';

export interface CommandItem {
  id: string;
  title: string;
  subtitle?: string;
  category: 'ACTION' | 'USER' | 'STORE';
  icon: string;
  badge?: string;
  action: () => void;
}

export interface CommandGroup {
  name: string;
  items: CommandItem[];
}

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matSearchOutline,
      matGroupOutline,
      matStorefrontOutline,
      matPersonOutline,
      matRefreshOutline,
      matLogoutOutline,
      matArrowForwardOutline,
      matCloseOutline,
      matBoltOutline,
    }),
  ],
  selector: 'app-admin-command-palette',
  styleUrl: './admin-command-palette.css',
  templateUrl: './admin-command-palette.html',
})
export class AdminCommandPalette {
  private readonly router = inject(Router);
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);

  readonly isOpen = input<boolean>(false);
  readonly closePalette = output<void>();

  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

  readonly query = signal<string>('');
  readonly selectedIndex = signal<number>(0);
  readonly isClosing = signal<boolean>(false);

  // Límite máximo de resultados para optimizar rendimiento y mantener la interfaz compacta
  private readonly MAX_TOTAL_RESULTS = 5;

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.isOpen() ? this.dismiss() : this.openPalette();
      return;
    }

    if (!this.isOpen() || this.isClosing()) return;

    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        this.dismiss();
        break;

      case 'ArrowDown': {
        event.preventDefault();
        const max = this.filteredItems().length - 1;
        this.selectedIndex.update((i) => (i < max ? i + 1 : 0));
        break;
      }

      case 'ArrowUp': {
        event.preventDefault();
        const max = this.filteredItems().length - 1;
        this.selectedIndex.update((i) => (i > 0 ? i - 1 : max));
        break;
      }

      case 'Enter': {
        event.preventDefault();
        const items = this.filteredItems();
        if (items.length > 0 && items[this.selectedIndex()]) {
          this.executeCommand(items[this.selectedIndex()]);
        }
        break;
      }
    }
  }

  private openPalette(): void {
    this.isClosing.set(false);
    this.query.set('');
    this.selectedIndex.set(0);
    setTimeout(() => this.searchInput?.nativeElement.focus(), 60);
  }

  dismiss(): void {
    if (this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.closePalette.emit();
    }, 160);
  }

  readonly filteredItems = computed<CommandItem[]>(() => {
    const q = this.query().trim().toLowerCase();

    // 1. Si no hay búsqueda, ofrecer únicamente acciones operativas directas
    if (!q) {
      return [
        {
          id: 'act-refresh',
          title: 'Sincronizar Datos de Plataforma',
          subtitle: 'Recargar usuarios y comercios desde la base de datos',
          category: 'ACTION',
          icon: 'matRefreshOutline',
          action: () => {
            this.adminService.loadUsers(true).subscribe();
            this.adminService.loadStores(true).subscribe();
            this.toastService.success(
              'Datos de plataforma sincronizados correctamente.',
              'Actualizado',
            );
            this.dismiss();
          },
        },
        {
          id: 'act-logout',
          title: 'Cerrar Sesión Administrativa',
          subtitle: 'Finalizar la sesión de administración actual',
          category: 'ACTION',
          icon: 'matLogoutOutline',
          action: () => {
            this.dismiss();
            this.authService.logout();
          },
        },
      ];
    }

    const results: CommandItem[] = [];

    // 2. Coincidencias en Usuarios (Búsqueda en memoria reactiva)
    const users = this.adminService.users();
    const matchedUsers: CommandItem[] = [];

    for (const u of users) {
      const full = `${u.firstName} ${u.lastName}`.toLowerCase();
      if (
        full.includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.phone && u.phone.includes(q)) ||
        u.roles.some((r) => r.toLowerCase().includes(q))
      ) {
        matchedUsers.push({
          id: `user-${u.id}`,
          title: `${u.firstName} ${u.lastName}`,
          subtitle: `${u.email} • ${u.roles.join(', ')}`,
          category: 'USER',
          icon: 'matPersonOutline',
          badge: u.status === 'ACTIVE' ? 'Activo' : 'Suspendido',
          action: () => {
            this.adminService.setUserSearchQuery(u.email);
            this.navigateAndClose('/admin/users');
          },
        });
      }
      if (matchedUsers.length >= 3) break;
    }
    results.push(...matchedUsers);

    // 3. Coincidencias en Tiendas (Completar hasta MAX_TOTAL_RESULTS)
    const stores = this.adminService.stores();
    const remainingSlots = this.MAX_TOTAL_RESULTS - results.length;
    const matchedStores: CommandItem[] = [];

    if (remainingSlots > 0) {
      for (const s of stores) {
        if (
          s.name.toLowerCase().includes(q) ||
          s.slug.toLowerCase().includes(q) ||
          (s.contactPhone && s.contactPhone.includes(q)) ||
          (s.merchant?.fullName && s.merchant.fullName.toLowerCase().includes(q))
        ) {
          matchedStores.push({
            id: `store-${s.id}`,
            title: s.name,
            subtitle: `/${s.slug} • Propietario: ${s.merchant?.fullName || 'N/A'}`,
            category: 'STORE',
            icon: 'matStorefrontOutline',
            badge: s.status === 'ACTIVE' ? 'Activa' : 'Suspendida',
            action: () => {
              this.adminService.setStoreSearchQuery(s.name);
              this.navigateAndClose('/admin/stores');
            },
          });
        }
        if (matchedStores.length >= remainingSlots) break;
      }
      results.push(...matchedStores);
    }

    return results;
  });

  readonly groupedItems = computed<CommandGroup[]>(() => {
    const items = this.filteredItems();
    const actionItems = items.filter((i) => i.category === 'ACTION');
    const userItems = items.filter((i) => i.category === 'USER');
    const storeItems = items.filter((i) => i.category === 'STORE');

    const groups: CommandGroup[] = [];

    if (actionItems.length > 0) {
      groups.push({ name: 'Acciones Rápidas', items: actionItems });
    }
    if (userItems.length > 0) {
      groups.push({ name: 'Usuarios', items: userItems });
    }
    if (storeItems.length > 0) {
      groups.push({ name: 'Tiendas', items: storeItems });
    }

    return groups;
  });

  onQueryChange(value: string): void {
    this.query.set(value);
    this.selectedIndex.set(0);
  }

  executeCommand(item: CommandItem): void {
    item.action();
  }

  getItemGlobalIndex(item: CommandItem): number {
    return this.filteredItems().findIndex((i) => i.id === item.id);
  }

  private navigateAndClose(url: string): void {
    this.router.navigate([url]);
    this.dismiss();
  }
}
