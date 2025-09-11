'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useAuthStore } from '@/lib/state';
import { useAnalytics } from '@/hooks/useAnalytics';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine
} from 'recharts';
import {
  TrendingUp,
  DollarSign,
  Clock,
  Eye,
  ShoppingCart,
  Calendar,
  RefreshCw,
  Download,
  AlertTriangle,
  CheckCircle,
  Target
} from 'lucide-react';
import { providerAnalyticsService, ProviderSummary, ProviderTrend } from '@/services/providerAnalyticsService';

interface SLAMarker {
  value: number;
  label: string;
  color: string;
}

export default function ProviderAnalyticsPage() {
  const { user } = useAuthStore();
  const { trackPageView, trackAction } = useAnalytics();
  
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState('30d');
  const [period, setPeriod] = useState<'daily' | 'weekly' | 'monthly'>('daily');
  const [summary, setSummary] = useState<ProviderSummary | null>(null);
  const [trends, setTrends] = useState<ProviderTrend[]>([]);
  const [error, setError] = useState<string | null>(null);

  // SLA thresholds
  const slaMarkers: SLAMarker[] = [
    { value: 0.95, label: 'Excellent (95%)', color: '#10B981' },
    { value: 0.85, label: 'Good (85%)', color: '#3B82F6' },
    { value: 0.70, label: 'Warning (70%)', color: '#F59E0B' }
  ];

  const responseTimeSLA = 5000; // 5 seconds in milliseconds

  useEffect(() => {
    if (user?.id) {
      trackPageView('/provider/analytics', 'Provider Analytics Dashboard');
      fetchAnalyticsData();
    }
  }, [user?.id, dateRange, period]);

  const fetchAnalyticsData = async () => {
    if (!user?.id) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const endDate = new Date();
      const startDate = new Date();
      
      // Calculate start date based on range
      switch (dateRange) {
        case '7d':
          startDate.setDate(endDate.getDate() - 7);
          break;
        case '30d':
          startDate.setDate(endDate.getDate() - 30);
          break;
        case '90d':
          startDate.setDate(endDate.getDate() - 90);
          break;
        default:
          startDate.setDate(endDate.getDate() - 30);
      }

      const filters = {
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0],
        period
      };

      const [summaryData, trendsData] = await Promise.all([
        providerAnalyticsService.getProviderSummary(user.id, filters),
        providerAnalyticsService.getProviderTrends(user.id, filters)
      ]);

      setSummary(summaryData);
      setTrends(trendsData);
    } catch (error) {
      console.error('Failed to fetch analytics data:', error);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    trackAction('provider_analytics_refresh', 'refresh_button', { dateRange, period });
    fetchAnalyticsData();
  };

  const handleDateRangeChange = (value: string) => {
    setDateRange(value);
    trackAction('provider_analytics_filter_change', 'date_range', { 
      oldRange: dateRange, 
      newRange: value 
    });
  };

  const handlePeriodChange = (value: 'daily' | 'weekly' | 'monthly') => {
    setPeriod(value);
    trackAction('provider_analytics_filter_change', 'period', { 
      oldPeriod: period, 
      newPeriod: value 
    });
  };

  const handleExport = () => {
    trackAction('provider_analytics_export', 'export_button', { dateRange, period });
    // TODO: Implement CSV export functionality
    alert('Export functionality will be implemented soon');
  };

  const getSLAStatus = (compliance: number) => {
    return providerAnalyticsService.calculateSLAStatus(compliance);
  };

  const formatCurrency = (value: number) => {
    return providerAnalyticsService.formatCurrency(value);
  };

  const formatPercentage = (value: number) => {
    return providerAnalyticsService.formatPercentage(value);
  };

  const formatResponseTime = (ms: number) => {
    return providerAnalyticsService.formatResponseTime(ms);
  };

  if (!user || user.role !== 'provider') {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Card>
          <CardContent className="p-6">
            <p className="text-red-600">Access denied. Provider account required.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2 text-red-600">
              <AlertTriangle className="h-5 w-5" />
              <p>{error}</p>
            </div>
            <Button onClick={handleRefresh} className="mt-4">
              Try Again
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">Provider Analytics</h1>
          <p className="text-gray-600 mt-1">Monitor your performance and optimize your listings</p>
        </div>
        <div className="flex items-center space-x-2">
          <Select value={period} onValueChange={handlePeriodChange}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="daily">Daily</SelectItem>
              <SelectItem value="weekly">Weekly</SelectItem>
              <SelectItem value="monthly">Monthly</SelectItem>
            </SelectContent>
          </Select>
          <Select value={dateRange} onValueChange={handleDateRangeChange}>
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="7d">Last 7 days</SelectItem>
              <SelectItem value="30d">Last 30 days</SelectItem>
              <SelectItem value="90d">Last 90 days</SelectItem>
            </SelectContent>
          </Select>
          <Button onClick={handleRefresh} variant="outline" size="sm" disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
          <Button onClick={handleExport} variant="outline" size="sm">
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-4 bg-gray-200 rounded mb-2"></div>
                <div className="h-8 bg-gray-200 rounded"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <>
          {/* KPI Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Total Bookings</p>
                    <p className="text-2xl font-bold">{summary?.totalBookings || 0}</p>
                  </div>
                  <TrendingUp className="h-8 w-8 text-blue-600" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Total Revenue</p>
                    <p className="text-2xl font-bold">{formatCurrency(summary?.totalRevenue || 0)}</p>
                  </div>
                  <DollarSign className="h-8 w-8 text-green-600" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">Avg Response Time</p>
                    <p className="text-2xl font-bold">{formatResponseTime(summary?.avgResponseTime || 0)}</p>
                    {summary && (
                      <Badge 
                        variant={summary.avgResponseTime <= responseTimeSLA ? "default" : "destructive"}
                        className="mt-1"
                      >
                        {summary.avgResponseTime <= responseTimeSLA ? 'Within SLA' : 'Above SLA'}
                      </Badge>
                    )}
                  </div>
                  <Clock className="h-8 w-8 text-orange-600" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">SLA Compliance</p>
                    <p className="text-2xl font-bold">{formatPercentage(summary?.slaCompliance || 0)}</p>
                    {summary && (
                      <Badge 
                        style={{ backgroundColor: getSLAStatus(summary.slaCompliance).color }}
                        className="mt-1 text-white"
                      >
                        {getSLAStatus(summary.slaCompliance).status}
                      </Badge>
                    )}
                  </div>
                  <Target className="h-8 w-8 text-purple-600" />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Charts */}
          <Tabs defaultValue="performance" className="space-y-6">
            <TabsList>
              <TabsTrigger value="performance">Performance Trends</TabsTrigger>
              <TabsTrigger value="funnel">Conversion Funnel</TabsTrigger>
              <TabsTrigger value="sla">SLA Monitoring</TabsTrigger>
            </TabsList>

            <TabsContent value="performance" className="space-y-6">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Bookings & Revenue Trends</CardTitle>
                    <CardDescription>Track your booking volume and revenue over time</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ResponsiveContainer width="100%" height={300}>
                      <LineChart data={trends}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis yAxisId="left" />
                        <YAxis yAxisId="right" orientation="right" />
                        <Tooltip />
                        <Legend />
                        <Bar yAxisId="left" dataKey="bookings" fill="#3B82F6" name="Bookings" />
                        <Line yAxisId="right" type="monotone" dataKey="revenue" stroke="#10B981" name="Revenue" />
                      </LineChart>
                    </ResponsiveContainer>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Response Time Trends</CardTitle>
                    <CardDescription>Monitor your response time performance</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ResponsiveContainer width="100%" height={300}>
                      <LineChart data={trends}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <Tooltip formatter={(value) => [formatResponseTime(value as number), 'Response Time']} />
                        <Legend />
                        <Line type="monotone" dataKey="responseTime" stroke="#F59E0B" name="Response Time" />
                        <ReferenceLine y={responseTimeSLA} stroke="#EF4444" strokeDasharray="5 5" label="SLA Threshold" />
                      </LineChart>
                    </ResponsiveContainer>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            <TabsContent value="funnel" className="space-y-6">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Conversion Funnel</CardTitle>
                    <CardDescription>Track user journey from impression to booking</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
                        <div className="flex items-center space-x-3">
                          <Eye className="h-5 w-5 text-blue-600" />
                          <span className="font-medium">Impressions</span>
                        </div>
                        <span className="text-xl font-bold">{summary?.impressions || 0}</span>
                      </div>
                      <div className="flex items-center justify-between p-4 bg-green-50 rounded-lg">
                        <div className="flex items-center space-x-3">
                          <Eye className="h-5 w-5 text-green-600" />
                          <span className="font-medium">Detail Views</span>
                        </div>
                        <span className="text-xl font-bold">{summary?.detailViews || 0}</span>
                      </div>
                      <div className="flex items-center justify-between p-4 bg-orange-50 rounded-lg">
                        <div className="flex items-center space-x-3">
                          <ShoppingCart className="h-5 w-5 text-orange-600" />
                          <span className="font-medium">Add to Cart</span>
                        </div>
                        <span className="text-xl font-bold">{summary?.addToCarts || 0}</span>
                      </div>
                      <div className="flex items-center justify-between p-4 bg-purple-50 rounded-lg">
                        <div className="flex items-center space-x-3">
                          <CheckCircle className="h-5 w-5 text-purple-600" />
                          <span className="font-medium">Bookings</span>
                        </div>
                        <span className="text-xl font-bold">{summary?.totalBookings || 0}</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Conversion Rates</CardTitle>
                    <CardDescription>Analyze conversion at each funnel stage</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ResponsiveContainer width="100%" height={300}>
                      <BarChart data={[
                        { stage: 'Impression to View', rate: summary ? (summary.detailViews / summary.impressions) : 0 },
                        { stage: 'View to Cart', rate: summary ? (summary.addToCarts / summary.detailViews) : 0 },
                        { stage: 'Cart to Booking', rate: summary ? (summary.totalBookings / summary.addToCarts) : 0 },
                        { stage: 'Overall', rate: summary?.conversionRate || 0 }
                      ]}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="stage" />
                        <YAxis tickFormatter={(value) => `${(value * 100).toFixed(1)}%`} />
                        <Tooltip formatter={(value) => [`${((value as number) * 100).toFixed(2)}%`, 'Conversion Rate']} />
                        <Bar dataKey="rate" fill="#8884D8" />
                      </BarChart>
                    </ResponsiveContainer>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            <TabsContent value="sla" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>SLA Performance Overview</CardTitle>
                  <CardDescription>Monitor your service level agreement compliance</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {slaMarkers.map((marker, index) => (
                      <div key={index} className="text-center p-4 border rounded-lg">
                        <div 
                          className="w-4 h-4 rounded-full mx-auto mb-2"
                          style={{ backgroundColor: marker.color }}
                        ></div>
                        <p className="font-medium">{marker.label}</p>
                        <p className="text-sm text-gray-600 mt-1">
                          {summary && summary.slaCompliance >= marker.value ? '✓ Achieved' : '✗ Not Met'}
                        </p>
                      </div>
                    ))}
                  </div>
                  
                  <div className="mt-6">
                    <ResponsiveContainer width="100%" height={300}>
                      <LineChart data={trends}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis tickFormatter={(value) => `${(value * 100).toFixed(0)}%`} />
                        <Tooltip formatter={(value) => [`${((value as number) * 100).toFixed(1)}%`, 'SLA Compliance']} />
                        <Legend />
                        <Line type="monotone" dataKey="conversionRate" stroke="#8884D8" name="Conversion Rate" />
                        {slaMarkers.map((marker, index) => (
                          <ReferenceLine 
                            key={index}
                            y={marker.value} 
                            stroke={marker.color} 
                            strokeDasharray="5 5" 
                            label={marker.label}
                          />
                        ))}
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </>
      )}
    </div>
  );
}