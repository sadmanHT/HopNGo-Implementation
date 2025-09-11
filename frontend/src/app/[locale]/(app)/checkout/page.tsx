'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import BookingStatusTracker from '@/components/BookingStatusTracker';
import { bookingsApi, CreateBookingRequest } from '@/lib/api/bookings';
import { useAuthStore } from '@/lib/state';
import analytics from '@/lib/analytics';
import { CalendarDays, DollarSign, MapPin, ArrowLeft } from 'lucide-react';

interface CheckoutState {
  step: 'form' | 'processing' | 'status';
  bookingId?: string;
  error?: string;
}

export default function CheckoutPage() {
  const router = useRouter();
  const { user, token } = useAuthStore();
  
  const [state, setState] = useState<CheckoutState>({ step: 'form' });
  const [formData, setFormData] = useState<CreateBookingRequest>({
    serviceType: 'transport',
    title: '',
    description: '',
    price: 0,
    currency: 'USD',
    startDate: '',
    endDate: ''
  });

  const handleInputChange = (field: keyof CreateBookingRequest, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!user || !token) {
      router.push('/login');
      return;
    }

    // Track booking creation attempt
    analytics.trackAction('booking_creation_attempt', 'checkout_form', {
      serviceType: formData.serviceType,
      price: formData.price,
      currency: formData.currency,
      userId: user.id
    });

    setState({ step: 'processing' });

    try {
      const response = await bookingsApi.createBooking(formData);
      
      if (response.success && response.data) {
        // Track successful booking creation
        analytics.trackConversion('booking_created', formData.price, {
          bookingId: response.data.id,
          serviceType: formData.serviceType,
          price: formData.price,
          currency: formData.currency,
          userId: user.id,
          title: formData.title
        });
        
        setState({ 
          step: 'status', 
          bookingId: response.data.id 
        });
      } else {
        // Track booking creation failure
        analytics.trackError('booking_creation_failed', {
          serviceType: formData.serviceType,
          price: formData.price,
          errorMessage: response.message || 'Failed to create booking',
          userId: user.id
        });
        
        setState({ 
          step: 'form', 
          error: response.message || 'Failed to create booking' 
        });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      
      // Track booking creation error
      analytics.trackError('booking_creation_error', {
        serviceType: formData.serviceType,
        price: formData.price,
        errorMessage,
        userId: user.id
      });
      
      setState({ 
        step: 'form', 
        error: errorMessage
      });
    }
  };

  const handleBookingComplete = (success: boolean) => {
    if (success) {
      // Track booking completion
      analytics.trackConversion('booking_completed', formData.price, {
        bookingId: state.bookingId,
        serviceType: formData.serviceType,
        userId: user?.id
      });
      
      // Redirect to bookings page or show success message
      setTimeout(() => {
        router.push('/bookings');
      }, 3000);
    } else {
      // Track booking failure
      analytics.trackError('booking_completion_failed', {
        bookingId: state.bookingId,
        serviceType: formData.serviceType,
        userId: user?.id
      });
    }
  };

  const handleRetry = () => {
    setState({ step: 'form' });
  };

  const goBack = () => {
    if (state.step === 'status') {
      setState({ step: 'form' });
    } else {
      router.back();
    }
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      {/* Header */}
      <div className="flex items-center space-x-4 mb-6">
        <Button
          onClick={goBack}
          variant="ghost"
          size="sm"
          className="flex items-center space-x-2"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back</span>
        </Button>
        <h1 className="text-2xl font-bold">Checkout</h1>
      </div>

      {/* Booking Form */}
      {state.step === 'form' && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Booking</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Service Type */}
              <div className="space-y-2">
                <Label htmlFor="serviceType">Service Type</Label>
                <select
                  id="serviceType"
                  value={formData.serviceType}
                  onChange={(e) => handleInputChange('serviceType', e.target.value as any)}
                  className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                >
                  <option value="transport">Transport</option>
                  <option value="accommodation">Accommodation</option>
                  <option value="activity">Activity</option>
                </select>
              </div>

              {/* Title */}
              <div className="space-y-2">
                <Label htmlFor="title">Title</Label>
                <Input
                  id="title"
                  value={formData.title}
                  onChange={(e) => handleInputChange('title', e.target.value)}
                  placeholder="e.g., Flight to Paris, Hotel Stay, City Tour"
                  required
                />
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  value={formData.description}
                  onChange={(e) => handleInputChange('description', e.target.value)}
                  placeholder="Brief description of the service"
                  required
                />
              </div>

              {/* Price */}
              <div className="space-y-2">
                <Label htmlFor="price">Price</Label>
                <div className="flex items-center space-x-2">
                  <DollarSign className="h-4 w-4 text-gray-500" />
                  <Input
                    id="price"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.price}
                    onChange={(e) => handleInputChange('price', parseFloat(e.target.value) || 0)}
                    placeholder="0.00"
                    required
                  />
                  <select
                    value={formData.currency}
                    onChange={(e) => handleInputChange('currency', e.target.value)}
                    className="p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                  </select>
                </div>
              </div>

              {/* Start Date */}
              <div className="space-y-2">
                <Label htmlFor="startDate">Start Date</Label>
                <div className="flex items-center space-x-2">
                  <CalendarDays className="h-4 w-4 text-gray-500" />
                  <Input
                    id="startDate"
                    type="datetime-local"
                    value={formData.startDate}
                    onChange={(e) => handleInputChange('startDate', e.target.value)}
                    required
                  />
                </div>
              </div>

              {/* End Date */}
              <div className="space-y-2">
                <Label htmlFor="endDate">End Date</Label>
                <div className="flex items-center space-x-2">
                  <CalendarDays className="h-4 w-4 text-gray-500" />
                  <Input
                    id="endDate"
                    type="datetime-local"
                    value={formData.endDate}
                    onChange={(e) => handleInputChange('endDate', e.target.value)}
                    required
                  />
                </div>
              </div>

              {/* Error Message */}
              {state.error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                  <p className="text-sm text-red-800">{state.error}</p>
                </div>
              )}

              {/* Submit Button */}
              <Button
                type="submit"
                className="w-full"
                disabled={state.step === ('processing' as CheckoutState['step'])}
              >
                {state.step === ('processing' as CheckoutState['step']) ? 'Creating Booking...' : 'Create Booking & Pay'}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Processing State */}
      {state.step === 'processing' && (
        <Card>
          <CardContent className="flex items-center justify-center py-8">
            <div className="text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-lg font-medium">Creating your booking...</p>
              <p className="text-sm text-gray-600 mt-2">Please wait while we process your request</p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Status Tracking */}
      {state.step === 'status' && state.bookingId && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Booking Created Successfully!</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-gray-600 mb-4">
                Your booking has been created and is being processed. 
                We'll track the payment status in real-time below.
              </p>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                <p className="text-sm text-blue-800">
                  <strong>Booking ID:</strong> {state.bookingId}
                </p>
              </div>
            </CardContent>
          </Card>

          <BookingStatusTracker
            bookingId={state.bookingId}
            onComplete={handleBookingComplete}
            onRetry={handleRetry}
          />
        </div>
      )}
    </div>
  );
}