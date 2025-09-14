'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Badge } from '../../../components/ui/badge';
import { Separator } from '../../../components/ui/separator';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { 
  ShoppingCart, 
  Trash2, 
  Plus, 
  Minus, 
  Calendar, 
  Package, 
  MapPin, 
  Clock, 
  CreditCard, 
  Truck, 
  Shield, 
  AlertCircle,
  ArrowRight,
  Heart,
  Star,
  Edit3,
  X,
  RefreshCw,
  AlertTriangle
} from 'lucide-react';
import { useCartStore } from '../../../lib/state';
import { shoppingService, CartItem, TravelGear } from '../../../services/shopping';
import { useRouter } from 'next/navigation';
import { format, differenceInDays, parseISO } from 'date-fns';
import InventoryConflictNotification from '../../../components/marketplace/InventoryConflictNotification';

interface CartItemWithGear extends CartItem {
  gear: TravelGear;
}

const CartPage = () => {
  const router = useRouter();
  const {
    cart,
    isLoading,
    error,
    loadCart,
    updateCartItem,
    removeFromCart,
    clearCart,
    isUpdatingItem,
    isRemovingItem,
    isValidatingInventory,
    inventoryConflicts,
    lastInventoryCheck,
    validateInventory,
    refreshInventory
  } = useCartStore();
  
  const [cartItemsWithGear, setCartItemsWithGear] = useState<CartItemWithGear[]>([]);
  const [isLoadingGear, setIsLoadingGear] = useState(false);
  const [gearError, setGearError] = useState<string | null>(null);
  const [editingRentDates, setEditingRentDates] = useState<string | null>(null);
  const [tempRentDates, setTempRentDates] = useState<{ startDate: string; endDate: string } | null>(null);
  const [promoCode, setPromoCode] = useState('');
  const [appliedPromo, setAppliedPromo] = useState<{ code: string; discount: number } | null>(null);
  const [showConflicts, setShowConflicts] = useState(true);
  
  // Load cart and gear details on mount
  useEffect(() => {
    loadCart();
  }, [loadCart]);
  
  // Validate inventory when cart loads or when user returns to page
  useEffect(() => {
    if (cart?.items.length) {
      // Check if inventory was validated recently (within 5 minutes)
      const shouldValidate = !lastInventoryCheck || 
        (Date.now() - new Date(lastInventoryCheck).getTime()) > 5 * 60 * 1000;
      
      if (shouldValidate) {
        validateInventory();
      }
    }
  }, [cart?.items.length, lastInventoryCheck, validateInventory]);

  // Show conflicts notification when conflicts are detected
  useEffect(() => {
    if (inventoryConflicts.length > 0) {
      setShowConflicts(true);
    }
  }, [inventoryConflicts.length]);
  
  // Load gear details for cart items
  useEffect(() => {
    const loadGearDetails = async () => {
      if (!cart?.items.length) {
        setCartItemsWithGear([]);
        return;
      }
      
      setIsLoadingGear(true);
      setGearError(null);
      
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
        setGearError('Failed to load some item details');
      } finally {
        setIsLoadingGear(false);
      }
    };
    
    loadGearDetails();
  }, [cart?.items]);
  
  // Calculate totals
  const calculateItemTotal = (item: CartItemWithGear) => {
    if (!item.gear) return 0;
    
    if (item.type === 'rent' && item.rentDuration) {
      const days = differenceInDays(
        parseISO(item.rentDuration.endDate),
        parseISO(item.rentDuration.startDate)
      ) + 1; // Include both start and end dates
      return (item.gear.rentPrice || 0) * days * item.quantity;
    }
    
    return item.gear.price * item.quantity;
  };
  
  const subtotal = cartItemsWithGear.reduce((sum, item) => sum + calculateItemTotal(item), 0);
  const discount = appliedPromo ? subtotal * (appliedPromo.discount / 100) : 0;
  const tax = (subtotal - discount) * 0.08; // 8% tax
  const shipping = subtotal > 100 ? 0 : 15; // Free shipping over $100
  const total = subtotal - discount + tax + shipping;
  
  // Handle quantity update
  const handleQuantityUpdate = async (itemId: string, newQuantity: number) => {
    if (newQuantity < 1) return;
    
    try {
      await updateCartItem(itemId, { quantity: newQuantity });
    } catch (error) {
      console.error('Failed to update quantity:', error);
    }
  };
  
  // Handle remove item
  const handleRemoveItem = async (itemId: string) => {
    try {
      await removeFromCart(itemId);
    } catch (error) {
      console.error('Failed to remove item:', error);
    }
  };
  
  // Handle rent date update
  const handleRentDateUpdate = async (itemId: string) => {
    if (!tempRentDates) return;
    
    try {
      await updateCartItem(itemId, {
        rentDuration: tempRentDates
      });
      setEditingRentDates(null);
      setTempRentDates(null);
    } catch (error) {
      console.error('Failed to update rent dates:', error);
    }
  };
  
  // Handle promo code
  const applyPromoCode = async () => {
    if (!promoCode.trim()) return;
    
    try {
      // Mock promo code validation
      const validPromoCodes = {
        'WELCOME10': 10,
        'SAVE20': 20,
        'FIRSTTIME': 15
      };
      
      const discount = validPromoCodes[promoCode.toUpperCase() as keyof typeof validPromoCodes];
      
      if (discount) {
        setAppliedPromo({ code: promoCode.toUpperCase(), discount });
        setPromoCode('');
      } else {
        // TODO: Show error toast
        console.error('Invalid promo code');
      }
    } catch (error) {
      console.error('Failed to apply promo code:', error);
    }
  };
  
  // Handle checkout
  const handleCheckout = () => {
    if (cartItemsWithGear.length === 0) return;
    
    // Navigate to checkout with cart data
    router.push('/checkout');
  };
  
  // Render cart item
  const renderCartItem = (item: CartItemWithGear) => {
    const { gear } = item;
    const itemTotal = calculateItemTotal(item);
    const isEditing = editingRentDates === item.id;
    
    return (
      <Card key={item.id} className="mb-4">
        <CardContent className="p-4">
          <div className="flex space-x-4">
            {/* Item Image */}
            <div className="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
              {gear?.images.length ? (
                <img 
                  src={gear.images[0]} 
                  alt={gear.title}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Package className="w-8 h-8 text-gray-400" />
                </div>
              )}
            </div>
            
            {/* Item Details */}
            <div className="flex-1 space-y-2">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold">{gear?.title || 'Loading...'}</h3>
                  <div className="flex items-center space-x-2 text-sm text-gray-500">
                    <Badge variant={item.type === 'rent' ? 'secondary' : 'default'}>
                      {item.type === 'rent' ? 'Rental' : 'Purchase'}
                    </Badge>
                    {gear && (
                      <div className="flex items-center space-x-1">
                        <MapPin className="w-3 h-3" />
                        <span>{gear.location.city}</span>
                      </div>
                    )}
                  </div>
                </div>
                
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleRemoveItem(item.id)}
                  disabled={isRemovingItem}
                >
                  <Trash2 className="w-4 h-4" />
                </Button>
              </div>
              
              {/* Rental Duration */}
              {item.type === 'rent' && item.rentDuration && (
                <div className="bg-blue-50 p-3 rounded-lg">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <Calendar className="w-4 h-4 text-blue-600" />
                      <span className="text-sm font-medium">Rental Period</span>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setEditingRentDates(item.id);
                        setTempRentDates(item.rentDuration!);
                      }}
                    >
                      <Edit3 className="w-3 h-3" />
                    </Button>
                  </div>
                  
                  {isEditing ? (
                    <div className="mt-2 space-y-2">
                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="text-xs text-gray-600">Start Date</label>
                          <Input
                            type="date"
                            value={tempRentDates?.startDate || ''}
                            onChange={(e) => setTempRentDates(prev => prev ? { ...prev, startDate: e.target.value } : null)}
                            className="text-sm"
                          />
                        </div>
                        <div>
                          <label className="text-xs text-gray-600">End Date</label>
                          <Input
                            type="date"
                            value={tempRentDates?.endDate || ''}
                            onChange={(e) => setTempRentDates(prev => prev ? { ...prev, endDate: e.target.value } : null)}
                            className="text-sm"
                          />
                        </div>
                      </div>
                      <div className="flex space-x-2">
                        <Button size="sm" onClick={() => handleRentDateUpdate(item.id)}>
                          Save
                        </Button>
                        <Button 
                          size="sm" 
                          variant="outline" 
                          onClick={() => {
                            setEditingRentDates(null);
                            setTempRentDates(null);
                          }}
                        >
                          Cancel
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className="mt-1 text-sm text-gray-700">
                      {format(parseISO(item.rentDuration.startDate), 'MMM dd')} - {format(parseISO(item.rentDuration.endDate), 'MMM dd, yyyy')}
                      <span className="text-gray-500 ml-2">
                        ({differenceInDays(parseISO(item.rentDuration.endDate), parseISO(item.rentDuration.startDate)) + 1} days)
                      </span>
                    </div>
                  )}
                </div>
              )}
              
              {/* Quantity and Price */}
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleQuantityUpdate(item.id, item.quantity - 1)}
                    disabled={item.quantity <= 1 || isUpdatingItem}
                  >
                    <Minus className="w-3 h-3" />
                  </Button>
                  <span className="w-8 text-center font-medium">{item.quantity}</span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleQuantityUpdate(item.id, item.quantity + 1)}
                    disabled={isUpdatingItem}
                  >
                    <Plus className="w-3 h-3" />
                  </Button>
                </div>
                
                <div className="text-right">
                  <div className="font-bold text-lg">${itemTotal.toFixed(2)}</div>
                  {item.type === 'rent' && gear?.rentPrice && (
                    <div className="text-sm text-gray-500">
                      ${gear.rentPrice}/day × {item.quantity}
                    </div>
                  )}
                </div>
              </div>
              
              {/* Availability Warning */}
              {gear && !gear.availability.inStock && (
                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>
                    This item is currently out of stock. Please remove it or check back later.
                  </AlertDescription>
                </Alert>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    );
  };
  
  // Loading state
  if (isLoading || isLoadingGear) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="animate-pulse space-y-4">
            <div className="h-8 bg-gray-200 rounded w-1/4"></div>
            <div className="space-y-4">
              {[1, 2, 3].map(i => (
                <div key={i} className="h-32 bg-gray-200 rounded"></div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }
  
  // Empty cart state
  if (!cart?.items.length) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          <div className="text-center py-12">
            <ShoppingCart className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Your cart is empty</h2>
            <p className="text-gray-500 mb-6">
              Discover amazing travel gear from fellow travelers
            </p>
            <Button onClick={() => router.push('/marketplace')}>
              Browse Marketplace
            </Button>
          </div>
        </div>
      </div>
    );
  }
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold mb-2">Shopping Cart</h1>
            <p className="text-gray-600">{cartItemsWithGear.length} items in your cart</p>
          </div>
          
          <div className="flex items-center gap-3">
            {cartItemsWithGear.length > 0 && (
              <Button
                variant="outline"
                onClick={refreshInventory}
                disabled={isValidatingInventory}
                className="flex items-center gap-2"
              >
                <RefreshCw className={`h-4 w-4 ${isValidatingInventory ? 'animate-spin' : ''}`} />
                Refresh Inventory
              </Button>
            )}
          </div>
        </div>
        
        {/* Error States */}
        {error && (
          <Alert className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        
        {gearError && (
          <Alert className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{gearError}</AlertDescription>
          </Alert>
        )}
        
        {/* Inventory Conflicts */}
        {inventoryConflicts.length > 0 && showConflicts && (
          <InventoryConflictNotification
             conflicts={inventoryConflicts}
             onClose={() => setShowConflicts(false)}
             className="mb-6"
           />
        )}
        
        {/* Inventory Validation Status */}
        {isValidatingInventory && (
          <Alert className="mb-6">
            <RefreshCw className="h-4 w-4 animate-spin" />
            <AlertDescription>Checking item availability...</AlertDescription>
          </Alert>
        )}
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-2">
            <div className="space-y-4">
              {cartItemsWithGear.map(renderCartItem)}
            </div>
            
            {/* Cart Actions */}
            <div className="mt-6 flex justify-between">
              <Button 
                variant="outline" 
                onClick={() => router.push('/marketplace')}
              >
                Continue Shopping
              </Button>
              <Button 
                variant="outline" 
                onClick={clearCart}
                disabled={isLoading}
              >
                Clear Cart
              </Button>
            </div>
          </div>
          
          {/* Order Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-4">
              <CardHeader>
                <CardTitle>Order Summary</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* Promo Code */}
                <div>
                  <label className="text-sm font-medium mb-2 block">Promo Code</label>
                  <div className="flex space-x-2">
                    <Input
                      placeholder="Enter code"
                      value={promoCode}
                      onChange={(e) => setPromoCode(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && applyPromoCode()}
                    />
                    <Button onClick={applyPromoCode} disabled={!promoCode.trim()}>
                      Apply
                    </Button>
                  </div>
                  {appliedPromo && (
                    <div className="mt-2 flex items-center justify-between text-sm text-green-600">
                      <span>Code: {appliedPromo.code}</span>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setAppliedPromo(null)}
                      >
                        <X className="w-3 h-3" />
                      </Button>
                    </div>
                  )}
                </div>
                
                <Separator />
                
                {/* Price Breakdown */}
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span>Subtotal</span>
                    <span>${subtotal.toFixed(2)}</span>
                  </div>
                  
                  {appliedPromo && (
                    <div className="flex justify-between text-green-600">
                      <span>Discount ({appliedPromo.discount}%)</span>
                      <span>-${discount.toFixed(2)}</span>
                    </div>
                  )}
                  
                  <div className="flex justify-between">
                    <span>Tax</span>
                    <span>${tax.toFixed(2)}</span>
                  </div>
                  
                  <div className="flex justify-between">
                    <span>Shipping</span>
                    <span>{shipping === 0 ? 'Free' : `$${shipping.toFixed(2)}`}</span>
                  </div>
                  
                  <Separator />
                  
                  <div className="flex justify-between font-bold text-lg">
                    <span>Total</span>
                    <span>${total.toFixed(2)}</span>
                  </div>
                </div>
                
                {/* Shipping Info */}
                <div className="bg-blue-50 p-3 rounded-lg">
                  <div className="flex items-center space-x-2 text-sm text-blue-700">
                    <Truck className="w-4 h-4" />
                    <span>
                      {shipping === 0 
                        ? 'Free shipping on orders over $100' 
                        : `Add $${(100 - subtotal).toFixed(2)} for free shipping`
                      }
                    </span>
                  </div>
                </div>
                
                {/* Security Badge */}
                <div className="flex items-center space-x-2 text-sm text-gray-600">
                  <Shield className="w-4 h-4" />
                  <span>Secure checkout with 256-bit SSL</span>
                </div>
                
                {/* Checkout Button */}
                <Button 
                  className="w-full" 
                  size="lg"
                  onClick={handleCheckout}
                  disabled={cartItemsWithGear.some(item => !item.gear?.availability.inStock)}
                >
                  <CreditCard className="w-4 h-4 mr-2" />
                  Proceed to Checkout
                  <ArrowRight className="w-4 h-4 ml-2" />
                </Button>
                
                {/* Payment Methods */}
                <div className="text-center text-xs text-gray-500">
                  We accept all major credit cards and PayPal
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CartPage;