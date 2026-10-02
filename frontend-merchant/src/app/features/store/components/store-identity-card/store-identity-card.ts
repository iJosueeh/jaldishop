import { Component, inject, input, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matBadgeOutline,
  matStorefrontOutline,
  matLockOutline,
  matChatOutline,
  matImageOutline,
  matLinkOutline,
  matShareOutline,
  matPhotoCameraOutline,
  matDeleteOutline,
  matUploadOutline,
  matCategoryOutline,
  matCheckOutline,
} from '@ng-icons/material-symbols/outline';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { ToastService } from '../../../../core/services/toast.service';
import { StoreCategory } from '../../../../core/models/store.models';

import { ConfirmModal } from '../../../../shared/components/confirm-modal/confirm-modal';

@Component({
  imports: [ReactiveFormsModule, NgIcon, ConfirmModal],
  providers: [
    provideIcons({
      matBadgeOutline,
      matStorefrontOutline,
      matLockOutline,
      matChatOutline,
      matImageOutline,
      matLinkOutline,
      matShareOutline,
      matPhotoCameraOutline,
      matDeleteOutline,
      matUploadOutline,
      matCategoryOutline,
      matCheckOutline,
    }),
  ],
  selector: 'app-store-identity-card',
  styleUrl: './store-identity-card.css',
  templateUrl: './store-identity-card.html',
})
export class StoreIdentityCard {
  private readonly mediaUploadService = inject(MediaUploadService);
  private readonly toastService = inject(ToastService);

  readonly form = input<FormGroup>(new FormGroup({}));
  readonly slug = input<string>('');
  readonly categories = input<StoreCategory[]>([]);
  readonly isLoadingCategories = input<boolean>(false);

  readonly logoMode = signal<'upload' | 'url'>('upload');
  readonly bannerMode = signal<'upload' | 'url'>('upload');

  readonly isUploadingLogo = signal<boolean>(false);
  readonly logoProgress = signal<number>(0);

  readonly isUploadingBanner = signal<boolean>(false);
  readonly bannerProgress = signal<number>(0);

  readonly showDeleteModal = signal<boolean>(false);
  readonly pendingDeleteControl = signal<'logoUrl' | 'bannerUrl' | null>(null);

  isSelected(categoryId: string): boolean {
    const selected: string[] = this.form().get('categoryIds')?.value || [];
    return selected.includes(categoryId);
  }

  toggleCategory(categoryId: string): void {
    const control = this.form().get('categoryIds');
    if (!control) return;
    const current: string[] = control.value ? [...control.value] : [];
    const index = current.indexOf(categoryId);
    if (index > -1) {
      current.splice(index, 1);
    } else {
      current.push(categoryId);
    }
    control.setValue(current);
    control.markAsDirty();
  }

  get deleteModalTitle(): string {
    return this.pendingDeleteControl() === 'logoUrl' ? '¿Quitar Logo de la Tienda?' : '¿Quitar Portada de la Tienda?';
  }

  get deleteModalMessage(): string {
    return this.pendingDeleteControl() === 'logoUrl'
      ? 'El logo actual será retirado de tu perfil de tienda. Puedes subir uno nuevo o guardar los cambios.'
      : 'La portada actual será retirada de tu tienda. Puedes subir una nueva o guardar los cambios.';
  }

  setLogoMode(mode: 'upload' | 'url'): void {
    this.logoMode.set(mode);
  }

  setBannerMode(mode: 'upload' | 'url'): void {
    this.bannerMode.set(mode);
  }

  onFileSelected(event: Event, controlName: 'logoUrl' | 'bannerUrl'): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0]) return;

    const file = input.files[0];
    const validation = this.mediaUploadService.validateImageFile(file);
    if (!validation.valid) {
      this.toastService.error(validation.error ?? 'Archivo no válido.');
      return;
    }

    this.showLocalPreview(file, controlName);
    this.startImageUpload(file, controlName);
  }

  private showLocalPreview(file: File, controlName: 'logoUrl' | 'bannerUrl'): void {
    const reader = new FileReader();
    reader.onload = (e) => {
      const previewUrl = e.target?.result as string;
      this.form().get(controlName)?.setValue(previewUrl);
      this.form().get(controlName)?.markAsDirty();
    };
    reader.readAsDataURL(file);
  }

  private startImageUpload(file: File, controlName: 'logoUrl' | 'bannerUrl'): void {
    const isLogo = controlName === 'logoUrl';
    const targetType = isLogo ? 'STORE_LOGO' : 'STORE_BANNER';

    this.setUploadingState(controlName, true, 10);

    this.mediaUploadService.uploadImage(file, targetType).subscribe({
      next: (progress) => {
        this.updateUploadProgress(controlName, progress.progress);

        if (progress.state === 'COMPLETED' && progress.result?.secure_url) {
          this.handleUploadSuccess(controlName, progress.result.secure_url);
        } else if (progress.state === 'ERROR') {
          this.handleUploadError(controlName, progress.error);
        }
      },
      error: (err) => this.handleUploadError(controlName, err?.message),
    });
  }

  private handleUploadSuccess(controlName: 'logoUrl' | 'bannerUrl', secureUrl: string): void {
    this.form().get(controlName)?.setValue(secureUrl);
    this.form().get(controlName)?.markAsDirty();
    this.setUploadingState(controlName, false, 100);

    const message = controlName === 'logoUrl' ? 'Logo de tienda cargado con éxito.' : 'Banner de portada cargado con éxito.';
    this.toastService.success(message);
  }

  private handleUploadError(controlName: 'logoUrl' | 'bannerUrl', errorMessage?: string): void {
    this.setUploadingState(controlName, false, 0);
    this.toastService.error(errorMessage ?? 'No se pudo completar la subida de la imagen.');
  }

  private setUploadingState(controlName: 'logoUrl' | 'bannerUrl', isUploading: boolean, progress: number): void {
    if (controlName === 'logoUrl') {
      this.isUploadingLogo.set(isUploading);
      this.logoProgress.set(progress);
    } else {
      this.isUploadingBanner.set(isUploading);
      this.bannerProgress.set(progress);
    }
  }

  private updateUploadProgress(controlName: 'logoUrl' | 'bannerUrl', progress: number): void {
    if (controlName === 'logoUrl') {
      this.logoProgress.set(progress);
    } else {
      this.bannerProgress.set(progress);
    }
  }

  requestDelete(controlName: 'logoUrl' | 'bannerUrl'): void {
    this.pendingDeleteControl.set(controlName);
    this.showDeleteModal.set(true);
  }

  confirmDelete(): void {
    const controlName = this.pendingDeleteControl();
    if (controlName) {
      this.clearImage(controlName);
    }
    this.cancelDelete();
  }

  cancelDelete(): void {
    this.showDeleteModal.set(false);
    this.pendingDeleteControl.set(null);
  }

  clearImage(controlName: 'logoUrl' | 'bannerUrl'): void {
    this.form().get(controlName)?.setValue('');
    this.form().get(controlName)?.markAsDirty();
  }
}
