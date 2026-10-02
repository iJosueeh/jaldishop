import React, { forwardRef } from 'react';
import { cn } from '@/shared/utils/cn';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'subtle' | 'terracotta' | 'amber';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      children,
      disabled,
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'inline-flex items-center justify-center font-semibold rounded-2xl transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]/50 disabled:opacity-50 disabled:cursor-not-allowed select-none active:scale-[0.98] cursor-pointer';

    const variants = {
      // Primary Verde Jade JaldiShop
      primary:
        'bg-[#005141] hover:bg-[#003d31] text-white shadow-md shadow-[#005141]/15 hover:shadow-lg hover:shadow-[#005141]/25 hover:-translate-y-0.5',
      // Secondary Terracotta / Orange Acento
      terracotta:
        'bg-[#ea580c] hover:bg-[#c2410c] text-white shadow-md shadow-[#ea580c]/20 hover:shadow-lg hover:shadow-[#ea580c]/30 hover:-translate-y-0.5',
      // Amber Honey
      amber:
        'bg-[#feae2c] hover:bg-[#f59e0b] text-[#1c1917] shadow-md shadow-[#feae2c]/20 hover:-translate-y-0.5',
      secondary:
        'bg-[#1c1917] hover:bg-[#292524] text-white shadow-sm',
      outline:
        'border-2 border-[#e7e0d6] hover:border-[#a8a29e] bg-white/80 hover:bg-white text-[#1c1917] shadow-xs',
      ghost:
        'hover:bg-[#f7f3ec] text-[#57534e] hover:text-[#1c1917]',
      danger:
        'bg-rose-600 hover:bg-rose-500 text-white shadow-sm',
      subtle:
        'bg-[#f0fdfa] text-[#005141] hover:bg-[#ccfbf1] border border-[#ccfbf1]',
    };

    const sizes = {
      sm: 'text-xs px-3.5 py-2 gap-1.5 rounded-xl',
      md: 'text-sm px-5 py-2.5 gap-2',
      lg: 'text-base px-7 py-3.5 gap-2.5 rounded-2xl font-bold',
      icon: 'p-2.5 rounded-2xl aspect-square',
    };

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={cn(baseStyles, variants[variant], sizes[size], className)}
        {...props}
      >
        {isLoading ? (
          <Loader2 className="h-4 w-4 animate-spin text-current" />
        ) : (
          leftIcon
        )}
        {children}
        {!isLoading && rightIcon}
      </button>
    );
  }
);

Button.displayName = 'Button';
