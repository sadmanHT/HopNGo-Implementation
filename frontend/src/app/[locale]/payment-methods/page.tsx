'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Plus, CreditCard, AlertCircle } from 'lucide-react';
import { PaymentMethodCard } from '../../../components/payment/PaymentMethodCard';
import { AddPaymentMethodForm } from '../../../components/payment/AddPaymentMethodForm';
import { paymentService, PaymentMethod } from '../../../services/payment';
import { useToast } from '../../../hooks/use-toast';

export default function PaymentMethodsPage() {
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const { toast } = useToast();

  useEffect(() => {
    loadPaymentMethods();
  }, []);

  const loadPaymentMethods = async () => {
    try {
      setIsLoading(true);
      const methods = await paymentService.getPaymentMethods();
      setPaymentMethods(methods);
    } catch (error) {
      console.error('Failed to load payment methods:', error);
      toast({
        title: 'Error',
        description: 'Failed to load payment methods. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddPaymentMethod = async (paymentMethodData: Omit<PaymentMethod, 'id' | 'createdAt'>) => {
    try {
      setIsAdding(true);
      const newMethod = await paymentService.addPaymentMethod(paymentMethodData);
      setPaymentMethods(prev => [...prev, newMethod]);
      setShowAddForm(false);
      toast({
        title: 'Success',
        description: 'Payment method added successfully.',
      });
    } catch (error) {
      console.error('Failed to add payment method:', error);
      toast({
        title: 'Error',
        description: 'Failed to add payment method. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsAdding(false);
    }
  };

  const handleRemovePaymentMethod = async (id: string) => {
    try {
      setRemovingId(id);
      await paymentService.removePaymentMethod(id);
      setPaymentMethods(prev => prev.filter(method => method.id !== id));
      toast({
        title: 'Success',
        description: 'Payment method removed successfully.',
      });
    } catch (error) {
      console.error('Failed to remove payment method:', error);
      toast({
        title: 'Error',
        description: 'Failed to remove payment method. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setRemovingId(null);
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      // Update locally first for better UX
      setPaymentMethods(prev => 
        prev.map(method => ({
          ...method,
          isDefault: method.id === id
        }))
      );
      
      // TODO: Call API to set default payment method
      toast({
        title: 'Success',
        description: 'Default payment method updated.',
      });
    } catch (error) {
      console.error('Failed to set default payment method:', error);
      toast({
        title: 'Error',
        description: 'Failed to update default payment method. Please try again.',
        variant: 'destructive',
      });
      // Revert the change
      loadPaymentMethods();
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="animate-pulse space-y-4">
            <div className="h-8 bg-gray-200 rounded w-1/4"></div>
            <div className="space-y-3">
              {[1, 2, 3].map(i => (
                <div key={i} className="h-24 bg-gray-200 rounded"></div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold">Payment Methods</h1>
            <p className="text-muted-foreground mt-2">
              Manage your payment methods for faster checkout
            </p>
          </div>
          {!showAddForm && (
            <Button onClick={() => setShowAddForm(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Add Payment Method
            </Button>
          )}
        </div>

        {showAddForm && (
          <div className="mb-8">
            <AddPaymentMethodForm
              onAdd={handleAddPaymentMethod}
              onCancel={() => setShowAddForm(false)}
              isLoading={isAdding}
            />
          </div>
        )}

        {paymentMethods.length === 0 && !showAddForm ? (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No Payment Methods</h3>
              <p className="text-muted-foreground text-center mb-6">
                Add a payment method to make checkout faster and easier.
              </p>
              <Button onClick={() => setShowAddForm(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Add Your First Payment Method
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-4">
            {paymentMethods.map(method => (
              <PaymentMethodCard
                key={method.id}
                paymentMethod={method}
                onRemove={handleRemovePaymentMethod}
                onSetDefault={handleSetDefault}
                isRemoving={removingId === method.id}
              />
            ))}
          </div>
        )}

        {paymentMethods.length > 0 && (
          <Alert className="mt-8">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Your payment information is securely encrypted and stored. We never store your full card number or CVV.
            </AlertDescription>
          </Alert>
        )}
      </div>
    </div>
  );
}