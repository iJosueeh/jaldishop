import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacityExceptionModal } from './capacity-exception-modal';

describe('CapacityExceptionModal', () => {
  let component: CapacityExceptionModal;
  let fixture: ComponentFixture<CapacityExceptionModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacityExceptionModal],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacityExceptionModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe inicializar el formulario con valores por defecto', () => {
    expect(component.form.get('serviceDate')?.value).toBeDefined();
    expect(component.form.get('isAllDay')?.value).toBe(true);
    expect(component.form.get('exceptionCapacity')?.value).toBe(0);
  });

  it('debe emitir saveException al enviar formulario válido en modo creación', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('exceptionToEdit', null);
    fixture.detectChanges();

    component.form.patchValue({
      serviceDate: '2026-12-25',
      isAllDay: true,
      exceptionCapacity: 0,
      reason: 'Navidad',
    });

    let emitted: any = null;
    component.saveException.subscribe((val) => {
      emitted = val;
    });

    component.onSubmit();

    expect(emitted).toBeTruthy();
    expect(emitted.id).toBeUndefined();
    expect(emitted.request.serviceDate).toBe('2026-12-25');
    expect(emitted.request.exceptionCapacity).toBe(0);
    expect(emitted.request.startTime).toBeNull();
    expect(emitted.request.endTime).toBeNull();
  });

  it('debe precargar datos y emitir id en modo edición', () => {
    const mockException: any = {
      id: 'exc-999',
      serviceDate: '2026-12-31',
      startTime: '10:00:00',
      endTime: '18:00:00',
      exceptionCapacity: 25,
      reason: 'Fin de año',
      status: 'ACTIVE',
    };

    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('exceptionToEdit', mockException);
    fixture.detectChanges();

    expect(component.form.get('serviceDate')?.value).toBe('2026-12-31');
    expect(component.form.get('isAllDay')?.value).toBe(false);
    expect(component.form.get('startTime')?.value).toBe('10:00');
    expect(component.form.get('endTime')?.value).toBe('18:00');
    expect(component.form.get('exceptionCapacity')?.value).toBe(25);
    expect(component.form.get('reason')?.value).toBe('Fin de año');

    let emitted: any = null;
    component.saveException.subscribe((val) => {
      emitted = val;
    });

    component.onSubmit();

    expect(emitted).toBeTruthy();
    expect(emitted.id).toBe('exc-999');
    expect(emitted.request.serviceDate).toBe('2026-12-31');
    expect(emitted.request.exceptionCapacity).toBe(25);
  });

  it('debe validar horario si isAllDay es falso', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    component.form.patchValue({
      serviceDate: '2026-12-25',
      isAllDay: false,
      startTime: '16:00',
      endTime: '12:00', // Inválido: start > end
      exceptionCapacity: 10,
    });

    component.onSubmit();
    expect(component.errorMessage()).toBe('La hora de inicio debe ser menor que la hora de fin.');
  });
});
