'use client';

import React, { useState } from 'react';
import { motion, useInView, AnimatePresence } from 'framer-motion';
import { Heart, Bookmark, Share2 } from 'lucide-react';
import { Button } from './button';
import { cn } from '@/lib/utils';

interface AnimatedCardProps {
  children: React.ReactNode;
  className?: string;
  delay?: number;
  direction?: 'up' | 'down' | 'left' | 'right';
  hover?: boolean;
  onClick?: () => void;
}

export function AnimatedCard({
  children,
  className,
  delay = 0,
  direction = 'up',
  hover = true,
  onClick
}: AnimatedCardProps) {
  const ref = React.useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-50px' });

  const directionVariants = {
    up: { y: 50, opacity: 0 },
    down: { y: -50, opacity: 0 },
    left: { x: 50, opacity: 0 },
    right: { x: -50, opacity: 0 }
  };

  return (
    <motion.div
      ref={ref}
      initial={directionVariants[direction]}
      animate={isInView ? { x: 0, y: 0, opacity: 1 } : directionVariants[direction]}
      transition={{
        duration: 0.6,
        delay,
        ease: [0.25, 0.46, 0.45, 0.94]
      }}
      whileHover={hover ? {
        y: -8,
        scale: 1.02,
        transition: { duration: 0.2, ease: 'easeOut' }
      } : undefined}
      whileTap={onClick ? { scale: 0.98 } : undefined}
      onClick={onClick}
      className={cn(
        'cursor-pointer',
        hover && 'hover:shadow-xl hover:shadow-black/10',
        className
      )}
    >
      {children}
    </motion.div>
  );
}

// Ripple effect component
interface RippleButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
  variant?: 'default' | 'ghost' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  rippleColor?: string;
}

export function RippleButton({
  children,
  onClick,
  className,
  variant = 'default',
  size = 'md',
  disabled = false,
  rippleColor = 'rgba(255, 255, 255, 0.6)'
}: RippleButtonProps) {
  const [ripples, setRipples] = useState<Array<{ id: number; x: number; y: number }>>([]);

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const id = Date.now();

    setRipples(prev => [...prev, { id, x, y }]);

    // Remove ripple after animation
    setTimeout(() => {
      setRipples(prev => prev.filter(ripple => ripple.id !== id));
    }, 600);

    onClick?.();
  };

  // Map size to valid Button sizes
  const buttonSize = size === 'md' ? 'default' : size;

  return (
    <Button
      variant={variant}
      size={buttonSize}
      disabled={disabled}
      onClick={handleClick}
      className={cn(
        'relative overflow-hidden',
        className
      )}
    >
      {children}
      {ripples.map(ripple => (
        <motion.span
          key={ripple.id}
          className="absolute rounded-full pointer-events-none"
          style={{
            left: ripple.x,
            top: ripple.y,
            backgroundColor: rippleColor
          }}
          initial={{
            width: 0,
            height: 0,
            opacity: 0.8,
            x: '-50%',
            y: '-50%'
          }}
          animate={{
            width: 300,
            height: 300,
            opacity: 0
          }}
          transition={{
            duration: 0.6,
            ease: 'easeOut'
          }}
        />
      ))}
    </Button>
  );
}

// Animated action buttons with burst effects
interface AnimatedActionButtonProps {
  icon: React.ReactNode;
  isActive?: boolean;
  onClick?: () => void;
  className?: string;
  activeColor?: string;
  burstColor?: string;
}

export function AnimatedActionButton({
  icon,
  isActive = false,
  onClick,
  className,
  activeColor = 'text-red-500',
  burstColor = '#ef4444'
}: AnimatedActionButtonProps) {
  const [showBurst, setShowBurst] = useState(false);

  const handleClick = () => {
    if (!isActive) {
      setShowBurst(true);
      setTimeout(() => setShowBurst(false), 600);
    }
    onClick?.();
  };

  return (
    <motion.button
      onClick={handleClick}
      className={cn(
        'relative p-2 rounded-full transition-colors duration-200',
        isActive ? activeColor : 'text-gray-500 hover:text-gray-700',
        className
      )}
      whileHover={{ scale: 1.1 }}
      whileTap={{ scale: 0.9 }}
    >
      <motion.div
        animate={isActive ? {
          scale: [1, 1.2, 1],
          rotate: [0, 10, -10, 0]
        } : {}}
        transition={{ duration: 0.3 }}
      >
        {icon}
      </motion.div>
      
      {/* Burst effect */}
      <AnimatePresence>
        {showBurst && (
          <motion.div
            className="absolute inset-0 pointer-events-none"
            initial={{ scale: 0, opacity: 1 }}
            animate={{ scale: 2, opacity: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6, ease: 'easeOut' }}
          >
            {[...Array(8)].map((_, i) => (
              <motion.div
                key={i}
                className="absolute w-1 h-1 rounded-full"
                style={{
                  backgroundColor: burstColor,
                  left: '50%',
                  top: '50%'
                }}
                initial={{
                  x: '-50%',
                  y: '-50%',
                  scale: 0
                }}
                animate={{
                  x: `${Math.cos(i * 45 * Math.PI / 180) * 20 - 50}%`,
                  y: `${Math.sin(i * 45 * Math.PI / 180) * 20 - 50}%`,
                  scale: [0, 1, 0]
                }}
                transition={{
                  duration: 0.6,
                  delay: i * 0.05,
                  ease: 'easeOut'
                }}
              />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.button>
  );
}

// Stagger animation container
interface StaggerContainerProps {
  children: React.ReactNode;
  className?: string;
  staggerDelay?: number;
  direction?: 'up' | 'down' | 'left' | 'right';
}

export function StaggerContainer({
  children,
  className,
  staggerDelay = 0.1,
  direction = 'up'
}: StaggerContainerProps) {
  const ref = React.useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-50px' });

  const directionVariants = {
    up: { y: 50, opacity: 0 },
    down: { y: -50, opacity: 0 },
    left: { x: 50, opacity: 0 },
    right: { x: -50, opacity: 0 }
  };

  return (
    <motion.div
      ref={ref}
      className={className}
      initial="hidden"
      animate={isInView ? "visible" : "hidden"}
      variants={{
        hidden: {},
        visible: {
          transition: {
            staggerChildren: staggerDelay
          }
        }
      }}
    >
      {React.Children.map(children, (child, index) => (
        <motion.div
          variants={{
            hidden: directionVariants[direction],
            visible: {
              x: 0,
              y: 0,
              opacity: 1,
              transition: {
                duration: 0.6,
                ease: [0.25, 0.46, 0.45, 0.94]
              }
            }
          }}
        >
          {child}
        </motion.div>
      ))}
    </motion.div>
  );
}

// Usage examples for common actions
export function LikeButton({ isLiked, onToggle, className }: {
  isLiked: boolean;
  onToggle: () => void;
  className?: string;
}) {
  return (
    <AnimatedActionButton
      icon={<Heart className={cn('w-5 h-5', isLiked && 'fill-current')} />}
      isActive={isLiked}
      onClick={onToggle}
      className={className}
      activeColor="text-red-500"
      burstColor="#ef4444"
    />
  );
}

export function BookmarkButton({ isBookmarked, onToggle, className }: {
  isBookmarked: boolean;
  onToggle: () => void;
  className?: string;
}) {
  return (
    <AnimatedActionButton
      icon={<Bookmark className={cn('w-5 h-5', isBookmarked && 'fill-current')} />}
      isActive={isBookmarked}
      onClick={onToggle}
      className={className}
      activeColor="text-blue-500"
      burstColor="#3b82f6"
    />
  );
}

export function ShareButton({ onShare, className }: {
  onShare: () => void;
  className?: string;
}) {
  return (
    <AnimatedActionButton
      icon={<Share2 className="w-5 h-5" />}
      onClick={onShare}
      className={className}
      activeColor="text-green-500"
      burstColor="#10b981"
    />
  );
}