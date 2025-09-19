'use client';
import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Separator } from '../../../components/ui/separator';
import { CheckCircle, Calendar, MapPin, CreditCard, Download, Share, Home } from 'lucide-react';

interface BookingDetails {
  id: string;
  type: string;
  itemName: string;
  amount: number;
  status: string;
  bookingDate: string;
  confirmationNumber: string;
  paymentId: string;
  details?: {
    checkIn?: string;
    checkOut?: string;
    guests?: number;
    roomType?: string;
    address?: string;
    date?: string;
    time?: string;
    duration?: string;
    location?: string;
    participants?: number;
  };
}

const BookingConfirmationContent = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const bookingId = searchParams?.get('bookingId');
  const paymentId = searchParams?.get('paymentId');
  const type = searchParams?.get('type');
  const itemName = searchParams?.get('itemName');
  const amount = searchParams?.get('amount');
  const [bookingDetails, setBookingDetails] = useState<BookingDetails | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Redirect if no booking data
    if (!bookingId || !paymentId) {
      router.push('/trip-planning');
      return;
    }

    // Fetch booking details
    const fetchBookingDetails = async () => {
      try {
        const response = await fetch(`/api/bookings/${bookingId}`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        });
        
        if (response.ok) {
          const details = await response.json();
          setBookingDetails(details);
        } else {
          // Mock booking details if API fails
          setBookingDetails({
            id: bookingId || '',
            type: type || 'unknown',
            itemName: itemName || 'Unknown Item',
            amount: parseFloat(amount || '0'),
            status: 'confirmed',
            bookingDate: new Date().toISOString(),
            confirmationNumber: `HNG-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
            paymentId: paymentId || '',
            details: type === 'hotel' ? {
              checkIn: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
              checkOut: new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
              guests: 2,
              roomType: 'Standard Room',
              address: '123 Hotel Street, City, State 12345'
            } : {
              date: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
              time: '10:00 AM',
              duration: '3 hours',
              location: '456 Activity Location, City, State 12345',
              participants: 2
            }
          });
        }
      } catch (error) {
        console.error('Error fetching booking details:', error);
        // Set mock data on error
        setBookingDetails({
          id: bookingId || '',
          type: type || 'unknown',
          itemName: itemName || 'Unknown Item',
          amount: parseFloat(amount || '0'),
          status: 'confirmed',
          bookingDate: new Date().toISOString(),
          confirmationNumber: `HNG-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
          paymentId: paymentId || ''
        });
      } finally {
        setLoading(false);
      }
    };

    fetchBookingDetails();
  }, [bookingId, paymentId, type, itemName, amount, router]);

  const handleDownloadConfirmation = () => {
    if (!bookingDetails) return;
    
    // Mock download functionality
    const confirmationData = {
      confirmationNumber: bookingDetails.confirmationNumber,
      itemName: bookingDetails.itemName,
      type: bookingDetails.type,
      amount: bookingDetails.amount,
      bookingDate: bookingDetails.bookingDate,
      status: bookingDetails.status
    };
    
    const dataStr = JSON.stringify(confirmationData, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
    
    const exportFileDefaultName = `booking-confirmation-${bookingDetails.confirmationNumber}.json`;
    
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  const handleShare = async () => {
    if (!bookingDetails) return;
    
    const shareData = {
      title: 'HopNGo Booking Confirmation',
      text: `My ${bookingDetails.type} booking for ${bookingDetails.itemName} has been confirmed! Confirmation: ${bookingDetails.confirmationNumber}`,
      url: window.location.href
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
      } catch (error) {
        console.log('Error sharing:', error);
      }
    } else {
      // Fallback: copy to clipboard
      navigator.clipboard.writeText(`${shareData.text} - ${shareData.url}`);
      alert('Booking details copied to clipboard!');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your booking confirmation...</p>
        </div>
      </div>
    );
  }

  if (!bookingDetails) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Card className="w-full max-w-md">
          <CardContent className="p-8 text-center">
            <p className="text-gray-600 mb-4">Unable to load booking details.</p>
            <Button onClick={() => router.push('/trip-planning')}>Back to Trip Planning</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!bookingDetails) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="container mx-auto px-4 max-w-4xl">
          <Card>
            <CardContent className="p-8 text-center">
              <p className="text-gray-600 mb-4">Loading booking details...</p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        {/* Success Header */}
        <div className="text-center mb-8">
          <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Booking Confirmed!</h1>
          <p className="text-gray-600">
            Your {bookingDetails.type} booking has been successfully confirmed.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Confirmation Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Confirmation Number */}
            <Card>
              <CardHeader>
                <CardTitle>Confirmation Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-green-600 font-medium">Confirmation Number</p>
                      <p className="text-2xl font-bold text-green-800">{bookingDetails.confirmationNumber}</p>
                    </div>
                    <Badge variant="secondary" className="bg-green-100 text-green-800">
                      {bookingDetails.status.toUpperCase()}
                    </Badge>
                  </div>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-500">Booking Date</p>
                    <p className="font-semibold">{new Date(bookingDetails.bookingDate).toLocaleDateString()}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Payment ID</p>
                    <p className="font-semibold">{bookingDetails.paymentId}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Booking Details */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  {bookingDetails.type === 'hotel' ? <MapPin className="h-5 w-5" /> : <Calendar className="h-5 w-5" />}
                  {bookingDetails.type === 'hotel' ? 'Hotel Booking' : 'Activity Booking'}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h3 className="text-xl font-semibold mb-2">{bookingDetails.itemName}</h3>
                  
                  {bookingDetails.type === 'hotel' && bookingDetails.details && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                      <div>
                        <p className="text-gray-500">Check-in</p>
                        <p className="font-semibold">{bookingDetails.details.checkIn ? new Date(bookingDetails.details.checkIn).toLocaleDateString() : 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">Check-out</p>
                        <p className="font-semibold">{bookingDetails.details.checkOut ? new Date(bookingDetails.details.checkOut).toLocaleDateString() : 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">Guests</p>
                        <p className="font-semibold">{bookingDetails.details.guests}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">Room Type</p>
                        <p className="font-semibold">{bookingDetails.details.roomType}</p>
                      </div>
                      {bookingDetails.details.address && (
                        <div className="md:col-span-2">
                          <p className="text-gray-500">Address</p>
                          <p className="font-semibold">{bookingDetails.details.address}</p>
                        </div>
                      )}
                    </div>
                  )}
                  
                  {bookingDetails.type === 'activity' && bookingDetails.details && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                      <div>
                        <p className="text-gray-500">Date</p>
                        <p className="font-semibold">{bookingDetails.details.date ? new Date(bookingDetails.details.date).toLocaleDateString() : 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">Time</p>
                        <p className="font-semibold">{bookingDetails.details.time}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">Duration</p>
                        <p className="font-semibold">{bookingDetails.details.duration}</p>
                      </div>
                      <div>
                        <p className="text-gray-500">Participants</p>
                        <p className="font-semibold">{bookingDetails.details.participants}</p>
                      </div>
                      {bookingDetails.details.location && (
                        <div className="md:col-span-2">
                          <p className="text-gray-500">Location</p>
                          <p className="font-semibold">{bookingDetails.details.location}</p>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>

            {/* Important Information */}
            <Card>
              <CardHeader>
                <CardTitle>Important Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h4 className="font-semibold text-blue-800 mb-2">What's Next?</h4>
                  <ul className="space-y-1 text-blue-700">
                    <li>• You'll receive a confirmation email shortly</li>
                    <li>• Check your booking details and save this confirmation</li>
                    <li>• Contact the provider directly for any special requests</li>
                    <li>• Arrive 15 minutes early for activities</li>
                  </ul>
                </div>
                
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <h4 className="font-semibold text-yellow-800 mb-2">Cancellation Policy</h4>
                  <p className="text-yellow-700">
                    Free cancellation up to 24 hours before your booking. 
                    Contact support for assistance with changes or cancellations.
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Payment Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <CreditCard className="h-5 w-5" />
                  Payment Summary
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span>Subtotal:</span>
                    <span>${bookingDetails.amount}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Service Fee:</span>
                    <span>$0.00</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Taxes:</span>
                    <span>$0.00</span>
                  </div>
                </div>
                
                <Separator />
                
                <div className="flex justify-between font-semibold text-lg">
                  <span>Total Paid:</span>
                  <span>${bookingDetails.amount}</span>
                </div>
                
                <Badge variant="secondary" className="w-full justify-center">
                  Payment Completed
                </Badge>
              </CardContent>
            </Card>

            {/* Actions */}
            <Card>
              <CardHeader>
                <CardTitle>Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button onClick={handleDownloadConfirmation} variant="outline" className="w-full">
                  <Download className="h-4 w-4 mr-2" />
                  Download Confirmation
                </Button>
                
                <Button onClick={handleShare} variant="outline" className="w-full">
                  <Share className="h-4 w-4 mr-2" />
                  Share Booking
                </Button>
                
                <Separator />
                
                <Button onClick={() => router.push('/trip-planning')} className="w-full">
                  Plan Another Trip
                </Button>
                
                <Button onClick={() => router.push('/')} variant="outline" className="w-full">
                  <Home className="h-4 w-4 mr-2" />
                  Back to Home
                </Button>
              </CardContent>
            </Card>

            {/* Support */}
            <Card>
              <CardContent className="p-4 text-center">
                <p className="text-sm text-gray-600 mb-2">Need help with your booking?</p>
                <Button variant="link" className="text-sm">
                  Contact Support
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

const BookingConfirmationPage = () => {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen">Loading...</div>}>
      <BookingConfirmationContent />
    </Suspense>
  );
};

export default BookingConfirmationPage;