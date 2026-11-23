import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ContextSwitcher } from './context-switcher';
import { AuthService } from '../../../core/services/auth-service';

describe('ContextSwitcher', () => {
  let component: ContextSwitcher;
  let fixture: ComponentFixture<ContextSwitcher>;
  let router: Router;
  let authServiceMock: {
    isDualRole: ReturnType<typeof vi.fn>;
    isMerchant: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      isDualRole: vi.fn().mockReturnValue(true),
      isMerchant: vi.fn().mockReturnValue(true),
      isAdmin: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [ContextSwitcher],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(ContextSwitcher);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente cuando el usuario tiene rol dual', () => {
    expect(component).toBeTruthy();
    expect(component.isDualRole()).toBe(true);
  });

  it('detecta el contexto MERCHANT en rutas de comerciante', () => {
    component.currentUrl.set('/dashboard');
    expect(component.currentContext()).toBe('MERCHANT');
  });

  it('detecta el contexto ADMIN en rutas de administración', () => {
    component.currentUrl.set('/admin/dashboard');
    expect(component.currentContext()).toBe('ADMIN');
  });

  it('abre y cierra el desplegable con toggleDropdown()', () => {
    expect(component.isOpen()).toBe(false);
    component.toggleDropdown();
    expect(component.isOpen()).toBe(true);
    component.toggleDropdown();
    expect(component.isOpen()).toBe(false);
  });

  it('navega a /admin/dashboard al cambiar a ADMIN', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.currentUrl.set('/dashboard');
    component.switchContext('ADMIN');
    expect(navSpy).toHaveBeenCalledWith(['/admin/dashboard']);
    expect(component.isOpen()).toBe(false);
  });

  it('navega a /dashboard al cambiar a MERCHANT desde ADMIN', () => {
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.currentUrl.set('/admin/dashboard');
    component.switchContext('MERCHANT');
    expect(navSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('reacciona al atajo de teclado Alt + S para alternar el menú', () => {
    const event = new KeyboardEvent('keydown', { altKey: true, key: 's' });
    window.dispatchEvent(event);
    expect(component.isOpen()).toBe(true);
  });

  it('cierra el menú al presionar Escape', () => {
    component.isOpen.set(true);
    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    window.dispatchEvent(event);
    expect(component.isOpen()).toBe(false);
  });
});
