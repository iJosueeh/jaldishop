import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matErrorOutline,
  matCheckOutline,
} from '@ng-icons/material-symbols/outline';
import { CapacityConfiguration, CreateCapacityConfigRequest } from '../../../../core/models/capacity.models';
import { TimeRangePicker } from '../../../../shared/components/time-range-picker/time-range-picker';

@Component({
  imports: [ReactiveFormsModule, NgIcon, TimeRangePicker],
  providers: [
    provideIcons({
      matCloseOutline,
      matErrorOutline,
      matCheckOutline,
    }),
  ],
  selector: 'app-capacity-slot-modal',
  styleUrl: './capacity-slot-modal.css',
  templateUrl: './capacity-slot-modal.html',
})
export class CapacitySlotModal {
  private readonly fb = inject(FormBuilder);

  readonly isOpen = input<boolean>(false);
  readonly isSubmitting = input<boolean>(false);
  readonly dayLabel = input<string>('Lunes');
  readonly dayOfWeek = input<number>(1);
  readonly slotToEdit = input<CapacityConfiguration | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly closeModal = output<void>();
  readonly saveSlot = output<{ request: CreateCapacityConfigRequest; id?: string }>();

  readonly form: FormGroup = this.fb.group({
    startTime: ['09:00', [Validators.required]],
    endTime: ['14:00', [Validators.required]],
    maxCapacity: [10, [Validators.required, Validators.min(1)]],
  });

  constructor() {
    effect(() => {
      const slot = this.slotToEdit();
      if (slot) {
        this.form.patchValue({
          startTime: slot.startTime.substring(0, 5),
          endTime: slot.endTime.substring(0, 5),
          maxCapacity: slot.maxCapacity,
        });
      } else {
        this.form.reset({
          startTime: '09:00',
          endTime: '14:00',
          maxCapacity: 10,
        });
      }
      this.errorMessage.set(null);
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { startTime, endTime, maxCapacity } = this.form.value;
    if (startTime >= endTime) {
      this.errorMessage.set('La hora de inicio debe ser menor que la hora de fin.');
      return;
    }

    this.errorMessage.set(null);

    const payload: CreateCapacityConfigRequest = {
      dayOfWeek: this.dayOfWeek(),
      startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
      endTime: endTime.length === 5 ? `${endTime}:00` : endTime,
      maxCapacity: Number(maxCapacity),
    };

    this.saveSlot.emit({
      request: payload,
      id: this.slotToEdit()?.id,
    });
  }

  resetForm(): void {
    this.errorMessage.set(null);
    this.form.reset({
      startTime: '09:00',
      endTime: '14:00',
      maxCapacity: 10,
    });
  }
}
