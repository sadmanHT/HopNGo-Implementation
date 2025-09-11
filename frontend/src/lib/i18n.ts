import { createInstance } from 'i18next';
import resourcesToBackend from 'i18next-resources-to-backend';

const initI18next = async (lng: string, ns: string) => {
  const i18nInstance = createInstance();
  await i18nInstance
    .use(
      resourcesToBackend(
        (language: string, namespace: string) =>
          import(`../../public/locales/${language}/${namespace}.json`)
      )
    )
    .init({
      debug: process.env.NODE_ENV === 'development',
      supportedLngs: ['en', 'bn'],
      fallbackLng: 'bn',
      lng,
      fallbackNS: 'common',
      defaultNS: 'common',
      ns,
    });
  return i18nInstance;
};

export async function useTranslation(
  lng: string,
  ns: string = 'common',
  options: { keyPrefix?: string } = {}
) {
  const i18nextInstance = await initI18next(lng, ns);
  return {
    t: i18nextInstance.getFixedT(
      lng,
      Array.isArray(ns) ? ns[0] : ns,
      options.keyPrefix
    ),
    i18n: i18nextInstance,
  };
}

export const languages = [
  { code: 'en', name: 'English' },
  { code: 'bn', name: 'বাংলা' },
];

export const defaultLanguage = 'bn';
export const cookieName = 'preferred-language';