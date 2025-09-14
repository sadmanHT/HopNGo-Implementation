'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';

interface SkeletonProps {
  className?: string;
  variant?: 'default' | 'rounded' | 'circular';
  animation?: 'pulse' | 'wave' | 'shimmer';
  width?: string | number;
  height?: string | number;
}

export function Skeleton({
  className,
  variant = 'default',
  animation = 'shimmer',
  width,
  height,
  ...props
}: SkeletonProps & React.HTMLAttributes<HTMLDivElement>) {
  const baseClasses = 'bg-gradient-to-r from-gray-200 via-gray-300 to-gray-200 dark:from-gray-800 dark:via-gray-700 dark:to-gray-800';
  
  const variantClasses = {
    default: 'rounded-md',
    rounded: 'rounded-lg',
    circular: 'rounded-full',
  };

  const animationVariants = {
    pulse: {
      opacity: [0.5, 1, 0.5],
      transition: {
        duration: 1.5,
        repeat: Infinity,
        ease: 'easeInOut',
      },
    },
    wave: {
      backgroundPosition: ['-200px 0', '200px 0'],
      transition: {
        duration: 1.5,
        repeat: Infinity,
        ease: 'linear',
      },
    },
    shimmer: {
      backgroundPosition: ['-200% 0', '200% 0'],
      transition: {
        duration: 2,
        repeat: Infinity,
        ease: 'linear',
      },
    },
  };

  const style = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
    backgroundSize: animation === 'shimmer' ? '400% 100%' : undefined,
  };

  return (
    <motion.div
      className={cn(
        baseClasses,
        variantClasses[variant],
        className
      )}
      style={style}
      animate={animationVariants[animation]}
      {...props}
    />
  );
}

// Predefined skeleton components for common use cases
export function CardSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('space-y-3', className)}>
      <Skeleton className="h-[200px] w-full" variant="rounded" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
      </div>
    </div>
  );
}

export function ListItemSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('flex items-center space-x-4', className)}>
      <Skeleton className="h-12 w-12" variant="circular" />
      <div className="space-y-2 flex-1">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
      </div>
    </div>
  );
}

export function ProfileSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('flex items-center space-x-4', className)}>
      <Skeleton className="h-16 w-16" variant="circular" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-3 w-24" />
        <Skeleton className="h-3 w-20" />
      </div>
    </div>
  );
}

export function TableSkeleton({ rows = 5, columns = 4, className }: { rows?: number; columns?: number; className?: string }) {
  return (
    <div className={cn('space-y-3', className)}>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="flex space-x-4">
          {Array.from({ length: columns }).map((_, colIndex) => (
            <Skeleton key={colIndex} className="h-4 flex-1" />
          ))}
        </div>
      ))}
    </div>
  );
}

export function FeedSkeleton({ items = 3, className }: { items?: number; className?: string }) {
  return (
    <div className={cn('space-y-6', className)}>
      {Array.from({ length: items }).map((_, index) => (
        <div key={index} className="space-y-3">
          <ProfileSkeleton />
          <Skeleton className="h-[300px] w-full" variant="rounded" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </div>
          <div className="flex space-x-4">
            <Skeleton className="h-8 w-16" variant="rounded" />
            <Skeleton className="h-8 w-16" variant="rounded" />
            <Skeleton className="h-8 w-16" variant="rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function SearchResultsSkeleton({ items = 6, className }: { items?: number; className?: string }) {
  return (
    <div className={cn('grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6', className)}>
      {Array.from({ length: items }).map((_, index) => (
        <CardSkeleton key={index} />
      ))}
    </div>
  );
}

export function BookingSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('space-y-4', className)}>
      <div className="flex justify-between items-start">
        <div className="space-y-2">
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-4 w-32" />
        </div>
        <Skeleton className="h-6 w-20" variant="rounded" />
      </div>
      <Skeleton className="h-[200px] w-full" variant="rounded" />
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-6 w-full" />
        </div>
        <div className="space-y-2">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-6 w-full" />
        </div>
      </div>
      <div className="flex justify-between items-center pt-4">
        <Skeleton className="h-8 w-24" />
        <Skeleton className="h-10 w-32" variant="rounded" />
      </div>
    </div>
  );
}

// Loading states for specific features
export function MarketplaceSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('space-y-6', className)}>
      {/* Search bar skeleton */}
      <div className="flex space-x-4">
        <Skeleton className="h-10 flex-1" variant="rounded" />
        <Skeleton className="h-10 w-20" variant="rounded" />
      </div>
      
      {/* Filters skeleton */}
      <div className="flex space-x-4 overflow-x-auto">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="h-8 w-24 flex-shrink-0" variant="rounded" />
        ))}
      </div>
      
      {/* Results skeleton */}
      <SearchResultsSkeleton />
    </div>
  );
}

export function ChatSkeleton({ messages = 5, className }: { messages?: number; className?: string }) {
  return (
    <div className={cn('space-y-4', className)}>
      {Array.from({ length: messages }).map((_, index) => (
        <div key={index} className={`flex ${index % 2 === 0 ? 'justify-start' : 'justify-end'}`}>
          <div className={`max-w-xs space-y-2 ${index % 2 === 0 ? 'items-start' : 'items-end'}`}>
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-16 w-full" variant="rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}

export default Skeleton;