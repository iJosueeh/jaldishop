import { Component, inject, input, linkedSignal, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddPhotoAlternateOutline,
  matDeleteOutline,
  matStarOutline,
  matArrowBackOutline,
  matArrowForwardOutline,
  matLinkOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';
import { ProductImage } from '../../../../core/models/product.models';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmModal } from '../../../../shared/components/confirm-modal/confirm-modal';

export interface GalleryImageItem extends ProductImage {
  isUploading?: boolean;
  uploadProgress?: number;
  previewUrl?: string;
  error?: string;
}

@Component({
  selector: 'app-product-gallery-uploader',
  standalone: true,
  imports: [CommonModule, NgIcon, ConfirmModal],
  providers: [
    provideIcons({
      matAddPhotoAlternateOutline,
      matDeleteOutline,
      matStarOutline,
      matArrowBackOutline,
      matArrowForwardOutline,
      matLinkOutline,
      matCheckCircleOutline,
    }),
  ],
  templateUrl: './product-gallery-uploader.html',
  styleUrl: './product-gallery-uploader.css',
})
export class ProductGalleryUploader {
  private readonly mediaUploadService = inject(MediaUploadService);
  private readonly toast = inject(ToastService);

  readonly images = input<ProductImage[]>([]);
  readonly maxImages = input<number>(5);
  readonly disabled = input<boolean>(false);

  readonly imagesChange = output<ProductImage[]>();

  readonly internalImages = linkedSignal<GalleryImageItem[]>(() =>
    (this.images() || []).map((img, idx) => ({
      ...img,
      position: idx,
      isPrimary: img.isPrimary ?? idx === 0,
    }))
  );

  readonly showUrlInput = signal<boolean>(false);
  readonly urlInputValue = signal<string>('');

  readonly showDeleteModal = signal<boolean>(false);
  readonly pendingDeleteIndex = signal<number | null>(null);

  requestDelete(index: number): void {
    this.pendingDeleteIndex.set(index);
    this.showDeleteModal.set(true);
  }

  confirmDelete(): void {
    const index = this.pendingDeleteIndex();
    if (index !== null && index >= 0) {
      this.removeImage(index);
    }
    this.cancelDelete();
  }

  cancelDelete(): void {
    this.showDeleteModal.set(false);
    this.pendingDeleteIndex.set(null);
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files);
    const availableSlots = this.maxImages() - this.internalImages().length;

    if (availableSlots <= 0) {
      this.toast.error(`Has alcanzado el límite máximo de ${this.maxImages()} imágenes.`);
      return;
    }

    const filesToUpload = files.slice(0, availableSlots);
    if (files.length > availableSlots) {
      this.toast.info(`Solo se cargarán ${availableSlots} imagen(es) para respetar el límite de ${this.maxImages()}.`);
    }

    filesToUpload.forEach((file) => this.uploadSingleFile(file));
    input.value = '';
  }

  private uploadSingleFile(file: File): void {
    const validation = this.mediaUploadService.validateImageFile(file);
    if (!validation.valid) {
      this.toast.error(validation.error ?? 'Archivo no válido.');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const previewUrl = e.target?.result as string;
      const newItem = this.createPreviewItem(previewUrl);
      this.internalImages.update((list) => [...list, newItem]);
      this.executeUpload(file, previewUrl);
    };
    reader.readAsDataURL(file);
  }

  private createPreviewItem(previewUrl: string): GalleryImageItem {
    const currentList = this.internalImages();
    return {
      imageUrl: previewUrl,
      previewUrl,
      position: currentList.length,
      isPrimary: currentList.length === 0,
      isUploading: true,
      uploadProgress: 10,
    };
  }

  private executeUpload(file: File, previewUrl: string): void {
    this.mediaUploadService.uploadImage(file, 'PRODUCT_IMAGE').subscribe({
      next: (progress) => this.handleUploadProgress(previewUrl, progress),
      error: (err) => this.handleUploadFailure(previewUrl, err?.message),
    });
  }

  private handleUploadProgress(previewUrl: string, progress: any): void {
    this.internalImages.update((list) =>
      list.map((item) => {
        if (item.previewUrl !== previewUrl || !item.isUploading) return item;

        if (progress.state === 'COMPLETED' && progress.result?.secure_url) {
          return { ...item, imageUrl: progress.result.secure_url, isUploading: false, uploadProgress: 100 };
        }
        if (progress.state === 'ERROR') {
          return { ...item, isUploading: false, error: progress.error };
        }
        return { ...item, uploadProgress: progress.progress };
      })
    );

    if (progress.state === 'COMPLETED') {
      this.emitChanges();
      this.toast.success('Imagen de producto subida exitosamente.');
    } else if (progress.state === 'ERROR') {
      this.toast.error(progress.error ?? 'Error al subir la imagen.');
    }
  }

  private handleUploadFailure(previewUrl: string, errorMessage?: string): void {
    this.internalImages.update((list) =>
      list.map((item) =>
        item.previewUrl === previewUrl ? { ...item, isUploading: false, error: errorMessage } : item
      )
    );
    this.toast.error('No se pudo subir la imagen.');
  }

  addFromUrl(): void {
    const url = this.urlInputValue().trim();
    if (!url) return;

    if (this.internalImages().length >= this.maxImages()) {
      this.toast.error(`Límite máximo de ${this.maxImages()} imágenes alcanzado.`);
      return;
    }

    const currentList = this.internalImages();
    const newItem: GalleryImageItem = {
      imageUrl: url,
      position: currentList.length,
      isPrimary: currentList.length === 0,
      isUploading: false,
      uploadProgress: 100,
    };

    this.internalImages.update((list) => [...list, newItem]);
    this.urlInputValue.set('');
    this.showUrlInput.set(false);
    this.emitChanges();
  }

  setPrimary(index: number): void {
    this.internalImages.update((list) =>
      list.map((item, idx) => ({
        ...item,
        isPrimary: idx === index,
      }))
    );
    this.emitChanges();
    this.toast.info('Imagen principal de portada actualizada.');
  }

  movePosition(index: number, direction: 'left' | 'right'): void {
    const list = [...this.internalImages()];
    const targetIndex = direction === 'left' ? index - 1 : index + 1;

    if (targetIndex < 0 || targetIndex >= list.length) return;

    const [movedItem] = list.splice(index, 1);
    list.splice(targetIndex, 0, movedItem);

    this.internalImages.set(this.reindexList(list));
    this.emitChanges();
  }

  removeImage(index: number): void {
    const list = [...this.internalImages()];
    const [removed] = list.splice(index, 1);

    const updated = this.reindexList(list, removed.isPrimary);
    this.internalImages.set(updated);
    this.emitChanges();
    this.toast.info('Imagen eliminada de la galería.');
  }

  private reindexList(list: GalleryImageItem[], assignNewPrimary = false): GalleryImageItem[] {
    return list.map((item, idx) => ({
      ...item,
      position: idx,
      isPrimary: assignNewPrimary && idx === 0 ? true : item.isPrimary,
    }));
  }

  private emitChanges(): void {
    const cleanImages: ProductImage[] = this.internalImages()
      .filter((img) => !img.isUploading && !img.error && img.imageUrl)
      .map((img, idx) => ({
        imageUrl: img.imageUrl,
        position: idx,
        isPrimary: img.isPrimary,
      }));

    this.imagesChange.emit(cleanImages);
  }
}

