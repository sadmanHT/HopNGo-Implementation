import { ReactNode } from 'react';
import { I18nProvider } from '@/providers/I18nProvider';
import { QueryProvider } from '@/providers/QueryProvider';
import { AnalyticsProvider } from '@/providers/AnalyticsProvider';
import { ToastProvider } from '@/components/ui/toast';
import { languages } from '@/lib/i18n';

// Force dynamic rendering for auth-protected routes
export const dynamic = 'force-dynamic';

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export async function generateStaticParams() {
  return languages.map((lang) => ({ locale: lang.code }));
}

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;
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