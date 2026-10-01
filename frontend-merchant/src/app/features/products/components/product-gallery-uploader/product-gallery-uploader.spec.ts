import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductGalleryUploader } from './product-gallery-uploader';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductImage } from '../../../../core/models/product.models';

describe('ProductGalleryUploader', () => {
  let component: ProductGalleryUploader;
  let fixture: ComponentFixture<ProductGalleryUploader>;

  const initialImages: ProductImage[] = [
    { imageUrl: 'https://res.cloudinary.com/demo/image/upload/sample1.jpg', position: 0, isPrimary: true },
    { imageUrl: 'https://res.cloudinary.com/demo/image/upload/sample2.jpg', position: 1, isPrimary: false },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductGalleryUploader],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductGalleryUploader);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('images', initialImages);
    fixture.componentRef.setInput('maxImages', 5);
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar las imágenes', () => {
    expect(component).toBeTruthy();
    expect(component.internalImages().length).toBe(2);
    expect(component.internalImages()[0].isPrimary).toBe(true);
  });

  it('debe permitir cambiar la imagen principal de portada', () => {
    component.setPrimary(1);
    expect(component.internalImages()[0].isPrimary).toBe(false);
    expect(component.internalImages()[1].isPrimary).toBe(true);
  });

  it('debe permitir mover la posición de una imagen', () => {
    component.movePosition(1, 'left');
    expect(component.internalImages()[0].imageUrl).toContain('sample2');
    expect(component.internalImages()[1].imageUrl).toContain('sample1');
  });

  it('debe permitir eliminar una imagen de la galería', () => {
    component.removeImage(0);
    expect(component.internalImages().length).toBe(1);
    expect(component.internalImages()[0].isPrimary).toBe(true);
  });

  it('debe solicitar confirmación antes de eliminar una imagen', () => {
    component.requestDelete(0);
    expect(component.showDeleteModal()).toBe(true);
    expect(component.pendingDeleteIndex()).toBe(0);

    component.cancelDelete();
    expect(component.showDeleteModal()).toBe(false);
    expect(component.pendingDeleteIndex()).toBeNull();
    expect(component.internalImages().length).toBe(2);

    component.requestDelete(0);
    component.confirmDelete();
    expect(component.showDeleteModal()).toBe(false);
    expect(component.internalImages().length).toBe(1);
  });

  it('debe permitir agregar una imagen mediante enlace URL', () => {
    component.urlInputValue.set('https://res.cloudinary.com/demo/image/upload/sample3.jpg');
    component.addFromUrl();
    expect(component.internalImages().length).toBe(3);
    expect(component.internalImages()[2].imageUrl).toContain('sample3');
  });
});
