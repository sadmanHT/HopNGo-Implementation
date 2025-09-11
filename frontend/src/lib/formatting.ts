import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import 'dayjs/locale/bn';

// Extend dayjs with plugins
dayjs.extend(utc);
dayjs.extend(timezone);

// Default timezone for Bangladesh
export const DEFAULT_TIMEZONE = 'Asia/Dhaka';

// Currency formatting
export interface CurrencyOptions {
  currency?: string;
  locale?: string;
  minimumFractionDigits?: number;
  maximumFractionDigits?: number;
}

export function formatCurrency(
  amount: number,
  options: CurrencyOptions = {}
): string {
  const {
    currency = 'BDT',
    locale = 'bn-BD',
    minimumFractionDigits = 0,
    maximumFractionDigits = 2,
  } = options;

  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits,
      maximumFractionDigits,
    }).format(amount);
  } catch (error) {
    // Fallback for unsupported currencies
    const currencySymbols: Record<string, string> = {
      BDT: '৳',
      USD: '$',
      EUR: '€',
      GBP: '£',
    };
    
    const symbol = currencySymbols[currency] || currency;
    const formattedNumber = new Intl.NumberFormat(locale, {
      minimumFractionDigits,
      maximumFractionDigits,
    }).format(amount);
    
    return `${symbol}${formattedNumber}`;
  }
}

// Date formatting
export interface DateOptions {
  format?: string;
  locale?: string;
  timezone?: string;
}

export function formatDate(
  date: string | Date | dayjs.Dayjs,
  options: DateOptions = {}
): string {
  const {
    format = 'DD MMMM YYYY',
    locale = 'bn',
    timezone = DEFAULT_TIMEZONE,
  } = options;

  return dayjs(date)
    .tz(timezone)
    .locale(locale)
    .format(format);
}

export function formatDateTime(
  date: string | Date | dayjs.Dayjs,
  options: DateOptions = {}
): string {
  const {
    format = 'DD MMMM YYYY, hh:mm A',
    locale = 'bn',
    timezone = DEFAULT_TIMEZONE,
  } = options;

  return dayjs(date)
    .tz(timezone)
    .locale(locale)
    .format(format);
}

export function formatRelativeTime(
  date: string | Date | dayjs.Dayjs,
  options: { locale?: string; timezone?: string } = {}
): string {
  const {
    locale = 'bn',
    timezone = DEFAULT_TIMEZONE,
  } = options;

  const now = dayjs().tz(timezone);
  const targetDate = dayjs(date).tz(timezone);
  
  const diffInMinutes = now.diff(targetDate, 'minute');
  const diffInHours = now.diff(targetDate, 'hour');
  const diffInDays = now.diff(targetDate, 'day');

  if (diffInMinutes < 1) {
    return locale === 'bn' ? 'এখনই' : 'just now';
  } else if (diffInMinutes < 60) {
    return locale === 'bn' 
      ? `${diffInMinutes} মিনিট আগে`
      : `${diffInMinutes} minutes ago`;
  } else if (diffInHours < 24) {
    return locale === 'bn'
      ? `${diffInHours} ঘন্টা আগে`
      : `${diffInHours} hours ago`;
  } else if (diffInDays < 7) {
    return locale === 'bn'
      ? `${diffInDays} দিন আগে`
      : `${diffInDays} days ago`;
  } else {
    return formatDate(date, { locale, timezone });
  }
}

// Number formatting
export function formatNumber(
  number: number,
  options: { locale?: string; minimumFractionDigits?: number; maximumFractionDigits?: number } = {}
): string {
  const {
    locale = 'bn-BD',
    minimumFractionDigits = 0,
    maximumFractionDigits = 2,
  } = options;

  return new Intl.NumberFormat(locale, {
    minimumFractionDigits,
    maximumFractionDigits,
  }).format(number);
}

// Utility to get current time in Bangladesh timezone
export function getCurrentTimeInBD(): dayjs.Dayjs {
  return dayjs().tz(DEFAULT_TIMEZONE);
}

// Utility to convert any date to Bangladesh timezone
export function toBDTime(date: string | Date | dayjs.Dayjs): dayjs.Dayjs {
  return dayjs(date).tz(DEFAULT_TIMEZONE);
}