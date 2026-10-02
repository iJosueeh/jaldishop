import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreIdentityCard } from './store-identity-card';
import { FormControl, FormGroup } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('StoreIdentityCard', () => {
  let component: StoreIdentityCard;
  let fixture: ComponentFixture<StoreIdentityCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreIdentityCard],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreIdentityCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', new FormGroup({
      name: new FormControl(''),
      contactPhone: new FormControl(''),
      description: new FormControl(''),
      logoUrl: new FormControl(''),
      bannerUrl: new FormControl(''),
      instagramUrl: new FormControl(''),
      facebookUrl: new FormControl(''),
      categoryIds: new FormControl<string[]>([]),
    }));
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe permitir seleccionar y deseleccionar categorias', () => {
    const catId = 'cat-123';
    expect(component.isSelected(catId)).toBe(false);

    component.toggleCategory(catId);
    expect(component.isSelected(catId)).toBe(true);
    expect(component.form().get('categoryIds')?.value).toEqual([catId]);

    component.toggleCategory(catId);
    expect(component.isSelected(catId)).toBe(false);
    expect(component.form().get('categoryIds')?.value).toEqual([]);
  });

  it('debe solicitar confirmación y remover el logo al confirmar', () => {
    component.form().get('logoUrl')?.setValue('https://res.cloudinary.com/demo/logo.png');
    
    component.requestDelete('logoUrl');
    expect(component.showDeleteModal()).toBe(true);
    expect(component.pendingDeleteControl()).toBe('logoUrl');
    expect(component.deleteModalTitle).toContain('Logo');

    component.cancelDelete();
    expect(component.showDeleteModal()).toBe(false);
    expect(component.pendingDeleteControl()).toBeNull();
    expect(component.form().get('logoUrl')?.value).toBe('https://res.cloudinary.com/demo/logo.png');

    component.requestDelete('logoUrl');
    component.confirmDelete();
    expect(component.showDeleteModal()).toBe(false);
    expect(component.form().get('logoUrl')?.value).toBe('');
  });

  it('debe solicitar confirmación y remover el banner al confirmar', () => {
    component.form().get('bannerUrl')?.setValue('https://res.cloudinary.com/demo/banner.png');
    
    component.requestDelete('bannerUrl');
    expect(component.showDeleteModal()).toBe(true);
    expect(component.pendingDeleteControl()).toBe('bannerUrl');
    expect(component.deleteModalTitle).toContain('Portada');

    component.confirmDelete();
    expect(component.showDeleteModal()).toBe(false);
    expect(component.form().get('bannerUrl')?.value).toBe('');
  });
});
