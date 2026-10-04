import { Component, inject, input, OnInit, output } from '@angular/core';
import { outputFromObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RegisterFooter } from '../../../../../shared/components/register-footer/register-footer';
import { RegisterStepHeader } from '../../../../../shared/components/register-step-header/register-step-header';
import { AlertError } from '../../../../../shared/components/alert-error/alert-error';
import { DayOption, RegisterStep4Data } from '../../../../../core/models/register.models';

@Component({
  selector: 'app-register-step4-form',
  standalone: true,
  imports: [ReactiveFormsModule, RegisterFooter, RegisterStepHeader, AlertError],
  styleUrl: './register-step4-form.css',
  templateUrl: './register-step4-form.html',
})
export class RegisterStep4Form implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);
  readonly initialData = input<RegisterStep4Data | null>(null);
  readonly isExistingUser = input<boolean>(false);

  readonly back = output<void>();
  readonly step4Submit = output<RegisterStep4Data>();

  readonly availableDays: DayOption[] = [
    { key: 'L', label: 'L' },
    { key: 'M', label: 'M' },
    { key: 'X', label: 'M' },
    { key: 'J', label: 'J' },
    { key: 'V', label: 'V' },
    { key: 'S', label: 'S' },
    { key: 'D', label: 'D' },
  ];

  readonly step4Form: FormGroup = this.fb.group({
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
      this.step4Form.patchValue(data);
    }
  }

  readonly formChange = outputFromObservable<RegisterStep4Data>(this.step4Form.valueChanges);

  incrementLimit(): void {
    const current = this.step4Form.get('dailyOrderLimit')?.value ?? 0;
    this.step4Form.patchValue({ dailyOrderLimit: current + 1 });
  }

  decrementLimit(): void {
    const current = this.step4Form.get('dailyOrderLimit')?.value ?? 1;
    if (current > 1) {
      this.step4Form.patchValue({ dailyOrderLimit: current - 1 });
    }
  }

  setPreset(limit: number | null): void {
    this.step4Form.patchValue({ dailyOrderLimit: limit });
  }

  isDaySelected(dayKey: string): boolean {
    const days: string[] = this.step4Form.get('operatingDays')?.value ?? [];
    return days.includes(dayKey);
  }

  toggleDay(dayKey: string): void {
    const currentDays: string[] = [...(this.step4Form.get('operatingDays')?.value ?? [])];
    const index = currentDays.indexOf(dayKey);

    if (index > -1) {
      if (currentDays.length > 1) {
        currentDays.splice(index, 1);
      }
    } else {
      currentDays.push(dayKey);
    }

    this.step4Form.patchValue({ operatingDays: currentDays });
  }

  onBack(): void {
    this.back.emit();
  }

  handleFinish(): void {
    if (this.step4Form.invalid || this.isLoading()) {
      this.step4Form.markAllAsTouched();
      return;
    }
    this.step4Submit.emit(this.step4Form.getRawValue());
  }
}
