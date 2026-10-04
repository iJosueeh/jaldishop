import { inject, Service } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { forkJoin, map, Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CloudinaryUploadResult,
  MediaTargetType,
  MediaUploadProgress,
  UploadSignatureRequest,
  UploadSignatureResponse,
} from '../models/media.models';
import { ProductImage } from '../models/product.models';

@Service()
export class MediaUploadService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/media`;

  getUploadSignature(
    targetType: MediaTargetType,
    storeId?: string,
  ): Observable<UploadSignatureResponse> {
    const payload: UploadSignatureRequest = { targetType, storeId };
    return this.http.post<UploadSignatureResponse>(`${this.baseUrl}/upload-signature`, payload);
  }

  validateImageFile(
    file: File,
    maxSizeBytes = 5 * 1024 * 1024,
    allowedTypes = ['image/jpeg', 'image/png', 'image/webp'],
  ): { valid: boolean; error?: string } {
    if (!file) {
      return { valid: false, error: 'No se seleccionó ningún archivo.' };
    }

    if (!allowedTypes.includes(file.type.toLowerCase())) {
      return {
        valid: false,
        error: 'Formato no soportado. Solo se admiten imágenes JPG, PNG o WebP.',
      };
    }

    if (file.size > maxSizeBytes) {
      const maxMb = Math.round(maxSizeBytes / (1024 * 1024));
      return {
        valid: false,
        error: `El archivo supera el tamaño máximo permitido (${maxMb} MB).`,
      };
    }

    return { valid: true };
  }

  uploadImage(
    file: File,
    targetType: MediaTargetType,
    storeId?: string,
  ): Observable<MediaUploadProgress> {
    const validation = this.validateImageFile(file);
    if (!validation.valid) {
      return of({
        state: 'ERROR',
        progress: 0,
        error: validation.error,
      });
    }

    return new Observable<MediaUploadProgress>((subscriber) => {
      subscriber.next({ state: 'REQUESTING_SIGNATURE', progress: 0 });

      this.getUploadSignature(targetType, storeId).subscribe({
        next: (signature) => this.performDirectUpload(file, signature, subscriber),
        error: (err) => {
          const errorMessage = this.extractErrorMessage(err, 'No se pudo autorizar la subida de la imagen.');
          subscriber.next({ state: 'ERROR', progress: 0, error: errorMessage });
          subscriber.error(err);
        },
      });
    });
  }

  /**
   * Sube un archivo a Cloudinary y retorna un Observable con la URL final segura (secure_url).
   */
  uploadImageDirect(
    file: File,
    targetType: MediaTargetType = 'PRODUCT_IMAGE',
    storeId?: string,
  ): Observable<string> {
    return new Observable<string>((subscriber) => {
      this.uploadImage(file, targetType, storeId).subscribe({
        next: (progress) => {
          if (progress.state === 'COMPLETED' && progress.result?.secure_url) {
            subscriber.next(progress.result.secure_url);
            subscriber.complete();
          } else if (progress.state === 'ERROR') {
            subscriber.error(new Error(progress.error || 'Error al subir la imagen.'));
          }
        },
        error: (err) => subscriber.error(err),
      });
    });
  }

  /**
   * Procesa una lista de imágenes que pueden contener archivos locales pendientes de subida (`file`).
   * Sube a Cloudinary únicamente aquellos ítems que tengan `file` y preserva las URLs de los que ya existían.
   */
  uploadPendingImages(
    images: ProductImage[],
    targetType: MediaTargetType = 'PRODUCT_IMAGE',
    storeId?: string,
  ): Observable<ProductImage[]> {
    if (!images || images.length === 0) {
      return of([]);
    }

    const tasks$ = images.map((img, idx) => {
      if (img.file) {
        return this.uploadImageDirect(img.file, targetType, storeId).pipe(
          map((uploadedUrl) => ({
            ...img,
            imageUrl: uploadedUrl,
            file: undefined,
            position: idx,
          })),
        );
      }
      return of({
        ...img,
        position: idx,
      });
    });

    return forkJoin(tasks$);
  }

  private performDirectUpload(
    file: File,
    signature: UploadSignatureResponse,
    subscriber: any,
  ): void {
    subscriber.next({ state: 'UPLOADING', progress: 10 });
    const formData = this.buildFormData(file, signature);

    this.http
      .post<CloudinaryUploadResult>(signature.uploadUrl, formData, {
        reportProgress: true,
        observe: 'events',
      })
      .subscribe({
        next: (event: HttpEvent<CloudinaryUploadResult>) => {
          this.handleUploadHttpEvent(event, subscriber);
        },
        error: (err) => {
          const errorMessage = this.extractErrorMessage(err, 'Error al subir la imagen al servidor.');
          subscriber.next({ state: 'ERROR', progress: 0, error: errorMessage });
          subscriber.error(err);
        },
      });
  }

  private handleUploadHttpEvent(
    event: HttpEvent<CloudinaryUploadResult>,
    subscriber: any,
  ): void {
    if (event.type === HttpEventType.UploadProgress) {
      const percent = this.calculateUploadProgress(event.loaded, event.total);
      subscriber.next({ state: 'UPLOADING', progress: percent });
    } else if (event.type === HttpEventType.Response) {
      subscriber.next({
        state: 'COMPLETED',
        progress: 100,
        result: event.body ?? undefined,
      });
      subscriber.complete();
    }
  }

  private buildFormData(file: File, signature: UploadSignatureResponse): FormData {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('api_key', signature.apiKey);
    formData.append('timestamp', signature.timestamp.toString());
    formData.append('signature', signature.signature);
    formData.append('folder', signature.folder);
    return formData;
  }

  private calculateUploadProgress(loaded: number, total?: number): number {
    if (!total || total <= 0) return 50;
    return Math.min(95, Math.round((loaded / total) * 90) + 10);
  }

  private extractErrorMessage(error: any, fallbackMessage: string): string {
    return error?.error?.error?.message ?? error?.error?.message ?? error?.message ?? fallbackMessage;
  }
}
