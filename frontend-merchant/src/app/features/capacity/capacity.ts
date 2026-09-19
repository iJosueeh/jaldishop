import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matErrorOutline,
  matCheckOutline,
} from '@ng-icons/material-symbols/outline';
import { CapacityService } from '../../core/services/capacity.service';
import { ToastService } from '../../core/services/toast.service';
import {
  CapacityConfiguration,
  CreateCapacityConfigRequest,
  DAYS_OF_WEEK,
  DayScheduleOption,
} from '../../core/models/capacity.models';
import { CapacitySlotsList } from './components/capacity-slots-list/capacity-slots-list';
import { CapacityDaySelector } from './components/capacity-day-selector/capacity-day-selector';
import { CapacityKpis } from './components/capacity-kpis/capacity-kpis';
import { CapacityHeader } from './components/capacity-header/capacity-header';

@Component({
  imports: [
    ReactiveFormsModule,
    NgIcon,
    CapacityHeader,
    CapacityKpis,
    CapacityDaySelector,
    CapacitySlotsList,
  ],
  providers: [provideIcons({ matCloseOutline, matErrorOutline, matCheckOutline })],
  selector: 'app-capacity',
  styleUrl: './capacity.css',
  templateUrl: './capacity.html',
})
export class Capacity implements OnInit {
  private readonly capacityService = inject(CapacityService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly daysOfWeek = DAYS_OF_WEEK;
  readonly selectedDay = signal<number>(this.getTodayDayOfWeek());
  readonly isModalOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form: FormGroup = this.fb.group({
    startTime: ['09:00', [Validators.required]],
    endTime: ['14:00', [Validators.required]],
    maxCapacity: [10, [Validators.required, Validators.min(1)]],
  });

  readonly isLoading = computed(() => this.capacityService.isLoading());
  readonly allConfigurations = computed(() => this.capacityService.configurations());

  readonly selectedDayConfigurations = computed(() => {
    const day = this.selectedDay();
    return this.allConfigurations()
      .filter((c) => c.dayOfWeek === day)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  readonly selectedDayTotalCapacity = computed(() => {
    return this.selectedDayConfigurations()
      .filter((c) => c.status === 'ACTIVE')
      .reduce((acc, c) => acc + c.maxCapacity, 0);
  });

  readonly selectedDayInfo = computed<DayScheduleOption>(() => {
    return this.daysOfWeek.find((d) => d.dayOfWeek === this.selectedDay()) || this.daysOfWeek[0];
  });

  ngOnInit(): void {
    this.capacityService.getConfigurations().subscribe({
      error: (err) => {
        this.toastService.error(
          err?.error?.message || 'Error al cargar configuraciones de capacidad.',
        );
      },
    });
  }

  selectDay(dayOfWeek: number): void {
    this.selectedDay.set(dayOfWeek);
  }

  getActiveSlotsCount(dayOfWeek: number): number {
    return this.allConfigurations().filter(
      (c) => c.dayOfWeek === dayOfWeek && c.status === 'ACTIVE',
    ).length;
  }

  openCreateModal(): void {
    this.errorMessage.set(null);
    this.form.reset({
      startTime: '09:00',
      endTime: '14:00',
      maxCapacity: 10,
    });
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.errorMessage.set(null);
  }

  onToggleStatus(config: CapacityConfiguration): void {
    const isActivating = config.status !== 'ACTIVE';
    const action$ = isActivating
      ? this.capacityService.activateConfiguration(config.id)
      : this.capacityService.deactivateConfiguration(config.id);

    const message = isActivating ? 'Franja horaria activada.' : 'Franja horaria pausada.';

    action$.subscribe({
      next: () => this.toastService.success(message),
      error: (err) =>
        this.toastService.error(
          err?.error?.message || 'Error al cambiar estado de la franja horaria.',
        ),
    });
  }

  onSubmitForm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { startTime, endTime, maxCapacity } = this.form.value;
    if (!this.isValidTimeRange(startTime, endTime)) {
      this.errorMessage.set('La hora de inicio debe ser menor que la hora de fin.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const payload = this.buildPayload(startTime, endTime, maxCapacity);
    this.capacityService.createConfiguration(payload).subscribe({
      next: () => this.handleCreateSuccess(),
      error: (err) => this.handleCreateError(err),
    });
  }

  private isValidTimeRange(startTime: string, endTime: string): boolean {
    return startTime < endTime;
  }

  private buildPayload(
    startTime: string,
    endTime: string,
    maxCapacity: number,
  ): CreateCapacityConfigRequest {
    return {
      dayOfWeek: this.selectedDay(),
      startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
      endTime: endTime.length === 5 ? `${endTime}:00` : endTime,
      maxCapacity: Number(maxCapacity),
    };
  }

  private handleCreateSuccess(): void {
    this.isSubmitting.set(false);
    this.closeModal();
    this.toastService.success('¡Franja de capacidad creada con éxito!');
  }

  private handleCreateError(err: any): void {
    this.isSubmitting.set(false);
    this.errorMessage.set(
      err?.error?.message ||
        'Error al crear la configuración. Verifica que no se solape con otra franja.',
    );
  }

  private getTodayDayOfWeek(): number {
    const day = new Date().getDay();
    return day === 0 ? 7 : day;
  }
}
