'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Sheet,
  SheetContent,
  SheetTrigger,
} from '@/components/ui/sheet';
import {
  Search,
  Menu,
  ShoppingCart,
  MessageCircle,
  User,
  Bell,
  Heart,
  MapPin,
  Compass,
  Package,
  Settings,
  LogOut,
  ChevronDown,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/lib/state/auth';
import { useCartStore } from '@/stores/cartStore';

interface NavItem {
  name: string;
  nameBn?: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: number;
}

const navigationItems: NavItem[] = [
  { name: 'Explore', nameBn: 'অন্বেষণ', href: '/explore', icon: Compass },
  { name: 'Map', nameBn: 'মানচিত্র', href: '/map', icon: MapPin },
  { name: 'Marketplace', nameBn: 'বাজার', href: '/marketplace', icon: Package },
  { name: 'Messages', nameBn: 'বার্তা', href: '/messages', icon: MessageCircle },
];

interface BdNavbarProps {
  className?: string;
}

export function BdNavbar({ className }: BdNavbarProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearAuth } = useAuthStore();
  const { items } = useCartStore();
  const [isScrolled, setIsScrolled] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Handle scroll effect
  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery('');
    }
  };

  const handleLogout = () => {
    clearAuth();
    router.push('/');
  };

  const cartItemCount = items.reduce((total, item) => total + item.quantity, 0);

  return (
    <motion.nav
      className={cn(
        "sticky top-0 z-50 w-full transition-all duration-300",
        isScrolled
          ? "bg-white/80 backdrop-blur-md shadow-lg border-b border-white/20"
          : "bg-white/95 backdrop-blur-sm border-b border-gray-200/50",
        className
      )}
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <motion.div
            className="flex items-center space-x-3"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
          >
            <Link href="/" className="flex items-center space-x-3 group">
              <div className="relative">
                <div className="w-10 h-10 bg-gradient-to-br from-bd-green to-bd-teal rounded-xl flex items-center justify-center shadow-lg group-hover:shadow-xl transition-shadow duration-300">
                  <span className="text-white font-bold text-lg font-bengali">হ</span>
                </div>
                <div className="absolute -inset-1 bg-gradient-to-br from-bd-green/20 to-bd-teal/20 rounded-xl blur opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              </div>
              <div className="hidden sm:block">
                <div className="text-xl font-bold text-bd-slate group-hover:text-bd-green transition-colors duration-200">
                  HopNGo
                </div>
                <div className="text-xs text-muted-foreground font-bengali -mt-1">
                  ভ্রমণের সাথী
                </div>
              </div>
            </Link>
          </motion.div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-1">
            {navigationItems.map((item) => {
              const isActive = pathname === item.href || (pathname && pathname.startsWith(item.href + '/'));
              return (
                <motion.div key={item.name} whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                  <Link
                    href={item.href}
                    className={cn(
                      "relative flex items-center space-x-2 px-3 py-2 rounded-xl text-sm font-medium transition-all duration-200",
                      "focus:outline-none focus:ring-2 focus:ring-bd-green/50 focus:ring-offset-2",
                      isActive
                        ? "bg-bd-green/10 text-bd-green shadow-sm"
                        : "text-bd-slate hover:text-bd-green hover:bg-bd-green/5"
                    )}
                  >
                    <item.icon className="w-4 h-4" />
                    <span className="hidden lg:block">{item.name}</span>
                    <span className="hidden xl:block text-xs text-muted-foreground font-bengali">
                      {item.nameBn}
                    </span>
                    {item.badge && item.badge > 0 && (
                      <Badge className="bg-bd-coral text-white text-xs px-1.5 py-0.5 min-w-[1.25rem] h-5">
                        {item.badge > 99 ? '99+' : item.badge}
                      </Badge>
                    )}
                    {isActive && (
                      <motion.div
                        className="absolute bottom-0 left-1/2 w-1 h-1 bg-bd-green rounded-full"
                        layoutId="activeIndicator"
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 300, damping: 30 }}
                        style={{ x: '-50%' }}
                      />
                    )}
                  </Link>
                </motion.div>
              );
            })}
          </div>

          {/* Search Bar */}
          <motion.form
            onSubmit={handleSearch}
            className="hidden md:flex items-center flex-1 max-w-md mx-8"
            animate={isSearchFocused ? { scale: 1.02 } : { scale: 1 }}
            transition={{ duration: 0.2 }}
          >
            <div className="relative w-full">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => setIsSearchFocused(true)}
                onBlur={() => setIsSearchFocused(false)}
                placeholder="Search destinations, experiences... / গন্তব্য খুঁজুন..."
                className={cn(
                  "pl-10 pr-4 py-2 w-full rounded-xl border-0 transition-all duration-200",
                  "bg-white/60 backdrop-blur-sm shadow-sm",
                  "focus:bg-white focus:shadow-md focus:ring-2 focus:ring-bd-green/50",
                  "placeholder:text-muted-foreground/70 font-bengali"
                )}
              />
            </div>
          </motion.form>

          {/* Right Actions */}
          <div className="flex items-center space-x-2">
            {/* Cart */}
            <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Button
                variant="ghost"
                size="sm"
                className="relative p-2 hover:bg-bd-green/10 hover:text-bd-green transition-colors duration-200"
                asChild
              >
                <Link href="/cart">
                  <ShoppingCart className="h-5 w-5" />
                  {cartItemCount > 0 && (
                    <Badge className="absolute -top-1 -right-1 bg-bd-coral text-white text-xs px-1.5 py-0.5 min-w-[1.25rem] h-5">
                      {cartItemCount > 99 ? '99+' : cartItemCount}
                    </Badge>
                  )}
                </Link>
              </Button>
            </motion.div>

            {/* Favorites */}
            <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Button
                variant="ghost"
                size="sm"
                className="p-2 hover:bg-bd-coral/10 hover:text-bd-coral transition-colors duration-200"
                asChild
              >
                <Link href="/favorites">
                  <Heart className="h-5 w-5" />
                </Link>
              </Button>
            </motion.div>

            {/* Notifications */}
            <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Button
                variant="ghost"
                size="sm"
                className="p-2 hover:bg-bd-sunrise/10 hover:text-bd-sunrise transition-colors duration-200"
              >
                <Bell className="h-5 w-5" />
              </Button>
            </motion.div>

            {/* User Menu */}
            {user ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                    <Button
                      variant="ghost"
                      className="flex items-center space-x-2 px-3 py-2 hover:bg-bd-green/10 transition-colors duration-200"
                    >
                      <div className="w-8 h-8 bg-gradient-to-br from-bd-green to-bd-teal rounded-full flex items-center justify-center">
                        <User className="h-4 w-4 text-white" />
                      </div>
                      <ChevronDown className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  </motion.div>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-56 bg-white/95 backdrop-blur-sm border-white/20">
                  <div className="px-3 py-2 border-b border-gray-200/50">
                    <p className="font-medium text-bd-slate">{user.firstName} {user.lastName}</p>
                    <p className="text-sm text-muted-foreground">{user.email}</p>
                  </div>
                  <DropdownMenuItem asChild>
                    <Link href="/profile" className="flex items-center space-x-2">
                      <User className="h-4 w-4" />
                      <span>Profile / প্রোফাইল</span>
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild>
                    <Link href="/settings" className="flex items-center space-x-2">
                      <Settings className="h-4 w-4" />
                      <span>Settings / সেটিংস</span>
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={handleLogout} className="text-red-600 focus:text-red-600">
                    <LogOut className="h-4 w-4 mr-2" />
                    <span>Logout / লগআউট</span>
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <div className="flex items-center space-x-2">
                <Button variant="ghost" size="sm" asChild>
                  <Link href="/login">Login / লগইন</Link>
                </Button>
                <Button size="sm" className="bg-bd-green hover:bg-bd-green/90" asChild>
                  <Link href="/register">Sign Up / নিবন্ধন</Link>
                </Button>
              </div>
            )}

            {/* Mobile Menu */}
            <div className="md:hidden">
              <Sheet open={isMobileMenuOpen} onOpenChange={setIsMobileMenuOpen}>
                <SheetTrigger asChild>
                  <Button variant="ghost" size="sm" className="p-2">
                    <Menu className="h-5 w-5" />
                  </Button>
                </SheetTrigger>
                <SheetContent side="right" className="w-80 bg-white/95 backdrop-blur-md">
                  <div className="flex flex-col h-full">
                    {/* Mobile Search */}
                    <form onSubmit={handleSearch} className="mb-6">
                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          type="text"
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                          placeholder="Search... / খুঁজুন..."
                          className="pl-10 font-bengali"
                        />
                      </div>
                    </form>

                    {/* Mobile Navigation */}
                    <nav className="flex-1 space-y-2">
                      {navigationItems.map((item, index) => {
                        const isActive = pathname === item.href || (pathname && pathname.startsWith(item.href + '/'));
                        return (
                          <motion.div
                            key={item.name}
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            transition={{ delay: index * 0.1 }}
                          >
                            <Link
                              href={item.href}
                              onClick={() => setIsMobileMenuOpen(false)}
                              className={cn(
                                "flex items-center space-x-3 px-4 py-3 rounded-xl transition-colors duration-200",
                                isActive
                                  ? "bg-bd-green/10 text-bd-green"
                                  : "text-bd-slate hover:bg-bd-green/5 hover:text-bd-green"
                              )}
                            >
                              <item.icon className="w-5 h-5" />
                              <div className="flex-1">
                                <div className="font-medium">{item.name}</div>
                                <div className="text-sm text-muted-foreground font-bengali">{item.nameBn}</div>
                              </div>
                              {item.badge && item.badge > 0 && (
                                <Badge className="bg-bd-coral text-white">
                                  {item.badge > 99 ? '99+' : item.badge}
                                </Badge>
                              )}
                            </Link>
                          </motion.div>
                        );
                      })}
                    </nav>
                  </div>
                </SheetContent>
              </Sheet>
            </div>
          </div>
        </div>
      </div>
    </motion.nav>
  );
}