'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Calendar,
  Search,
  Filter,
  Eye,
  Download,
  RefreshCw,
  User,
  FileText,
  Shield,
  Loader2
} from 'lucide-react';
import { useAuthStore } from '@/lib/state';
import { adminApi, AdminAuditEntry } from '@/lib/api/admin';

export default function AuditPage() {
  const { token } = useAuthStore();
  const [entries, setEntries] = useState<AdminAuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedEntry, setSelectedEntry] = useState<AdminAuditEntry | null>(null);
  const [showEntryDialog, setShowEntryDialog] = useState(false);
  const [filters, setFilters] = useState({
    action: 'all',
    targetType: 'all',
    actor: '',
    search: '',
    dateFrom: '',
    dateTo: ''
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const itemsPerPage = 20;

  const fetchAuditEntries = async () => {
    if (!token) return;
    
    try {
      setLoading(true);
      const response = await adminApi.getAuditLogs({
        page: currentPage,
        size: itemsPerPage,
        action: filters.action !== 'all' ? filters.action : undefined,
        targetType: filters.targetType !== 'all' ? filters.targetType : undefined,
        actorUserId: filters.actor || undefined,
        startDate: filters.dateFrom || undefined,
        endDate: filters.dateTo || undefined
      });
      
      setEntries(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Failed to fetch audit entries:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditEntries();
  }, [token, currentPage, filters]);

  const getActionColor = (action: string) => {
    switch (action) {
      case 'CONTENT_APPROVED': return 'bg-green-100 text-green-800';
      case 'CONTENT_REJECTED': return 'bg-red-100 text-red-800';
      case 'CONTENT_REMOVED': return 'bg-purple-100 text-purple-800';
      case 'USER_BANNED': return 'bg-orange-100 text-orange-800';
      case 'MODERATION_ASSIGNED': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getTargetTypeIcon = (targetType: string) => {
    switch (targetType) {
      case 'USER': return <User className="h-4 w-4" />;
      case 'POST': case 'COMMENT': case 'LISTING': return <FileText className="h-4 w-4" />;
      case 'MODERATION_ITEM': return <Shield className="h-4 w-4" />;
      default: return <FileText className="h-4 w-4" />;
    }
  };

  const filteredEntries = entries.filter(entry => {
    if (filters.action !== 'all' && entry.action !== filters.action) return false;
    if (filters.targetType !== 'all' && entry.targetType !== filters.targetType) return false;
    if (filters.actor && !entry.actorUserName.toLowerCase().includes(filters.actor.toLowerCase())) return false;
    if (filters.search && 
        !entry.action.toLowerCase().includes(filters.search.toLowerCase()) &&
        !entry.targetDescription?.toLowerCase().includes(filters.search.toLowerCase())) return false;
    if (filters.dateFrom && new Date(entry.createdAt) < new Date(filters.dateFrom)) return false;
    if (filters.dateTo && new Date(entry.createdAt) > new Date(filters.dateTo + 'T23:59:59')) return false;
    return true;
  });

  const paginatedEntries = filteredEntries.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const exportAuditLog = () => {
    // In a real implementation, this would call an API to generate and download a CSV/Excel file
    console.log('Exporting audit log...');
  };

  const refreshData = () => {
    fetchAuditEntries();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-600" />
          <p className="text-gray-600">Loading audit logs...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Audit Logs</h1>
          <p className="text-gray-600 mt-1">
            Track all administrative actions and system events.
          </p>
        </div>
        <div className="flex space-x-2">
          <Button variant="outline" onClick={refreshData}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
          <Button variant="outline" onClick={exportAuditLog}>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-blue-600">{entries.length}</p>
              <p className="text-sm text-gray-600">Total Entries</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-green-600">
                {entries.filter(e => e.action === 'CONTENT_APPROVED').length}
              </p>
              <p className="text-sm text-gray-600">Approved</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-red-600">
                {entries.filter(e => e.action === 'CONTENT_REJECTED').length}
              </p>
              <p className="text-sm text-gray-600">Rejected</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-purple-600">
                {entries.filter(e => e.action === 'CONTENT_REMOVED').length}
              </p>
              <p className="text-sm text-gray-600">Removed</p>
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
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search actions..."
                value={filters.search}
                onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                className="pl-10"
              />
            </div>
            
            <Select value={filters.action} onValueChange={(value) => setFilters(prev => ({ ...prev, action: value }))}>
              <SelectTrigger>
                <SelectValue placeholder="Action" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Actions</SelectItem>
                <SelectItem value="CONTENT_APPROVED">Content Approved</SelectItem>
                <SelectItem value="CONTENT_REJECTED">Content Rejected</SelectItem>
                <SelectItem value="CONTENT_REMOVED">Content Removed</SelectItem>
                <SelectItem value="USER_BANNED">User Banned</SelectItem>
                <SelectItem value="MODERATION_ASSIGNED">Moderation Assigned</SelectItem>
              </SelectContent>
            </Select>
            
            <Select value={filters.targetType} onValueChange={(value) => setFilters(prev => ({ ...prev, targetType: value }))}>
              <SelectTrigger>
                <SelectValue placeholder="Target Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                <SelectItem value="POST">Posts</SelectItem>
                <SelectItem value="COMMENT">Comments</SelectItem>
                <SelectItem value="LISTING">Listings</SelectItem>
                <SelectItem value="USER">Users</SelectItem>
                <SelectItem value="MODERATION_ITEM">Moderation Items</SelectItem>
              </SelectContent>
            </Select>
            
            <Input
              placeholder="Actor name..."
              value={filters.actor}
              onChange={(e) => setFilters(prev => ({ ...prev, actor: e.target.value }))}
            />
            
            <Input
              type="date"
              placeholder="From date"
              value={filters.dateFrom}
              onChange={(e) => setFilters(prev => ({ ...prev, dateFrom: e.target.value }))}
            />
            
            <Input
              type="date"
              placeholder="To date"
              value={filters.dateTo}
              onChange={(e) => setFilters(prev => ({ ...prev, dateTo: e.target.value }))}
            />
          </div>
          
          <div className="mt-4">
            <Button 
              variant="outline" 
              onClick={() => setFilters({ action: 'all', targetType: 'all', actor: '', search: '', dateFrom: '', dateTo: '' })}
            >
              Clear All Filters
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Audit Table */}
      <Card>
        <CardHeader>
          <CardTitle>Audit Entries ({filteredEntries.length} entries)</CardTitle>
          <CardDescription>
            Detailed log of all administrative actions and system events
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Timestamp</TableHead>
                <TableHead>Actor</TableHead>
                <TableHead>Action</TableHead>
                <TableHead>Target</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>IP Address</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {paginatedEntries.map((entry) => (
                <TableRow key={entry.id}>
                  <TableCell>
                    <span className="text-sm">
                      {new Date(entry.createdAt).toLocaleString()}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <User className="h-4 w-4 text-gray-400" />
                      <span className="font-medium">{entry.actorUserName}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge className={getActionColor(entry.action)}>
                      {entry.action.replace('_', ' ')}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      {getTargetTypeIcon(entry.targetType)}
                      <span className="text-sm">{entry.targetType}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <span className="text-sm max-w-xs truncate block">
                      {entry.targetDescription || 'No description'}
                    </span>
                  </TableCell>
                  <TableCell>
                    <span className="text-sm text-gray-600">{entry.ipAddress}</span>
                  </TableCell>
                  <TableCell>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setSelectedEntry(entry)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          
          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center mt-4">
              <p className="text-sm text-gray-600">
                Showing {(currentPage - 1) * itemsPerPage + 1} to {Math.min(currentPage * itemsPerPage, filteredEntries.length)} of {filteredEntries.length} entries
              </p>
              <div className="flex space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(prev => prev - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage(prev => prev + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Entry Detail Dialog */}
      <Dialog open={!!selectedEntry} onOpenChange={(open) => !open && setSelectedEntry(null)}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Audit Entry Details</DialogTitle>
            <DialogDescription>
              Detailed information about this administrative action
            </DialogDescription>
          </DialogHeader>
          
          {selectedEntry && (
            <div className="space-y-6">
              {/* Basic Info */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-600">Timestamp</label>
                  <p>{new Date(selectedEntry.createdAt).toLocaleString()}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Actor</label>
                  <p>{selectedEntry.actorUserName} ({selectedEntry.actorUserId})</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Action</label>
                  <Badge className={getActionColor(selectedEntry.action)}>
                    {selectedEntry.action.replace('_', ' ')}
                  </Badge>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Target Type</label>
                  <p>{selectedEntry.targetType}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Target ID</label>
                  <p className="font-mono text-sm">{selectedEntry.targetId}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">IP Address</label>
                  <p className="font-mono text-sm">{selectedEntry.ipAddress}</p>
                </div>
              </div>
              
              {/* Target Description */}
              {selectedEntry.targetDescription && (
                <div>
                  <label className="text-sm font-medium text-gray-600">Target Description</label>
                  <p className="mt-1">{selectedEntry.targetDescription}</p>
                </div>
              )}
              
              {/* User Agent */}
              <div>
                <label className="text-sm font-medium text-gray-600">User Agent</label>
                <p className="mt-1 text-sm font-mono bg-gray-50 p-2 rounded">
                  {selectedEntry.userAgent}
                </p>
              </div>
              
              {/* Details */}
              <div>
                <label className="text-sm font-medium text-gray-600">Action Details</label>
                <pre className="mt-1 text-sm bg-gray-50 p-4 rounded overflow-auto max-h-64">
                  {JSON.stringify(selectedEntry.details, null, 2)}
                </pre>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}