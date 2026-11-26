import React from 'react';
import { cn } from '@/shared/utils/cn';

interface AccountMetricTileProps {
  label: string;
  value: React.ReactNode;
  icon?: React.ReactNode;
  hint?: string;
  valueClassName?: string;
  className?: string;
}

/**
 * Compact KPI tile for customer account summaries and analytics.
 * Enforces tabular-nums and single 1px hairline border elevation.
 */
export function AccountMetricTile({
  label,
  value,
  icon,
  hint,
  valueClassName,
  className,
}: AccountMetricTileProps) {
  return (
    <article
      className={cn(
        'rounded-xl bg-white border border-[#e7e0d6] p-4 sm:p-5 flex flex-col justify-between gap-2',
        className
      )}
    >
      <div className="flex items-center justify-between gap-2 text-[#57534e]">
        <span className="text-xs font-semibold">{label}</span>
        {icon && (
          <span className="shrink-0" aria-hidden="true">
            {icon}
          </span>
        )}
      </div>
      <div>
        <p className={cn('text-2xl font-bold text-[#1c1917] tabular-nums leading-tight', valueClassName)}>
          {value}
        </p>
        {hint && <p className="mt-1 text-xs text-[#78716c]">{hint}</p>}
      </div>
    </article>
  );
}
