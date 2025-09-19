'use client';
import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import TripPlanning from '../../../components/TripPlanning';
import ItineraryDisplay from '../../../components/ItineraryDisplay';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { ArrowLeft, AlertCircle, CheckCircle } from 'lucide-react';

interface Activity {
  id: string;
  name: string;
  type: string;
  price?: number;
  [key: string]: any;
}

interface Itinerary {
  id?: string;
  activities: Activity[];
  [key: string]: any;
}

interface Notification {
  type: 'success' | 'error';
  message: string;
}

interface Hotel {
  id: string;
  name: string;
  pricePerNight: number;
  [key: string]: any;
}

const TripPlanningPage = () => {
  const router = useRouter();
  const [currentStep, setCurrentStep] = useState('planning'); // 'planning', 'itinerary', 'booking'
  const [generatedItinerary, setGeneratedItinerary] = useState<Itinerary | null>(null);
  const [bookingStatus, setBookingStatus] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [notification, setNotification] = useState<Notification | null>(null);

  const handlePlanGenerated = (itinerary: Itinerary) => {
    setGeneratedItinerary(itinerary);
    setCurrentStep('itinerary');
    setError(null);
  };

  const handleBookActivity = async (activity: Activity) => {
    try {
      setError(null);
      
      // Simulate API call to book activity
      const response = await fetch('/api/bookings/activity', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          activityId: activity.id,
          tripId: generatedItinerary?.id,
          travelers: generatedItinerary?.travelers || 1
        })
      });

      if (!response.ok) {
        throw new Error('Failed to book activity');
      }

      const booking = await response.json();
      
      setBookingStatus(prev => ({
        ...prev,
        [`activity-${activity.id}`]: 'booked'
      }));
      
      setNotification({
        type: 'success',
        message: `Successfully booked ${activity.title}!`
      });
      
      // Clear notification after 5 seconds
      setTimeout(() => setNotification(null), 5000);
      
    } catch (error) {
      console.error('Booking error:', error);
      setError(`Failed to book ${activity.title}. Please try again.`);
      
      // Handle specific error cases
      if (error instanceof Error && (error.message.includes('timeout') || error.name === 'TimeoutError')) {
        setError('The booking service is currently unavailable. Please try again later.');
      }
    }
  };

  const handleBookHotel = async (hotel: Hotel) => {
    try {
      setError(null);
      
      // Simulate API call to book hotel
      const response = await fetch('/api/bookings/hotel', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          hotelId: hotel.id,
          tripId: generatedItinerary?.id,
          checkIn: generatedItinerary?.startDate,
          checkOut: generatedItinerary?.endDate,
          guests: generatedItinerary?.travelers || 1
        })
      });

      if (!response.ok) {
        throw new Error('Failed to book hotel');
      }

      const booking = await response.json();
      
      setBookingStatus(prev => ({
        ...prev,
        [`hotel-${hotel.id}`]: 'booked'
      }));
      
      // Redirect to payment flow
      const queryParams = new URLSearchParams({
        type: 'hotel',
        bookingId: booking.id,
        amount: (hotel.pricePerNight * (generatedItinerary?.duration || 1)).toString(),
        itemName: hotel.name
      });
      router.push(`/checkout?${queryParams.toString()}`);
      
    } catch (error) {
      console.error('Hotel booking error:', error);
      setError(`Failed to book ${hotel.name}. Please try again.`);
      
      // Handle specific error cases
      if (error instanceof Error && (error.message.includes('timeout') || error.name === 'TimeoutError')) {
        setError('The hotel booking service is currently unavailable. Please try again later.');
      }
    }
  };

  const handleBackToPlanning = () => {
    setCurrentStep('planning');
    setGeneratedItinerary(null);
    setError(null);
    setNotification(null);
  };

  const renderStepIndicator = () => {
    const steps = [
      { id: 'planning', label: 'Plan Trip', active: currentStep === 'planning' },
      { id: 'itinerary', label: 'View Itinerary', active: currentStep === 'itinerary' },
      { id: 'booking', label: 'Book & Pay', active: currentStep === 'booking' }
    ];

    return (
      <div className="flex items-center justify-center mb-8">
        {steps.map((step, index) => (
          <React.Fragment key={step.id}>
            <div className={`flex items-center gap-2 px-4 py-2 rounded-full ${
              step.active ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-500'
            }`}>
              <div className={`w-6 h-6 rounded-full flex items-center justify-center text-sm font-semibold ${
                step.active ? 'bg-blue-600 text-white' : 'bg-gray-300 text-gray-600'
              }`}>
                {index + 1}
              </div>
              <span className="font-medium">{step.label}</span>
            </div>
            {index < steps.length - 1 && (
              <div className="w-8 h-px bg-gray-300 mx-2" />
            )}
          </React.Fragment>
        ))}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        {/* Header */}
        <div className="mb-8">
          <Button
            variant="ghost"
            onClick={() => router.back()}
            className="mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <h1 className="text-3xl font-bold text-gray-900">Trip Planning</h1>
          <p className="text-gray-600 mt-2">
            Plan your perfect trip with AI-powered recommendations
          </p>
        </div>

        {/* Step Indicator */}
        {renderStepIndicator()}

        {/* Error Alert */}
        {error && (
          <Alert className="mb-6 border-red-200 bg-red-50">
            <AlertCircle className="h-4 w-4 text-red-600" />
            <AlertDescription className="text-red-800">
              {error}
            </AlertDescription>
          </Alert>
        )}

        {/* Success Notification */}
        {notification && notification.type === 'success' && (
          <Alert className="mb-6 border-green-200 bg-green-50">
            <CheckCircle className="h-4 w-4 text-green-600" />
            <AlertDescription className="text-green-800">
              {notification.message}
            </AlertDescription>
          </Alert>
        )}

        {/* Main Content */}
        <div className="space-y-6">
          {currentStep === 'planning' && (
            <TripPlanning onPlanGenerated={handlePlanGenerated} />
          )}

          {currentStep === 'itinerary' && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h2 className="text-2xl font-semibold">Your AI-Generated Itinerary</h2>
                <Button variant="outline" onClick={handleBackToPlanning}>
                  Modify Plan
                </Button>
              </div>
              <ItineraryDisplay
                itinerary={generatedItinerary}
                onBookActivity={handleBookActivity}
                onBookHotel={handleBookHotel}
              />
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-12 text-center text-gray-500">
          <p>Need help? Contact our travel experts at support@hopngo.com</p>
        </div>
      </div>
    </div>
  );
};

export default TripPlanningPage;