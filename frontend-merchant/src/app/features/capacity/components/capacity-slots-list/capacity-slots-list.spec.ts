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
});
