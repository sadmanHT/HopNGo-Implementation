import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import relativeTime from 'dayjs/plugin/relativeTime';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import 'dayjs/locale/bn';
import 'dayjs/locale/en';
import { useTranslation } from 'react-i18next';

// Configure dayjs plugins
dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(relativeTime);
dayjs.extend(localizedFormat);

// Default timezone for Bangladesh
export const DEFAULT_TIMEZONE = 'Asia/Dhaka';

export interface DateFormatOptions {
  timezone?: string;
  locale?: string;
  format?: string;
}

/**
 * Format date using dayjs with timezone support
 * @param date - Date to format (string, Date, or dayjs object)
 * @param options - Formatting options
 * @returns Formatted date string
 */
export function formatDate(
  date: string | Date | dayjs.Dayjs,
  options: DateFormatOptions = {}
): string {
  const {
    timezone = DEFAULT_TIMEZONE,
    locale = 'en',
    format = 'YYYY-MM-DD'
  } = options;

  return dayjs(date)
    .tz(timezone)
    .locale(locale)
    .format(format);
}

/**
 * Format date and time with localized format
 */
export function formatDateTime(
  date: string | Date | dayjs.Dayjs,
  options: DateFormatOptions = {}
): string {
  const {
    timezone = DEFAULT_TIMEZONE,
    locale = 'en',
    format = 'LLL' // Localized format
  } = options;

  return dayjs(date)
    .tz(timezone)
    .locale(locale)
    .format(format);
}

/**
 * Get relative time (e.g., "2 hours ago")
 */
export function formatRelativeTime(
  date: string | Date | dayjs.Dayjs,
  options: Pick<DateFormatOptions, 'timezone' | 'locale'> = {}
): string {
  const {
    timezone = DEFAULT_TIMEZONE,
    locale = 'en'
  } = options;

  return dayjs(date)
    .tz(timezone)
    .locale(locale)
    .fromNow();
}

/**
 * Format time only
 */
export function formatTime(
  date: string | Date | dayjs.Dayjs,
  options: DateFormatOptions = {}
): string {
  const {
    timezone = DEFAULT_TIMEZONE,
    locale = 'en',
    format = 'LT' // Localized time format
  } = options;

  return dayjs(date)
    .tz(timezone)
    .locale(locale)
    .format(format);
}

/**
 * React hook for date formatting with i18n support
 */
export function useDateFormatter() {
  const { i18n } = useTranslation();
  
  const getLocale = () => i18n.language === 'bn' ? 'bn' : 'en';
  
  const formatLocalizedDate = (date: string | Date | dayjs.Dayjs, format?: string) => {
    return formatDate(date, {
      locale: getLocale(),
      format: format || (i18n.language === 'bn' ? 'DD/MM/YYYY' : 'MM/DD/YYYY')
    });
  };

  const formatLocalizedDateTime = (date: string | Date | dayjs.Dayjs) => {
    return formatDateTime(date, {
      locale: getLocale()
    });
  };

  const formatLocalizedTime = (date: string | Date | dayjs.Dayjs) => {
    return formatTime(date, {
      locale: getLocale()
    });
  };

  const formatLocalizedRelativeTime = (date: string | Date | dayjs.Dayjs) => {
    return formatRelativeTime(date, {
      locale: getLocale()
    });
  };

  return {
    formatLocalizedDate,
    formatLocalizedDateTime,
    formatLocalizedTime,
    formatLocalizedRelativeTime
  };
}

/**
 * Convert date to ISO 8601 string with timezone
 */
export function toISOStringWithTimezone(
  date: string | Date | dayjs.Dayjs,
  timezone: string = DEFAULT_TIMEZONE
): string {
  return dayjs(date).tz(timezone).toISOString();
}

/**
 * Get current date/time in Bangladesh timezone
 */
export function nowInBangladesh(): dayjs.Dayjs {
  return dayjs().tz(DEFAULT_TIMEZONE);
}

/**
 * Check if a date is today in Bangladesh timezone
 */
export function isToday(date: string | Date | dayjs.Dayjs): boolean {
  const today = nowInBangladesh();
  const checkDate = dayjs(date).tz(DEFAULT_TIMEZONE);
  return today.isSame(checkDate, 'day');
}

/**
 * Common date format patterns
 */
export const DATE_FORMATS = {
  SHORT: 'MM/DD/YYYY',
  SHORT_BD: 'DD/MM/YYYY',
  LONG: 'MMMM DD, YYYY',
  LONG_BD: 'DD MMMM, YYYY',
  ISO: 'YYYY-MM-DD',
  TIME_12: 'h:mm A',
  TIME_24: 'HH:mm',
  DATETIME: 'MM/DD/YYYY h:mm A',
  DATETIME_BD: 'DD/MM/YYYY h:mm A'
} as const;