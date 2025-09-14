'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  ArrowLeft,
  Star,
  MapPin,
  Calendar,
  ShoppingCart,
  Heart,
  Share2,
  Shield,
  Truck,
  RotateCcw,
  MessageCircle,
  User,
  ChevronLeft,
  ChevronRight,
  Zap
} from 'lucide-react';
import { shoppingService } from '@/services/shopping';
import { useCartStore } from '@/stores/cartStore';
import RatingReview from '@/components/marketplace/RatingReview';
import { cn } from '@/lib/utils';
import { format, addDays } from 'date-fns';

interface GearDetailPageProps {}

export default function GearDetailPage({}: GearDetailPageProps) {
  const params = useParams();
  const router = useRouter();
  const gearId = params?.id as string;
  const { addItem } = useCartStore();
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  
  const [gear, setGear] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [rentalDays, setRentalDays] = useState(1);
  const [quantity, setQuantity] = useState(1);
  const [isFavorited, setIsFavorited] = useState(false);
  const [relatedGear, setRelatedGear] = useState<any[]>([]);

  useEffect(() => {
    loadGearDetails();
  }, [gearId]);

  const loadGearDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [gearResponse, relatedResponse] = await Promise.all([
        shoppingService.getGearById(gearId),
        shoppingService.getRecommendations(gearId)
      ]);
      
      setGear(gearResponse);
      setRelatedGear(relatedResponse || []);
    } catch (err) {
      console.error('Error loading gear details:', err);
      setError('Failed to load gear details. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = async (type: 'rent' | 'sale') => {
    if (!gear) return;
    
    try {
      await addItem({
        id: `${gear.id}-${type}-${Date.now()}`,
        name: gear.title,
        price: type === 'rent' ? (gear.rentPrice || 0) : gear.price,
        quantity: 1,
        image: gear.images?.[0],
        category: gear.category,
      });
    } catch (error) {
      console.error('Error adding to cart:', error);
    }
  };

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: gear?.name,
          text: gear?.description,
          url: window.location.href
        });
      } catch (error) {
        console.log('Error sharing:', error);
      }
    } else {
      // Fallback: copy to clipboard
      navigator.clipboard.writeText(window.location.href);
    }
  };

  const calculateRentalTotal = () => {
    if (!gear?.rentPrice) return 0;
    return gear.rentPrice * rentalDays * quantity;
  };

  const calculateSaleTotal = () => {
    if (!gear?.price) return 0;
    return gear.price * quantity;
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <div className="aspect-square bg-gray-200 rounded-lg"></div>
            <div className="space-y-4">
              <div className="h-8 bg-gray-200 rounded w-3/4"></div>
              <div className="h-4 bg-gray-200 rounded w-1/2"></div>
              <div className="h-20 bg-gray-200 rounded"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !gear) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Gear Not Found</h2>
          <p className="text-gray-600 mb-6">{error || 'The requested gear item could not be found.'}</p>
          <Button onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Go Back
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Breadcrumb */}
      <div className="flex items-center space-x-2 text-sm text-gray-500 mb-6">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => router.push('/marketplace')}
          className="p-0 h-auto font-normal"
        >
          Marketplace
        </Button>
        <span>/</span>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => router.push(`/marketplace/${gear.category}`)}
          className="p-0 h-auto font-normal"
        >
          {gear.category}
        </Button>
        <span>/</span>
        <span className="text-gray-900">{gear.name}</span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-12">
        {/* Image Gallery */}
        <div className="space-y-4">
          <div className="relative aspect-square rounded-lg overflow-hidden bg-gray-100">
            <img
              src={gear.images?.[selectedImageIndex] || '/placeholder-gear.jpg'}
              alt={gear.name}
              className="w-full h-full object-cover"
            />
            
            {gear.images && gear.images.length > 1 && (
              <>
                <Button
                  variant="outline"
                  size="sm"
                  className="absolute left-2 top-1/2 transform -translate-y-1/2 bg-white/80 backdrop-blur-sm"
                  onClick={() => setSelectedImageIndex(prev => 
                    prev === 0 ? gear.images.length - 1 : prev - 1
                  )}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="absolute right-2 top-1/2 transform -translate-y-1/2 bg-white/80 backdrop-blur-sm"
                  onClick={() => setSelectedImageIndex(prev => 
                    prev === gear.images.length - 1 ? 0 : prev + 1
                  )}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </>
            )}
          </div>
          
          {gear.images && gear.images.length > 1 && (
            <div className="flex space-x-2 overflow-x-auto">
              {gear.images.map((image: string, index: number) => (
                <button
                  key={index}
                  onClick={() => setSelectedImageIndex(index)}
                  className={cn(
                    "flex-shrink-0 w-16 h-16 rounded-lg overflow-hidden border-2 transition-colors",
                    selectedImageIndex === index ? "border-blue-500" : "border-gray-200"
                  )}
                >
                  <img
                    src={image}
                    alt={`${gear.name} ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Product Details */}
        <div className="space-y-6">
          <div>
            <div className="flex items-start justify-between mb-2">
              <h1 className="text-3xl font-bold text-gray-900">{gear.name}</h1>
              <div className="flex space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsFavorited(!isFavorited)}
                >
                  <Heart className={cn(
                    "h-4 w-4",
                    isFavorited ? "fill-red-500 text-red-500" : "text-gray-500"
                  )} />
                </Button>
                <Button variant="outline" size="sm" onClick={handleShare}>
                  <Share2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
            
            <div className="flex items-center space-x-4 mb-4">
              <div className="flex items-center space-x-1">
                <Star className="h-5 w-5 fill-yellow-400 text-yellow-400" />
                <span className="font-medium">{gear.rating}</span>
                <span className="text-gray-500">({gear.reviewCount} reviews)</span>
              </div>
              <Badge variant="secondary">{gear.condition}</Badge>
              {gear.isVerified && (
                <Badge variant="default" className="bg-green-100 text-green-800">
                  <Shield className="h-3 w-3 mr-1" />
                  Verified
                </Badge>
              )}
            </div>
            
            <div className="flex items-center text-gray-600 mb-4">
              <MapPin className="h-4 w-4 mr-1" />
              <span>{gear.location}</span>
            </div>
            
            <p className="text-gray-700 leading-relaxed">{gear.description}</p>
          </div>

          {/* Pricing and Actions */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Pricing & Availability</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {gear.availableForRent && (
                <div className="p-4 border rounded-lg">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <span className="text-2xl font-bold text-green-600">${gear.rentPrice}</span>
                      <span className="text-gray-500">/day</span>
                    </div>
                    <Badge variant="outline" className="text-green-600">
                      <Calendar className="h-3 w-3 mr-1" />
                      Available for Rent
                    </Badge>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-3 mb-3">
                    <div>
                      <Label htmlFor="rental-days">Rental Days</Label>
                      <Input
                        id="rental-days"
                        type="number"
                        min="1"
                        value={rentalDays}
                        onChange={(e) => setRentalDays(Math.max(1, parseInt(e.target.value) || 1))}
                      />
                    </div>
                    <div>
                      <Label htmlFor="rental-quantity">Quantity</Label>
                      <Input
                        id="rental-quantity"
                        type="number"
                        min="1"
                        max={gear.availableQuantity}
                        value={quantity}
                        onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                      />
                    </div>
                  </div>
                  
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-medium">Total: ${calculateRentalTotal()}</span>
                    <span className="text-sm text-gray-500">
                      Return by {format(addDays(new Date(), rentalDays), 'MMM dd, yyyy')}
                    </span>
                  </div>
                  
                  <Button
                    className="w-full"
                    onClick={() => handleAddToCart('rent')}
                    disabled={isAddingToCart}
                  >
                    <Calendar className="h-4 w-4 mr-2" />
                    Add to Cart - Rent
                  </Button>
                </div>
              )}
              
              {gear.availableForSale && (
                <div className="p-4 border rounded-lg">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <span className="text-2xl font-bold text-blue-600">${gear.price}</span>
                    </div>
                    <Badge variant="outline" className="text-blue-600">
                      <ShoppingCart className="h-3 w-3 mr-1" />
                      Available for Purchase
                    </Badge>
                  </div>
                  
                  <div className="mb-3">
                    <Label htmlFor="purchase-quantity">Quantity</Label>
                    <Input
                      id="purchase-quantity"
                      type="number"
                      min="1"
                      max={gear.availableQuantity}
                      value={quantity}
                      onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                    />
                  </div>
                  
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-medium">Total: ${calculateSaleTotal()}</span>
                    <span className="text-sm text-gray-500">
                      {gear.availableQuantity} available
                    </span>
                  </div>
                  
                  <Button
                    className="w-full"
                    onClick={() => handleAddToCart('sale')}
                    disabled={isAddingToCart}
                  >
                    <ShoppingCart className="h-4 w-4 mr-2" />
                    Add to Cart - Buy
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Owner Info */}
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center">
                  <User className="h-5 w-5 text-gray-600" />
                </div>
                <div className="flex-1">
                  <p className="font-medium">{gear.owner?.name || 'Gear Owner'}</p>
                  <p className="text-sm text-gray-500">Member since {gear.owner?.memberSince || '2023'}</p>
                </div>
                <Button variant="outline" size="sm">
                  <MessageCircle className="h-4 w-4 mr-2" />
                  Contact
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Tabs Section */}
      <Tabs defaultValue="details" className="mb-12">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="details">Details</TabsTrigger>
          <TabsTrigger value="reviews">Reviews</TabsTrigger>
          <TabsTrigger value="policies">Policies</TabsTrigger>
          <TabsTrigger value="location">Location</TabsTrigger>
        </TabsList>
        
        <TabsContent value="details" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Product Details</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <h4 className="font-medium mb-3">Specifications</h4>
                  <dl className="space-y-2">
                    <div className="flex justify-between">
                      <dt className="text-gray-600">Brand:</dt>
                      <dd className="font-medium">{gear.brand || 'N/A'}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-gray-600">Model:</dt>
                      <dd className="font-medium">{gear.model || 'N/A'}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-gray-600">Weight:</dt>
                      <dd className="font-medium">{gear.weight || 'N/A'}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-gray-600">Dimensions:</dt>
                      <dd className="font-medium">{gear.dimensions || 'N/A'}</dd>
                    </div>
                  </dl>
                </div>
                
                <div>
                  <h4 className="font-medium mb-3">Features</h4>
                  <ul className="space-y-1">
                    {gear.features?.map((feature: string, index: number) => (
                      <li key={index} className="flex items-center text-sm">
                        <Zap className="h-3 w-3 text-green-500 mr-2" />
                        {feature}
                      </li>
                    )) || (
                      <li className="text-gray-500 text-sm">No features listed</li>
                    )}
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="reviews" className="mt-6">
          <RatingReview
            gearId={gear.id}
            gear={gear}
            canReview={true}
            showReviews={true}
          />
        </TabsContent>
        
        <TabsContent value="policies" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Rental & Purchase Policies</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="text-center">
                  <Truck className="h-8 w-8 text-blue-500 mx-auto mb-2" />
                  <h4 className="font-medium mb-1">Free Delivery</h4>
                  <p className="text-sm text-gray-600">Within 10km radius</p>
                </div>
                <div className="text-center">
                  <RotateCcw className="h-8 w-8 text-green-500 mx-auto mb-2" />
                  <h4 className="font-medium mb-1">Easy Returns</h4>
                  <p className="text-sm text-gray-600">7-day return policy</p>
                </div>
                <div className="text-center">
                  <Shield className="h-8 w-8 text-purple-500 mx-auto mb-2" />
                  <h4 className="font-medium mb-1">Damage Protection</h4>
                  <p className="text-sm text-gray-600">Coverage included</p>
                </div>
              </div>
              
              <div className="prose prose-sm max-w-none">
                <h4>Terms & Conditions</h4>
                <ul>
                  <li>All rentals must be returned in the same condition</li>
                  <li>Late returns incur additional daily charges</li>
                  <li>Damage assessment will be conducted upon return</li>
                  <li>Security deposit may be required for high-value items</li>
                  <li>Cancellations must be made 24 hours in advance</li>
                </ul>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="location" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Pickup Location</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="aspect-video bg-gray-100 rounded-lg flex items-center justify-center mb-4">
                <div className="text-center">
                  <MapPin className="h-12 w-12 text-gray-400 mx-auto mb-2" />
                  <p className="text-gray-600">Map integration coming soon</p>
                </div>
              </div>
              <div className="space-y-2">
                <p className="font-medium">{gear.location}</p>
                <p className="text-sm text-gray-600">
                  Pickup available during business hours (9 AM - 6 PM)
                </p>
                <p className="text-sm text-gray-600">
                  Delivery available within 10km radius for additional fee
                </p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Related Items */}
      {relatedGear.length > 0 && (
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Related Items</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {relatedGear.slice(0, 4).map((item) => (
              <Card key={item.id} className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
                    onClick={() => router.push(`/marketplace/gear/${item.id}`)}>
                <div className="aspect-square">
                  <img
                    src={item.images?.[0] || '/placeholder-gear.jpg'}
                    alt={item.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <CardContent className="p-4">
                  <h3 className="font-semibold text-gray-900 line-clamp-2 mb-2">{item.name}</h3>
                  <div className="flex items-center gap-1 mb-2">
                    <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                    <span className="text-sm font-medium">{item.rating}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    {item.availableForRent && (
                      <span className="text-sm font-semibold text-green-600">
                        ${item.rentPrice}/day
                      </span>
                    )}
                    {item.availableForSale && (
                      <span className="text-sm font-semibold text-blue-600">
                        ${item.price}
                      </span>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}