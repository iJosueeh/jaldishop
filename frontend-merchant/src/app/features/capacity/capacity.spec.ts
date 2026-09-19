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

  beforeEach(async () => {
    capacityServiceMock = {
      configurations: signal<CapacityConfiguration[]>(mockConfigs),
      isLoading: signal<boolean>(false),
      getConfigurations: vi.fn().mockReturnValue(of(mockConfigs)),
      createConfiguration: vi.fn().mockReturnValue(of(mockConfigs[0])),
      activateConfiguration: vi.fn().mockReturnValue(of(mockConfigs[0])),
      deactivateConfiguration: vi.fn().mockReturnValue(of(mockConfigs[1])),
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

  it('should open and close the modal', () => {
    component.openCreateModal();
    expect(component.isModalOpen()).toBe(true);

    component.closeModal();
    expect(component.isModalOpen()).toBe(false);
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

  it('should validate time range when submitting new configuration', () => {
    component.openCreateModal();
    component.form.patchValue({
      startTime: '16:00',
      endTime: '12:00',
      maxCapacity: 10,
    });

    component.onSubmitForm();

    expect(component.errorMessage()).toBe('La hora de inicio debe ser menor que la hora de fin.');
    expect(capacityServiceMock.createConfiguration).not.toHaveBeenCalled();
  });

  it('should submit valid configuration and show success toast', () => {
    component.selectDay(1);
    component.openCreateModal();
    component.form.patchValue({
      startTime: '09:00',
      endTime: '13:00',
      maxCapacity: 12,
    });

    component.onSubmitForm();

    expect(capacityServiceMock.createConfiguration).toHaveBeenCalledWith({
      dayOfWeek: 1,
      startTime: '09:00:00',
      endTime: '13:00:00',
      maxCapacity: 12,
    });
    expect(component.isModalOpen()).toBe(false);
    expect(toastServiceMock.success).toHaveBeenCalledWith('¡Franja de capacidad creada con éxito!');
  });
});
