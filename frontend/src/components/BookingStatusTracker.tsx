'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { useBookingStatusPolling } from '@/hooks/useBookingStatusPolling';
import {
  CheckCircle,
  XCircle,
  Clock,
  Loader2,
  RefreshCw,
  AlertCircle
} from 'lucide-react';

export interface BookingStatusTrackerProps {
  bookingId: string;
  onComplete?: (success: boolean) => void;
  onRetry?: () => void;
  className?: string;
}

const getStatusIcon = (status: string, isPolling: boolean) => {
  if (isPolling) {
    return <Loader2 className="h-5 w-5 animate-spin" />;
  }

  switch (status) {
    case 'confirmed':
      return <CheckCircle className="h-5 w-5 text-green-600" />;
    case 'cancelled':
      return <XCircle className="h-5 w-5 text-red-600" />;
    case 'pending':
      return <Clock className="h-5 w-5 text-yellow-600" />;
    default:
      return <AlertCircle className="h-5 w-5 text-gray-600" />;
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
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusMessage = (status: string, paymentStatus?: string) => {
  switch (status) {
    case 'pending':
      if (paymentStatus === 'processing') {
        return 'Processing payment...';
      }
      return 'Waiting for payment confirmation...';
    case 'confirmed':
      return 'Booking confirmed! Payment successful.';
    case 'cancelled':
      return 'Booking cancelled. Payment failed or was declined.';
    default:
      return 'Checking booking status...';
  }
};

export default function BookingStatusTracker({
  bookingId,
  onComplete,
  onRetry,
  className = ''
}: BookingStatusTrackerProps) {
  const [showDetails, setShowDetails] = useState(false);

  const {
    status,
    isPolling,
    error,
    attempts,
    isComplete,
    isSuccess,
    isFailure,
    startPolling,
    stopPolling
  } = useBookingStatusPolling({
    bookingId,
    enabled: true,
    interval: 2000,
    maxAttempts: 150,
    onSuccess: (status) => {
      onComplete?.(true);
    },
    onFailure: (status) => {
      onComplete?.(false);
    },
    onTimeout: () => {
      // Handle timeout - maybe show retry option
    }
  });

  const progressPercentage = Math.min((attempts / 150) * 100, 100);

  const handleRetry = () => {
    onRetry?.();
    startPolling();
  };

  return (
    <Card className={`w-full max-w-md ${className}`}>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center justify-between text-lg">
          <span>Booking Status</span>
          {status && (
            <Badge className={getStatusColor(status.status)}>
              {status.status.charAt(0).toUpperCase() + status.status.slice(1)}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      
      <CardContent className="space-y-4">
        {/* Status Icon and Message */}
        <div className="flex items-center space-x-3">
          {getStatusIcon(status?.status || 'pending', isPolling)}
          <div className="flex-1">
            <p className="text-sm font-medium">
              {getStatusMessage(status?.status || 'pending', status?.paymentStatus)}
            </p>
            {status?.updatedAt && (
              <p className="text-xs text-gray-500 mt-1">
                Last updated: {new Date(status.updatedAt).toLocaleTimeString()}
              </p>
            )}
          </div>
        </div>

        {/* Progress Bar (only show when polling) */}
        {isPolling && (
          <div className="space-y-2">
            <div className="flex justify-between text-xs text-gray-600">
              <span>Checking status...</span>
              <span>{attempts}/150 attempts</span>
            </div>
            <Progress value={progressPercentage} className="h-2" />
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <div className="flex items-center space-x-2">
              <AlertCircle className="h-4 w-4 text-red-600" />
              <p className="text-sm text-red-800">Error: {error}</p>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex space-x-2">
          {isComplete && isFailure && onRetry && (
            <Button
              onClick={handleRetry}
              variant="outline"
              size="sm"
              className="flex items-center space-x-2"
            >
              <RefreshCw className="h-4 w-4" />
              <span>Retry Payment</span>
            </Button>
          )}
          
          {isPolling && (
            <Button
              onClick={stopPolling}
              variant="outline"
              size="sm"
            >
              Stop Checking
            </Button>
          )}
          
          {!isPolling && !isComplete && (
            <Button
              onClick={startPolling}
              variant="outline"
              size="sm"
              className="flex items-center space-x-2"
            >
              <RefreshCw className="h-4 w-4" />
              <span>Check Status</span>
            </Button>
          )}
        </div>

        {/* Details Toggle */}
        <Button
          onClick={() => setShowDetails(!showDetails)}
          variant="ghost"
          size="sm"
          className="w-full text-xs"
        >
          {showDetails ? 'Hide Details' : 'Show Details'}
        </Button>

        {/* Debug Details */}
        {showDetails && (
          <div className="bg-gray-50 rounded-lg p-3 text-xs space-y-2">
            <div><strong>Booking ID:</strong> {bookingId}</div>
            <div><strong>Attempts:</strong> {attempts}</div>
            <div><strong>Is Polling:</strong> {isPolling ? 'Yes' : 'No'}</div>
            {status && (
              <div className="space-y-1">
                <div><strong>Status:</strong> {status.status}</div>
                {status.paymentStatus && (
                  <div><strong>Payment Status:</strong> {status.paymentStatus}</div>
                )}
                <div><strong>Updated:</strong> {status.updatedAt}</div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}