import React, { useState, useEffect } from 'react';
import { payoutService } from '../../services/payoutService';
import EarningsSummary from '../../components/provider/earnings/EarningsSummary';
import PayoutHistory from '../../components/provider/earnings/PayoutHistory';
import PayoutRequestForm from '../../components/provider/earnings/PayoutRequestForm';
import LoadingSpinner from '../../components/ui/loading-spinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import { toast } from 'react-toastify';

interface EarningsData {
  availableBalance: number;
  pendingBalance: number;
  totalEarnings: number;
  currency: string;
  lastPayoutDate?: string;
  nextPayoutDate?: string;
}

interface Payout {
  id: string;
  amount: number;
  currency: string;
  status: string;
  method: string;
  requestedAt: string;
  processedAt?: string;
  paidAt?: string;
  failedAt?: string;
  referenceNumber?: string;
  failureReason?: string;
}

interface PayoutFilters {
  status: string;
  method: string;
  startDate: string;
  endDate: string;
}

interface Pagination {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

const EarningsPage: React.FC = () => {
  const [earnings, setEarnings] = useState<EarningsData | null>(null);
  const [payouts, setPayouts] = useState<Payout[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [showPayoutForm, setShowPayoutForm] = useState<boolean>(false);
  const [filters, setFilters] = useState<PayoutFilters>({
    status: '',
    method: '',
    startDate: '',
    endDate: ''
  });
  const [pagination, setPagination] = useState<Pagination>({
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0
  });

  // Load earnings data
  const loadEarnings = async () => {
    try {
      const data = await payoutService.getProviderEarnings();
      setEarnings(data);
    } catch (err) {
      console.error('Failed to load earnings:', err);
      setError('Failed to load earnings data');
    }
  };

  // Load payout history
  const loadPayouts = async () => {
    try {
      setLoading(true);
      const params = {
        page: pagination.page,
        size: pagination.size,
        ...filters
      };
      
      const response = await payoutService.getProviderPayouts(params);
      setPayouts(response.content);
      setPagination(prev => ({
        ...prev,
        totalPages: response.totalPages,
        totalElements: response.totalElements
      }));
    } catch (err) {
      console.error('Failed to load payouts:', err);
      setError('Failed to load payout history');
    } finally {
      setLoading(false);
    }
  };

  // Initial load
  useEffect(() => {
    const loadData = async () => {
      await Promise.all([loadEarnings(), loadPayouts()]);
    };
    loadData();
  }, []);

  // Reload payouts when filters or pagination change
  useEffect(() => {
    loadPayouts();
  }, [filters, pagination.page]);

  // Handle payout request
  const handlePayoutRequest = async (payoutData: {
    amount: number;
    method: string;
    bankDetails?: any;
    mobileMoneyDetails?: any;
  }) => {
    try {
      await payoutService.requestPayout(payoutData);
      toast.success('Payout request submitted successfully');
      setShowPayoutForm(false);
      
      // Reload data
      await Promise.all([loadEarnings(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to request payout:', err);
      toast.error(err.response?.data?.message || 'Failed to submit payout request');
    }
  };

  // Handle payout cancellation
  const handleCancelPayout = async (payoutId: string) => {
    try {
      await payoutService.cancelPayout(payoutId);
      toast.success('Payout request cancelled successfully');
      
      // Reload data
      await Promise.all([loadEarnings(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to cancel payout:', err);
      toast.error(err.response?.data?.message || 'Failed to cancel payout request');
    }
  };

  // Handle filters change
  const handleFiltersChange = (newFilters: PayoutFilters) => {
    setFilters(newFilters);
    setPagination(prev => ({ ...prev, page: 0 }));
  };

  // Handle page change
  const handlePageChange = (newPage: number) => {
    setPagination(prev => ({ ...prev, page: newPage }));
  };

  if (loading && !earnings) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Earnings & Payouts</h1>
              <p className="mt-2 text-gray-600">
                Manage your earnings and request payouts
              </p>
            </div>
            
            {earnings && earnings.availableBalance > 0 && (
              <button
                onClick={() => setShowPayoutForm(true)}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Request Payout
              </button>
            )}
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6">
            <ErrorMessage 
              message={error} 
              onRetry={() => {
                setError(null);
                loadEarnings();
                loadPayouts();
              }} 
            />
          </div>
        )}

        {/* Earnings Summary */}
        {earnings && (
          <div className="mb-8">
            <EarningsSummary 
              earnings={earnings}
              loading={loading}
            />
          </div>
        )}

        {/* Payout History */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">Payout History</h2>
          </div>
          
          <PayoutHistory
            payouts={payouts}
            filters={filters}
            pagination={pagination}
            loading={loading}
            onFiltersChange={handleFiltersChange}
            onPageChange={handlePageChange}
            onCancelPayout={handleCancelPayout}
          />
        </div>

        {/* Payout Request Modal */}
        {showPayoutForm && earnings && (
          <PayoutRequestForm
            availableBalance={earnings.availableBalance}
            currency={earnings.currency}
            onSubmit={handlePayoutRequest}
            onCancel={() => setShowPayoutForm(false)}
          />
        )}
      </div>
    </div>
  );
};

export default EarningsPage;