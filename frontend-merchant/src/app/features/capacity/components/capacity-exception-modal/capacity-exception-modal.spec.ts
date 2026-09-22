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

  it('debe emitir saveException al enviar formulario válido', () => {
    fixture.componentRef.setInput('isOpen', true);
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
    expect(emitted.serviceDate).toBe('2026-12-25');
    expect(emitted.exceptionCapacity).toBe(0);
    expect(emitted.startTime).toBeNull();
    expect(emitted.endTime).toBeNull();
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
