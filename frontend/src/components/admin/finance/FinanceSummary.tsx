import React, { useState, useEffect } from 'react';
import { payoutService } from '../../../services/payoutService';
import LoadingSpinner from '../../ui/loading-spinner';

interface LedgerSummary {
  totalRevenue: number;
  totalCommissions: number;
  totalPayouts: number;
  pendingPayouts: number;
  availableBalance: number;
  currency: string;
  lastUpdated: string;
}

interface FinanceSummaryProps {
  ledgerSummary: LedgerSummary;
  loading?: boolean;
}

interface PayoutStatistics {
  totalPayouts: number;
  totalAmount: number;
  averageAmount: number;
  payoutsByStatus: Record<string, number>;
  payoutsByMethod: Record<string, number>;
  currency: string;
}

const FinanceSummary: React.FC<FinanceSummaryProps> = ({ ledgerSummary, loading = false }) => {
  const [statistics, setStatistics] = useState<PayoutStatistics | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState<'7d' | '30d' | '90d' | 'all'>('30d');

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getDateRange = (period: string) => {
    const now = new Date();
    const startDate = new Date();
    
    switch (period) {
      case '7d':
        startDate.setDate(now.getDate() - 7);
        break;
      case '30d':
        startDate.setDate(now.getDate() - 30);
        break;
      case '90d':
        startDate.setDate(now.getDate() - 90);
        break;
      default:
        return { startDate: undefined, endDate: undefined };
    }
    
    return {
      startDate: startDate.toISOString().split('T')[0],
      endDate: now.toISOString().split('T')[0]
    };
  };

  const loadStatistics = async () => {
    try {
      setStatsLoading(true);
      const { startDate, endDate } = getDateRange(selectedPeriod);
      const data = await payoutService.getPayoutStatistics(startDate, endDate);
      setStatistics(data);
    } catch (error) {
      console.error('Failed to load payout statistics:', error);
    } finally {
      setStatsLoading(false);
    }
  };

  useEffect(() => {
    loadStatistics();
  }, [selectedPeriod]);

  if (loading) {
    return (
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex justify-center">
          <LoadingSpinner size="md" />
        </div>
      </div>
    );
  }

  const profitMargin = ledgerSummary.totalRevenue > 0 
    ? ((ledgerSummary.totalRevenue - ledgerSummary.totalCommissions - ledgerSummary.totalPayouts) / ledgerSummary.totalRevenue) * 100
    : 0;

  return (
    <div className="space-y-6">
      {/* Main Financial Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Revenue */}
        <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-blue-100 text-sm font-medium">Total Revenue</p>
              <p className="text-2xl font-bold">
                {formatCurrency(ledgerSummary.totalRevenue, ledgerSummary.currency)}
              </p>
              <p className="text-blue-100 text-xs mt-1">All time earnings</p>
            </div>
            <div className="bg-blue-400 bg-opacity-30 rounded-full p-3">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
          </div>
        </div>

        {/* Total Commissions */}
        <div className="bg-gradient-to-r from-green-500 to-green-600 rounded-lg p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-100 text-sm font-medium">Platform Commissions</p>
              <p className="text-2xl font-bold">
                {formatCurrency(ledgerSummary.totalCommissions, ledgerSummary.currency)}
              </p>
              <p className="text-green-100 text-xs mt-1">Platform earnings</p>
            </div>
            <div className="bg-green-400 bg-opacity-30 rounded-full p-3">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
              </svg>
            </div>
          </div>
        </div>

        {/* Total Payouts */}
        <div className="bg-gradient-to-r from-purple-500 to-purple-600 rounded-lg p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-100 text-sm font-medium">Total Payouts</p>
              <p className="text-2xl font-bold">
                {formatCurrency(ledgerSummary.totalPayouts, ledgerSummary.currency)}
              </p>
              <p className="text-purple-100 text-xs mt-1">Paid to providers</p>
            </div>
            <div className="bg-purple-400 bg-opacity-30 rounded-full p-3">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
          </div>
        </div>

        {/* Pending Payouts */}
        <div className="bg-gradient-to-r from-yellow-500 to-yellow-600 rounded-lg p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-yellow-100 text-sm font-medium">Pending Payouts</p>
              <p className="text-2xl font-bold">
                {formatCurrency(ledgerSummary.pendingPayouts, ledgerSummary.currency)}
              </p>
              <p className="text-yellow-100 text-xs mt-1">Awaiting processing</p>
            </div>
            <div className="bg-yellow-400 bg-opacity-30 rounded-full p-3">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      {/* Financial Health Indicators */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Available Balance & Profit Margin */}
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Financial Health</h3>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
              <div>
                <p className="text-sm font-medium text-gray-700">Available Balance</p>
                <p className="text-xl font-bold text-gray-900">
                  {formatCurrency(ledgerSummary.availableBalance, ledgerSummary.currency)}
                </p>
              </div>
              <div className={`px-3 py-1 rounded-full text-sm font-medium ${
                ledgerSummary.availableBalance > 0 
                  ? 'bg-green-100 text-green-800' 
                  : 'bg-red-100 text-red-800'
              }`}>
                {ledgerSummary.availableBalance > 0 ? 'Positive' : 'Negative'}
              </div>
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
              <div>
                <p className="text-sm font-medium text-gray-700">Profit Margin</p>
                <p className="text-xl font-bold text-gray-900">
                  {profitMargin.toFixed(2)}%
                </p>
              </div>
              <div className={`px-3 py-1 rounded-full text-sm font-medium ${
                profitMargin > 20 
                  ? 'bg-green-100 text-green-800' 
                  : profitMargin > 10
                  ? 'bg-yellow-100 text-yellow-800'
                  : 'bg-red-100 text-red-800'
              }`}>
                {profitMargin > 20 ? 'Excellent' : profitMargin > 10 ? 'Good' : 'Low'}
              </div>
            </div>
          </div>
        </div>

        {/* Payout Statistics */}
        <div className="bg-white shadow rounded-lg p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Payout Statistics</h3>
            <select
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value as any)}
              className="text-sm border border-gray-300 rounded-md px-3 py-1 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="7d">Last 7 days</option>
              <option value="30d">Last 30 days</option>
              <option value="90d">Last 90 days</option>
              <option value="all">All time</option>
            </select>
          </div>

          {statsLoading ? (
            <div className="flex justify-center py-8">
              <LoadingSpinner size="md" />
            </div>
          ) : statistics ? (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="text-center p-3 bg-blue-50 rounded-lg">
                  <p className="text-2xl font-bold text-blue-600">{statistics.totalPayouts}</p>
                  <p className="text-sm text-blue-800">Total Payouts</p>
                </div>
                <div className="text-center p-3 bg-green-50 rounded-lg">
                  <p className="text-2xl font-bold text-green-600">
                    {formatCurrency(statistics.averageAmount, statistics.currency)}
                  </p>
                  <p className="text-sm text-green-800">Average Amount</p>
                </div>
              </div>

              {/* Payout by Status */}
              <div>
                <p className="text-sm font-medium text-gray-700 mb-2">By Status</p>
                <div className="space-y-2">
                  {Object.entries(statistics.payoutsByStatus).map(([status, count]) => (
                    <div key={status} className="flex items-center justify-between text-sm">
                      <span className="capitalize text-gray-600">{status.toLowerCase()}</span>
                      <span className="font-medium">{count}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Payout by Method */}
              <div>
                <p className="text-sm font-medium text-gray-700 mb-2">By Method</p>
                <div className="space-y-2">
                  {Object.entries(statistics.payoutsByMethod).map(([method, count]) => (
                    <div key={method} className="flex items-center justify-between text-sm">
                      <span className="capitalize text-gray-600">
                        {method.replace('_', ' ').toLowerCase()}
                      </span>
                      <span className="font-medium">{count}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              <p>No payout data available</p>
            </div>
          )}
        </div>
      </div>

      {/* Last Updated */}
      <div className="bg-white shadow rounded-lg p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center text-sm text-gray-500">
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Last updated: {formatDate(ledgerSummary.lastUpdated)}
          </div>
          
          <button
            onClick={() => window.location.reload()}
            className="text-sm text-blue-600 hover:text-blue-800 font-medium"
          >
            Refresh Data
          </button>
        </div>
      </div>

      {/* Alerts */}
      {ledgerSummary.pendingPayouts > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-center">
            <svg className="w-5 h-5 text-yellow-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <div>
              <h4 className="text-sm font-medium text-yellow-900">Pending Payouts Require Attention</h4>
              <p className="text-sm text-yellow-700">
                You have {formatCurrency(ledgerSummary.pendingPayouts, ledgerSummary.currency)} in pending payouts that need review.
              </p>
            </div>
          </div>
        </div>
      )}

      {ledgerSummary.availableBalance < 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center">
            <svg className="w-5 h-5 text-red-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <h4 className="text-sm font-medium text-red-900">Negative Balance Alert</h4>
              <p className="text-sm text-red-700">
                Your available balance is negative. Review recent transactions and payouts.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FinanceSummary;