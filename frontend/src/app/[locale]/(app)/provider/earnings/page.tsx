import { Metadata } from 'next';
import EarningsPage from '../../../../../pages/provider/EarningsPage';

export const metadata: Metadata = {
  title: 'Earnings & Payouts - HopNGo Provider',
  description: 'Manage your earnings, view payout history, and request payouts as a HopNGo provider.',
};

export default function ProviderEarningsRoute() {
  return <EarningsPage />;
}