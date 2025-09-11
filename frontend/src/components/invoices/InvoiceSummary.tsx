import React from 'react';
import { invoiceService } from '../../services/invoiceService';

const InvoiceSummary = ({ summary, loading }) => {
  if (loading && !summary) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {[...Array(4)].map((_, index) => (
          <div key={index} className="bg-white overflow-hidden shadow rounded-lg animate-pulse">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-gray-200 rounded"></div>
                </div>
                <div className="ml-5 w-0 flex-1">
                  <div className="h-4 bg-gray-200 rounded mb-2"></div>
                  <div className="h-6 bg-gray-200 rounded"></div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (!summary) {
    return null;
  }

  const summaryCards = [
    {
      title: 'Total Invoices',
      value: summary.totalCount?.toLocaleString() || '0',
      icon: (
        <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
      bgColor: 'bg-blue-50',
      textColor: 'text-blue-600'
    },
    {
      title: 'Total Amount',
      value: summary.totalAmount ? invoiceService.formatCurrency(summary.totalAmount, summary.currency || 'USD') : '$0.00',
      subtitle: summary.currency || 'USD',
      icon: (
        <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
        </svg>
      ),
      bgColor: 'bg-green-50',
      textColor: 'text-green-600'
    },
    {
      title: 'Paid Amount',
      value: summary.paidAmount ? invoiceService.formatCurrency(summary.paidAmount, summary.currency || 'USD') : '$0.00',
      subtitle: summary.paidCount ? `${summary.paidCount} invoice${summary.paidCount !== 1 ? 's' : ''}` : '0 invoices',
      icon: (
        <svg className="w-8 h-8 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
      bgColor: 'bg-emerald-50',
      textColor: 'text-emerald-600'
    },
    {
      title: 'Outstanding',
      value: summary.outstandingAmount ? invoiceService.formatCurrency(summary.outstandingAmount, summary.currency || 'USD') : '$0.00',
      subtitle: summary.outstandingCount ? `${summary.outstandingCount} invoice${summary.outstandingCount !== 1 ? 's' : ''}` : '0 invoices',
      icon: (
        <svg className="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
      bgColor: 'bg-yellow-50',
      textColor: 'text-yellow-600'
    }
  ];

  // Add overdue card if there are overdue invoices
  if (summary.overdueCount > 0) {
    summaryCards.push({
      title: 'Overdue',
      value: summary.overdueAmount ? invoiceService.formatCurrency(summary.overdueAmount, summary.currency || 'USD') : '$0.00',
      subtitle: `${summary.overdueCount} invoice${summary.overdueCount !== 1 ? 's' : ''}`,
      icon: (
        <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
      ),
      bgColor: 'bg-red-50',
      textColor: 'text-red-600'
    });
  }

  return (
    <div className="mb-8">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-6">
        {summaryCards.map((card, index) => (
          <div key={index} className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <div className={`p-2 rounded-lg ${card.bgColor}`}>
                    {card.icon}
                  </div>
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      {card.title}
                    </dt>
                    <dd className={`text-lg font-semibold ${card.textColor}`}>
                      {card.value}
                    </dd>
                    {card.subtitle && (
                      <dd className="text-sm text-gray-500">
                        {card.subtitle}
                      </dd>
                    )}
                  </dl>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Additional insights */}
      {summary.recentActivity && summary.recentActivity.length > 0 && (
        <div className="mt-6 bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-medium text-gray-900">Recent Activity</h3>
          </div>
          <div className="px-6 py-4">
            <div className="space-y-3">
              {summary.recentActivity.slice(0, 3).map((activity, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className={`w-2 h-2 rounded-full ${
                      activity.type === 'paid' ? 'bg-green-400' :
                      activity.type === 'issued' ? 'bg-blue-400' :
                      activity.type === 'overdue' ? 'bg-red-400' :
                      'bg-gray-400'
                    }`}></div>
                    <span className="text-sm text-gray-900">
                      Invoice {activity.invoiceNumber} {activity.action}
                    </span>
                  </div>
                  <span className="text-sm text-gray-500">
                    {new Date(activity.timestamp).toLocaleDateString()}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Payment status breakdown */}
      {(summary.paidCount > 0 || summary.outstandingCount > 0 || summary.overdueCount > 0) && (
        <div className="mt-6 bg-white shadow rounded-lg">
          <div className="px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-medium text-gray-900">Payment Status Breakdown</h3>
          </div>
          <div className="px-6 py-4">
            <div className="space-y-4">
              {summary.paidCount > 0 && (
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-4 h-4 bg-green-400 rounded"></div>
                    <span className="text-sm font-medium text-gray-900">Paid</span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium text-gray-900">
                      {summary.paidCount} invoice{summary.paidCount !== 1 ? 's' : ''}
                    </div>
                    <div className="text-sm text-gray-500">
                      {invoiceService.formatCurrency(summary.paidAmount, summary.currency || 'USD')}
                    </div>
                  </div>
                </div>
              )}
              
              {summary.outstandingCount > 0 && (
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-4 h-4 bg-yellow-400 rounded"></div>
                    <span className="text-sm font-medium text-gray-900">Outstanding</span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium text-gray-900">
                      {summary.outstandingCount} invoice{summary.outstandingCount !== 1 ? 's' : ''}
                    </div>
                    <div className="text-sm text-gray-500">
                      {invoiceService.formatCurrency(summary.outstandingAmount, summary.currency || 'USD')}
                    </div>
                  </div>
                </div>
              )}
              
              {summary.overdueCount > 0 && (
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-4 h-4 bg-red-400 rounded"></div>
                    <span className="text-sm font-medium text-gray-900">Overdue</span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium text-gray-900">
                      {summary.overdueCount} invoice{summary.overdueCount !== 1 ? 's' : ''}
                    </div>
                    <div className="text-sm text-gray-500">
                      {invoiceService.formatCurrency(summary.overdueAmount, summary.currency || 'USD')}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InvoiceSummary;