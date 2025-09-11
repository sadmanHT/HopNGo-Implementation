import React, { useState, useEffect } from 'react';
import { invoiceService } from '../services/invoiceService';
import InvoiceList from '../components/invoices/InvoiceList';
import InvoiceFilters from '../components/invoices/InvoiceFilters';
import InvoiceSummary from '../components/invoices/InvoiceSummary';

interface Invoice {
  id: string;
  invoiceNumber: string;
  status: string;
  type: string;
  total: number;
  currency: string;
  description?: string;
  createdAt: string;
  issuedAt?: string;
  dueAt?: string;
  paidAt?: string;
}

interface InvoiceSummaryData {
  totalCount: number;
  totalAmount: number;
  paidAmount: number;
  paidCount: number;
  outstandingAmount: number;
  outstandingCount: number;
  overdueAmount?: number;
  overdueCount?: number;
  currency: string;
  recentActivity?: Array<{
    invoiceNumber: string;
    action: string;
    type: string;
    timestamp: string;
  }>;
}

interface Filters {
  search: string;
  status: string;
  type: string;
  currency: string;
  minAmount: string;
  maxAmount: string;
  startDate: string;
  endDate: string;
}

interface Pagination {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}
import LoadingSpinner from '../components/ui/loading-spinner';
import ErrorMessage from '../components/common/ErrorMessage';
import { toast } from 'react-toastify';

const InvoicesPage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [summary, setSummary] = useState<InvoiceSummaryData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<Filters>({
    search: '',
    status: '',
    type: '',
    currency: '',
    minAmount: '',
    maxAmount: '',
    startDate: '',
    endDate: ''
  });
  const [pagination, setPagination] = useState<Pagination>({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  });

  // Load invoices when component mounts or filters change
  useEffect(() => {
    if (user?.id) {
      loadInvoices();
      loadSummary();
    }
  }, [user?.id, filters, pagination.page]);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const params = {
        userId: user.id,
        page: pagination.page,
        size: pagination.size,
        ...(filters.status && { status: filters.status }),
        ...(filters.dateFrom && { dateFrom: filters.dateFrom }),
        ...(filters.dateTo && { dateTo: filters.dateTo })
      };
      
      const response = await invoiceService.getUserInvoices(params);
      
      setInvoices(response.content || []);
      setPagination(prev => ({
        ...prev,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0
      }));
    } catch (err) {
      console.error('Error loading invoices:', err);
      setError('Failed to load invoices. Please try again.');
      toast.error('Failed to load invoices');
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    try {
      const summaryData = await invoiceService.getInvoiceSummary({
        userId: user.id,
        currency: filters.currency
      });
      setSummary(summaryData);
    } catch (err) {
      console.error('Error loading summary:', err);
      // Don't show error for summary as it's not critical
    }
  };

  const handleFiltersChange = (newFilters: Filters) => {
    setFilters(newFilters);
    setPagination(prev => ({ ...prev, page: 0 })); // Reset to first page
  };

  const handleFiltersReset = () => {
    const resetFilters: Filters = {
      search: '',
      status: '',
      type: '',
      currency: '',
      minAmount: '',
      maxAmount: '',
      startDate: '',
      endDate: ''
    };
    setFilters(resetFilters);
    setPagination(prev => ({ ...prev, page: 0 }));
  };

  const handlePageChange = (newPage: number) => {
    setPagination(prev => ({ ...prev, page: newPage }));
  };

  const handleDownloadInvoice = async (invoiceId: string, invoiceNumber: string) => {
    try {
      const blob = await invoiceService.downloadInvoicePdf(invoiceId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${invoiceNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success('Invoice downloaded successfully');
    } catch (err) {
      console.error('Error downloading invoice:', err);
      toast.error('Failed to download invoice');
    }
  };

  const handleDownloadReceipt = async (invoiceId: string, invoiceNumber: string) => {
    try {
      const blob = await invoiceService.downloadReceiptPdf(invoiceId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `receipt-${invoiceNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success('Receipt downloaded successfully');
    } catch (err) {
      console.error('Error downloading receipt:', err);
      toast.error('Failed to download receipt');
    }
  };

  if (loading && invoices.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <LoadingSpinner size="large" />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Invoices & Receipts</h1>
          <p className="mt-2 text-gray-600">
            View and download your invoices and receipts
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6">
            <ErrorMessage message={error} onRetry={loadInvoices} />
          </div>
        )}

        {/* Invoice Summary */}
        {summary && (
          <div className="mb-8">
            <InvoiceSummary summary={summary} currency={filters.currency} />
          </div>
        )}

        {/* Filters */}
        <div className="mb-6">
          <InvoiceFilters 
            filters={filters}
            onFiltersChange={handleFiltersChange}
            onReset={handleFiltersReset}
            loading={loading}
          />
        </div>

        {/* Invoice List */}
        <div className="bg-white shadow-sm rounded-lg">
          <InvoiceList
            invoices={invoices}
            loading={loading}
            pagination={pagination}
            onPageChange={handlePageChange}
            onDownloadInvoice={handleDownloadInvoice}
            onDownloadReceipt={handleDownloadReceipt}
          />
        </div>

        {/* Empty State */}
        {!loading && invoices.length === 0 && (
          <div className="bg-white shadow-sm rounded-lg p-12 text-center">
            <div className="mx-auto h-12 w-12 text-gray-400">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <h3 className="mt-4 text-lg font-medium text-gray-900">
              No invoices found
            </h3>
            <p className="mt-2 text-gray-500">
              {filters.status || filters.dateFrom || filters.dateTo
                ? 'No invoices match your current filters. Try adjusting your search criteria.'
                : 'You don\'t have any invoices yet. Invoices will appear here after you make bookings or purchases.'}
            </p>
            {(filters.status || filters.dateFrom || filters.dateTo) && (
              <button
                onClick={() => setFilters({ status: '', dateFrom: '', dateTo: '', currency: 'BDT' })}
                className="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-blue-600 bg-blue-100 hover:bg-blue-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Clear filters
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default InvoicesPage;