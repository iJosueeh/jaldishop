import {
  Component,
  computed,
  effect,
  HostListener,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matInventory2Outline,
  matAddOutline,
  matCheckOutline,
  matWarningOutline,
  matInfoOutline,
  matTuneOutline,
  matVisibilityOutline,
  matVisibilityOffOutline,
  matAttachMoneyOutline,
  matCategoryOutline,
  matLayersOutline,
  matEditOutline,
  matSkilletOutline,
  matDeleteOutline,
} from '@ng-icons/material-symbols/outline';
import {
  Product,
  ProductCategory,
  ProductImage,
  ProductVariant,
  UpdateProductRequest,
  CreateProductVariantRequest,
  UpdateProductVariantRequest,
} from '../../../../core/models/product.models';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { InventoryService } from '../../../../core/services/inventory.service';
import { InventoryResponse } from '../../../../core/models/inventory.models';
import {
  VariantInventoryCoordinatorService,
  CreateVariantWithInventoryRequest,
  VariantInventoryStepFailed,
} from '../../../../core/services/variant-inventory-coordinator.service';
import { map, of, switchMap } from 'rxjs';
import { ProductGalleryUploader } from '../product-gallery-uploader/product-gallery-uploader';
import { generateProductSku } from '../product-create-drawer/product-create-drawer';
import { DestructiveActionModal } from '../../../../shared/components/destructive-action-modal/destructive-action-modal';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';
import { ApiErrorMetadata } from '../../../../core/models/api-error.models';

@Component({
  selector: 'app-product-edit-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon, ProductGalleryUploader, DestructiveActionModal],
  providers: [
    provideIcons({
      matCloseOutline,
      matInventory2Outline,
      matAddOutline,
      matCheckOutline,
      matWarningOutline,
      matInfoOutline,
      matTuneOutline,
      matVisibilityOutline,
      matVisibilityOffOutline,
      matAttachMoneyOutline,
      matCategoryOutline,
      matLayersOutline,
      matEditOutline,
      matSkilletOutline,
      matDeleteOutline,
    }),
  ],
  templateUrl: './product-edit-drawer.html',
  styleUrl: './product-edit-drawer.css',
})
export class ProductEditDrawer {
  private readonly productService = inject(ProductService);
  private readonly storeService = inject(StoreService);
  private readonly mediaUploadService = inject(MediaUploadService);
  private readonly coordinator = inject(VariantInventoryCoordinatorService);
  readonly inventoryService = inject(InventoryService);
  private readonly toast = inject(ToastService);

  readonly inventoryMap = computed(() => {
    const list = this.inventoryService.inventory();
    const map = new Map<string, InventoryResponse>();
    for (const item of list) {
      map.set(item.variantId, item);
    }
    return map;
  });

  readonly isOpen = input<boolean>(false);
  readonly product = input<Product | null>(null);
  readonly categories = input<ProductCategory[]>([]);
  readonly initialTab = input<'general' | 'variants'>('general');

  readonly closeDrawer = output<void>();
  readonly productUpdated = output<Product>();

  readonly isClosing = signal<boolean>(false);
  readonly activeTab = signal<'general' | 'variants'>('general');

  // Formulario General
  readonly name = signal<string>('');
  readonly description = signal<string>('');
  readonly selectedCategoryId = signal<string>('');
  readonly slug = signal<string>('');
  readonly status = signal<'ACTIVE' | 'INACTIVE'>('ACTIVE');
  readonly images = signal<ProductImage[]>([]);
  readonly imageUrl = signal<string>('');

  // Gestión de Variantes (Formatos y Precios)
  readonly variants = signal<ProductVariant[]>([]);
  readonly isLoadingVariants = signal<boolean>(false);
  readonly isAddingVariant = signal<boolean>(false);
  readonly newVariantName = signal<string>('');
  readonly newVariantPrice = signal<number | null>(null);
  readonly newVariantSku = signal<string>('');
  readonly newVariantTracksInventory = signal<boolean>(false);
  readonly newVariantInitialStock = signal<number | null>(null);
  readonly newVariantLowStockThreshold = signal<number | null>(null);
  readonly pendingCreatedVariant = signal<ProductVariant | null>(null);
  readonly pendingInventoryStep = signal<VariantInventoryStepFailed>(null);
  readonly isSavingVariant = signal<boolean>(false);

  // Edición Inline de Variante Existente
  readonly editingVariantId = signal<string | null>(null);
  readonly editVariantName = signal<string>('');
  readonly editVariantPrice = signal<number | null>(null);
  readonly editVariantSku = signal<string>('');
  readonly editVariantTracksInventory = signal<boolean>(false);
  readonly editVariantInitialStock = signal<number | null>(null);
  readonly editVariantLowStockThreshold = signal<number | null>(null);
  readonly isSavingEditVariant = signal<boolean>(false);

  readonly isSavingGeneral = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Edición directa de Precio y Disponibilidad en pestaña General (cuando es formato único)
  readonly singleVariantPrice = signal<number | null>(null);

  readonly isSingleVariant = computed(() => this.variants().length <= 1);
  readonly firstVariant = computed(() => (this.variants().length > 0 ? this.variants()[0] : null));

  // Computados
  readonly isGeneralFormValid = computed(
    () => {
      const basicValid = this.name().trim().length >= 2 && !!this.selectedCategoryId();
      if (!basicValid) return false;
      if (this.isSingleVariant() && this.singleVariantPrice() !== null) {
        return Number(this.singleVariantPrice()) > 0;
      }
      return true;
    },
  );

  readonly isNewVariantValid = computed(() => {
    const hasValidName = this.newVariantName().trim().length >= 2;
    const hasValidPrice =
      this.newVariantPrice() !== null && this.newVariantPrice()! > 0;
    if (!hasValidName || !hasValidPrice) return false;

    if (this.newVariantTracksInventory()) {
      const stock = this.newVariantInitialStock();
      if (stock === null || !Number.isInteger(stock) || stock < 0) {
        return false;
      }
      const threshold = this.newVariantLowStockThreshold();
      if (
        threshold !== null &&
        threshold !== undefined &&
        (!Number.isInteger(threshold) || threshold < 0)
      ) {
        return false;
      }
    }
    return true;
  });

  constructor() {
    // Sincronizar datos del producto cuando cambie el input product o se abra el drawer
    effect(() => {
      const p = this.product();
      const open = this.isOpen();
      const tab = this.initialTab();
      if (open && p) {
        this.initFormData(p);
        this.loadVariants(p);
        this.activeTab.set(tab);
      }
    });
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event?: Event): void {
    if (this.isOpen() && !this.isClosing()) {
      event?.preventDefault();
      this.handleClose();
    }
  }

  handleClose(): void {
    if (this.isSavingGeneral() || this.isSavingVariant()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.activeTab.set(this.initialTab());
      this.isAddingVariant.set(false);
      this.errorMessage.set(null);
      this.closeDrawer.emit();
    }, 220);
  }

  setActiveTab(tab: 'general' | 'variants'): void {
    this.activeTab.set(tab);
    this.errorMessage.set(null);
  }

  initFormData(p: Product): void {
    this.name.set(p.name || '');
    this.description.set(p.description || '');
    this.selectedCategoryId.set(p.categoryId || '');
    this.slug.set(p.slug || '');
    this.status.set(p.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE');

    let initialImages: ProductImage[] = [];
    if (p.images && p.images.length > 0) {
      initialImages = p.images.map((img, idx) => ({
        ...img,
        position: idx,
        isPrimary: img.isPrimary ?? idx === 0,
      }));
    } else if (p.imageUrl && p.imageUrl.trim().length > 0) {
      initialImages = [
        {
          id: p.id ? `img-${p.id}` : undefined,
          productId: p.id,
          imageUrl: p.imageUrl.trim(),
          position: 0,
          isPrimary: true,
        },
      ];
    }

    this.images.set(initialImages);
    this.imageUrl.set(p.imageUrl || (initialImages[0]?.imageUrl ?? ''));
    this.singleVariantPrice.set(p.minPrice != null ? p.minPrice : (p.variants && p.variants[0] ? p.variants[0].priceAmount : null));
    this.errorMessage.set(null);
    this.isAddingVariant.set(false);
  }

  loadVariants(p: Product): void {
    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId || !p.id) return;

    // 1. Inventario: solo consultar si no está cargado previamente en memoria
    if (!this.inventoryService.isLoaded()) {
      this.inventoryService.getStoreInventory().subscribe();
    }

    // 2. Cache-First estricto para variantes: si ya residen en memoria en el producto o servicio
    const cachedVariants =
      p.variants && p.variants.length > 0
        ? p.variants
        : this.productService.getCachedVariants?.(p.id) ?? null;

    if (cachedVariants && cachedVariants.length > 0) {
      this.variants.set(cachedVariants);
      this.isLoadingVariants.set(false);
      this.productService.syncProductVariants(p.id, cachedVariants);
      if (this.singleVariantPrice() === null) {
        this.singleVariantPrice.set(cachedVariants[0].priceAmount);
      }
      return;
    }

    this.isLoadingVariants.set(true);
    this.productService.getProductVariants(storeId, p.id).subscribe({
      next: (data) => {
        const variantsList = data || [];
        this.variants.set(variantsList);
        this.isLoadingVariants.set(false);
        this.productService.syncProductVariants(p.id, variantsList);
        if (variantsList.length > 0 && this.singleVariantPrice() === null) {
          this.singleVariantPrice.set(variantsList[0].priceAmount);
        }
      },
      error: () => {
        // Si la llamada falla o no tiene variantes aún, utilizar variantes en memoria si existen
        this.variants.set(p.variants || []);
        this.isLoadingVariants.set(false);
      },
    });
  }

  onImagesChange(newImages: ProductImage[]): void {
    this.images.set(newImages);
    const primary = newImages.find((img) => img.isPrimary) || newImages[0];
    this.imageUrl.set(primary ? primary.imageUrl : '');
  }

  saveGeneralInfo(): void {
    const p = this.product();
    if (!p || !this.isGeneralFormValid()) return;

    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId) return;

    this.isSavingGeneral.set(true);
    this.errorMessage.set(null);

    const pendingImages = this.images();
    const hasFilesToUpload = pendingImages.some((img) => !!img.file);

    const uploadStep$ = hasFilesToUpload
      ? this.mediaUploadService.uploadPendingImages(pendingImages, 'PRODUCT_IMAGE', storeId)
      : of(pendingImages);

    uploadStep$.pipe(
      switchMap((finalImages) => {
        const primary = finalImages.find((img) => img.isPrimary) || finalImages[0];
        const primaryUrl = primary ? primary.imageUrl : undefined;

        const payload: UpdateProductRequest = {
          name: this.name().trim(),
          description: this.description().trim() || undefined,
          categoryId: this.selectedCategoryId(),
          slug: this.slug().trim() || undefined,
          status: this.status(),
          imageUrl: primaryUrl,
          images: finalImages,
        };

        return this.productService.updateProduct(storeId, p.id, payload).pipe(
          switchMap((updatedProd) => {
            const vList = this.variants();
            const newPrice = this.singleVariantPrice();
            if (this.isSingleVariant() && vList.length === 1 && newPrice !== null && newPrice > 0 && newPrice !== vList[0].priceAmount) {
              const v = vList[0];
              const updateVarPayload: UpdateProductVariantRequest = {
                presentationName: v.presentationName,
                priceAmount: Number(newPrice),
                priceCurrency: v.priceCurrency || 'PEN',
                sku: v.sku || undefined,
                tracksInventory: v.tracksInventory,
                status: v.status,
              };
              return this.productService.updateProductVariant(storeId, p.id, v.id, updateVarPayload).pipe(
                map((updatedVar) => {
                  const newList = [updatedVar];
                  this.variants.set(newList);
                  this.productService.syncProductVariants(p.id, newList);
                  return updatedProd;
                }),
              );
            }
            return of(updatedProd);
          }),
        );
      }),
    ).subscribe({
      next: (updatedProduct) => {
        this.isSavingGeneral.set(false);
        this.toast.success(
          `Los datos de "${updatedProduct.name}" se actualizaron correctamente.`,
          'Cambios Guardados',
        );
        this.productUpdated.emit(updatedProduct);
      },
      error: (err) => {
        this.isSavingGeneral.set(false);
        const msg =
          err?.error?.message ||
          err?.message ||
          'No se pudo actualizar la información del producto.';
        this.errorMessage.set(msg);
      },
    });
  }

  toggleAddVariant(): void {
    const current = this.isAddingVariant();
    this.isAddingVariant.set(!current);
    if (!current) {
      this.newVariantName.set('');
      this.newVariantPrice.set(null);
      this.newVariantSku.set('');
      this.newVariantTracksInventory.set(false);
      this.newVariantInitialStock.set(null);
      this.newVariantLowStockThreshold.set(null);
      this.pendingCreatedVariant.set(null);
      this.pendingInventoryStep.set(null);
      this.errorMessage.set(null);
    }
  }

  onNewVariantNameChange(val: string): void {
    this.newVariantName.set(val);
    if (!this.newVariantSku()) {
      const prodName = this.product()?.name || '';
      this.newVariantSku.set(generateProductSku(`${prodName} ${val}`));
    }
  }

  onNewVariantInitialStockChange(val: any): void {
    const num = val === '' || val === null || val === undefined ? null : Number(val);
    this.newVariantInitialStock.set(num);
  }

  onNewVariantLowStockThresholdChange(val: any): void {
    const num = val === '' || val === null || val === undefined ? null : Number(val);
    this.newVariantLowStockThreshold.set(num);
  }

  retryPendingVariantInventory(): void {
    this.submitNewVariant();
  }

  submitNewVariant(): void {
    const p = this.product();
    if (!p || !this.isNewVariantValid()) return;

    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId) return;

    this.isSavingVariant.set(true);
    this.errorMessage.set(null);

    const pendingVariant = this.pendingCreatedVariant();

    // Si ya existe una variante creada cuyo inventario falló, reintentar solo el inventario sin duplicar
    if (pendingVariant) {
      const stock = this.newVariantInitialStock() ?? 0;
      const threshold = this.newVariantLowStockThreshold();

      this.coordinator.initializePendingInventory(pendingVariant, stock, threshold).subscribe({
        next: (result) => {
          this.isSavingVariant.set(false);
          if (result.success) {
            const currentList = this.variants().some((v) => v.id === pendingVariant.id)
              ? this.variants()
              : [...this.variants(), pendingVariant];
            this.variants.set(currentList);
            this.productService.syncProductVariants(p.id, currentList);
            this.toast.success(
              `Inventario configurado exitosamente para "${pendingVariant.presentationName}".`,
              '¡Inventario Inicializado!',
            );
            this.pendingCreatedVariant.set(null);
            this.pendingInventoryStep.set(null);
            this.toggleAddVariant();
          } else {
            this.errorMessage.set(result.errorMessage);
            this.pendingInventoryStep.set(result.stepFailed);
            this.toast.error(
              result.errorMessage || 'No se pudo completar la inicialización del inventario.',
            );
          }
        },
        error: (err) => {
          this.isSavingVariant.set(false);
          const msg = err?.message || 'Error al reintentar la inicialización de inventario.';
          this.errorMessage.set(msg);
        },
      });
      return;
    }

    const payload: CreateVariantWithInventoryRequest = {
      presentationName: this.newVariantName().trim(),
      priceAmount: Number(this.newVariantPrice()),
      priceCurrency: 'PEN',
      sku: this.newVariantSku().trim() || undefined,
      tracksInventory: this.newVariantTracksInventory(),
      initialStock: this.newVariantInitialStock(),
      lowStockThreshold: this.newVariantLowStockThreshold(),
    };

    this.coordinator.createVariantWithInventory(storeId, p.id, payload).subscribe({
      next: (result) => {
        this.isSavingVariant.set(false);
        if (result.success && result.variant) {
          const currentList = [...this.variants(), result.variant];
          this.variants.set(currentList);
          this.productService.syncProductVariants(p.id, currentList);
          this.toast.success(
            `Formato "${result.variant.presentationName}" agregado a S/ ${result.variant.priceAmount.toFixed(2)}.`,
            '¡Formato Agregado!',
          );
          this.toggleAddVariant();
        } else {
          // Manejo de fallo parcial
          if (result.variant) {
            this.pendingCreatedVariant.set(result.variant);
            this.pendingInventoryStep.set(result.stepFailed);
            if (result.stepFailed === 'THRESHOLD') {
              // Si solo falló el umbral, la variante ya tiene stock
              const currentList = [...this.variants(), result.variant];
              this.variants.set(currentList);
              this.productService.syncProductVariants(p.id, currentList);
            }
          }
          this.errorMessage.set(result.errorMessage);
          this.toast.error(
            result.errorMessage || 'Ocurrió un error al registrar el formato.',
          );
        }
      },
      error: (err) => {
        this.isSavingVariant.set(false);
        const msg = err?.message || 'No se pudo agregar la nueva presentación.';
        this.errorMessage.set(msg);
      },
    });
  }

  toggleVariantStatus(variant: ProductVariant): void {
    const p = this.product();
    if (!p) return;

    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId) return;

    const newStatus = variant.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

    const payload: UpdateProductVariantRequest = {
      presentationName: variant.presentationName,
      priceAmount: variant.priceAmount,
      priceCurrency: variant.priceCurrency || 'PEN',
      sku: variant.sku || undefined,
      tracksInventory: variant.tracksInventory,
      status: newStatus,
    };

    this.productService.updateProductVariant(storeId, p.id, variant.id, payload).subscribe({
      next: (updatedVariant) => {
        const updatedList = this.variants().map((v) =>
          v.id === updatedVariant.id ? updatedVariant : v,
        );
        this.variants.set(updatedList);
        this.productService.syncProductVariants(p.id, updatedList);
        const statusLabel = newStatus === 'ACTIVE' ? 'activado' : 'pausado';
        this.toast.success(`Formato "${variant.presentationName}" ${statusLabel}.`);
      },
      error: () => {
        this.toast.error('No se pudo actualizar el estado de este formato.');
      },
    });
  }

  startEditVariant(variant: ProductVariant): void {
    this.editingVariantId.set(variant.id);
    this.editVariantName.set(variant.presentationName);
    this.editVariantPrice.set(variant.priceAmount);
    this.editVariantSku.set(variant.sku || '');
    this.editVariantTracksInventory.set(variant.tracksInventory);
  }

  cancelEditVariant(): void {
    this.editingVariantId.set(null);
  }

  saveEditVariant(variant: ProductVariant): void {
    const p = this.product();
    if (!p) return;

    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId) return;

    const name = this.editVariantName().trim();
    const price = this.editVariantPrice();
    if (!name || price === null || price <= 0) {
      this.toast.error('Ingresa un nombre y precio válidos para este formato.');
      return;
    }

    this.isSavingEditVariant.set(true);

    const payload: UpdateProductVariantRequest = {
      presentationName: name,
      priceAmount: Number(price),
      priceCurrency: variant.priceCurrency || 'PEN',
      sku: this.editVariantSku().trim() || undefined,
      tracksInventory: this.editVariantTracksInventory(),
      status: variant.status,
    };

    this.productService.updateProductVariant(storeId, p.id, variant.id, payload).subscribe({
      next: (updatedVariant) => {
        this.isSavingEditVariant.set(false);
        const updatedList = this.variants().map((v) =>
          v.id === updatedVariant.id ? updatedVariant : v,
        );
        this.variants.set(updatedList);
        this.productService.syncProductVariants(p.id, updatedList);
        this.editingVariantId.set(null);
        this.toast.success(`Formato "${updatedVariant.presentationName}" actualizado exitosamente.`);
      },
      error: (err) => {
        this.isSavingEditVariant.set(false);
        const msg = err?.error?.message || err?.message || 'No se pudo actualizar el formato.';
        this.toast.error(msg);
      },
    });
  }

  private readonly errorHandler = inject(ErrorHandlerService);
  readonly deleteTarget = signal<
    | { type: 'product'; product: Product }
    | { type: 'variant'; variant: ProductVariant }
    | null
  >(null);
  readonly isDeletingTarget = signal<boolean>(false);
  readonly deleteErrorStatus = signal<number | null>(null);
  readonly deleteErrorMessage = signal<string | null>(null);
  readonly deleteConflictMetadata = signal<ApiErrorMetadata | null>(null);

  requestDeleteProduct(): void {
    const p = this.product();
    if (!p) return;
    this.deleteErrorStatus.set(null);
    this.deleteErrorMessage.set(null);
    this.deleteConflictMetadata.set(null);
    this.deleteTarget.set({ type: 'product', product: p });
  }

  requestDeleteVariant(variant: ProductVariant): void {
    this.deleteErrorStatus.set(null);
    this.deleteErrorMessage.set(null);
    this.deleteConflictMetadata.set(null);
    this.deleteTarget.set({ type: 'variant', variant });
  }

  cancelDeleteTarget(): void {
    if (!this.isDeletingTarget()) {
      this.deleteTarget.set(null);
    }
  }

  confirmDeleteTarget(): void {
    const target = this.deleteTarget();
    const p = this.product();
    const storeId = this.storeService.currentStore()?.id || p?.storeId;
    if (!target || !p || !storeId) return;

    this.isDeletingTarget.set(true);
    if (target.type === 'product') {
      this.productService.deleteProduct(storeId, p.id).subscribe({
        next: (res) => {
          this.isDeletingTarget.set(false);
          this.deleteTarget.set(null);
          this.toast.success(res.message);
          this.handleClose();
        },
        error: (err) => this.handleDeleteError(err),
      });
    } else {
      this.productService.deleteProductVariant(storeId, p.id, target.variant.id).subscribe({
        next: (res) => {
          this.isDeletingTarget.set(false);
          this.deleteTarget.set(null);
          this.variants.update((list) => list.filter((v) => v.id !== target.variant.id));
          this.toast.success(res.message);
        },
        error: (err) => this.handleDeleteError(err),
      });
    }
  }

  fallbackDeactivateTarget(): void {
    const target = this.deleteTarget();
    const p = this.product();
    const storeId = this.storeService.currentStore()?.id || p?.storeId;
    if (!target || !p || !storeId) return;

    this.isDeletingTarget.set(true);
    if (target.type === 'product') {
      this.productService.updateProductStatus(storeId, p.id, 'INACTIVE').subscribe({
        next: () => {
          this.isDeletingTarget.set(false);
          this.deleteTarget.set(null);
          this.toast.info(`Producto "${p.name}" desactivado.`);
        },
        error: (err) => this.handleDeleteError(err),
      });
    } else {
      this.productService.updateVariantStatus(storeId, p.id, target.variant.id, 'INACTIVE').subscribe({
        next: (updated) => {
          this.isDeletingTarget.set(false);
          this.deleteTarget.set(null);
          this.variants.update((list) => list.map((v) => (v.id === updated.id ? updated : v)));
          this.toast.info(`Formato "${updated.presentationName}" desactivado.`);
        },
        error: (err) => this.handleDeleteError(err),
      });
    }
  }

  private handleDeleteError(err: unknown): void {
    this.isDeletingTarget.set(false);
    const normalized = this.errorHandler.normalize(err);
    if (normalized.status === 409 || normalized.status === 403) {
      this.deleteErrorStatus.set(normalized.status);
      this.deleteErrorMessage.set(normalized.message);
      this.deleteConflictMetadata.set(normalized.metadata ?? null);
    } else {
      this.deleteTarget.set(null);
      this.toast.error(normalized.message);
    }
  }
}
