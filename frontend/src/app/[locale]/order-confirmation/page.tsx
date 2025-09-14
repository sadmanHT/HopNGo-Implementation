'use client';

import React, { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { CheckCircle, Package, Calendar, MapPin, Star, ArrowLeft, Download } from 'lucide-react';
import { shoppingService, type Order, type TravelGear } from '@/services/shopping';
import RatingReview from '@/components/marketplace/RatingReview';
import { format, differenceInDays, parseISO } from 'date-fns';

interface OrderItemWithGear {
  gearId: string;
  quantity: number;
  type: 'rent' | 'sale';
  rentDuration?: {
    startDate: string;
    endDate: string;
  };
  gear?: TravelGear;
}

const OrderConfirmationContent = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderId = searchParams?.get('orderId');
  
  const [order, setOrder] = useState<Order | null>(null);
  const [orderItems, setOrderItems] = useState<OrderItemWithGear[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showReviews, setShowReviews] = useState<{ [key: string]: boolean }>({});

  useEffect(() => {
    if (!orderId) {
      router.push('/marketplace');
      return;
    }
    
    loadOrderDetails();
  }, [orderId, router]);

  const loadOrderDetails = async () => {
    if (!orderId) return;
    
    setIsLoading(true);
    try {
      // Load order details
      const orderData = await shoppingService.getOrder(orderId);
      setOrder(orderData);
      
      // Load gear details for each item
      const itemsWithGear = await Promise.all(
        orderData.items.map(async (item) => {
          try {
            const gear = await shoppingService.getGearById(item.gearId);
            return { 
              ...item, 
              gear,
              type: (item.type === 'purchase' ? 'sale' : item.type) as 'rent' | 'sale'
            };
          } catch (error) {
            console.error(`Failed to load gear ${item.gearId}:`, error);
            return { 
              ...item, 
              gear: undefined,
              type: (item.type === 'purchase' ? 'sale' : item.type) as 'rent' | 'sale'
            };
          }
        })
      );
      
      setOrderItems(itemsWithGear);
    } catch (error) {
      console.error('Failed to load order details:', error);
      setError('Failed to load order details');
    } finally {
      setIsLoading(false);
    }
  };

  const toggleReviewSection = (gearId: string) => {
    setShowReviews(prev => ({
      ...prev,
      [gearId]: !prev[gearId]
    }));
  };

  const calculateItemTotal = (item: OrderItemWithGear) => {
    if (!item.gear) return 0;
    
    if (item.type === 'rent' && item.rentDuration) {
      const days = differenceInDays(
        parseISO(item.rentDuration.endDate),
        parseISO(item.rentDuration.startDate)
      ) + 1;
      return (item.gear.rentPrice || 0) * days * item.quantity;
    }
    
    return item.gear.price * item.quantity;
  };

  const canReviewItem = (item: OrderItemWithGear) => {
    // Allow reviews for completed orders
    return order?.status === 'delivered';
  };



  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-4xl mx-auto px-4">
          <div className="animate-pulse space-y-6">
            <div className="h-8 bg-gray-200 rounded w-1/3"></div>
            <div className="h-64 bg-gray-200 rounded"></div>
            <div className="h-32 bg-gray-200 rounded"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-4xl mx-auto px-4">
          <Card className="border-red-200 bg-red-50">
            <CardContent className="p-6 text-center">
              <div className="text-red-600 mb-4">
                <Package className="h-12 w-12 mx-auto mb-2" />
                <h2 className="text-xl font-semibold">Order Not Found</h2>
                <p className="mt-2">{error || 'The order you are looking for could not be found.'}</p>
              </div>
              <Button onClick={() => router.push('/marketplace')} variant="outline">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Marketplace
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <Button
            variant="ghost"
            onClick={() => router.push('/marketplace')}
            className="flex items-center gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Marketplace
          </Button>
        </div>

        {/* Order Confirmation */}
        <Card className="border-green-200 bg-green-50">
          <CardContent className="p-6">
            <div className="text-center">
              <CheckCircle className="h-16 w-16 text-green-600 mx-auto mb-4" />
              <h1 className="text-2xl font-bold text-gray-900 mb-2">
                Order Confirmed!
              </h1>
              <p className="text-gray-600 mb-4">
                Thank you for your order. We've received your payment and will process your items shortly.
              </p>
              <div className="flex items-center justify-center gap-4 text-sm text-gray-600">
                <span>Order ID: <strong>{order.id}</strong></span>
                <span>•</span>
                <span>Placed on {format(new Date(order.createdAt), 'MMM d, yyyy')}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Order Status */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span>Order Status</span>
              <Badge
                variant={order.status === 'delivered' ? 'default' : 'secondary'}
                className={order.status === 'delivered' ? 'bg-green-600' : ''}
              >
                {order.status.charAt(0).toUpperCase() + order.status.slice(1)}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Package className="h-4 w-4" />
                  Payment Status
                </h4>
                <Badge
                  variant={order.status === 'delivered' ? 'default' : 'secondary'}
                  className={order.status === 'delivered' ? 'bg-green-600' : ''}
                >
                  {order.status === 'delivered' ? 'Paid' : 'Pending'}
                </Badge>
              </div>
              
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <MapPin className="h-4 w-4" />
                  Shipping Address
                </h4>
                <div className="text-sm text-gray-600">
                  <div>Shipping address not available</div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Order Items */}
        <Card>
          <CardHeader>
            <CardTitle>Order Items</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              {orderItems.map((item, index) => (
                <div key={`${item.gearId}-${item.type}-${index}`}>
                  <div className="flex items-start gap-4 p-4 border rounded-lg">
                    {/* Item Image */}
                    <div className="w-20 h-20 bg-gray-200 rounded-lg flex items-center justify-center">
                      {item.gear?.images?.[0] ? (
                        <img
                          src={item.gear.images[0]}
                          alt={item.gear.title}
                          className="w-full h-full object-cover rounded-lg"
                        />
                      ) : (
                        <Package className="h-8 w-8 text-gray-400" />
                      )}
                    </div>

                    {/* Item Details */}
                    <div className="flex-1">
                      <h4 className="font-semibold">{item.gear?.title || 'Unknown Item'}</h4>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="secondary">
                          {item.type === 'rent' ? 'Rental' : 'Purchase'}
                        </Badge>
                        <span className="text-sm text-gray-600">Qty: {item.quantity}</span>
                      </div>
                      
                      {item.type === 'rent' && item.rentDuration && (
                        <div className="text-sm text-gray-600 mt-2 flex items-center gap-1">
                          <Calendar className="h-3 w-3" />
                          {format(parseISO(item.rentDuration.startDate), 'MMM d')} - 
                          {format(parseISO(item.rentDuration.endDate), 'MMM d, yyyy')}
                        </div>
                      )}
                    </div>

                    {/* Item Price */}
                    <div className="text-right">
                      <div className="font-semibold">
                        ${calculateItemTotal(item).toFixed(2)}
                      </div>
                      {item.gear && (
                        <div className="text-sm text-gray-600">
                          ${item.type === 'rent' ? item.gear.rentPrice : item.gear.price} each
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Review Section */}
                  {canReviewItem(item) && item.gear && (
                    <div className="mt-4">
                      <div className="flex items-center justify-between mb-4">
                        <h5 className="font-medium flex items-center gap-2">
                          <Star className="h-4 w-4" />
                          Rate & Review This Item
                        </h5>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => toggleReviewSection(item.gearId)}
                        >
                          {showReviews[item.gearId] ? 'Hide Reviews' : 'Show Reviews'}
                        </Button>
                      </div>
                      
                      {showReviews[item.gearId] && (
                        <RatingReview
                          gearId={item.gearId}
                          gear={item.gear}
                          orderId={order.id}
                          canReview={true}
                          showReviews={true}
                        />
                      )}
                    </div>
                  )}

                  {index < orderItems.length - 1 && <Separator className="mt-6" />}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Order Total */}
        <Card>
          <CardHeader>
            <CardTitle>Order Summary</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {(() => {
                const itemsTotal = orderItems.length * 50; // Placeholder calculation
                const subtotal = itemsTotal * 0.85;
                const tax = itemsTotal * 0.08;
                const shipping = itemsTotal * 0.07;
                const total = subtotal + tax + shipping;
                return (
                  <>
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
                      <span>${shipping.toFixed(2)}</span>
                    </div>
                    <Separator />
                    <div className="flex justify-between font-semibold text-lg">
                      <span>Total:</span>
                      <span>${total.toFixed(2)}</span>
                    </div>
                  </>
                );
              })()}
            </div>
          </CardContent>
        </Card>

        {/* Action Buttons */}
        <div className="flex gap-4">
          <Button onClick={() => router.push('/marketplace')} className="flex-1">
            Continue Shopping
          </Button>
          <Button variant="outline" className="flex items-center gap-2">
            <Download className="h-4 w-4" />
            Download Receipt
          </Button>
        </div>
      </div>
    </div>
  );
};

const OrderConfirmationPage = () => {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen">Loading...</div>}>
      <OrderConfirmationContent />
    </Suspense>
  );
};

export default OrderConfirmationPage;