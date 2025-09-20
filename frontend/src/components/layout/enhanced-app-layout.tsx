'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { usePathname, useRouter } from 'next/navigation';
import { useState as useClientState } from 'react';

// Import our UX components
import { PageTransition } from '../ui/page-transitions';
import { TopNavigation, BottomNavigation, BackButton } from '../navigation/enhanced-navigation';
import { OfflineBanner, NetworkStatusIndicator } from '../ui/offline-handler';
import { AccessibilityAuditTrigger, AutoAccessibilityAudit } from '../ui/accessibility-audit';
import { SkipToContent, useFocusTrap } from '../ui/accessibility';
import { AnimatedToast } from '../ui/micro-interactions';
import { ErrorBoundary } from '../../utils/console-fixes';

// Layout configuration
interface LayoutConfig {
  showTopNav: boolean;
  showBottomNav: boolean;
  showBackButton: boolean;
  pageTransition: 'fade' | 'slideLeft' | 'slideRight' | 'slideUp' | 'slideDown';
  enableOfflineSupport: boolean;
  enableA11yAudit: boolean;
}

// Route-based layout configurations
const routeConfigs: Record<string, Partial<LayoutConfig>> = {
  '/': {
    showTopNav: true,
    showBottomNav: true,
    showBackButton: false,
    pageTransition: 'fade',
  },
  '/search': {
    showTopNav: true,
    showBottomNav: true,
    showBackButton: false,
    pageTransition: 'slideUp',
  },
  '/bookings': {
    showTopNav: true,
    showBottomNav: true,
    showBackButton: false,
    pageTransition: 'slideLeft',
  },
  '/profile': {
    showTopNav: true,
    showBottomNav: true,
    showBackButton: false,
    pageTransition: 'slideRight',
  },
  '/trip': {
    showTopNav: true,
    showBottomNav: false,
    showBackButton: true,
    pageTransition: 'slideLeft',
  },
  '/booking': {
    showTopNav: true,
    showBottomNav: false,
    showBackButton: true,
    pageTransition: 'slideUp',
  },
  '/payment': {
    showTopNav: false,
    showBottomNav: false,
    showBackButton: true,
    pageTransition: 'slideUp',
  },
};

// Default layout configuration
const defaultConfig: LayoutConfig = {
  showTopNav: true,
  showBottomNav: true,
  showBackButton: false,
  pageTransition: 'fade',
  enableOfflineSupport: true,
  enableA11yAudit: process.env.NODE_ENV === 'development',
};

// Enhanced App Layout Component
interface EnhancedAppLayoutProps {
  children: React.ReactNode;
  className?: string;
}

export const EnhancedAppLayout: React.FC<EnhancedAppLayoutProps> = ({
  children,
  className = '',
}) => {
  const pathname = usePathname();
  const router = useRouter();
  const [isClient, setIsClient] = useClientState(false);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);
  const [toasts, setToasts] = useState<Array<{ id: string; message: string; type: 'success' | 'error' | 'info' }>>([]);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const containerRef = useFocusTrap(isMenuOpen);

  // Get current route configuration
  const getRouteConfig = (): LayoutConfig => {
    const routeConfig = pathname ? routeConfigs[pathname] || {} : {};
    return { ...defaultConfig, ...routeConfig };
  };

  const config = getRouteConfig();

  // Handle route changes
  useEffect(() => {
    const handleRouteChangeStart = () => setIsLoading(true);
    const handleRouteChangeComplete = () => setIsLoading(false);

    // Simulate route change events (Next.js 13+ app router doesn't have these events)
    handleRouteChangeStart();
    const timer = setTimeout(handleRouteChangeComplete, 300);

    return () => clearTimeout(timer);
  }, [pathname]);

  // Toast management
  const addToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    const id = Date.now().toString();
    setToasts(prev => [...prev, { id, message, type }]);
    
    // Auto-remove toast after 5 seconds
    setTimeout(() => {
      setToasts(prev => prev.filter(toast => toast.id !== id));
    }, 5000);
  };

  const removeToast = (id: string) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Alt + H: Go to home
      if (e.altKey && e.key === 'h') {
        e.preventDefault();
        isClient && router.push('/');
      }
      
      // Alt + S: Go to search
      if (e.altKey && e.key === 's') {
        e.preventDefault();
        isClient && router.push('/search');
      }
      
      // Alt + B: Go to bookings
      if (e.altKey && e.key === 'b') {
        e.preventDefault();
        isClient && router.push('/bookings');
      }
      
      // Alt + P: Go to profile
      if (e.altKey && e.key === 'p') {
        e.preventDefault();
        isClient && router.push('/profile');
      }
      
      // Escape: Close menu/modal
      if (e.key === 'Escape') {
        setIsMenuOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [router]);

  // Loading overlay
  const LoadingOverlay = () => (
    <AnimatePresence>
      {isLoading && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-white/80 backdrop-blur-sm z-50 flex items-center justify-center"
        >
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
            className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full"
          />
        </motion.div>
      )}
    </AnimatePresence>
  );

  return (
    <ErrorBoundary>
      <div className={`min-h-screen bg-gray-50 ${className}`}>
        {/* Skip to content link */}
        <SkipToContent />
        
        {/* Offline support */}
        {config.enableOfflineSupport && (
          <>
            <OfflineBanner />
            <NetworkStatusIndicator />
          </>
        )}
        
        {/* Top Navigation */}
        <AnimatePresence>
          {config.showTopNav && (
            <motion.div
              initial={{ y: -100 }}
              animate={{ y: 0 }}
              exit={{ y: -100 }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              className="sticky top-0 z-40"
            >
              <TopNavigation
                items={[]}
                className=""
              />
            </motion.div>
          )}
        </AnimatePresence>
        
        {/* Back Button */}
        <AnimatePresence>
          {config.showBackButton && (
            <motion.div
              initial={{ x: -100, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              exit={{ x: -100, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              className="fixed top-4 left-4 z-30"
            >
              <BackButton />
            </motion.div>
          )}
        </AnimatePresence>
        
        {/* Main Content Area */}
        <main 
          id="main-content"
          className={`flex-1 ${config.showTopNav ? 'pt-16' : ''} ${config.showBottomNav ? 'pb-20' : ''}`}
          role="main"
          aria-label="Main content"
        >
          <PageTransition
            variant={config.pageTransition}
            timing="normal"
            className="min-h-screen"
          >
            <div ref={containerRef}>
              {children}
            </div>
          </PageTransition>
        </main>
        
        {/* Bottom Navigation */}
        <AnimatePresence>
          {config.showBottomNav && (
            <motion.div
              initial={{ y: 100 }}
              animate={{ y: 0 }}
              exit={{ y: 100 }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              className="fixed bottom-0 left-0 right-0 z-40"
            >
              <BottomNavigation items={[]} />
            </motion.div>
          )}
        </AnimatePresence>
        
        {/* Toast Notifications */}
        <div className="fixed top-4 right-4 z-50 space-y-2">
          <AnimatePresence>
            {toasts.map((toast) => (
              <motion.div
                key={toast.id}
                initial={{ x: 300, opacity: 0 }}
                animate={{ x: 0, opacity: 1 }}
                exit={{ x: 300, opacity: 0 }}
                transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              >
                <AnimatedToast
                  message={toast.message}
                  type={toast.type}
                  onClose={() => removeToast(toast.id)}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
        
        {/* Loading Overlay */}
        <LoadingOverlay />
        
        {/* Accessibility Features */}
        {config.enableA11yAudit && (
          <>
            <AccessibilityAuditTrigger />
            <AutoAccessibilityAudit />
          </>
        )}
        
        {/* Global Keyboard Shortcuts Help */}
        <div className="sr-only" aria-live="polite">
          Keyboard shortcuts: Alt+H for Home, Alt+S for Search, Alt+B for Bookings, Alt+P for Profile, Escape to close menus
        </div>
      </div>
    </ErrorBoundary>
  );
};

// Layout Provider for managing global layout state
interface LayoutContextType {
  addToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  setLoading: (loading: boolean) => void;
  isMenuOpen: boolean;
  setIsMenuOpen: (open: boolean) => void;
}

const LayoutContext = React.createContext<LayoutContextType | null>(null);

export const useLayout = () => {
  const context = React.useContext(LayoutContext);
  if (!context) {
    throw new Error('useLayout must be used within a LayoutProvider');
  }
  return context;
};

export const LayoutProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Array<{ id: string; message: string; type: 'success' | 'error' | 'info' }>>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const addToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    const id = Date.now().toString();
    setToasts(prev => [...prev, { id, message, type }]);
    
    setTimeout(() => {
      setToasts(prev => prev.filter(toast => toast.id !== id));
    }, 5000);
  };

  const setLoading = (loading: boolean) => {
    setIsLoading(loading);
  };

  const value: LayoutContextType = {
    addToast,
    setLoading,
    isMenuOpen,
    setIsMenuOpen,
  };

  return (
    <LayoutContext.Provider value={value}>
      {children}
    </LayoutContext.Provider>
  );
};

// HOC for pages that need custom layout configuration
export const withLayout = <P extends object>(
  Component: React.ComponentType<P>,
  layoutConfig?: Partial<LayoutConfig>
) => {
  const WrappedComponent = (props: P) => {
    return (
      <EnhancedAppLayout>
        <Component {...props} />
      </EnhancedAppLayout>
    );
  };
  
  WrappedComponent.displayName = `withLayout(${Component.displayName || Component.name})`;
  return WrappedComponent;
};

// Utility hook for managing page-specific layout needs
export const usePageLayout = (config?: Partial<LayoutConfig>) => {
  const layout = useLayout();
  
  useEffect(() => {
    // Apply page-specific configuration
    if (config) {
      // This could be extended to dynamically update layout configuration
      console.log('Page layout config:', config);
    }
  }, [config]);
  
  return layout;
};

// Export layout configuration types for use in other components
export type { LayoutConfig, LayoutContextType };
export { routeConfigs, defaultConfig };