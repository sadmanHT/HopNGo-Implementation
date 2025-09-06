import { apiClient } from './client';

// Types for admin API
export interface ModerationItem {
  id: string;
  type: 'POST' | 'COMMENT' | 'LISTING' | 'USER_PROFILE';
  referenceId: string;
  reportedBy: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REMOVED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  assignedTo?: string;
  createdAt: string;
  updatedAt: string;
  details?: Record<string, any>;
}

export interface AdminAuditEntry {
  id: string;
  actorUserId: string;
  actorUserName: string;
  action: string;
  targetType: string;
  targetId: string;
  targetDescription?: string;
  details: Record<string, any>;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
}

export interface ModerationDecisionRequest {
  decisionNote?: string;
}

export interface ModerationFilters {
  status?: string;
  type?: string;
  priority?: string;
  assignedTo?: string;
  page?: number;
  size?: number;
}

export interface AuditFilters {
  actorUserId?: string;
  action?: string;
  targetType?: string;
  targetId?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface BanUserRequest {
  userId: string;
  reason: string;
  duration?: 'TEMPORARY' | 'PERMANENT';
  durationDays?: number;
}

// Admin API client
export class AdminApiClient {
  private baseUrl = '/api/v1/admin';

  // Moderation endpoints
  async getModerationItems(filters?: ModerationFilters): Promise<PagedResponse<ModerationItem>> {
    const params = new URLSearchParams();
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });
    }
    
    const queryString = params.toString();
    const url = `${this.baseUrl}/moderation${queryString ? `?${queryString}` : ''}`;
    
    const response = await apiClient.get(url);
    return response.data;
  }

  async approveModerationItem(itemId: string, request?: ModerationDecisionRequest): Promise<void> {
    await apiClient.post(`${this.baseUrl}/moderation/${itemId}/approve`, request || {});
  }

  async rejectModerationItem(itemId: string, request?: ModerationDecisionRequest): Promise<void> {
    await apiClient.post(`${this.baseUrl}/moderation/${itemId}/reject`, request || {});
  }

  async removeModerationItem(itemId: string, request?: ModerationDecisionRequest): Promise<void> {
    await apiClient.post(`${this.baseUrl}/moderation/${itemId}/remove`, request || {});
  }

  async banUser(request: BanUserRequest): Promise<void> {
    await apiClient.post(`${this.baseUrl}/moderation/ban-user`, request);
  }

  // Audit endpoints
  async getAuditLogs(filters?: AuditFilters): Promise<PagedResponse<AdminAuditEntry>> {
    const params = new URLSearchParams();
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });
    }
    
    const queryString = params.toString();
    const url = `${this.baseUrl}/audit${queryString ? `?${queryString}` : ''}`;
    
    const response = await apiClient.get(url);
    return response.data;
  }

  async getRecentAuditByTarget(targetType: string, targetId: string, limit: number = 10): Promise<AdminAuditEntry[]> {
    const response = await apiClient.get(
      `${this.baseUrl}/audit/recent/${targetType}/${targetId}?limit=${limit}`
    );
    return response.data;
  }

  async getAuditCountByActor(actorUserId: string, startDate?: string, endDate?: string): Promise<number> {
    const params = new URLSearchParams({ actorUserId });
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const response = await apiClient.get(
      `${this.baseUrl}/audit/count/actor?${params.toString()}`
    );
    return response.data;
  }

  async getAuditCountByAction(action: string, startDate?: string, endDate?: string): Promise<number> {
    const params = new URLSearchParams({ action });
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const response = await apiClient.get(
      `${this.baseUrl}/audit/count/action?${params.toString()}`
    );
    return response.data;
  }

  async getAuditCountByTarget(targetType: string, targetId?: string, startDate?: string, endDate?: string): Promise<number> {
    const params = new URLSearchParams({ targetType });
    if (targetId) params.append('targetId', targetId);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const response = await apiClient.get(
      `${this.baseUrl}/audit/count/target?${params.toString()}`
    );
    return response.data;
  }

  // Dashboard stats
  async getDashboardStats(): Promise<{
    pendingReports: number;
    approvedToday: number;
    rejectedToday: number;
    removedToday: number;
    totalAuditEntries: number;
    recentActivity: AdminAuditEntry[];
  }> {
    try {
      const today = new Date().toISOString().split('T')[0];
      const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split('T')[0];
      
      const [pendingItems, auditLogs] = await Promise.all([
        this.getModerationItems({ status: 'PENDING', size: 1 }),
        this.getAuditLogs({ startDate: today, endDate: tomorrow, size: 10 })
      ]);
      
      const recentActivity = auditLogs.content;
      
      // Count actions from recent activity
      const approvedToday = recentActivity.filter(entry => 
        entry.action === 'CONTENT_APPROVED' && 
        entry.createdAt.startsWith(today)
      ).length;
      
      const rejectedToday = recentActivity.filter(entry => 
        entry.action === 'CONTENT_REJECTED' && 
        entry.createdAt.startsWith(today)
      ).length;
      
      const removedToday = recentActivity.filter(entry => 
        entry.action === 'CONTENT_REMOVED' && 
        entry.createdAt.startsWith(today)
      ).length;
      
      return {
        pendingReports: pendingItems.totalElements,
        approvedToday,
        rejectedToday,
        removedToday,
        totalAuditEntries: auditLogs.totalElements,
        recentActivity: recentActivity.slice(0, 5) // Latest 5 entries
      };
    } catch (error) {
      console.error('Error fetching dashboard stats:', error);
      // Return mock data as fallback
      return {
        pendingReports: 0,
        approvedToday: 0,
        rejectedToday: 0,
        removedToday: 0,
        totalAuditEntries: 0,
        recentActivity: []
      };
    }
  }
}

// Export singleton instance
export const adminApi = new AdminApiClient();