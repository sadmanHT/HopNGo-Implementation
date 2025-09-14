'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/utils';

// Focus trap hook for modals and dialogs
export function useFocusTrap(isActive: boolean) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isActive || !containerRef.current) return;

    const container = containerRef.current;
    const focusableElements = container.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    const firstElement = focusableElements[0] as HTMLElement;
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          lastElement?.focus();
          e.preventDefault();
        }
      } else {
        if (document.activeElement === lastElement) {
          firstElement?.focus();
          e.preventDefault();
        }
      }
    };

    const handleEscapeKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        const closeButton = container.querySelector('[data-close-modal]') as HTMLElement;
        closeButton?.click();
      }
    };

    document.addEventListener('keydown', handleTabKey);
    document.addEventListener('keydown', handleEscapeKey);
    firstElement?.focus();

    return () => {
      document.removeEventListener('keydown', handleTabKey);
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [isActive]);

  return containerRef;
}

// Accessible Modal Component
interface AccessibleModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
}

export function AccessibleModal({
  isOpen,
  onClose,
  title,
  description,
  children,
  className,
  size = 'md',
}: AccessibleModalProps) {
  const focusTrapRef = useFocusTrap(isOpen);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!mounted) return null;

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            className="absolute inset-0 bg-black/50 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            aria-hidden="true"
          />
          
          {/* Modal */}
          <motion.div
            ref={focusTrapRef}
            className={cn(
              'relative bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full',
              sizeClasses[size],
              className
            )}
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ type: 'spring', stiffness: 300, damping: 30 }}
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
            aria-describedby={description ? "modal-description" : undefined}
          >
            {/* Header */}
            <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
              <div>
                <h2 id="modal-title" className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                  {title}
                </h2>
                {description && (
                  <p id="modal-description" className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                    {description}
                  </p>
                )}
              </div>
              <button
                onClick={onClose}
                data-close-modal
                className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
                aria-label="Close modal"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            {/* Content */}
            <div className="p-6">
              {children}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

// Accessible Button with proper focus states
interface AccessibleButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'destructive';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  loadingText?: string;
  children: React.ReactNode;
}

export function AccessibleButton({
  variant = 'primary',
  size = 'md',
  loading = false,
  loadingText = 'Loading...',
  className,
  children,
  disabled,
  ...props
}: AccessibleButtonProps) {
  const baseClasses = `
    relative inline-flex items-center justify-center font-medium rounded-lg
    transition-all duration-200 ease-in-out
    focus:outline-none focus:ring-2 focus:ring-offset-2
    disabled:pointer-events-none disabled:opacity-50
    active:scale-95
  `;
  
  const variantClasses = {
    primary: `
      bg-blue-600 text-white hover:bg-blue-700 
      focus:ring-blue-500 focus:ring-offset-white
      dark:focus:ring-offset-gray-900
    `,
    secondary: `
      bg-gray-200 text-gray-900 hover:bg-gray-300
      focus:ring-gray-500 focus:ring-offset-white
      dark:bg-gray-700 dark:text-gray-100 dark:hover:bg-gray-600
      dark:focus:ring-offset-gray-900
    `,
    ghost: `
      text-gray-700 hover:bg-gray-100 hover:text-gray-900
      focus:ring-gray-500 focus:ring-offset-white
      dark:text-gray-300 dark:hover:bg-gray-800 dark:hover:text-gray-100
      dark:focus:ring-offset-gray-900
    `,
    destructive: `
      bg-red-600 text-white hover:bg-red-700
      focus:ring-red-500 focus:ring-offset-white
      dark:focus:ring-offset-gray-900
    `,
  };
  
  const sizeClasses = {
    sm: 'h-8 px-3 text-sm',
    md: 'h-10 px-4 text-sm',
    lg: 'h-12 px-6 text-base',
  };

  return (
    <button
      className={cn(
        baseClasses,
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      disabled={disabled || loading}
      aria-disabled={disabled || loading}
      aria-describedby={loading ? 'loading-description' : undefined}
      {...props}
    >
      {loading ? (
        <>
          <svg
            className="w-4 h-4 mr-2 animate-spin"
            fill="none"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          <span>{loadingText}</span>
          <span id="loading-description" className="sr-only">
            Please wait while the action is being processed
          </span>
        </>
      ) : (
        children
      )}
    </button>
  );
}

// Accessible Form Input with proper labeling
interface AccessibleInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  helperText?: string;
  required?: boolean;
}

export function AccessibleInput({
  label,
  error,
  helperText,
  required,
  className,
  id,
  ...props
}: AccessibleInputProps) {
  const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  return (
    <div className="space-y-2">
      <label
        htmlFor={inputId}
        className="block text-sm font-medium text-gray-700 dark:text-gray-300"
      >
        {label}
        {required && (
          <span className="text-red-500 ml-1" aria-label="required">
            *
          </span>
        )}
      </label>
      
      <input
        id={inputId}
        className={cn(
          `
            block w-full px-3 py-2 border rounded-lg shadow-sm
            transition-colors duration-200
            focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
            disabled:bg-gray-50 disabled:text-gray-500 disabled:cursor-not-allowed
            dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100
            dark:focus:ring-blue-400 dark:focus:border-blue-400
          `,
          error
            ? 'border-red-300 focus:ring-red-500 focus:border-red-500 dark:border-red-600'
            : 'border-gray-300 dark:border-gray-600',
          className
        )}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={cn(
          error && errorId,
          helperText && helperId
        )}
        aria-required={required}
        {...props}
      />
      
      {error && (
        <p id={errorId} className="text-sm text-red-600 dark:text-red-400" role="alert">
          {error}
        </p>
      )}
      
      {helperText && !error && (
        <p id={helperId} className="text-sm text-gray-600 dark:text-gray-400">
          {helperText}
        </p>
      )}
    </div>
  );
}

// Accessible Navigation with keyboard support
interface AccessibleNavProps {
  items: Array<{
    label: string;
    href: string;
    current?: boolean;
    icon?: React.ReactNode;
  }>;
  className?: string;
  orientation?: 'horizontal' | 'vertical';
}

export function AccessibleNav({ items, className, orientation = 'horizontal' }: AccessibleNavProps) {
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const navRef = useRef<HTMLElement>(null);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    const { key } = e;
    const isHorizontal = orientation === 'horizontal';
    const nextKey = isHorizontal ? 'ArrowRight' : 'ArrowDown';
    const prevKey = isHorizontal ? 'ArrowLeft' : 'ArrowUp';

    if (key === nextKey) {
      e.preventDefault();
      setFocusedIndex((prev) => (prev + 1) % items.length);
    } else if (key === prevKey) {
      e.preventDefault();
      setFocusedIndex((prev) => (prev - 1 + items.length) % items.length);
    } else if (key === 'Home') {
      e.preventDefault();
      setFocusedIndex(0);
    } else if (key === 'End') {
      e.preventDefault();
      setFocusedIndex(items.length - 1);
    }
  }, [items.length, orientation]);

  useEffect(() => {
    if (focusedIndex >= 0 && navRef.current) {
      const links = navRef.current.querySelectorAll('a');
      links[focusedIndex]?.focus();
    }
  }, [focusedIndex]);

  return (
    <nav
      ref={navRef}
      className={cn(
        'flex',
        orientation === 'horizontal' ? 'space-x-1' : 'flex-col space-y-1',
        className
      )}
      role="navigation"
      aria-label="Main navigation"
      onKeyDown={handleKeyDown}
    >
      {items.map((item, index) => (
        <a
          key={item.href}
          href={item.href}
          className={cn(
            `
              flex items-center px-3 py-2 rounded-lg text-sm font-medium
              transition-colors duration-200
              focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
              dark:focus:ring-offset-gray-900
            `,
            item.current
              ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-100'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50 dark:text-gray-300 dark:hover:text-gray-100 dark:hover:bg-gray-800'
          )}
          aria-current={item.current ? 'page' : undefined}
          onFocus={() => setFocusedIndex(index)}
        >
          {item.icon && (
            <span className="mr-2" aria-hidden="true">
              {item.icon}
            </span>
          )}
          {item.label}
        </a>
      ))}
    </nav>
  );
}

// Skip to content link
export function SkipToContent() {
  return (
    <a
      href="#main-content"
      className="
        sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4
        bg-blue-600 text-white px-4 py-2 rounded-lg z-50
        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
      "
    >
      Skip to main content
    </a>
  );
}

// Screen reader only text
export function ScreenReaderOnly({ children }: { children: React.ReactNode }) {
  return <span className="sr-only">{children}</span>;
}

// Accessible tooltip
interface AccessibleTooltipProps {
  content: string;
  children: React.ReactNode;
  placement?: 'top' | 'bottom' | 'left' | 'right';
}

export function AccessibleTooltip({ content, children, placement = 'top' }: AccessibleTooltipProps) {
  const [isVisible, setIsVisible] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const tooltipId = `tooltip-${Math.random().toString(36).substr(2, 9)}`;

  const placementClasses = {
    top: 'bottom-full left-1/2 transform -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 transform -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 transform -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 transform -translate-y-1/2 ml-2',
  };

  return (
    <div className="relative inline-block">
      <div
        onMouseEnter={() => setIsVisible(true)}
        onMouseLeave={() => setIsVisible(false)}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        aria-describedby={tooltipId}
      >
        {children}
      </div>
      
      <AnimatePresence>
        {(isVisible || isFocused) && (
          <motion.div
            id={tooltipId}
            role="tooltip"
            className={cn(
              'absolute z-50 px-2 py-1 text-sm text-white bg-gray-900 rounded shadow-lg whitespace-nowrap',
              placementClasses[placement]
            )}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.15 }}
          >
            {content}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// Accessible alert/notification
interface AccessibleAlertProps {
  type: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  children: React.ReactNode;
  onClose?: () => void;
  className?: string;
}

export function AccessibleAlert({
  type,
  title,
  children,
  onClose,
  className,
}: AccessibleAlertProps) {
  const typeConfig = {
    info: {
      bgColor: 'bg-blue-50 dark:bg-blue-900/20',
      borderColor: 'border-blue-200 dark:border-blue-800',
      textColor: 'text-blue-800 dark:text-blue-200',
      iconColor: 'text-blue-400',
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
        </svg>
      ),
    },
    success: {
      bgColor: 'bg-green-50 dark:bg-green-900/20',
      borderColor: 'border-green-200 dark:border-green-800',
      textColor: 'text-green-800 dark:text-green-200',
      iconColor: 'text-green-400',
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
        </svg>
      ),
    },
    warning: {
      bgColor: 'bg-yellow-50 dark:bg-yellow-900/20',
      borderColor: 'border-yellow-200 dark:border-yellow-800',
      textColor: 'text-yellow-800 dark:text-yellow-200',
      iconColor: 'text-yellow-400',
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
      ),
    },
    error: {
      bgColor: 'bg-red-50 dark:bg-red-900/20',
      borderColor: 'border-red-200 dark:border-red-800',
      textColor: 'text-red-800 dark:text-red-200',
      iconColor: 'text-red-400',
      icon: (
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
        </svg>
      ),
    },
  };

  const config = typeConfig[type];

  return (
    <div
      className={cn(
        'rounded-lg border p-4',
        config.bgColor,
        config.borderColor,
        className
      )}
      role={type === 'error' ? 'alert' : 'status'}
      aria-live={type === 'error' ? 'assertive' : 'polite'}
    >
      <div className="flex">
        <div className={cn('flex-shrink-0', config.iconColor)}>
          {config.icon}
        </div>
        <div className="ml-3 flex-1">
          {title && (
            <h3 className={cn('text-sm font-medium', config.textColor)}>
              {title}
            </h3>
          )}
          <div className={cn('text-sm', config.textColor, title && 'mt-2')}>
            {children}
          </div>
        </div>
        {onClose && (
          <div className="ml-auto pl-3">
            <button
              onClick={onClose}
              className={cn(
                'inline-flex rounded-md p-1.5 focus:outline-none focus:ring-2 focus:ring-offset-2',
                config.textColor,
                'hover:bg-black/5 dark:hover:bg-white/5'
              )}
              aria-label="Dismiss alert"
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default {
  useFocusTrap,
  AccessibleModal,
  AccessibleButton,
  AccessibleInput,
  AccessibleNav,
  SkipToContent,
  ScreenReaderOnly,
  AccessibleTooltip,
  AccessibleAlert,
};