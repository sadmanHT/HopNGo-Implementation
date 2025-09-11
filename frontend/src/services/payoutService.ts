import api from './api';

interface EarningsData {
  availableBalance: number;
  pendingBalance: number;
  totalEarnings: number;
  currency: string;
  lastPayoutDate?: string;
  nextPayoutDate?: string;
}

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
  status?: string;
  method?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

interface PayoutResponse {
  content: Payout[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

interface BankDetails {
  accountNumber: string;
  accountName: string;
  bankName: string;
  bankCode?: string;
  swiftCode?: string;
}

interface MobileMoneyDetails {
  phoneNumber: string;
  provider: string;
  accountName: string;
}

interface PayoutRequestData {
  amount: number;
  method: string;
  bankDetails?: BankDetails;
  mobileMoneyDetails?: MobileMoneyDetails;
}

// Admin interfaces
interface LedgerSummary {
  totalRevenue: number;
  totalCommissions: number;
  totalPayouts: number;
  pendingPayouts: number;
  availableBalance: number;
  currency: string;
  lastUpdated: string;
}

interface AdminPayoutFilters {
  status?: string;
  method?: string;
  providerId?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

class PayoutService {
  // Provider endpoints
  async getProviderEarnings(): Promise<EarningsData> {
    try {
      const response = await api.get('/api/provider/earnings');
      return response.data;
    } catch (error) {
      console.error('Failed to fetch provider earnings:', error);
      throw error;
    }
  }

  async getProviderPayouts(filters: PayoutFilters = {}): Promise<PayoutResponse> {
    try {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/provider/payouts?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch provider payouts:', error);
      throw error;
    }
  }

  async requestPayout(payoutData: PayoutRequestData): Promise<Payout> {
    try {
      const response = await api.post('/api/provider/payouts/request', payoutData);
      return response.data;
    } catch (error) {
      console.error('Failed to request payout:', error);
      throw error;
    }
  }

  async cancelPayout(payoutId: string): Promise<void> {
    try {
      await api.put(`/api/provider/payouts/${payoutId}/cancel`);
    } catch (error) {
      console.error('Failed to cancel payout:', error);
      throw error;
    }
  }

  async getPayoutDetails(payoutId: string): Promise<Payout> {
    try {
      const response = await api.get(`/api/provider/payouts/${payoutId}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch payout details:', error);
      throw error;
    }
  }

  // Admin endpoints
  async getLedgerSummary(): Promise<LedgerSummary> {
    try {
      const response = await api.get('/api/admin/finance/ledger/summary');
      return response.data;
    } catch (error) {
      console.error('Failed to fetch ledger summary:', error);
      throw error;
    }
  }

  async getAllPayouts(filters: AdminPayoutFilters = {}): Promise<PayoutResponse> {
    try {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/admin/finance/payouts?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch all payouts:', error);
      throw error;
    }
  }

  async approvePayout(payoutId: string, notes?: string): Promise<void> {
    try {
      await api.put(`/api/admin/finance/payouts/${payoutId}/approve`, { notes });
    } catch (error) {
      console.error('Failed to approve payout:', error);
      throw error;
    }
  }

  async rejectPayout(payoutId: string, reason: string): Promise<void> {
    try {
      await api.put(`/api/admin/finance/payouts/${payoutId}/reject`, { reason });
    } catch (error) {
      console.error('Failed to reject payout:', error);
      throw error;
    }
  }

  async processPayout(payoutId: string, referenceNumber?: string): Promise<void> {
    try {
      await api.put(`/api/admin/finance/payouts/${payoutId}/process`, { referenceNumber });
    } catch (error) {
      console.error('Failed to process payout:', error);
      throw error;
    }
  }

  async markPayoutPaid(payoutId: string, referenceNumber: string): Promise<void> {
    try {
      await api.put(`/api/admin/finance/payouts/${payoutId}/paid`, { referenceNumber });
    } catch (error) {
      console.error('Failed to mark payout as paid:', error);
      throw error;
    }
  }

  async markPayoutFailed(payoutId: string, reason: string): Promise<void> {
    try {
      await api.put(`/api/admin/finance/payouts/${payoutId}/failed`, { reason });
    } catch (error) {
      console.error('Failed to mark payout as failed:', error);
      throw error;
    }
  }

  // Utility methods
  async downloadPayoutReport(filters: AdminPayoutFilters = {}): Promise<Blob> {
    try {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/admin/finance/payouts/export?${params.toString()}`, {
        responseType: 'blob'
      });
      
      return response.data;
    } catch (error) {
      console.error('Failed to download payout report:', error);
      throw error;
    }
  }

  async getPayoutStatistics(startDate?: string, endDate?: string): Promise<{
    totalPayouts: number;
    totalAmount: number;
    averageAmount: number;
    payoutsByStatus: Record<string, number>;
    payoutsByMethod: Record<string, number>;
    currency: string;
  }> {
    try {
      const params = new URLSearchParams();
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);

      const response = await api.get(`/api/admin/finance/payouts/statistics?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch payout statistics:', error);
      throw error;
    }
  }
}

export const payoutService = new PayoutService();
export default payoutService;

// Export types for use in components
export type {
  EarningsData,
  Payout,
  PayoutFilters,
  PayoutResponse,
  PayoutRequestData,
  BankDetails,
  MobileMoneyDetails,
  LedgerSummary,
  AdminPayoutFilters
};