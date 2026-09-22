import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacitySlotModal } from './capacity-slot-modal';

describe('CapacitySlotModal', () => {
  let component: CapacitySlotModal;
  let fixture: ComponentFixture<CapacitySlotModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacitySlotModal],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacitySlotModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe inicializar el formulario con valores por defecto', () => {
    expect(component.form.get('startTime')?.value).toBe('09:00');
    expect(component.form.get('endTime')?.value).toBe('14:00');
    expect(component.form.get('maxCapacity')?.value).toBe(10);
  });

  it('debe emitir saveSlot al enviar formulario válido', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('dayOfWeek', 2);
    fixture.detectChanges();

    component.form.patchValue({
      startTime: '10:00',
      endTime: '15:00',
      maxCapacity: 15,
    });

    let emitted: any = null;
    component.saveSlot.subscribe((val) => {
      emitted = val;
    });

    component.onSubmit();

    expect(emitted).toBeTruthy();
    expect(emitted.dayOfWeek).toBe(2);
    expect(emitted.startTime).toBe('10:00:00');
    expect(emitted.endTime).toBe('15:00:00');
    expect(emitted.maxCapacity).toBe(15);
  });

  it('debe validar horario inicio < fin', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    component.form.patchValue({
      startTime: '16:00',
      endTime: '12:00',
      maxCapacity: 10,
    });

    component.onSubmit();
    expect(component.errorMessage()).toBe('La hora de inicio debe ser menor que la hora de fin.');
  });
});
