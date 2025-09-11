import { ReactNode } from 'react';
import { I18nProvider } from '@/providers/I18nProvider';
import { QueryProvider } from '@/providers/QueryProvider';
import { AnalyticsProvider } from '@/providers/AnalyticsProvider';
import { ToastProvider } from '@/components/ui/toast';
import { languages } from '@/lib/i18n';

type Props = {
  children: ReactNode;
  params: { locale: string };
};

export async function generateStaticParams() {
  return languages.map((lang) => ({ locale: lang.code }));
}

export default function LocaleLayout({ children, params: { locale } }: Props) {
  return (
    <I18nProvider locale={locale}>
      <QueryProvider>
        <AnalyticsProvider>
          <ToastProvider>
            {children}
          </ToastProvider>
        </AnalyticsProvider>
      </QueryProvider>
    </I18nProvider>
  );
}