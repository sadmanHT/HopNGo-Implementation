'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Mail, CheckCircle, Loader2 } from 'lucide-react';
import { useNewsletter } from '@/hooks/use-newsletter';
import { cn } from '@/lib/utils';

interface FooterNewsletterProps {
  className?: string;
  title?: string;
  description?: string;
  placeholder?: string;
  buttonText?: string;
  variant?: 'default' | 'minimal' | 'compact';
  showIcon?: boolean;
}

export function FooterNewsletter({
  className,
  title = "Stay Updated",
  description = "Get the latest travel deals and destination guides delivered to your inbox.",
  placeholder = "Enter your email",
  buttonText = "Subscribe",
  variant = 'default',
  showIcon = true,
}: FooterNewsletterProps) {
  const [email, setEmail] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);
  const { isLoading, subscribeFromFooter, validateEmail, hasSubscribed } = useNewsletter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email.trim()) {
      return;
    }

    if (!validateEmail(email)) {
      return;
    }

    const success = await subscribeFromFooter(email);
    if (success) {
      setIsSuccess(true);
      setEmail('');
      
      // Reset success state after 3 seconds
      setTimeout(() => {
        setIsSuccess(false);
      }, 3000);
    }
  };

  const isAlreadySubscribed = hasSubscribed();

  if (variant === 'minimal') {
    return (
      <div className={cn("space-y-2", className)}>
        {isSuccess ? (
          <div className="flex items-center gap-2 text-green-600 dark:text-green-400">
            <CheckCircle className="h-4 w-4" />
            <span className="text-sm">Thanks for subscribing!</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="flex gap-2">
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={placeholder}
              className="flex-1 h-9"
              disabled={isLoading || isAlreadySubscribed}
            />
            <Button 
              type="submit" 
              size="sm"
              disabled={isLoading || !email.trim() || isAlreadySubscribed}
            >
              {isLoading ? (
                <Loader2 className="h-3 w-3 animate-spin" />
              ) : (
                buttonText
              )}
            </Button>
          </form>
        )}
      </div>
    );
  }

  if (variant === 'compact') {
    return (
      <div className={cn("space-y-3", className)}>
        <h4 className="text-sm font-medium">{title}</h4>
        {isSuccess ? (
          <div className="flex items-center gap-2 text-green-600 dark:text-green-400">
            <CheckCircle className="h-4 w-4" />
            <span className="text-sm">Successfully subscribed!</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-2">
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={placeholder}
              className="h-9"
              disabled={isLoading || isAlreadySubscribed}
            />
            <Button 
              type="submit" 
              size="sm"
              className="w-full"
              disabled={isLoading || !email.trim() || isAlreadySubscribed}
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-3 w-3 animate-spin mr-2" />
                  Subscribing...
                </>
              ) : isAlreadySubscribed ? (
                'Already Subscribed'
              ) : (
                <>
                  {showIcon && <Mail className="h-3 w-3 mr-2" />}
                  {buttonText}
                </>
              )}
            </Button>
          </form>
        )}
      </div>
    );
  }

  // Default variant
  return (
    <div className={cn("space-y-4", className)}>
      <div className="space-y-2">
        <h3 className="text-lg font-semibold flex items-center gap-2">
          {showIcon && <Mail className="h-5 w-5" />}
          {title}
        </h3>
        <p className="text-sm text-muted-foreground">
          {description}
        </p>
      </div>

      {isSuccess ? (
        <div className="flex items-center gap-2 text-green-600 dark:text-green-400 p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
          <CheckCircle className="h-5 w-5" />
          <div>
            <p className="font-medium">Thanks for subscribing!</p>
            <p className="text-sm opacity-90">You'll receive our latest updates soon.</p>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="flex gap-2">
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={placeholder}
              className="flex-1"
              disabled={isLoading || isAlreadySubscribed}
            />
            <Button 
              type="submit"
              disabled={isLoading || !email.trim() || isAlreadySubscribed}
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  Subscribing...
                </>
              ) : isAlreadySubscribed ? (
                'Already Subscribed'
              ) : (
                buttonText
              )}
            </Button>
          </div>
          
          {isAlreadySubscribed && (
            <p className="text-xs text-muted-foreground">
              You're already subscribed to our newsletter.
            </p>
          )}
          
          <p className="text-xs text-muted-foreground">
            By subscribing, you agree to receive marketing emails from HopNGo. 
            You can unsubscribe at any time.
          </p>
        </form>
      )}
    </div>
  );
}

// Export variants for easy usage
export function MinimalFooterNewsletter(props: Omit<FooterNewsletterProps, 'variant'>) {
  return <FooterNewsletter {...props} variant="minimal" />;
}

export function CompactFooterNewsletter(props: Omit<FooterNewsletterProps, 'variant'>) {
  return <FooterNewsletter {...props} variant="compact" />;
}

export default FooterNewsletter;