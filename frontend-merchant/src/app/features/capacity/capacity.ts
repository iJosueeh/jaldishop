import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CapacityService } from '../../core/services/capacity.service';
import { ToastService } from '../../core/services/toast.service';
import {
  CapacityConfiguration,
  CapacityException,
  CapacityTab,
  CreateCapacityConfigRequest,
  CreateCapacityExceptionRequest,
  DAYS_OF_WEEK,
  DayScheduleOption,
} from '../../core/models/capacity.models';
import { CapacitySlotsList } from './components/capacity-slots-list/capacity-slots-list';
import { CapacityDaySelector } from './components/capacity-day-selector/capacity-day-selector';
import { CapacityKpis } from './components/capacity-kpis/capacity-kpis';
import { CapacityHeader } from './components/capacity-header/capacity-header';
import { CapacitySlotModal } from './components/capacity-slot-modal/capacity-slot-modal';
import { CapacityExceptionsList } from './components/capacity-exceptions-list/capacity-exceptions-list';
import { CapacityExceptionModal } from './components/capacity-exception-modal/capacity-exception-modal';
import { CapacityTabSelector } from './components/capacity-tab-selector/capacity-tab-selector';

@Component({
  imports: [
    CapacityHeader,
    CapacityTabSelector,
    CapacityKpis,
    CapacityDaySelector,
    CapacitySlotsList,
    CapacitySlotModal,
    CapacityExceptionsList,
    CapacityExceptionModal,
  ],
  selector: 'app-capacity',
  styleUrl: './capacity.css',
  templateUrl: './capacity.html',
})
export class Capacity implements OnInit {
  private readonly capacityService = inject(CapacityService);
  private readonly toastService = inject(ToastService);

  readonly daysOfWeek = DAYS_OF_WEEK;
  readonly activeTab = signal<CapacityTab>('SCHEDULE');
  readonly selectedDay = signal<number>(this.getTodayDayOfWeek());

  // Estados de modales
  readonly isModalOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly selectedSlotToEdit = signal<CapacityConfiguration | null>(null);

  readonly isExceptionModalOpen = signal<boolean>(false);
  readonly isSubmittingException = signal<boolean>(false);
  readonly selectedExceptionToEdit = signal<CapacityException | null>(null);

  // Señales de Configuración Semanal
  readonly isLoading = computed(() => this.capacityService.isLoading());
  readonly allConfigurations = computed(() => this.capacityService.configurations());

  // Señales de Excepciones
  readonly isLoadingExceptions = computed(() => this.capacityService.isLoadingExceptions());
  readonly allExceptions = computed(() => this.capacityService.exceptions());

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
    this.loadConfigurations();
  }

  setActiveTab(tab: CapacityTab): void {
    this.activeTab.set(tab);
    if (tab === 'EXCEPTIONS') {
      this.loadExceptions();
    }
  }

  loadConfigurations(): void {
    this.capacityService.getConfigurations().subscribe({
      error: (err) => {
        this.toastService.error(
          err?.error?.message || 'Error al cargar configuraciones de capacidad.',
        );
      },
    });
  }

  loadExceptions(): void {
    this.capacityService.getExceptions().subscribe({
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Error al cargar excepciones de capacidad.');
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

  // Métodos de Modal Semanal
  openCreateModal(): void {
    this.selectedSlotToEdit.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(slot: CapacityConfiguration): void {
    this.selectedSlotToEdit.set(slot);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.selectedSlotToEdit.set(null);
  }

  onSaveSlot(event: { request: CreateCapacityConfigRequest; id?: string }): void {
    this.isSubmitting.set(true);
    const action$ = event.id
      ? this.capacityService.updateConfiguration(event.id, event.request)
      : this.capacityService.createConfiguration(event.request);

    const successMsg = event.id
      ? '¡Franja de capacidad actualizada con éxito!'
      : '¡Franja de capacidad creada con éxito!';

    action$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeModal();
        this.toastService.success(successMsg);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(
          err?.error?.message ||
            'Error al guardar la configuración. Verifica que no se solape con otra franja.',
        );
      },
    });
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

  // Métodos de Excepciones
  openCreateExceptionModal(): void {
    this.selectedExceptionToEdit.set(null);
    this.isExceptionModalOpen.set(true);
  }

  openEditExceptionModal(exc: CapacityException): void {
    this.selectedExceptionToEdit.set(exc);
    this.isExceptionModalOpen.set(true);
  }

  closeExceptionModal(): void {
    this.isExceptionModalOpen.set(false);
    this.selectedExceptionToEdit.set(null);
  }

  onSaveException(event: { request: CreateCapacityExceptionRequest; id?: string }): void {
    this.isSubmittingException.set(true);
    const action$ = event.id
      ? this.capacityService.updateException(event.id, event.request)
      : this.capacityService.createException(event.request);

    const successMsg = event.id
      ? '¡Fecha especial / excepción actualizada con éxito!'
      : '¡Fecha especial / excepción creada con éxito!';

    action$.subscribe({
      next: () => {
        this.isSubmittingException.set(false);
        this.closeExceptionModal();
        this.toastService.success(successMsg);
      },
      error: (err) => {
        this.isSubmittingException.set(false);
        this.toastService.error(
          err?.error?.message || 'Error al guardar la excepción de capacidad.',
        );
      },
    });
  }

  onToggleExceptionStatus(exc: CapacityException): void {
    const isActivating = exc.status !== 'ACTIVE';
    const action$ = isActivating
      ? this.capacityService.activateException(exc.id)
      : this.capacityService.deactivateException(exc.id);

    const message = isActivating ? 'Excepción activada.' : 'Excepción pausada.';

    action$.subscribe({
      next: () => this.toastService.success(message),
      error: (err) =>
        this.toastService.error(err?.error?.message || 'Error al cambiar estado de la excepción.'),
    });
  }

  private getTodayDayOfWeek(): number {
    const day = new Date().getDay();
    return day === 0 ? 7 : day;
  }
}
