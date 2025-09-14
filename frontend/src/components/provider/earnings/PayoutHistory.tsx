import React from 'react';
import LoadingSpinner from '../../ui/loading-spinner';
import Pagination from '../../common/Pagination';

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

interface PaginationData {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

interface PayoutHistoryProps {
  payouts: Payout[];
  filters: PayoutFilters;
  pagination: PaginationData;
  loading: boolean;
  onFiltersChange: (filters: PayoutFilters) => void;
  onPageChange: (page: number) => void;
  onCancelPayout: (payoutId: string) => void;
}

const PayoutHistory: React.FC<PayoutHistoryProps> = ({
  payouts,
  filters,
  pagination,
  loading,
  onFiltersChange,
  onPageChange,
  onCancelPayout
}) => {
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

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Pending' },
      PROCESSING: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'Processing' },
      COMPLETED: { bg: 'bg-green-100', text: 'text-green-800', label: 'Completed' },
      FAILED: { bg: 'bg-red-100', text: 'text-red-800', label: 'Failed' },
      CANCELLED: { bg: 'bg-gray-100', text: 'text-gray-800', label: 'Cancelled' }
    };

    const config = statusConfig[status as keyof typeof statusConfig] || statusConfig.PENDING;
    
    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.bg} ${config.text}`}>
        {config.label}
      </span>
    );
  };

  const getMethodIcon = (method: string) => {
    switch (method.toLowerCase()) {
      case 'bank_transfer':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        );
      case 'mobile_money':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
        );
      default:
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        );
    }
  };

  const handleFilterChange = (field: keyof PayoutFilters, value: string) => {
    onFiltersChange({
      ...filters,
      [field]: value
    });
  };

  const clearFilters = () => {
    onFiltersChange({
      status: '',
      method: '',
      startDate: '',
      endDate: ''
    });
  };

  const canCancelPayout = (payout: Payout) => {
    return payout.status === 'PENDING';
  };

  return (
    <div>
      {/* Filters */}
      <div className="p-6 border-b border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Status Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status
            </label>
            <select
              value={filters.status}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="PROCESSING">Processing</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          {/* Method Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Method
            </label>
            <select
              value={filters.method}
              onChange={(e) => handleFilterChange('method', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Methods</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="MOBILE_MONEY">Mobile Money</option>
            </select>
          </div>

          {/* Start Date Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Start Date
            </label>
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => handleFilterChange('startDate', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* End Date Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              End Date
            </label>
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => handleFilterChange('endDate', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        </div>

        {/* Clear Filters */}
        <div className="mt-4 flex justify-end">
          <button
            onClick={clearFilters}
            className="text-sm text-blue-600 hover:text-blue-800 font-medium"
          >
            Clear Filters
          </button>
        </div>
      </div>

      {/* Payout List */}
      <div className="p-6">
        {loading ? (
          <div className="flex justify-center py-8">
            <LoadingSpinner size="md" />
          </div>
        ) : payouts.length === 0 ? (
          <div className="text-center py-8">
            <svg className="w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                    d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <h3 className="text-lg font-medium text-gray-900 mb-2">No Payouts Found</h3>
            <p className="text-gray-500">
              {Object.values(filters).some(f => f) 
                ? 'No payouts match your current filters.' 
                : 'You haven\'t requested any payouts yet.'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {payouts.map((payout) => (
              <div key={payout.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    {/* Method Icon */}
                    <div className="flex-shrink-0">
                      <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center text-gray-600">
                        {getMethodIcon(payout.method)}
                      </div>
                    </div>

                    {/* Payout Details */}
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-1">
                        <h4 className="text-lg font-semibold text-gray-900">
                          {formatCurrency(payout.amount, payout.currency)}
                        </h4>
                        {getStatusBadge(payout.status)}
                      </div>
                      
                      <div className="flex items-center space-x-4 text-sm text-gray-500">
                        <span className="capitalize">
                          {payout.method.replace('_', ' ').toLowerCase()}
                        </span>
                        <span>•</span>
                        <span>Requested {formatDate(payout.requestedAt)}</span>
                        {payout.referenceNumber && (
                          <>
                            <span>•</span>
                            <span>Ref: {payout.referenceNumber}</span>
                          </>
                        )}
                      </div>

                      {/* Status-specific information */}
                      {payout.processedAt && (
                        <p className="text-sm text-gray-500 mt-1">
                          Processed on {formatDate(payout.processedAt)}
                        </p>
                      )}
                      
                      {payout.paidAt && (
                        <p className="text-sm text-green-600 mt-1">
                          Paid on {formatDate(payout.paidAt)}
                        </p>
                      )}
                      
                      {payout.failedAt && payout.failureReason && (
                        <p className="text-sm text-red-600 mt-1">
                          Failed on {formatDate(payout.failedAt)}: {payout.failureReason}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center space-x-2">
                    {canCancelPayout(payout) && (
                      <button
                        onClick={() => onCancelPayout(payout.id)}
                        className="px-3 py-1 text-sm font-medium text-red-600 hover:text-red-800 border border-red-300 hover:border-red-400 rounded-md transition-colors"
                      >
                        Cancel
                      </button>
                    )}
                    
                    <button className="p-2 text-gray-400 hover:text-gray-600 transition-colors">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                              d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {pagination.totalPages > 1 && (
          <div className="mt-6">
            <Pagination
              currentPage={pagination.page + 1}
              totalPages={pagination.totalPages}
              totalElements={pagination.totalElements}
              onPageChange={(page) => onPageChange(page - 1)}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default PayoutHistory;