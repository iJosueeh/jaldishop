import React from 'react';
import { Store, Inbox } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
  className?: string;
  variant?: 'default' | 'card' | 'minimal';
}

/**
 * Universal Empty State component (equivalent to @empty block in Angular 17+ / Clean UI)
 * Provides consistent, accessible, and user-friendly fallback messaging when datasets are empty.
 */
export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
  variant = 'default',
}: EmptyStateProps) {
  const isCard = variant === 'card';
  const isMinimal = variant === 'minimal';

  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'flex flex-col items-center justify-center text-center select-none',
        isCard && 'p-8 sm:p-12 rounded-3xl bg-white/70 dark:bg-stone-900/60 border border-stone-200/80 dark:border-white/10 shadow-sm backdrop-blur-sm',
        !isCard && !isMinimal && 'py-12 sm:py-16 px-4',
        isMinimal && 'py-6 px-3',
        className
      )}
    >
      {/* Icon / Illustration Container */}
      <div className={cn(
        'rounded-2xl flex items-center justify-center mb-4 transition-transform hover:scale-105',
        isMinimal ? 'w-10 h-10 bg-stone-100 dark:bg-white/10 text-stone-500' : 'w-14 h-14 bg-[#005141]/10 dark:bg-emerald-500/15 text-[#005141] dark:text-emerald-400 border border-[#005141]/15'
      )}>
        {icon || (isMinimal ? <Inbox className="w-5 h-5" /> : <Store className="w-6 h-6" />)}
      </div>

      {/* Title */}
      <h3 className={cn(
        'font-bold text-stone-800 dark:text-stone-100',
        isMinimal ? 'text-sm' : 'text-base sm:text-lg'
      )}>
        {title}
      </h3>

      {/* Description */}
      {description && (
        <p className={cn(
          'text-stone-500 dark:text-stone-400 max-w-sm mt-1.5 leading-relaxed font-normal',
          isMinimal ? 'text-xs' : 'text-xs sm:text-sm'
        )}>
          {description}
        </p>
      )}

      {/* Action Slot */}
      {action && (
        <div className="mt-5 flex items-center justify-center gap-3">
          {action}
        </div>
      )}
    </div>
  );
}
