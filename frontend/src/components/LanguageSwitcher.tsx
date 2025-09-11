'use client';

import { useI18n } from '@/providers/I18nProvider';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

const languageFlags: Record<string, string> = {
  en: '🇺🇸',
  bn: '🇧🇩',
};

export function LanguageSwitcher() {
  const { locale, setLocale, languages } = useI18n();

  const currentLanguage = languages.find(lang => lang.code === locale) || languages[1];

  return (
    <Select value={locale} onValueChange={setLocale}>
      <SelectTrigger className="w-32">
        <SelectValue>
          <span className="flex items-center gap-2">
            <span>{languageFlags[currentLanguage.code]}</span>
            <span className="hidden sm:inline">{currentLanguage.name}</span>
          </span>
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {languages.map((language) => (
          <SelectItem key={language.code} value={language.code}>
            <span className="flex items-center gap-2">
              <span>{languageFlags[language.code]}</span>
              <span>{language.name}</span>
            </span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}