'use client';

import React from 'react';
import { motion, AnimatePresence, Variants } from 'framer-motion';
import { cn } from '@/lib/utils';

// Page transition variants
export const pageTransitionVariants: Variants = {
  initial: {
    opacity: 0,
    y: 20,
    scale: 0.95,
  },
  animate: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: {
      duration: 0.4,
      ease: [0.25, 0.46, 0.45, 0.94],
    },
  },
  exit: {
    opacity: 0,
    y: -20,
    scale: 0.95,
    transition: {
      duration: 0.3,
      ease: [0.25, 0.46, 0.45, 0.94],
    },
  },
};

// Slide transition variants
export const slideVariants: Variants = {
  initial: (direction: number) => ({
    x: direction > 0 ? 300 : -300,
    opacity: 0,
  }),
  animate: {
    x: 0,
    opacity: 1,
    transition: {
      duration: 0.5,
      ease: [0.25, 0.46, 0.45, 0.94],
    },
  },
  exit: (direction: number) => ({
    x: direction < 0 ? 300 : -300,
    opacity: 0,
    transition: {
      duration: 0.3,
      ease: [0.25, 0.46, 0.45, 0.94],
    },
  }),
};

// Stagger animation for lists
export const staggerContainer: Variants = {
  animate: {
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.2,
    },
  },
};

export const staggerItem: Variants = {
  initial: {
    opacity: 0,
    y: 20,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.4,
      ease: [0.25, 0.46, 0.45, 0.94],
    },
  },
};

// Interactive Button Component
interface InteractiveButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'onDrag' | 'onDragStart' | 'onDragEnd' | 'onAnimationStart' | 'onAnimationEnd' | 'onAnimationIteration'> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'destructive';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  children: React.ReactNode;
}

export function InteractiveButton({
  variant = 'primary',
  size = 'md',
  loading = false,
  className,
  children,
  disabled,
  ...props
}: InteractiveButtonProps) {
  const baseClasses = 'relative inline-flex items-center justify-center font-medium rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50';
  
  const variantClasses = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700 focus-visible:ring-blue-500',
    secondary: 'bg-gray-200 text-gray-900 hover:bg-gray-300 focus-visible:ring-gray-500 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700',
    ghost: 'hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-100',
    destructive: 'bg-red-600 text-white hover:bg-red-700 focus-visible:ring-red-500',
  };
  
  const sizeClasses = {
    sm: 'h-8 px-3 text-sm',
    md: 'h-10 px-4 text-sm',
    lg: 'h-12 px-6 text-base',
  };

  return (
    <motion.button
      className={cn(
        baseClasses,
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      transition={{ type: 'spring', stiffness: 400, damping: 17 }}
      disabled={disabled || loading}
      {...props}
    >
      <AnimatePresence mode="wait">
        {loading ? (
          <motion.div
            key="loading"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex items-center space-x-2"
          >
            <motion.div
              className="w-4 h-4 border-2 border-current border-t-transparent rounded-full"
              animate={{ rotate: 360 }}
              transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
            />
            <span>Loading...</span>
          </motion.div>
        ) : (
          <motion.span
            key="content"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            {children}
          </motion.span>
        )}
      </AnimatePresence>
    </motion.button>
  );
}

// Like/Bookmark Animation Component
interface LikeButtonProps {
  isLiked: boolean;
  onToggle: () => void;
  count?: number;
  className?: string;
}

export function LikeButton({ isLiked, onToggle, count, className }: LikeButtonProps) {
  return (
    <motion.button
      className={cn(
        'flex items-center space-x-2 p-2 rounded-full transition-colors',
        isLiked ? 'text-red-500' : 'text-gray-500 hover:text-red-500',
        className
      )}
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
      onClick={onToggle}
    >
      <motion.div
        animate={isLiked ? { scale: [1, 1.2, 1] } : { scale: 1 }}
        transition={{ duration: 0.3, ease: 'easeInOut' }}
      >
        <motion.svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill={isLiked ? 'currentColor' : 'none'}
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
        </motion.svg>
      </motion.div>
      {count !== undefined && (
        <motion.span
          key={count}
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="text-sm font-medium"
        >
          {count}
        </motion.span>
      )}
    </motion.button>
  );
}

// Bookmark Animation Component
interface BookmarkButtonProps {
  isBookmarked: boolean;
  onToggle: () => void;
  className?: string;
}

export function BookmarkButton({ isBookmarked, onToggle, className }: BookmarkButtonProps) {
  return (
    <motion.button
      className={cn(
        'p-2 rounded-full transition-colors',
        isBookmarked ? 'text-yellow-500' : 'text-gray-500 hover:text-yellow-500',
        className
      )}
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
      onClick={onToggle}
    >
      <motion.div
        animate={isBookmarked ? { 
          scale: [1, 1.2, 1],
          rotate: [0, -10, 10, 0]
        } : { scale: 1 }}
        transition={{ duration: 0.4, ease: 'easeInOut' }}
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill={isBookmarked ? 'currentColor' : 'none'}
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
        </svg>
      </motion.div>
    </motion.button>
  );
}

// Floating Action Button
interface FloatingActionButtonProps {
  onClick: () => void;
  icon: React.ReactNode;
  label?: string;
  className?: string;
}

export function FloatingActionButton({ onClick, icon, label, className }: FloatingActionButtonProps) {
  return (
    <motion.button
      className={cn(
        'fixed bottom-6 right-6 bg-blue-600 text-white p-4 rounded-full shadow-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
        className
      )}
      whileHover={{ scale: 1.1, y: -2 }}
      whileTap={{ scale: 0.9 }}
      initial={{ scale: 0, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ type: 'spring', stiffness: 400, damping: 17 }}
      onClick={onClick}
      aria-label={label}
    >
      {icon}
    </motion.button>
  );
}

// Card with hover effects
interface InteractiveCardProps {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  hoverable?: boolean;
}

export function InteractiveCard({ children, className, onClick, hoverable = true }: InteractiveCardProps) {
  return (
    <motion.div
      className={cn(
        'bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden',
        onClick && 'cursor-pointer',
        className
      )}
      whileHover={hoverable ? { 
        y: -4,
        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)'
      } : undefined}
      whileTap={onClick ? { scale: 0.98 } : undefined}
      transition={{ type: 'spring', stiffness: 400, damping: 17 }}
      onClick={onClick}
    >
      {children}
    </motion.div>
  );
}

// Page transition wrapper
interface PageTransitionProps {
  children: React.ReactNode;
  className?: string;
}

export function PageTransition({ children, className }: PageTransitionProps) {
  return (
    <motion.div
      className={className}
      variants={pageTransitionVariants}
      initial="initial"
      animate="animate"
      exit="exit"
    >
      {children}
    </motion.div>
  );
}

// Stagger animation wrapper
interface StaggerAnimationProps {
  children: React.ReactNode;
  className?: string;
}

export function StaggerAnimation({ children, className }: StaggerAnimationProps) {
  return (
    <motion.div
      className={className}
      variants={staggerContainer}
      initial="initial"
      animate="animate"
    >
      {React.Children.map(children, (child, index) => (
        <motion.div key={index} variants={staggerItem}>
          {child}
        </motion.div>
      ))}
    </motion.div>
  );
}

// Success/Error toast animations
interface AnimatedToastProps {
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
  onClose: () => void;
}

export function AnimatedToast({ type, message, onClose }: AnimatedToastProps) {
  const typeStyles = {
    success: 'bg-green-500 text-white',
    error: 'bg-red-500 text-white',
    info: 'bg-blue-500 text-white',
    warning: 'bg-yellow-500 text-black',
  };

  return (
    <motion.div
      className={cn(
        'fixed top-4 right-4 p-4 rounded-lg shadow-lg flex items-center space-x-3 max-w-sm',
        typeStyles[type]
      )}
      initial={{ opacity: 0, y: -50, scale: 0.9 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -50, scale: 0.9 }}
      transition={{ type: 'spring', stiffness: 400, damping: 17 }}
    >
      <span className="flex-1">{message}</span>
      <motion.button
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        onClick={onClose}
        className="text-current opacity-70 hover:opacity-100"
      >
        ×
      </motion.button>
    </motion.div>
  );
}

// Loading spinner with animation
export function AnimatedSpinner({ size = 'md', className }: { size?: 'sm' | 'md' | 'lg'; className?: string }) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
  };

  return (
    <motion.div
      className={cn(
        'border-2 border-current border-t-transparent rounded-full',
        sizeClasses[size],
        className
      )}
      animate={{ rotate: 360 }}
      transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
    />
  );
}

export default {
  InteractiveButton,
  LikeButton,
  BookmarkButton,
  FloatingActionButton,
  InteractiveCard,
  PageTransition,
  StaggerAnimation,
  AnimatedToast,
  AnimatedSpinner,
  pageTransitionVariants,
  slideVariants,
  staggerContainer,
  staggerItem,
};