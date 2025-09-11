import React, { useState, useEffect } from 'react';
import { payoutService, LedgerSummary, Payout, AdminPayoutFilters } from '../../services/payoutService';
import FinanceSummary from '../../components/admin/finance/FinanceSummary';
import PayoutManagement from '../../components/admin/finance/PayoutManagement';
import LoadingSpinner from '../../components/ui/loading-spinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import { toast } from 'react-toastify';

interface Pagination {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

const FinancePage: React.FC = () => {
  const [ledgerSummary, setLedgerSummary] = useState<LedgerSummary | null>(null);
  const [payouts, setPayouts] = useState<Payout[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [payoutsLoading, setPayoutsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'payouts'>('overview');
  const [filters, setFilters] = useState<AdminPayoutFilters>({
    status: '',
    method: '',
    providerId: '',
    startDate: '',
    endDate: ''
  });
  const [pagination, setPagination] = useState<Pagination>({
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0
  });

  // Load ledger summary
  const loadLedgerSummary = async () => {
    try {
      const data = await payoutService.getLedgerSummary();
      setLedgerSummary(data);
    } catch (err) {
      console.error('Failed to load ledger summary:', err);
      setError('Failed to load financial summary');
    }
  };

  // Load payouts
  const loadPayouts = async () => {
    try {
      setPayoutsLoading(true);
      const params = {
        page: pagination.page,
        size: pagination.size,
        ...filters
      };
      
      const response = await payoutService.getAllPayouts(params);
      setPayouts(response.content);
      setPagination(prev => ({
        ...prev,
        totalPages: response.totalPages,
        totalElements: response.totalElements
      }));
    } catch (err) {
      console.error('Failed to load payouts:', err);
      setError('Failed to load payout data');
    } finally {
      setPayoutsLoading(false);
    }
  };

  // Initial load
  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      await Promise.all([loadLedgerSummary(), loadPayouts()]);
      setLoading(false);
    };
    loadData();
  }, []);

  // Reload payouts when filters or pagination change
  useEffect(() => {
    if (!loading) {
      loadPayouts();
    }
  }, [filters, pagination.page]);

  // Handle payout approval
  const handleApprovePayout = async (payoutId: string, notes?: string) => {
    try {
      await payoutService.approvePayout(payoutId, notes);
      toast.success('Payout approved successfully');
      
      // Reload data
      await Promise.all([loadLedgerSummary(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to approve payout:', err);
      toast.error(err.response?.data?.message || 'Failed to approve payout');
    }
  };

  // Handle payout rejection
  const handleRejectPayout = async (payoutId: string, reason: string) => {
    try {
      await payoutService.rejectPayout(payoutId, reason);
      toast.success('Payout rejected successfully');
      
      // Reload data
      await Promise.all([loadLedgerSummary(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to reject payout:', err);
      toast.error(err.response?.data?.message || 'Failed to reject payout');
    }
  };

  // Handle payout processing
  const handleProcessPayout = async (payoutId: string, referenceNumber?: string) => {
    try {
      await payoutService.processPayout(payoutId, referenceNumber);
      toast.success('Payout marked as processing');
      
      // Reload data
      await Promise.all([loadLedgerSummary(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to process payout:', err);
      toast.error(err.response?.data?.message || 'Failed to process payout');
    }
  };

  // Handle mark payout as paid
  const handleMarkPaid = async (payoutId: string, referenceNumber: string) => {
    try {
      await payoutService.markPayoutPaid(payoutId, referenceNumber);
      toast.success('Payout marked as paid');
      
      // Reload data
      await Promise.all([loadLedgerSummary(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to mark payout as paid:', err);
      toast.error(err.response?.data?.message || 'Failed to mark payout as paid');
    }
  };

  // Handle mark payout as failed
  const handleMarkFailed = async (payoutId: string, reason: string) => {
    try {
      await payoutService.markPayoutFailed(payoutId, reason);
      toast.success('Payout marked as failed');
      
      // Reload data
      await Promise.all([loadLedgerSummary(), loadPayouts()]);
    } catch (err: any) {
      console.error('Failed to mark payout as failed:', err);
      toast.error(err.response?.data?.message || 'Failed to mark payout as failed');
    }
  };

  // Handle filters change
  const handleFiltersChange = (newFilters: AdminPayoutFilters) => {
    setFilters(newFilters);
    setPagination(prev => ({ ...prev, page: 0 }));
  };

  // Handle page change
  const handlePageChange = (newPage: number) => {
    setPagination(prev => ({ ...prev, page: newPage }));
  };

  // Handle export
  const handleExportPayouts = async () => {
    try {
      const blob = await payoutService.downloadPayoutReport(filters);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `payouts-report-${new Date().toISOString().split('T')[0]}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success('Payout report downloaded successfully');
    } catch (err: any) {
      console.error('Failed to export payouts:', err);
      toast.error('Failed to download payout report');
    }
  };

  if (loading) {
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
              <h1 className="text-3xl font-bold text-gray-900">Finance Management</h1>
              <p className="mt-2 text-gray-600">
                Monitor financial performance and manage provider payouts
              </p>
            </div>
            
            <div className="flex space-x-3">
              <button
                onClick={handleExportPayouts}
                className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Export Report
              </button>
            </div>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6">
            <ErrorMessage 
              message={error} 
              onRetry={() => {
                setError(null);
                loadLedgerSummary();
                loadPayouts();
              }} 
            />
          </div>
        )}

        {/* Navigation Tabs */}
        <div className="mb-6">
          <nav className="flex space-x-8" aria-label="Tabs">
            <button
              onClick={() => setActiveTab('overview')}
              className={`whitespace-nowrap py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'overview'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Financial Overview
            </button>
            <button
              onClick={() => setActiveTab('payouts')}
              className={`whitespace-nowrap py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'payouts'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Payout Management
              {ledgerSummary && ledgerSummary.pendingPayouts > 0 && (
                <span className="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                  {ledgerSummary.pendingPayouts}
                </span>
              )}
            </button>
          </nav>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && ledgerSummary && (
          <FinanceSummary 
            ledgerSummary={ledgerSummary}
            loading={loading}
          />
        )}

        {activeTab === 'payouts' && (
          <PayoutManagement
            payouts={payouts}
            filters={filters}
            pagination={pagination}
            loading={payoutsLoading}
            onFiltersChange={handleFiltersChange}
            onPageChange={handlePageChange}
            onApprovePayout={handleApprovePayout}
            onRejectPayout={handleRejectPayout}
            onProcessPayout={handleProcessPayout}
            onMarkPaid={handleMarkPaid}
            onMarkFailed={handleMarkFailed}
          />
        )}
      </div>
    </div>
  );
};

export default FinancePage;