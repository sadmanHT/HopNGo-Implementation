'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { 
  Shield, 
  Flag, 
  FileText, 
  Users, 
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock
} from 'lucide-react';
import { adminApi, AdminAuditEntry } from '@/lib/api/admin';

interface DashboardStats {
  pendingReports: number;
  approvedToday: number;
  rejectedToday: number;
  removedToday: number;
  totalAuditEntries: number;
  recentActivity: AdminAuditEntry[];
}

export default function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    pendingReports: 0,
    approvedToday: 0,
    rejectedToday: 0,
    removedToday: 0,
    totalAuditEntries: 0,
    recentActivity: []
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const dashboardStats = await adminApi.getDashboardStats();
        setStats(dashboardStats);
      } catch (error) {
        console.error('Error fetching dashboard stats:', error);
        // Keep default stats on error
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const getActionIcon = (action: string) => {
    switch (action) {
      case 'CONTENT_APPROVED': return <CheckCircle className="h-5 w-5 text-green-600" />;
      case 'CONTENT_REJECTED': return <XCircle className="h-5 w-5 text-red-600" />;
      case 'CONTENT_REMOVED': return <AlertTriangle className="h-5 w-5 text-purple-600" />;
      case 'USER_BANNED': return <Users className="h-5 w-5 text-blue-600" />;
      default: return <Shield className="h-5 w-5 text-gray-600" />;
    }
  };

  const getActionText = (action: string) => {
    switch (action) {
      case 'CONTENT_APPROVED': return 'Content approved';
      case 'CONTENT_REJECTED': return 'Content rejected';
      case 'CONTENT_REMOVED': return 'Content removed';
      case 'USER_BANNED': return 'User banned';
      default: return action.replace('_', ' ').toLowerCase();
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }
  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-gray-600 mt-2">
          Manage content moderation, user accounts, and system audit logs.
        </p>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Pending Reports</p>
                <p className="text-2xl font-bold text-orange-600">{stats.pendingReports}</p>
              </div>
              <Clock className="h-8 w-8 text-orange-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Approved Today</p>
                <p className="text-2xl font-bold text-green-600">{stats.approvedToday}</p>
              </div>
              <CheckCircle className="h-8 w-8 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Rejected Today</p>
                <p className="text-2xl font-bold text-red-600">{stats.rejectedToday}</p>
              </div>
              <XCircle className="h-8 w-8 text-red-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Content Removed</p>
                <p className="text-2xl font-bold text-purple-600">{stats.removedToday}</p>
              </div>
              <AlertTriangle className="h-8 w-8 text-purple-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Card className="hover:shadow-lg transition-shadow">
          <CardHeader>
            <div className="flex items-center space-x-3">
              <Flag className="h-6 w-6 text-blue-600" />
              <CardTitle>Content Moderation</CardTitle>
            </div>
            <CardDescription>
              Review flagged content, approve or reject reports, and manage user-generated content.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Pending Reviews:</span>
                <span className="font-medium">{stats.pendingReports} items</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Total Audit Entries:</span>
                <span className="font-medium">{stats.totalAuditEntries} entries</span>
              </div>
              <Link href="/admin/moderation">
                <Button className="w-full">
                  Open Moderation Console
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-lg transition-shadow">
          <CardHeader>
            <div className="flex items-center space-x-3">
              <CheckCircle className="h-6 w-6 text-purple-600" />
              <CardTitle>KYC Review</CardTitle>
            </div>
            <CardDescription>
              Review and approve provider verification requests and KYC submissions.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Pending Reviews:</span>
                <span className="font-medium">0 requests</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Approved Today:</span>
                <span className="font-medium">0 requests</span>
              </div>
              <Link href="/admin/kyc">
                <Button className="w-full">
                  Open KYC Console
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-lg transition-shadow">
          <CardHeader>
            <div className="flex items-center space-x-3">
              <FileText className="h-6 w-6 text-green-600" />
              <CardTitle>Audit Logs</CardTitle>
            </div>
            <CardDescription>
              View system audit logs, track admin actions, and monitor platform activity.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Today's Actions:</span>
                <span className="font-medium">{stats.approvedToday + stats.rejectedToday + stats.removedToday} entries</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Total Entries:</span>
                <span className="font-medium">{stats.totalAuditEntries} entries</span>
              </div>
              <Link href="/admin/audit">
                <Button variant="outline" className="w-full">
                  View Audit Logs
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Admin Activity</CardTitle>
          <CardDescription>
            Latest moderation decisions and system actions
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {stats.recentActivity.length > 0 ? (
              stats.recentActivity.map((activity, index) => (
                <div key={activity.id} className={`flex items-center justify-between py-3 ${index < stats.recentActivity.length - 1 ? 'border-b' : ''}`}>
                  <div className="flex items-center space-x-3">
                    {getActionIcon(activity.action)}
                    <div>
                      <p className="font-medium">{getActionText(activity.action)}</p>
                      <p className="text-sm text-gray-600">
                        {activity.targetDescription || `${activity.targetType} ${activity.targetId}`}
                      </p>
                    </div>
                  </div>
                  <span className="text-sm text-gray-500">
                    {new Date(activity.createdAt).toLocaleString()}
                  </span>
                </div>
              ))
            ) : (
              <div className="text-center py-8">
                <Shield className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500">No recent activity</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}