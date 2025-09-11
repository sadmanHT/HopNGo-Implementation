'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ReviewCard } from '@/components/ReviewCard';
import { VerifiedBadge } from '@/components/ui/verified-badge';
import {
  MapPin,
  Calendar,
  DollarSign,
  Star,
  MessageSquare,
  Heart,
  Share2,
  Flag,
  Loader2,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import { useToast } from '@/components/ui/toast';
import recommendationService, { RecommendedItem } from '@/services/recommendations';
import { useFeatureFlag } from '@/lib/flags';
import React from 'react';

interface Listing {
  id: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  category: string;
  location: {
    city: string;
    country: string;
    address?: string;
  };
  images: string[];
  author: {
    id: string;
    name: string;
    avatar?: string;
    isVerified?: boolean;
  };
  createdAt: string;
  status: 'ACTIVE' | 'SOLD' | 'INACTIVE';
  tags?: string[];
  amenities?: string[];
}

interface Review {
  id: string;
  content: string;
  rating: number;
  author: {
    id: string;
    name: string;
    avatar?: string;
    isVerified?: boolean;
  };
  createdAt: string;
  vendorResponse?: {
    id: string;
    content: string;
    createdAt: string;
  };
}

interface ListingDetailsClientProps {
  listing: Listing;
  reviews: Review[];
}

export default function ListingDetailsClient({ listing, reviews }: ListingDetailsClientProps) {
  const { addToast } = useToast();
  const { isEnabled: recsEnabled } = useFeatureFlag('recs_v1');
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [activeTab, setActiveTab] = useState('overview');
  const [similarItems, setSimilarItems] = useState<RecommendedItem[]>([]);
  const [similarItemsLoading, setSimilarItemsLoading] = useState(false);

  useEffect(() => {
    const fetchSimilarItems = async () => {
      if (!listing.id || !recsEnabled) {
        setSimilarItems([]);
        return;
      }
      
      try {
        setSimilarItemsLoading(true);
        const data = await recommendationService.getSimilarItems(listing.id, { limit: 4 });
        setSimilarItems(data.recommendations || []);
      } catch (error) {
        console.error('Failed to fetch similar items:', error);
        // Fallback to mock data
        setSimilarItems(mockSimilarItems);
      } finally {
        setSimilarItemsLoading(false);
      }
    };

    fetchSimilarItems();
  }, [listing.id, recsEnabled]);

  const formatPrice = (price: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(price);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const calculateAverageRating = () => {
    if (reviews.length === 0) return 0;
    const sum = reviews.reduce((acc, review) => acc + review.rating, 0);
    return sum / reviews.length;
  };

  const renderStars = (rating: number) => {
    return Array.from({ length: 5 }, (_, index) => (
      <Star
        key={index}
        className={`h-4 w-4 ${
          index < rating
            ? 'fill-yellow-400 text-yellow-400'
            : 'text-gray-300'
        }`}
      />
    ));
  };

  const nextImage = () => {
    if (listing?.images) {
      setCurrentImageIndex((prev) => 
        prev === listing.images.length - 1 ? 0 : prev + 1
      );
    }
  };

  const prevImage = () => {
    if (listing?.images) {
      setCurrentImageIndex((prev) => 
        prev === 0 ? listing.images.length - 1 : prev - 1
      );
    }
  };

  const handleContact = () => {
    addToast('Contact feature coming soon!', 'info');
  };

  const handleFavorite = () => {
    addToast('Added to favorites!', 'success');
  };

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    addToast('Link copied to clipboard!', 'success');
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      {/* Header */}
      <div className="mb-6">
        <Link href="/search" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-4">
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Search
        </Link>
        
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-3xl font-bold mb-2">{listing.title}</h1>
            <div className="flex items-center space-x-4 text-sm text-muted-foreground">
              <div className="flex items-center space-x-1">
                <MapPin className="h-4 w-4" />
                <span>{listing.location.city}, {listing.location.country}</span>
              </div>
              <div className="flex items-center space-x-1">
                <Calendar className="h-4 w-4" />
                <span>Listed {formatDate(listing.createdAt)}</span>
              </div>
              {reviews.length > 0 && (
                <div className="flex items-center space-x-1">
                  <div className="flex items-center space-x-1">
                    {renderStars(Math.round(calculateAverageRating()))}
                  </div>
                  <span>{calculateAverageRating().toFixed(1)} ({reviews.length} reviews)</span>
                </div>
              )}
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            <Button variant="outline" size="sm" onClick={handleShare}>
              <Share2 className="h-4 w-4 mr-2" />
              Share
            </Button>
            <Button variant="outline" size="sm" onClick={handleFavorite}>
              <Heart className="h-4 w-4 mr-2" />
              Save
            </Button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Image Gallery */}
          {listing.images && listing.images.length > 0 && (
            <Card className="overflow-hidden">
              <div className="relative aspect-video">
                <Image
                  src={listing.images[currentImageIndex]}
                  alt={listing.title}
                  fill
                  className="object-cover"
                />
                {listing.images.length > 1 && (
                  <>
                    <Button
                      variant="outline"
                      size="sm"
                      className="absolute left-2 top-1/2 transform -translate-y-1/2 bg-white/80 hover:bg-white"
                      onClick={prevImage}
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      className="absolute right-2 top-1/2 transform -translate-y-1/2 bg-white/80 hover:bg-white"
                      onClick={nextImage}
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                    <div className="absolute bottom-2 left-1/2 transform -translate-x-1/2 flex space-x-1">
                      {listing.images.map((_, index) => (
                        <button
                          key={index}
                          className={`w-2 h-2 rounded-full ${
                            index === currentImageIndex ? 'bg-white' : 'bg-white/50'
                          }`}
                          onClick={() => setCurrentImageIndex(index)}
                        />
                      ))}
                    </div>
                  </>
                )}
              </div>
            </Card>
          )}

          {/* Tabs */}
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="amenities">Amenities</TabsTrigger>
              <TabsTrigger value="reviews">Reviews ({reviews.length})</TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="space-y-4">
              <Card>
                <CardHeader>
                  <h3 className="text-lg font-semibold">Description</h3>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground leading-relaxed">
                    {listing.description}
                  </p>
                </CardContent>
              </Card>

              {listing.tags && listing.tags.length > 0 && (
                <Card>
                  <CardHeader>
                    <h3 className="text-lg font-semibold">Tags</h3>
                  </CardHeader>
                  <CardContent>
                    <div className="flex flex-wrap gap-2">
                      {listing.tags.map((tag, index) => (
                        <Badge key={index} variant="secondary">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}
            </TabsContent>

            <TabsContent value="amenities">
              <Card>
                <CardHeader>
                  <h3 className="text-lg font-semibold">Amenities</h3>
                </CardHeader>
                <CardContent>
                  {listing.amenities && listing.amenities.length > 0 ? (
                    <div className="grid grid-cols-2 gap-2">
                      {listing.amenities.map((amenity, index) => (
                        <div key={index} className="flex items-center space-x-2">
                          <div className="w-2 h-2 bg-green-500 rounded-full" />
                          <span className="text-sm">{amenity}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-muted-foreground">No amenities listed.</p>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="reviews" className="space-y-4">
              {reviews.length > 0 ? (
                reviews.map((review) => (
                  <ReviewCard key={review.id} review={review} />
                ))
              ) : (
                <Card>
                  <CardContent className="py-8 text-center">
                    <MessageSquare className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                    <h3 className="text-lg font-semibold mb-2">No reviews yet</h3>
                    <p className="text-muted-foreground">Be the first to leave a review!</p>
                  </CardContent>
                </Card>
              )}
            </TabsContent>
          </Tabs>

          {/* Similar Items */}
          {recsEnabled && (
            <Card>
              <CardHeader>
                <h3 className="text-lg font-semibold">Similar Items</h3>
              </CardHeader>
              <CardContent>
                {similarItemsLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                  </div>
                ) : similarItems.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {similarItems.map((item, index) => (
                      <SimilarItemCard key={item.id} item={item} index={index} />
                    ))}
                  </div>
                ) : (
                  <p className="text-muted-foreground text-center py-4">No similar items found.</p>
                )}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Booking Card */}
          <Card className="sticky top-4">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-2xl font-bold">
                    {formatPrice(listing.price, listing.currency)}
                  </div>
                  <div className="text-sm text-muted-foreground">per night</div>
                </div>
                <Badge 
                  variant={listing.status === 'ACTIVE' ? 'default' : 'secondary'}
                  className={listing.status === 'ACTIVE' ? 'bg-green-500' : ''}
                >
                  {listing.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <Button className="w-full" size="lg">
                <DollarSign className="h-4 w-4 mr-2" />
                Book Now
              </Button>
              <Button variant="outline" className="w-full" onClick={handleContact}>
                <MessageSquare className="h-4 w-4 mr-2" />
                Contact Host
              </Button>
              <Separator />
              <div className="text-xs text-muted-foreground text-center">
                You won't be charged yet
              </div>
            </CardContent>
          </Card>

          {/* Host Info */}
          <Card>
            <CardHeader>
              <h3 className="text-lg font-semibold">Hosted by</h3>
            </CardHeader>
            <CardContent>
              <div className="flex items-center space-x-3">
                <Avatar>
                  <AvatarImage src={listing.author.avatar} alt={listing.author.name} />
                  <AvatarFallback>
                    {listing.author.name.split(' ').map(n => n[0]).join('')}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-medium">{listing.author.name}</span>
                    {listing.author.isVerified && <VerifiedBadge />}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    Host since {formatDate(listing.createdAt)}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Report */}
          <Card>
            <CardContent className="pt-6">
              <Button variant="ghost" size="sm" className="w-full text-muted-foreground">
                <Flag className="h-4 w-4 mr-2" />
                Report this listing
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

function SimilarItemCard({ item, index }: { item: RecommendedItem; index: number }) {
  return (
    <Link href={`/places/${item.id}`} className="block">
      <Card className="overflow-hidden hover:shadow-md transition-shadow">
        <div className="relative aspect-video">
          <Image
            src={item.imageUrl}
            alt={item.title}
            fill
            className="object-cover"
          />
          <Badge className="absolute top-2 left-2 bg-black/70 text-white">
            {item.type}
          </Badge>
        </div>
        <CardContent className="p-4">
          <h4 className="font-semibold mb-1 line-clamp-1">{item.title}</h4>
          <p className="text-sm text-muted-foreground mb-2 line-clamp-2">
            {item.description}
          </p>
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-1">
              <MapPin className="h-3 w-3 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{item.location}</span>
            </div>
            <div className="flex items-center space-x-1">
              <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
              <span className="text-xs font-medium">{item.rating}</span>
            </div>
          </div>
          <div className="mt-2 font-semibold">
            ${item.price}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

const mockSimilarItems: RecommendedItem[] = [
  {
    id: 'similar-1',
    title: 'Charming Downtown Loft',
    description: 'Modern loft in the heart of the city with great amenities',
    imageUrl: '/api/placeholder/300/200',
    price: 95,
    rating: 4.6,
    location: 'Downtown',
    type: 'stay'
  },
  {
    id: 'similar-2',
    title: 'Cozy Garden Apartment',
    description: 'Peaceful retreat with beautiful garden views',
    imageUrl: '/api/placeholder/300/200',
    price: 110,
    rating: 4.8,
    location: 'Garden District',
    type: 'stay'
  },
  {
    id: 'similar-3',
    title: 'Historic Walking Tour',
    description: 'Explore the rich history of the old town',
    imageUrl: '/api/placeholder/300/200',
    price: 45,
    rating: 4.7,
    location: 'Old Town',
    type: 'tour'
  },
  {
    id: 'similar-4',
    title: 'Sunset Photography Workshop',
    description: 'Learn photography techniques during golden hour',
    imageUrl: '/api/placeholder/300/200',
    price: 75,
    rating: 4.9,
    location: 'Scenic Overlook',
    type: 'experience'
  },
];