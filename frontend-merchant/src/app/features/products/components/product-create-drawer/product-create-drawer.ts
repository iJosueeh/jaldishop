import { Component, computed, HostListener, inject, input, output, signal } from '@angular/core';
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
  matCategoryOutline,
  matKeyboardArrowDownOutline,
  matKeyboardArrowUpOutline,
  matRefreshOutline,
  matLayersOutline,
  matDeleteOutline,
} from '@ng-icons/material-symbols/outline';
import {
  ProductCategory,
  Product,
  ProductImage,
  CreateProductWithVariantPayload,
  CreateProductVariantItem,
} from '../../../../core/models/product.models';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { of, switchMap } from 'rxjs';
import { ProductGalleryUploader } from '../product-gallery-uploader/product-gallery-uploader';

export function generateProductSlug(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function generateProductSku(text: string): string {
  if (!text || text.trim().length === 0) return '';
  const clean = text
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Z0-9\s]/g, '')
    .trim();
  const words = clean
    .split(/\s+/)
    .filter((w) => !['DE', 'DEL', 'LA', 'EL', 'LOS', 'LAS', 'Y', 'CON', 'EN', 'POR', 'PARA'].includes(w));
  if (words.length === 0) return 'PRD-01';
  if (words.length === 1) {
    const w = words[0];
    return `${w.substring(0, Math.min(6, w.length))}-01`;
  }
  const parts = words.slice(0, 3).map((w) => w.substring(0, Math.min(4, w.length)));
  return `${parts.join('-')}-01`;
}

export function generateVariantSku(productName: string, variantName: string, index: number): string {
  const base = generateProductSku(productName).replace(/-\d+$/, '');
  const cleanVar = variantName
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Z0-9]/g, '')
    .substring(0, 4);
  const suffix = cleanVar || String(index + 1).padStart(2, '0');
  return `${base || 'PRD'}-${suffix}`;
}

export interface FormVariantItem {
  id: string;
  presentationName: string;
  priceAmount: number | null;
  sku: string;
  isCustomSku: boolean;
  tracksInventory: boolean;
}

const createInitialVariants = (): FormVariantItem[] => [
  {
    id: 'v-1',
    presentationName: 'Chico / Personal',
    priceAmount: null,
    sku: '',
    isCustomSku: false,
    tracksInventory: false,
  },
  {
    id: 'v-2',
    presentationName: 'Grande / Familiar',
    priceAmount: null,
    sku: '',
    isCustomSku: false,
    tracksInventory: false,
  },
];

@Component({
  selector: 'app-product-create-drawer',
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
      matCategoryOutline,
      matKeyboardArrowDownOutline,
      matKeyboardArrowUpOutline,
      matRefreshOutline,
      matLayersOutline,
      matDeleteOutline,
    }),
  ],
  templateUrl: './product-create-drawer.html',
  styleUrl: './product-create-drawer.css',
})
export class ProductCreateDrawer {
  private readonly productService = inject(ProductService);
  private readonly storeService = inject(StoreService);
  private readonly mediaUploadService = inject(MediaUploadService);
  private readonly toast = inject(ToastService);

  readonly isOpen = input<boolean>(false);
  readonly categories = input<ProductCategory[]>([]);

  readonly closeDrawer = output<void>();
  readonly productCreated = output<Product>();

  readonly isClosing = signal<boolean>(false);

  // Form State Principal
  readonly name = signal<string>('');
  readonly description = signal<string>('');

  // Category
  readonly selectedCategoryId = signal<string>('');
  readonly isNewCategory = signal<boolean>(false);
  readonly newCategoryName = signal<string>('');

  // Pricing & Operational Mode
  readonly pricingMode = signal<'single' | 'multiple'>('single');
  readonly priceAmount = signal<number | null>(null);
  readonly presentationName = signal<string>('Unidad Estándar');
  readonly tracksInventory = signal<boolean>(false);
  readonly formVariants = signal<FormVariantItem[]>(createInitialVariants());

  // Opciones Avanzadas (Auto-generadas en background con opción de personalización)
  readonly showAdvancedOptions = signal<boolean>(false);
  readonly slug = signal<string>('');
  readonly isCustomSlug = signal<boolean>(false);
  readonly sku = signal<string>('');
  readonly isCustomSku = signal<boolean>(false);

  // Images
  readonly images = signal<ProductImage[]>([]);
  readonly imageUrl = signal<string>('');

  // Status
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Computed Validations
  readonly isNameValid = computed(() => this.name().trim().length >= 2);
  readonly isPriceValid = computed(() => {
    if (this.pricingMode() === 'single') {
      return this.priceAmount() !== null && this.priceAmount()! > 0;
    }
    const list = this.formVariants();
    return (
      list.length >= 2 &&
      list.every(
        (v) =>
          v.presentationName.trim().length >= 1 &&
          v.priceAmount !== null &&
          v.priceAmount > 0,
      )
    );
  });
  readonly isCategoryValid = computed(() => {
    if (this.isNewCategory() || this.categories().length === 0) {
      return this.newCategoryName().trim().length >= 2;
    }
    return !!this.selectedCategoryId();
  });

  readonly canSubmit = computed(
    () => this.isNameValid() && this.isPriceValid() && this.isCategoryValid() && !this.isLoading(),
  );

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event?: Event): void {
    if (this.isOpen() && !this.isClosing()) {
      event?.preventDefault();
      this.handleClose();
    }
  }

  handleClose(): void {
    if (this.isLoading()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.resetForm();
      this.closeDrawer.emit();
    }, 220);
  }

  onNameChange(value: string): void {
    this.name.set(value);
    if (!this.isCustomSlug()) {
      this.slug.set(generateProductSlug(value));
    }
    if (!this.isCustomSku()) {
      this.sku.set(generateProductSku(value));
    }
    this.formVariants.update((variants) =>
      variants.map((v, i) =>
        v.isCustomSku ? v : { ...v, sku: generateVariantSku(value, v.presentationName, i) },
      ),
    );
  }

  setPricingMode(mode: 'single' | 'multiple'): void {
    this.pricingMode.set(mode);
    if (mode === 'multiple' && this.formVariants().some((v) => !v.sku)) {
      this.formVariants.update((variants) =>
        variants.map((v, i) =>
          v.isCustomSku ? v : { ...v, sku: generateVariantSku(this.name(), v.presentationName, i) },
        ),
      );
    }
  }

  addVariantOption(): void {
    const nextIdx = this.formVariants().length;
    const newItem: FormVariantItem = {
      id: `v-${Date.now()}`,
      presentationName: '',
      priceAmount: null,
      sku: generateVariantSku(this.name(), '', nextIdx),
      isCustomSku: false,
      tracksInventory: false,
    };
    this.formVariants.update((list) => [...list, newItem]);
  }

  removeVariantOption(index: number): void {
    if (this.formVariants().length <= 1) return;
    this.formVariants.update((list) => list.filter((_, i) => i !== index));
  }

  updateVariantName(index: number, name: string): void {
    this.formVariants.update((list) =>
      list.map((v, i) => {
        if (i !== index) return v;
        const sku = v.isCustomSku ? v.sku : generateVariantSku(this.name(), name, index);
        return { ...v, presentationName: name, sku };
      }),
    );
  }

  updateVariantPrice(index: number, price: number | null): void {
    this.formVariants.update((list) =>
      list.map((v, i) => (i === index ? { ...v, priceAmount: price } : v)),
    );
  }

  updateVariantSku(index: number, sku: string): void {
    this.formVariants.update((list) =>
      list.map((v, i) =>
        i === index ? { ...v, sku: sku.toUpperCase().trim(), isCustomSku: true } : v,
      ),
    );
  }

  updateVariantTracksInventory(index: number, tracks: boolean): void {
    this.formVariants.update((list) =>
      list.map((v, i) => (i === index ? { ...v, tracksInventory: tracks } : v)),
    );
  }

  toggleAdvancedOptions(): void {
    this.showAdvancedOptions.update((v) => !v);
  }

  onSlugChange(value: string): void {
    this.isCustomSlug.set(true);
    this.slug.set(generateProductSlug(value));
  }

  resetSlugToDefault(): void {
    this.isCustomSlug.set(false);
    this.slug.set(generateProductSlug(this.name()));
  }

  onSkuChange(value: string): void {
    this.isCustomSku.set(true);
    this.sku.set(value.toUpperCase().trim());
  }

  resetSkuToDefault(): void {
    this.isCustomSku.set(false);
    this.sku.set(generateProductSku(this.name()));
  }

  clearSku(): void {
    this.isCustomSku.set(true);
    this.sku.set('');
  }

  toggleCategoryMode(isNew: boolean): void {
    this.isNewCategory.set(isNew);
    if (!isNew && this.categories().length > 0 && !this.selectedCategoryId()) {
      this.selectedCategoryId.set(this.categories()[0].id);
    }
  }

  onImagesChange(newImages: ProductImage[]): void {
    this.images.set(newImages);
    const primary = newImages.find((img) => img.isPrimary) || newImages[0];
    this.imageUrl.set(primary ? primary.imageUrl : '');
  }

  onSubmit(): void {
    if (!this.canSubmit()) {
      return;
    }

    const store = this.storeService.currentStore();
    if (!store?.id) {
      this.toast.error('No se pudo identificar la tienda del comerciante.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const pendingImages = this.images();
    const hasFilesToUpload = pendingImages.some((img) => !!img.file);

    const uploadStep$ = hasFilesToUpload
      ? this.mediaUploadService.uploadPendingImages(pendingImages, 'PRODUCT_IMAGE', store.id)
      : of(pendingImages);

    uploadStep$.pipe(
      switchMap((finalImages) => {
        const primary = finalImages.find((img) => img.isPrimary) || finalImages[0];
        const primaryUrl = primary ? primary.imageUrl : undefined;

        const isCreatingNew = this.isNewCategory() || this.categories().length === 0;

        const payload: CreateProductWithVariantPayload = {
          categoryId: isCreatingNew ? undefined : this.selectedCategoryId(),
          newCategoryName: isCreatingNew ? this.newCategoryName().trim() : undefined,
          name: this.name().trim(),
          slug: this.slug().trim() || undefined,
          description: this.description().trim() || undefined,
          imageUrl: primaryUrl,
          images: finalImages,
          variants:
            this.pricingMode() === 'multiple'
              ? this.formVariants().map((v) => ({
                  presentationName: v.presentationName.trim(),
                  sku: v.sku.trim() || undefined,
                  priceAmount: Number(v.priceAmount),
                  priceCurrency: 'PEN',
                  tracksInventory: v.tracksInventory,
                }))
              : undefined,
          presentationName:
            this.pricingMode() === 'multiple'
              ? this.formVariants()[0].presentationName.trim()
              : this.presentationName().trim() || 'Unidad Estándar',
          sku:
            this.pricingMode() === 'multiple'
              ? this.formVariants()[0].sku.trim() || undefined
              : this.sku().trim() || undefined,
          priceAmount:
            this.pricingMode() === 'multiple'
              ? Number(this.formVariants()[0].priceAmount)
              : Number(this.priceAmount()),
          priceCurrency: 'PEN',
          tracksInventory:
            this.pricingMode() === 'multiple'
              ? this.formVariants()[0].tracksInventory
              : this.tracksInventory(),
        };

        return this.productService.createProductWithDefaultVariant(store.id, payload);
      }),
    ).subscribe({
      next: (createdProduct) => {
        this.isLoading.set(false);
        this.toast.success(
          `"${createdProduct.name}" se publicó exitosamente en tu catálogo.`,
          '¡Producto Creado!',
        );
        this.productCreated.emit(createdProduct);
        this.handleClose();
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg =
          err?.error?.message ||
          err?.message ||
          'Ocurrió un error al registrar el producto. Inténtalo nuevamente.';
        this.errorMessage.set(msg);
      },
    });
  }

  private resetForm(): void {
    this.name.set('');
    this.slug.set('');
    this.isCustomSlug.set(false);
    this.description.set('');
    this.selectedCategoryId.set(this.categories().length > 0 ? this.categories()[0].id : '');
    this.isNewCategory.set(this.categories().length === 0);
    this.newCategoryName.set('');
    this.pricingMode.set('single');
    this.priceAmount.set(null);
    this.presentationName.set('Unidad Estándar');
    this.formVariants.set(createInitialVariants());
    this.sku.set('');
    this.isCustomSku.set(false);
    this.tracksInventory.set(false);
    this.showAdvancedOptions.set(false);
    this.images.set([]);
    this.imageUrl.set('');
    this.errorMessage.set(null);
    this.isLoading.set(false);
  }
}
