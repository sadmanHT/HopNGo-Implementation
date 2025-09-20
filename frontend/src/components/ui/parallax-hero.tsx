'use client';

import React, { useEffect, useState, useRef } from 'react';
import { motion, useScroll, useTransform, useReducedMotion } from 'framer-motion';
import { OptimizedImage } from './optimized-image';
import { cn } from '@/lib/utils';

interface ParallaxHeroProps {
  backgroundImage: string;
  children?: React.ReactNode;
  height?: string;
  className?: string;
  parallaxStrength?: number;
  overlay?: boolean;
  overlayOpacity?: number;
  priority?: boolean;
}

export function ParallaxHero({
  backgroundImage,
  children,
  height = 'h-screen',
  className,
  parallaxStrength = 0.5,
  overlay = true,
  overlayOpacity = 0.4,
  priority = true
}: ParallaxHeroProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const shouldReduceMotion = useReducedMotion();
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  // Check for user's motion preferences
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    setPrefersReducedMotion(mediaQuery.matches);

    const handleChange = (e: MediaQueryListEvent) => {
      setPrefersReducedMotion(e.matches);
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  const { scrollY } = useScroll({
    target: containerRef,
    offset: ['start start', 'end start']
  });

  // Reduce parallax effect if user prefers reduced motion
  const effectiveStrength = shouldReduceMotion || prefersReducedMotion ? 0.1 : parallaxStrength;
  
  const y = useTransform(scrollY, [0, 1000], [0, 1000 * effectiveStrength]);
  const opacity = useTransform(scrollY, [0, 300], [1, 0.3]);

  return (
    <div
      ref={containerRef}
      className={cn(
        'relative overflow-hidden',
        height,
        className
      )}
    >
      {/* Background Image with Parallax */}
      <motion.div
        style={{ y: shouldReduceMotion ? 0 : y }}
        className="absolute inset-0 w-full h-[120%] -top-[10%]"
      >
        <OptimizedImage
          src={backgroundImage}
          alt="Hero background"
          fill
          priority={priority}
          quality={90}
          className="object-cover"
          sizes="100vw"
        />
      </motion.div>

      {/* Overlay */}
      {overlay && (
        <motion.div
          style={{ opacity: shouldReduceMotion ? overlayOpacity : opacity }}
          className="absolute inset-0 bg-gradient-to-b from-black/20 via-black/40 to-black/60"
        />
      )}

      {/* Content */}
      <div className="relative z-10 h-full flex items-center justify-center">
        <motion.div
          initial={{ opacity: 0, y: shouldReduceMotion ? 0 : 50 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: shouldReduceMotion ? 0.3 : 1,
            delay: shouldReduceMotion ? 0 : 0.2,
            ease: 'easeOut'
          }}
          className="text-center text-white px-4 max-w-4xl mx-auto"
        >
          {children}
        </motion.div>
      </div>

      {/* Scroll indicator */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: shouldReduceMotion ? 0.5 : 1.5 }}
        className="absolute bottom-8 left-1/2 -translate-x-1/2 z-10"
      >
        <motion.div
          animate={shouldReduceMotion ? {} : {
            y: [0, 10, 0]
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: 'easeInOut'
          }}
          className="w-6 h-10 border-2 border-white/50 rounded-full flex justify-center"
        >
          <motion.div
            animate={shouldReduceMotion ? {} : {
              y: [0, 12, 0],
              opacity: [0, 1, 0]
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: 'easeInOut'
            }}
            className="w-1 h-3 bg-white/70 rounded-full mt-2"
          />
        </motion.div>
      </motion.div>
    </div>
  );
}

// Layered parallax component for more complex effects
interface ParallaxLayerProps {
  children: React.ReactNode;
  speed?: number;
  className?: string;
}

export function ParallaxLayer({
  children,
  speed = 0.5,
  className
}: ParallaxLayerProps) {
  const ref = useRef<HTMLDivElement>(null);
  const shouldReduceMotion = useReducedMotion();

  const { scrollY } = useScroll({
    target: ref,
    offset: ['start end', 'end start']
  });

  const y = useTransform(scrollY, [0, 1000], [0, shouldReduceMotion ? 0 : 1000 * speed]);

  return (
    <motion.div
      ref={ref}
      style={{ y }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

// Multi-layer parallax container
interface MultiLayerParallaxProps {
  layers: Array<{
    content: React.ReactNode;
    speed: number;
    className?: string;
  }>;
  height?: string;
  className?: string;
}

export function MultiLayerParallax({
  layers,
  height = 'h-screen',
  className
}: MultiLayerParallaxProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const shouldReduceMotion = useReducedMotion();

  return (
    <div
      ref={containerRef}
      className={cn(
        'relative overflow-hidden',
        height,
        className
      )}
    >
      {layers.map((layer, index) => (
        <ParallaxLayer
          key={index}
          speed={shouldReduceMotion ? 0 : layer.speed}
          className={cn(
            'absolute inset-0',
            layer.className
          )}
        >
          {layer.content}
        </ParallaxLayer>
      ))}
    </div>
  );
}

// Accessibility-first parallax with motion controls
interface AccessibleParallaxProps extends ParallaxHeroProps {
  showMotionToggle?: boolean;
  motionTogglePosition?: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
}

export function AccessibleParallax({
  showMotionToggle = true,
  motionTogglePosition = 'top-right',
  ...props
}: AccessibleParallaxProps) {
  const [motionEnabled, setMotionEnabled] = useState(true);
  const shouldReduceMotion = useReducedMotion();

  const togglePositionClasses = {
    'top-left': 'top-4 left-4',
    'top-right': 'top-4 right-4',
    'bottom-left': 'bottom-4 left-4',
    'bottom-right': 'bottom-4 right-4'
  };

  const effectiveMotion = motionEnabled && !shouldReduceMotion;

  return (
    <div className="relative">
      <ParallaxHero
        {...props}
        parallaxStrength={effectiveMotion ? props.parallaxStrength : 0}
      />
      
      {showMotionToggle && (
        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 2 }}
          onClick={() => setMotionEnabled(!motionEnabled)}
          className={cn(
            'absolute z-20 px-3 py-2 bg-black/50 text-white text-sm rounded-lg backdrop-blur-sm hover:bg-black/70 transition-colors',
            togglePositionClasses[motionTogglePosition]
          )}
          aria-label={motionEnabled ? 'Disable motion effects' : 'Enable motion effects'}
        >
          {motionEnabled ? '🎬 Motion On' : '⏸️ Motion Off'}
        </motion.button>
      )}
    </div>
  );
}