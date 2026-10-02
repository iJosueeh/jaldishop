import React from 'react';
import { cn } from '@/shared/utils/cn';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'warm' | 'interactive' | 'outline' | 'glass';
}

export function Card({
  className,
  variant = 'default',
  children,
  ...props
}: CardProps) {
  const baseStyles = 'rounded-3xl transition-all duration-300';

  const variants = {
    default:
      'bg-white border border-[#e7e0d6] shadow-sm shadow-[#1c1917]/5',
    warm:
      'bg-[#f7f3ec] border border-[#e7e0d6]/80 shadow-xs',
    interactive:
      'bg-white border border-[#e7e0d6] shadow-sm hover:shadow-xl hover:shadow-[#1c1917]/8 hover:border-[#a8a29e] hover:-translate-y-1 cursor-pointer',
    outline:
      'bg-transparent border-2 border-[#e7e0d6]',
    glass:
      'bg-white/80 backdrop-blur-xl border border-white/60 shadow-xl shadow-[#1c1917]/5',
  };

  return (
    <div className={cn(baseStyles, variants[variant], className)} {...props}>
      {children}
    </div>
  );
}
