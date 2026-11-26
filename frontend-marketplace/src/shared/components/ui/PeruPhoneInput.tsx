'use client';

import React from 'react';
import { toLocalPeruPhone } from '@/features/auth/utils/validation';
import { cn } from '@/shared/utils/cn';

export interface PeruPhoneInputProps {
  id: string;
  name?: string;
  value: string;
  onChange: (localPhone: string) => void;
  onBlur?: () => void;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  error?: string;
  hint?: string;
  errorId?: string;
  hintId?: string;
  ariaLabel?: string;
  autoComplete?: string;
  className?: string;
  inputClassName?: string;
}

export function PeruPhoneInput({
  id,
  name,
  value,
  onChange,
  onBlur,
  placeholder = '987654321',
  disabled = false,
  required = false,
  error,
  hint,
  errorId,
  hintId,
  ariaLabel,
  autoComplete = 'tel-national',
  className,
  inputClassName,
}: PeruPhoneInputProps) {
  const resolvedErrorId = error ? errorId || `${id}-error` : undefined;
  const resolvedHintId = !error && hint ? hintId || `${id}-hint` : undefined;
  const describedBy = resolvedErrorId || resolvedHintId;
  const displayValue = toLocalPeruPhone(value);

  return (
    <div className={cn('w-full', className)}>
      <div
        className={cn(
          'flex min-h-[44px] w-full overflow-hidden rounded-xl border bg-white transition-colors duration-150 ease-out motion-reduce:transition-none',
          error
            ? 'border-rose-400 focus-within:border-rose-500 focus-within:ring-2 focus-within:ring-rose-500/20'
            : 'border-[#e7e0d6] focus-within:border-[#005141] focus-within:ring-2 focus-within:ring-[#005141]/15',
          disabled && 'opacity-60 bg-stone-100 cursor-not-allowed'
        )}
      >
        <div
          aria-hidden="true"
          className="flex shrink-0 select-none items-center gap-2 border-r border-[#e7e0d6] bg-[#faf7f2] px-3 text-xs font-bold text-[#1c1917]"
        >
          <svg
            width="18"
            height="12"
            viewBox="0 0 18 12"
            className="h-3 w-[18px] shrink-0 rounded-[2px] border border-stone-300/70"
          >
            <rect width="6" height="12" x="0" y="0" fill="#D91023" />
            <rect width="6" height="12" x="6" y="0" fill="#FFFFFF" />
            <rect width="6" height="12" x="12" y="0" fill="#D91023" />
          </svg>
          <span className="tabular-nums">+51</span>
        </div>

        <input
          id={id}
          name={name}
          type="tel"
          inputMode="numeric"
          autoComplete={autoComplete}
          value={displayValue}
          onChange={(e) => onChange(toLocalPeruPhone(e.target.value))}
          onBlur={onBlur}
          disabled={disabled}
          required={required}
          maxLength={14}
          placeholder={placeholder}
          aria-label={ariaLabel}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          className={cn(
            'min-h-[44px] w-full bg-transparent px-3.5 py-2 text-sm text-[#1c1917] placeholder-[#78716c] tabular-nums focus:outline-none disabled:cursor-not-allowed',
            inputClassName
          )}
        />
      </div>

      {error ? (
        <p id={resolvedErrorId} role="alert" className="mt-1 text-xs font-medium text-rose-700">
          {error}
        </p>
      ) : hint ? (
        <p id={resolvedHintId} className="mt-1.5 text-xs text-[#57534e]">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
