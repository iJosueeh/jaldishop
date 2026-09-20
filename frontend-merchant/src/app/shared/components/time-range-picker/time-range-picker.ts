import { Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matBoltOutline,
  matScheduleOutline,
  matTimerOutline,
} from '@ng-icons/material-symbols/outline';
import { switchMap } from 'rxjs';

export interface TimeRangePreset {
  label: string;
  start: string;
  end: string;
}

@Component({
  imports: [ReactiveFormsModule, NgIcon],
  providers: [
    provideIcons({
      matBoltOutline,
      matScheduleOutline,
      matTimerOutline,
    }),
  ],
  selector: 'app-time-range-picker',
  styleUrl: './time-range-picker.css',
  templateUrl: './time-range-picker.html',
})
export class TimeRangePicker {
  readonly form = input.required<FormGroup>();
  readonly startControlName = input<string>('startTime');
  readonly endControlName = input<string>('endTime');
  readonly labelStart = input<string>('Hora de Inicio');
  readonly labelEnd = input<string>('Hora de Fin');
  readonly showPresets = input<boolean>(true);
  readonly showDuration = input<boolean>(true);

  readonly presets = input<TimeRangePreset[]>([
    { label: '🌅 08:00 - 12:00', start: '08:00', end: '12:00' },
    { label: '☀️ 12:00 - 18:00', start: '12:00', end: '18:00' },
    { label: '🌙 18:00 - 22:00', start: '18:00', end: '22:00' },
  ]);

  private readonly form$ = toObservable(this.form);
  private readonly formValues = toSignal(this.form$.pipe(switchMap((f) => f.valueChanges)), {
    initialValue: null,
  });

  readonly durationText = computed(() => {
    const val = this.formValues() || this.form().getRawValue();
    const start = val?.[this.startControlName()];
    const end = val?.[this.endControlName()];
    return this.calculateDuration(start, end);
  });

  applyPreset(preset: TimeRangePreset): void {
    const targetForm = this.form();
    targetForm.patchValue({
      [this.startControlName()]: preset.start,
      [this.endControlName()]: preset.end,
    });
    targetForm.get(this.startControlName())?.markAsDirty();
    targetForm.get(this.endControlName())?.markAsDirty();
  }

  private calculateDuration(start?: string, end?: string): string | null {
    if (!start || !end) return null;
    const [sH, sM] = String(start).split(':').map(Number);
    const [eH, eM] = String(end).split(':').map(Number);
    if (isNaN(sH) || isNaN(sM) || isNaN(eH) || isNaN(eM)) return null;

    const diff = eH * 60 + eM - (sH * 60 + sM);
    if (diff <= 0) return 'Horario inválido (Fin debe ser posterior a Inicio)';

    const hours = Math.floor(diff / 60);
    const mins = diff % 60;
    if (mins === 0) return `${hours} ${hours === 1 ? 'hora' : 'horas'}`;
    if (hours === 0) return `${mins} min`;
    return `${hours}h ${mins}min`;
  }
}
