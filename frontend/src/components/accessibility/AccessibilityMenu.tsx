'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { AccessibilityIcon, Settings, Eye, Type, Contrast } from 'lucide-react';
import { useAccessibility } from './AccessibilityProvider';

export function AccessibilityMenu() {
  const {
    preferences,
    updatePreferences,
    resetPreferences,
  } = useAccessibility();

  const [isOpen, setIsOpen] = useState(false);

  const toggleReducedMotion = () => {
    updatePreferences({ reducedMotion: !preferences.reducedMotion });
  };

  const toggleHighContrast = () => {
    updatePreferences({ highContrast: !preferences.highContrast });
  };

  const cycleFontSize = () => {
    const sizes = ['normal', 'large', 'extra-large'] as const;
    const currentIndex = sizes.indexOf(preferences.fontSize);
    const nextIndex = (currentIndex + 1) % sizes.length;
    updatePreferences({ fontSize: sizes[nextIndex] });
  };

  return (
    <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          aria-label="Accessibility settings"
        >
          <AccessibilityIcon className="h-5 w-5" />
          <span className="sr-only">Accessibility Settings</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="w-64"
        role="menu"
        aria-label="Accessibility options"
      >
        <DropdownMenuLabel className="flex items-center gap-2">
          <Settings className="h-4 w-4" aria-hidden="true" />
          Accessibility Settings
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        
        <DropdownMenuItem
          onClick={toggleReducedMotion}
          className="flex items-center justify-between cursor-pointer focus:bg-blue-50 focus:text-blue-900"
          role="menuitemcheckbox"
          aria-checked={preferences.reducedMotion}
        >
          <div className="flex items-center gap-2">
            <Eye className="h-4 w-4" aria-hidden="true" />
            <span>Reduce Motion</span>
          </div>
          <div className="flex items-center">
            <div
              className={`w-4 h-4 rounded border-2 flex items-center justify-center ${
                preferences.reducedMotion
                  ? 'bg-blue-600 border-blue-600'
                  : 'border-gray-300'
              }`}
              aria-hidden="true"
            >
              {preferences.reducedMotion && (
                <div className="w-2 h-2 bg-white rounded-sm" />
              )}
            </div>
          </div>
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={toggleHighContrast}
          className="flex items-center justify-between cursor-pointer focus:bg-blue-50 focus:text-blue-900"
          role="menuitemcheckbox"
          aria-checked={preferences.highContrast}
        >
          <div className="flex items-center gap-2">
            <Contrast className="h-4 w-4" aria-hidden="true" />
            <span>High Contrast</span>
          </div>
          <div className="flex items-center">
            <div
              className={`w-4 h-4 rounded border-2 flex items-center justify-center ${
                preferences.highContrast
                  ? 'bg-blue-600 border-blue-600'
                  : 'border-gray-300'
              }`}
              aria-hidden="true"
            >
              {preferences.highContrast && (
                <div className="w-2 h-2 bg-white rounded-sm" />
              )}
            </div>
          </div>
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={cycleFontSize}
          className="flex items-center justify-between cursor-pointer focus:bg-blue-50 focus:text-blue-900"
          role="menuitem"
        >
          <div className="flex items-center gap-2">
            <Type className="h-4 w-4" aria-hidden="true" />
            <span>Font Size</span>
          </div>
          <span className="text-sm text-gray-500 capitalize">
            {preferences.fontSize.replace('-', ' ')}
          </span>
        </DropdownMenuItem>

        <DropdownMenuSeparator />
        
        <DropdownMenuItem
          onClick={resetPreferences}
          className="cursor-pointer focus:bg-blue-50 focus:text-blue-900"
          role="menuitem"
        >
          Reset to Defaults
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

// Keyboard shortcut component for accessibility
export function AccessibilityShortcuts() {
  const { updatePreferences, preferences } = useAccessibility();

  React.useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Alt + Shift + M: Toggle reduced motion
      if (event.altKey && event.shiftKey && event.key === 'M') {
        event.preventDefault();
        updatePreferences({ reducedMotion: !preferences.reducedMotion });
      }
      
      // Alt + Shift + C: Toggle high contrast
      if (event.altKey && event.shiftKey && event.key === 'C') {
        event.preventDefault();
        updatePreferences({ highContrast: !preferences.highContrast });
      }
      
      // Alt + Shift + F: Cycle font size
      if (event.altKey && event.shiftKey && event.key === 'F') {
        event.preventDefault();
        const sizes = ['normal', 'large', 'extra-large'] as const;
        const currentIndex = sizes.indexOf(preferences.fontSize);
        const nextIndex = (currentIndex + 1) % sizes.length;
        updatePreferences({ fontSize: sizes[nextIndex] });
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [preferences, updatePreferences]);

  return null;
}