'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
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

interface VendorResponse {
  id: string;
  content: string;
  createdAt: string;
}

export default function ListingDetailsPage() {
  const params = useParams();
  const { addToast } = useToast();
  const [listing, setListing] = useState<Listing | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    const fetchListing = async () => {
      try {
        setIsLoading(true);
        // Fetch listing details
        const listingResponse = await fetch(`/api/bookings/${params.id}`);
        if (listingResponse.ok) {
          const listingData = await listingResponse.json();
          setListing(listingData);
        }

        // Fetch reviews
        const reviewsResponse = await fetch(`/api/bookings/${params.id}/reviews`);
        if (reviewsResponse.ok) {
          const reviewsData = await reviewsResponse.json();
          setReviews(reviewsData);
        }
      } catch (error) {
        console.error('Error fetching listing:', error);
        addToast('Failed to load listing details', 'error');
      } finally {
        setIsLoading(false);
      }
    };

    if (params.id) {
      fetchListing();
    }
  }, [params.id, addToast]);

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
    return (sum / reviews.length).toFixed(1);
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

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </div>
    );
  }

  if (!listing) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="text-center py-12">
          <h2 className="text-xl font-semibold mb-2">Listing not found</h2>
          <p className="text-muted-foreground mb-4">The listing you're looking for doesn't exist.</p>
          <Link href="/search">
            <Button>Back to Search</Button>
          </Link>
        </div>
      </div>
    );
  }

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
                    {renderStars(Math.round(parseFloat(calculateAverageRating())))}
                  </div>
                  <span>{calculateAverageRating()} ({reviews.length} reviews)</span>
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
                    <div className="absolute bottom-2 right-2 bg-black/50 text-white text-xs px-2 py-1 rounded">
                      {currentImageIndex + 1} / {listing.images.length}
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
              <TabsTrigger value="reviews">Reviews ({reviews.length})</TabsTrigger>
              <TabsTrigger value="location">Location</TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="space-y-6">
              {/* Description */}
              <Card>
                <CardHeader>
                  <h3 className="text-lg font-semibold">Description</h3>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground leading-relaxed">{listing.description}</p>
                </CardContent>
              </Card>

              {/* Tags */}
              {listing.tags && listing.tags.length > 0 && (
                <Card>
                  <CardHeader>
                    <h3 className="text-lg font-semibold">Tags</h3>
                  </CardHeader>
                  <CardContent>
                    <div className="flex flex-wrap gap-2">
                      {listing.tags.map((tag, index) => (
                        <Badge key={index} variant="secondary">{tag}</Badge>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Amenities */}
              {listing.amenities && listing.amenities.length > 0 && (
                <Card>
                  <CardHeader>
                    <h3 className="text-lg font-semibold">Amenities</h3>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-2 gap-2">
                      {listing.amenities.map((amenity, index) => (
                        <div key={index} className="flex items-center space-x-2">
                          <div className="w-2 h-2 bg-green-500 rounded-full" />
                          <span className="text-sm">{amenity}</span>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}
            </TabsContent>

            <TabsContent value="reviews" className="space-y-4">
              {reviews.length === 0 ? (
                <Card>
                  <CardContent className="text-center py-8">
                    <MessageSquare className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                    <h3 className="text-lg font-medium mb-2">No reviews yet</h3>
                    <p className="text-muted-foreground">Be the first to leave a review!</p>
                  </CardContent>
                </Card>
              ) : (
                <div className="space-y-4">
                  {reviews.map((review) => (
                    <div key={review.id} className="space-y-4">
                      <ReviewCard review={review} />
                      {review.vendorResponse && (
                        <Card className="ml-8 border-l-4 border-l-blue-500">
                          <CardHeader className="pb-3">
                            <div className="flex items-center space-x-3">
                              <Avatar className="h-8 w-8">
                                <AvatarImage src={listing.author.avatar} />
                                <AvatarFallback>
                                  {listing.author.name.charAt(0).toUpperCase()}
                                </AvatarFallback>
                              </Avatar>
                              <div>
                                <div className="flex items-center space-x-2">
                                  <h4 className="font-semibold text-sm">{listing.author.name}</h4>
                                  <VerifiedBadge isVerified={listing.author.isVerified} size="sm" variant="minimal" />
                                  <Badge variant="outline" className="text-xs">Host Response</Badge>
                                </div>
                                <p className="text-xs text-muted-foreground">
                                  {formatDate(review.vendorResponse.createdAt)}
                                </p>
                              </div>
                            </div>
                          </CardHeader>
                          <CardContent className="pt-0">
                            <p className="text-sm leading-relaxed">{review.vendorResponse.content}</p>
                          </CardContent>
                        </Card>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </TabsContent>

            <TabsContent value="location">
              <Card>
                <CardHeader>
                  <h3 className="text-lg font-semibold">Location</h3>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex items-center space-x-2">
                      <MapPin className="h-5 w-5 text-muted-foreground" />
                      <div>
                        <p className="font-medium">{listing.location.city}, {listing.location.country}</p>
                        {listing.location.address && (
                          <p className="text-sm text-muted-foreground">{listing.location.address}</p>
                        )}
                      </div>
                    </div>
                    <div className="bg-gray-100 rounded-lg h-64 flex items-center justify-center">
                      <p className="text-muted-foreground">Map integration coming soon</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Pricing Card */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <DollarSign className="h-5 w-5 text-green-600" />
                  <span className="text-2xl font-bold text-green-600">
                    {formatPrice(listing.price, listing.currency)}
                  </span>
                  <span className="text-sm text-muted-foreground">/ night</span>
                </div>
                <Badge className={listing.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}>
                  {listing.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <Button className="w-full" size="lg" onClick={handleContact}>
                Contact Host
              </Button>
              <Button variant="outline" className="w-full" onClick={handleFavorite}>
                <Heart className="h-4 w-4 mr-2" />
                Add to Wishlist
              </Button>
            </CardContent>
          </Card>

          {/* Host Info */}
          <Card>
            <CardHeader>
              <h3 className="text-lg font-semibold">Hosted by</h3>
            </CardHeader>
            <CardContent>
              <div className="flex items-center space-x-3">
                <Avatar className="h-12 w-12">
                  <AvatarImage src={listing.author.avatar} />
                  <AvatarFallback>
                    {listing.author.name.charAt(0).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <div className="flex items-center space-x-2">
                    <h4 className="font-semibold">{listing.author.name}</h4>
                    <VerifiedBadge isVerified={listing.author.isVerified} size="sm" variant="default" />
                  </div>
                  <p className="text-sm text-muted-foreground">Host since {formatDate(listing.createdAt)}</p>
                </div>
              </div>
              <Separator className="my-4" />
              <Button variant="outline" className="w-full">
                <MessageSquare className="h-4 w-4 mr-2" />
                Message Host
              </Button>
            </CardContent>
          </Card>

          {/* Category */}
          <Card>
            <CardHeader>
              <h3 className="text-lg font-semibold">Category</h3>
            </CardHeader>
            <CardContent>
              <Badge variant="secondary" className="text-sm">{listing.category}</Badge>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}