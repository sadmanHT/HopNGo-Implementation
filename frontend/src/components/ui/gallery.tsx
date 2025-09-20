'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence, PanInfo } from 'framer-motion';
import { useDrag, useGesture } from '@use-gesture/react';
import Image from 'next/image';
import { X, ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Download } from 'lucide-react';
import { cn } from '@/lib/utils';

interface GalleryItem {
  id: string;
  src: string;
  alt: string;
  caption?: {
    en: string;
    bn: string;
  };
  type: 'image' | 'video';
  poster?: string; // For videos
}

interface GalleryProps {
  items: GalleryItem[];
  initialIndex?: number;
  onClose?: () => void;
  className?: string;
  showCaptions?: boolean;
  allowDownload?: boolean;
  locale?: 'en' | 'bn';
}

interface LightboxProps {
  items: GalleryItem[];
  currentIndex: number;
  onClose: () => void;
  onNext: () => void;
  onPrev: () => void;
}

const Lightbox: React.FC<LightboxProps> = ({
  items,
  currentIndex,
  onClose,
  onNext,
  onPrev,
}) => {
  const [scale, setScale] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [language, setLanguage] = useState<'en' | 'bn'>('en');
  const currentItem = items[currentIndex];

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'Escape':
          onClose();
          break;
        case 'ArrowLeft':
          onPrev();
          break;
        case 'ArrowRight':
          onNext();
          break;
        case '+':
        case '=':
          setScale(prev => Math.min(prev * 1.2, 3));
          break;
        case '-':
          setScale(prev => Math.max(prev / 1.2, 0.5));
          break;
        case '0':
          setScale(1);
          setPosition({ x: 0, y: 0 });
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose, onNext, onPrev]);

  // Gesture handling for zoom and pan
  const bind = useGesture(
    {
      onDrag: ({ offset: [x, y], pinching }) => {
        if (!pinching && scale > 1) {
          setPosition({ x, y });
        }
      },
      onPinch: ({ offset: [scale] }) => {
        setScale(Math.max(0.5, Math.min(3, scale)));
      },
      onWheel: ({ delta: [, dy] }) => {
        setScale(prev => Math.max(0.5, Math.min(3, prev - dy * 0.001)));
      },
    },
    {
      drag: {
        from: () => [position.x, position.y],
      },
      pinch: {
        from: () => [scale, 0],
        scaleBounds: { min: 0.5, max: 3 },
      },
    }
  );

  // Swipe navigation
  const swipeBind = useDrag(
    ({ direction: [dx], velocity: [vx], distance, cancel }) => {
      if (scale > 1) {
        cancel();
        return;
      }
      
      const distanceMagnitude = Math.sqrt(distance[0] ** 2 + distance[1] ** 2);
      if (distanceMagnitude > 50 || Math.abs(vx) > 0.5) {
        if (dx > 0) {
          onPrev();
        } else {
          onNext();
        }
      }
    },
    {
      axis: 'x',
      threshold: 10,
    }
  );

  const resetZoom = () => {
    setScale(1);
    setPosition({ x: 0, y: 0 });
  };

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center"
        onClick={onClose}
      >
        {/* Header */}
        <div className="absolute top-4 left-4 right-4 flex justify-between items-center z-10">
          <div className="text-white text-sm">
            {currentIndex + 1} / {items.length}
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setLanguage(language === 'en' ? 'bn' : 'en')}
              className="text-white hover:text-gray-300 px-2 py-1 rounded bg-black/50"
            >
              {language === 'en' ? 'বাংলা' : 'English'}
            </button>
            <button
              onClick={onClose}
              className="text-white hover:text-gray-300 p-2 rounded-full bg-black/50"
            >
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Navigation buttons */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onPrev();
          }}
          className="absolute left-4 top-1/2 -translate-y-1/2 text-white hover:text-gray-300 p-2 rounded-full bg-black/50 z-10"
          disabled={items.length <= 1}
        >
          <ChevronLeft size={24} />
        </button>

        <button
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="absolute right-4 top-1/2 -translate-y-1/2 text-white hover:text-gray-300 p-2 rounded-full bg-black/50 z-10"
          disabled={items.length <= 1}
        >
          <ChevronRight size={24} />
        </button>

        {/* Zoom controls */}
        <div className="absolute bottom-20 right-4 flex flex-col gap-2 z-10">
          <button
            onClick={(e) => {
              e.stopPropagation();
              setScale(prev => Math.min(prev * 1.2, 3));
            }}
            className="text-white hover:text-gray-300 p-2 rounded-full bg-black/50"
          >
            <ZoomIn size={20} />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              setScale(prev => Math.max(prev / 1.2, 0.5));
            }}
            className="text-white hover:text-gray-300 p-2 rounded-full bg-black/50"
          >
            <ZoomOut size={20} />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              resetZoom();
            }}
            className="text-white hover:text-gray-300 p-1 rounded-full bg-black/50 text-xs"
          >
            1:1
          </button>
        </div>

        {/* Media content */}
        <motion.div
          onPointerDown={scale > 1 ? bind().onPointerDown : swipeBind().onPointerDown}
          onPointerMove={scale > 1 ? bind().onPointerMove : swipeBind().onPointerMove}
          onPointerUp={scale > 1 ? bind().onPointerUp : swipeBind().onPointerUp}
          onWheel={scale > 1 ? bind().onWheel : undefined}
          className="relative max-w-[90vw] max-h-[80vh] cursor-grab active:cursor-grabbing"
          onClick={(e) => e.stopPropagation()}
          style={{
            transform: `scale(${scale}) translate(${position.x}px, ${position.y}px)`,
          }}
        >
          {currentItem.type === 'image' ? (
            <Image
              src={currentItem.src}
              alt={currentItem.alt}
              width={1200}
              height={800}
              className="max-w-full max-h-full object-contain"
              priority
              sizes="90vw"
            />
          ) : (
            <video
              src={currentItem.src}
              poster={currentItem.poster}
              controls
              className="max-w-full max-h-full"
              autoPlay={false}
            />
          )}
        </motion.div>

        {/* Caption */}
        {currentItem.caption && (
          <div className="absolute bottom-4 left-4 right-4 text-center">
            <div className="bg-black/70 text-white p-4 rounded-lg max-w-2xl mx-auto">
              <p className="text-sm md:text-base">
                {currentItem.caption[language]}
              </p>
            </div>
          </div>
        )}
      </motion.div>
    </AnimatePresence>
  );
};

export const Gallery: React.FC<GalleryProps> = ({
  items,
  initialIndex = 0,
  onClose,
  className,
  showCaptions = true,
  allowDownload = false,
  locale = 'en'
}) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [isOpen, setIsOpen] = useState(false);
  const [zoom, setZoom] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const containerRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLDivElement>(null);

  const handleNext = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1) % items.length);
    resetZoom();
  }, [items.length]);

  const handlePrev = useCallback(() => {
    setCurrentIndex((prev) => (prev - 1 + items.length) % items.length);
    resetZoom();
  }, [items.length]);

  const handleZoomIn = () => {
    setZoom((prev) => Math.min(prev * 1.5, 4));
  };

  const handleZoomOut = () => {
    setZoom((prev) => Math.max(prev / 1.5, 0.5));
  };

  const resetZoom = () => {
    setZoom(1);
    setPosition({ x: 0, y: 0 });
  };

  const handlePinchZoom = (scale: number) => {
    setZoom((prev) => Math.max(0.5, Math.min(4, prev * scale)));
  };

  const handleDrag = (event: any, info: PanInfo) => {
    if (zoom > 1) {
      setPosition({
        x: position.x + info.delta.x,
        y: position.y + info.delta.y
      });
    }
  };

  const handleSwipe = (event: any, info: PanInfo) => {
    if (zoom === 1 && Math.abs(info.offset.x) > 100) {
      if (info.offset.x > 0) {
        handlePrev();
      } else {
        handleNext();
      }
    }
  };

  const handleDownload = async () => {
    if (!allowDownload) return;
    
    try {
      const response = await fetch(items[currentIndex].src);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `image-${currentIndex + 1}.jpg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download image:', error);
    }
  };

  const handleClose = useCallback(() => {
    setIsOpen(false);
    resetZoom();
    onClose?.();
  }, [onClose]);

  // Reset state when gallery opens
  useEffect(() => {
    if (isOpen) {
      setCurrentIndex(initialIndex);
      setZoom(1);
      setPosition({ x: 0, y: 0 });
    }
  }, [isOpen, initialIndex]);

  // Keyboard navigation
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'Escape':
          handleClose();
          break;
        case 'ArrowLeft':
          handlePrev();
          break;
        case 'ArrowRight':
          handleNext();
          break;
        case '+':
        case '=':
          handleZoomIn();
          break;
        case '-':
          handleZoomOut();
          break;
        case '0':
          resetZoom();
          break;
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, currentIndex]);

  // Prevent body scroll when gallery is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  const openLightbox = (index: number) => {
    setCurrentIndex(index);
    setIsOpen(true);
  };

  return (
    <>
      {/* Gallery Grid */}
      <div className={cn("grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4", className)}>
        {items.map((item, index) => (
          <motion.div
            key={item.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className="relative aspect-square cursor-pointer group overflow-hidden rounded-lg"
            onClick={() => openLightbox(index)}
          >
            {item.type === 'image' ? (
              <Image
                src={item.src}
                alt={item.alt}
                fill
                className="object-cover transition-transform group-hover:scale-105"
                sizes="(max-width: 768px) 50vw, (max-width: 1024px) 33vw, 25vw"
              />
            ) : (
              <div className="relative w-full h-full">
                <Image
                  src={item.poster || '/placeholder.jpg'}
                  alt={item.alt}
                  fill
                  className="object-cover transition-transform group-hover:scale-105"
                  sizes="(max-width: 768px) 50vw, (max-width: 1024px) 33vw, 25vw"
                />
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="w-12 h-12 bg-black/50 rounded-full flex items-center justify-center">
                    <div className="w-0 h-0 border-l-[8px] border-l-white border-y-[6px] border-y-transparent ml-1" />
                  </div>
                </div>
              </div>
            )}
            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors" />
          </motion.div>
        ))}
      </div>

      {/* Lightbox */}
      {isOpen && (
        <Lightbox
          items={items}
          currentIndex={currentIndex}
          onClose={handleClose}
          onNext={handleNext}
          onPrev={handlePrev}
        />
      )}
    </>
  );
};

export default Gallery;