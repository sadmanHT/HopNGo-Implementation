import { PayoutService, EarningsData, Payout, PayoutRequest, LedgerSummary } from '../payoutService';

// Mock fetch globally
global.fetch = jest.fn();

const mockFetch = fetch as jest.MockedFunction<typeof fetch>;

describe('PayoutService', () => {
  let payoutService: PayoutService;

  beforeEach(() => {
    payoutService = new PayoutService();
    mockFetch.mockClear();
  });

  describe('Provider Operations', () => {
    describe('getEarnings', () => {
      it('should fetch provider earnings successfully', async () => {
        const mockEarnings: EarningsData = {
          totalEarnings: 1500.00,
          availableBalance: 1200.00,
          pendingPayouts: 300.00,
          currency: 'USD',
          lastUpdated: '2024-01-15T10:00:00Z',
          monthlyEarnings: [
            { month: '2024-01', amount: 800.00 },
            { month: '2024-02', amount: 700.00 }
          ],
          recentTransactions: [
            {
              id: '1',
              type: 'RIDE_PAYMENT',
              amount: 25.00,
              currency: 'USD',
              date: '2024-01-15T09:00:00Z',
              description: 'Ride payment from user123'
            }
          ]
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockEarnings
        } as Response);

        const result = await payoutService.getEarnings('provider123');

        expect(mockFetch).toHaveBeenCalledWith('/api/payouts/provider123/earnings', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          }
        });
        expect(result).toEqual(mockEarnings);
      });

      it('should handle earnings fetch error', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 404,
          statusText: 'Not Found'
        } as Response);

        await expect(payoutService.getEarnings('invalid-provider'))
          .rejects.toThrow('Failed to fetch earnings: 404 Not Found');
      });
    });

    describe('requestPayout', () => {
      it('should create payout request successfully', async () => {
        const payoutRequest: PayoutRequest = {
          amount: 500.00,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        const mockResponse: Payout = {
          id: 'payout123',
          providerId: 'provider123',
          amount: 500.00,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          status: 'PENDING',
          requestedAt: '2024-01-15T10:00:00Z',
          bankDetails: payoutRequest.bankDetails
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse
        } as Response);

        const result = await payoutService.requestPayout('provider123', payoutRequest);

        expect(mockFetch).toHaveBeenCalledWith('/api/payouts/provider123/request', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          },
          body: JSON.stringify(payoutRequest)
        });
        expect(result).toEqual(mockResponse);
      });

      it('should validate payout amount', async () => {
        const invalidRequest: PayoutRequest = {
          amount: -100,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        await expect(payoutService.requestPayout('provider123', invalidRequest))
          .rejects.toThrow('Invalid payout amount');
      });

      it('should validate bank details for bank transfer', async () => {
        const invalidRequest: PayoutRequest = {
          amount: 100,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        await expect(payoutService.requestPayout('provider123', invalidRequest))
          .rejects.toThrow('Bank details are required for bank transfer');
      });

      it('should validate mobile money details', async () => {
        const invalidRequest: PayoutRequest = {
          amount: 100,
          currency: 'USD',
          method: 'MOBILE_MONEY',
          mobileMoneyDetails: {
            phoneNumber: '',
            provider: 'MTN',
            accountName: 'John Doe'
          }
        };

        await expect(payoutService.requestPayout('provider123', invalidRequest))
          .rejects.toThrow('Mobile money details are required');
      });
    });

    describe('cancelPayout', () => {
      it('should cancel payout successfully', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => ({ success: true })
        } as Response);

        const result = await payoutService.cancelPayout('provider123', 'payout123');

        expect(mockFetch).toHaveBeenCalledWith('/api/payouts/provider123/payout123/cancel', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          }
        });
        expect(result).toEqual({ success: true });
      });

      it('should handle cancel payout error', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 400,
          statusText: 'Bad Request'
        } as Response);

        await expect(payoutService.cancelPayout('provider123', 'payout123'))
          .rejects.toThrow('Failed to cancel payout: 400 Bad Request');
      });
    });
  });

  describe('Admin Operations', () => {
    describe('getLedgerSummary', () => {
      it('should fetch ledger summary successfully', async () => {
        const mockLedger: LedgerSummary = {
          totalRevenue: 50000.00,
          totalPayouts: 35000.00,
          pendingPayouts: 5000.00,
          availableFunds: 10000.00,
          currency: 'USD',
          lastUpdated: '2024-01-15T10:00:00Z',
          monthlyRevenue: [
            { month: '2024-01', revenue: 25000.00, payouts: 18000.00 },
            { month: '2024-02', revenue: 25000.00, payouts: 17000.00 }
          ],
          payoutStats: {
            pending: 10,
            approved: 5,
            processing: 3,
            completed: 150,
            failed: 2,
            rejected: 1
          }
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockLedger
        } as Response);

        const result = await payoutService.getLedgerSummary();

        expect(mockFetch).toHaveBeenCalledWith('/api/admin/payouts/ledger', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          }
        });
        expect(result).toEqual(mockLedger);
      });
    });

    describe('approvePayout', () => {
      it('should approve payout successfully', async () => {
        const mockResponse = { success: true, message: 'Payout approved' };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse
        } as Response);

        const result = await payoutService.approvePayout('payout123', 'Approved by admin');

        expect(mockFetch).toHaveBeenCalledWith('/api/admin/payouts/payout123/approve', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          },
          body: JSON.stringify({ notes: 'Approved by admin' })
        });
        expect(result).toEqual(mockResponse);
      });
    });

    describe('rejectPayout', () => {
      it('should reject payout successfully', async () => {
        const mockResponse = { success: true, message: 'Payout rejected' };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse
        } as Response);

        const result = await payoutService.rejectPayout('payout123', 'Insufficient documentation');

        expect(mockFetch).toHaveBeenCalledWith('/api/admin/payouts/payout123/reject', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer undefined'
          },
          body: JSON.stringify({ reason: 'Insufficient documentation' })
        });
        expect(result).toEqual(mockResponse);
      });

      it('should require rejection reason', async () => {
        await expect(payoutService.rejectPayout('payout123', ''))
          .rejects.toThrow('Rejection reason is required');
      });
    });
  });

  describe('Utility Functions', () => {
    describe('formatCurrency', () => {
      it('should format currency correctly', () => {
        expect(PayoutService.formatCurrency(1234.56, 'USD')).toBe('$1,234.56');
        expect(PayoutService.formatCurrency(1000, 'EUR')).toBe('€1,000.00');
        expect(PayoutService.formatCurrency(500.5, 'GBP')).toBe('£500.50');
      });

      it('should handle zero amounts', () => {
        expect(PayoutService.formatCurrency(0, 'USD')).toBe('$0.00');
      });

      it('should handle negative amounts', () => {
        expect(PayoutService.formatCurrency(-100, 'USD')).toBe('-$100.00');
      });
    });

    describe('calculateFees', () => {
      it('should calculate bank transfer fees correctly', () => {
        const fees = PayoutService.calculateFees(1000, 'BANK_TRANSFER');
        expect(fees.processingFee).toBe(5.00); // 0.5% with $5 minimum
        expect(fees.platformFee).toBe(10.00); // 1%
        expect(fees.totalFees).toBe(15.00);
        expect(fees.netAmount).toBe(985.00);
      });

      it('should calculate mobile money fees correctly', () => {
        const fees = PayoutService.calculateFees(500, 'MOBILE_MONEY');
        expect(fees.processingFee).toBe(5.00); // $2 + 0.6% with $5 minimum
        expect(fees.platformFee).toBe(5.00); // 1%
        expect(fees.totalFees).toBe(10.00);
        expect(fees.netAmount).toBe(490.00);
      });

      it('should apply minimum processing fees', () => {
        const fees = PayoutService.calculateFees(100, 'BANK_TRANSFER');
        expect(fees.processingFee).toBe(5.00); // Minimum fee applied
      });

      it('should handle zero amounts', () => {
        const fees = PayoutService.calculateFees(0, 'BANK_TRANSFER');
        expect(fees.processingFee).toBe(5.00); // Minimum fee
        expect(fees.platformFee).toBe(0);
        expect(fees.totalFees).toBe(5.00);
        expect(fees.netAmount).toBe(-5.00);
      });
    });

    describe('validatePayoutRequest', () => {
      it('should validate valid bank transfer request', () => {
        const request: PayoutRequest = {
          amount: 100,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        expect(() => PayoutService.validatePayoutRequest(request)).not.toThrow();
      });

      it('should validate valid mobile money request', () => {
        const request: PayoutRequest = {
          amount: 100,
          currency: 'USD',
          method: 'MOBILE_MONEY',
          mobileMoneyDetails: {
            phoneNumber: '+1234567890',
            provider: 'MTN',
            accountName: 'John Doe'
          }
        };

        expect(() => PayoutService.validatePayoutRequest(request)).not.toThrow();
      });

      it('should reject invalid amounts', () => {
        const request: PayoutRequest = {
          amount: -100,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        expect(() => PayoutService.validatePayoutRequest(request))
          .toThrow('Invalid payout amount');
      });

      it('should reject amounts below minimum', () => {
        const request: PayoutRequest = {
          amount: 5,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        expect(() => PayoutService.validatePayoutRequest(request))
          .toThrow('Minimum payout amount is $10.00');
      });

      it('should reject amounts above maximum', () => {
        const request: PayoutRequest = {
          amount: 15000,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        };

        expect(() => PayoutService.validatePayoutRequest(request))
          .toThrow('Maximum payout amount is $10,000.00');
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(payoutService.getEarnings('provider123'))
        .rejects.toThrow('Network error');
    });

    it('should handle invalid JSON responses', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => { throw new Error('Invalid JSON'); }
      } as Response);

      await expect(payoutService.getEarnings('provider123'))
        .rejects.toThrow('Invalid JSON');
    });

    it('should handle server errors', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error'
      } as Response);

      await expect(payoutService.getEarnings('provider123'))
        .rejects.toThrow('Failed to fetch earnings: 500 Internal Server Error');
    });
  });
});

// Additional test utilities
export const createMockEarningsData = (overrides: Partial<EarningsData> = {}): EarningsData => ({
  totalEarnings: 1000.00,
  availableBalance: 800.00,
  pendingPayouts: 200.00,
  currency: 'USD',
  lastUpdated: '2024-01-15T10:00:00Z',
  monthlyEarnings: [],
  recentTransactions: [],
  ...overrides
});

export const createMockPayout = (overrides: Partial<Payout> = {}): Payout => ({
  id: 'payout123',
  providerId: 'provider123',
  amount: 500.00,
  currency: 'USD',
  method: 'BANK_TRANSFER',
  status: 'PENDING',
  requestedAt: '2024-01-15T10:00:00Z',
  ...overrides
});

export const createMockLedgerSummary = (overrides: Partial<LedgerSummary> = {}): LedgerSummary => ({
  totalRevenue: 50000.00,
  totalPayouts: 35000.00,
  pendingPayouts: 5000.00,
  availableFunds: 10000.00,
  currency: 'USD',
  lastUpdated: '2024-01-15T10:00:00Z',
  monthlyRevenue: [],
  payoutStats: {
    pending: 10,
    approved: 5,
    processing: 3,
    completed: 150,
    failed: 2,
    rejected: 1
  },
  ...overrides
});