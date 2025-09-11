'use client';

import { useParams } from 'next/navigation';
import { useMemo } from 'react';

// Supported locales
export const locales = ['en', 'bn'] as const;
export type Locale = typeof locales[number];

// Default locale
export const defaultLocale: Locale = 'en';

// Translation keys type
export type TranslationKey = string;

// Translation function type
export type TranslationFunction = (key: TranslationKey, params?: Record<string, string | number>) => string;

// Basic translations - in a real app, these would come from translation files
const translations: Record<Locale, Record<string, string>> = {
  en: {
    'common.loading': 'Loading...',
    'common.error': 'Error',
    'common.success': 'Success',
    'common.cancel': 'Cancel',
    'common.save': 'Save',
    'common.delete': 'Delete',
    'common.edit': 'Edit',
    'common.view': 'View',
    'common.search': 'Search',
    'common.filter': 'Filter',
    'common.sort': 'Sort',
    'nav.home': 'Home',
    'nav.trips': 'Trips',
    'nav.bookings': 'Bookings',
    'nav.profile': 'Profile',
    'trips.title': 'My Trips',
    'trips.create': 'Create Trip',
    'trips.search': 'Search trips...',
    'trips.noResults': 'No trips found',
    'bookings.title': 'My Bookings',
    'bookings.upcoming': 'Upcoming',
    'bookings.past': 'Past',
  },
  bn: {
    'common.loading': 'লোড হচ্ছে...',
    'common.error': 'ত্রুটি',
    'common.success': 'সফল',
    'common.cancel': 'বাতিল',
    'common.save': 'সংরক্ষণ',
    'common.delete': 'মুছুন',
    'common.edit': 'সম্পাদনা',
    'common.view': 'দেখুন',
    'common.search': 'অনুসন্ধান',
    'common.filter': 'ফিল্টার',
    'common.sort': 'সাজান',
    'nav.home': 'হোম',
    'nav.trips': 'ভ্রমণ',
    'nav.bookings': 'বুকিং',
    'nav.profile': 'প্রোফাইল',
    'trips.title': 'আমার ভ্রমণ',
    'trips.create': 'ভ্রমণ তৈরি করুন',
    'trips.search': 'ভ্রমণ অনুসন্ধান...',
    'trips.noResults': 'কোন ভ্রমণ পাওয়া যায়নি',
    'bookings.title': 'আমার বুকিং',
    'bookings.upcoming': 'আসন্ন',
    'bookings.past': 'অতীত',
  },
};

/**
 * Get the current locale from URL params
 */
export function useLocale(): Locale {
  const params = useParams();
  const locale = params?.locale as string;
  
  if (locale && locales.includes(locale as Locale)) {
    return locale as Locale;
  }
  
  return defaultLocale;
}

/**
 * Translation hook for client components
 */
export function useTranslation() {
  const locale = useLocale();
  
  const t = useMemo<TranslationFunction>(
    () => (key: TranslationKey, params?: Record<string, string | number>) => {
      const translation = translations[locale]?.[key] || translations[defaultLocale]?.[key] || key;
      
      if (!params) {
        return translation;
      }
      
      // Simple parameter replacement
      return Object.entries(params).reduce(
        (text, [param, value]) => text.replace(new RegExp(`{{${param}}}`, 'g'), String(value)),
        translation
      );
    },
    [locale]
  );
  
  return {
    t,
    locale,
    isRTL: false, // Add RTL support if needed
  };
}

/**
 * Get translation without hook (for use outside components)
 */
export function getTranslation(locale: Locale, key: TranslationKey, params?: Record<string, string | number>): string {
  const translation = translations[locale]?.[key] || translations[defaultLocale]?.[key] || key;
  
  if (!params) {
    return translation;
  }
  
  return Object.entries(params).reduce(
    (text, [param, value]) => text.replace(new RegExp(`{{${param}}}`, 'g'), String(value)),
    translation
  );
}

/**
 * Check if a locale is supported
 */
export function isValidLocale(locale: string): locale is Locale {
  return locales.includes(locale as Locale);
}

/**
 * Get the opposite locale (for language switching)
 */
export function getAlternateLocale(currentLocale: Locale): Locale {
  return currentLocale === 'en' ? 'bn' : 'en';
}