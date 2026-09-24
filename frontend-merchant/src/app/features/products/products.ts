import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../core/services/product.service';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { Product, ProductViewMode } from '../../core/models/product.models';
import { ProductsHeader } from './components/products-header/products-header';
import { ProductsAlertBanner } from './components/products-alert-banner/products-alert-banner';
import { ProductsFilterBar } from './components/products-filter-bar/products-filter-bar';
import { ProductsGrid } from './components/products-grid/products-grid';
import { ProductsList } from './components/products-list/products-list';
import { Pagination } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule,
    ProductsHeader,
    ProductsAlertBanner,
    ProductsFilterBar,
    ProductsGrid,
    ProductsList,
    Pagination,
  ],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products implements OnInit {
  readonly productService = inject(ProductService);
  readonly storeService = inject(StoreService);
  private readonly toast = inject(ToastService);

  ngOnInit(): void {
    this.initCatalog();
  }

  initCatalog(forceRefresh = false): void {
    const currentStore = this.storeService.currentStore();
    if (currentStore) {
      this.loadCatalogData(currentStore.id, forceRefresh);
    } else {
      this.storeService.getMyStore().subscribe({
        next: (store) => {
          if (store) {
            this.loadCatalogData(store.id, forceRefresh);
          }
        },
      });
    }
  }

  private loadCatalogData(storeId: string, forceRefresh = false): void {
    this.productService.loadCategories(storeId, forceRefresh).subscribe({
      error: (err) => console.error('Error al cargar categorías:', err),
    });

    this.productService.loadProducts(storeId, forceRefresh).subscribe({
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.toast.error('No se pudieron cargar los productos del catálogo.');
      },
    });
  }

  onCategoryChange(categoryId: string | null): void {
    this.productService.setCategoryFilter(categoryId);
  }

  onSearchChange(query: string): void {
    this.productService.setSearchQuery(query);
  }

  onStatusChange(status: string): void {
    this.productService.setStatusFilter(status);
  }

  onViewModeChange(mode: ProductViewMode): void {
    this.productService.setViewMode(mode);
  }

  onPageChange(page: number): void {
    this.productService.setPage(page);
  }

  onClearFilters(): void {
    this.productService.clearFilters();
  }

  onToggleStatus(product: Product): void {
    const store = this.storeService.currentStore();
    const storeId = store?.id || product.storeId;
    if (!storeId) return;

    this.productService.toggleProductStatus(storeId, product).subscribe({
      next: (updated) => {
        const statusText = updated.status === 'ACTIVE' ? 'activado' : 'pausado';
        this.toast.success(`Producto "${product.name}" ${statusText}`);
      },
      error: () => {
        this.toast.error('No se pudo actualizar el estado del producto.');
      },
    });
  }

  onEditProduct(product: Product): void {
    this.toast.info(`Editar "${product.name}" estará disponible en el modal de edición.`);
  }

  onAdjustQuota(product: Product): void {
    this.toast.info(`Ajuste de cupo para "${product.name}"`);
  }

  onCreateProduct(): void {
    this.toast.info('El formulario de nuevo producto se abrirá a continuación.');
  }
}
