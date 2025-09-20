'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Download,
  Trash2,
  Shield,
  AlertTriangle,
  CheckCircle,
  Clock,
  FileText,
  User,
  Mail,
  Calendar,
  Database,
} from 'lucide-react';
import { useAuthStore } from '@/lib/state/auth';
import { CookieStatus } from '@/components/ui/cookie-banner';
import { useToast } from '@/hooks/use-toast';

interface DataExportJob {
  id: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  createdAt: string;
  completedAt?: string;
  downloadUrl?: string;
  expiresAt?: string;
}

interface AccountDeletionRequest {
  id: string;
  status: 'pending' | 'processing' | 'completed';
  scheduledAt: string;
  reason?: string;
}

export default function SettingsPage() {
  const { user, logout } = useAuthStore();
  const { toast } = useToast();
  const [isExporting, setIsExporting] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState('');
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [exportJobs, setExportJobs] = useState<DataExportJob[]>([]);
  const [deletionRequest, setDeletionRequest] = useState<AccountDeletionRequest | null>(null);

  // Mock data - replace with actual API calls
  React.useEffect(() => {
    // Load existing export jobs and deletion requests
    // This would be replaced with actual API calls
    const mockExportJobs: DataExportJob[] = [
      {
        id: '1',
        status: 'completed',
        createdAt: '2024-01-15T10:00:00Z',
        completedAt: '2024-01-15T10:05:00Z',
        downloadUrl: '/api/exports/download/1',
        expiresAt: '2024-01-22T10:05:00Z',
      },
    ];
    setExportJobs(mockExportJobs);
  }, []);

  const handleDataExport = async () => {
    setIsExporting(true);
    try {
      // API call to initiate data export
      const response = await fetch('/api/auth/me/export', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (response.ok) {
        const job = await response.json();
        setExportJobs(prev => [job, ...prev]);
        toast({
          title: 'Data Export Started',
          description: 'Your data export has been initiated. You will receive an email when it\'s ready.',
        });
      } else {
        throw new Error('Failed to start export');
      }
    } catch (error) {
      toast({
        title: 'Export Failed',
        description: 'Failed to start data export. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsExporting(false);
    }
  };

  const handleAccountDeletion = async () => {
    if (deleteConfirmation !== 'DELETE') {
      toast({
        title: 'Confirmation Required',
        description: 'Please type "DELETE" to confirm account deletion.',
        variant: 'destructive',
      });
      return;
    }

    setIsDeletingAccount(true);
    try {
      // API call to initiate account deletion
      const response = await fetch('/api/auth/me/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({
          confirmation: deleteConfirmation,
        }),
      });

      if (response.ok) {
        const request = await response.json();
        setDeletionRequest(request);
        setShowDeleteDialog(false);
        toast({
          title: 'Account Deletion Scheduled',
          description: 'Your account has been scheduled for deletion. You have 30 days to cancel this request.',
        });
      } else {
        throw new Error('Failed to delete account');
      }
    } catch (error) {
      toast({
        title: 'Deletion Failed',
        description: 'Failed to schedule account deletion. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsDeletingAccount(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'processing':
        return <Clock className="h-4 w-4 text-blue-500" />;
      case 'failed':
        return <AlertTriangle className="h-4 w-4 text-red-500" />;
      default:
        return <Clock className="h-4 w-4 text-gray-500" />;
    }
  };

  const getStatusBadge = (status: string) => {
    const variants = {
      completed: 'default',
      processing: 'secondary',
      pending: 'outline',
      failed: 'destructive',
    } as const;

    return (
      <Badge variant={variants[status as keyof typeof variants] || 'outline'}>
        {status.charAt(0).toUpperCase() + status.slice(1)}
      </Badge>
    );
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Account Settings</h1>
          <p className="text-muted-foreground">
            Manage your account preferences and data
          </p>
        </div>
      </div>

      {/* Account Information */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-4 w-4" />
            Account Information
          </CardTitle>
          <CardDescription>
            Your basic account details and verification status.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label className="text-sm font-medium">Name</Label>
              <p className="text-sm text-muted-foreground">
                {user?.firstName} {user?.lastName}
              </p>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-medium">Email</Label>
              <div className="flex items-center gap-2">
                <Mail className="h-3 w-3 text-muted-foreground" />
                <p className="text-sm text-muted-foreground">{user?.email}</p>
              </div>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-medium">Member Since</Label>
              <div className="flex items-center gap-2">
                <Calendar className="h-3 w-3 text-muted-foreground" />
                <p className="text-sm text-muted-foreground">
                  {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
                </p>
              </div>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-medium">Verification Status</Label>
              <div className="flex items-center gap-2">
                {user?.isVerified ? (
                  <>
                    <CheckCircle className="h-3 w-3 text-green-500" />
                    <span className="text-sm text-green-600">Verified</span>
                  </>
                ) : (
                  <>
                    <AlertTriangle className="h-3 w-3 text-yellow-500" />
                    <span className="text-sm text-yellow-600">Unverified</span>
                  </>
                )}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Cookie Preferences */}
      <CookieStatus />

      {/* Data Export */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Download className="h-4 w-4" />
            Download My Data
          </CardTitle>
          <CardDescription>
            Export all your personal data including profile, bookings, orders, and messages.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <FileText className="h-4 w-4" />
            <AlertDescription>
              Your data export will include: profile information, booking history, order records, 
              message metadata (content excluded for privacy), and account activity logs. 
              The export will be available for download for 7 days.
            </AlertDescription>
          </Alert>

          <div className="flex items-center gap-2">
            <Button 
              onClick={handleDataExport} 
              disabled={isExporting}
              className="flex items-center gap-2"
            >
              <Download className="h-4 w-4" />
              {isExporting ? 'Starting Export...' : 'Request Data Export'}
            </Button>
          </div>

          {exportJobs.length > 0 && (
            <div className="space-y-3">
              <Separator />
              <h4 className="font-medium">Export History</h4>
              {exportJobs.map((job) => (
                <div key={job.id} className="flex items-center justify-between p-3 border rounded-lg">
                  <div className="flex items-center gap-3">
                    {getStatusIcon(job.status)}
                    <div>
                      <p className="text-sm font-medium">
                        Export requested on {new Date(job.createdAt).toLocaleDateString()}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {job.status === 'completed' && job.completedAt && (
                          `Completed on ${new Date(job.completedAt).toLocaleDateString()}`
                        )}
                        {job.status === 'processing' && 'Processing your data...'}
                        {job.status === 'pending' && 'Waiting to start...'}
                        {job.status === 'failed' && 'Export failed - please try again'}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {getStatusBadge(job.status)}
                    {job.status === 'completed' && job.downloadUrl && (
                      <Button size="sm" variant="outline" asChild>
                        <a href={job.downloadUrl} download>
                          <Download className="h-3 w-3 mr-1" />
                          Download
                        </a>
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Account Deletion */}
      <Card className="border-red-200">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-red-600">
            <Trash2 className="h-4 w-4" />
            Delete Account
          </CardTitle>
          <CardDescription>
            Permanently delete your account and all associated data.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {deletionRequest ? (
            <Alert className="border-red-200">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>
                Your account is scheduled for deletion on{' '}
                {new Date(deletionRequest.scheduledAt).toLocaleDateString()}.
                You can cancel this request by contacting support before this date.
              </AlertDescription>
            </Alert>
          ) : (
            <>
              <Alert className="border-red-200">
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  <strong>Warning:</strong> This action cannot be undone. Your account and all 
                  associated data will be permanently deleted after a 30-day grace period. 
                  Some data may be retained for legal and accounting purposes.
                </AlertDescription>
              </Alert>

              <div className="space-y-2">
                <h4 className="font-medium text-sm">What will be deleted:</h4>
                <ul className="text-sm text-muted-foreground space-y-1 ml-4">
                  <li>• Profile information and personal data</li>
                  <li>• Booking history and preferences</li>
                  <li>• Messages and communications</li>
                  <li>• Saved places and itineraries</li>
                  <li>• Account settings and preferences</li>
                </ul>
              </div>

              <div className="space-y-2">
                <h4 className="font-medium text-sm">What will be retained:</h4>
                <ul className="text-sm text-muted-foreground space-y-1 ml-4">
                  <li>• Anonymized transaction records (for accounting)</li>
                  <li>• Invoice numbers and payment records</li>
                  <li>• Aggregated analytics data (non-identifiable)</li>
                </ul>
              </div>

              <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <DialogTrigger asChild>
                  <Button variant="destructive" className="flex items-center gap-2">
                    <Trash2 className="h-4 w-4" />
                    Delete My Account
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 text-red-600">
                      <AlertTriangle className="h-5 w-5" />
                      Confirm Account Deletion
                    </DialogTitle>
                    <DialogDescription>
                      This action will permanently delete your account and all associated data 
                      after a 30-day grace period. This cannot be undone.
                    </DialogDescription>
                  </DialogHeader>
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="delete-confirmation">
                        Type <strong>DELETE</strong> to confirm:
                      </Label>
                      <Input
                        id="delete-confirmation"
                        value={deleteConfirmation}
                        onChange={(e) => setDeleteConfirmation(e.target.value)}
                        placeholder="Type DELETE here"
                      />
                    </div>
                  </div>
                  <DialogFooter>
                    <Button
                      variant="outline"
                      onClick={() => {
                        setShowDeleteDialog(false);
                        setDeleteConfirmation('');
                      }}
                    >
                      Cancel
                    </Button>
                    <Button
                      variant="destructive"
                      onClick={handleAccountDeletion}
                      disabled={isDeletingAccount || deleteConfirmation !== 'DELETE'}
                    >
                      {isDeletingAccount ? 'Deleting...' : 'Delete Account'}
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}