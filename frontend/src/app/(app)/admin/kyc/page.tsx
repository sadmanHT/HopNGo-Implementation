'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  CheckCircle,
  XCircle,
  Eye,
  Filter,
  Search,
  FileText,
  User,
  Building,
  Calendar,
  Loader2,
  Download,
  RefreshCw
} from 'lucide-react';
import { useAuthStore } from '@/lib/state';

interface KycRequest {
  id: string;
  userId: string;
  userEmail: string;
  userName: string;
  businessName: string;
  businessType: string;
  businessRegistrationNumber: string;
  businessAddress: string;
  contactPhone: string;
  taxId: string;
  bankAccountNumber: string;
  bankName: string;
  documents: {
    businessRegistration?: string;
    taxCertificate?: string;
    bankStatement?: string;
    identityDocument?: string;
  };
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  submittedAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewNotes?: string;
}

export default function KycReviewPage() {
  const { token } = useAuthStore();
  const [requests, setRequests] = useState<KycRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState<KycRequest | null>(null);
  const [showRequestDialog, setShowRequestDialog] = useState(false);
  const [actionDialog, setActionDialog] = useState<{
    show: boolean;
    action: 'approve' | 'reject' | null;
    request: KycRequest | null;
  }>({ show: false, action: null, request: null });
  const [reviewNotes, setReviewNotes] = useState('');
  const [filters, setFilters] = useState({
    status: 'all',
    businessType: 'all',
    search: '',
    dateFrom: '',
    dateTo: ''
  });

  // Fetch KYC requests from API
  useEffect(() => {
    const fetchKycRequests = async () => {
      if (!token) return;
      
      try {
        setLoading(true);
        const response = await fetch('/api/admin/kyc', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok) {
          const data = await response.json();
          setRequests(data);
        } else {
          console.error('Failed to fetch KYC requests');
        }
      } catch (error) {
        console.error('Error fetching KYC requests:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchKycRequests();
  }, [token]);

  const handleAction = (action: 'approve' | 'reject', request: KycRequest) => {
    setActionDialog({ show: true, action, request });
    setReviewNotes('');
  };

  const confirmAction = async () => {
    if (!actionDialog.request || !actionDialog.action || !token) return;

    try {
      const response = await fetch(`/api/admin/kyc/${actionDialog.request.id}/${actionDialog.action}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ notes: reviewNotes })
      });
      
      if (response.ok) {
        // Update local state
        setRequests(prev => prev.map(req => 
          req.id === actionDialog.request?.id 
            ? { 
                ...req, 
                status: actionDialog.action === 'approve' ? 'APPROVED' : 'REJECTED',
                reviewedAt: new Date().toISOString(),
                reviewNotes: reviewNotes
              }
            : req
        ));
        
        setActionDialog({ show: false, action: null, request: null });
        setReviewNotes('');
      } else {
        console.error('Action failed');
      }
    } catch (error) {
      console.error('Action failed:', error);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'bg-orange-100 text-orange-800';
      case 'APPROVED': return 'bg-green-100 text-green-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const filteredRequests = requests.filter(request => {
    if (filters.status !== 'all' && request.status !== filters.status) return false;
    if (filters.businessType !== 'all' && request.businessType !== filters.businessType) return false;
    if (filters.search && 
        !request.businessName.toLowerCase().includes(filters.search.toLowerCase()) &&
        !request.userEmail.toLowerCase().includes(filters.search.toLowerCase()) &&
        !request.userName.toLowerCase().includes(filters.search.toLowerCase())) return false;
    if (filters.dateFrom && new Date(request.submittedAt) < new Date(filters.dateFrom)) return false;
    if (filters.dateTo && new Date(request.submittedAt) > new Date(filters.dateTo + 'T23:59:59')) return false;
    return true;
  });

  const refreshData = () => {
    setLoading(true);
    // Re-fetch data
    const fetchKycRequests = async () => {
      if (!token) return;
      
      try {
        const response = await fetch('/api/admin/kyc', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok) {
          const data = await response.json();
          setRequests(data);
        }
      } catch (error) {
        console.error('Error fetching KYC requests:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchKycRequests();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-600" />
          <p className="text-gray-600">Loading KYC requests...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">KYC Review</h1>
          <p className="text-gray-600 mt-1">
            Review and approve provider verification requests.
          </p>
        </div>
        <div className="flex space-x-2">
          <Button variant="outline" onClick={refreshData}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-orange-600">
                {requests.filter(r => r.status === 'PENDING').length}
              </p>
              <p className="text-sm text-gray-600">Pending Review</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-green-600">
                {requests.filter(r => r.status === 'APPROVED').length}
              </p>
              <p className="text-sm text-gray-600">Approved</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-red-600">
                {requests.filter(r => r.status === 'REJECTED').length}
              </p>
              <p className="text-sm text-gray-600">Rejected</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-blue-600">{requests.length}</p>
              <p className="text-sm text-gray-600">Total Requests</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Filter className="h-5 w-5" />
            <span>Filters</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search requests..."
                value={filters.search}
                onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                className="pl-10"
              />
            </div>
            
            <Select value={filters.status} onValueChange={(value) => setFilters(prev => ({ ...prev, status: value }))}>
              <SelectTrigger>
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
              </SelectContent>
            </Select>
            
            <Select value={filters.businessType} onValueChange={(value) => setFilters(prev => ({ ...prev, businessType: value }))}>
              <SelectTrigger>
                <SelectValue placeholder="Business Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                <SelectItem value="INDIVIDUAL">Individual</SelectItem>
                <SelectItem value="COMPANY">Company</SelectItem>
                <SelectItem value="PARTNERSHIP">Partnership</SelectItem>
                <SelectItem value="LLC">LLC</SelectItem>
              </SelectContent>
            </Select>
            
            <Input
              type="date"
              placeholder="From Date"
              value={filters.dateFrom}
              onChange={(e) => setFilters(prev => ({ ...prev, dateFrom: e.target.value }))}
            />
            
            <Input
              type="date"
              placeholder="To Date"
              value={filters.dateTo}
              onChange={(e) => setFilters(prev => ({ ...prev, dateTo: e.target.value }))}
            />
          </div>
        </CardContent>
      </Card>

      {/* KYC Requests Table */}
      <Card>
        <CardHeader>
          <CardTitle>KYC Requests ({filteredRequests.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Business</TableHead>
                <TableHead>User</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Submitted</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredRequests.map((request) => (
                <TableRow key={request.id}>
                  <TableCell>
                    <div>
                      <p className="font-medium">{request.businessName}</p>
                      <p className="text-sm text-gray-600">{request.businessRegistrationNumber}</p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div>
                      <p className="font-medium">{request.userName}</p>
                      <p className="text-sm text-gray-600">{request.userEmail}</p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{request.businessType}</Badge>
                  </TableCell>
                  <TableCell>
                    <Badge className={getStatusColor(request.status)}>
                      {request.status}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      {new Date(request.submittedAt).toLocaleDateString()}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setSelectedRequest(request);
                          setShowRequestDialog(true);
                        }}
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                      {request.status === 'PENDING' && (
                        <>
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-green-600 hover:text-green-700"
                            onClick={() => handleAction('approve', request)}
                          >
                            <CheckCircle className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-red-600 hover:text-red-700"
                            onClick={() => handleAction('reject', request)}
                          >
                            <XCircle className="h-4 w-4" />
                          </Button>
                        </>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Request Details Dialog */}
      <Dialog open={showRequestDialog} onOpenChange={setShowRequestDialog}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>KYC Request Details</DialogTitle>
            <DialogDescription>
              Review the complete KYC submission for {selectedRequest?.businessName}
            </DialogDescription>
          </DialogHeader>
          
          {selectedRequest && (
            <div className="space-y-6">
              {/* Business Information */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center space-x-2">
                      <Building className="h-5 w-5" />
                      <span>Business Information</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div>
                      <label className="text-sm font-medium text-gray-600">Business Name</label>
                      <p className="text-sm">{selectedRequest.businessName}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Business Type</label>
                      <p className="text-sm">{selectedRequest.businessType}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Registration Number</label>
                      <p className="text-sm">{selectedRequest.businessRegistrationNumber}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Address</label>
                      <p className="text-sm">{selectedRequest.businessAddress}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Tax ID</label>
                      <p className="text-sm">{selectedRequest.taxId}</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center space-x-2">
                      <User className="h-5 w-5" />
                      <span>Contact Information</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div>
                      <label className="text-sm font-medium text-gray-600">Contact Person</label>
                      <p className="text-sm">{selectedRequest.userName}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Email</label>
                      <p className="text-sm">{selectedRequest.userEmail}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Phone</label>
                      <p className="text-sm">{selectedRequest.contactPhone}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Bank Name</label>
                      <p className="text-sm">{selectedRequest.bankName}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Account Number</label>
                      <p className="text-sm">{selectedRequest.bankAccountNumber}</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Documents */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <FileText className="h-5 w-5" />
                    <span>Submitted Documents</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {Object.entries(selectedRequest.documents).map(([docType, docUrl]) => (
                      docUrl && (
                        <div key={docType} className="border rounded-lg p-4">
                          <p className="font-medium capitalize mb-2">
                            {docType.replace(/([A-Z])/g, ' $1').trim()}
                          </p>
                          <Button variant="outline" size="sm" asChild>
                            <a href={docUrl} target="_blank" rel="noopener noreferrer">
                              <Download className="h-4 w-4 mr-2" />
                              View Document
                            </a>
                          </Button>
                        </div>
                      )
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Review Information */}
              {selectedRequest.reviewedAt && (
                <Card>
                  <CardHeader>
                    <CardTitle>Review Information</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <div>
                        <label className="text-sm font-medium text-gray-600">Reviewed At</label>
                        <p className="text-sm">{new Date(selectedRequest.reviewedAt).toLocaleString()}</p>
                      </div>
                      <div>
                        <label className="text-sm font-medium text-gray-600">Reviewed By</label>
                        <p className="text-sm">{selectedRequest.reviewedBy || 'N/A'}</p>
                      </div>
                      {selectedRequest.reviewNotes && (
                        <div>
                          <label className="text-sm font-medium text-gray-600">Review Notes</label>
                          <p className="text-sm">{selectedRequest.reviewNotes}</p>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Action Confirmation Dialog */}
      <Dialog open={actionDialog.show} onOpenChange={(open) => setActionDialog(prev => ({ ...prev, show: open }))}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {actionDialog.action === 'approve' ? 'Approve' : 'Reject'} KYC Request
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to {actionDialog.action} the KYC request for {actionDialog.request?.businessName}?
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-gray-700">Review Notes</label>
              <Textarea
                placeholder="Add notes about your decision..."
                value={reviewNotes}
                onChange={(e) => setReviewNotes(e.target.value)}
                className="mt-1"
              />
            </div>
          </div>
          
          <DialogFooter>
            <Button variant="outline" onClick={() => setActionDialog({ show: false, action: null, request: null })}>
              Cancel
            </Button>
            <Button
              onClick={confirmAction}
              className={actionDialog.action === 'approve' ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'}
            >
              {actionDialog.action === 'approve' ? 'Approve' : 'Reject'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}