'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../../components/ui/select';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Separator } from '../../../components/ui/separator';
import { Badge } from '../../../components/ui/badge';
import { ArrowLeft, CreditCard, Shield, CheckCircle, AlertCircle, Loader2, Package, Calendar, MapPin, Star } from 'lucide-react';
import { useCartStore } from '../../../lib/state';
import { shoppingService, TravelGear, CartItem, Order } from '../../../services/shopping';
import { paymentService, PaymentMethod } from '../../../services/payment';
import { format, differenceInDays, parseISO } from 'date-fns';

interface CartItemWithGear extends CartItem {
  gear: TravelGear;
}

interface PaymentData {
  cardNumber: string;
  expiryDate: string;
  cvv: string;
  cardholderName: string;
  billingAddress: {
    street: string;
    city: string;
    state: string;
    zipCode: string;
    country: string;
  };
}

interface ValidationErrors {
  [key: string]: string | null;
}

const CheckoutPage = () => {
  const router = useRouter();
  const { cart, clearCart, isLoading: cartLoading } = useCartStore();
  
  // Support both legacy booking and cart-based checkout
  const [legacyBooking, setLegacyBooking] = useState<{
    type?: string;
    bookingId?: string;
    amount?: string;
    itemName?: string;
  } | null>(null);
  
  const [cartItemsWithGear, setCartItemsWithGear] = useState<CartItemWithGear[]>([]);
  const [isLoadingGear, setIsLoadingGear] = useState(false);
  
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState<string | null>(null);
  const [showAddPaymentForm, setShowAddPaymentForm] = useState(false);
  
  const [paymentData, setPaymentData] = useState<PaymentData>({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
    billingAddress: {
      street: '',
      city: '',
      state: '',
      zipCode: '',
      country: 'US'
    }
  });
  
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [orderResult, setOrderResult] = useState<Order | null>(null);

  // Handle URL parameters for legacy booking support
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const urlParams = new URLSearchParams(window.location.search);
      const type = urlParams.get('type');
      const bookingId = urlParams.get('bookingId');
      const amount = urlParams.get('amount');
      const itemName = urlParams.get('itemName');
      
      if (type && bookingId && amount && itemName) {
        setLegacyBooking({ type, bookingId, amount, itemName });
      }
    }
  }, []);
  
  // Load payment methods
  useEffect(() => {
    const loadPaymentMethods = async () => {
      try {
        const methods = await paymentService.getPaymentMethods();
        setPaymentMethods(methods);
        
        // Select default payment method if available
        const defaultMethod = methods.find(method => method.isDefault);
        if (defaultMethod) {
          setSelectedPaymentMethod(defaultMethod.id);
        }
      } catch (error) {
        console.error('Failed to load payment methods:', error);
      }
    };
    
    loadPaymentMethods();
  }, []);
  
  // Load cart items with gear details
  useEffect(() => {
    const loadCartGear = async () => {
      if (!cart?.items.length) {
        setCartItemsWithGear([]);
        return;
      }
      
      setIsLoadingGear(true);
      try {
        const itemsWithGear = await Promise.all(
          cart.items.map(async (item) => {
            try {
              const gear = await shoppingService.getGearById(item.gearId);
              return { ...item, gear };
            } catch (error) {
              console.error(`Failed to load gear ${item.gearId}:`, error);
              return null;
            }
          })
        );
        
        // Filter out items where gear loading failed
        const validItemsWithGear = itemsWithGear.filter((item): item is CartItemWithGear => item !== null);
        setCartItemsWithGear(validItemsWithGear);
      } catch (error) {
        console.error('Failed to load gear details:', error);
        setError('Failed to load some item details');
      } finally {
        setIsLoadingGear(false);
      }
    };
    
    loadCartGear();
  }, [cart?.items]);
  
  // Redirect if no checkout data
  useEffect(() => {
    if (!cartLoading && !isLoadingGear && !legacyBooking && (!cart?.items.length)) {
      router.push('/marketplace');
    }
  }, [cartLoading, isLoadingGear, legacyBooking, cart?.items, router]);

  // Calculate totals for cart checkout
  const calculateCartTotals = () => {
    if (!cartItemsWithGear.length) return { subtotal: 0, tax: 0, shipping: 0, total: 0 };
    
    const subtotal = cartItemsWithGear.reduce((sum, item) => {
      if (!item.gear) return sum;
      
      if (item.type === 'rent' && item.rentDuration) {
        const days = differenceInDays(
          parseISO(item.rentDuration.endDate),
          parseISO(item.rentDuration.startDate)
        ) + 1;
        return sum + (item.gear.rentPrice || 0) * days * item.quantity;
      }
      
      return sum + item.gear.price * item.quantity;
    }, 0);
    
    const tax = subtotal * 0.08; // 8% tax
    const shipping = subtotal > 100 ? 0 : 15; // Free shipping over $100
    const total = subtotal + tax + shipping;
    
    return { subtotal, tax, shipping, total };
  };
  
  const handleInputChange = (field: string, value: string) => {
    if (field.includes('.')) {
      const [parent, child] = field.split('.');
      setPaymentData(prev => {
        const parentValue = prev[parent as keyof PaymentData];
        return {
          ...prev,
          [parent]: {
            ...(typeof parentValue === 'object' && parentValue !== null ? parentValue : {}),
            [child]: value
          } as any
        };
      });
    } else {
      setPaymentData(prev => ({ ...prev, [field]: value } as PaymentData));
    }
    
    // Clear validation error when user starts typing
    if (validationErrors[field]) {
      setValidationErrors(prev => ({ ...prev, [field]: null }));
    }
  };

  const formatCardNumber = (value: string) => {
    // Remove all non-digits
    const digits = value.replace(/\D/g, '');
    // Add spaces every 4 digits
    return digits.replace(/(\d{4})(?=\d)/g, '$1 ').trim();
  };

  const formatExpiryDate = (value: string) => {
    // Remove all non-digits
    const digits = value.replace(/\D/g, '');
    // Add slash after 2 digits
    if (digits.length >= 2) {
      return digits.substring(0, 2) + '/' + digits.substring(2, 4);
    }
    return digits;
  };

  const validateForm = (): boolean => {
    const errors: ValidationErrors = {};
    
    // Check if we have items to checkout
    if (!legacyBooking && cartItemsWithGear.length === 0) {
      setError('Your cart is empty. Please add items before checkout.');
      return false;
    }
    
    // Card number validation
    const cardNumber = paymentData.cardNumber.replace(/\s/g, '');
    if (!cardNumber) {
      errors.cardNumber = 'Card number is required';
    } else if (cardNumber.length < 13 || cardNumber.length > 19) {
      errors.cardNumber = 'Invalid card number';
    } else if (!/^\d+$/.test(cardNumber)) {
      errors.cardNumber = 'Card number must contain only digits';
    }
    
    // Expiry date validation
    if (!paymentData.expiryDate) {
      errors.expiryDate = 'Expiry date is required';
    } else {
      const [month, year] = paymentData.expiryDate.split('/');
      const currentDate = new Date();
      const currentYear = currentDate.getFullYear() % 100;
      const currentMonth = currentDate.getMonth() + 1;
      
      if (!month || !year || parseInt(month) < 1 || parseInt(month) > 12) {
        errors.expiryDate = 'Invalid expiry date';
      } else if (parseInt(year) < currentYear || (parseInt(year) === currentYear && parseInt(month) < currentMonth)) {
        errors.expiryDate = 'Card has expired';
      }
    }
    
    // CVV validation
    if (!paymentData.cvv) {
      errors.cvv = 'CVV is required';
    } else if (!/^\d{3,4}$/.test(paymentData.cvv)) {
      errors.cvv = 'Invalid CVV';
    }
    
    // Cardholder name validation
    if (!paymentData.cardholderName.trim()) {
      errors.cardholderName = 'Cardholder name is required';
    }
    
    // Billing address validation
    if (!paymentData.billingAddress.street.trim()) {
      errors['billingAddress.street'] = 'Street address is required';
    }
    if (!paymentData.billingAddress.city.trim()) {
      errors['billingAddress.city'] = 'City is required';
    }
    if (!paymentData.billingAddress.zipCode.trim()) {
      errors['billingAddress.zipCode'] = 'ZIP code is required';
    }
    
    // Cart-specific validation
    if (!legacyBooking) {
      for (const item of cartItemsWithGear) {
        if (item.type === 'rent' && (!item.rentDuration || !item.rentDuration.startDate || !item.rentDuration.endDate)) {
          errors.rentDuration = 'Rental duration is required for rental items';
          break;
        }
      }
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setProcessing(true);
    setError(null);
    
    try {
      let order: Order;
      
      if (legacyBooking) {
        // Handle legacy booking checkout with PaymentService
        let paymentMethodId = selectedPaymentMethod;
        
        // If no payment method selected or using new card, create payment intent
        if (!paymentMethodId || showAddPaymentForm) {
          const paymentIntent = await paymentService.createPaymentIntent({
            orderId: legacyBooking.bookingId!,
            amount: parseFloat(legacyBooking.amount!) * 100, // Convert to cents
            currency: 'usd'
          });
          
          const confirmedIntent = await paymentService.confirmPayment({
            paymentIntentId: paymentIntent.id,
            paymentMethodId: paymentMethodId || 'new-card'
          });
          
          if (confirmedIntent.status !== 'succeeded') {
            throw new Error('Payment processing failed');
          }
        } else {
          // Use existing payment method
          const paymentIntent = await paymentService.createPaymentIntent({
            orderId: legacyBooking.bookingId!,
            amount: parseFloat(legacyBooking.amount!) * 100,
            currency: 'usd',
            paymentMethodId: paymentMethodId
          });
          
          const confirmedIntent = await paymentService.confirmPayment({
            paymentIntentId: paymentIntent.id,
            paymentMethodId: paymentMethodId
          });
          
          if (confirmedIntent.status !== 'succeeded') {
            throw new Error('Payment processing failed');
          }
        }
        
        // Create mock order for legacy booking
        order = {
          id: 'legacy-' + Date.now(),
          userId: 'current-user',
          items: [],
          status: 'confirmed',
          payment: {
            method: 'card',
            status: 'completed',
            amount: parseFloat(legacyBooking.amount!)
          },
          shipping: {
            address: paymentData.billingAddress,
            method: 'standard'
          },
          totals: {
            subtotal: parseFloat(legacyBooking.amount!),
            tax: 0,
            shipping: 0,
            total: parseFloat(legacyBooking.amount!)
          },
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };
        
        // Send legacy notification
        await fetch('/api/notifications/send', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({
            type: 'booking_confirmation',
            bookingId: legacyBooking.bookingId,
            message: `Your ${legacyBooking.type} booking for ${legacyBooking.itemName} has been confirmed!`
          })
        });
      } else {
        // Handle cart-based checkout with PaymentService integration
        const { total } = calculateCartTotals();
        
        // Create order first
        order = await shoppingService.createOrder({
          cartId: cart?.id || 'temp-cart-' + Date.now(),
          shipping: {
            address: paymentData.billingAddress,
            method: 'standard'
          },
          payment: {
            method: 'card'
          }
        });
        
        // Process payment through PaymentService
        let paymentMethodId = selectedPaymentMethod;
        
        if (!paymentMethodId || showAddPaymentForm) {
          // Create and confirm payment intent with new card
          const paymentIntent = await paymentService.createPaymentIntent({
            orderId: order.id,
            amount: Math.round(total * 100), // Convert to cents
            currency: 'usd'
          });
          
          await paymentService.confirmPayment({
            paymentIntentId: paymentIntent.id,
            paymentMethodId: paymentMethodId || 'new-card'
          });
        } else {
          // Use existing payment method
          const paymentIntent = await paymentService.createPaymentIntent({
            orderId: order.id,
            amount: Math.round(total * 100),
            currency: 'usd',
            paymentMethodId: paymentMethodId
          });
          
          await paymentService.confirmPayment({
            paymentIntentId: paymentIntent.id,
            paymentMethodId: paymentMethodId
          });
        }
        
        // Clear cart after successful payment
        await clearCart();
      }
      
      setOrderResult(order);
      setSuccess(true);
      
      // Redirect to confirmation page after 3 seconds
      setTimeout(() => {
        if (legacyBooking) {
          router.push(`/booking-confirmation?bookingId=${legacyBooking.bookingId}&paymentId=${order.id}&type=${legacyBooking.type}&itemName=${legacyBooking.itemName}&amount=${legacyBooking.amount}`);
        } else {
          router.push(`/order-confirmation?orderId=${order.id}`);
        }
      }, 3000);
      
    } catch (error) {
      console.error('Payment error:', error);
      setError('Payment processing failed. Please check your card details and try again.');
      
      // Handle specific error cases
      if (error instanceof Error) {
        if (error.message.includes('timeout') || error.name === 'TimeoutError') {
          setError('Payment service is currently unavailable. Please try again later.');
        } else if (error.message.includes('inventory')) {
          setError('Some items in your cart are no longer available. Please review your cart.');
        } else if (error.message.includes('payment')) {
          setError('Payment was declined. Please check your card details.');
        }
      }
    } finally {
      setProcessing(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center py-8">
        <Card className="w-full max-w-md">
          <CardContent className="p-8 text-center">
            <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Payment Successful!</h2>
            <p className="text-gray-600 mb-4">
              {legacyBooking ? 
                `Your booking for ${legacyBooking.itemName} has been confirmed.` :
                'Your order has been confirmed.'
              }
            </p>
            <p className="text-sm text-gray-500">
              Redirecting to confirmation page...
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        {/* Header */}
        <div className="mb-8">
          <Button
            variant="ghost"
            onClick={() => router.back()}
            className="mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Itinerary
          </Button>
          <h1 className="text-3xl font-bold text-gray-900">Secure Checkout</h1>
          <p className="text-gray-600 mt-2">
            Complete your booking with secure payment
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Payment Form */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <CreditCard className="h-5 w-5" />
                  Payment Information
                </CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* Payment Method Selection */}
                  {paymentMethods.length > 0 && (
                    <div className="space-y-4">
                      <h3 className="text-lg font-semibold">Payment Method</h3>
                      <div className="space-y-3">
                        {paymentMethods.map((method) => (
                          <div key={method.id} className="flex items-center space-x-3">
                            <input
                              type="radio"
                              id={method.id}
                              name="paymentMethod"
                              value={method.id}
                              checked={selectedPaymentMethod === method.id}
                              onChange={(e) => {
                                setSelectedPaymentMethod(e.target.value);
                                setShowAddPaymentForm(false);
                              }}
                              className="h-4 w-4 text-blue-600"
                            />
                            <label htmlFor={method.id} className="flex-1 cursor-pointer">
                              <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50">
                                <div className="flex items-center space-x-3">
                                  <CreditCard className="h-5 w-5 text-gray-400" />
                                  <div>
                                    <div className="font-medium">**** **** **** {method.last4}</div>
                                    <div className="text-sm text-gray-500">
                                      {method.brand?.toUpperCase() || 'CARD'} • Expires {method.expiryMonth}/{method.expiryYear}
                                    </div>
                                  </div>
                                </div>
                                {method.isDefault && (
                                  <Badge variant="secondary">Default</Badge>
                                )}
                              </div>
                            </label>
                          </div>
                        ))}
                        
                        <div className="flex items-center space-x-3">
                          <input
                            type="radio"
                            id="new-card"
                            name="paymentMethod"
                            value="new"
                            checked={showAddPaymentForm}
                            onChange={(e) => {
                              setShowAddPaymentForm(true);
                              setSelectedPaymentMethod(null);
                            }}
                            className="h-4 w-4 text-blue-600"
                          />
                          <label htmlFor="new-card" className="cursor-pointer">
                            <div className="flex items-center space-x-3 p-3 border rounded-lg hover:bg-gray-50">
                              <CreditCard className="h-5 w-5 text-gray-400" />
                              <span className="font-medium">Add new payment method</span>
                            </div>
                          </label>
                        </div>
                      </div>
                    </div>
                  )}
                  
                  {/* Card Details - Show when adding new payment method or no saved methods */}
                  {(showAddPaymentForm || paymentMethods.length === 0) && (
                    <div className="space-y-4">
                      <h3 className="text-lg font-semibold">
                        {paymentMethods.length > 0 ? 'New Payment Method' : 'Payment Information'}
                      </h3>
                    <div className="space-y-2">
                      <Label htmlFor="cardNumber">Card Number</Label>
                      <Input
                        id="cardNumber"
                        placeholder="1234 5678 9012 3456"
                        value={paymentData.cardNumber}
                        onChange={(e) => handleInputChange('cardNumber', formatCardNumber(e.target.value))}
                        maxLength={19}
                        className={validationErrors.cardNumber ? 'border-red-500' : ''}
                      />
                      {validationErrors.cardNumber && (
                        <p className="text-sm text-red-500">{validationErrors.cardNumber}</p>
                      )}
                    </div>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="expiryDate">Expiry Date</Label>
                        <Input
                          id="expiryDate"
                          placeholder="MM/YY"
                          value={paymentData.expiryDate}
                          onChange={(e) => handleInputChange('expiryDate', formatExpiryDate(e.target.value))}
                          maxLength={5}
                          className={validationErrors.expiryDate ? 'border-red-500' : ''}
                        />
                        {validationErrors.expiryDate && (
                          <p className="text-sm text-red-500">{validationErrors.expiryDate}</p>
                        )}
                      </div>
                      
                      <div className="space-y-2">
                        <Label htmlFor="cvv">CVV</Label>
                        <Input
                          id="cvv"
                          placeholder="123"
                          value={paymentData.cvv}
                          onChange={(e) => handleInputChange('cvv', e.target.value.replace(/\D/g, ''))}
                          maxLength={4}
                          className={validationErrors.cvv ? 'border-red-500' : ''}
                        />
                        {validationErrors.cvv && (
                          <p className="text-sm text-red-500">{validationErrors.cvv}</p>
                        )}
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="cardholderName">Cardholder Name</Label>
                      <Input
                        id="cardholderName"
                        placeholder="John Doe"
                        value={paymentData.cardholderName}
                        onChange={(e) => handleInputChange('cardholderName', e.target.value)}
                        className={validationErrors.cardholderName ? 'border-red-500' : ''}
                      />
                      {validationErrors.cardholderName && (
                        <p className="text-sm text-red-500">{validationErrors.cardholderName}</p>
                      )}
                    </div>
                    </div>
                  )}

                  <Separator />

                  {/* Billing Address */}
                  <div className="space-y-4">
                    <h3 className="text-lg font-semibold">Billing Address</h3>
                    
                    <div className="space-y-2">
                      <Label htmlFor="street">Street Address</Label>
                      <Input
                        id="street"
                        placeholder="123 Main St"
                        value={paymentData.billingAddress.street}
                        onChange={(e) => handleInputChange('billingAddress.street', e.target.value)}
                        className={validationErrors['billingAddress.street'] ? 'border-red-500' : ''}
                      />
                      {validationErrors['billingAddress.street'] && (
                        <p className="text-sm text-red-500">{validationErrors['billingAddress.street']}</p>
                      )}
                    </div>
                    
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="city">City</Label>
                        <Input
                          id="city"
                          placeholder="New York"
                          value={paymentData.billingAddress.city}
                          onChange={(e) => handleInputChange('billingAddress.city', e.target.value)}
                          className={validationErrors['billingAddress.city'] ? 'border-red-500' : ''}
                        />
                        {validationErrors['billingAddress.city'] && (
                          <p className="text-sm text-red-500">{validationErrors['billingAddress.city']}</p>
                        )}
                      </div>
                      
                      <div className="space-y-2">
                        <Label htmlFor="state">State</Label>
                        <Select value={paymentData.billingAddress.state} onValueChange={(value) => handleInputChange('billingAddress.state', value)}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select state" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="NY">New York</SelectItem>
                            <SelectItem value="CA">California</SelectItem>
                            <SelectItem value="TX">Texas</SelectItem>
                            <SelectItem value="FL">Florida</SelectItem>
                            {/* Add more states as needed */}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="zipCode">ZIP Code</Label>
                      <Input
                        id="zipCode"
                        placeholder="10001"
                        value={paymentData.billingAddress.zipCode}
                        onChange={(e) => handleInputChange('billingAddress.zipCode', e.target.value)}
                        className={validationErrors['billingAddress.zipCode'] ? 'border-red-500' : ''}
                      />
                      {validationErrors['billingAddress.zipCode'] && (
                        <p className="text-sm text-red-500">{validationErrors['billingAddress.zipCode']}</p>
                      )}
                    </div>
                  </div>

                  {/* Error Alert */}
                  {error && (
                    <Alert className="border-red-200 bg-red-50">
                      <AlertCircle className="h-4 w-4 text-red-600" />
                      <AlertDescription className="text-red-800">
                        {error}
                      </AlertDescription>
                    </Alert>
                  )}

                  {/* Submit Button */}
                  <Button type="submit" disabled={processing} className="w-full">
                    {processing ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Processing Payment...
                      </>
                    ) : (
                      legacyBooking ? 
                        `Pay $${legacyBooking.amount}` : 
                        `Pay $${calculateCartTotals().total.toFixed(2)}`
                    )}
                  </Button>
                </form>
              </CardContent>
            </Card>
          </div>

          {/* Order Summary */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Order Summary</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {legacyBooking ? (
                  // Legacy booking summary
                  <>
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-semibold">{legacyBooking.itemName}</h4>
                        <Badge variant="secondary" className="mt-1">
                          {legacyBooking.type === 'hotel' ? 'Hotel Booking' : 'Activity Booking'}
                        </Badge>
                      </div>
                      <span className="font-semibold">${legacyBooking.amount}</span>
                    </div>
                    
                    <Separator />
                    
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span>Subtotal:</span>
                        <span>${legacyBooking.amount}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Service Fee:</span>
                        <span>$0.00</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Taxes:</span>
                        <span>$0.00</span>
                      </div>
                    </div>
                    
                    <Separator />
                    
                    <div className="flex justify-between font-semibold text-lg">
                      <span>Total:</span>
                      <span>${legacyBooking.amount}</span>
                    </div>
                  </>
                ) : (
                  // Cart-based summary
                  <>
                    {cartItemsWithGear.map((item) => (
                      <div key={`${item.gearId}-${item.type}`} className="space-y-2">
                        <div className="flex justify-between items-start">
                          <div className="flex-1">
                            <h4 className="font-semibold">{item.gear?.title}</h4>
                            <div className="flex items-center gap-2 mt-1">
                              <Badge variant="secondary">
                                {item.type === 'rent' ? 'Rental' : 'Purchase'}
                              </Badge>
                              <span className="text-sm text-gray-600">Qty: {item.quantity}</span>
                            </div>
                            {item.type === 'rent' && item.rentDuration && (
                              <div className="text-sm text-gray-600 mt-1">
                                <Calendar className="h-3 w-3 inline mr-1" />
                                {format(parseISO(item.rentDuration.startDate), 'MMM d')} - 
                                {format(parseISO(item.rentDuration.endDate), 'MMM d, yyyy')}
                              </div>
                            )}
                          </div>
                          <div className="text-right">
                            <span className="font-semibold">
                              ${item.type === 'rent' && item.rentDuration ? 
                                ((item.gear?.rentPrice || 0) * 
                                 (differenceInDays(parseISO(item.rentDuration.endDate), parseISO(item.rentDuration.startDate)) + 1) * 
                                 item.quantity).toFixed(2) :
                                ((item.gear?.price || 0) * item.quantity).toFixed(2)
                              }
                            </span>
                          </div>
                        </div>
                      </div>
                    ))}
                    
                    <Separator />
                    
                    {(() => {
                      const { subtotal, tax, shipping, total } = calculateCartTotals();
                      return (
                        <>
                          <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                              <span>Subtotal:</span>
                              <span>${subtotal.toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>Tax (8%):</span>
                              <span>${tax.toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>Shipping:</span>
                              <span>{shipping === 0 ? 'Free' : `$${shipping.toFixed(2)}`}</span>
                            </div>
                          </div>
                          
                          <Separator />
                          
                          <div className="flex justify-between font-semibold text-lg">
                            <span>Total:</span>
                            <span>${total.toFixed(2)}</span>
                          </div>
                        </>
                      );
                    })()
                    }
                  </>
                )}
              </CardContent>
            </Card>

            {/* Security Info */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <Shield className="h-4 w-4" />
                  <span>Your payment is secured with 256-bit SSL encryption</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;