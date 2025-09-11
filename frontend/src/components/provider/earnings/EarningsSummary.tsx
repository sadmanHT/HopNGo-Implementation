import React from 'react';
import LoadingSpinner from '../../ui/loading-spinner';

interface EarningsData {
  availableBalance: number;
  pendingBalance: number;
  totalEarnings: number;
  currency: string;
  lastPayoutDate?: string;
  nextPayoutDate?: string;
}

interface EarningsSummaryProps {
  earnings: EarningsData;
  loading?: boolean;
}

const EarningsSummary: React.FC<EarningsSummaryProps> = ({ earnings, loading = false }) => {
  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  if (loading) {
    return (
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex justify-center">
          <LoadingSpinner size="medium" />
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white shadow rounded-lg overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-medium text-gray-900">Earnings Summary</h2>
      </div>
      
      <div className="p-6">
        {/* Balance Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Available Balance */}
          <div className="bg-gradient-to-r from-green-500 to-green-600 rounded-lg p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-green-100 text-sm font-medium">Available Balance</p>
                <p className="text-2xl font-bold">
                  {formatCurrency(earnings.availableBalance, earnings.currency)}
                </p>
                <p className="text-green-100 text-xs mt-1">Ready for payout</p>
              </div>
              <div className="bg-green-400 bg-opacity-30 rounded-full p-3">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
                </svg>
              </div>
            </div>
          </div>

          {/* Pending Balance */}
          <div className="bg-gradient-to-r from-yellow-500 to-yellow-600 rounded-lg p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-yellow-100 text-sm font-medium">Pending Balance</p>
                <p className="text-2xl font-bold">
                  {formatCurrency(earnings.pendingBalance, earnings.currency)}
                </p>
                <p className="text-yellow-100 text-xs mt-1">Processing payouts</p>
              </div>
              <div className="bg-yellow-400 bg-opacity-30 rounded-full p-3">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>

          {/* Total Earnings */}
          <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-blue-100 text-sm font-medium">Total Earnings</p>
                <p className="text-2xl font-bold">
                  {formatCurrency(earnings.totalEarnings, earnings.currency)}
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
        </div>

        {/* Payout Information */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Last Payout */}
          <div className="bg-gray-50 rounded-lg p-4">
            <div className="flex items-center mb-2">
              <svg className="w-5 h-5 text-gray-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h3 className="text-sm font-medium text-gray-900">Last Payout</h3>
            </div>
            <p className="text-lg font-semibold text-gray-900">
              {formatDate(earnings.lastPayoutDate)}
            </p>
            {!earnings.lastPayoutDate && (
              <p className="text-sm text-gray-500 mt-1">No payouts yet</p>
            )}
          </div>

          {/* Next Payout */}
          <div className="bg-gray-50 rounded-lg p-4">
            <div className="flex items-center mb-2">
              <svg className="w-5 h-5 text-gray-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <h3 className="text-sm font-medium text-gray-900">Next Scheduled Payout</h3>
            </div>
            <p className="text-lg font-semibold text-gray-900">
              {formatDate(earnings.nextPayoutDate)}
            </p>
            {!earnings.nextPayoutDate && (
              <p className="text-sm text-gray-500 mt-1">No scheduled payouts</p>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        {earnings.availableBalance > 0 && (
          <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-blue-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div className="flex-1">
                <h4 className="text-sm font-medium text-blue-900">Ready for Payout</h4>
                <p className="text-sm text-blue-700">
                  You have {formatCurrency(earnings.availableBalance, earnings.currency)} available for payout.
                </p>
              </div>
            </div>
          </div>
        )}

        {earnings.availableBalance === 0 && earnings.pendingBalance === 0 && (
          <div className="mt-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <div className="text-center">
              <svg className="w-12 h-12 text-gray-400 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
              </svg>
              <h4 className="text-sm font-medium text-gray-900 mb-1">No Available Balance</h4>
              <p className="text-sm text-gray-500">
                Complete more rides to start earning and request payouts.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EarningsSummary;