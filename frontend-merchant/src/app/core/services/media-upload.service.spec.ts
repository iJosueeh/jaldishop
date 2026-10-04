import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MediaUploadService } from './media-upload.service';
import { UploadSignatureResponse, CloudinaryUploadResult, MediaUploadProgress } from '../models/media.models';
import { environment } from '../../../environments/environment';

describe('MediaUploadService', () => {
  let service: MediaUploadService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/media`;

  const mockSignature: UploadSignatureResponse = {
    cloudName: 'test-cloud',
    apiKey: 'test-key',
    timestamp: 1700000000,
    folder: 'jaldishop/tenants/store-1/branding',
    signature: 'signed-hash-123',
    uploadUrl: 'https://api.cloudinary.com/v1_1/test-cloud/image/upload',
  };

  const mockUploadResult: CloudinaryUploadResult = {
    public_id: 'jaldishop/tenants/store-1/branding/logo_123',
    secure_url: 'https://res.cloudinary.com/test-cloud/image/upload/v1/logo_123.webp',
    format: 'webp',
    width: 400,
    height: 400,
    bytes: 25000,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MediaUploadService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MediaUploadService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  describe('validateImageFile', () => {
    it('debe validar correctamente una imagen JPG de tamaño adecuado', () => {
      const file = new File(['dummy content'], 'logo.jpg', { type: 'image/jpeg' });
      const result = service.validateImageFile(file);
      expect(result.valid).toBe(true);
    });

    it('debe rechazar archivo con formato no soportado (ej. PDF)', () => {
      const file = new File(['dummy content'], 'document.pdf', { type: 'application/pdf' });
      const result = service.validateImageFile(file);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('Formato no soportado');
    });

    it('debe rechazar archivo que supere el tamaño límite', () => {
      const largeContent = new Uint8Array(6 * 1024 * 1024);
      const file = new File([largeContent], 'heavy.png', { type: 'image/png' });
      const result = service.validateImageFile(file, 5 * 1024 * 1024);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('supera el tamaño máximo');
    });
  });

  describe('getUploadSignature', () => {
    it('debe solicitar firma al endpoint del backend', () => {
      service.getUploadSignature('STORE_LOGO', 'store-1').subscribe((response) => {
        expect(response).toEqual(mockSignature);
      });

      const req = httpTesting.expectOne(`${baseUrl}/upload-signature`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ targetType: 'STORE_LOGO', storeId: 'store-1' });
      req.flush(mockSignature);
    });
  });

  describe('uploadImage', () => {
    it('debe orquestar la obtención de firma y la subida directa a Cloudinary', () => {
      const file = new File(['sample image data'], 'logo.png', { type: 'image/png' });
      const progressUpdates: MediaUploadProgress[] = [];

      service.uploadImage(file, 'STORE_LOGO', 'store-1').subscribe({
        next: (progress) => {
          progressUpdates.push(progress);
        },
      });

      // 1. Backend signature request
      const signatureReq = httpTesting.expectOne(`${baseUrl}/upload-signature`);
      expect(signatureReq.request.method).toBe('POST');
      signatureReq.flush(mockSignature);

      // 2. Cloudinary direct upload request
      const cloudinaryReq = httpTesting.expectOne(mockSignature.uploadUrl);
      expect(cloudinaryReq.request.method).toBe('POST');
      cloudinaryReq.flush(mockUploadResult);

      // Verify states emitted
      const states = progressUpdates.map((p) => p.state);
      expect(states).toContain('REQUESTING_SIGNATURE');
      expect(states).toContain('UPLOADING');
      expect(states).toContain('COMPLETED');

      const completed = progressUpdates.find((p) => p.state === 'COMPLETED');
      expect(completed?.result?.secure_url).toBe(mockUploadResult.secure_url);
    });
  });

  describe('uploadPendingImages', () => {
    it('debe retornar lista vacía si no se proporcionan imágenes', () => {
      service.uploadPendingImages([]).subscribe((res) => {
        expect(res).toEqual([]);
      });
    });

    it('debe preservar imágenes que no tengan archivo File pendiente', () => {
      const existingImages = [
        { imageUrl: 'https://res.cloudinary.com/test/img1.webp', position: 0, isPrimary: true },
        { imageUrl: 'https://res.cloudinary.com/test/img2.webp', position: 1, isPrimary: false },
      ];

      service.uploadPendingImages(existingImages).subscribe((res) => {
        expect(res).toEqual(existingImages);
      });
    });
  });
});
