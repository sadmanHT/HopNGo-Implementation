import { Metadata } from 'next';
import FinancePage from '../../../../../pages/admin/FinancePage';

export const metadata: Metadata = {
  title: 'Finance Management - HopNGo Admin',
  description: 'Manage ledger summary and payout requests'
};

export default function AdminFinanceRoute() {
  return <FinancePage />;
}