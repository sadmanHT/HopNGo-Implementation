import api from './api';

interface ProviderSummary {
  providerId: string;
  totalBookings: number;
  totalRevenue: number;
  cancellationRate: number;
  avgResponseTime: number;
  impressions: number;
  detailViews: number;
  addToCarts: number;
  conversionRate: number;
  slaCompliance: number;
  period: string;
}

interface ProviderTrend {
  date: string;
  bookings: number;
  revenue: number;
  responseTime: number;
  conversionRate: number;
}

interface ProviderAnalyticsFilters {
  startDate?: string;
  endDate?: string;
  period?: 'daily' | 'weekly' | 'monthly';
}

interface AnalyticsEvent {
  eventType: 'booking' | 'response_time' | 'funnel';
  providerId: string;
  data: Record<string, any>;
}

class ProviderAnalyticsService {
  async getProviderSummary(
    providerId: string, 
    filters: ProviderAnalyticsFilters = {}
  ): Promise<ProviderSummary> {
    try {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(
        `/api/analytics/provider/${providerId}/summary?${params.toString()}`
      );
      return response.data;
    } catch (error) {
      console.error('Failed to fetch provider summary:', error);
      throw error;
    }
  }

  async getProviderTrends(
    providerId: string,
    filters: ProviderAnalyticsFilters = {}
  ): Promise<ProviderTrend[]> {
    try {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(
        `/api/analytics/provider/${providerId}/trends?${params.toString()}`
      );
      return response.data;
    } catch (error) {
      console.error('Failed to fetch provider trends:', error);
      throw error;
    }
  }

  async recordAnalyticsEvent(event: AnalyticsEvent): Promise<void> {
    try {
      await api.post('/api/analytics/provider/events', event);
    } catch (error) {
      console.error('Failed to record analytics event:', error);
      throw error;
    }
  }

  async getHealthCheck(): Promise<{ status: string; timestamp: string }> {
    try {
      const response = await api.get('/api/analytics/provider/health');
      return response.data;
    } catch (error) {
      console.error('Failed to get analytics health check:', error);
      throw error;
    }
  }

  async getMetadata(): Promise<{
    availableMetrics: string[];
    supportedPeriods: string[];
    dataRetentionDays: number;
  }> {
    try {
      const response = await api.get('/api/analytics/provider/metadata');
      return response.data;
    } catch (error) {
      console.error('Failed to get analytics metadata:', error);
      throw error;
    }
  }

  // Utility methods for formatting data
  formatCurrency(value: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency
    }).format(value);
  }

  formatPercentage(value: number): string {
    return `${(value * 100).toFixed(1)}%`;
  }

  formatResponseTime(milliseconds: number): string {
    if (milliseconds < 1000) {
      return `${milliseconds}ms`;
    } else if (milliseconds < 60000) {
      return `${(milliseconds / 1000).toFixed(1)}s`;
    } else {
      const minutes = Math.floor(milliseconds / 60000);
      const seconds = Math.floor((milliseconds % 60000) / 1000);
      return `${minutes}m ${seconds}s`;
    }
  }

  calculateSLAStatus(compliance: number): {
    status: 'excellent' | 'good' | 'warning' | 'critical';
    color: string;
  } {
    if (compliance >= 0.95) {
      return { status: 'excellent', color: '#10B981' };
    } else if (compliance >= 0.85) {
      return { status: 'good', color: '#3B82F6' };
    } else if (compliance >= 0.70) {
      return { status: 'warning', color: '#F59E0B' };
    } else {
      return { status: 'critical', color: '#EF4444' };
    }
  }
}

export const providerAnalyticsService = new ProviderAnalyticsService();
export default providerAnalyticsService;

export type {
  ProviderSummary,
  ProviderTrend,
  ProviderAnalyticsFilters,
  AnalyticsEvent
};