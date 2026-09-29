import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminCommandPalette } from './admin-command-palette';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth-service';
import { ToastService } from '../../../../core/services/toast.service';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';

describe('AdminCommandPalette', () => {
  let component: AdminCommandPalette;
  let fixture: ComponentFixture<AdminCommandPalette>;

  const mockAdminService = {
    users: signal([
      {
        id: 'u1',
        email: 'maria@test.com',
        firstName: 'Maria',
        lastName: 'Lopez',
        phone: '987654321',
        roles: ['MERCHANT'],
        status: 'ACTIVE',
        createdAt: '2026-03-01T10:00:00Z',
      },
    ]),
    stores: signal([
      {
        id: 's1',
        merchantUserId: 'u1',
        name: 'Panaderia San Jose',
        slug: 'panaderia-san-jose',
        status: 'ACTIVE' as const,
        deliveryEnabled: true,
        pickupEnabled: true,
        contactPhone: '999888777',
        createdAt: '2026-03-01T10:00:00Z',
        updatedAt: '2026-03-01T10:00:00Z',
        merchant: {
          id: 'u1',
          email: 'maria@test.com',
          fullName: 'Maria Lopez',
          status: 'ACTIVE' as const,
        },
      },
    ]),
    loadUsers: vi.fn().mockReturnValue(of([])),
    loadStores: vi.fn().mockReturnValue(of([])),
    setUserSearchQuery: vi.fn(),
    setStoreSearchQuery: vi.fn(),
  };

  const mockAuthService = {
    logout: vi.fn(),
  };

  const mockToastService = {
    success: vi.fn(),
    error: vi.fn(),
  };

  const mockRouter = {
    navigate: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCommandPalette],
      providers: [
        { provide: AdminService, useValue: mockAdminService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCommandPalette);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe listar acciones directas por defecto cuando no hay búsqueda', () => {
    const items = component.filteredItems();
    expect(items.length).toBeGreaterThan(0);
    expect(items.some((i) => i.id === 'act-refresh')).toBe(true);
    expect(items.some((i) => i.id === 'act-logout')).toBe(true);
  });

  it('debe filtrar usuarios y tiendas según el query limitando a máximo 5 resultados', () => {
    component.onQueryChange('maria');
    const items = component.filteredItems();
    expect(items.some((i) => i.id === 'user-u1')).toBe(true);
    expect(items.length).toBeLessThanOrEqual(5);
  });

  it('debe agrupar items en categorías visuales', () => {
    const groups = component.groupedItems();
    expect(groups.length).toBeGreaterThan(0);
    expect(groups[0].name).toBe('Acciones Rápidas');
  });

  it('debe calcular el índice global de un item', () => {
    const items = component.filteredItems();
    if (items.length > 0) {
      const idx = component.getItemGlobalIndex(items[0]);
      expect(idx).toBe(0);
    }
  });

  it('debe manejar eventos de teclado Escape, ArrowDown, ArrowUp y Enter en el switch', () => {
    vi.useFakeTimers();
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    const spyClose = vi.spyOn(component.closePalette, 'emit');

    // 1. Escape activa isClosing y luego emite closePalette
    const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
    component.handleKeyDown(escapeEvent);
    expect(component.isClosing()).toBe(true);
    vi.advanceTimersByTime(200);
    expect(spyClose).toHaveBeenCalled();

    // 2. ArrowDown
    const arrowDownEvent = new KeyboardEvent('keydown', { key: 'ArrowDown' });
    component.selectedIndex.set(0);
    component.handleKeyDown(arrowDownEvent);
    expect(component.selectedIndex()).toBe(1);

    // 3. ArrowUp
    const arrowUpEvent = new KeyboardEvent('keydown', { key: 'ArrowUp' });
    component.handleKeyDown(arrowUpEvent);
    expect(component.selectedIndex()).toBe(0);

    // 4. Enter
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    component.handleKeyDown(enterEvent);
    expect(mockAdminService.loadUsers).toHaveBeenCalled();

    vi.useRealTimers();
  });
});
