import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import {
  InventoryFilterStatus,
  InventoryService,
} from '../../core/services/inventory.service';
import { SettingsService } from '../../core/services/settings.service';
import { ToastService } from '../../core/services/toast.service';
import { InventoryResponse } from '../../core/models/inventory.models';
import { InventoryHeader } from './components/inventory-header/inventory-header';
import { InventoryFilterBar } from './components/inventory-filter-bar/inventory-filter-bar';
import { InventoryTable } from './components/inventory-table/inventory-table';
import { Pagination } from '../../shared/components/pagination/pagination';
import { InventoryStockDrawer } from './components/inventory-stock-drawer/inventory-stock-drawer';
import { InventoryThresholdModal } from './components/inventory-threshold-modal/inventory-threshold-modal';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [
    CommonModule,
    InventoryHeader,
    InventoryFilterBar,
    InventoryTable,
    Pagination,
    InventoryStockDrawer,
    InventoryThresholdModal,
  ],
  templateUrl: './inventory.html',
  styleUrl: './inventory.css',
})
export class Inventory implements OnInit, OnDestroy {
  readonly inventoryService = inject(InventoryService);
  readonly settingsService = inject(SettingsService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  readonly selectedVariantForStock = signal<InventoryResponse | null>(null);
  readonly isStockDrawerOpen = signal<boolean>(false);

  readonly selectedVariantForThreshold = signal<InventoryResponse | null>(null);
  readonly isThresholdModalOpen = signal<boolean>(false);

  readonly pagedInventory = computed(() => {
    const items = this.inventoryService.filteredInventory();
    const page = this.currentPage();
    const size = this.pageSize();
    const start = (page - 1) * size;
    return items.slice(start, start + size);
  });

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap.get('q');
    if (query) {
      this.inventoryService.setSearchQuery(query);
    }
    this.inventoryService.setViewActive(true);
    this.settingsService.loadSettings();
    this.inventoryService.getStoreInventory().subscribe({
      error: () => {
        this.toast.error('No se pudo cargar el inventario de la tienda.');
      },
    });
  }

  ngOnDestroy(): void {
    this.inventoryService.setViewActive(false);
  }

  onRefresh(): void {
    this.inventoryService.getStoreInventory(true).subscribe({
      next: () => {
        this.toast.success('Inventario sincronizado.');
      },
      error: () => {
        this.toast.error('Error al sincronizar el inventario.');
      },
    });
  }

  onSearchChange(term: string): void {
    this.inventoryService.setSearchQuery(term);
    this.currentPage.set(1);
  }

  onFilterChange(status: InventoryFilterStatus): void {
    this.inventoryService.setStatusFilter(status);
    this.currentPage.set(1);
  }

  onClearFilters(): void {
    this.inventoryService.clearFilters();
    this.currentPage.set(1);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    if (typeof window !== 'undefined' && typeof window.scrollTo === 'function') {
      try {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      } catch {
        // Ignorar en entornos de test sin implementación completa de scrollTo
      }
    }
  }

  openStockDrawer(item: InventoryResponse): void {
    this.selectedVariantForStock.set(item);
    this.isStockDrawerOpen.set(true);
  }

  closeStockDrawer(): void {
    this.isStockDrawerOpen.set(false);
    this.selectedVariantForStock.set(null);
  }

  onStockSaved(item: InventoryResponse): void {
    this.toast.success(
      `Stock físico actualizado a ${item.physicalQuantity ?? 0} unidades.`,
    );
  }

  openThresholdModal(item: InventoryResponse): void {
    this.selectedVariantForThreshold.set(item);
    this.isThresholdModalOpen.set(true);
  }

  closeThresholdModal(): void {
    this.isThresholdModalOpen.set(false);
    this.selectedVariantForThreshold.set(null);
  }

  onThresholdSaved(item: InventoryResponse): void {
    if (item.lowStockThreshold !== null) {
      this.toast.success(
        `Umbral de stock bajo actualizado a ${item.lowStockThreshold} unidades.`,
      );
    } else {
      this.toast.success('Alerta de stock bajo desactivada.');
    }
  }
}
