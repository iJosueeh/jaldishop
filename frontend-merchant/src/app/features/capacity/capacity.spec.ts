import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Capacity } from './capacity';
import { CapacityService } from '../../core/services/capacity.service';
import { ToastService } from '../../core/services/toast.service';
import { CapacityConfiguration } from '../../core/models/capacity.models';
import { signal } from '@angular/core';

describe('Capacity', () => {
  let component: Capacity;
  let fixture: ComponentFixture<Capacity>;
  let capacityServiceMock: any;
  let toastServiceMock: any;

  const mockConfigs: CapacityConfiguration[] = [
    {
      id: 'cfg-1',
      storeId: 'store-1',
      dayOfWeek: 1, // Lunes
      startTime: '09:00:00',
      endTime: '14:00:00',
      maxCapacity: 10,
      status: 'ACTIVE',
      createdAt: '2026-09-19T10:00:00Z',
      updatedAt: '2026-09-19T10:00:00Z',
    },
    {
      id: 'cfg-2',
      storeId: 'store-1',
      dayOfWeek: 1, // Lunes
      startTime: '14:00:00',
      endTime: '18:00:00',
      maxCapacity: 15,
      status: 'INACTIVE',
      createdAt: '2026-09-19T10:00:00Z',
      updatedAt: '2026-09-19T10:00:00Z',
    },
    {
      id: 'cfg-3',
      storeId: 'store-1',
      dayOfWeek: 2, // Martes
      startTime: '10:00:00',
      endTime: '15:00:00',
      maxCapacity: 8,
      status: 'ACTIVE',
      createdAt: '2026-09-19T10:00:00Z',
      updatedAt: '2026-09-19T10:00:00Z',
    },
  ];

  const mockExceptions = [
    {
      id: 'exc-1',
      storeId: 'store-1',
      serviceDate: '2026-12-25',
      startTime: '09:00:00',
      endTime: '14:00:00',
      exceptionCapacity: 0,
      reason: 'Cerrado por Navidad',
      status: 'ACTIVE' as const,
      createdAt: '2026-09-22T10:00:00Z',
      updatedAt: '2026-09-22T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    capacityServiceMock = {
      configurations: signal<CapacityConfiguration[]>(mockConfigs),
      isLoading: signal<boolean>(false),
      exceptions: signal<any[]>(mockExceptions),
      isLoadingExceptions: signal<boolean>(false),
      getConfigurations: vi.fn().mockReturnValue(of(mockConfigs)),
      createConfiguration: vi.fn().mockReturnValue(of(mockConfigs[0])),
      updateConfiguration: vi.fn().mockReturnValue(of(mockConfigs[0])),
      activateConfiguration: vi.fn().mockReturnValue(of(mockConfigs[0])),
      deactivateConfiguration: vi.fn().mockReturnValue(of(mockConfigs[1])),
      getExceptions: vi.fn().mockReturnValue(of(mockExceptions)),
      createException: vi.fn().mockReturnValue(of(mockExceptions[0])),
      updateException: vi.fn().mockReturnValue(of(mockExceptions[0])),
      activateException: vi.fn().mockReturnValue(of(mockExceptions[0])),
      deactivateException: vi.fn().mockReturnValue(of(mockExceptions[0])),
    };

    toastServiceMock = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Capacity],
      providers: [
        { provide: CapacityService, useValue: capacityServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Capacity);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create and load configurations on init', () => {
    expect(component).toBeTruthy();
    expect(capacityServiceMock.getConfigurations).toHaveBeenCalled();
  });

  it('should filter configurations by selected day', () => {
    component.selectDay(1); // Lunes
    const mondaySlots = component.selectedDayConfigurations();
    expect(mondaySlots.length).toBe(2);
    expect(mondaySlots[0].id).toBe('cfg-1');
    expect(mondaySlots[1].id).toBe('cfg-2');
  });

  it('should calculate active total capacity for the selected day', () => {
    component.selectDay(1); // Lunes (cfg-1 is ACTIVE with 10, cfg-2 is INACTIVE with 15)
    expect(component.selectedDayTotalCapacity()).toBe(10);
  });

  it('should count active slots for a given day', () => {
    expect(component.getActiveSlotsCount(1)).toBe(1);
    expect(component.getActiveSlotsCount(2)).toBe(1);
    expect(component.getActiveSlotsCount(3)).toBe(0);
  });

  it('should open and close the modal in create and edit mode', () => {
    component.openCreateModal();
    expect(component.isModalOpen()).toBe(true);
    expect(component.selectedSlotToEdit()).toBeNull();

    component.closeModal();
    expect(component.isModalOpen()).toBe(false);

    component.openEditModal(mockConfigs[0]);
    expect(component.isModalOpen()).toBe(true);
    expect(component.selectedSlotToEdit()).toEqual(mockConfigs[0]);

    component.closeModal();
    expect(component.isModalOpen()).toBe(false);
    expect(component.selectedSlotToEdit()).toBeNull();
  });

  it('should deactivate an active configuration', () => {
    component.onToggleStatus(mockConfigs[0]);
    expect(capacityServiceMock.deactivateConfiguration).toHaveBeenCalledWith('cfg-1');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Franja horaria pausada.');
  });

  it('should activate an inactive configuration', () => {
    component.onToggleStatus(mockConfigs[1]);
    expect(capacityServiceMock.activateConfiguration).toHaveBeenCalledWith('cfg-2');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Franja horaria activada.');
  });

  it('should create new configuration slot and show success toast', () => {
    component.openCreateModal();

    const payload = {
      dayOfWeek: 1,
      startTime: '09:00:00',
      endTime: '13:00:00',
      maxCapacity: 12,
    };

    component.onSaveSlot({ request: payload });

    expect(capacityServiceMock.createConfiguration).toHaveBeenCalledWith(payload);
    expect(component.isModalOpen()).toBe(false);
    expect(toastServiceMock.success).toHaveBeenCalledWith('¡Franja de capacidad creada con éxito!');
  });

  it('should update existing configuration slot and show success toast', () => {
    component.openEditModal(mockConfigs[0]);

    const payload = {
      dayOfWeek: 1,
      startTime: '09:00:00',
      endTime: '14:00:00',
      maxCapacity: 18,
    };

    component.onSaveSlot({ id: 'cfg-1', request: payload });

    expect(capacityServiceMock.updateConfiguration).toHaveBeenCalledWith('cfg-1', payload);
    expect(component.isModalOpen()).toBe(false);
    expect(toastServiceMock.success).toHaveBeenCalledWith('¡Franja de capacidad actualizada con éxito!');
  });

  describe('Excepciones de Capacidad en Capacity Component', () => {
    const mockException = {
      id: 'exc-1',
      storeId: 'store-1',
      serviceDate: '2026-12-25',
      startTime: '09:00:00',
      endTime: '14:00:00',
      exceptionCapacity: 0,
      reason: 'Cerrado por Navidad',
      status: 'ACTIVE' as const,
      createdAt: '2026-09-22T10:00:00Z',
      updatedAt: '2026-09-22T10:00:00Z',
    };

    it('debe cambiar de pestaña entre SCHEDULE y EXCEPTIONS', () => {
      expect(component.activeTab()).toBe('SCHEDULE');
      component.setActiveTab('EXCEPTIONS');
      expect(component.activeTab()).toBe('EXCEPTIONS');
      expect(capacityServiceMock.getExceptions).toHaveBeenCalled();
    });

    it('debe abrir y cerrar el modal de excepciones en create y edit', () => {
      component.openCreateExceptionModal();
      expect(component.isExceptionModalOpen()).toBe(true);
      expect(component.selectedExceptionToEdit()).toBeNull();

      component.closeExceptionModal();
      expect(component.isExceptionModalOpen()).toBe(false);

      component.openEditExceptionModal(mockException);
      expect(component.isExceptionModalOpen()).toBe(true);
      expect(component.selectedExceptionToEdit()).toEqual(mockException);

      component.closeExceptionModal();
      expect(component.isExceptionModalOpen()).toBe(false);
      expect(component.selectedExceptionToEdit()).toBeNull();
    });

    it('debe guardar nueva excepción con éxito', () => {
      capacityServiceMock.createException = vi.fn().mockReturnValue(of(mockException));
      component.openCreateExceptionModal();

      const payload = {
        serviceDate: '2026-12-25',
        startTime: '09:00:00',
        endTime: '14:00:00',
        exceptionCapacity: 0,
        reason: 'Navidad',
      };

      component.onSaveException({ request: payload });

      expect(capacityServiceMock.createException).toHaveBeenCalledWith(payload);
      expect(component.isExceptionModalOpen()).toBe(false);
      expect(toastServiceMock.success).toHaveBeenCalledWith(
        '¡Fecha especial / excepción creada con éxito!',
      );
    });

    it('debe actualizar excepción existente con éxito', () => {
      capacityServiceMock.updateException = vi.fn().mockReturnValue(of(mockException));
      component.openEditExceptionModal(mockException);

      const payload = {
        serviceDate: '2026-12-25',
        startTime: '10:00:00',
        endTime: '15:00:00',
        exceptionCapacity: 5,
        reason: 'Navidad Medio Tiempo',
      };

      component.onSaveException({ id: 'exc-1', request: payload });

      expect(capacityServiceMock.updateException).toHaveBeenCalledWith('exc-1', payload);
      expect(component.isExceptionModalOpen()).toBe(false);
      expect(toastServiceMock.success).toHaveBeenCalledWith(
        '¡Fecha especial / excepción actualizada con éxito!',
      );
    });

    it('debe activar y desactivar excepción', () => {
      capacityServiceMock.deactivateException = vi.fn().mockReturnValue(of(mockException));
      capacityServiceMock.activateException = vi.fn().mockReturnValue(of(mockException));

      // Toggle active to inactive
      component.onToggleExceptionStatus(mockException);
      expect(capacityServiceMock.deactivateException).toHaveBeenCalledWith('exc-1');

      // Toggle inactive to active
      component.onToggleExceptionStatus({ ...mockException, status: 'INACTIVE' });
      expect(capacityServiceMock.activateException).toHaveBeenCalledWith('exc-1');
    });
  });
});
