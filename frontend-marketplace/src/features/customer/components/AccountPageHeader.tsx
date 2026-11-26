import React from 'react';
import { cn } from '@/shared/utils/cn';

interface AccountPageHeaderProps {
  title: string;
  description?: string;
  icon?: React.ReactNode;
  badge?: React.ReactNode;
  actions?: React.ReactNode;
  className?: string;
}

/**
 * Shared page header for all mi-jaldishop views.
 * Owns the single semantic <h1> per view and keeps a consistent border-bottom rhythm.
 */
export function AccountPageHeader({
  title,
  description,
  icon,
  badge,
  actions,
  className,
}: AccountPageHeaderProps) {
  return (
    <header
      className={cn(
        'flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-[#e7e0d6]',
        className
      )}
    >
      <div className="min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-2.5">
          {icon && (
            <span className="text-[#005141] shrink-0" aria-hidden="true">
              {icon}
            </span>
          )}
          <h1 className="font-display text-2xl sm:text-[28px] font-bold text-[#1c1917] tracking-tight leading-tight">
            {title}
          </h1>
          {badge}
        </div>
        {description && (
          <p className="text-sm text-[#57534e] leading-relaxed max-w-2xl">
            {description}
          </p>
        )}
      </div>

      {actions && (
        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          {actions}
        </div>
      )}
    </header>
  );
}
