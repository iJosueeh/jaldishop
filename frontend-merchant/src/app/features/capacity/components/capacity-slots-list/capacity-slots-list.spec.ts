import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacitySlotsList } from './capacity-slots-list';

describe('CapacitySlotsList', () => {
  let component: CapacitySlotsList;
  let fixture: ComponentFixture<CapacitySlotsList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacitySlotsList],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacitySlotsList);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('slots', []);
    fixture.componentRef.setInput('dayLabel', 'Sábado');
    fixture.componentRef.setInput('isLoading', false);
    await fixture.whenStable();
  });

  it('should create with initial inputs', () => {
    expect(component).toBeTruthy();
    expect(component.dayLabel()).toBe('Sábado');
    expect(component.slots()).toEqual([]);
  });

  it('debe emitir editSlot al presionar el botón Editar', () => {
    const mockSlot: any = {
      id: 'cfg-1',
      dayOfWeek: 6,
      startTime: '09:00:00',
      endTime: '12:00:00',
      maxCapacity: 15,
      status: 'ACTIVE',
    };

    fixture.componentRef.setInput('slots', [mockSlot]);
    fixture.detectChanges();

    let emittedSlot: any = null;
    component.editSlot.subscribe((slot) => {
      emittedSlot = slot;
    });

    const editBtn = fixture.nativeElement.querySelector('button');
    editBtn.click();

    expect(emittedSlot).toEqual(mockSlot);
  });

  it('debe emitir toggleStatus al presionar el botón Pausar/Reactivar', () => {
    const mockSlot: any = {
      id: 'cfg-1',
      dayOfWeek: 6,
      startTime: '09:00:00',
      endTime: '12:00:00',
      maxCapacity: 15,
      status: 'ACTIVE',
    };

    fixture.componentRef.setInput('slots', [mockSlot]);
    fixture.detectChanges();

    let emittedSlot: any = null;
    component.toggleStatus.subscribe((slot) => {
      emittedSlot = slot;
    });

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[1].click();

    expect(emittedSlot).toEqual(mockSlot);
  });
});
