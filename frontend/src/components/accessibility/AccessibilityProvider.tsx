'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';

/**
 * Accessibility context for managing global accessibility state
 */
interface AccessibilityContextType {
  announcements: string[];
  announce: (message: string, priority?: 'polite' | 'assertive') => void;
  reducedMotion: boolean;
  highContrast: boolean;
  fontSize: 'small' | 'medium' | 'large' | 'extra-large';
  setFontSize: (size: 'small' | 'medium' | 'large' | 'extra-large') => void;
  toggleHighContrast: () => void;
}

const AccessibilityContext = createContext<AccessibilityContextType | undefined>(undefined);

export const useAccessibility = () => {
  const context = useContext(AccessibilityContext);
  if (!context) {
    throw new Error('useAccessibility must be used within AccessibilityProvider');
  }
  return context;
};

interface AccessibilityProviderProps {
  children: React.ReactNode;
}

export const AccessibilityProvider: React.FC<AccessibilityProviderProps> = ({ children }) => {
  const [announcements, setAnnouncements] = useState<string[]>([]);
  const [reducedMotion, setReducedMotion] = useState(false);
  const [highContrast, setHighContrast] = useState(false);
  const [fontSize, setFontSize] = useState<'small' | 'medium' | 'large' | 'extra-large'>('medium');

  // Detect user preferences
  useEffect(() => {
    // Check for reduced motion preference
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    setReducedMotion(mediaQuery.matches);

    const handleChange = (e: MediaQueryListEvent) => {
      setReducedMotion(e.matches);
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  // Load saved preferences
  useEffect(() => {
    const savedFontSize = localStorage.getItem('accessibility-font-size') as typeof fontSize;
    const savedHighContrast = localStorage.getItem('accessibility-high-contrast') === 'true';

    if (savedFontSize) {
      setFontSize(savedFontSize);
    }
    if (savedHighContrast) {
      setHighContrast(true);
    }
  }, []);

  // Apply accessibility settings to document
  useEffect(() => {
    const root = document.documentElement;
    
    // Apply font size
    root.classList.remove('text-sm', 'text-base', 'text-lg', 'text-xl');
    switch (fontSize) {
      case 'small':
        root.classList.add('text-sm');
        break;
      case 'medium':
        root.classList.add('text-base');
        break;
      case 'large':
        root.classList.add('text-lg');
        break;
      case 'extra-large':
        root.classList.add('text-xl');
        break;
    }

    // Apply high contrast
    if (highContrast) {
      root.classList.add('high-contrast');
    } else {
      root.classList.remove('high-contrast');
    }

    // Apply reduced motion
    if (reducedMotion) {
      root.classList.add('reduce-motion');
    } else {
      root.classList.remove('reduce-motion');
    }
  }, [fontSize, highContrast, reducedMotion]);

  const announce = (message: string, priority: 'polite' | 'assertive' = 'polite') => {
    setAnnouncements(prev => [...prev, message]);
    
    // Clear announcement after a delay
    setTimeout(() => {
      setAnnouncements(prev => prev.filter(msg => msg !== message));
    }, 5000);
  };

  const handleSetFontSize = (size: typeof fontSize) => {
    setFontSize(size);
    localStorage.setItem('accessibility-font-size', size);
    announce(`Font size changed to ${size}`);
  };

  const toggleHighContrast = () => {
    const newValue = !highContrast;
    setHighContrast(newValue);
    localStorage.setItem('accessibility-high-contrast', newValue.toString());
    announce(newValue ? 'High contrast mode enabled' : 'High contrast mode disabled');
  };

  const value: AccessibilityContextType = {
    announcements,
    announce,
    reducedMotion,
    highContrast,
    fontSize,
    setFontSize: handleSetFontSize,
    toggleHighContrast
  };

  return (
    <AccessibilityContext.Provider value={value}>
      {children}
      
      {/* Screen reader announcements */}
      <div className="sr-only">
        {announcements.map((message, index) => (
          <div
            key={`${message}-${index}`}
            role="status"
            aria-live="polite"
            aria-atomic="true"
          >
            {message}
          </div>
        ))}
      </div>
    </AccessibilityContext.Provider>
  );
};

/**
 * Skip Links Component
 */
export const SkipLinks: React.FC = () => {
  return (
    <div className="skip-links">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      >
        Skip to main content
      </a>
      <a
        href="#navigation"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-32 focus:z-50 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      >
        Skip to navigation
      </a>
      <a
        href="#search"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-64 focus:z-50 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      >
        Skip to search
      </a>
    </div>
  );
};

/**
 * Accessibility Settings Panel
 */
export const AccessibilitySettings: React.FC<{
  isOpen: boolean;
  onClose: () => void;
}> = ({ isOpen, onClose }) => {
  const { fontSize, setFontSize, highContrast, toggleHighContrast } = useAccessibility();

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="accessibility-settings-title"
    >
      <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
        <div className="flex justify-between items-center mb-4">
          <h2 id="accessibility-settings-title" className="text-xl font-semibold">
            Accessibility Settings
          </h2>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label="Close accessibility settings"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="space-y-6">
          {/* Font Size */}
          <fieldset>
            <legend className="text-sm font-medium text-gray-700 mb-3">
              Font Size
            </legend>
            <div className="space-y-2">
              {(['small', 'medium', 'large', 'extra-large'] as const).map((size) => (
                <label key={size} className="flex items-center">
                  <input
                    type="radio"
                    name="fontSize"
                    value={size}
                    checked={fontSize === size}
                    onChange={() => setFontSize(size)}
                    className="mr-3 focus:ring-blue-500"
                  />
                  <span className="capitalize">{size.replace('-', ' ')}</span>
                </label>
              ))}
            </div>
          </fieldset>

          {/* High Contrast */}
          <div>
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={highContrast}
                onChange={toggleHighContrast}
                className="mr-3 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">
                High Contrast Mode
              </span>
            </label>
            <p className="text-sm text-gray-500 mt-1">
              Increases contrast for better visibility
            </p>
          </div>
        </div>

        <div className="mt-6 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};

/**
 * Focus Trap Hook
 */
export const useFocusTrap = (isActive: boolean) => {
  useEffect(() => {
    if (!isActive) return;

    const focusableElements = document.querySelectorAll(
      'a[href], button, textarea, input[type="text"], input[type="radio"], input[type="checkbox"], select, [tabindex]:not([tabindex="-1"])'
    );

    const firstElement = focusableElements[0] as HTMLElement;
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          lastElement.focus();
          e.preventDefault();
        }
      } else {
        if (document.activeElement === lastElement) {
          firstElement.focus();
          e.preventDefault();
        }
      }
    };

    document.addEventListener('keydown', handleTabKey);
    firstElement?.focus();

    return () => {
      document.removeEventListener('keydown', handleTabKey);
    };
  }, [isActive]);
};

/**
 * Keyboard Navigation Hook
 */
export const useKeyboardNavigation = () => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Escape key handling for modals
      if (e.key === 'Escape') {
        const modal = document.querySelector('[role="dialog"][aria-modal="true"]');
        if (modal) {
          const closeButton = modal.querySelector('[aria-label*="close" i], [data-close]') as HTMLElement;
          closeButton?.click();
        }
      }

      // Arrow key navigation for menus
      if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
        const activeElement = document.activeElement;
        if (activeElement?.closest('[role="menu"], [role="menubar"], [role="tablist"]')) {
          e.preventDefault();
          // Handle arrow key navigation logic here
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);
};