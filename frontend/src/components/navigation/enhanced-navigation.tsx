'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useRouter, usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { InteractiveButton } from '../ui/micro-interactions';
import { AccessibleNav } from '../ui/accessibility';

// Navigation item interface
interface NavItem {
  id: string;
  label: string;
  href: string;
  icon: React.ReactNode;
  badge?: number;
  requiresAuth?: boolean;
  children?: NavItem[];
}

// Top navigation component
interface TopNavigationProps {
  items: NavItem[];
  logo?: React.ReactNode;
  userMenu?: React.ReactNode;
  className?: string;
  onSearch?: (query: string) => void;
}

export function TopNavigation({ 
  items, 
  logo, 
  userMenu, 
  className, 
  onSearch 
}: TopNavigationProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [searchFocused, setSearchFocused] = useState(false);

  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    if (onSearch && searchQuery.trim()) {
      onSearch(searchQuery.trim());
    }
  }, [onSearch, searchQuery]);

  const handleNavigation = useCallback((href: string) => {
    router.push(href);
    setIsMobileMenuOpen(false);
  }, [router]);

  // Close mobile menu on route change
  useEffect(() => {
    setIsMobileMenuOpen(false);
  }, [pathname]);

  return (
    <nav className={cn(
      'sticky top-0 z-40 bg-white/95 dark:bg-gray-900/95 backdrop-blur-sm border-b border-gray-200 dark:border-gray-800',
      className
    )}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center space-x-4">
            <motion.div
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="cursor-pointer"
              onClick={() => handleNavigation('/')}
            >
              {logo || (
                <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                  HopNGo
                </div>
              )}
            </motion.div>
          </div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-1">
            {items.map((item) => {
              const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
              return (
                <motion.button
                  key={item.id}
                  onClick={() => handleNavigation(item.href)}
                  className={cn(
                    'flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                    'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
                    isActive
                      ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                      : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100 dark:text-gray-300 dark:hover:text-gray-100 dark:hover:bg-gray-800'
                  )}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <span className="w-5 h-5">{item.icon}</span>
                  <span>{item.label}</span>
                  {item.badge && item.badge > 0 && (
                    <motion.span
                      className="bg-red-500 text-white text-xs rounded-full px-2 py-0.5 min-w-[1.25rem] h-5 flex items-center justify-center"
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      key={item.badge}
                    >
                      {item.badge > 99 ? '99+' : item.badge}
                    </motion.span>
                  )}
                </motion.button>
              );
            })}
          </div>

          {/* Search Bar */}
          {onSearch && (
            <motion.form
              onSubmit={handleSearch}
              className="hidden md:flex items-center flex-1 max-w-md mx-8"
              animate={searchFocused ? { scale: 1.02 } : { scale: 1 }}
              transition={{ duration: 0.2 }}
            >
              <div className="relative w-full">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onFocus={() => setSearchFocused(true)}
                  onBlur={() => setSearchFocused(false)}
                  className="block w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                  placeholder="Search destinations, services..."
                  aria-label="Search"
                />
              </div>
            </motion.form>
          )}

          {/* User Menu & Mobile Menu Button */}
          <div className="flex items-center space-x-4">
            {userMenu}
            
            {/* Mobile menu button */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2 rounded-lg text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              aria-expanded={isMobileMenuOpen}
              aria-label="Toggle mobile menu"
            >
              <motion.div
                animate={isMobileMenuOpen ? { rotate: 90 } : { rotate: 0 }}
                transition={{ duration: 0.2 }}
              >
                {isMobileMenuOpen ? (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                ) : (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                  </svg>
                )}
              </motion.div>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Navigation Menu */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            className="md:hidden border-t border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: 'easeInOut' }}
          >
            <div className="px-4 py-4 space-y-2">
              {/* Mobile Search */}
              {onSearch && (
                <form onSubmit={handleSearch} className="mb-4">
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                      </svg>
                    </div>
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="block w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Search destinations, services..."
                      aria-label="Search"
                    />
                  </div>
                </form>
              )}
              
              {/* Mobile Navigation Items */}
              {items.map((item, index) => {
                const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
                return (
                  <motion.button
                    key={item.id}
                    onClick={() => handleNavigation(item.href)}
                    className={cn(
                      'w-full flex items-center space-x-3 px-3 py-3 rounded-lg text-left transition-colors',
                      'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
                      isActive
                        ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
                        : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100 dark:text-gray-300 dark:hover:text-gray-100 dark:hover:bg-gray-800'
                    )}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.1 }}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    <span className="w-6 h-6">{item.icon}</span>
                    <span className="flex-1 font-medium">{item.label}</span>
                    {item.badge && item.badge > 0 && (
                      <span className="bg-red-500 text-white text-xs rounded-full px-2 py-0.5 min-w-[1.25rem] h-5 flex items-center justify-center">
                        {item.badge > 99 ? '99+' : item.badge}
                      </span>
                    )}
                  </motion.button>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  );
}

// Bottom navigation component for mobile
interface BottomNavigationProps {
  items: NavItem[];
  className?: string;
}

export function BottomNavigation({ items, className }: BottomNavigationProps) {
  const pathname = usePathname();
  const router = useRouter();

  const handleNavigation = useCallback((href: string) => {
    router.push(href);
  }, [router]);

  return (
    <nav className={cn(
      'fixed bottom-0 left-0 right-0 z-40 bg-white/95 dark:bg-gray-900/95 backdrop-blur-sm border-t border-gray-200 dark:border-gray-800 md:hidden',
      className
    )}>
      <div className="flex items-center justify-around px-2 py-2">
        {items.slice(0, 5).map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
          return (
            <motion.button
              key={item.id}
              onClick={() => handleNavigation(item.href)}
              className={cn(
                'flex flex-col items-center justify-center p-2 rounded-lg min-w-[60px] transition-colors',
                'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
                isActive
                  ? 'text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100'
              )}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              aria-current={isActive ? 'page' : undefined}
              aria-label={item.label}
            >
              <div className="relative">
                <motion.div
                  className="w-6 h-6"
                  animate={isActive ? { scale: [1, 1.2, 1] } : { scale: 1 }}
                  transition={{ duration: 0.3 }}
                >
                  {item.icon}
                </motion.div>
                {item.badge && item.badge > 0 && (
                  <motion.span
                    className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full px-1 py-0.5 min-w-[1rem] h-4 flex items-center justify-center"
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    key={item.badge}
                  >
                    {item.badge > 9 ? '9+' : item.badge}
                  </motion.span>
                )}
              </div>
              <span className={cn(
                'text-xs mt-1 transition-colors',
                isActive ? 'font-medium' : 'font-normal'
              )}>
                {item.label}
              </span>
            </motion.button>
          );
        })}
      </div>
    </nav>
  );
}

// Breadcrumb navigation
interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
  className?: string;
}

export function Breadcrumb({ items, className }: BreadcrumbProps) {
  const router = useRouter();

  const handleNavigation = useCallback((href: string) => {
    router.push(href);
  }, [router]);

  return (
    <nav className={cn('flex items-center space-x-2 text-sm', className)} aria-label="Breadcrumb">
      <ol className="flex items-center space-x-2">
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          return (
            <li key={index} className="flex items-center space-x-2">
              {index > 0 && (
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              )}
              {isLast || !item.href ? (
                <span className="text-gray-500 dark:text-gray-400" aria-current="page">
                  {item.label}
                </span>
              ) : (
                <motion.button
                  onClick={() => handleNavigation(item.href!)}
                  className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 rounded"
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  {item.label}
                </motion.button>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

// Back button with proper navigation handling
interface BackButtonProps {
  fallbackHref?: string;
  label?: string;
  className?: string;
  onBack?: () => void;
}

export function BackButton({ 
  fallbackHref = '/', 
  label = 'Back', 
  className, 
  onBack 
}: BackButtonProps) {
  const router = useRouter();

  const handleBack = useCallback(() => {
    if (onBack) {
      onBack();
      return;
    }

    // Check if there's history to go back to
    if (window.history.length > 1) {
      router.back();
    } else {
      router.push(fallbackHref);
    }
  }, [router, fallbackHref, onBack]);

  return (
    <motion.button
      onClick={handleBack}
      className={cn(
        'flex items-center space-x-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 rounded-lg p-2',
        className
      )}
      whileHover={{ scale: 1.05, x: -2 }}
      whileTap={{ scale: 0.95 }}
      aria-label={label}
    >
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
      <span className="font-medium">{label}</span>
    </motion.button>
  );
}

// Deep link handler for shared content
export function useDeepLinking() {
  const router = useRouter();
  const pathname = usePathname();

  const generateShareableLink = useCallback((path: string, params?: Record<string, string>) => {
    const url = new URL(path, window.location.origin);
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        url.searchParams.set(key, value);
      });
    }
    return url.toString();
  }, []);

  const shareCurrentPage = useCallback(async (title?: string) => {
    const url = window.location.href;
    const shareData = {
      title: title || document.title,
      url,
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
      } catch (err) {
        // Fallback to clipboard
        await navigator.clipboard.writeText(url);
      }
    } else {
      // Fallback to clipboard
      await navigator.clipboard.writeText(url);
    }
  }, []);

  const navigateToSharedContent = useCallback((path: string, params?: Record<string, string>) => {
    let targetPath = path;
    if (params) {
      const searchParams = new URLSearchParams(params);
      targetPath += `?${searchParams.toString()}`;
    }
    router.push(targetPath);
  }, [router]);

  return {
    generateShareableLink,
    shareCurrentPage,
    navigateToSharedContent,
    currentPath: pathname,
  };
}

export default {
  TopNavigation,
  BottomNavigation,
  Breadcrumb,
  BackButton,
  useDeepLinking,
};