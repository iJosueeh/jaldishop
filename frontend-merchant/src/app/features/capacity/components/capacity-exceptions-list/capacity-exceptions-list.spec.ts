import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacityExceptionsList } from './capacity-exceptions-list';
import { CapacityException } from '../../../../core/models/capacity.models';

describe('CapacityExceptionsList', () => {
  let component: CapacityExceptionsList;
  let fixture: ComponentFixture<CapacityExceptionsList>;

  const mockExceptions: CapacityException[] = [
    {
      id: 'exc-1',
      storeId: 'store-1',
      serviceDate: '2026-12-25',
      startTime: '09:00:00',
      endTime: '14:00:00',
      exceptionCapacity: 0,
      reason: 'Cerrado por Navidad',
      status: 'ACTIVE',
      createdAt: '2026-09-22T10:00:00Z',
      updatedAt: '2026-09-22T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacityExceptionsList],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacityExceptionsList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe formatear la fecha correctamente', () => {
    const formatted = component.formatDate('2026-12-25');
    expect(formatted).toContain('25');
    expect(formatted).toContain('2026');
  });

  it('debe formatear el rango de horas correctamente', () => {
    expect(component.formatTimeRange('09:00:00', '14:00:00')).toBe('09:00 - 14:00');
    expect(component.formatTimeRange(null, null)).toBe('Todo el día');
  });

  it('debe emitir toggleStatus al hacer clic en el botón de acción', () => {
    fixture.componentRef.setInput('exceptions', mockExceptions);
    fixture.detectChanges();

    let emitted: CapacityException | null = null;
    component.toggleStatus.subscribe((val) => {
      emitted = val;
    });

    const button = fixture.nativeElement.querySelector('article button');
    button.click();

    expect(emitted).toEqual(mockExceptions[0]);
  });
});
