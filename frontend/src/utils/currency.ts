import { useTranslation } from 'react-i18next';

export interface CurrencyOptions {
  currency?: string;
  locale?: string;
  minimumFractionDigits?: number;
  maximumFractionDigits?: number;
}

/**
 * Format currency using Intl.NumberFormat with BDT as default
 * @param amount - The amount to format
 * @param options - Currency formatting options
 * @returns Formatted currency string
 */
export function formatCurrency(
  amount: number,
  options: CurrencyOptions = {}
): string {
  const {
    currency = 'BDT',
    locale = 'bn-BD',
    minimumFractionDigits = 0,
    maximumFractionDigits = 2
  } = options;

  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits,
      maximumFractionDigits
    }).format(amount);
  } catch (error) {
    // Fallback to basic formatting if currency is not supported
    console.warn(`Currency ${currency} not supported, falling back to basic formatting`);
    return `${currency} ${amount.toLocaleString(locale, {
      minimumFractionDigits,
      maximumFractionDigits
    })}`;
  }
}

/**
 * React hook for currency formatting with i18n support
 */
export function useCurrencyFormatter() {
  const { i18n } = useTranslation();
  
  const formatPrice = (amount: number, currency?: string) => {
    const locale = i18n.language === 'bn' ? 'bn-BD' : 'en-US';
    return formatCurrency(amount, { 
      currency: currency || 'BDT',
      locale 
    });
  };

  return { formatPrice };
}

/**
 * Get currency symbol for a given currency code
 */
export function getCurrencySymbol(currency: string = 'BDT', locale: string = 'bn-BD'): string {
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).formatToParts(0).find(part => part.type === 'currency')?.value || currency;
  } catch (error) {
    return currency;
  }
}

/**
 * Common currency codes used in the application
 */
export const SUPPORTED_CURRENCIES = {
  BDT: 'Bangladeshi Taka',
  USD: 'US Dollar',
  EUR: 'Euro',
  GBP: 'British Pound',
  INR: 'Indian Rupee'
} as const;

export type SupportedCurrency = keyof typeof SUPPORTED_CURRENCIES;