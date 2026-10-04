import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { outputFromObservable, toSignal } from '@angular/core/rxjs-interop';
import { RegisterFooter } from '../../../../../shared/components/register-footer/register-footer';
import { RegisterStepHeader } from '../../../../../shared/components/register-step-header/register-step-header';
import { AlertError } from '../../../../../shared/components/alert-error/alert-error';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';
import { RegisterStep2Data } from '../../../../../core/models/register.models';
import { StoreService } from '../../../../../core/services/store.service';
import { MediaUploadService } from '../../../../../core/services/media-upload.service';
import { StoreCategory } from '../../../../../core/models/store.models';
import { isValidImageUrl } from '../../../../../core/utils/image.utils';

const DEFAULT_CATEGORIES: StoreCategory[] = [
  { id: 'cat-restaurantes', name: 'Restaurantes y Cafeterías', slug: 'restaurantes-cafeterias', status: 'ACTIVE' },
  { id: 'cat-moda', name: 'Moda y Calzado', slug: 'moda-calzado', status: 'ACTIVE' },
  { id: 'cat-supermercado', name: 'Supermercado y Bodega', slug: 'supermercado-bodega', status: 'ACTIVE' },
  { id: 'cat-tecnologia', name: 'Tecnología y Electrónica', slug: 'tecnologia-electronica', status: 'ACTIVE' },
  { id: 'cat-salud', name: 'Salud y Belleza', slug: 'salud-belleza', status: 'ACTIVE' },
  { id: 'cat-hogar', name: 'Hogar y Decoración', slug: 'hogar-decoracion', status: 'ACTIVE' },
  { id: 'cat-mascotas', name: 'Mascotas', slug: 'mascotas', status: 'ACTIVE' },
  { id: 'cat-servicios', name: 'Servicios y Otros', slug: 'servicios-otros', status: 'ACTIVE' },
];

@Component({
  imports: [
    ReactiveFormsModule,
    RegisterFooter,
    RegisterStepHeader,
    AlertError,
    BusinessAvatar,
  ],
  selector: 'app-register-step2-form',
  styleUrl: './register-step2-form.css',
  templateUrl: './register-step2-form.html',
})
export class RegisterStep2Form implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly storeService = inject(StoreService);
  private readonly mediaUploadService = inject(MediaUploadService);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);
  readonly initialData = input<RegisterStep2Data | null>(null);
  readonly isExistingUser = input<boolean>(false);

  readonly back = output<void>();
  readonly step2Submit = output<RegisterStep2Data>();
  readonly skip = output<void>();

  readonly categories = signal<StoreCategory[]>(DEFAULT_CATEGORIES);
  readonly isLoadingCategories = signal<boolean>(false);

  readonly isCategoryDropdownOpen = signal<boolean>(false);
  readonly categorySearchQuery = signal<string>('');

  readonly filteredCategories = computed(() => {
    const query = this.categorySearchQuery().toLowerCase().trim();
    const all = this.categories();
    if (!query) return all;
    return all.filter(
      (c) =>
        c.name.toLowerCase().includes(query) ||
        (c.slug && c.slug.toLowerCase().includes(query)),
    );
  });

  readonly selectedCategory = computed(() => {
    const currentName = this.formValues().businessType;
    const currentIds: string[] = this.formValues().categoryIds || [];
    return (
      this.categories().find(
        (c) => currentIds.includes(c.id) || c.name === currentName,
      ) || null
    );
  });

  readonly isUploadingLogo = signal<boolean>(false);
  readonly logoProgress = signal<number>(0);

  readonly isUploadingBanner = signal<boolean>(false);
  readonly bannerProgress = signal<number>(0);

  readonly lastFailedLogoUrl = signal<string | null>(null);
  readonly lastFailedBannerUrl = signal<string | null>(null);

  readonly uploadError = signal<string | null>(null);

  readonly step2Form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    businessType: ['Restaurantes y Cafeterías', [Validators.required]],
    categoryIds: [[] as string[]],
    contactPhone: ['', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
    pickupEnabled: [true],
    deliveryEnabled: [true],
    address: [''],
    logoUrl: [''],
    bannerUrl: [''],
  });

  ngOnInit(): void {
    this.loadCategories();
    const data = this.initialData();
    if (data) {
      this.step2Form.patchValue(data);
    }
  }

  loadCategories(): void {
    this.isLoadingCategories.set(true);
    this.storeService.getStoreCategories().subscribe({
      next: (cats) => {
        if (cats && cats.length > 0) {
          this.categories.set(cats);
        }
        this.isLoadingCategories.set(false);
      },
      error: () => {
        this.isLoadingCategories.set(false);
      },
    });
  }

  readonly formChange = outputFromObservable<RegisterStep2Data>(this.step2Form.valueChanges);

  readonly formValues = toSignal(this.step2Form.valueChanges, {
    initialValue: this.step2Form.getRawValue(),
  });

  toggleCategoryDropdown(): void {
    const nextState = !this.isCategoryDropdownOpen();
    this.isCategoryDropdownOpen.set(nextState);
    if (nextState) {
      this.categorySearchQuery.set('');
    }
  }

  closeCategoryDropdown(): void {
    this.isCategoryDropdownOpen.set(false);
    this.categorySearchQuery.set('');
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.categorySearchQuery.set(input.value);
  }

  onCategorySelect(category: StoreCategory): void {
    this.step2Form.patchValue({
      businessType: category.name,
      categoryIds: [category.id],
    });
    this.step2Form.markAsDirty();
    this.closeCategoryDropdown();
  }

  isSelectedCategory(category: StoreCategory): boolean {
    const currentName = this.step2Form.get('businessType')?.value;
    const currentIds: string[] = this.step2Form.get('categoryIds')?.value || [];
    return currentName === category.name || currentIds.includes(category.id);
  }

  hasValidLogo(): boolean {
    const url = this.step2Form.get('logoUrl')?.value;
    return isValidImageUrl(url) && this.lastFailedLogoUrl() !== url;
  }

  hasValidBanner(): boolean {
    const url = this.step2Form.get('bannerUrl')?.value;
    return isValidImageUrl(url) && this.lastFailedBannerUrl() !== url;
  }

  onLogoError(): void {
    this.lastFailedLogoUrl.set(this.step2Form.get('logoUrl')?.value ?? null);
  }

  onBannerError(): void {
    this.lastFailedBannerUrl.set(this.step2Form.get('bannerUrl')?.value ?? null);
  }

  onLogoFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    const validation = this.mediaUploadService.validateImageFile(file);
    if (!validation.valid) {
      this.uploadError.set(validation.error || 'Archivo inválido');
      return;
    }

    this.lastFailedLogoUrl.set(null);

    // 1. Vista previa local inmediata en la tarjeta de marca (0ms de latencia)
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.step2Form.patchValue({ logoUrl: reader.result });
        this.step2Form.markAsDirty();
      }
    };
    reader.readAsDataURL(file);

    // 2. Subida asíncrona a Cloudinary / Backend
    this.isUploadingLogo.set(true);
    this.logoProgress.set(10);
    this.uploadError.set(null);

    this.mediaUploadService.uploadImage(file, 'STORE_LOGO').subscribe({
      next: (progress) => {
        if (progress.state === 'UPLOADING') {
          this.logoProgress.set(progress.progress);
        } else if (progress.state === 'COMPLETED' && progress.result) {
          // Reemplaza la preview local con la URL permanente en Cloudinary
          this.step2Form.patchValue({ logoUrl: progress.result.secure_url });
          this.step2Form.markAsDirty();
          this.isUploadingLogo.set(false);
          this.logoProgress.set(0);
        } else if (progress.state === 'ERROR') {
          this.uploadError.set(progress.error || 'Error al subir el logo');
          this.isUploadingLogo.set(false);
          this.logoProgress.set(0);
        }
      },
      error: () => {
        this.uploadError.set('No se pudo conectar con el servicio de subida.');
        this.isUploadingLogo.set(false);
        this.logoProgress.set(0);
      },
    });
  }

  onBannerFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    const validation = this.mediaUploadService.validateImageFile(file, 8 * 1024 * 1024);
    if (!validation.valid) {
      this.uploadError.set(validation.error || 'Archivo inválido');
      return;
    }

    this.lastFailedBannerUrl.set(null);

    // 1. Vista previa local inmediata en la tarjeta de marca (0ms de latencia)
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.step2Form.patchValue({ bannerUrl: reader.result });
        this.step2Form.markAsDirty();
      }
    };
    reader.readAsDataURL(file);

    // 2. Subida asíncrona a Cloudinary / Backend
    this.isUploadingBanner.set(true);
    this.bannerProgress.set(10);
    this.uploadError.set(null);

    this.mediaUploadService.uploadImage(file, 'STORE_BANNER').subscribe({
      next: (progress) => {
        if (progress.state === 'UPLOADING') {
          this.bannerProgress.set(progress.progress);
        } else if (progress.state === 'COMPLETED' && progress.result) {
          // Reemplaza la preview local con la URL permanente en Cloudinary
          this.step2Form.patchValue({ bannerUrl: progress.result.secure_url });
          this.step2Form.markAsDirty();
          this.isUploadingBanner.set(false);
          this.bannerProgress.set(0);
        } else if (progress.state === 'ERROR') {
          this.uploadError.set(progress.error || 'Error al subir la portada');
          this.isUploadingBanner.set(false);
          this.bannerProgress.set(0);
        }
      },
      error: () => {
        this.uploadError.set('No se pudo conectar con el servicio de subida.');
        this.isUploadingBanner.set(false);
        this.bannerProgress.set(0);
      },
    });
  }

  removeLogo(): void {
    this.lastFailedLogoUrl.set(null);
    this.step2Form.patchValue({ logoUrl: '' });
    this.step2Form.markAsDirty();
  }

  removeBanner(): void {
    this.lastFailedBannerUrl.set(null);
    this.step2Form.patchValue({ bannerUrl: '' });
    this.step2Form.markAsDirty();
  }

  onBack(): void {
    this.back.emit();
  }

  handleContinue(): void {
    if (this.step2Form.invalid || this.isLoading()) {
      this.step2Form.markAllAsTouched();
      return;
    }
    this.step2Submit.emit(this.step2Form.getRawValue());
  }

  handleSkip(): void {
    this.skip.emit();
  }
}

export const atLeastOneDeliveryMethodValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const pickup = control.get('pickupEnabled')?.value;
  const delivery = control.get('deliveryEnabled')?.value;
  return !pickup && !delivery ? { noDeliveryMethod: true } : null;
};
