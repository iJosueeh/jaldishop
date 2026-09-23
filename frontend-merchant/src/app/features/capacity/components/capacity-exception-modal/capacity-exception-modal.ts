import { Component, effect, inject, input, signal, output } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matCalendarTodayOutline,
  matErrorOutline,
  matCheckOutline,
} from '@ng-icons/material-symbols/outline';
import {
  CapacityException,
  CreateCapacityExceptionRequest,
  UpdateCapacityExceptionRequest,
} from '../../../../core/models/capacity.models';
import { TimeRangePicker } from '../../../../shared/components/time-range-picker/time-range-picker';

@Component({
  imports: [ReactiveFormsModule, NgIcon, TimeRangePicker],
  providers: [
    provideIcons({
      matCloseOutline,
      matCalendarTodayOutline,
      matErrorOutline,
      matCheckOutline,
    }),
  ],
  selector: 'app-capacity-exception-modal',
  styleUrl: './capacity-exception-modal.css',
  templateUrl: './capacity-exception-modal.html',
})
export class CapacityExceptionModal {
  private readonly fb = inject(FormBuilder);

  readonly isOpen = input<boolean>(false);
  readonly isSubmitting = input<boolean>(false);
  readonly exceptionToEdit = input<CapacityException | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly closeModal = output<void>();
  readonly saveException = output<{
    request: CreateCapacityExceptionRequest | UpdateCapacityExceptionRequest;
    id?: string;
  }>();

  readonly minDate: string = this.getTodayDateString();

  readonly form: FormGroup = this.fb.group({
    serviceDate: [this.minDate, [Validators.required]],
    isAllDay: [true],
    startTime: ['09:00'],
    endTime: ['14:00'],
    exceptionCapacity: [0, [Validators.required, Validators.min(0)]],
    reason: [''],
  });

  constructor() {
    effect(() => {
      const exc = this.exceptionToEdit();
      if (exc) {
        const isAllDay = !exc.startTime && !exc.endTime;
        this.form.patchValue({
          serviceDate: exc.serviceDate,
          isAllDay,
          startTime: exc.startTime ? exc.startTime.substring(0, 5) : '09:00',
          endTime: exc.endTime ? exc.endTime.substring(0, 5) : '14:00',
          exceptionCapacity: exc.exceptionCapacity,
          reason: exc.reason || '',
        });
      } else {
        this.form.reset({
          serviceDate: this.minDate,
          isAllDay: true,
          startTime: '09:00',
          endTime: '14:00',
          exceptionCapacity: 0,
          reason: '',
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

    const { serviceDate, isAllDay, startTime, endTime, exceptionCapacity, reason } =
      this.form.value;

    if (!isAllDay && startTime >= endTime) {
      this.errorMessage.set('La hora de inicio debe ser menor que la hora de fin.');
      return;
    }

    this.errorMessage.set(null);

    const payload: CreateCapacityExceptionRequest = {
      serviceDate,
      startTime: isAllDay ? null : startTime.length === 5 ? `${startTime}:00` : startTime,
      endTime: isAllDay ? null : endTime.length === 5 ? `${endTime}:00` : endTime,
      exceptionCapacity: Number(exceptionCapacity),
      reason: reason ? reason.trim() : null,
    };

    this.saveException.emit({
      request: payload,
      id: this.exceptionToEdit()?.id,
    });
  }

  resetForm(): void {
    this.errorMessage.set(null);
    this.form.reset({
      serviceDate: this.minDate,
      isAllDay: true,
      startTime: '09:00',
      endTime: '14:00',
      exceptionCapacity: 0,
      reason: '',
    });
  }

  private getTodayDateString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
