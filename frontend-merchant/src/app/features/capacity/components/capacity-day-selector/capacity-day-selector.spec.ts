import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacityDaySelector } from './capacity-day-selector';
import { DAYS_OF_WEEK } from '../../../../core/models/capacity.models';

describe('CapacityDaySelector', () => {
  let component: CapacityDaySelector;
  let fixture: ComponentFixture<CapacityDaySelector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacityDaySelector],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacityDaySelector);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('days', DAYS_OF_WEEK);
    fixture.componentRef.setInput('selectedDay', 6);
    fixture.componentRef.setInput('allConfigurations', [
      {
        id: 'cfg-1',
        storeId: 'store-1',
        dayOfWeek: 6,
        startTime: '09:00:00',
        endTime: '14:00:00',
        maxCapacity: 10,
        status: 'ACTIVE',
        createdAt: '2026-09-19T10:00:00Z',
        updatedAt: '2026-09-19T10:00:00Z',
      },
    ]);
    await fixture.whenStable();
  });

  it('should create and count active slots accurately', () => {
    expect(component).toBeTruthy();
    expect(component.getActiveCount(6)).toBe(1);
    expect(component.getActiveCount(1)).toBe(0);
  });
});
