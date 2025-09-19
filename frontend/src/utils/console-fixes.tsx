// Console Warnings Fix Utility
// This file contains utilities to fix common React console warnings

import React from 'react';

/**
 * Generate unique keys for React lists
 * Fixes: "Warning: Each child in a list should have a unique 'key' prop"
 */
export const generateKey = (item: any, index: number, prefix = 'item'): string => {
  // Try to use item's id first
  if (item && typeof item === 'object' && item.id) {
    return `${prefix}-${item.id}`;
  }
  
  // Try to use item's unique properties
  if (item && typeof item === 'object') {
    const uniqueProps = ['uuid', 'key', 'slug', 'name', 'title'];
    for (const prop of uniqueProps) {
      if (item[prop]) {
        return `${prefix}-${item[prop]}`;
      }
    }
  }
  
  // Fallback to index with prefix
  return `${prefix}-${index}`;
};

/**
 * Safe key generator for nested objects
 */
export const generateNestedKey = (
  parentKey: string,
  item: any,
  index: number
): string => {
  return `${parentKey}-${generateKey(item, index)}`;
};

/**
 * Hydration-safe component wrapper
 * Fixes: "Warning: Text content did not match. Server vs Client"
 */
export const HydrationSafe: React.FC<{
  children: React.ReactNode;
  fallback?: React.ReactNode;
}> = ({ children, fallback = null }) => {
  const [hasMounted, setHasMounted] = React.useState(false);

  React.useEffect(() => {
    setHasMounted(true);
  }, []);

  if (!hasMounted) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};

/**
 * Client-only component wrapper
 * Prevents hydration mismatches for client-specific content
 */
export const ClientOnly: React.FC<{
  children: React.ReactNode;
  fallback?: React.ReactNode;
}> = ({ children, fallback = null }) => {
  const [hasMounted, setHasMounted] = React.useState(false);

  React.useEffect(() => {
    setHasMounted(true);
  }, []);

  return hasMounted ? <>{children}</> : <>{fallback}</>;
};

/**
 * Safe HTML content renderer
 * Fixes: "Warning: validateDOMNesting" and XSS issues
 */
export const SafeHTML: React.FC<{
  content: string;
  tag?: keyof JSX.IntrinsicElements;
  className?: string;
}> = ({ content, tag: Tag = 'div', className }) => {
  // Basic HTML sanitization (in production, use a proper library like DOMPurify)
  const sanitizeHTML = (html: string): string => {
    return html
      .replace(/<script[^>]*>.*?<\/script>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+="[^"]*"/gi, '')
      .replace(/on\w+='[^']*'/gi, '');
  };

  const sanitizedContent = sanitizeHTML(content);

  return (
    <Tag
      className={className}
      dangerouslySetInnerHTML={{ __html: sanitizedContent }}
    />
  );
};

/**
 * Safe image component with proper loading states
 * Fixes: Image loading warnings and accessibility issues
 */
export const SafeImage: React.FC<{
  src: string;
  alt: string;
  width?: number;
  height?: number;
  className?: string;
  fallback?: string;
  loading?: 'lazy' | 'eager';
}> = ({
  src,
  alt,
  width,
  height,
  className,
  fallback = '/images/placeholder.jpg',
  loading = 'lazy',
}) => {
  const [imageSrc, setImageSrc] = React.useState(src);
  const [isLoading, setIsLoading] = React.useState(true);
  const [hasError, setHasError] = React.useState(false);

  const handleLoad = () => {
    setIsLoading(false);
    setHasError(false);
  };

  const handleError = () => {
    setIsLoading(false);
    setHasError(true);
    setImageSrc(fallback);
  };

  return (
    <div className={`relative ${className || ''}`}>
      {isLoading && (
        <div className="absolute inset-0 bg-gray-200 animate-pulse rounded" />
      )}
      <img
        src={imageSrc}
        alt={alt}
        width={width}
        height={height}
        loading={loading}
        onLoad={handleLoad}
        onError={handleError}
        className={`${isLoading ? 'opacity-0' : 'opacity-100'} transition-opacity duration-300`}
      />
      {hasError && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-100 text-gray-500 text-sm">
          Failed to load image
        </div>
      )}
    </div>
  );
};

/**
 * Safe form input wrapper
 * Fixes: Controlled/uncontrolled input warnings
 */
export const SafeInput: React.FC<{
  value?: string;
  defaultValue?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  [key: string]: any;
}> = ({ value, defaultValue, onChange, ...props }) => {
  const [internalValue, setInternalValue] = React.useState(defaultValue || '');
  const isControlled = value !== undefined;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!isControlled) {
      setInternalValue(e.target.value);
    }
    onChange?.(e);
  };

  return (
    <input
      {...props}
      value={isControlled ? value : internalValue}
      onChange={handleChange}
    />
  );
};

/**
 * Safe textarea wrapper
 */
export const SafeTextarea: React.FC<{
  value?: string;
  defaultValue?: string;
  onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  [key: string]: any;
}> = ({ value, defaultValue, onChange, ...props }) => {
  const [internalValue, setInternalValue] = React.useState(defaultValue || '');
  const isControlled = value !== undefined;

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (!isControlled) {
      setInternalValue(e.target.value);
    }
    onChange?.(e);
  };

  return (
    <textarea
      {...props}
      value={isControlled ? value : internalValue}
      onChange={handleChange}
    />
  );
};

/**
 * Safe select wrapper
 */
export const SafeSelect: React.FC<{
  value?: string;
  defaultValue?: string;
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void;
  children: React.ReactNode;
  [key: string]: any;
}> = ({ value, defaultValue, onChange, children, ...props }) => {
  const [internalValue, setInternalValue] = React.useState(defaultValue || '');
  const isControlled = value !== undefined;

  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    if (!isControlled) {
      setInternalValue(e.target.value);
    }
    onChange?.(e);
  };

  return (
    <select
      {...props}
      value={isControlled ? value : internalValue}
      onChange={handleChange}
    >
      {children}
    </select>
  );
};

/**
 * Console warning suppressor for development
 * Use sparingly and only for known false positives
 */
export const suppressConsoleWarnings = (patterns: string[]) => {
  if (process.env.NODE_ENV === 'development') {
    const originalWarn = console.warn;
    const originalError = console.error;

    console.warn = (...args) => {
      const message = args.join(' ');
      if (!patterns.some(pattern => message.includes(pattern))) {
        originalWarn.apply(console, args);
      }
    };

    console.error = (...args) => {
      const message = args.join(' ');
      if (!patterns.some(pattern => message.includes(pattern))) {
        originalError.apply(console, args);
      }
    };

    // Return cleanup function
    return () => {
      console.warn = originalWarn;
      console.error = originalError;
    };
  }
  return () => {};
};

/**
 * Development-only component for debugging
 */
export const DevOnly: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  if (process.env.NODE_ENV !== 'development') {
    return null;
  }
  return <>{children}</>;
};

/**
 * Production-only component
 */
export const ProdOnly: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  if (process.env.NODE_ENV === 'development') {
    return null;
  }
  return <>{children}</>;
};

/**
 * Safe array mapper with automatic key generation
 */
export const safeMap = <T,>(
  array: T[],
  renderItem: (item: T, index: number, key: string) => React.ReactNode,
  keyPrefix = 'item'
): React.ReactNode[] => {
  if (!Array.isArray(array)) {
    console.warn('safeMap: Expected array but received:', typeof array);
    return [];
  }

  return array.map((item, index) => {
    const key = generateKey(item, index, keyPrefix);
    return renderItem(item, index, key);
  });
};

/**
 * Safe object key mapper
 */
export const safeMapObject = <T,>(
  obj: Record<string, T>,
  renderItem: (key: string, value: T, index: number) => React.ReactNode
): React.ReactNode[] => {
  if (!obj || typeof obj !== 'object') {
    console.warn('safeMapObject: Expected object but received:', typeof obj);
    return [];
  }

  return Object.entries(obj).map(([key, value], index) => {
    return renderItem(key, value, index);
  });
};

/**
 * Error boundary component
 */
interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends React.Component<
  { children: React.ReactNode; fallback?: React.ReactNode },
  ErrorBoundaryState
> {
  constructor(props: { children: React.ReactNode; fallback?: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback || (
          <div className="p-4 border border-red-200 rounded-lg bg-red-50">
            <h3 className="text-red-800 font-semibold">Something went wrong</h3>
            <p className="text-red-600 text-sm mt-1">
              {this.state.error?.message || 'An unexpected error occurred'}
            </p>
          </div>
        )
      );
    }

    return this.props.children;
  }
}

/**
 * Hook for safe async operations
 */
export const useSafeAsync = () => {
  const mountedRef = React.useRef(true);

  React.useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const safeSetState = React.useCallback(
    (callback: () => void) => {
      if (mountedRef.current) {
        callback();
      }
    },
    []
  );

  return safeSetState;
};

/**
 * Utility to validate and fix common prop types
 */
export const validateProps = {
  string: (value: any, fallback = ''): string => {
    return typeof value === 'string' ? value : fallback;
  },
  number: (value: any, fallback = 0): number => {
    const num = Number(value);
    return isNaN(num) ? fallback : num;
  },
  boolean: (value: any, fallback = false): boolean => {
    return typeof value === 'boolean' ? value : fallback;
  },
  array<T>(value: any, fallback: T[] = []): T[] {
    return Array.isArray(value) ? value : fallback;
  },
  object<T>(value: any, fallback: T | null = null): T | null {
    return value && typeof value === 'object' && !Array.isArray(value) ? value : fallback;
  },
};

/**
 * React import for TypeScript
 */

// Export types for better TypeScript support
export type SafeComponentProps = {
  children?: React.ReactNode;
  className?: string;
};

export type SafeInputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  value?: string;
  defaultValue?: string;
};

export type SafeTextareaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> & {
  value?: string;
  defaultValue?: string;
};

export type SafeSelectProps = React.SelectHTMLAttributes<HTMLSelectElement> & {
  value?: string;
  defaultValue?: string;
  children: React.ReactNode;
};