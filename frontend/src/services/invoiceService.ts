import api from './api';

/**
 * Service for invoice and receipt operations.
 */
class InvoiceService {
  /**
   * Get user invoices with pagination and filters.
   */
  async getUserInvoices(params = {}) {
    try {
      const response = await api.get('/invoices', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching user invoices:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get specific invoice by ID.
   */
  async getInvoiceById(invoiceId) {
    try {
      const response = await api.get(`/invoices/${invoiceId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching invoice:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get invoice by invoice number.
   */
  async getInvoiceByNumber(invoiceNumber) {
    try {
      const response = await api.get(`/invoices/number/${invoiceNumber}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching invoice by number:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get invoice summary for user.
   */
  async getInvoiceSummary(params = {}) {
    try {
      const response = await api.get('/invoices/summary', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching invoice summary:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Download invoice PDF.
   */
  async downloadInvoicePdf(invoiceId) {
    try {
      const response = await api.get(`/invoices/${invoiceId}/pdf`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      console.error('Error downloading invoice PDF:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Download receipt PDF.
   */
  async downloadReceiptPdf(invoiceId) {
    try {
      const response = await api.get(`/invoices/${invoiceId}/receipt`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      console.error('Error downloading receipt PDF:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Create invoice for order.
   */
  async createInvoiceForOrder(orderData) {
    try {
      const response = await api.post('/invoices/order', orderData);
      return response.data;
    } catch (error) {
      console.error('Error creating invoice for order:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Create invoice for booking.
   */
  async createInvoiceForBooking(bookingData) {
    try {
      const response = await api.post('/invoices/booking', bookingData);
      return response.data;
    } catch (error) {
      console.error('Error creating invoice for booking:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Cancel an invoice.
   */
  async cancelInvoice(invoiceId, reason) {
    try {
      const response = await api.post(`/invoices/${invoiceId}/cancel`, { reason });
      return response.data;
    } catch (error) {
      console.error('Error cancelling invoice:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get overdue invoices (Admin only).
   */
  async getOverdueInvoices() {
    try {
      const response = await api.get('/invoices/overdue');
      return response.data;
    } catch (error) {
      console.error('Error fetching overdue invoices:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Format currency amount for display.
   */
  formatCurrency(amount, currency = 'BDT') {
    const formatter = new Intl.NumberFormat('en-BD', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    return formatter.format(amount);
  }

  /**
   * Format invoice status for display.
   */
  formatStatus(status) {
    const statusMap = {
      'DRAFT': 'Draft',
      'ISSUED': 'Issued',
      'PAID': 'Paid',
      'CANCELLED': 'Cancelled',
      'REFUNDED': 'Refunded',
      'OVERDUE': 'Overdue'
    };
    return statusMap[status] || status;
  }

  /**
   * Get status color class for UI.
   */
  getStatusColor(status) {
    const colorMap = {
      'DRAFT': 'bg-gray-100 text-gray-800',
      'ISSUED': 'bg-blue-100 text-blue-800',
      'PAID': 'bg-green-100 text-green-800',
      'CANCELLED': 'bg-red-100 text-red-800',
      'REFUNDED': 'bg-yellow-100 text-yellow-800',
      'OVERDUE': 'bg-red-100 text-red-800'
    };
    return colorMap[status] || 'bg-gray-100 text-gray-800';
  }

  /**
   * Check if invoice can be downloaded.
   */
  canDownloadInvoice(invoice) {
    return invoice.status === 'ISSUED' || invoice.status === 'PAID' || invoice.status === 'OVERDUE';
  }

  /**
   * Check if receipt can be downloaded.
   */
  canDownloadReceipt(invoice) {
    return invoice.status === 'PAID';
  }

  /**
   * Check if invoice can be cancelled.
   */
  canCancelInvoice(invoice) {
    return invoice.status === 'DRAFT' || invoice.status === 'ISSUED';
  }

  /**
   * Calculate days until due date.
   */
  getDaysUntilDue(dueDate) {
    if (!dueDate) return null;
    
    const due = new Date(dueDate);
    const now = new Date();
    const diffTime = due - now;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    return diffDays;
  }

  /**
   * Check if invoice is overdue.
   */
  isOverdue(invoice) {
    if (invoice.status !== 'ISSUED' || !invoice.dueAt) return false;
    
    const daysUntilDue = this.getDaysUntilDue(invoice.dueAt);
    return daysUntilDue < 0;
  }

  /**
   * Get invoice type display name.
   */
  getInvoiceType(invoice) {
    if (invoice.orderId) return 'Order';
    if (invoice.bookingId) return 'Booking';
    return 'Invoice';
  }

  /**
   * Generate invoice breakdown for display.
   */
  getInvoiceBreakdown(invoice) {
    const breakdown = [];
    
    breakdown.push({
      label: 'Subtotal',
      amount: invoice.subtotal,
      type: 'subtotal'
    });
    
    if (invoice.tax && invoice.tax > 0) {
      breakdown.push({
        label: `Tax${invoice.taxRate ? ` (${(invoice.taxRate * 100).toFixed(1)}%)` : ''}`,
        amount: invoice.tax,
        type: 'tax'
      });
    }
    
    if (invoice.platformFee && invoice.platformFee > 0) {
      breakdown.push({
        label: `Platform Fee${invoice.platformFeeRate ? ` (${(invoice.platformFeeRate * 100).toFixed(1)}%)` : ''}`,
        amount: invoice.platformFee,
        type: 'fee'
      });
    }
    
    if (invoice.paymentProcessingFee && invoice.paymentProcessingFee > 0) {
      breakdown.push({
        label: 'Payment Processing Fee',
        amount: invoice.paymentProcessingFee,
        type: 'fee'
      });
    }
    
    breakdown.push({
      label: 'Total',
      amount: invoice.total,
      type: 'total'
    });
    
    return breakdown;
  }

  /**
   * Handle API errors.
   */
  handleError(error) {
    if (error.response) {
      // Server responded with error status
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          return new Error('Authentication required. Please log in.');
        case 403:
          return new Error('You do not have permission to access this resource.');
        case 404:
          return new Error('Invoice not found.');
        case 422:
          return new Error(data.message || 'Invalid request data.');
        case 500:
          return new Error('Server error. Please try again later.');
        default:
          return new Error(data.message || 'An unexpected error occurred.');
      }
    } else if (error.request) {
      // Network error
      return new Error('Network error. Please check your connection and try again.');
    } else {
      // Other error
      return new Error(error.message || 'An unexpected error occurred.');
    }
  }
}

// Export singleton instance
export const invoiceService = new InvoiceService();
export default invoiceService;