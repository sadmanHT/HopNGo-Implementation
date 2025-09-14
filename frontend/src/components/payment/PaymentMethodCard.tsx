'use client';

import React from 'react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { CreditCard, Trash2, Star } from 'lucide-react';
import { PaymentMethod } from '../../services/payment';

interface PaymentMethodCardProps {
  paymentMethod: PaymentMethod;
  onRemove: (id: string) => void;
  onSetDefault: (id: string) => void;
  isRemoving?: boolean;
}

export function PaymentMethodCard({ 
  paymentMethod, 
  onRemove, 
  onSetDefault, 
  isRemoving = false 
}: PaymentMethodCardProps) {
  const getCardIcon = () => {
    switch (paymentMethod.brand?.toLowerCase()) {
      case 'visa':
        return '💳';
      case 'mastercard':
        return '💳';
      case 'amex':
        return '💳';
      default:
        return <CreditCard className="h-6 w-6" />;
    }
  };

  const getPaymentTypeLabel = () => {
    switch (paymentMethod.type) {
      case 'card':
        return `${paymentMethod.brand?.toUpperCase()} •••• ${paymentMethod.last4}`;
      case 'paypal':
        return 'PayPal Account';
      case 'bank_transfer':
        return 'Bank Transfer';
      default:
        return 'Payment Method';
    }
  };

  const getExpiryText = () => {
    if (paymentMethod.type === 'card' && paymentMethod.expiryMonth && paymentMethod.expiryYear) {
      return `Expires ${paymentMethod.expiryMonth.toString().padStart(2, '0')}/${paymentMethod.expiryYear.toString().slice(-2)}`;
    }
    return null;
  };

  return (
    <Card className={`relative ${paymentMethod.isDefault ? 'ring-2 ring-primary' : ''}`}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="text-2xl">
              {getCardIcon()}
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-medium">{getPaymentTypeLabel()}</span>
                {paymentMethod.isDefault && (
                  <Badge variant="secondary" className="text-xs">
                    <Star className="h-3 w-3 mr-1" />
                    Default
                  </Badge>
                )}
              </div>
              {getExpiryText() && (
                <p className="text-sm text-muted-foreground">{getExpiryText()}</p>
              )}
              <p className="text-xs text-muted-foreground">
                Added {new Date(paymentMethod.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            {!paymentMethod.isDefault && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onSetDefault(paymentMethod.id)}
              >
                Set Default
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={() => onRemove(paymentMethod.id)}
              disabled={isRemoving}
              className="text-destructive hover:text-destructive"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}