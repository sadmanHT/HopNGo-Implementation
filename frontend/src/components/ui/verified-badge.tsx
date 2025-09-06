import React from 'react';
import { Badge } from '@/components/ui/badge';
import { CheckCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

interface VerifiedBadgeProps {
  isVerified?: boolean;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'default' | 'minimal';
  className?: string;
}

export function VerifiedBadge({ 
  isVerified = false, 
  size = 'sm', 
  variant = 'default',
  className 
}: VerifiedBadgeProps) {
  if (!isVerified) return null;

  const sizeClasses = {
    sm: 'h-3 w-3',
    md: 'h-4 w-4', 
    lg: 'h-5 w-5'
  };

  const badgeClasses = {
    sm: 'text-xs px-1.5 py-0.5',
    md: 'text-xs px-2 py-1',
    lg: 'text-sm px-2.5 py-1'
  };

  if (variant === 'minimal') {
    return (
      <CheckCircle 
        className={cn(
          'text-blue-600 fill-current',
          sizeClasses[size],
          className
        )}
        title="Verified Provider"
      />
    );
  }

  return (
    <Badge 
      className={cn(
        'bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-100 flex items-center gap-1',
        badgeClasses[size],
        className
      )}
      title="Verified Provider"
    >
      <CheckCircle className={sizeClasses[size]} />
      <span>Verified</span>
    </Badge>
  );
}