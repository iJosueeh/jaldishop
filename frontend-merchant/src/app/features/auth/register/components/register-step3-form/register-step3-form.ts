import { Component, inject, input, OnInit, output } from '@angular/core';
import { outputFromObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RegisterFooter } from '../../../../../shared/components/register-footer/register-footer';
import { RegisterStepHeader } from '../../../../../shared/components/register-step-header/register-step-header';
import { AlertError } from '../../../../../shared/components/alert-error/alert-error';
import { DayOption, RegisterStep3Data } from '../../interface/register.models';

@Component({
  imports: [ReactiveFormsModule, RegisterFooter, RegisterStepHeader, AlertError],
  selector: 'app-register-step3-form',
  styleUrl: './register-step3-form.css',
  templateUrl: './register-step3-form.html',
})
export class RegisterStep3Form implements OnInit {
  private fb = inject(FormBuilder);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);

  readonly initialData = input<RegisterStep3Data | null>();

  readonly back = output<void>();
  readonly step3Submit = output<RegisterStep3Data>();

  readonly availableDays: DayOption[] = [
    { key: 'L', label: 'L' },
    { key: 'M', label: 'M' },
    { key: 'X', label: 'M' },
    { key: 'J', label: 'J' },
    { key: 'V', label: 'V' },
    { key: 'S', label: 'S' },
    { key: 'D', label: 'D' },
  ];

  readonly step3Form: FormGroup = this.fb.group({
    dailyOrderLimit: [12, [Validators.min(1)]],
    prepTime: ['30 - 60 min', Validators.required],
    openingTime: ['09:00', Validators.required],
    closingTime: ['19:00', Validators.required],
    operatingDays: [['L', 'M', 'X', 'J', 'V', 'S']],
    autoPauseOnLimit: [true],
  });

  ngOnInit(): void {
    const data = this.initialData();
    if (data) {
      this.step3Form.patchValue(data);
    }
  }

  readonly formChange = outputFromObservable<RegisterStep3Data>(this.step3Form.valueChanges);

  incrementLimit(): void {
    const current = this.step3Form.get('dailyOrderLimit')?.value ?? 0;
    this.step3Form.patchValue({ dailyOrderLimit: current + 1 });
  }

  decrementLimit(): void {
    const current = this.step3Form.get('dailyOrderLimit')?.value ?? 1;
    if (current > 1) {
      this.step3Form.patchValue({ dailyOrderLimit: current - 1 });
    }
  }

  setLimitPreset(value: number | null): void {
    this.step3Form.patchValue({ dailyOrderLimit: value });
  }

  isDaySelected(dayKey: string): boolean {
    const days: string[] = this.step3Form.get('operatingDays')?.value || [];
    return days.includes(dayKey);
  }

  toggleDay(dayKey: string): void {
    const currentDays: string[] = [...(this.step3Form.get('operatingDays')?.value || [])];
    const index = currentDays.indexOf(dayKey);

    if (index > -1) {
      if (currentDays.length > 1) {
        currentDays.splice(index, 1);
      }
    } else {
      currentDays.push(dayKey);
    }
    this.step3Form.patchValue({ operatingDays: currentDays });
  }

  toggleAutoPause(): void {
    const current = this.step3Form.get('autoPauseOnLimit')?.value;
    this.step3Form.patchValue({ autoPauseOnLimit: !current });
  }

  onBack(): void {
    this.back.emit();
  }

  handleContinue(): void {
    if (this.step3Form.invalid || this.isLoading()) {
      this.step3Form.markAllAsTouched();
      return;
    }
    this.step3Submit.emit(this.step3Form.getRawValue());
  }
}
