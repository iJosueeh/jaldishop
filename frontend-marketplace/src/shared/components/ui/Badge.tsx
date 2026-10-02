import React from 'react';
import { cn } from '@/shared/utils/cn';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'outline' | 'capacity' | 'jade' | 'terracotta' | 'amber';
  size?: 'sm' | 'md' | 'lg';
}

export function Badge({
  className,
  variant = 'default',
  size = 'md',
  children,
  ...props
}: BadgeProps) {
  const baseStyles =
    'inline-flex items-center font-semibold rounded-full border transition-colors select-none';

  const variants = {
    default:
      'bg-[#f7f3ec] text-[#57534e] border-[#e7e0d6]',
    jade:
      'bg-[#f0fdfa] text-[#005141] border-[#ccfbf1]',
    terracotta:
      'bg-[#fff7ed] text-[#ea580c] border-[#fed7aa]',
    amber:
      'bg-[#fffbeb] text-[#92400e] border-[#fef3c7]',
    success:
      'bg-[#f0fdfa] text-[#005141] border-[#99f6e4]',
    warning:
      'bg-[#fffbeb] text-[#b45309] border-[#fde68a]',
    danger:
      'bg-[#fef2f2] text-[#b91c1c] border-[#fecaca]',
    info:
      'bg-[#f0f9ff] text-[#0369a1] border-[#bae6fd]',
    outline:
      'bg-transparent text-[#57534e] border-[#e7e0d6]',
    capacity:
      'bg-[#f0fdfa] text-[#005141] border-[#99f6e4] font-bold shadow-xs',
  };

  const sizes = {
    sm: 'text-[11px] px-2.5 py-0.5 gap-1',
    md: 'text-xs px-3 py-1 gap-1.5',
    lg: 'text-sm px-4 py-1.5 gap-2',
  };

  return (
    <span
      className={cn(baseStyles, variants[variant], sizes[size], className)}
      {...props}
    >
      {children}
    </span>
  );
}
