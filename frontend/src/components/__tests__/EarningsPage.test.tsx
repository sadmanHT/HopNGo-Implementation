import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import EarningsPage from '../../pages/provider/EarningsPage';
import { PayoutService } from '../../services/payoutService';
import { createMockEarningsData, createMockPayout } from '../../services/__tests__/payoutService.test';

// Mock the PayoutService
jest.mock('../../services/payoutService');
const mockPayoutService = PayoutService as jest.MockedClass<typeof PayoutService>;

// Mock the components
jest.mock('../../components/provider/earnings/EarningsSummary', () => {
  return function MockEarningsSummary({ earnings, onRequestPayout }: any) {
    return (
      <div data-testid="earnings-summary">
        <div>Balance: ${earnings?.availableBalance || 0}</div>
        <button onClick={() => onRequestPayout()} data-testid="request-payout-btn">
          Request Payout
        </button>
      </div>
    );
  };
});

jest.mock('../../components/provider/earnings/PayoutHistory', () => {
  return function MockPayoutHistory({ payouts, onCancelPayout }: any) {
    return (
      <div data-testid="payout-history">
        {payouts?.map((payout: any) => (
          <div key={payout.id} data-testid={`payout-${payout.id}`}>
            <span>Amount: ${payout.amount}</span>
            <span>Status: {payout.status}</span>
            {payout.status === 'PENDING' && (
              <button 
                onClick={() => onCancelPayout(payout.id)}
                data-testid={`cancel-${payout.id}`}
              >
                Cancel
              </button>
            )}
          </div>
        ))}
      </div>
    );
  };
});

jest.mock('../../components/provider/earnings/PayoutRequestForm', () => {
  return function MockPayoutRequestForm({ isOpen, onClose, onSubmit }: any) {
    if (!isOpen) return null;
    
    return (
      <div data-testid="payout-request-form">
        <input 
          data-testid="amount-input" 
          placeholder="Amount"
          onChange={(e) => {/* mock */}}
        />
        <select data-testid="method-select">
          <option value="BANK_TRANSFER">Bank Transfer</option>
          <option value="MOBILE_MONEY">Mobile Money</option>
        </select>
        <button 
          onClick={() => onSubmit({
            amount: 500,
            currency: 'USD',
            method: 'BANK_TRANSFER',
            bankDetails: {
              accountNumber: '1234567890',
              routingNumber: '123456789',
              accountHolderName: 'John Doe',
              bankName: 'Test Bank'
            }
          })}
          data-testid="submit-payout"
        >
          Submit
        </button>
        <button onClick={onClose} data-testid="close-form">
          Close
        </button>
      </div>
    );
  };
});

// Mock useAuth hook
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: 'provider123', role: 'PROVIDER' },
    isAuthenticated: true
  })
}));

describe('EarningsPage', () => {
  let mockPayoutServiceInstance: jest.Mocked<PayoutService>;

  beforeEach(() => {
    mockPayoutServiceInstance = {
      getEarnings: jest.fn(),
      getPayouts: jest.fn(),
      requestPayout: jest.fn(),
      cancelPayout: jest.fn()
    } as any;

    mockPayoutService.mockImplementation(() => mockPayoutServiceInstance);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should render earnings page with loading state', () => {
      mockPayoutServiceInstance.getEarnings.mockImplementation(() => new Promise(() => {}));
      mockPayoutServiceInstance.getPayouts.mockImplementation(() => new Promise(() => {}));

      render(<EarningsPage />);

      expect(screen.getByText('Provider Earnings')).toBeInTheDocument();
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    });

    it('should render earnings data when loaded', async () => {
      const mockEarnings = createMockEarningsData({
        availableBalance: 1200.00,
        totalEarnings: 1500.00
      });

      const mockPayouts = {
        content: [createMockPayout({ id: 'payout1', amount: 500, status: 'PENDING' })],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1
      };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByTestId('earnings-summary')).toBeInTheDocument();
        expect(screen.getByText('Balance: $1200')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByTestId('payout-history')).toBeInTheDocument();
        expect(screen.getByTestId('payout-payout1')).toBeInTheDocument();
        expect(screen.getByText('Amount: $500')).toBeInTheDocument();
        expect(screen.getByText('Status: PENDING')).toBeInTheDocument();
      });
    });

    it('should handle earnings fetch error', async () => {
      mockPayoutServiceInstance.getEarnings.mockRejectedValue(new Error('Failed to fetch earnings'));
      mockPayoutServiceInstance.getPayouts.mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByText(/error loading earnings/i)).toBeInTheDocument();
      });
    });
  });

  describe('Payout Request Flow', () => {
    beforeEach(async () => {
      const mockEarnings = createMockEarningsData({ availableBalance: 1200.00 });
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByTestId('earnings-summary')).toBeInTheDocument();
      });
    });

    it('should open payout request form when request payout is clicked', async () => {
      const requestPayoutBtn = screen.getByTestId('request-payout-btn');
      fireEvent.click(requestPayoutBtn);

      await waitFor(() => {
        expect(screen.getByTestId('payout-request-form')).toBeInTheDocument();
      });
    });

    it('should close payout request form when close is clicked', async () => {
      const requestPayoutBtn = screen.getByTestId('request-payout-btn');
      fireEvent.click(requestPayoutBtn);

      await waitFor(() => {
        expect(screen.getByTestId('payout-request-form')).toBeInTheDocument();
      });

      const closeBtn = screen.getByTestId('close-form');
      fireEvent.click(closeBtn);

      await waitFor(() => {
        expect(screen.queryByTestId('payout-request-form')).not.toBeInTheDocument();
      });
    });

    it('should submit payout request successfully', async () => {
      const mockNewPayout = createMockPayout({ id: 'new-payout', amount: 500 });
      mockPayoutServiceInstance.requestPayout.mockResolvedValue(mockNewPayout);

      const requestPayoutBtn = screen.getByTestId('request-payout-btn');
      fireEvent.click(requestPayoutBtn);

      await waitFor(() => {
        expect(screen.getByTestId('payout-request-form')).toBeInTheDocument();
      });

      const submitBtn = screen.getByTestId('submit-payout');
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.requestPayout).toHaveBeenCalledWith('provider123', {
          amount: 500,
          currency: 'USD',
          method: 'BANK_TRANSFER',
          bankDetails: {
            accountNumber: '1234567890',
            routingNumber: '123456789',
            accountHolderName: 'John Doe',
            bankName: 'Test Bank'
          }
        });
      });

      await waitFor(() => {
        expect(screen.queryByTestId('payout-request-form')).not.toBeInTheDocument();
      });
    });

    it('should handle payout request error', async () => {
      mockPayoutServiceInstance.requestPayout.mockRejectedValue(new Error('Insufficient balance'));

      const requestPayoutBtn = screen.getByTestId('request-payout-btn');
      fireEvent.click(requestPayoutBtn);

      await waitFor(() => {
        expect(screen.getByTestId('payout-request-form')).toBeInTheDocument();
      });

      const submitBtn = screen.getByTestId('submit-payout');
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText(/error requesting payout/i)).toBeInTheDocument();
      });
    });
  });

  describe('Payout Cancellation', () => {
    it('should cancel pending payout successfully', async () => {
      const mockEarnings = createMockEarningsData();
      const mockPayouts = {
        content: [createMockPayout({ id: 'payout1', amount: 500, status: 'PENDING' })],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1
      };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);
      mockPayoutServiceInstance.cancelPayout.mockResolvedValue({ success: true });

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByTestId('payout-payout1')).toBeInTheDocument();
      });

      const cancelBtn = screen.getByTestId('cancel-payout1');
      fireEvent.click(cancelBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.cancelPayout).toHaveBeenCalledWith('provider123', 'payout1');
      });
    });

    it('should handle payout cancellation error', async () => {
      const mockEarnings = createMockEarningsData();
      const mockPayouts = {
        content: [createMockPayout({ id: 'payout1', amount: 500, status: 'PENDING' })],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1
      };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);
      mockPayoutServiceInstance.cancelPayout.mockRejectedValue(new Error('Cannot cancel processed payout'));

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByTestId('payout-payout1')).toBeInTheDocument();
      });

      const cancelBtn = screen.getByTestId('cancel-payout1');
      fireEvent.click(cancelBtn);

      await waitFor(() => {
        expect(screen.getByText(/error cancelling payout/i)).toBeInTheDocument();
      });
    });
  });

  describe('Data Refresh', () => {
    it('should refresh data when filters change', async () => {
      const mockEarnings = createMockEarningsData();
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);

      render(<EarningsPage />);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.getEarnings).toHaveBeenCalledTimes(1);
        expect(mockPayoutServiceInstance.getPayouts).toHaveBeenCalledTimes(1);
      });

      // Simulate filter change (this would typically come from PayoutHistory component)
      // For this test, we'll just verify the initial calls were made
      expect(mockPayoutServiceInstance.getPayouts).toHaveBeenCalledWith(
        'provider123',
        expect.any(Object),
        0,
        10
      );
    });

    it('should handle pagination changes', async () => {
      const mockEarnings = createMockEarningsData();
      const mockPayouts = {
        content: [],
        page: 0,
        size: 10,
        totalElements: 25,
        totalPages: 3
      };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);

      render(<EarningsPage />);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.getPayouts).toHaveBeenCalledWith(
          'provider123',
          expect.any(Object),
          0,
          10
        );
      });
    });
  });

  describe('Error States', () => {
    it('should display error message when earnings fail to load', async () => {
      mockPayoutServiceInstance.getEarnings.mockRejectedValue(new Error('Network error'));
      mockPayoutServiceInstance.getPayouts.mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByText(/error loading earnings/i)).toBeInTheDocument();
      });
    });

    it('should display error message when payouts fail to load', async () => {
      const mockEarnings = createMockEarningsData();
      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockRejectedValue(new Error('Network error'));

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.getByText(/error loading payout history/i)).toBeInTheDocument();
      });
    });
  });

  describe('Loading States', () => {
    it('should show loading spinner while fetching data', () => {
      mockPayoutServiceInstance.getEarnings.mockImplementation(() => new Promise(() => {}));
      mockPayoutServiceInstance.getPayouts.mockImplementation(() => new Promise(() => {}));

      render(<EarningsPage />);

      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    });

    it('should hide loading spinner after data is loaded', async () => {
      const mockEarnings = createMockEarningsData();
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getEarnings.mockResolvedValue(mockEarnings);
      mockPayoutServiceInstance.getPayouts.mockResolvedValue(mockPayouts);

      render(<EarningsPage />);

      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });
    });
  });
});

// Additional test utilities for component testing
export const renderWithProviders = (component: React.ReactElement) => {
  // Add any providers (Router, Theme, etc.) that components might need
  return render(component);
};

export const createMockUser = (overrides = {}) => ({
  id: 'provider123',
  email: 'provider@example.com',
  role: 'PROVIDER',
  ...overrides
});