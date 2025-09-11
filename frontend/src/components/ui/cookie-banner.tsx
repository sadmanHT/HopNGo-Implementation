'use client';

import React, { useState, useEffect } from 'react';
import { X, Cookie, Settings, Shield, BarChart3, Target, Wrench } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { cookieConsentService, CookiePreferences, ConsentState } from '@/lib/services/cookie-consent';
import Link from 'next/link';

interface CookieBannerProps {
  className?: string;
}

interface CookieCategory {
  key: keyof CookiePreferences;
  title: string;
  description: string;
  icon: React.ReactNode;
  required?: boolean;
  examples: string[];
}

const cookieCategories: CookieCategory[] = [
  {
    key: 'essential',
    title: 'Essential Cookies',
    description: 'Required for the website to function properly. These cannot be disabled.',
    icon: <Shield className="h-4 w-4" />,
    required: true,
    examples: ['Authentication', 'Security', 'Session management', 'Load balancing'],
  },
  {
    key: 'functional',
    title: 'Functional Cookies',
    description: 'Enable enhanced functionality and personalization.',
    icon: <Wrench className="h-4 w-4" />,
    examples: ['Language preferences', 'Region settings', 'Accessibility options'],
  },
  {
    key: 'analytics',
    title: 'Analytics Cookies',
    description: 'Help us understand how visitors interact with our website.',
    icon: <BarChart3 className="h-4 w-4" />,
    examples: ['Google Analytics', 'Page views', 'User behavior', 'Performance metrics'],
  },
  {
    key: 'marketing',
    title: 'Marketing Cookies',
    description: 'Used to deliver relevant advertisements and track campaign effectiveness.',
    icon: <Target className="h-4 w-4" />,
    examples: ['Ad targeting', 'Social media integration', 'Campaign tracking'],
  },
];

export function CookieBanner({ className }: CookieBannerProps) {
  const [showBanner, setShowBanner] = useState(false);
  const [showPreferences, setShowPreferences] = useState(false);
  const [preferences, setPreferences] = useState<CookiePreferences>({
    essential: true,
    functional: false,
    analytics: false,
    marketing: false,
  });
  const [consentState, setConsentState] = useState<ConsentState | null>(null);

  useEffect(() => {
    // Check if banner should be shown
    setShowBanner(cookieConsentService.shouldShowBanner());
    setConsentState(cookieConsentService.getConsentState());

    // Subscribe to consent changes
    const unsubscribe = cookieConsentService.subscribe((state) => {
      setConsentState(state);
      setShowBanner(false);
    });

    return unsubscribe;
  }, []);

  const handleAcceptAll = () => {
    cookieConsentService.acceptAll();
    setShowBanner(false);
  };

  const handleRejectAll = () => {
    cookieConsentService.rejectAll();
    setShowBanner(false);
  };

  const handleSavePreferences = () => {
    cookieConsentService.setPreferences(preferences);
    setShowPreferences(false);
    setShowBanner(false);
  };

  const handlePreferenceChange = (category: keyof CookiePreferences, enabled: boolean) => {
    if (category === 'essential') return; // Cannot disable essential cookies
    
    setPreferences(prev => ({
      ...prev,
      [category]: enabled,
    }));
  };

  const openPreferences = () => {
    // Load current preferences if they exist
    const currentState = cookieConsentService.getConsentState();
    if (currentState) {
      setPreferences(currentState.preferences);
    }
    setShowPreferences(true);
  };

  if (!showBanner) {
    return null;
  }

  return (
    <>
      {/* Cookie Banner */}
      <div className={`fixed bottom-0 left-0 right-0 z-50 bg-background border-t shadow-lg ${className}`}>
        <div className="container mx-auto p-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
            <div className="flex items-center gap-2 flex-shrink-0">
              <Cookie className="h-5 w-5 text-primary" />
              <span className="font-semibold text-sm">Cookie Preferences</span>
            </div>
            
            <div className="flex-1 text-sm text-muted-foreground">
              <p>
                We use cookies to enhance your experience, analyze site usage, and assist in marketing efforts.
                {' '}
                <Link href="/legal/cookies" className="text-primary hover:underline">
                  Learn more
                </Link>
                {' '}
                about our cookie policy.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-2 flex-shrink-0">
              <Button
                variant="outline"
                size="sm"
                onClick={openPreferences}
                className="flex items-center gap-1"
              >
                <Settings className="h-3 w-3" />
                Customize
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleRejectAll}
              >
                Reject All
              </Button>
              <Button
                size="sm"
                onClick={handleAcceptAll}
              >
                Accept All
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Cookie Preferences Dialog */}
      <Dialog open={showPreferences} onOpenChange={setShowPreferences}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Cookie className="h-5 w-5" />
              Cookie Preferences
            </DialogTitle>
            <DialogDescription>
              Choose which cookies you want to allow. You can change these settings at any time.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {cookieCategories.map((category) => {
              const isEnabled = preferences[category.key];
              const isRequired = category.required;

              return (
                <Card key={category.key} className="relative">
                  <CardHeader className="pb-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {category.icon}
                        <CardTitle className="text-base">{category.title}</CardTitle>
                        {isRequired && (
                          <Badge variant="secondary" className="text-xs">
                            Required
                          </Badge>
                        )}
                      </div>
                      <Switch
                        checked={isEnabled}
                        onCheckedChange={(checked) => handlePreferenceChange(category.key, checked)}
                        disabled={isRequired}
                      />
                    </div>
                    <CardDescription className="text-sm">
                      {category.description}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="pt-0">
                    <div className="text-xs text-muted-foreground">
                      <span className="font-medium">Examples:</span>
                      <div className="flex flex-wrap gap-1 mt-1">
                        {category.examples.map((example, index) => (
                          <Badge key={index} variant="outline" className="text-xs">
                            {example}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>

          <div className="flex flex-col sm:flex-row gap-2 pt-4 border-t">
            <div className="flex-1 text-xs text-muted-foreground">
              <p>
                For more information, read our{' '}
                <Link href="/legal/privacy" className="text-primary hover:underline">
                  Privacy Policy
                </Link>
                {' '}and{' '}
                <Link href="/legal/cookies" className="text-primary hover:underline">
                  Cookie Policy
                </Link>
                .
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => setShowPreferences(false)}
              >
                Cancel
              </Button>
              <Button onClick={handleSavePreferences}>
                Save Preferences
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

// Cookie Status Component (for settings page)
export function CookieStatus() {
  const [consentState, setConsentState] = useState<ConsentState | null>(null);
  const [showPreferences, setShowPreferences] = useState(false);

  useEffect(() => {
    setConsentState(cookieConsentService.getConsentState());

    const unsubscribe = cookieConsentService.subscribe((state) => {
      setConsentState(state);
    });

    return unsubscribe;
  }, []);

  if (!consentState) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Cookie className="h-4 w-4" />
            Cookie Preferences
          </CardTitle>
          <CardDescription>
            No cookie preferences have been set.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button onClick={() => setShowPreferences(true)}>
            Set Cookie Preferences
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Cookie className="h-4 w-4" />
            Cookie Preferences
          </CardTitle>
          <CardDescription>
            Manage your cookie preferences and data collection settings.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="font-medium">Status:</span>
              <p className="text-muted-foreground">{cookieConsentService.getConsentSummary()}</p>
            </div>
            <div>
              <span className="font-medium">Consent Date:</span>
              <p className="text-muted-foreground">
                {new Date(consentState.consentDate).toLocaleDateString()}
              </p>
            </div>
            <div>
              <span className="font-medium">Expires:</span>
              <p className="text-muted-foreground">
                {cookieConsentService.getDaysUntilExpiry()} days
              </p>
            </div>
            <div>
              <span className="font-medium">Version:</span>
              <p className="text-muted-foreground">{consentState.version}</p>
            </div>
          </div>

          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => setShowPreferences(true)}
            >
              <Settings className="h-4 w-4 mr-1" />
              Modify Preferences
            </Button>
            <Button
              variant="outline"
              onClick={() => {
                cookieConsentService.clearConsent();
                window.location.reload();
              }}
            >
              Reset All
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Reuse the preferences dialog */}
      <Dialog open={showPreferences} onOpenChange={setShowPreferences}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Cookie className="h-5 w-5" />
              Update Cookie Preferences
            </DialogTitle>
            <DialogDescription>
              Modify your cookie preferences. Changes will take effect immediately.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {cookieCategories.map((category) => {
              const isEnabled = consentState?.preferences[category.key] ?? false;
              const isRequired = category.required;

              return (
                <Card key={category.key}>
                  <CardHeader className="pb-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {category.icon}
                        <CardTitle className="text-base">{category.title}</CardTitle>
                        {isRequired && (
                          <Badge variant="secondary" className="text-xs">
                            Required
                          </Badge>
                        )}
                        {isEnabled && !isRequired && (
                          <Badge variant="default" className="text-xs">
                            Enabled
                          </Badge>
                        )}
                      </div>
                      <Switch
                        checked={isEnabled}
                        onCheckedChange={(checked) => {
                          if (!isRequired) {
                            cookieConsentService.setPreferences({
                              [category.key]: checked,
                            });
                          }
                        }}
                        disabled={isRequired}
                      />
                    </div>
                    <CardDescription className="text-sm">
                      {category.description}
                    </CardDescription>
                  </CardHeader>
                </Card>
              );
            })}
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button
              variant="outline"
              onClick={() => setShowPreferences(false)}
            >
              Close
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

export default CookieBanner;