import { InvoiceService, Invoice, InvoiceFilters, Receipt } from '../invoiceService';

// Mock fetch globally
global.fetch = jest.fn();

const mockFetch = fetch as jest.MockedFunction<typeof fetch>;

describe('InvoiceService', () => {
  let invoiceService: InvoiceService;

  beforeEach(() => {
    invoiceService = new InvoiceService();
    mockFetch.mockClear();
  });

  describe('getInvoices', () => {
    it('should fetch invoices successfully', async () => {
      const mockInvoices = {
        content: [
          {
            id: 'inv123',
            rideId: 'ride123',
            userId: 'user123',
            providerId: 'provider123',
            amount: 25.50,
            currency: 'USD',
            status: 'PAID',
            createdAt: '2024-01-15T10:00:00Z',
            paidAt: '2024-01-15T10:05:00Z',
            breakdown: {
              baseFare: 15.00,
              distanceFare: 8.50,
              timeFare: 2.00,
              platformFee: 2.50,
              taxes: 1.50,
              discount: -4.00
            }
          }
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockInvoices
      } as Response);

      const filters: InvoiceFilters = {
        status: 'PAID',
        startDate: '2024-01-01',
        endDate: '2024-01-31',
        minAmount: 0,
        maxAmount: 100
      };

      const result = await invoiceService.getInvoices('user123', filters, 0, 10);

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/invoices/user123?status=PAID&startDate=2024-01-01&endDate=2024-01-31&minAmount=0&maxAmount=100&page=0&size=10',
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          }
        }
      );
      expect(result).toEqual(mockInvoices);
    });

    it('should handle empty filters', async () => {
      const mockInvoices = {
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockInvoices
      } as Response);

      const result = await invoiceService.getInvoices('user123', {}, 0, 10);

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/invoices/user123?page=0&size=10',
        expect.any(Object)
      );
      expect(result).toEqual(mockInvoices);
    });

    it('should handle fetch error', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: 'Not Found'
      } as Response);

      await expect(invoiceService.getInvoices('invalid-user', {}, 0, 10))
        .rejects.toThrow('Failed to fetch invoices: 404 Not Found');
    });
  });

  describe('getInvoice', () => {
    it('should fetch single invoice successfully', async () => {
      const mockInvoice: Invoice = {
        id: 'inv123',
        rideId: 'ride123',
        userId: 'user123',
        providerId: 'provider123',
        amount: 25.50,
        currency: 'USD',
        status: 'PAID',
        createdAt: '2024-01-15T10:00:00Z',
        paidAt: '2024-01-15T10:05:00Z',
        breakdown: {
          baseFare: 15.00,
          distanceFare: 8.50,
          timeFare: 2.00,
          platformFee: 2.50,
          taxes: 1.50,
          discount: -4.00
        },
        rideDetails: {
          pickupLocation: '123 Main St',
          dropoffLocation: '456 Oak Ave',
          distance: 5.2,
          duration: 15,
          vehicleType: 'STANDARD'
        }
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockInvoice
      } as Response);

      const result = await invoiceService.getInvoice('user123', 'inv123');

      expect(mockFetch).toHaveBeenCalledWith('/api/invoices/user123/inv123', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer undefined'
        }
      });
      expect(result).toEqual(mockInvoice);
    });
  });

  describe('downloadInvoice', () => {
    it('should download invoice PDF successfully', async () => {
      const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
      
      mockFetch.mockResolvedValueOnce({
        ok: true,
        blob: async () => mockBlob
      } as Response);

      // Mock URL.createObjectURL and document.createElement
      const mockUrl = 'blob:mock-url';
      global.URL.createObjectURL = jest.fn(() => mockUrl);
      global.URL.revokeObjectURL = jest.fn();
      
      const mockLink = {
        href: '',
        download: '',
        click: jest.fn(),
        style: { display: '' }
      };
      document.createElement = jest.fn(() => mockLink as any);
      document.body.appendChild = jest.fn();
      document.body.removeChild = jest.fn();

      await invoiceService.downloadInvoice('user123', 'inv123');

      expect(mockFetch).toHaveBeenCalledWith('/api/invoices/user123/inv123/pdf', {
        method: 'GET',
        headers: {
          'Authorization': 'Bearer undefined'
        }
      });
      expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
      expect(mockLink.href).toBe(mockUrl);
      expect(mockLink.download).toBe('invoice-inv123.pdf');
      expect(mockLink.click).toHaveBeenCalled();
      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith(mockUrl);
    });

    it('should handle download error', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: 'Not Found'
      } as Response);

      await expect(invoiceService.downloadInvoice('user123', 'invalid-invoice'))
        .rejects.toThrow('Failed to download invoice: 404 Not Found');
    });
  });

  describe('generateReceipt', () => {
    it('should generate receipt successfully', async () => {
      const mockReceipt: Receipt = {
        id: 'receipt123',
        invoiceId: 'inv123',
        receiptNumber: 'RCP-2024-001',
        generatedAt: '2024-01-15T10:10:00Z',
        downloadUrl: '/api/receipts/receipt123/download'
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockReceipt
      } as Response);

      const result = await invoiceService.generateReceipt('user123', 'inv123');

      expect(mockFetch).toHaveBeenCalledWith('/api/invoices/user123/inv123/receipt', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer undefined'
        }
      });
      expect(result).toEqual(mockReceipt);
    });
  });

  describe('downloadReceipt', () => {
    it('should download receipt PDF successfully', async () => {
      const mockBlob = new Blob(['Receipt PDF content'], { type: 'application/pdf' });
      
      mockFetch.mockResolvedValueOnce({
        ok: true,
        blob: async () => mockBlob
      } as Response);

      // Mock URL.createObjectURL and document.createElement
      const mockUrl = 'blob:mock-receipt-url';
      global.URL.createObjectURL = jest.fn(() => mockUrl);
      global.URL.revokeObjectURL = jest.fn();
      
      const mockLink = {
        href: '',
        download: '',
        click: jest.fn(),
        style: { display: '' }
      };
      document.createElement = jest.fn(() => mockLink as any);
      document.body.appendChild = jest.fn();
      document.body.removeChild = jest.fn();

      await invoiceService.downloadReceipt('user123', 'receipt123');

      expect(mockFetch).toHaveBeenCalledWith('/api/receipts/user123/receipt123/pdf', {
        method: 'GET',
        headers: {
          'Authorization': 'Bearer undefined'
        }
      });
      expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
      expect(mockLink.href).toBe(mockUrl);
      expect(mockLink.download).toBe('receipt-receipt123.pdf');
      expect(mockLink.click).toHaveBeenCalled();
      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith(mockUrl);
    });
  });

  describe('Utility Functions', () => {
    describe('formatCurrency', () => {
      it('should format currency correctly', () => {
        expect(InvoiceService.formatCurrency(25.50, 'USD')).toBe('$25.50');
        expect(InvoiceService.formatCurrency(1000, 'EUR')).toBe('€1,000.00');
        expect(InvoiceService.formatCurrency(0, 'GBP')).toBe('£0.00');
      });

      it('should handle negative amounts', () => {
        expect(InvoiceService.formatCurrency(-10.50, 'USD')).toBe('-$10.50');
      });
    });

    describe('calculateTotal', () => {
      it('should calculate total correctly', () => {
        const breakdown = {
          baseFare: 15.00,
          distanceFare: 8.50,
          timeFare: 2.00,
          platformFee: 2.50,
          taxes: 1.50,
          discount: -4.00
        };

        const total = InvoiceService.calculateTotal(breakdown);
        expect(total).toBe(25.50);
      });

      it('should handle zero values', () => {
        const breakdown = {
          baseFare: 0,
          distanceFare: 0,
          timeFare: 0,
          platformFee: 0,
          taxes: 0,
          discount: 0
        };

        const total = InvoiceService.calculateTotal(breakdown);
        expect(total).toBe(0);
      });

      it('should handle missing optional fields', () => {
        const breakdown = {
          baseFare: 15.00,
          distanceFare: 8.50,
          timeFare: 2.00,
          platformFee: 2.50,
          taxes: 1.50
          // discount is optional
        };

        const total = InvoiceService.calculateTotal(breakdown);
        expect(total).toBe(29.50);
      });
    });

    describe('validateInvoiceFilters', () => {
      it('should validate valid filters', () => {
        const filters: InvoiceFilters = {
          status: 'PAID',
          startDate: '2024-01-01',
          endDate: '2024-01-31',
          minAmount: 0,
          maxAmount: 100
        };

        expect(() => InvoiceService.validateInvoiceFilters(filters)).not.toThrow();
      });

      it('should reject invalid date range', () => {
        const filters: InvoiceFilters = {
          startDate: '2024-01-31',
          endDate: '2024-01-01'
        };

        expect(() => InvoiceService.validateInvoiceFilters(filters))
          .toThrow('Start date must be before end date');
      });

      it('should reject invalid amount range', () => {
        const filters: InvoiceFilters = {
          minAmount: 100,
          maxAmount: 50
        };

        expect(() => InvoiceService.validateInvoiceFilters(filters))
          .toThrow('Minimum amount must be less than maximum amount');
      });

      it('should reject negative amounts', () => {
        const filters: InvoiceFilters = {
          minAmount: -10
        };

        expect(() => InvoiceService.validateInvoiceFilters(filters))
          .toThrow('Amount values must be non-negative');
      });
    });

    describe('getStatusColor', () => {
      it('should return correct colors for each status', () => {
        expect(InvoiceService.getStatusColor('PENDING')).toBe('yellow');
        expect(InvoiceService.getStatusColor('PAID')).toBe('green');
        expect(InvoiceService.getStatusColor('FAILED')).toBe('red');
        expect(InvoiceService.getStatusColor('CANCELLED')).toBe('gray');
        expect(InvoiceService.getStatusColor('REFUNDED')).toBe('blue');
      });

      it('should return default color for unknown status', () => {
        expect(InvoiceService.getStatusColor('UNKNOWN' as any)).toBe('gray');
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(invoiceService.getInvoices('user123', {}, 0, 10))
        .rejects.toThrow('Network error');
    });

    it('should handle invalid JSON responses', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => { throw new Error('Invalid JSON'); }
      } as Response);

      await expect(invoiceService.getInvoices('user123', {}, 0, 10))
        .rejects.toThrow('Invalid JSON');
    });

    it('should handle server errors', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error'
      } as Response);

      await expect(invoiceService.getInvoices('user123', {}, 0, 10))
        .rejects.toThrow('Failed to fetch invoices: 500 Internal Server Error');
    });
  });
});

// Test utilities
export const createMockInvoice = (overrides: Partial<Invoice> = {}): Invoice => ({
  id: 'inv123',
  rideId: 'ride123',
  userId: 'user123',
  providerId: 'provider123',
  amount: 25.50,
  currency: 'USD',
  status: 'PAID',
  createdAt: '2024-01-15T10:00:00Z',
  breakdown: {
    baseFare: 15.00,
    distanceFare: 8.50,
    timeFare: 2.00,
    platformFee: 2.50,
    taxes: 1.50,
    discount: -4.00
  },
  ...overrides
});

export const createMockReceipt = (overrides: Partial<Receipt> = {}): Receipt => ({
  id: 'receipt123',
  invoiceId: 'inv123',
  receiptNumber: 'RCP-2024-001',
  generatedAt: '2024-01-15T10:10:00Z',
  downloadUrl: '/api/receipts/receipt123/download',
  ...overrides
});

export const createMockInvoiceFilters = (overrides: Partial<InvoiceFilters> = {}): InvoiceFilters => ({
  status: 'PAID',
  startDate: '2024-01-01',
  endDate: '2024-01-31',
  minAmount: 0,
  maxAmount: 1000,
  ...overrides
});