'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuthStore } from '@/lib/state/auth';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet';
import {
  Home,
  Search,
  MapPin,
  ShoppingBag,
  MessageCircle,
  User,
  Shield,
  Menu,
  Bell,
  Settings,
  LogOut,
  CheckCircle
} from 'lucide-react';
import { VerifiedBadge } from '@/components/ui/verified-badge';
import { cn } from '@/lib/utils';
// import { SearchBar } from '@/components/search/SearchBar';
import { useTranslation } from '@/lib/i18n/client';
import { useFeatureFlag } from '@/hooks/useFeatureFlag';
import { SkipLinks, useKeyboardNavigation } from '@/components/accessibility/AccessibilityProvider';
import { AccessibilityMenu, AccessibilityShortcuts } from '@/components/accessibility/AccessibilityMenu';
import { PerformanceMonitor, PerformanceMetrics } from '@/components/performance/PerformanceMonitor';
import { DemoBanner } from '@/components/demo/DemoBanner';
import { useDemo } from '@/hooks/useDemo';

interface AppLayoutProps {
  children: React.ReactNode;
}

interface NavItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  adminOnly?: boolean;
}

const getNavigation = (t: any): NavItem[] => [
  { name: t('navigation.home'), href: '/dashboard', icon: Home },
  { name: t('navigation.explore'), href: '/explore', icon: Search },
  { name: t('navigation.heatmap'), href: '/map', icon: MapPin },
  { name: t('navigation.marketplace'), href: '/marketplace', icon: ShoppingBag },
  { name: t('navigation.messages'), href: '/messages', icon: MessageCircle },
  { name: t('navigation.profile'), href: '/profile', icon: User },
  { name: t('navigation.settings'), href: '/settings', icon: Settings },
  { name: t('navigation.providerVerification'), href: '/provider/verification', icon: CheckCircle },
  { name: t('navigation.adminConsole'), href: '/admin', icon: Shield, adminOnly: true },
];

export default function AppLayout({ children }: AppLayoutProps) {
  const pathname = usePathname();
  const { user, token, clearAuth } = useAuthStore();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { t } = useTranslation();
  const { initializeFlags } = useFeatureFlag();
  const { isDemoMode, isInitialized } = useDemo();
  
  // Initialize keyboard navigation
  useKeyboardNavigation();

  // Initialize feature flags when user is available
  React.useEffect(() => {
    if (user?.id) {
      initializeFlags(user.id);
    }
  }, [user?.id, initializeFlags]);

  // Check if user has admin role
  const checkAdminRole = (user: any, token: string): boolean => {
    if (!user || !token) return false;
    
    try {
      // Decode JWT payload (simple base64 decode - in production use proper JWT library)
      const payload = JSON.parse(atob(token.split('.')[1]));
      
      // Check if roles claim contains 'ADMIN'
      const roles = payload.roles || [];
      return roles.includes('ADMIN') || roles.includes('ROLE_ADMIN');
    } catch (error) {
      console.error('Error checking admin role:', error);
      // Fallback: check if email contains admin (temporary)
      return user.email?.toLowerCase().includes('admin') || false;
    }
  };

  const hasAdminRole = user && token ? checkAdminRole(user, token) : false;

  // Get navigation with translations and filter based on admin role
  const navigation = getNavigation(t);
  const filteredNavigation = navigation.filter(item => {
    if (item.adminOnly) {
      return hasAdminRole;
    }
    return true;
  });

  const handleLogout = () => {
    clearAuth();
  };

  const Sidebar = ({ mobile = false }: { mobile?: boolean }) => (
    <div className={cn(
      "flex flex-col h-full",
      mobile ? "" : "w-64 bg-white border-r border-gray-200"
    )}>
      {/* Logo */}
      <div className="flex items-center px-6 py-4 border-b border-gray-200">
        <Link href="/dashboard" className="flex items-center space-x-2" aria-label="HopNGo home">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">H</span>
          </div>
          <span className="text-xl font-bold text-gray-900">HopNGo</span>
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-4 py-4 space-y-2" role="navigation" aria-label="Main navigation">
        {filteredNavigation.map((item) => {
          const isActive = pathname === item.href || (pathname && pathname.startsWith(item.href + '/'));
          return (
            <Link
              key={item.name}
              href={item.href}
              onClick={() => mobile && setSidebarOpen(false)}
              className={cn(
                "flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2",
                isActive
                  ? "bg-blue-50 text-blue-700 border-r-2 border-blue-700"
                  : "text-gray-600 hover:text-gray-900 hover:bg-gray-50"
              )}
              aria-current={isActive ? 'page' : undefined}
              aria-label={item.adminOnly ? `${item.name} (Admin only)` : item.name}
            >
              <item.icon className={cn(
                "mr-3 h-5 w-5",
                isActive ? "text-blue-700" : "text-gray-400"
              )} aria-hidden="true" />
              {item.name}
              {item.adminOnly && (
                <Shield className="ml-auto h-4 w-4 text-orange-500" aria-hidden="true" />
              )}
            </Link>
          );
        })}
      </nav>

      {/* User Profile */}
      <div className="border-t border-gray-200 p-4" role="region" aria-label="User profile">
        <div className="flex items-center space-x-3 mb-3">
          <div className="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center" aria-hidden="true">
            <User className="h-4 w-4 text-gray-600" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center space-x-1">
              <p className="text-sm font-medium text-gray-900 truncate" aria-label={`User: ${user?.firstName} ${user?.lastName}`}>
                {user?.firstName} {user?.lastName}
              </p>
              <VerifiedBadge isVerified={user?.isVerified} size="sm" variant="minimal" />
            </div>
            <p className="text-xs text-gray-500 truncate" aria-label={`Email: ${user?.email}`}>
              {user?.email}
            </p>
            {hasAdminRole && (
              <p className="text-xs text-orange-600 font-medium" aria-label="Admin role">
                Admin
              </p>
            )}
          </div>
        </div>
        
        <div className="space-y-1" role="group" aria-label="User actions">
          <Link
            href="/settings"
            className="flex items-center px-2 py-1 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            onClick={() => mobile && setSidebarOpen(false)}
            aria-label="Go to settings"
          >
            <Settings className="mr-2 h-4 w-4" aria-hidden="true" />
            {t('navigation.settings')}
          </Link>
          <button
            onClick={handleLogout}
            className="flex items-center w-full px-2 py-1 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            aria-label="Log out of your account"
          >
            <LogOut className="mr-2 h-4 w-4" aria-hidden="true" />
            {t('navigation.logout')}
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <>
      <SkipLinks />
      <AccessibilityShortcuts />
      <PerformanceMonitor />
      <PerformanceMetrics />
      <DemoBanner />
      <div className={`flex h-screen bg-gray-50 ${isDemoMode ? 'pt-12' : ''}`}>
        {/* Desktop Sidebar */}
        <aside className="hidden lg:flex lg:flex-shrink-0" aria-label="Main navigation">
          <Sidebar />
        </aside>

        {/* Mobile Sidebar */}
        <Sheet open={sidebarOpen} onOpenChange={setSidebarOpen}>
          <SheetContent side="left" className="p-0 w-64" role="dialog" aria-label="Mobile navigation">
            <Sidebar mobile />
          </SheetContent>
        </Sheet>

        {/* Main Content */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Top Navigation */}
          <header className="bg-white border-b border-gray-200" role="banner">
            <div className="flex items-center justify-between px-4 py-3">
              {/* Mobile menu button */}
              <div className="lg:hidden">
                <Sheet>
                  <SheetTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setSidebarOpen(true)}
                      aria-label="Open navigation menu"
                    >
                      <Menu className="h-5 w-5" />
                    </Button>
                  </SheetTrigger>
                </Sheet>
              </div>
              
              {/* Logo - mobile only */}
              <Link href="/dashboard" className="flex items-center space-x-2 lg:hidden" aria-label="HopNGo home">
                <div className="w-6 h-6 bg-blue-600 rounded flex items-center justify-center">
                  <span className="text-white font-bold text-xs">H</span>
                </div>
                <span className="text-lg font-bold text-gray-900">HopNGo</span>
              </Link>
              
              {/* Search Bar - desktop */}
              <div className="hidden lg:flex flex-1 max-w-2xl mx-8" role="search">
                {/* <SearchBar /> */}
              </div>
              
              {/* Right side actions */}
              <div className="flex items-center space-x-2">
                <AccessibilityMenu />
                <Button variant="ghost" size="sm" aria-label="Notifications">
                  <Bell className="h-5 w-5" />
                </Button>
              </div>
            </div>
            
            {/* Mobile Search Bar */}
            <div className="lg:hidden px-4 pb-3" role="search">
              {/* <SearchBar /> */}
            </div>
          </header>

          {/* Page Content */}
          <main className="flex-1 overflow-auto" role="main" id="main-content">
            <div className="h-full">
              {children}
            </div>
          </main>
        </div>
      </div>
    </>
  );
}