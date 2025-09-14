'use client';

import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence, Variants } from 'framer-motion';
import { useRouter, usePathname } from 'next/navigation';

// Page Transition Variants
const pageVariants: Record<string, Variants> = {
  slideLeft: {
    initial: { x: '100%', opacity: 0 },
    animate: { x: 0, opacity: 1 },
    exit: { x: '-100%', opacity: 0 },
  },
  slideRight: {
    initial: { x: '-100%', opacity: 0 },
    animate: { x: 0, opacity: 1 },
    exit: { x: '100%', opacity: 0 },
  },
  slideUp: {
    initial: { y: '100%', opacity: 0 },
    animate: { y: 0, opacity: 1 },
    exit: { y: '-100%', opacity: 0 },
  },
  slideDown: {
    initial: { y: '-100%', opacity: 0 },
    animate: { y: 0, opacity: 1 },
    exit: { y: '100%', opacity: 0 },
  },
  fade: {
    initial: { opacity: 0 },
    animate: { opacity: 1 },
    exit: { opacity: 0 },
  },
  scale: {
    initial: { scale: 0.8, opacity: 0 },
    animate: { scale: 1, opacity: 1 },
    exit: { scale: 1.2, opacity: 0 },
  },
  flip: {
    initial: { rotateY: -90, opacity: 0 },
    animate: { rotateY: 0, opacity: 1 },
    exit: { rotateY: 90, opacity: 0 },
  },
  zoom: {
    initial: { scale: 0, opacity: 0 },
    animate: { scale: 1, opacity: 1 },
    exit: { scale: 0, opacity: 0 },
  },
};

// Transition timing configurations
const transitionConfigs = {
  fast: { duration: 0.2, ease: 'easeInOut' },
  normal: { duration: 0.3, ease: 'easeInOut' },
  slow: { duration: 0.5, ease: 'easeInOut' },
  spring: { type: 'spring', stiffness: 300, damping: 30 },
  bouncy: { type: 'spring', stiffness: 400, damping: 25 },
};

// Page Transition Wrapper
interface PageTransitionProps {
  children: React.ReactNode;
  variant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
  className?: string;
}

export const PageTransition: React.FC<PageTransitionProps> = ({
  children,
  variant = 'fade',
  timing = 'normal',
  className = '',
}) => {
  const pathname = usePathname();

  return (
    <AnimatePresence mode="wait">
      <motion.div
        key={pathname}
        variants={pageVariants[variant]}
        initial="initial"
        animate="animate"
        exit="exit"
        transition={transitionConfigs[timing]}
        className={`${className}`}
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
};

// Route-based Page Transitions
interface RouteTransitionProps {
  children: React.ReactNode;
  routes?: Record<string, keyof typeof pageVariants>;
  defaultVariant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
}

export const RouteTransition: React.FC<RouteTransitionProps> = ({
  children,
  routes = {},
  defaultVariant = 'fade',
  timing = 'normal',
}) => {
  const pathname = usePathname();
  const [previousPath, setPreviousPath] = useState<string>('');
  
  useEffect(() => {
    setPreviousPath(pathname);
  }, [pathname]);

  const getTransitionVariant = () => {
    // Check for specific route transitions
    if (routes[pathname]) {
      return routes[pathname];
    }
    
    // Determine direction based on route hierarchy
    const currentDepth = pathname.split('/').length;
    const previousDepth = previousPath.split('/').length;
    
    if (currentDepth > previousDepth) {
      return 'slideLeft'; // Going deeper
    } else if (currentDepth < previousDepth) {
      return 'slideRight'; // Going back
    }
    
    return defaultVariant;
  };

  return (
    <AnimatePresence mode="wait">
      <motion.div
        key={pathname}
        variants={pageVariants[getTransitionVariant()]}
        initial="initial"
        animate="animate"
        exit="exit"
        transition={transitionConfigs[timing]}
        className="min-h-screen"
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
};

// Section Transitions (for within-page content)
interface SectionTransitionProps {
  children: React.ReactNode;
  isVisible: boolean;
  variant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
  delay?: number;
  className?: string;
}

export const SectionTransition: React.FC<SectionTransitionProps> = ({
  children,
  isVisible,
  variant = 'slideUp',
  timing = 'normal',
  delay = 0,
  className = '',
}) => {
  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          variants={pageVariants[variant]}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={{
            ...transitionConfigs[timing],
            delay,
          }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Staggered List Transitions
interface StaggeredListProps {
  children: React.ReactNode[];
  variant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
  staggerDelay?: number;
  className?: string;
}

export const StaggeredList: React.FC<StaggeredListProps> = ({
  children,
  variant = 'slideUp',
  timing = 'normal',
  staggerDelay = 0.1,
  className = '',
}) => {
  const containerVariants: Variants = {
    initial: {},
    animate: {
      transition: {
        staggerChildren: staggerDelay,
      },
    },
  };

  return (
    <motion.div
      variants={containerVariants}
      initial="initial"
      animate="animate"
      className={className}
    >
      {children.map((child, index) => (
        <motion.div
          key={index}
          variants={pageVariants[variant]}
          transition={transitionConfigs[timing]}
        >
          {child}
        </motion.div>
      ))}
    </motion.div>
  );
};

// Modal Transitions
interface ModalTransitionProps {
  children: React.ReactNode;
  isOpen: boolean;
  variant?: 'scale' | 'slideUp' | 'fade' | 'zoom';
  timing?: keyof typeof transitionConfigs;
  overlayClassName?: string;
  contentClassName?: string;
}

export const ModalTransition: React.FC<ModalTransitionProps> = ({
  children,
  isOpen,
  variant = 'scale',
  timing = 'normal',
  overlayClassName = '',
  contentClassName = '',
}) => {
  const overlayVariants: Variants = {
    initial: { opacity: 0 },
    animate: { opacity: 1 },
    exit: { opacity: 0 },
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          variants={overlayVariants}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={transitionConfigs[timing]}
          className={`fixed inset-0 z-50 flex items-center justify-center ${overlayClassName}`}
        >
          <motion.div
            variants={pageVariants[variant]}
            transition={transitionConfigs[timing]}
            className={contentClassName}
          >
            {children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Tab Transitions
interface TabTransitionProps {
  children: React.ReactNode;
  activeTab: string;
  tabId: string;
  variant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
  className?: string;
}

export const TabTransition: React.FC<TabTransitionProps> = ({
  children,
  activeTab,
  tabId,
  variant = 'fade',
  timing = 'fast',
  className = '',
}) => {
  return (
    <AnimatePresence mode="wait">
      {activeTab === tabId && (
        <motion.div
          key={tabId}
          variants={pageVariants[variant]}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={transitionConfigs[timing]}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Loading Transition
interface LoadingTransitionProps {
  isLoading: boolean;
  children: React.ReactNode;
  loadingComponent?: React.ReactNode;
  variant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
}

export const LoadingTransition: React.FC<LoadingTransitionProps> = ({
  isLoading,
  children,
  loadingComponent,
  variant = 'fade',
  timing = 'normal',
}) => {
  return (
    <AnimatePresence mode="wait">
      {isLoading ? (
        <motion.div
          key="loading"
          variants={pageVariants[variant]}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={transitionConfigs[timing]}
        >
          {loadingComponent || (
            <div className="flex items-center justify-center min-h-[200px]">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
            </div>
          )}
        </motion.div>
      ) : (
        <motion.div
          key="content"
          variants={pageVariants[variant]}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={transitionConfigs[timing]}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Accordion Transition
interface AccordionTransitionProps {
  children: React.ReactNode;
  isOpen: boolean;
  className?: string;
}

export const AccordionTransition: React.FC<AccordionTransitionProps> = ({
  children,
  isOpen,
  className = '',
}) => {
  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: 'auto', opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          transition={{ duration: 0.3, ease: 'easeInOut' }}
          className={`overflow-hidden ${className}`}
        >
          <motion.div
            initial={{ y: -10 }}
            animate={{ y: 0 }}
            exit={{ y: -10 }}
            transition={{ duration: 0.2, delay: 0.1 }}
          >
            {children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Drawer Transition
interface DrawerTransitionProps {
  children: React.ReactNode;
  isOpen: boolean;
  direction?: 'left' | 'right' | 'top' | 'bottom';
  className?: string;
}

export const DrawerTransition: React.FC<DrawerTransitionProps> = ({
  children,
  isOpen,
  direction = 'right',
  className = '',
}) => {
  const getDrawerVariants = (): Variants => {
    switch (direction) {
      case 'left':
        return {
          initial: { x: '-100%' },
          animate: { x: 0 },
          exit: { x: '-100%' },
        };
      case 'right':
        return {
          initial: { x: '100%' },
          animate: { x: 0 },
          exit: { x: '100%' },
        };
      case 'top':
        return {
          initial: { y: '-100%' },
          animate: { y: 0 },
          exit: { y: '-100%' },
        };
      case 'bottom':
        return {
          initial: { y: '100%' },
          animate: { y: 0 },
          exit: { y: '100%' },
        };
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          variants={getDrawerVariants()}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Notification Transition
interface NotificationTransitionProps {
  children: React.ReactNode;
  isVisible: boolean;
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' | 'top-center' | 'bottom-center';
  className?: string;
}

export const NotificationTransition: React.FC<NotificationTransitionProps> = ({
  children,
  isVisible,
  position = 'top-right',
  className = '',
}) => {
  const getNotificationVariants = (): Variants => {
    switch (position) {
      case 'top-right':
      case 'top-left':
      case 'top-center':
        return {
          initial: { y: -100, opacity: 0, scale: 0.95 },
          animate: { y: 0, opacity: 1, scale: 1 },
          exit: { y: -100, opacity: 0, scale: 0.95 },
        };
      case 'bottom-right':
      case 'bottom-left':
      case 'bottom-center':
        return {
          initial: { y: 100, opacity: 0, scale: 0.95 },
          animate: { y: 0, opacity: 1, scale: 1 },
          exit: { y: 100, opacity: 0, scale: 0.95 },
        };
    }
  };

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          variants={getNotificationVariants()}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={{ type: 'spring', stiffness: 500, damping: 30 }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Carousel Transition
interface CarouselTransitionProps {
  children: React.ReactNode[];
  currentIndex: number;
  direction?: 'horizontal' | 'vertical';
  className?: string;
}

export const CarouselTransition: React.FC<CarouselTransitionProps> = ({
  children,
  currentIndex,
  direction = 'horizontal',
  className = '',
}) => {
  const [previousIndex, setPreviousIndex] = useState(currentIndex);
  
  useEffect(() => {
    setPreviousIndex(currentIndex);
  }, [currentIndex]);

  const getCarouselVariants = (): Variants => {
    const isForward = currentIndex > previousIndex;
    
    if (direction === 'horizontal') {
      return {
        initial: { x: isForward ? '100%' : '-100%', opacity: 0 },
        animate: { x: 0, opacity: 1 },
        exit: { x: isForward ? '-100%' : '100%', opacity: 0 },
      };
    } else {
      return {
        initial: { y: isForward ? '100%' : '-100%', opacity: 0 },
        animate: { y: 0, opacity: 1 },
        exit: { y: isForward ? '-100%' : '100%', opacity: 0 },
      };
    }
  };

  return (
    <div className={`relative overflow-hidden ${className}`}>
      <AnimatePresence mode="wait">
        <motion.div
          key={currentIndex}
          variants={getCarouselVariants()}
          initial="initial"
          animate="animate"
          exit="exit"
          transition={{ duration: 0.3, ease: 'easeInOut' }}
        >
          {children[currentIndex]}
        </motion.div>
      </AnimatePresence>
    </div>
  );
};

// Parallax Transition
interface ParallaxTransitionProps {
  children: React.ReactNode;
  offset?: number;
  className?: string;
}

export const ParallaxTransition: React.FC<ParallaxTransitionProps> = ({
  children,
  offset = 0.5,
  className = '',
}) => {
  const [scrollY, setScrollY] = useState(0);

  useEffect(() => {
    const handleScroll = () => setScrollY(window.scrollY);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <motion.div
      style={{
        y: scrollY * offset,
      }}
      className={className}
    >
      {children}
    </motion.div>
  );
};

// Reveal Transition (for scroll-triggered animations)
interface RevealTransitionProps {
  children: React.ReactNode;
  variant?: keyof typeof pageVariants;
  timing?: keyof typeof transitionConfigs;
  threshold?: number;
  className?: string;
}

export const RevealTransition: React.FC<RevealTransitionProps> = ({
  children,
  variant = 'slideUp',
  timing = 'normal',
  threshold = 0.1,
  className = '',
}) => {
  const [isVisible, setIsVisible] = useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { threshold }
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, [threshold]);

  return (
    <div ref={ref} className={className}>
      <motion.div
        variants={pageVariants[variant]}
        initial="initial"
        animate={isVisible ? 'animate' : 'initial'}
        transition={transitionConfigs[timing]}
      >
        {children}
      </motion.div>
    </div>
  );
};

// Export all transition configurations for external use
export { pageVariants, transitionConfigs };