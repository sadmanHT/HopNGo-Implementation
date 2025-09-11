'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import BookingStatusTracker from '@/components/BookingStatusTracker';
import { BookingSearchExperiment } from '@/components/bookings/BookingSearchExperiment';
import { bookingsApi } from '@/lib/api/bookings';
import { Booking, CancellationResponse } from '@/lib/api/types';
import { useAuthStore } from '@/lib/state';
import { useExperiment } from '@/lib/flags';
import analytics from '@/lib/analytics';
import {
  Plus,
  Search,
  Calendar,
  DollarSign,
  MapPin,
  Clock,
  CheckCircle,
  XCircle,
  Eye,
  RefreshCw,
  AlertTriangle,
  Ban
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

export default function BookingsPage() {
  const router = useRouter();
  const { user, token } = useAuthStore();
  
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedBooking, setSelectedBooking] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [cancellingBooking, setCancellingBooking] = useState<string | null>(null);
  const [showCancelDialog, setShowCancelDialog] = useState<string | null>(null);
  const [cancellationReason, setCancellationReason] = useState('');
  const [cancellationResult, setCancellationResult] = useState<CancellationResponse | null>(null);
  
  // A/B experiment for booking search
  const { variant: searchVariant, trackEvent } = useExperiment('booking-search-layout');

  useEffect(() => {
    if (!user || !token) {
      router.push('/login');
      return;
    }
    fetchBookings();
  }, [user, token, router]);
  
  useEffect(() => {
    // Track page view for experiment
    if (user) {
      trackEvent('bookings_page_viewed', {
        variant: searchVariant,
        user_id: user.id
      });
    }
  }, [user, searchVariant, trackEvent]);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      const response = await bookingsApi.getUserBookings(0, 50);
      
      if (response.success && response.data) {
        setBookings(response.data);
      }
    } catch (error) {
      console.error('Failed to fetch bookings:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    
    // Track refresh action
    analytics.trackAction('bookings_refresh', 'refresh_button', {
      userId: user?.id,
      bookingsCount: bookings.length
    });
    
    await fetchBookings();
    setRefreshing(false);
  };

  const handleCancelBooking = async (bookingId: string) => {
    if (!cancellationReason.trim()) {
      alert('Please provide a reason for cancellation');
      return;
    }

    setCancellingBooking(bookingId);
    
    try {
      const response = await bookingsApi.cancelBookingWithRefund(bookingId, {
        reason: cancellationReason
      });
      
      if (response.success && response.data) {
        setCancellationResult(response.data);
        
        // Track cancellation
        analytics.trackAction('booking_cancelled', 'cancel_button', {
          userId: user?.id,
          bookingId,
          refundAmount: response.data.refundAmount
        });
        
        // Refresh bookings to show updated status
        await fetchBookings();
        
        // Close dialog
        setShowCancelDialog(null);
        setCancellationReason('');
      }
    } catch (error) {
      console.error('Failed to cancel booking:', error);
      alert('Failed to cancel booking. Please try again.');
    } finally {
      setCancellingBooking(null);
    }
  };

  const canCancelBooking = (booking: Booking) => {
    return booking.status === 'confirmed' || booking.status === 'pending';
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'confirmed':
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      case 'cancelled':
        return <XCircle className="h-4 w-4 text-red-600" />;
      case 'pending':
        return <Clock className="h-4 w-4 text-yellow-600" />;
      default:
        return <Clock className="h-4 w-4 text-gray-600" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'confirmed':
        return 'bg-green-100 text-green-800';
      case 'cancelled':
        return 'bg-red-100 text-red-800';
      case 'pending':
        return 'bg-yellow-100 text-yellow-800';
      case 'completed':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getServiceTypeIcon = (serviceType: string) => {
    switch (serviceType) {
      case 'transport':
        return '🚗';
      case 'accommodation':
        return '🏨';
      case 'activity':
        return '🎯';
      default:
        return '📋';
    }
  };

  const filteredBookings = bookings.filter(booking =>
    booking.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    booking.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    booking.serviceType.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const formatPrice = (price: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(price);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (!user) {
    return null;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">My Bookings</h1>
          <p className="text-gray-600 mt-1">Manage and track your service bookings</p>
        </div>
        <div className="flex space-x-2">
          <Button
            onClick={handleRefresh}
            variant="outline"
            size="sm"
            disabled={refreshing}
            className="flex items-center space-x-2"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </Button>
          <Link href="/checkout">
            <Button className="flex items-center space-x-2">
              <Plus className="h-4 w-4" />
              <span>New Booking</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* Search - A/B Experiment */}
      <BookingSearchExperiment 
        searchQuery={searchQuery}
        onSearchChange={(query) => {
          setSearchQuery(query);
          // Track search usage for experiment
          trackEvent('booking_search_used', {
            variant: searchVariant,
            query_length: query.length
          });
          
          // Track search usage for analytics
          analytics.trackAction('booking_search', 'search_input', {
            query: query.substring(0, 50), // Limit query length for privacy
            queryLength: query.length,
            resultsCount: filteredBookings.length,
            variant: searchVariant
          });
        }}
      />

      {/* Loading State */}
      {loading && (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      )}

      {/* Empty State */}
      {!loading && filteredBookings.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <div className="text-center">
              <Calendar className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                {searchQuery ? 'No bookings found' : 'No bookings yet'}
              </h3>
              <p className="text-gray-600 mb-4">
                {searchQuery 
                  ? 'Try adjusting your search terms'
                  : 'Create your first booking to get started'
                }
              </p>
              {!searchQuery && (
                <Link href="/checkout">
                  <Button>
                    <Plus className="h-4 w-4 mr-2" />
                    Create Booking
                  </Button>
                </Link>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Bookings List */}
      {!loading && filteredBookings.length > 0 && (
        <div className="space-y-4">
          {filteredBookings.map((booking) => (
            <Card key={booking.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className="text-2xl">
                      {getServiceTypeIcon(booking.serviceType)}
                    </div>
                    <div>
                      <CardTitle className="text-lg">{booking.title}</CardTitle>
                      <p className="text-sm text-gray-600 mt-1">{booking.description}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    {getStatusIcon(booking.status)}
                    <Badge className={getStatusColor(booking.status)}>
                      {booking.status.charAt(0).toUpperCase() + booking.status.slice(1)}
                    </Badge>
                  </div>
                </div>
              </CardHeader>
              
              <CardContent className="space-y-4">
                {/* Booking Details */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                  <div className="flex items-center space-x-2">
                    <DollarSign className="h-4 w-4 text-gray-500" />
                    <span className="font-medium">
                      {formatPrice(booking.price, booking.currency)}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Calendar className="h-4 w-4 text-gray-500" />
                    <span>{formatDate(booking.startDate)}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Clock className="h-4 w-4 text-gray-500" />
                    <span>{formatDistanceToNow(new Date(booking.createdAt), { addSuffix: true })}</span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center justify-between pt-2 border-t">
                  <div className="text-xs text-gray-500">
                    ID: {booking.id}
                  </div>
                  <div className="flex space-x-2">
                    {booking.status === 'pending' && (
                      <Button
                        onClick={() => setSelectedBooking(
                          selectedBooking === booking.id ? null : booking.id
                        )}
                        variant="outline"
                        size="sm"
                        className="flex items-center space-x-2"
                      >
                        <Eye className="h-4 w-4" />
                        <span>
                          {selectedBooking === booking.id ? 'Hide Status' : 'Track Status'}
                        </span>
                      </Button>
                    )}
                    {canCancelBooking(booking) && (
                      <Button
                        onClick={() => setShowCancelDialog(booking.id)}
                        variant="outline"
                        size="sm"
                        className="flex items-center space-x-2 text-red-600 hover:text-red-700 hover:bg-red-50"
                      >
                        <Ban className="h-4 w-4" />
                        <span>Cancel</span>
                      </Button>
                    )}
                  </div>
                </div>

                {/* Status Tracker */}
                {selectedBooking === booking.id && booking.status === 'pending' && (
                  <div className="mt-4 pt-4 border-t">
                    <BookingStatusTracker
                      bookingId={booking.id}
                      onComplete={(success) => {
                        // Refresh bookings when status changes
                        fetchBookings();
                        setSelectedBooking(null);
                      }}
                      className="max-w-none"
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Cancellation Dialog */}
      {showCancelDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex items-center space-x-3 mb-4">
              <AlertTriangle className="h-6 w-6 text-red-600" />
              <h3 className="text-lg font-semibold">Cancel Booking</h3>
            </div>
            
            <p className="text-gray-600 mb-4">
              Are you sure you want to cancel this booking? This action cannot be undone.
            </p>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Reason for cancellation *
              </label>
              <textarea
                value={cancellationReason}
                onChange={(e) => setCancellationReason(e.target.value)}
                placeholder="Please provide a reason for cancellation..."
                className="w-full p-3 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                rows={3}
              />
            </div>
            
            <div className="flex space-x-3">
              <Button
                onClick={() => {
                  setShowCancelDialog(null);
                  setCancellationReason('');
                }}
                variant="outline"
                className="flex-1"
                disabled={cancellingBooking === showCancelDialog}
              >
                Keep Booking
              </Button>
              <Button
                onClick={() => handleCancelBooking(showCancelDialog)}
                className="flex-1 bg-red-600 hover:bg-red-700"
                disabled={cancellingBooking === showCancelDialog || !cancellationReason.trim()}
              >
                {cancellingBooking === showCancelDialog ? (
                  <div className="flex items-center space-x-2">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                    <span>Cancelling...</span>
                  </div>
                ) : (
                  'Cancel Booking'
                )}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Cancellation Success Message */}
      {cancellationResult && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex items-center space-x-3 mb-4">
              <CheckCircle className="h-6 w-6 text-green-600" />
              <h3 className="text-lg font-semibold">Booking Cancelled</h3>
            </div>
            
            <div className="space-y-3 mb-6">
              <p className="text-gray-600">
                Your booking has been successfully cancelled.
              </p>
              
              <div className="bg-green-50 p-4 rounded-lg">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-green-800">Refund Amount:</span>
                  <span className="text-lg font-bold text-green-800">
                    {formatPrice(cancellationResult.refundAmount, 'USD')}
                  </span>
                </div>
                <p className="text-xs text-green-600 mt-1">
                  Refund will be processed within 3-5 business days
                </p>
              </div>
              
              <div className="text-sm text-gray-500">
                <p><strong>Reference:</strong> {cancellationResult.bookingReference}</p>
                <p><strong>Status:</strong> {cancellationResult.refundStatus}</p>
              </div>
            </div>
            
            <Button
              onClick={() => setCancellationResult(null)}
              className="w-full"
            >
              Close
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}