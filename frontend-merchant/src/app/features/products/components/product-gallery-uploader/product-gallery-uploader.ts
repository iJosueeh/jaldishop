import { Component, inject, input, linkedSignal, OnDestroy, output, signal } from '@angular/core';
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
export class ProductGalleryUploader implements OnDestroy {
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

  ngOnDestroy(): void {
    // Revocar cualquier URL local temporal para evitar fugas de memoria
    for (const img of this.internalImages()) {
      if (img.previewUrl && img.previewUrl.startsWith('blob:')) {
        try {
          URL.revokeObjectURL(img.previewUrl);
        } catch {}
      }
    }
  }

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

    const filesToProcess = files.slice(0, availableSlots);
    if (files.length > availableSlots) {
      this.toast.info(`Solo se añadirán ${availableSlots} imagen(es) para respetar el límite de ${this.maxImages()}.`);
    }

    for (const file of filesToProcess) {
      this.addLocalFilePreview(file);
    }
    input.value = '';
    this.emitChanges();
  }

  private addLocalFilePreview(file: File): void {
    const validation = this.mediaUploadService.validateImageFile(file);
    if (!validation.valid) {
      this.toast.error(validation.error ?? 'Archivo no válido.');
      return;
    }

    // Previsualización instantánea en memoria local sin costo ni subida a red
    let previewUrl: string;
    try {
      previewUrl = URL.createObjectURL(file);
    } catch {
      const reader = new FileReader();
      reader.onload = (e) => {
        const dataUrl = e.target?.result as string;
        this.insertPreviewItem(file, dataUrl);
      };
      reader.readAsDataURL(file);
      return;
    }

    this.insertPreviewItem(file, previewUrl);
  }

  private insertPreviewItem(file: File, previewUrl: string): void {
    const currentList = this.internalImages();
    const newItem: GalleryImageItem = {
      imageUrl: previewUrl,
      previewUrl,
      file,
      position: currentList.length,
      isPrimary: currentList.length === 0,
      isUploading: false,
      uploadProgress: 100,
    };
    this.internalImages.update((list) => [...list, newItem]);
    this.emitChanges();
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

    if (removed.previewUrl && removed.previewUrl.startsWith('blob:')) {
      try {
        URL.revokeObjectURL(removed.previewUrl);
      } catch {}
    }

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
      .filter((img) => !img.error && (img.imageUrl || img.previewUrl))
      .map((img, idx) => ({
        imageUrl: img.imageUrl,
        position: idx,
        isPrimary: img.isPrimary,
        file: img.file,
      }));

    this.imagesChange.emit(cleanImages);
  }
}

