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
import { of, switchMap } from 'rxjs';
import { ProductGalleryUploader } from '../product-gallery-uploader/product-gallery-uploader';
import { generateProductSku } from '../product-create-drawer/product-create-drawer';

@Component({
  selector: 'app-product-edit-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon, ProductGalleryUploader],
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
    }),
  ],
  templateUrl: './product-edit-drawer.html',
  styleUrl: './product-edit-drawer.css',
})
export class ProductEditDrawer {
  private readonly productService = inject(ProductService);
  private readonly storeService = inject(StoreService);
  private readonly mediaUploadService = inject(MediaUploadService);
  private readonly toast = inject(ToastService);

  readonly isOpen = input<boolean>(false);
  readonly product = input<Product | null>(null);
  readonly categories = input<ProductCategory[]>([]);

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
  readonly isSavingVariant = signal<boolean>(false);

  // Edición Inline de Variante Existente
  readonly editingVariantId = signal<string | null>(null);
  readonly editVariantName = signal<string>('');
  readonly editVariantPrice = signal<number | null>(null);
  readonly editVariantSku = signal<string>('');
  readonly editVariantTracksInventory = signal<boolean>(false);
  readonly isSavingEditVariant = signal<boolean>(false);

  // Estados de Carga
  readonly isSavingGeneral = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Computados
  readonly isGeneralFormValid = computed(
    () => this.name().trim().length >= 2 && !!this.selectedCategoryId(),
  );

  readonly isNewVariantValid = computed(
    () =>
      this.newVariantName().trim().length >= 2 &&
      this.newVariantPrice() !== null &&
      this.newVariantPrice()! > 0,
  );

  constructor() {
    // Sincronizar datos del producto cuando cambie el input product o se abra el drawer
    effect(() => {
      const p = this.product();
      const open = this.isOpen();
      if (open && p) {
        this.initFormData(p);
        this.loadVariants(p);
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
      this.activeTab.set('general');
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
    this.errorMessage.set(null);
    this.isAddingVariant.set(false);
  }

  loadVariants(p: Product): void {
    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId || !p.id) return;

    this.isLoadingVariants.set(true);
    this.productService.getProductVariants(storeId, p.id).subscribe({
      next: (data) => {
        this.variants.set(data || []);
        this.isLoadingVariants.set(false);
        this.productService.syncProductVariants(p.id, data || []);
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

        return this.productService.updateProduct(storeId, p.id, payload);
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

  submitNewVariant(): void {
    const p = this.product();
    if (!p || !this.isNewVariantValid()) return;

    const store = this.storeService.currentStore();
    const storeId = store?.id || p.storeId;
    if (!storeId) return;

    this.isSavingVariant.set(true);
    this.errorMessage.set(null);

    const payload: CreateProductVariantRequest = {
      presentationName: this.newVariantName().trim(),
      priceAmount: Number(this.newVariantPrice()),
      priceCurrency: 'PEN',
      sku: this.newVariantSku().trim() || undefined,
      tracksInventory: this.newVariantTracksInventory(),
    };

    this.productService.createProductVariant(storeId, p.id, payload).subscribe({
      next: (createdVariant) => {
        this.isSavingVariant.set(false);
        const currentList = [...this.variants(), createdVariant];
        this.variants.set(currentList);
        this.productService.syncProductVariants(p.id, currentList);
        this.toast.success(
          `Formato "${createdVariant.presentationName}" agregado a S/ ${createdVariant.priceAmount.toFixed(2)}.`,
          '¡Formato Agregado!',
        );
        this.toggleAddVariant();
      },
      error: (err) => {
        this.isSavingVariant.set(false);
        const msg =
          err?.error?.message ||
          err?.message ||
          'No se pudo agregar la nueva presentación.';
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
}
