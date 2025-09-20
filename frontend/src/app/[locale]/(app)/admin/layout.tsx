'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/state';
import { Card, CardContent } from '@/components/ui/card';
import { Shield, AlertTriangle } from 'lucide-react';

interface AdminLayoutProps {
  children: React.ReactNode;
}

export default function AdminLayout({ children }: AdminLayoutProps) {
  const router = useRouter();
  const { user, token, isAuthenticated } = useAuthStore();
  const [isClient, setIsClient] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);

  useEffect(() => {
    // Check if user is authenticated
    if (!isAuthenticated || !token) {
      isClient && router.push('/login');
      return;
    }

    // Check if user has admin role
    // Note: This assumes the user object contains roles or we can decode JWT
    // For now, we'll check if user email contains 'admin' as a simple check
    // In production, this should be properly implemented with JWT role claims
    const hasAdminRole = checkAdminRole(user, token);
    
    if (!hasAdminRole) {
      isClient && router.push('/dashboard'); // Redirect to main dashboard
      return;
    }
  }, [isAuthenticated, token, user, router]);

  // Simple admin role check - in production, decode JWT and check roles claim
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

  // Show loading or unauthorized state
  if (!isAuthenticated || !token) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Card className="w-96">
          <CardContent className="flex flex-col items-center p-6">
            <Shield className="h-12 w-12 text-gray-400 mb-4" />
            <h2 className="text-xl font-semibold mb-2">Authentication Required</h2>
            <p className="text-gray-600 text-center">Please log in to access the admin console.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const hasAdminRole = checkAdminRole(user, token);
  
  if (!hasAdminRole) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Card className="w-96">
          <CardContent className="flex flex-col items-center p-6">
            <AlertTriangle className="h-12 w-12 text-red-400 mb-4" />
            <h2 className="text-xl font-semibold mb-2">Access Denied</h2>
            <p className="text-gray-600 text-center">
              You don't have permission to access the admin console.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="border-b bg-white">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-4">
              <Shield className="h-6 w-6 text-blue-600" />
              <h1 className="text-xl font-semibold text-gray-900">Admin Console</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600">
                Welcome, {user?.firstName} {user?.lastName}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <main className="container mx-auto px-4 py-8">
        {children}
      </main>
    </div>
  );
}