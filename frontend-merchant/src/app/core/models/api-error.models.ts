export interface ApiErrorMetadata {
    suggestedAction?: 'DEACTIVATE' | 'SUSPEND' | string;
    currentStatus?: string;
    blockingReason?: string;
}

export interface ApiError {
    code: string;
    message: string;
    errors?: Record<string, string>;
    metadata?: ApiErrorMetadata;
}

export interface NormalizedApiError {
    status: number;
    code: string;
    message: string;
    fieldErrors: Record<string, string>;
    metadata?: ApiErrorMetadata;
    timestamp: Date;
}

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
    id: string;
    type: ToastType;
    title?: string;
    message: string;
    durationMs?: number;
    dismissing?: boolean;
    action?: {
      label: string;
      run: () => void;
    };
}
