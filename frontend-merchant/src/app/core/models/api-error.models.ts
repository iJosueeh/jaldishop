export interface ApiError {
    code: string;
    message: string;
    errors?: Record<string, string>;
}

export interface NormalizedApiError {
    status: number;
    code: string;
    message: string;
    fieldErrors: Record<string, string>;
    timestamp: Date;
}

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
    id: string;
    type: ToastType;
    title?: string;
    message: string;
    durationMs?: number;
}