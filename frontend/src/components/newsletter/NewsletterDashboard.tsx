'use client';

import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  Mail, 
  Users, 
  TrendingUp, 
  TrendingDown, 
  Calendar,
  Tag,
  Plus,
  Minus,
  RefreshCw,
  Download,
  Search
} from 'lucide-react';
import { useNewsletter } from '@/hooks/use-newsletter';
import { formatDistanceToNow } from 'date-fns';
import type { SubscriptionStats, SubscriberInfo } from '@/lib/services/newsletter';

interface NewsletterDashboardProps {
  className?: string;
}

export function NewsletterDashboard({ className }: NewsletterDashboardProps) {
  const {
    isLoading,
    stats,
    recentSubscribers,
    loadStats,
    loadRecentSubscribers,
    addTag,
    removeTag,
  } = useNewsletter();

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSubscriber, setSelectedSubscriber] = useState<SubscriberInfo | null>(null);
  const [newTag, setNewTag] = useState('');

  useEffect(() => {
    loadStats();
    loadRecentSubscribers(50);
  }, [loadStats, loadRecentSubscribers]);

  const handleRefresh = () => {
    loadStats();
    loadRecentSubscribers(50);
  };

  const handleAddTag = async () => {
    if (!selectedSubscriber || !newTag.trim()) return;
    
    const success = await addTag(selectedSubscriber.email, newTag.trim());
    if (success) {
      setNewTag('');
      loadRecentSubscribers(50); // Refresh to show updated tags
    }
  };

  const handleRemoveTag = async (tag: string) => {
    if (!selectedSubscriber) return;
    
    const success = await removeTag(selectedSubscriber.email, tag);
    if (success) {
      loadRecentSubscribers(50); // Refresh to show updated tags
    }
  };

  const filteredSubscribers = recentSubscribers.filter(subscriber =>
    subscriber.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    subscriber.source.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (subscriber.tags && subscriber.tags.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
        return 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400';
      case 'unsubscribed':
        return 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400';
      case 'pending':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/20 dark:text-gray-400';
    }
  };

  const getSourceColor = (source: string) => {
    switch (source.toLowerCase()) {
      case 'footer':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400';
      case 'popup':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900/20 dark:text-purple-400';
      case 'referral':
        return 'bg-orange-100 text-orange-800 dark:bg-orange-900/20 dark:text-orange-400';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/20 dark:text-gray-400';
    }
  };

  return (
    <div className={className}>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Newsletter Dashboard</h1>
            <p className="text-muted-foreground">
              Manage your email subscribers and track newsletter performance
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleRefresh} disabled={isLoading}>
              <RefreshCw className={`h-4 w-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <Button variant="outline">
              <Download className="h-4 w-4 mr-2" />
              Export
            </Button>
          </div>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Subscribers</CardTitle>
                <Users className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.totalSubscribers.toLocaleString()}</div>
                <p className="text-xs text-muted-foreground">
                  {stats.activeSubscribers} active subscribers
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Today's Signups</CardTitle>
                <Calendar className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.todaySubscribers}</div>
                <p className="text-xs text-muted-foreground">
                  New subscribers today
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Weekly Growth</CardTitle>
                {stats.weeklyGrowth >= 0 ? (
                  <TrendingUp className="h-4 w-4 text-green-600" />
                ) : (
                  <TrendingDown className="h-4 w-4 text-red-600" />
                )}
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {stats.weeklyGrowth >= 0 ? '+' : ''}{stats.weeklyGrowth.toFixed(1)}%
                </div>
                <p className="text-xs text-muted-foreground">
                  vs last week
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Conversion Rate</CardTitle>
                <Mail className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.conversionRate.toFixed(1)}%</div>
                <p className="text-xs text-muted-foreground">
                  Visitor to subscriber
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Main Content */}
        <Tabs defaultValue="subscribers" className="space-y-4">
          <TabsList>
            <TabsTrigger value="subscribers">Subscribers</TabsTrigger>
            <TabsTrigger value="sources">Sources</TabsTrigger>
            <TabsTrigger value="analytics">Analytics</TabsTrigger>
          </TabsList>

          <TabsContent value="subscribers" className="space-y-4">
            {/* Search */}
            <div className="flex items-center gap-4">
              <div className="relative flex-1 max-w-sm">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search subscribers..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            {/* Subscribers List */}
            <Card>
              <CardHeader>
                <CardTitle>Recent Subscribers</CardTitle>
                <CardDescription>
                  {filteredSubscribers.length} of {recentSubscribers.length} subscribers
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {filteredSubscribers.map((subscriber) => (
                    <div
                      key={subscriber.email}
                      className="flex items-center justify-between p-4 border rounded-lg hover:bg-muted/50 cursor-pointer"
                      onClick={() => setSelectedSubscriber(subscriber)}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{subscriber.email}</span>
                          <Badge className={getStatusColor(subscriber.status)}>
                            {subscriber.status}
                          </Badge>
                          <Badge variant="outline" className={getSourceColor(subscriber.source)}>
                            {subscriber.source}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-4 text-sm text-muted-foreground">
                          <span>
                            Joined {formatDistanceToNow(new Date(subscriber.createdAt))} ago
                          </span>
                          {subscriber.userId && (
                            <span>User ID: {subscriber.userId}</span>
                          )}
                        </div>
                        {subscriber.tags && (
                          <div className="flex gap-1 mt-2">
                            {subscriber.tags.split(',').map((tag) => (
                              <Badge key={tag.trim()} variant="secondary" className="text-xs">
                                <Tag className="h-3 w-3 mr-1" />
                                {tag.trim()}
                              </Badge>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="sources" className="space-y-4">
            {stats && (
              <Card>
                <CardHeader>
                  <CardTitle>Top Subscription Sources</CardTitle>
                  <CardDescription>
                    Where your subscribers are coming from
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {stats.topSources.map((source, index) => (
                      <div key={source.source} className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center text-sm font-medium">
                            {index + 1}
                          </div>
                          <div>
                            <p className="font-medium capitalize">{source.source}</p>
                            <p className="text-sm text-muted-foreground">
                              {((source.count / stats.totalSubscribers) * 100).toFixed(1)}% of total
                            </p>
                          </div>
                        </div>
                        <div className="text-right">
                          <p className="font-bold">{source.count}</p>
                          <p className="text-sm text-muted-foreground">subscribers</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>

          <TabsContent value="analytics" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Newsletter Analytics</CardTitle>
                <CardDescription>
                  Detailed insights about your newsletter performance
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="text-center py-8 text-muted-foreground">
                  <Mail className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>Advanced analytics coming soon...</p>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        {/* Tag Management Modal */}
        {selectedSubscriber && (
          <Card className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-background border rounded-lg p-6 w-full max-w-md">
              <div className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold">Manage Subscriber</h3>
                  <p className="text-sm text-muted-foreground">{selectedSubscriber.email}</p>
                </div>
                
                <div className="space-y-2">
                  <label className="text-sm font-medium">Tags</label>
                  <div className="flex gap-1 flex-wrap">
                    {selectedSubscriber.tags ? (
                      selectedSubscriber.tags.split(',').map((tag) => (
                        <Badge key={tag.trim()} variant="secondary" className="text-xs">
                          <Tag className="h-3 w-3 mr-1" />
                          {tag.trim()}
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-4 w-4 p-0 ml-1 hover:bg-destructive hover:text-destructive-foreground"
                            onClick={() => handleRemoveTag(tag.trim())}
                          >
                            <Minus className="h-3 w-3" />
                          </Button>
                        </Badge>
                      ))
                    ) : (
                      <span className="text-sm text-muted-foreground">No tags</span>
                    )}
                  </div>
                </div>
                
                <div className="flex gap-2">
                  <Input
                    placeholder="Add new tag"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleAddTag()}
                  />
                  <Button onClick={handleAddTag} disabled={!newTag.trim()}>
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                
                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => setSelectedSubscriber(null)}>
                    Close
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}

export default NewsletterDashboard;