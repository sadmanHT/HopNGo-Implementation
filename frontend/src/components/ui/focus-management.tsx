'use client';

import React, { useRef, useEffect, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, ChevronDown, ChevronUp, ArrowLeft, ArrowRight, ArrowUp, ArrowDown } from 'lucide-react';

// Focus Trap Hook
export const useFocusTrap = (isActive: boolean = true) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isActive || !containerRef.current) return;

    const container = containerRef.current;
    const focusableElements = container.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    
    const firstElement = focusableElements[0] as HTMLElement;
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement?.focus();
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault();
          firstElement?.focus();
        }
      }
    };

    const handleEscapeKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        const closeButton = container.querySelector('[data-close-button]') as HTMLElement;
        closeButton?.click();
      }
    };

    document.addEventListener('keydown', handleTabKey);
    document.addEventListener('keydown', handleEscapeKey);
    
    // Focus first element when trap becomes active
    firstElement?.focus();

    return () => {
      document.removeEventListener('keydown', handleTabKey);
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [isActive]);

  return containerRef;
};

// Focus Visible Hook
export const useFocusVisible = () => {
  const [isFocusVisible, setIsFocusVisible] = useState(false);
  const ref = useRef<HTMLElement>(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    let hadKeyboardEvent = false;

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.metaKey || e.altKey || e.ctrlKey) return;
      hadKeyboardEvent = true;
    };

    const onPointerDown = () => {
      hadKeyboardEvent = false;
    };

    const onFocus = () => {
      setIsFocusVisible(hadKeyboardEvent);
    };

    const onBlur = () => {
      setIsFocusVisible(false);
    };

    document.addEventListener('keydown', onKeyDown, true);
    document.addEventListener('pointerdown', onPointerDown, true);
    element.addEventListener('focus', onFocus);
    element.addEventListener('blur', onBlur);

    return () => {
      document.removeEventListener('keydown', onKeyDown, true);
      document.removeEventListener('pointerdown', onPointerDown, true);
      element.removeEventListener('focus', onFocus);
      element.removeEventListener('blur', onBlur);
    };
  }, []);

  return { ref, isFocusVisible };
};

// Roving Tabindex Hook for Lists
export const useRovingTabindex = <T extends HTMLElement>(
  items: T[],
  orientation: 'horizontal' | 'vertical' = 'vertical'
) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    const { key } = e;
    let nextIndex = currentIndex;

    if (orientation === 'vertical') {
      if (key === 'ArrowDown') {
        e.preventDefault();
        nextIndex = (currentIndex + 1) % items.length;
      } else if (key === 'ArrowUp') {
        e.preventDefault();
        nextIndex = currentIndex === 0 ? items.length - 1 : currentIndex - 1;
      }
    } else {
      if (key === 'ArrowRight') {
        e.preventDefault();
        nextIndex = (currentIndex + 1) % items.length;
      } else if (key === 'ArrowLeft') {
        e.preventDefault();
        nextIndex = currentIndex === 0 ? items.length - 1 : currentIndex - 1;
      }
    }

    if (key === 'Home') {
      e.preventDefault();
      nextIndex = 0;
    } else if (key === 'End') {
      e.preventDefault();
      nextIndex = items.length - 1;
    }

    if (nextIndex !== currentIndex) {
      setCurrentIndex(nextIndex);
      items[nextIndex]?.focus();
    }
  }, [currentIndex, items, orientation]);

  useEffect(() => {
    items.forEach((item, index) => {
      if (item) {
        item.tabIndex = index === currentIndex ? 0 : -1;
        item.addEventListener('keydown', handleKeyDown);
        item.addEventListener('focus', () => setCurrentIndex(index));
      }
    });

    return () => {
      items.forEach((item) => {
        if (item) {
          item.removeEventListener('keydown', handleKeyDown);
        }
      });
    };
  }, [items, currentIndex, handleKeyDown]);

  return { currentIndex, setCurrentIndex };
};

// Accessible Modal Component
interface AccessibleModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  closeOnOverlayClick?: boolean;
  closeOnEscape?: boolean;
}

export const AccessibleModal: React.FC<AccessibleModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  size = 'md',
  closeOnOverlayClick = true,
  closeOnEscape = true,
}) => {
  const focusTrapRef = useFocusTrap(isOpen);
  const previousActiveElement = useRef<HTMLElement | null>(null);

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  };

  useEffect(() => {
    if (isOpen) {
      previousActiveElement.current = document.activeElement as HTMLElement;
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
      previousActiveElement.current?.focus();
    }

    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  useEffect(() => {
    if (!closeOnEscape) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose, closeOnEscape]);

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Overlay */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black bg-opacity-50"
            onClick={closeOnOverlayClick ? onClose : undefined}
            aria-hidden="true"
          />

          {/* Modal */}
          <motion.div
            ref={focusTrapRef}
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            transition={{ duration: 0.2 }}
            className={`
              relative bg-white dark:bg-neutral-800 rounded-lg shadow-xl
              w-full ${sizeClasses[size]} max-h-[90vh] overflow-hidden
            `}
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
          >
            {/* Header */}
            <div className="flex items-center justify-between p-6 border-b border-neutral-200 dark:border-neutral-700">
              <h2
                id="modal-title"
                className="text-lg font-semibold text-neutral-900 dark:text-neutral-100"
              >
                {title}
              </h2>
              <button
                data-close-button
                onClick={onClose}
                className="
                  p-2 rounded-lg text-neutral-500 hover:text-neutral-700 hover:bg-neutral-100
                  dark:text-neutral-400 dark:hover:text-neutral-200 dark:hover:bg-neutral-700
                  focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
                  transition-colors
                "
                aria-label="Close modal"
              >
                <X size={20} />
              </button>
            </div>

            {/* Content */}
            <div className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
              {children}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};

// Accessible Dropdown Component
interface AccessibleDropdownProps {
  trigger: React.ReactNode;
  children: React.ReactNode;
  isOpen: boolean;
  onToggle: () => void;
  onClose: () => void;
  placement?: 'bottom-start' | 'bottom-end' | 'top-start' | 'top-end';
}

export const AccessibleDropdown: React.FC<AccessibleDropdownProps> = ({
  trigger,
  children,
  isOpen,
  onToggle,
  onClose,
  placement = 'bottom-start',
}) => {
  const triggerRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const focusTrapRef = useFocusTrap(isOpen);

  const placementClasses = {
    'bottom-start': 'top-full left-0 mt-1',
    'bottom-end': 'top-full right-0 mt-1',
    'top-start': 'bottom-full left-0 mb-1',
    'top-end': 'bottom-full right-0 mb-1',
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node) &&
        !triggerRef.current?.contains(event.target as Node)
      ) {
        onClose();
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen) {
        onClose();
        triggerRef.current?.focus();
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onClose]);

  return (
    <div className="relative inline-block">
      <button
        ref={triggerRef}
        onClick={onToggle}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' || e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (!isOpen) onToggle();
          }
        }}
        aria-expanded={isOpen}
        aria-haspopup="true"
        className="
          inline-flex items-center justify-center px-4 py-2 text-sm font-medium
          text-neutral-700 bg-white border border-neutral-300 rounded-md
          hover:bg-neutral-50 focus:outline-none focus:ring-2 focus:ring-primary-500
          dark:text-neutral-200 dark:bg-neutral-800 dark:border-neutral-600
          dark:hover:bg-neutral-700
        "
      >
        {trigger}
        <ChevronDown
          className={`ml-2 h-4 w-4 transition-transform ${
            isOpen ? 'rotate-180' : ''
          }`}
        />
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            ref={focusTrapRef}
            initial={{ opacity: 0, scale: 0.95, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -10 }}
            transition={{ duration: 0.1 }}
            className={`
              absolute z-50 ${placementClasses[placement]}
              bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700
              rounded-md shadow-lg min-w-[200px] max-w-xs
            `}
          >
            <div
              ref={dropdownRef}
              role="menu"
              aria-orientation="vertical"
              className="py-1"
            >
              {children}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

// Accessible Menu Item
interface AccessibleMenuItemProps {
  children: React.ReactNode;
  onClick?: () => void;
  href?: string;
  disabled?: boolean;
  icon?: React.ReactNode;
}

export const AccessibleMenuItem: React.FC<AccessibleMenuItemProps> = ({
  children,
  onClick,
  href,
  disabled = false,
  icon,
}) => {
  const { ref, isFocusVisible } = useFocusVisible();

  const baseClasses = `
    flex items-center w-full px-4 py-2 text-sm text-left
    text-neutral-700 dark:text-neutral-200
    hover:bg-neutral-100 dark:hover:bg-neutral-700
    focus:outline-none focus:bg-neutral-100 dark:focus:bg-neutral-700
    transition-colors
    ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
    ${isFocusVisible ? 'ring-2 ring-primary-500 ring-inset' : ''}
  `;

  const content = (
    <>
      {icon && <span className="mr-3 flex-shrink-0">{icon}</span>}
      {children}
    </>
  );

  if (href && !disabled) {
    return (
      <a
        ref={ref as React.RefObject<HTMLAnchorElement>}
        href={href}
        className={baseClasses}
        role="menuitem"
      >
        {content}
      </a>
    );
  }

  return (
    <button
      ref={ref as React.RefObject<HTMLButtonElement>}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      className={baseClasses}
      role="menuitem"
    >
      {content}
    </button>
  );
};

// Accessible Tabs Component
interface Tab {
  id: string;
  label: string;
  content: React.ReactNode;
  disabled?: boolean;
}

interface AccessibleTabsProps {
  tabs: Tab[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
  orientation?: 'horizontal' | 'vertical';
}

export const AccessibleTabs: React.FC<AccessibleTabsProps> = ({
  tabs,
  activeTab,
  onTabChange,
  orientation = 'horizontal',
}) => {
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const { currentIndex } = useRovingTabindex(tabRefs.current.filter((ref): ref is HTMLButtonElement => ref !== null), orientation);

  const activeTabContent = tabs.find(tab => tab.id === activeTab)?.content;

  return (
    <div className={`${orientation === 'vertical' ? 'flex' : ''}`}>
      {/* Tab List */}
      <div
        role="tablist"
        aria-orientation={orientation}
        className={`
          ${orientation === 'horizontal' 
            ? 'flex border-b border-neutral-200 dark:border-neutral-700' 
            : 'flex flex-col border-r border-neutral-200 dark:border-neutral-700 min-w-[200px]'
          }
        `}
      >
        {tabs.map((tab, index) => {
          const isActive = tab.id === activeTab;
          
          return (
            <button
              key={tab.id}
              ref={(el) => {
                tabRefs.current[index] = el;
              }}
              role="tab"
              aria-selected={isActive}
              aria-controls={`panel-${tab.id}`}
              id={`tab-${tab.id}`}
              disabled={tab.disabled}
              onClick={() => !tab.disabled && onTabChange(tab.id)}
              className={`
                px-4 py-2 text-sm font-medium transition-colors
                focus:outline-none focus:ring-2 focus:ring-primary-500
                ${orientation === 'horizontal' ? 'border-b-2' : 'border-r-2'}
                ${isActive
                  ? `text-primary-600 dark:text-primary-400 ${
                      orientation === 'horizontal' ? 'border-primary-600' : 'border-primary-600'
                    } bg-primary-50 dark:bg-primary-950`
                  : `text-neutral-600 dark:text-neutral-400 border-transparent
                     hover:text-neutral-800 dark:hover:text-neutral-200
                     hover:bg-neutral-50 dark:hover:bg-neutral-800`
                }
                ${tab.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
              `}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Tab Panel */}
      <div
        role="tabpanel"
        id={`panel-${activeTab}`}
        aria-labelledby={`tab-${activeTab}`}
        className={`
          ${orientation === 'vertical' ? 'flex-1 p-4' : 'p-4'}
        `}
      >
        {activeTabContent}
      </div>
    </div>
  );
};

// Skip Link Component
interface SkipLinkProps {
  href: string;
  children: React.ReactNode;
}

export const SkipLink: React.FC<SkipLinkProps> = ({ href, children }) => {
  return (
    <a
      href={href}
      className="
        sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4
        bg-primary-500 text-white px-4 py-2 rounded-md z-50
        focus:outline-none focus:ring-2 focus:ring-primary-600 focus:ring-offset-2
        transition-all duration-200
      "
    >
      {children}
    </a>
  );
};

// Accessible Tooltip Component
interface AccessibleTooltipProps {
  content: string;
  children: React.ReactNode;
  placement?: 'top' | 'bottom' | 'left' | 'right';
  delay?: number;
}

export const AccessibleTooltip: React.FC<AccessibleTooltipProps> = ({
  content,
  children,
  placement = 'top',
  delay = 500,
}) => {
  const [isVisible, setIsVisible] = useState(false);
  const [timeoutId, setTimeoutId] = useState<NodeJS.Timeout | null>(null);
  const tooltipId = `tooltip-${Math.random().toString(36).substr(2, 9)}`;

  const showTooltip = () => {
    const id = setTimeout(() => setIsVisible(true), delay);
    setTimeoutId(id);
  };

  const hideTooltip = () => {
    if (timeoutId) {
      clearTimeout(timeoutId);
      setTimeoutId(null);
    }
    setIsVisible(false);
  };

  const placementClasses = {
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 -translate-y-1/2 ml-2',
  };

  return (
    <div
      className="relative inline-block"
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocus={showTooltip}
      onBlur={hideTooltip}
    >
      <div aria-describedby={isVisible ? tooltipId : undefined}>
        {children}
      </div>
      
      <AnimatePresence>
        {isVisible && (
          <motion.div
            id={tooltipId}
            role="tooltip"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.1 }}
            className={`
              absolute z-50 ${placementClasses[placement]}
              px-2 py-1 text-sm text-white bg-neutral-900 dark:bg-neutral-700
              rounded shadow-lg whitespace-nowrap pointer-events-none
            `}
          >
            {content}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

// Keyboard Navigation Indicators
export const KeyboardNavigationHelp: React.FC = () => {
  const [showHelp, setShowHelp] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === '?' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        setShowHelp(true);
      }
      if (e.key === 'Escape') {
        setShowHelp(false);
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <AccessibleModal
      isOpen={showHelp}
      onClose={() => setShowHelp(false)}
      title="Keyboard Navigation Help"
      size="md"
    >
      <div className="space-y-4">
        <div className="grid grid-cols-1 gap-3">
          <div className="flex justify-between items-center py-2 border-b border-neutral-200 dark:border-neutral-700">
            <span className="font-medium">Tab</span>
            <span className="text-neutral-600 dark:text-neutral-400">Navigate forward</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-neutral-200 dark:border-neutral-700">
            <span className="font-medium">Shift + Tab</span>
            <span className="text-neutral-600 dark:text-neutral-400">Navigate backward</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-neutral-200 dark:border-neutral-700">
            <span className="font-medium">Enter / Space</span>
            <span className="text-neutral-600 dark:text-neutral-400">Activate button</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-neutral-200 dark:border-neutral-700">
            <span className="font-medium">Arrow Keys</span>
            <span className="text-neutral-600 dark:text-neutral-400">Navigate lists/menus</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-neutral-200 dark:border-neutral-700">
            <span className="font-medium">Escape</span>
            <span className="text-neutral-600 dark:text-neutral-400">Close modal/menu</span>
          </div>
          <div className="flex justify-between items-center py-2">
            <span className="font-medium">Ctrl/Cmd + ?</span>
            <span className="text-neutral-600 dark:text-neutral-400">Show this help</span>
          </div>
        </div>
      </div>
    </AccessibleModal>
  );
};