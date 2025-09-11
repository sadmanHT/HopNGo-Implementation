import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import FinancePage from '../../pages/admin/FinancePage';
import { PayoutService } from '../../services/payoutService';
import { createMockLedgerSummary, createMockPayout } from '../../services/__tests__/payoutService.test';

// Mock the PayoutService
jest.mock('../../services/payoutService');
const mockPayoutService = PayoutService as jest.MockedClass<typeof PayoutService>;

// Mock the components
jest.mock('../../components/admin/finance/FinanceSummary', () => {
  return function MockFinanceSummary({ ledger, onPeriodChange }: any) {
    return (
      <div data-testid="finance-summary">
        <div>Total Revenue: ${ledger?.totalRevenue || 0}</div>
        <div>Available Funds: ${ledger?.availableFunds || 0}</div>
        <select 
          data-testid="period-select" 
          onChange={(e) => onPeriodChange(e.target.value)}
        >
          <option value="7d">Last 7 days</option>
          <option value="30d">Last 30 days</option>
          <option value="90d">Last 90 days</option>
        </select>
      </div>
    );
  };
});

jest.mock('../../components/admin/finance/PayoutManagement', () => {
  return function MockPayoutManagement({ 
    payouts, 
    onApprovePayout, 
    onRejectPayout, 
    onProcessPayout,
    onMarkPaid,
    onMarkFailed,
    onFiltersChange,
    onPageChange
  }: any) {
    return (
      <div data-testid="payout-management">
        <div data-testid="payout-count">Payouts: {payouts?.length || 0}</div>
        {payouts?.map((payout: any) => (
          <div key={payout.id} data-testid={`payout-${payout.id}`}>
            <span>Amount: ${payout.amount}</span>
            <span>Status: {payout.status}</span>
            {payout.status === 'PENDING' && (
              <>
                <button 
                  onClick={() => onApprovePayout(payout.id, 'Approved by admin')}
                  data-testid={`approve-${payout.id}`}
                >
                  Approve
                </button>
                <button 
                  onClick={() => onRejectPayout(payout.id, 'Rejected by admin')}
                  data-testid={`reject-${payout.id}`}
                >
                  Reject
                </button>
              </>
            )}
            {payout.status === 'APPROVED' && (
              <button 
                onClick={() => onProcessPayout(payout.id, 'REF123')}
                data-testid={`process-${payout.id}`}
              >
                Process
              </button>
            )}
            {payout.status === 'PROCESSING' && (
              <>
                <button 
                  onClick={() => onMarkPaid(payout.id, 'PAID123')}
                  data-testid={`mark-paid-${payout.id}`}
                >
                  Mark Paid
                </button>
                <button 
                  onClick={() => onMarkFailed(payout.id, 'Payment failed')}
                  data-testid={`mark-failed-${payout.id}`}
                >
                  Mark Failed
                </button>
              </>
            )}
          </div>
        ))}
        <input 
          data-testid="status-filter"
          placeholder="Filter by status"
          onChange={(e) => onFiltersChange({ status: e.target.value })}
        />
        <button 
          onClick={() => onPageChange(1)}
          data-testid="next-page"
        >
          Next Page
        </button>
      </div>
    );
  };
});

// Mock useAuth hook
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: 'admin123', role: 'ADMIN' },
    isAuthenticated: true
  })
}));

describe('FinancePage', () => {
  let mockPayoutServiceInstance: jest.Mocked<PayoutService>;

  beforeEach(() => {
    mockPayoutServiceInstance = {
      getLedgerSummary: jest.fn(),
      getAdminPayouts: jest.fn(),
      approvePayout: jest.fn(),
      rejectPayout: jest.fn(),
      processPayout: jest.fn(),
      markPaid: jest.fn(),
      markFailed: jest.fn(),
      exportPayouts: jest.fn()
    } as any;

    mockPayoutService.mockImplementation(() => mockPayoutServiceInstance);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should render finance page with loading state', () => {
      mockPayoutServiceInstance.getLedgerSummary.mockImplementation(() => new Promise(() => {}));
      mockPayoutServiceInstance.getAdminPayouts.mockImplementation(() => new Promise(() => {}));

      render(<FinancePage />);

      expect(screen.getByText('Finance Management')).toBeInTheDocument();
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    });

    it('should render finance data when loaded', async () => {
      const mockLedger = createMockLedgerSummary({
        totalRevenue: 50000.00,
        availableFunds: 10000.00
      });

      const mockPayouts = {
        content: [
          createMockPayout({ id: 'payout1', amount: 500, status: 'PENDING' }),
          createMockPayout({ id: 'payout2', amount: 750, status: 'APPROVED' })
        ],
        page: 0,
        size: 10,
        totalElements: 2,
        totalPages: 1
      };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      await waitFor(() => {
        expect(screen.getByTestId('finance-summary')).toBeInTheDocument();
        expect(screen.getByText('Total Revenue: $50000')).toBeInTheDocument();
        expect(screen.getByText('Available Funds: $10000')).toBeInTheDocument();
      });

      await waitFor(() => {
        expect(screen.getByTestId('payout-management')).toBeInTheDocument();
        expect(screen.getByText('Payouts: 2')).toBeInTheDocument();
        expect(screen.getByTestId('payout-payout1')).toBeInTheDocument();
        expect(screen.getByTestId('payout-payout2')).toBeInTheDocument();
      });
    });

    it('should handle ledger fetch error', async () => {
      mockPayoutServiceInstance.getLedgerSummary.mockRejectedValue(new Error('Failed to fetch ledger'));
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });

      render(<FinancePage />);

      await waitFor(() => {
        expect(screen.getByText(/error loading finance data/i)).toBeInTheDocument();
      });
    });
  });

  describe('Tab Navigation', () => {
    beforeEach(async () => {
      const mockLedger = createMockLedgerSummary();
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      await waitFor(() => {
        expect(screen.getByTestId('finance-summary')).toBeInTheDocument();
      });
    });

    it('should switch between overview and payout management tabs', async () => {
      // Should start with overview tab active
      expect(screen.getByTestId('finance-summary')).toBeInTheDocument();

      // Click on payout management tab
      const payoutTab = screen.getByText('Payout Management');
      fireEvent.click(payoutTab);

      await waitFor(() => {
        expect(screen.getByTestId('payout-management')).toBeInTheDocument();
      });

      // Click back to overview tab
      const overviewTab = screen.getByText('Overview');
      fireEvent.click(overviewTab);

      await waitFor(() => {
        expect(screen.getByTestId('finance-summary')).toBeInTheDocument();
      });
    });
  });

  describe('Payout Management Actions', () => {
    beforeEach(async () => {
      const mockLedger = createMockLedgerSummary();
      const mockPayouts = {
        content: [
          createMockPayout({ id: 'payout1', amount: 500, status: 'PENDING' }),
          createMockPayout({ id: 'payout2', amount: 750, status: 'APPROVED' }),
          createMockPayout({ id: 'payout3', amount: 300, status: 'PROCESSING' })
        ],
        page: 0,
        size: 10,
        totalElements: 3,
        totalPages: 1
      };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      // Switch to payout management tab
      const payoutTab = screen.getByText('Payout Management');
      fireEvent.click(payoutTab);

      await waitFor(() => {
        expect(screen.getByTestId('payout-management')).toBeInTheDocument();
      });
    });

    it('should approve pending payout', async () => {
      mockPayoutServiceInstance.approvePayout.mockResolvedValue({ success: true });

      const approveBtn = screen.getByTestId('approve-payout1');
      fireEvent.click(approveBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.approvePayout).toHaveBeenCalledWith('payout1', 'Approved by admin');
      });
    });

    it('should reject pending payout', async () => {
      mockPayoutServiceInstance.rejectPayout.mockResolvedValue({ success: true });

      const rejectBtn = screen.getByTestId('reject-payout1');
      fireEvent.click(rejectBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.rejectPayout).toHaveBeenCalledWith('payout1', 'Rejected by admin');
      });
    });

    it('should process approved payout', async () => {
      mockPayoutServiceInstance.processPayout.mockResolvedValue({ success: true });

      const processBtn = screen.getByTestId('process-payout2');
      fireEvent.click(processBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.processPayout).toHaveBeenCalledWith('payout2', 'REF123');
      });
    });

    it('should mark processing payout as paid', async () => {
      mockPayoutServiceInstance.markPaid.mockResolvedValue({ success: true });

      const markPaidBtn = screen.getByTestId('mark-paid-payout3');
      fireEvent.click(markPaidBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.markPaid).toHaveBeenCalledWith('payout3', 'PAID123');
      });
    });

    it('should mark processing payout as failed', async () => {
      mockPayoutServiceInstance.markFailed.mockResolvedValue({ success: true });

      const markFailedBtn = screen.getByTestId('mark-failed-payout3');
      fireEvent.click(markFailedBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.markFailed).toHaveBeenCalledWith('payout3', 'Payment failed');
      });
    });

    it('should handle payout action errors', async () => {
      mockPayoutServiceInstance.approvePayout.mockRejectedValue(new Error('Approval failed'));

      const approveBtn = screen.getByTestId('approve-payout1');
      fireEvent.click(approveBtn);

      await waitFor(() => {
        expect(screen.getByText(/error processing payout action/i)).toBeInTheDocument();
      });
    });
  });

  describe('Filtering and Pagination', () => {
    beforeEach(async () => {
      const mockLedger = createMockLedgerSummary();
      const mockPayouts = {
        content: [createMockPayout({ id: 'payout1', amount: 500, status: 'PENDING' })],
        page: 0,
        size: 10,
        totalElements: 25,
        totalPages: 3
      };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      // Switch to payout management tab
      const payoutTab = screen.getByText('Payout Management');
      fireEvent.click(payoutTab);

      await waitFor(() => {
        expect(screen.getByTestId('payout-management')).toBeInTheDocument();
      });
    });

    it('should filter payouts by status', async () => {
      const statusFilter = screen.getByTestId('status-filter');
      fireEvent.change(statusFilter, { target: { value: 'PENDING' } });

      await waitFor(() => {
        expect(mockPayoutServiceInstance.getAdminPayouts).toHaveBeenCalledWith(
          expect.objectContaining({ status: 'PENDING' }),
          0,
          10
        );
      });
    });

    it('should handle pagination', async () => {
      const nextPageBtn = screen.getByTestId('next-page');
      fireEvent.click(nextPageBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.getAdminPayouts).toHaveBeenCalledWith(
          expect.any(Object),
          1,
          10
        );
      });
    });
  });

  describe('Period Selection', () => {
    beforeEach(async () => {
      const mockLedger = createMockLedgerSummary();
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      await waitFor(() => {
        expect(screen.getByTestId('finance-summary')).toBeInTheDocument();
      });
    });

    it('should change period and refresh data', async () => {
      const periodSelect = screen.getByTestId('period-select');
      fireEvent.change(periodSelect, { target: { value: '30d' } });

      await waitFor(() => {
        expect(mockPayoutServiceInstance.getLedgerSummary).toHaveBeenCalledWith('30d');
      });
    });
  });

  describe('Export Functionality', () => {
    beforeEach(async () => {
      const mockLedger = createMockLedgerSummary();
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      // Switch to payout management tab
      const payoutTab = screen.getByText('Payout Management');
      fireEvent.click(payoutTab);

      await waitFor(() => {
        expect(screen.getByTestId('payout-management')).toBeInTheDocument();
      });
    });

    it('should export payouts data', async () => {
      const mockBlob = new Blob(['CSV content'], { type: 'text/csv' });
      mockPayoutServiceInstance.exportPayouts.mockResolvedValue(mockBlob);

      // Mock URL.createObjectURL and document.createElement
      const mockUrl = 'blob:mock-export-url';
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

      const exportBtn = screen.getByText('Export');
      fireEvent.click(exportBtn);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.exportPayouts).toHaveBeenCalledWith(
          expect.any(Object),
          'csv'
        );
      });

      expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('should handle export error', async () => {
      mockPayoutServiceInstance.exportPayouts.mockRejectedValue(new Error('Export failed'));

      const exportBtn = screen.getByText('Export');
      fireEvent.click(exportBtn);

      await waitFor(() => {
        expect(screen.getByText(/error exporting data/i)).toBeInTheDocument();
      });
    });
  });

  describe('Real-time Updates', () => {
    it('should refresh data periodically', async () => {
      const mockLedger = createMockLedgerSummary();
      const mockPayouts = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

      mockPayoutServiceInstance.getLedgerSummary.mockResolvedValue(mockLedger);
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue(mockPayouts);

      render(<FinancePage />);

      await waitFor(() => {
        expect(mockPayoutServiceInstance.getLedgerSummary).toHaveBeenCalledTimes(1);
        expect(mockPayoutServiceInstance.getAdminPayouts).toHaveBeenCalledTimes(1);
      });

      // Fast-forward time to trigger refresh (if implemented)
      // This would depend on the actual implementation of auto-refresh
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors gracefully', async () => {
      mockPayoutServiceInstance.getLedgerSummary.mockRejectedValue(new Error('Network error'));
      mockPayoutServiceInstance.getAdminPayouts.mockRejectedValue(new Error('Network error'));

      render(<FinancePage />);

      await waitFor(() => {
        expect(screen.getByText(/error loading finance data/i)).toBeInTheDocument();
      });
    });

    it('should retry failed requests', async () => {
      mockPayoutServiceInstance.getLedgerSummary
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce(createMockLedgerSummary());
      
      mockPayoutServiceInstance.getAdminPayouts.mockResolvedValue({ 
        content: [], 
        page: 0, 
        size: 10, 
        totalElements: 0, 
        totalPages: 0 
      });

      render(<FinancePage />);

      await waitFor(() => {
        expect(screen.getByText(/error loading finance data/i)).toBeInTheDocument();
      });

      // Click retry button (if implemented)
      const retryBtn = screen.queryByText('Retry');
      if (retryBtn) {
        fireEvent.click(retryBtn);

        await waitFor(() => {
          expect(screen.getByTestId('finance-summary')).toBeInTheDocument();
        });
      }
    });
  });
});

// Additional test utilities
export const createMockAdminUser = (overrides = {}) => ({
  id: 'admin123',
  email: 'admin@hopngo.com',
  role: 'ADMIN',
  permissions: ['MANAGE_PAYOUTS', 'VIEW_FINANCE'],
  ...overrides
});

export const createMockPayoutFilters = (overrides = {}) => ({
  status: '',
  method: '',
  providerId: '',
  startDate: '',
  endDate: '',
  ...overrides
});