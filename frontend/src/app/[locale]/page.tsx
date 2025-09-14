import { redirect } from 'next/navigation';

type Props = {
  params: Promise<{ locale: string }>;
};

export default async function LocalePage({ params }: Props) {
  const { locale } = await params;
  
  // Redirect to the home page for the given locale
  redirect(`/${locale}/home`);
}