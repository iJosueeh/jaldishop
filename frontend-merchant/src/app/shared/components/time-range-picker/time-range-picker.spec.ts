import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TimeRangePicker } from './time-range-picker';

describe('TimeRangePicker', () => {
  let component: TimeRangePicker;
  let fixture: ComponentFixture<TimeRangePicker>;
  let testForm: FormGroup;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, TimeRangePicker],
    }).compileComponents();

    testForm = new FormGroup({
      startTime: new FormControl('09:00', Validators.required),
      endTime: new FormControl('14:00', Validators.required),
    });

    fixture = TestBed.createComponent(TimeRangePicker);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', testForm);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate duration correctly for initial values', () => {
    expect(component.durationText()).toBe('5 horas');
  });

  it('should calculate duration for fractional hours and minutes', () => {
    testForm.patchValue({ startTime: '09:00', endTime: '10:30' });
    fixture.detectChanges();
    expect(component.durationText()).toBe('1h 30min');
  });

  it('should show error text when endTime is earlier than startTime', () => {
    testForm.patchValue({ startTime: '15:00', endTime: '10:00' });
    fixture.detectChanges();
    expect(component.durationText()).toContain('Horario inválido');
  });

  it('should apply preset correctly', () => {
    const preset = { label: '🌙 18:00 - 22:00', start: '18:00', end: '22:00' };
    component.applyPreset(preset);
    fixture.detectChanges();

    expect(testForm.get('startTime')?.value).toBe('18:00');
    expect(testForm.get('endTime')?.value).toBe('22:00');
    expect(testForm.get('startTime')?.dirty).toBe(true);
    expect(testForm.get('endTime')?.dirty).toBe(true);
    expect(component.durationText()).toBe('4 horas');
  });

  it('should support custom control names', () => {
    const customForm = new FormGroup({
      openTime: new FormControl('08:00'),
      closeTime: new FormControl('20:00'),
    });

    const customFixture = TestBed.createComponent(TimeRangePicker);
    const customComp = customFixture.componentInstance;
    customFixture.componentRef.setInput('form', customForm);
    customFixture.componentRef.setInput('startControlName', 'openTime');
    customFixture.componentRef.setInput('endControlName', 'closeTime');
    customFixture.detectChanges();

    expect(customComp.durationText()).toBe('12 horas');

    customComp.applyPreset({ label: 'Test', start: '10:00', end: '15:00' });
    customFixture.detectChanges();

    expect(customForm.get('openTime')?.value).toBe('10:00');
    expect(customForm.get('closeTime')?.value).toBe('15:00');
  });
});
