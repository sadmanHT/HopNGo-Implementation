'use client';

import React from 'react';
import { AlertCircle, RefreshCw, HelpCircle, ExternalLink } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
  showHelpLink?: boolean;
  helpContext?: 'booking' | 'payment' | 'general' | 'auth' | 'profile';
  className?: string;
}

const getHelpArticleSlug = (context: string): string => {
  const helpMap: Record<string, string> = {
    booking: 'booking-issues',
    payment: 'payment-problems',
    auth: 'login-issues',
    profile: 'account-problems',
    general: 'common-issues'
  };
  return helpMap[context] || 'common-issues';
};

const getHelpTitle = (context: string): string => {
  const titleMap: Record<string, string> = {
    booking: 'Booking Help',
    payment: 'Payment Support',
    auth: 'Login Help',
    profile: 'Account Support',
    general: 'General Help'
  };
  return titleMap[context] || 'Get Help';
};

export default function ErrorMessage({
  message,
  onRetry,
  showHelpLink = true,
  helpContext = 'general',
  className = ''
}: ErrorMessageProps) {
  const handleContactSupport = () => {
    // Open contact form in new tab/window
    window.open('/support/contact', '_blank');
  };

  const handleViewHelp = () => {
    // Navigate to relevant help article
    const slug = getHelpArticleSlug(helpContext);
    window.open(`/help/${slug}`, '_blank');
  };

  return (
    <Card className={`border-red-200 bg-red-50 ${className}`}>
      <CardContent className="p-4">
        <div className="flex items-start space-x-3">
          <AlertCircle className="h-5 w-5 text-red-600 mt-0.5 flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-medium text-red-800 mb-1">
              Something went wrong
            </h3>
            <p className="text-sm text-red-700 mb-3">
              {message}
            </p>
            
            <div className="flex flex-wrap gap-2">
              {onRetry && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={onRetry}
                  className="border-red-300 text-red-700 hover:bg-red-100"
                >
                  <RefreshCw className="h-4 w-4 mr-1" />
                  Try Again
                </Button>
              )}
              
              {showHelpLink && (
                <>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleViewHelp}
                    className="border-blue-300 text-blue-700 hover:bg-blue-50"
                  >
                    <HelpCircle className="h-4 w-4 mr-1" />
                    {getHelpTitle(helpContext)}
                    <ExternalLink className="h-3 w-3 ml-1" />
                  </Button>
                  
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleContactSupport}
                    className="border-green-300 text-green-700 hover:bg-green-50"
                  >
                    <HelpCircle className="h-4 w-4 mr-1" />
                    Contact Support
                    <ExternalLink className="h-3 w-3 ml-1" />
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// Specialized error components for common use cases
export function BookingErrorMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <ErrorMessage
      message={message}
      onRetry={onRetry}
      helpContext="booking"
      className="mb-4"
    />
  );
}

export function PaymentErrorMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <ErrorMessage
      message={message}
      onRetry={onRetry}
      helpContext="payment"
      className="mb-4"
    />
  );
}

export function AuthErrorMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <ErrorMessage
      message={message}
      onRetry={onRetry}
      helpContext="auth"
      className="mb-4"
    />
  );
}