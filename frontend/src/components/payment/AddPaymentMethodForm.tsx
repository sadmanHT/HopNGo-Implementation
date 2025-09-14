'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Checkbox } from '../ui/checkbox';
import { CreditCard, Plus } from 'lucide-react';
import { PaymentMethod } from '../../services/payment';

interface AddPaymentMethodFormProps {
  onAdd: (paymentMethod: Omit<PaymentMethod, 'id' | 'createdAt'>) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

export function AddPaymentMethodForm({ onAdd, onCancel, isLoading = false }: AddPaymentMethodFormProps) {
  const [formData, setFormData] = useState({
    type: 'card' as const,
    cardNumber: '',
    expiryMonth: '',
    expiryYear: '',
    cvv: '',
    cardholderName: '',
    isDefault: false,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (formData.type === 'card') {
      if (!formData.cardNumber || formData.cardNumber.replace(/\s/g, '').length < 13) {
        newErrors.cardNumber = 'Please enter a valid card number';
      }
      if (!formData.expiryMonth || !formData.expiryYear) {
        newErrors.expiry = 'Please enter expiry date';
      }
      if (!formData.cvv || formData.cvv.length < 3) {
        newErrors.cvv = 'Please enter a valid CVV';
      }
      if (!formData.cardholderName.trim()) {
        newErrors.cardholderName = 'Please enter cardholder name';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const formatCardNumber = (value: string) => {
    const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    const matches = v.match(/\d{4,16}/g);
    const match = matches && matches[0] || '';
    const parts = [];
    for (let i = 0, len = match.length; i < len; i += 4) {
      parts.push(match.substring(i, i + 4));
    }
    if (parts.length) {
      return parts.join(' ');
    } else {
      return v;
    }
  };

  const getCardBrand = (cardNumber: string) => {
    const number = cardNumber.replace(/\s/g, '');
    if (/^4/.test(number)) return 'visa';
    if (/^5[1-5]/.test(number)) return 'mastercard';
    if (/^3[47]/.test(number)) return 'amex';
    return 'unknown';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) return;

    const paymentMethod: Omit<PaymentMethod, 'id' | 'createdAt'> = {
      type: formData.type,
      last4: formData.cardNumber.replace(/\s/g, '').slice(-4),
      brand: getCardBrand(formData.cardNumber),
      expiryMonth: parseInt(formData.expiryMonth),
      expiryYear: parseInt(formData.expiryYear),
      isDefault: formData.isDefault,
    };

    await onAdd(paymentMethod);
  };

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 20 }, (_, i) => currentYear + i);
  const months = Array.from({ length: 12 }, (_, i) => i + 1);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Plus className="h-5 w-5" />
          <span>Add Payment Method</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="cardNumber">Card Number</Label>
            <div className="relative">
              <Input
                id="cardNumber"
                type="text"
                placeholder="1234 5678 9012 3456"
                value={formData.cardNumber}
                onChange={(e) => {
                  const formatted = formatCardNumber(e.target.value);
                  if (formatted.replace(/\s/g, '').length <= 16) {
                    setFormData(prev => ({ ...prev, cardNumber: formatted }));
                  }
                }}
                className={errors.cardNumber ? 'border-destructive' : ''}
                maxLength={19}
              />
              <CreditCard className="absolute right-3 top-3 h-4 w-4 text-muted-foreground" />
            </div>
            {errors.cardNumber && (
              <p className="text-sm text-destructive">{errors.cardNumber}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Expiry Month</Label>
              <Select
                value={formData.expiryMonth}
                onValueChange={(value) => setFormData(prev => ({ ...prev, expiryMonth: value }))}
              >
                <SelectTrigger className={errors.expiry ? 'border-destructive' : ''}>
                  <SelectValue placeholder="Month" />
                </SelectTrigger>
                <SelectContent>
                  {months.map(month => (
                    <SelectItem key={month} value={month.toString()}>
                      {month.toString().padStart(2, '0')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Expiry Year</Label>
              <Select
                value={formData.expiryYear}
                onValueChange={(value) => setFormData(prev => ({ ...prev, expiryYear: value }))}
              >
                <SelectTrigger className={errors.expiry ? 'border-destructive' : ''}>
                  <SelectValue placeholder="Year" />
                </SelectTrigger>
                <SelectContent>
                  {years.map(year => (
                    <SelectItem key={year} value={year.toString()}>
                      {year}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          {errors.expiry && (
            <p className="text-sm text-destructive">{errors.expiry}</p>
          )}

          <div className="space-y-2">
            <Label htmlFor="cvv">CVV</Label>
            <Input
              id="cvv"
              type="text"
              placeholder="123"
              value={formData.cvv}
              onChange={(e) => {
                const value = e.target.value.replace(/\D/g, '');
                if (value.length <= 4) {
                  setFormData(prev => ({ ...prev, cvv: value }));
                }
              }}
              className={errors.cvv ? 'border-destructive' : ''}
              maxLength={4}
            />
            {errors.cvv && (
              <p className="text-sm text-destructive">{errors.cvv}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="cardholderName">Cardholder Name</Label>
            <Input
              id="cardholderName"
              type="text"
              placeholder="John Doe"
              value={formData.cardholderName}
              onChange={(e) => setFormData(prev => ({ ...prev, cardholderName: e.target.value }))}
              className={errors.cardholderName ? 'border-destructive' : ''}
            />
            {errors.cardholderName && (
              <p className="text-sm text-destructive">{errors.cardholderName}</p>
            )}
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="isDefault"
              checked={formData.isDefault}
              onCheckedChange={(checked) => 
                setFormData(prev => ({ ...prev, isDefault: checked as boolean }))
              }
            />
            <Label htmlFor="isDefault" className="text-sm">
              Set as default payment method
            </Label>
          </div>

          <div className="flex space-x-2 pt-4">
            <Button type="submit" disabled={isLoading} className="flex-1">
              {isLoading ? 'Adding...' : 'Add Payment Method'}
            </Button>
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancel
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}