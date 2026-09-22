export type CapacityStatus = 'ACTIVE' | 'INACTIVE';
export type CapacityTab = 'SCHEDULE' | 'EXCEPTIONS';

export interface CapacityConfiguration {
  id: string;
  storeId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  maxCapacity: number;
  status: CapacityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCapacityConfigRequest {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  maxCapacity: number;
}

export interface UpdateCapacityConfigRequest {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  maxCapacity: number;
}

export interface DayScheduleOption {
  dayOfWeek: number;
  label: string;
  shortLabel: string;
}

export const DAYS_OF_WEEK: DayScheduleOption[] = [
  { dayOfWeek: 1, label: 'Lunes', shortLabel: 'Lun' },
  { dayOfWeek: 2, label: 'Martes', shortLabel: 'Mar' },
  { dayOfWeek: 3, label: 'Miércoles', shortLabel: 'Mié' },
  { dayOfWeek: 4, label: 'Jueves', shortLabel: 'Jue' },
  { dayOfWeek: 5, label: 'Viernes', shortLabel: 'Vie' },
  { dayOfWeek: 6, label: 'Sábado', shortLabel: 'Sáb' },
  { dayOfWeek: 7, label: 'Domingo', shortLabel: 'Dom' },
];

export type CapacityExceptionStatus = 'ACTIVE' | 'INACTIVE';

export interface CapacityException {
  id: string;
  storeId: string;
  serviceDate: string;
  startTime: string | null;
  endTime: string | null;
  exceptionCapacity: number;
  reason?: string | null;
  status: CapacityExceptionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCapacityExceptionRequest {
  serviceDate: string;
  startTime?: string | null;
  endTime?: string | null;
  exceptionCapacity: number;
  reason?: string | null;
}

export interface UpdateCapacityExceptionRequest {
  serviceDate: string;
  startTime?: string | null;
  endTime?: string | null;
  exceptionCapacity: number;
  reason?: string | null;
}

