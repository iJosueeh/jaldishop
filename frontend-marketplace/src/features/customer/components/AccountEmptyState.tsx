import React from 'react';
import { cn } from '@/shared/utils/cn';

interface AccountEmptyStateProps {
  icon: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
  dataTestId?: string;
  className?: string;
}

/**
 * Shared empty state for mi-jaldishop views.
 * Single-surface container (no nested cards) with accessible status semantics.
 */
export function AccountEmptyState({
  icon,
  title,
  description,
  action,
  dataTestId,
  className,
}: AccountEmptyStateProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      data-testid={dataTestId}
      className={cn(
        'rounded-xl border border-dashed border-[#e7e0d6] bg-white p-8 sm:p-12 text-center',
        className
      )}
    >
      <div
        className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-[#f0fdfa] border border-[#ccfbf1] text-[#005141]"
        aria-hidden="true"
      >
        {icon}
      </div>
      <h2 className="font-display text-base sm:text-lg font-bold text-[#1c1917] tracking-tight">
        {title}
      </h2>
      {description && (
        <p className="mx-auto mt-1.5 max-w-md text-sm text-[#57534e] leading-relaxed">
          {description}
        </p>
      )}
      {action && (
        <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
          {action}
        </div>
      )}
    </div>
  );
}
