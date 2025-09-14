import React from 'react';
import { invoiceService } from '../../services/invoiceService';
import LoadingSpinner from '../ui/loading-spinner';
import Pagination from '../common/Pagination';

interface Invoice {
  id: string;
  invoiceNumber: string;
  status: string;
  total: number;
  currency: string;
  createdAt: string;
  issuedAt?: string;
  dueAt?: string;
  paidAt?: string;
  description?: string;
}

interface PaginationData {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

interface InvoiceListProps {
  invoices: Invoice[];
  loading: boolean;
  pagination: PaginationData;
  onPageChange: (page: number) => void;
  onDownloadInvoice: (id: string, invoiceNumber: string) => void;
  onDownloadReceipt: (id: string, invoiceNumber: string) => void;
}

const InvoiceList: React.FC<InvoiceListProps> = ({
  invoices,
  loading,
  pagination,
  onPageChange,
  onDownloadInvoice,
  onDownloadReceipt
}) => {
  const formatDate = (dateString: string | undefined): string => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  };

  const formatDateTime = (dateString: string | undefined): string => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status: string) => {
    const colorClass = invoiceService.getStatusColor(status);
    const statusText = invoiceService.formatStatus(status);
    
    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colorClass}`}>
        {statusText}
      </span>
    );
  };

  const getDueDateStatus = (invoice: Invoice) => {
    if (invoice.status !== 'ISSUED' || !invoice.dueAt) return null;
    
    const daysUntilDue = invoiceService.getDaysUntilDue(invoice.dueAt);
    
    if (daysUntilDue !== null && daysUntilDue < 0) {
      return (
        <span className="text-red-600 text-sm font-medium">
          Overdue by {Math.abs(daysUntilDue)} day{Math.abs(daysUntilDue) !== 1 ? 's' : ''}
        </span>
      );
    } else if (daysUntilDue !== null && daysUntilDue <= 7) {
      return (
        <span className="text-yellow-600 text-sm font-medium">
          Due in {daysUntilDue} day{daysUntilDue !== 1 ? 's' : ''}
        </span>
      );
    }
    
    return null;
  };

  const InvoiceRow: React.FC<{ invoice: Invoice }> = ({ invoice }) => {
    const canDownloadInvoice = invoiceService.canDownloadInvoice(invoice);
    const canDownloadReceipt = invoiceService.canDownloadReceipt(invoice);
    const dueDateStatus = getDueDateStatus(invoice);
    const invoiceType = invoiceService.getInvoiceType(invoice);

    return (
      <tr className="hover:bg-gray-50">
        {/* Invoice Number & Type */}
        <td className="px-6 py-4 whitespace-nowrap">
          <div className="flex flex-col">
            <div className="text-sm font-medium text-gray-900">
              {invoice.invoiceNumber}
            </div>
            <div className="text-sm text-gray-500">
              {invoiceType}
            </div>
          </div>
        </td>

        {/* Status */}
        <td className="px-6 py-4 whitespace-nowrap">
          <div className="flex flex-col space-y-1">
            {getStatusBadge(invoice.status)}
            {dueDateStatus}
          </div>
        </td>

        {/* Amount */}
        <td className="px-6 py-4 whitespace-nowrap">
          <div className="text-sm font-medium text-gray-900">
            {invoiceService.formatCurrency(invoice.total, invoice.currency)}
          </div>
          <div className="text-sm text-gray-500">
            {invoice.currency}
          </div>
        </td>

        {/* Dates */}
        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
          <div className="flex flex-col space-y-1">
            <div>
              <span className="font-medium">Created:</span> {formatDate(invoice.createdAt)}
            </div>
            {invoice.issuedAt && (
              <div>
                <span className="font-medium">Issued:</span> {formatDate(invoice.issuedAt)}
              </div>
            )}
            {invoice.dueAt && (
              <div>
                <span className="font-medium">Due:</span> {formatDate(invoice.dueAt)}
              </div>
            )}
            {invoice.paidAt && (
              <div>
                <span className="font-medium">Paid:</span> {formatDateTime(invoice.paidAt)}
              </div>
            )}
          </div>
        </td>

        {/* Description */}
        <td className="px-6 py-4">
          <div className="text-sm text-gray-900 max-w-xs truncate" title={invoice.description}>
            {invoice.description || '-'}
          </div>
        </td>

        {/* Actions */}
        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
          <div className="flex space-x-2 justify-end">
            {canDownloadInvoice && (
              <button
                onClick={() => onDownloadInvoice(invoice.id, invoice.invoiceNumber)}
                className="inline-flex items-center px-3 py-1.5 border border-gray-300 shadow-sm text-xs font-medium rounded text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                title="Download Invoice"
              >
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Invoice
              </button>
            )}
            
            {canDownloadReceipt && (
              <button
                onClick={() => onDownloadReceipt(invoice.id, invoice.invoiceNumber)}
                className="inline-flex items-center px-3 py-1.5 border border-green-300 shadow-sm text-xs font-medium rounded text-green-700 bg-green-50 hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                title="Download Receipt"
              >
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Receipt
              </button>
            )}
          </div>
        </td>
      </tr>
    );
  };

  if (loading && invoices.length === 0) {
    return (
      <div className="flex justify-center items-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex flex-col">
      {/* Table */}
      <div className="overflow-x-auto">
        <div className="inline-block min-w-full align-middle">
          <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg">
            <table className="min-w-full divide-y divide-gray-300">
              <thead className="bg-gray-50">
                <tr>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Invoice
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Amount
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Dates
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Description
                  </th>
                  <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {invoices.map((invoice) => (
                  <InvoiceRow key={invoice.id} invoice={invoice} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Loading overlay for pagination */}
      {loading && invoices.length > 0 && (
        <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center">
          <LoadingSpinner />
        </div>
      )}

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="mt-6">
          <Pagination
            currentPage={pagination.page}
            totalPages={pagination.totalPages}
            totalElements={pagination.totalElements}
            onPageChange={onPageChange}
            disabled={loading}
          />
        </div>
      )}

      {/* Results info */}
      {pagination.totalElements > 0 && (
        <div className="mt-4 text-sm text-gray-500 text-center">
          Showing {pagination.page * pagination.size + 1} to{' '}
          {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
          {pagination.totalElements} invoice{pagination.totalElements !== 1 ? 's' : ''}
        </div>
      )}
    </div>
  );
};

export default InvoiceList;