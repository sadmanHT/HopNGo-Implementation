'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { X, Mail, Gift, CheckCircle, Loader2 } from 'lucide-react';
import { useNewsletter } from '@/hooks/use-newsletter';
import { cn } from '@/lib/utils';

interface NewsletterPopupProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  subtitle?: string;
  description?: string;
  offer?: string;
  placeholder?: string;
  buttonText?: string;
  showOffer?: boolean;
  className?: string;
}

export function NewsletterPopup({
  isOpen,
  onClose,
  title = "Don't Miss Out!",
  subtitle = "Get Exclusive Travel Deals",
  description = "Subscribe to our newsletter and be the first to know about amazing travel deals, destination guides, and exclusive offers.",
  offer = "Get 10% off your first booking!",
  placeholder = "Enter your email address",
  buttonText = "Get My Discount",
  showOffer = true,
  className,
}: NewsletterPopupProps) {
  const [email, setEmail] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);
  const { isLoading, subscribeFromPopup, validateEmail, hasSubscribed } = useNewsletter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email.trim()) {
      return;
    }

    if (!validateEmail(email)) {
      return;
    }

    const success = await subscribeFromPopup(email);
    if (success) {
      setIsSuccess(true);
      setEmail('');
      
      // Close popup after showing success for 2 seconds
      setTimeout(() => {
        onClose();
        setIsSuccess(false);
      }, 2000);
    }
  };

  const handleClose = () => {
    onClose();
    setIsSuccess(false);
    setEmail('');
  };

  const isAlreadySubscribed = hasSubscribed();

  // Don't show popup if user already subscribed
  useEffect(() => {
    if (isAlreadySubscribed && isOpen) {
      onClose();
    }
  }, [isAlreadySubscribed, isOpen, onClose]);

  return (
    <Dialog open={isOpen && !isAlreadySubscribed} onOpenChange={handleClose}>
      <DialogContent className={cn("sm:max-w-md", className)}>
        <DialogHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              {showOffer ? (
                <Gift className="h-6 w-6 text-primary" />
              ) : (
                <Mail className="h-6 w-6 text-primary" />
              )}
              <DialogTitle className="text-xl font-bold">{title}</DialogTitle>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClose}
              className="h-6 w-6 p-0"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </DialogHeader>

        <div className="space-y-6">
          {/* Header */}
          <div className="text-center space-y-2">
            <h3 className="text-lg font-semibold text-primary">{subtitle}</h3>
            <p className="text-sm text-muted-foreground">{description}</p>
            {showOffer && (
              <div className="bg-gradient-to-r from-primary/10 to-secondary/10 p-3 rounded-lg">
                <p className="font-semibold text-primary">{offer}</p>
              </div>
            )}
          </div>

          {/* Success State */}
          {isSuccess ? (
            <div className="text-center space-y-4">
              <div className="flex justify-center">
                <div className="bg-green-100 dark:bg-green-900/20 p-3 rounded-full">
                  <CheckCircle className="h-8 w-8 text-green-600 dark:text-green-400" />
                </div>
              </div>
              <div>
                <h4 className="font-semibold text-green-600 dark:text-green-400">
                  Welcome to HopNGo!
                </h4>
                <p className="text-sm text-muted-foreground">
                  Check your email for your discount code and travel inspiration.
                </p>
              </div>
            </div>
          ) : (
            /* Subscription Form */
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={placeholder}
                  className="text-center"
                  disabled={isLoading}
                  autoFocus
                />
              </div>
              
              <Button 
                type="submit" 
                className="w-full"
                disabled={isLoading || !email.trim()}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                    Subscribing...
                  </>
                ) : (
                  <>
                    <Mail className="h-4 w-4 mr-2" />
                    {buttonText}
                  </>
                )}
              </Button>
              
              <p className="text-xs text-center text-muted-foreground">
                By subscribing, you agree to receive marketing emails from HopNGo. 
                You can unsubscribe at any time.
              </p>
            </form>
          )}

          {/* Features */}
          {!isSuccess && (
            <div className="space-y-3">
              <div className="flex items-center gap-3 text-sm">
                <div className="bg-primary/10 p-1 rounded">
                  <Mail className="h-3 w-3 text-primary" />
                </div>
                <span>Exclusive travel deals and offers</span>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <div className="bg-primary/10 p-1 rounded">
                  <Gift className="h-3 w-3 text-primary" />
                </div>
                <span>Destination guides and travel tips</span>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <div className="bg-primary/10 p-1 rounded">
                  <CheckCircle className="h-3 w-3 text-primary" />
                </div>
                <span>No spam, unsubscribe anytime</span>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

// Hook for managing popup display logic
export function useNewsletterPopup() {
  const [isOpen, setIsOpen] = useState(false);
  const { hasSubscribed } = useNewsletter();

  useEffect(() => {
    // Don't show if user already subscribed
    if (hasSubscribed()) {
      return;
    }

    // Check if popup was already shown today
    const lastShown = localStorage.getItem('newsletter-popup-shown');
    const today = new Date().toDateString();
    
    if (lastShown === today) {
      return;
    }

    // Show popup after 30 seconds on page
    const timer = setTimeout(() => {
      setIsOpen(true);
      localStorage.setItem('newsletter-popup-shown', today);
    }, 30000);

    return () => clearTimeout(timer);
  }, [hasSubscribed]);

  const closePopup = () => {
    setIsOpen(false);
  };

  const showPopup = () => {
    if (!hasSubscribed()) {
      setIsOpen(true);
    }
  };

  return {
    isOpen,
    closePopup,
    showPopup,
  };
}

// Exit-intent popup variant
export function ExitIntentNewsletterPopup(props: Omit<NewsletterPopupProps, 'isOpen' | 'onClose'>) {
  const [isOpen, setIsOpen] = useState(false);
  const { hasSubscribed } = useNewsletter();

  useEffect(() => {
    if (hasSubscribed()) {
      return;
    }

    let hasShown = false;
    
    const handleMouseLeave = (e: MouseEvent) => {
      if (e.clientY <= 0 && !hasShown) {
        setIsOpen(true);
        hasShown = true;
      }
    };

    document.addEventListener('mouseleave', handleMouseLeave);
    
    return () => {
      document.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, [hasSubscribed]);

  return (
    <NewsletterPopup
      {...props}
      isOpen={isOpen}
      onClose={() => setIsOpen(false)}
      title="Wait! Don't Leave Yet!"
      subtitle="Get Your Travel Discount"
    />
  );
}

export default NewsletterPopup;