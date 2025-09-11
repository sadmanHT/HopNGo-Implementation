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
  Trash2,
  Eye,
  Filter,
  Search,
  AlertTriangle,
  Ban,
  Loader2
} from 'lucide-react';
import { useAuthStore } from '@/lib/state';
import { adminApi, ModerationItem } from '@/lib/api/admin';



export default function ModerationPage() {
  const { token } = useAuthStore();
  const [items, setItems] = useState<ModerationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [priorityFilter, setPriorityFilter] = useState<string>('all');
  const [selectedItem, setSelectedItem] = useState<ModerationItem | null>(null);
  const [showItemDialog, setShowItemDialog] = useState(false);
  const [actionDialog, setActionDialog] = useState<{
    show: boolean;
    action: 'approve' | 'reject' | 'remove' | 'ban' | null;
    item: ModerationItem | null;
  }>({ show: false, action: null, item: null });
  const [decisionNote, setDecisionNote] = useState('');

  // Fetch moderation items from API
  useEffect(() => {
    const fetchModerationItems = async () => {
      if (!token) return;
      
      try {
        setLoading(true);
        const data = await adminApi.getModerationItems();
        setItems(data.content);
      } catch (error) {
        console.error('Failed to fetch moderation items:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchModerationItems();
  }, [token]);

  const handleAction = async (action: 'approve' | 'reject' | 'remove' | 'ban', item: ModerationItem) => {
    setActionDialog({ show: true, action, item });
  };

  const confirmAction = async () => {
    if (!actionDialog.item || !actionDialog.action) return;

    try {
      const request = decisionNote ? { decisionNote } : undefined;
      
      switch (actionDialog.action) {
        case 'approve':
          await adminApi.approveModerationItem(actionDialog.item.id, request);
          break;
        case 'reject':
          await adminApi.rejectModerationItem(actionDialog.item.id, request);
          break;
        case 'remove':
          await adminApi.removeModerationItem(actionDialog.item.id, request);
          break;
        case 'ban':
          // For ban action, we need userId from the item details
          if (actionDialog.item.details?.userId) {
            await adminApi.banUser({
              userId: actionDialog.item.details.userId,
              reason: decisionNote || 'Banned via moderation',
              duration: 'PERMANENT'
            });
          }
          break;
      }
      
      // Update local state
      setItems(prev => prev.map(item => 
        item.id === actionDialog.item?.id 
          ? { ...item, status: getNewStatus(actionDialog.action!) }
          : item
      ));
      
      setActionDialog({ show: false, action: null, item: null });
      setDecisionNote('');
    } catch (error) {
      console.error('Action failed:', error);
    }
  };

  const getNewStatus = (action: string): ModerationItem['status'] => {
    switch (action) {
      case 'approve': return 'APPROVED';
      case 'reject': return 'REJECTED';
      case 'remove': return 'REMOVED';
      default: return 'PENDING';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'bg-red-100 text-red-800';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800';
      case 'LOW': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'bg-orange-100 text-orange-800';
      case 'APPROVED': return 'bg-green-100 text-green-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      case 'REMOVED': return 'bg-purple-100 text-purple-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const filteredItems = items.filter(item => {
    if (statusFilter !== 'all' && item.status !== statusFilter) return false;
    if (typeFilter !== 'all' && item.type !== typeFilter) return false;
    if (priorityFilter !== 'all' && item.priority !== priorityFilter) return false;
    if (searchTerm && !item.reason.toLowerCase().includes(searchTerm.toLowerCase()) &&
        !item.details?.title?.toLowerCase().includes(searchTerm.toLowerCase())) return false;
    return true;
  });

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-600" />
          <p className="text-gray-600">Loading moderation items...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Content Moderation</h1>
        <p className="text-gray-600 mt-1">
          Review and moderate flagged content across the platform.
        </p>
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
                placeholder="Search items..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
                <SelectItem value="REMOVED">Removed</SelectItem>
              </SelectContent>
            </Select>
            
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                <SelectItem value="POST">Posts</SelectItem>
                <SelectItem value="COMMENT">Comments</SelectItem>
                <SelectItem value="LISTING">Listings</SelectItem>
                <SelectItem value="USER_PROFILE">User Profiles</SelectItem>
              </SelectContent>
            </Select>
            
            <Select value={priorityFilter} onValueChange={setPriorityFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Priority" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Priorities</SelectItem>
                <SelectItem value="HIGH">High</SelectItem>
                <SelectItem value="MEDIUM">Medium</SelectItem>
                <SelectItem value="LOW">Low</SelectItem>
              </SelectContent>
            </Select>
            
            <Button
              variant="outline"
              onClick={() => {
                setStatusFilter('all');
                setTypeFilter('all');
                setPriorityFilter('all');
                setSearchTerm('');
              }}
            >
              Clear Filters
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Moderation Table */}
      <Card>
        <CardHeader>
          <CardTitle>Moderation Queue ({filteredItems.length} items)</CardTitle>
          <CardDescription>
            Review flagged content and take appropriate actions
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Content</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Reason</TableHead>
                <TableHead>Priority</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Reported</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredItems.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>
                    <div className="max-w-xs">
                      <p className="font-medium truncate">
                        {item.details?.title || item.reason || 'No title'}
                      </p>
                      <p className="text-sm text-gray-600 truncate">
                        {item.details?.description || 'No description'}
                      </p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{item.type}</Badge>
                  </TableCell>
                  <TableCell>
                    <span className="text-sm">{item.reason}</span>
                  </TableCell>
                  <TableCell>
                    <Badge className={getPriorityColor(item.priority)}>
                      {item.priority}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge className={getStatusColor(item.status)}>
                      {item.status}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <span className="text-sm text-gray-600">
                      {new Date(item.createdAt).toLocaleDateString()}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex space-x-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setSelectedItem(item)}
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                      {item.status === 'PENDING' && (
                        <>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-green-600 hover:text-green-700"
                            onClick={() => handleAction('approve', item)}
                          >
                            <CheckCircle className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-red-600 hover:text-red-700"
                            onClick={() => handleAction('reject', item)}
                          >
                            <XCircle className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-purple-600 hover:text-purple-700"
                            onClick={() => handleAction('remove', item)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-orange-600 hover:text-orange-700"
                            onClick={() => handleAction('ban', item)}
                          >
                            <Ban className="h-4 w-4" />
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

      {/* Action Confirmation Dialog */}
      <Dialog open={actionDialog.show} onOpenChange={(open) => !open && setActionDialog({ show: false, action: null, item: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {actionDialog.action === 'approve' && 'Approve Content'}
              {actionDialog.action === 'reject' && 'Reject Report'}
              {actionDialog.action === 'remove' && 'Remove Content'}
              {actionDialog.action === 'ban' && 'Ban User'}
            </DialogTitle>
            <DialogDescription>
              {actionDialog.action === 'approve' && 'This will mark the content as approved and dismiss the report.'}
              {actionDialog.action === 'reject' && 'This will reject the report and keep the content visible.'}
              {actionDialog.action === 'remove' && 'This will permanently remove the content from the platform.'}
              {actionDialog.action === 'ban' && 'This will ban the user who created this content.'}
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Decision Note (Optional)</label>
              <Textarea
                placeholder="Add a note explaining your decision..."
                value={decisionNote}
                onChange={(e) => setDecisionNote(e.target.value)}
                className="mt-1"
              />
            </div>
          </div>
          
          <DialogFooter>
            <Button variant="outline" onClick={() => setActionDialog({ show: false, action: null, item: null })}>
              Cancel
            </Button>
            <Button 
              onClick={confirmAction}
              className={
                actionDialog.action === 'approve' ? 'bg-green-600 hover:bg-green-700' :
                actionDialog.action === 'reject' ? 'bg-red-600 hover:bg-red-700' :
                actionDialog.action === 'remove' ? 'bg-purple-600 hover:bg-purple-700' :
                'bg-orange-600 hover:bg-orange-700'
              }
            >
              Confirm {actionDialog.action}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Item Detail Dialog */}
      <Dialog open={!!selectedItem} onOpenChange={(open) => !open && setSelectedItem(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Content Details</DialogTitle>
          </DialogHeader>
          
          {selectedItem && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-600">Type</label>
                  <p>{selectedItem.type}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Status</label>
                  <Badge className={getStatusColor(selectedItem.status)}>
                    {selectedItem.status}
                  </Badge>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Priority</label>
                  <Badge className={getPriorityColor(selectedItem.priority)}>
                    {selectedItem.priority}
                  </Badge>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Reported</label>
                  <p>{new Date(selectedItem.createdAt).toLocaleString()}</p>
                </div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-gray-600">Report Reason</label>
                <p className="mt-1">{selectedItem.reason}</p>
              </div>
              
              {selectedItem.details && (
                <div>
                  <label className="text-sm font-medium text-gray-600">Details</label>
                  <div className="mt-2 p-4 bg-gray-50 rounded-lg">
                    <h4 className="font-medium">{selectedItem.details.title || 'No title'}</h4>
                    <p className="text-sm text-gray-600 mt-1">{selectedItem.details.description || 'No description'}</p>
                  </div>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}