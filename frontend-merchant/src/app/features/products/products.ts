import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ProductService } from '../../core/services/product.service';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { Product, ProductViewMode } from '../../core/models/product.models';
import { ProductsHeader } from './components/products-header/products-header';
import { ProductsAlertBanner } from './components/products-alert-banner/products-alert-banner';
import { ProductsFilterBar } from './components/products-filter-bar/products-filter-bar';
import { ProductsGrid } from './components/products-grid/products-grid';
import { ProductsList } from './components/products-list/products-list';
import { ProductCreateDrawer } from './components/product-create-drawer/product-create-drawer';
import { ProductEditDrawer } from './components/product-edit-drawer/product-edit-drawer';
import { CategoryManagerModal } from './components/category-manager-modal/category-manager-modal';
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
    ProductCreateDrawer,
    ProductEditDrawer,
    CategoryManagerModal,
    Pagination,
  ],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products implements OnInit {
  readonly productService = inject(ProductService);
  readonly storeService = inject(StoreService);
  private readonly toast = inject(ToastService);

  readonly isCreateDrawerOpen = signal<boolean>(false);
  readonly isEditDrawerOpen = signal<boolean>(false);
  readonly isCategoryModalOpen = signal<boolean>(false);
  readonly selectedProductForEdit = signal<Product | null>(null);
  readonly editDrawerInitialTab = signal<'general' | 'variants'>('general');
  private readonly router = inject(Router);

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
    this.editDrawerInitialTab.set('general');
    this.selectedProductForEdit.set(product);
    this.isEditDrawerOpen.set(true);
  }

  onCloseEditDrawer(): void {
    this.isEditDrawerOpen.set(false);
    this.selectedProductForEdit.set(null);
  }

  onProductUpdated(updatedProduct: Product): void {
    this.selectedProductForEdit.set(updatedProduct);
  }

  onAdjustQuota(product: Product): void {
    if (!product) return;
    this.editDrawerInitialTab.set('variants');
    this.selectedProductForEdit.set(product);
    this.isEditDrawerOpen.set(true);
  }

  onAdjustAvailability(): void {
    this.router.navigate(['/capacity']);
  }

  onViewInventory(product: Product): void {
    this.router.navigate(['/inventory'], { queryParams: { q: product.name } });
  }

  onOpenStorePreview(_product: Product): void {
    const slug = this.storeService.storeSlug() || this.storeService.currentStore()?.slug;
    const url = slug
      ? `${environment.marketplaceUrl}/tienda/${slug}`
      : `${environment.marketplaceUrl}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  onCreateProduct(): void {
    this.isCreateDrawerOpen.set(true);
  }

  onCloseCreateDrawer(): void {
    this.isCreateDrawerOpen.set(false);
  }

  onProductCreated(_product: Product): void {
    this.isCreateDrawerOpen.set(false);
  }
}
