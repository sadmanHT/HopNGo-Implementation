'use client';

import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { OptimizedImage, generateBlurPlaceholder } from './optimized-image';
import { cn } from '@/lib/utils';

interface LazyImageProps {
  src: string;
  alt: string;
  width?: number;
  height?: number;
  className?: string;
  priority?: boolean;
  quality?: number;
  fill?: boolean;
  sizes?: string;
  aspectRatio?: 'square' | 'video' | 'portrait' | 'landscape' | string;
  onLoad?: () => void;
  onError?: () => void;
  animate?: boolean;
  rootMargin?: string;
}

export function LazyImage({
  src,
  alt,
  width,
  height,
  className,
  priority = false,
  quality = 80,
  fill = false,
  sizes,
  aspectRatio,
  onLoad,
  onError,
  animate = true,
  rootMargin = '50px'
}: LazyImageProps) {
  const [isInView, setIsInView] = useState(priority);
  const [isLoaded, setIsLoaded] = useState(false);
  const imgRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (priority) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          observer.disconnect();
        }
      },
      {
        rootMargin,
        threshold: 0.1
      }
    );

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, [priority, rootMargin]);

  const handleLoad = () => {
    setIsLoaded(true);
    onLoad?.();
  };

  const getAspectRatioClass = () => {
    switch (aspectRatio) {
      case 'square':
        return 'aspect-square';
      case 'video':
        return 'aspect-video';
      case 'portrait':
        return 'aspect-[3/4]';
      case 'landscape':
        return 'aspect-[4/3]';
      default:
        return aspectRatio ? `aspect-[${aspectRatio}]` : '';
    }
  };

  const containerClasses = cn(
    'relative overflow-hidden bg-gradient-to-br from-gray-100 to-gray-200',
    !fill && getAspectRatioClass(),
    className
  );

  if (!isInView) {
    return (
      <div ref={imgRef} className={containerClasses}>
        <div className="absolute inset-0 bg-gradient-to-br from-gray-100 to-gray-200 animate-pulse" />
        {src.includes('cloudinary.com') && (
          <div 
            className="absolute inset-0 bg-cover bg-center filter blur-lg scale-110"
            style={{
              backgroundImage: `url(${generateBlurPlaceholder(src)})`
            }}
          />
        )}
      </div>
    );
  }

  const imageComponent = (
    <OptimizedImage
      src={src}
      alt={alt}
      width={width}
      height={height}
      fill={fill}
      priority={priority}
      quality={quality}
      sizes={sizes}
      onLoad={handleLoad}
      onError={onError}
      className={cn(
        'transition-all duration-700 ease-out',
        isLoaded ? 'opacity-100' : 'opacity-0'
      )}
    />
  );

  return (
    <div ref={imgRef} className={containerClasses}>
      {animate ? (
        <motion.div
          initial={{ opacity: 0, scale: 1.1 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
          className={fill ? 'absolute inset-0' : 'w-full h-full'}
        >
          {imageComponent}
        </motion.div>
      ) : (
        imageComponent
      )}
    </div>
  );
}

// Preset components for common use cases
export function LazyAvatar({
  src,
  alt,
  size = 40,
  className,
  ...props
}: Omit<LazyImageProps, 'width' | 'height' | 'aspectRatio'> & {
  size?: number;
}) {
  return (
    <LazyImage
      src={src}
      alt={alt}
      width={size}
      height={size}
      aspectRatio="square"
      className={cn('rounded-full', className)}
      {...props}
    />
  );
}

export function LazyCard({
  src,
  alt,
  className,
  ...props
}: Omit<LazyImageProps, 'aspectRatio'>) {
  return (
    <LazyImage
      src={src}
      alt={alt}
      aspectRatio="video"
      className={cn('rounded-lg', className)}
      {...props}
    />
  );
}

export function LazyHero({
  src,
  alt,
  className,
  ...props
}: LazyImageProps) {
  return (
    <LazyImage
      src={src}
      alt={alt}
      fill
      priority
      quality={90}
      className={cn('rounded-xl', className)}
      {...props}
    />
  );
}