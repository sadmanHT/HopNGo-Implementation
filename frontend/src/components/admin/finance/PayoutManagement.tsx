import React, { useState } from 'react';
import { Payout, AdminPayoutFilters } from '../../../services/payoutService';
import LoadingSpinner from '../../ui/loading-spinner';
import Pagination from '../../common/Pagination';

interface PaginationData {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

interface PayoutManagementProps {
  payouts: Payout[];
  filters: AdminPayoutFilters;
  pagination: PaginationData;
  loading: boolean;
  onFiltersChange: (filters: AdminPayoutFilters) => void;
  onPageChange: (page: number) => void;
  onApprovePayout: (payoutId: string, notes?: string) => Promise<void>;
  onRejectPayout: (payoutId: string, reason: string) => Promise<void>;
  onProcessPayout: (payoutId: string, referenceNumber?: string) => Promise<void>;
  onMarkPaid: (payoutId: string, referenceNumber: string) => Promise<void>;
  onMarkFailed: (payoutId: string, reason: string) => Promise<void>;
}

interface ActionModalData {
  type: 'approve' | 'reject' | 'process' | 'paid' | 'failed' | null;
  payoutId: string | null;
  payout: Payout | null;
}

const PayoutManagement: React.FC<PayoutManagementProps> = ({
  payouts,
  filters,
  pagination,
  loading,
  onFiltersChange,
  onPageChange,
  onApprovePayout,
  onRejectPayout,
  onProcessPayout,
  onMarkPaid,
  onMarkFailed
}) => {
  const [actionModal, setActionModal] = useState<ActionModalData>({
    type: null,
    payoutId: null,
    payout: null
  });
  const [actionLoading, setActionLoading] = useState(false);
  const [actionInput, setActionInput] = useState('');

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
      PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Pending Approval' },
      APPROVED: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'Approved' },
      PROCESSING: { bg: 'bg-indigo-100', text: 'text-indigo-800', label: 'Processing' },
      COMPLETED: { bg: 'bg-green-100', text: 'text-green-800', label: 'Completed' },
      FAILED: { bg: 'bg-red-100', text: 'text-red-800', label: 'Failed' },
      REJECTED: { bg: 'bg-gray-100', text: 'text-gray-800', label: 'Rejected' },
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
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        );
      case 'mobile_money':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
        );
      default:
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        );
    }
  };

  const handleFilterChange = (field: keyof AdminPayoutFilters, value: string) => {
    onFiltersChange({
      ...filters,
      [field]: value
    });
  };

  const clearFilters = () => {
    onFiltersChange({
      status: '',
      method: '',
      providerId: '',
      startDate: '',
      endDate: ''
    });
  };

  const openActionModal = (type: ActionModalData['type'], payout: Payout) => {
    setActionModal({ type, payoutId: payout.id, payout });
    setActionInput('');
  };

  const closeActionModal = () => {
    setActionModal({ type: null, payoutId: null, payout: null });
    setActionInput('');
  };

  const handleAction = async () => {
    if (!actionModal.payoutId || !actionModal.type) return;

    setActionLoading(true);
    try {
      switch (actionModal.type) {
        case 'approve':
          await onApprovePayout(actionModal.payoutId, actionInput || undefined);
          break;
        case 'reject':
          if (!actionInput.trim()) {
            alert('Please provide a reason for rejection');
            return;
          }
          await onRejectPayout(actionModal.payoutId, actionInput);
          break;
        case 'process':
          await onProcessPayout(actionModal.payoutId, actionInput || undefined);
          break;
        case 'paid':
          if (!actionInput.trim()) {
            alert('Please provide a reference number');
            return;
          }
          await onMarkPaid(actionModal.payoutId, actionInput);
          break;
        case 'failed':
          if (!actionInput.trim()) {
            alert('Please provide a failure reason');
            return;
          }
          await onMarkFailed(actionModal.payoutId, actionInput);
          break;
      }
      closeActionModal();
    } catch (error) {
      console.error('Action failed:', error);
    } finally {
      setActionLoading(false);
    }
  };

  const getAvailableActions = (payout: Payout) => {
    const actions = [];
    
    switch (payout.status) {
      case 'PENDING':
        actions.push(
          { type: 'approve', label: 'Approve', color: 'text-green-600 hover:text-green-800' },
          { type: 'reject', label: 'Reject', color: 'text-red-600 hover:text-red-800' }
        );
        break;
      case 'APPROVED':
        actions.push(
          { type: 'process', label: 'Process', color: 'text-blue-600 hover:text-blue-800' }
        );
        break;
      case 'PROCESSING':
        actions.push(
          { type: 'paid', label: 'Mark Paid', color: 'text-green-600 hover:text-green-800' },
          { type: 'failed', label: 'Mark Failed', color: 'text-red-600 hover:text-red-800' }
        );
        break;
    }
    
    return actions;
  };

  return (
    <div className="bg-white shadow rounded-lg">
      {/* Filters */}
      <div className="p-6 border-b border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          {/* Status Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status
            </label>
            <select
              value={filters.status || ''}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="PROCESSING">Processing</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="REJECTED">Rejected</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          {/* Method Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Method
            </label>
            <select
              value={filters.method || ''}
              onChange={(e) => handleFilterChange('method', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Methods</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="MOBILE_MONEY">Mobile Money</option>
            </select>
          </div>

          {/* Provider ID Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Provider ID
            </label>
            <input
              type="text"
              value={filters.providerId || ''}
              onChange={(e) => handleFilterChange('providerId', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter provider ID"
            />
          </div>

          {/* Start Date Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Start Date
            </label>
            <input
              type="date"
              value={filters.startDate || ''}
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
              value={filters.endDate || ''}
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
            <LoadingSpinner size="medium" />
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
                : 'No payout requests have been made yet.'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {payouts.map((payout) => (
              <div key={payout.id} className="border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-4">
                    {/* Method Icon */}
                    <div className="flex-shrink-0">
                      <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center text-gray-600">
                        {getMethodIcon(payout.method)}
                      </div>
                    </div>

                    {/* Payout Details */}
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-2">
                        <h4 className="text-xl font-semibold text-gray-900">
                          {formatCurrency(payout.amount, payout.currency)}
                        </h4>
                        {getStatusBadge(payout.status)}
                      </div>
                      
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
                        <div>
                          <p><span className="font-medium">Method:</span> {payout.method.replace('_', ' ')}</p>
                          <p><span className="font-medium">Requested:</span> {formatDate(payout.requestedAt)}</p>
                          {payout.referenceNumber && (
                            <p><span className="font-medium">Reference:</span> {payout.referenceNumber}</p>
                          )}
                        </div>
                        <div>
                          {payout.processedAt && (
                            <p><span className="font-medium">Processed:</span> {formatDate(payout.processedAt)}</p>
                          )}
                          {payout.paidAt && (
                            <p><span className="font-medium">Paid:</span> {formatDate(payout.paidAt)}</p>
                          )}
                          {payout.failedAt && payout.failureReason && (
                            <p><span className="font-medium text-red-600">Failed:</span> {payout.failureReason}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center space-x-2">
                    {getAvailableActions(payout).map((action) => (
                      <button
                        key={action.type}
                        onClick={() => openActionModal(action.type as any, payout)}
                        className={`px-3 py-1 text-sm font-medium border rounded-md transition-colors ${action.color} border-current hover:bg-gray-50`}
                      >
                        {action.label}
                      </button>
                    ))}
                    
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
              onPageChange={(page) => onPageChange(page - 1)}
            />
          </div>
        )}
      </div>

      {/* Action Modal */}
      {actionModal.type && actionModal.payout && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
            <div className="mt-3">
              {/* Header */}
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900 capitalize">
                  {actionModal.type} Payout
                </h3>
                <button
                  onClick={closeActionModal}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Payout Info */}
              <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                <p className="text-sm text-gray-600">
                  <span className="font-medium">Amount:</span> {formatCurrency(actionModal.payout.amount, actionModal.payout.currency)}
                </p>
                <p className="text-sm text-gray-600">
                  <span className="font-medium">Method:</span> {actionModal.payout.method.replace('_', ' ')}
                </p>
              </div>

              {/* Input Field */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {actionModal.type === 'approve' && 'Notes (Optional)'}
                  {actionModal.type === 'reject' && 'Rejection Reason *'}
                  {actionModal.type === 'process' && 'Reference Number (Optional)'}
                  {actionModal.type === 'paid' && 'Reference Number *'}
                  {actionModal.type === 'failed' && 'Failure Reason *'}
                </label>
                <textarea
                  value={actionInput}
                  onChange={(e) => setActionInput(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  rows={3}
                  placeholder={`Enter ${actionModal.type === 'approve' ? 'notes' : 
                    actionModal.type === 'reject' ? 'rejection reason' :
                    actionModal.type === 'process' ? 'reference number' :
                    actionModal.type === 'paid' ? 'reference number' :
                    'failure reason'}...`}
                />
              </div>

              {/* Actions */}
              <div className="flex justify-end space-x-3">
                <button
                  onClick={closeActionModal}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  disabled={actionLoading}
                >
                  Cancel
                </button>
                <button
                  onClick={handleAction}
                  className={`inline-flex items-center px-4 py-2 text-sm font-medium text-white border border-transparent rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${
                    actionModal.type === 'reject' || actionModal.type === 'failed'
                      ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500'
                      : 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500'
                  }`}
                  disabled={actionLoading}
                >
                  {actionLoading && <LoadingSpinner size="small" className="mr-2" />}
                  {actionLoading ? 'Processing...' : `${actionModal.type.charAt(0).toUpperCase() + actionModal.type.slice(1)}`}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PayoutManagement;