'use client';

import Image from 'next/image';
import { useState } from 'react';
import { cn } from '@/lib/utils';

interface OptimizedImageProps {
  src: string;
  alt: string;
  width?: number;
  height?: number;
  className?: string;
  priority?: boolean;
  quality?: number;
  fill?: boolean;
  sizes?: string;
  placeholder?: 'blur' | 'empty';
  blurDataURL?: string;
  onLoad?: () => void;
  onError?: () => void;
}

export function OptimizedImage({
  src,
  alt,
  width = 400,
  height = 300,
  className,
  priority = false,
  quality = 80,
  fill = false,
  sizes = '(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw',
  placeholder = 'blur',
  blurDataURL,
  onLoad,
  onError
}: OptimizedImageProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  // Generate optimized src and blur placeholder
  const optimizedSrc = getOptimizedCloudinaryUrl(src, {
    width: fill ? 1280 : width,
    height: fill ? undefined : height,
    quality,
    format: 'auto',
    crop: fill ? 'fill' : 'fit',
    gravity: 'auto',
    dpr: 2
  });

  const blurPlaceholder = blurDataURL || (src.includes('cloudinary.com') ? generateBlurPlaceholder(src) : undefined);

  const handleLoad = () => {
    setIsLoading(false);
    onLoad?.();
  };

  const handleError = () => {
    setIsLoading(false);
    setHasError(true);
    onError?.();
  };

  if (hasError) {
    return (
      <div className={cn(
        'flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 text-gray-400',
        fill ? 'absolute inset-0' : 'aspect-video',
        className
      )}>
        <div className="text-center">
          <div className="w-8 h-8 mx-auto mb-2 opacity-50">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
            </svg>
          </div>
          <span className="text-xs">Image unavailable</span>
        </div>
      </div>
    );
  }

  return (
    <div className={cn('relative overflow-hidden', className)}>
      {isLoading && (
        <div className="absolute inset-0 bg-gradient-to-br from-gray-100 to-gray-200 animate-pulse" />
      )}
      <Image
        src={optimizedSrc}
        alt={alt}
        width={fill ? undefined : width}
        height={fill ? undefined : height}
        fill={fill}
        priority={priority}
        quality={quality}
        sizes={sizes}
        placeholder={blurPlaceholder ? 'blur' : placeholder}
        blurDataURL={blurPlaceholder}
        className={cn(
          'transition-all duration-500 ease-out',
          isLoading ? 'opacity-0 scale-105' : 'opacity-100 scale-100',
          fill ? 'object-cover' : 'object-contain'
        )}
        onLoad={handleLoad}
        onError={handleError}
      />
    </div>
  );
}

// Enhanced Cloudinary URL generation with responsive breakpoints
export function getOptimizedCloudinaryUrl(
  originalUrl: string,
  options: {
    width?: number;
    height?: number;
    quality?: 'auto' | number;
    format?: 'auto' | 'webp' | 'avif' | 'jpg' | 'png';
    crop?: 'fill' | 'fit' | 'scale' | 'crop' | 'thumb';
    gravity?: 'auto' | 'face' | 'center' | 'faces';
    dpr?: number; // Device pixel ratio
    blur?: number; // For placeholder images
  } = {}
) {
  const {
    width,
    height,
    quality = 'auto',
    format = 'auto',
    crop = 'fill',
    gravity = 'auto',
    dpr = 1,
    blur
  } = options;

  // Return original URL if not Cloudinary
  if (!originalUrl.includes('cloudinary.com') && !originalUrl.includes('res.cloudinary.com')) {
    return originalUrl;
  }

  // Extract the public ID from Cloudinary URL
  const urlParts = originalUrl.split('/');
  const uploadIndex = urlParts.findIndex(part => part === 'upload');
  if (uploadIndex === -1) return originalUrl;

  const baseUrl = urlParts.slice(0, uploadIndex + 1).join('/');
  const publicIdWithExtension = urlParts.slice(uploadIndex + 1).join('/');
  const publicId = publicIdWithExtension.replace(/\.[^/.]+$/, '');

  // Build transformation string with performance optimizations
  const transformations = [];
  
  // Size transformations
  if (width) transformations.push(`w_${Math.min(width * dpr, 2048)}`);
  if (height) transformations.push(`h_${Math.min(height * dpr, 2048)}`);
  if (crop) transformations.push(`c_${crop}`);
  if (gravity && ['fill', 'crop', 'thumb'].includes(crop)) transformations.push(`g_${gravity}`);
  
  // Quality and format optimizations
  transformations.push(`q_${quality}`);
  transformations.push(`f_${format}`);
  
  // DPR for high-density displays
  if (dpr > 1) transformations.push(`dpr_${dpr}`);
  
  // Blur for placeholders
  if (blur) transformations.push(`e_blur:${blur}`);
  
  // Performance flags
  transformations.push('fl_progressive'); // Progressive JPEG
  transformations.push('fl_immutable_cache'); // Better caching

  const transformationString = transformations.join(',');
  return `${baseUrl}/${transformationString}/${publicId}`;
}

// Generate responsive image srcset for different breakpoints
export function generateResponsiveSrcSet(
  originalUrl: string,
  breakpoints: number[] = [320, 640, 768, 1024, 1280, 1920]
): string {
  return breakpoints
    .map(width => {
      const optimizedUrl = getOptimizedCloudinaryUrl(originalUrl, {
        width,
        quality: 'auto',
        format: 'auto',
        dpr: 2
      });
      return `${optimizedUrl} ${width}w`;
    })
    .join(', ');
}

// Generate blur placeholder for loading states
export function generateBlurPlaceholder(originalUrl: string): string {
  return getOptimizedCloudinaryUrl(originalUrl, {
    width: 40,
    height: 30,
    quality: 10,
    format: 'jpg',
    blur: 1000
  });
}