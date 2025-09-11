'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { languages, defaultLanguage, cookieName } from '@/lib/i18n';

interface I18nContextType {
  locale: string;
  setLocale: (locale: string) => void;
  languages: typeof languages;
}

const I18nContext = createContext<I18nContextType | undefined>(undefined);

export function I18nProvider({
  children,
  locale,
}: {
  children: React.ReactNode;
  locale: string;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const [currentLocale, setCurrentLocale] = useState(locale);

  const setLocale = (newLocale: string) => {
    // Set cookie
    document.cookie = `${cookieName}=${newLocale}; path=/; max-age=31536000`; // 1 year
    
    // Update state
    setCurrentLocale(newLocale);
    
    // Navigate to new locale
    if (!pathname) return;
    const segments = pathname.split('/');
    segments[1] = newLocale; // Replace locale segment
    const newPath = segments.join('/');
    router.push(newPath);
  };

  useEffect(() => {
    setCurrentLocale(locale);
  }, [locale]);

  return (
    <I18nContext.Provider
      value={{
        locale: currentLocale,
        setLocale,
        languages,
      }}
    >
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (context === undefined) {
    throw new Error('useI18n must be used within an I18nProvider');
  }
  return context;
}