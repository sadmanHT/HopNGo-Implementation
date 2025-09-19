'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { LoadingSpinner } from '@/components/ui/loading-spinner';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ChevronLeft, ChevronRight, MapPin, Star, Users, Heart } from 'lucide-react';
import Link from 'next/link';
import recommendationService, { RecommendedItem, RecommendedUser } from '@/services/recommendations';
import { useFeatureFlag } from '@/lib/flags';
import { bdDestinations, getBDDestinationsByRegion } from '@/data/bd-destinations';
import { bdFestivals, getFestivalsByRegion } from '@/data/festivals';
import { TouristHeatmap } from '@/components/social/TouristHeatmap';
import SafetyNotesCard from '@/components/safety/SafetyNotesCard';
import { WeatherAdvisoryCard } from '@/components/weather/WeatherAdvisoryCard';
import { useTranslation } from '@/lib/i18n/client';
import React from 'react';



interface CarouselProps {
  title: string;
  items: (RecommendedItem | RecommendedUser)[];
  type: 'items' | 'users';
  loading?: boolean;
}

function Carousel({ title, items, type, loading }: CarouselProps) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const itemsPerView = 3;
  const maxIndex = Math.max(0, items.length - itemsPerView);

  const nextSlide = () => {
    setCurrentIndex(prev => Math.min(prev + 1, maxIndex));
  };

  const prevSlide = () => {
    setCurrentIndex(prev => Math.max(prev - 1, 0));
  };

  if (loading) {
    return (
      <div className="mb-8">
        <h2 className="text-2xl font-bold mb-4">{title}</h2>
        <div className="flex justify-center py-8">
          <LoadingSpinner />
        </div>
      </div>
    );
  }

  return (
    <div className="mb-8">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-2xl font-bold">{title}</h2>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={prevSlide}
            disabled={currentIndex === 0}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={nextSlide}
            disabled={currentIndex >= maxIndex}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
      
      <div className="overflow-hidden">
        <div 
          className="flex transition-transform duration-300 ease-in-out"
          style={{ transform: `translateX(-${currentIndex * (100 / itemsPerView)}%)` }}
        >
          {items.map((item, index) => (
            <div key={item.id} className="flex-shrink-0 w-1/3 px-2">
              {type === 'items' ? (
                <ItemCard item={item as RecommendedItem} index={index} />
              ) : (
                <UserCard user={item as RecommendedUser} index={index} />
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function ItemCard({ item, index }: { item: RecommendedItem; index?: number }) {
  React.useEffect(() => {
    if (index !== undefined) {
      recommendationService.trackImpression({
        recommendationId: item.id,
        algorithm: 'personalized_recommendations',
        position: index,
        context: 'discover'
      });
    }
  }, [item.id, index]);

  const handleClick = () => {
    if (index !== undefined) {
      recommendationService.trackClick({
        recommendationId: item.id,
        algorithm: 'personalized_recommendations',
        position: index,
        context: 'discover'
      });
    }
  };

  return (
    <Link href={`/places/${item.id}`}>
      <Card className="hover:shadow-lg transition-shadow cursor-pointer" onClick={handleClick}>
        <div className="aspect-video relative overflow-hidden rounded-t-lg">
          <img 
            src={item.imageUrl} 
            alt={item.title}
            className="w-full h-full object-cover"
          />
          <Badge className="absolute top-2 right-2 capitalize">
            {item.type}
          </Badge>
        </div>
        <CardContent className="p-4">
          <h3 className="font-semibold text-lg mb-2 line-clamp-1">{item.title}</h3>
          <p className="text-gray-600 text-sm mb-3 line-clamp-2">{item.description}</p>
          
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-1 text-sm text-gray-500">
              {item.location && (
                <>
                  <MapPin className="h-3 w-3" />
                  <span className="line-clamp-1">{item.location}</span>
                </>
              )}
            </div>
            {item.rating && (
              <div className="flex items-center gap-1">
                <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                <span className="text-sm font-medium">{item.rating}</span>
              </div>
            )}
          </div>
          
          {item.price && (
            <div className="mt-2 text-lg font-bold text-blue-600">
              ${item.price}/night
            </div>
          )}
        </CardContent>
      </Card>
    </Link>
  );
}

function UserCard({ user, index }: { user: RecommendedUser; index?: number }) {
  const [isFollowing, setIsFollowing] = useState(false);

  React.useEffect(() => {
    if (index !== undefined) {
      recommendationService.trackImpression({
        recommendationId: user.id,
        algorithm: 'user_recommendations',
        position: index,
        context: 'discover'
      });
    }
  }, [user.id, index]);

  const handleFollow = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsFollowing(!isFollowing);
    if (index !== undefined) {
      recommendationService.trackClick({
        recommendationId: user.id,
        algorithm: 'user_recommendations',
        position: index,
        context: 'discover'
      });
    }
    // TODO: Implement actual follow/unfollow API call
  };

  return (
    <Card className="hover:shadow-lg transition-shadow">
      <CardContent className="p-4 text-center">
        <Avatar className="w-16 h-16 mx-auto mb-3">
          <AvatarImage src={user.avatarUrl} alt={user.displayName} />
          <AvatarFallback>{user.displayName.charAt(0)}</AvatarFallback>
        </Avatar>
        
        <div className="flex items-center justify-center gap-1 mb-1">
          <h3 className="font-semibold">{user.displayName}</h3>
          {user.isVerified && (
            <Badge variant="secondary" className="text-xs">✓</Badge>
          )}
        </div>
        
        <p className="text-gray-500 text-sm mb-2">@{user.username}</p>
        
        {user.bio && (
          <p className="text-gray-600 text-xs mb-3 line-clamp-2">{user.bio}</p>
        )}
        
        <div className="flex items-center justify-center gap-1 mb-3 text-sm text-gray-500">
          <Users className="h-3 w-3" />
          <span>{user.followersCount} followers</span>
        </div>
        
        <Button 
          size="sm" 
          variant={isFollowing ? "outline" : "default"}
          onClick={handleFollow}
          className="w-full"
        >
          {isFollowing ? (
            <>
              <Heart className="h-3 w-3 mr-1 fill-current" />
              Following
            </>
          ) : (
            'Follow'
          )}
        </Button>
      </CardContent>
    </Card>
  );
}

// Bangladesh Destination Card Component
function BDDestinationCard({ destination }: { destination: typeof bdDestinations[0] }) {
  return (
    <Link href={`/places/bd/${destination.slug}`}>
      <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
        <div className="aspect-video relative overflow-hidden rounded-t-lg">
          <img 
            src={destination.imageUrls[0] || '/api/placeholder/300/200'} 
            alt={destination.name_en}
            className="w-full h-full object-cover"
          />
          <Badge className="absolute top-2 right-2 capitalize">
            {destination.best_season}
          </Badge>
          <div className="absolute bottom-2 left-2 bg-black/50 text-white px-2 py-1 rounded text-xs">
            {destination.district}
          </div>
        </div>
        <CardContent className="p-4">
          <h3 className="font-semibold text-lg mb-1 line-clamp-1">{destination.name_en}</h3>
          <p className="text-sm text-gray-500 mb-2">{destination.name_bn}</p>
          <p className="text-gray-600 text-sm mb-3 line-clamp-2">
            {destination.highlights.slice(0, 2).join(', ')}
          </p>
          
          <div className="flex justify-between items-center text-sm">
            <div className="flex items-center gap-1 text-gray-500">
              <MapPin className="h-4 w-4" />
              <span className="line-clamp-1">{destination.region}</span>
            </div>
            <div className="text-blue-600 font-medium">
              ${destination.typical_budget_range.min}-{destination.typical_budget_range.max}
            </div>
          </div>
          
          <SafetyNotesCard 
            destination={destination}
            compact={true}
            className="mt-2"
          />
        </CardContent>
      </Card>
    </Link>
  );
}

// Bangladesh Festival Card Component
function BDFestivalCard({ festival }: { festival: typeof bdFestivals[0] }) {
  return (
    <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
      <CardContent className="p-4">
        <div className="mb-3">
          <h3 className="font-semibold text-lg mb-1">{festival.name_en}</h3>
          <p className="text-sm text-gray-500 mb-2">{festival.name_bn}</p>
          <Badge variant="outline" className="text-xs">
            {festival.dates.months.join(', ')}
          </Badge>
        </div>
        
        <p className="text-gray-600 text-sm mb-3 line-clamp-2">
          {festival.description}
        </p>
        
        <div className="space-y-2 text-xs">
          <div className="flex items-center gap-1 text-gray-500">
            <MapPin className="h-3 w-3" />
            <span>{festival.best_destinations.slice(0, 2).join(', ')}</span>
          </div>
          
          <div className="flex flex-wrap gap-1">
            {festival.activities.slice(0, 3).map((activity, index) => (
              <Badge key={index} variant="secondary" className="text-xs px-1 py-0">
                {activity}
              </Badge>
            ))}
          </div>
          
          <div className="text-amber-600 bg-amber-50 px-2 py-1 rounded">
            💡 {festival.travel_tips[0]}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// Bangladesh administrative divisions - will be localized in component
const bdRegionIds = [
  'all',
  'chittagong', 
  'sylhet',
  'khulna',
  'dhaka',
  'rajshahi',
  'barisal',
  'rangpur',
  'mymensingh'
];

export default function DiscoverPage() {
  const { t } = useTranslation();
  const { isEnabled: recsEnabled } = useFeatureFlag('recs_v1');
  const [forYouItems, setForYouItems] = useState<RecommendedItem[]>([]);
  const [travelBuddies, setTravelBuddies] = useState<RecommendedUser[]>([]);
  const [trendingItems, setTrendingItems] = useState<RecommendedItem[]>([]);
  const [selectedRegion, setSelectedRegion] = useState('all');
  const [filteredDestinations, setFilteredDestinations] = useState(bdDestinations);
  const [filteredFestivals, setFilteredFestivals] = useState(bdFestivals);
  const [loading, setLoading] = useState({
    forYou: true,
    buddies: true,
    trending: true
  });

  // Create localized regions array
  const bdRegions = bdRegionIds.map(id => ({
    id,
    name: id === 'all' ? t('discover.filters.all') : t(`discover.regions.${id}`)
  }));

  // Filter destinations and festivals by region
  useEffect(() => {
    if (selectedRegion === 'all') {
      setFilteredDestinations(bdDestinations);
      setFilteredFestivals(bdFestivals);
    } else {
      const regionDestinations = getBDDestinationsByRegion(selectedRegion);
      const regionFestivals = getFestivalsByRegion(selectedRegion);
      setFilteredDestinations(regionDestinations);
      setFilteredFestivals(regionFestivals);
    }
  }, [selectedRegion]);

  useEffect(() => {
    if (!recsEnabled) {
      setLoading({ forYou: false, buddies: false, trending: false });
      setForYouItems([]);
      setTravelBuddies([]);
      setTrendingItems([]);
      return;
    }

    // Fetch "For you" recommendations
    const fetchForYou = async () => {
      try {
        const data = await recommendationService.getHomeRecommendations({ limit: 6 });
        setForYouItems(data.recommendations || []);
      } catch (error) {
        console.error('Failed to fetch for you recommendations:', error);
        // Fallback to mock data
        setForYouItems(mockItems);
      } finally {
        setLoading(prev => ({ ...prev, forYou: false }));
      }
    };

    // Fetch travel buddies recommendations
    const fetchTravelBuddies = async () => {
      try {
        const data = await recommendationService.getUserRecommendations({ limit: 6 });
        setTravelBuddies(data.recommendations || []);
      } catch (error) {
        console.error('Failed to fetch travel buddies:', error);
        // Fallback to mock data
        setTravelBuddies(mockUsers);
      } finally {
        setLoading(prev => ({ ...prev, buddies: false }));
      }
    };

    // Fetch trending near you
    const fetchTrending = async () => {
      try {
        const data = await recommendationService.getTrendingNearby({ limit: 6 });
        setTrendingItems(data.recommendations || []);
      } catch (error) {
        console.error('Failed to fetch trending items:', error);
        // Fallback to mock data
        setTrendingItems(mockTrendingItems);
      } finally {
        setLoading(prev => ({ ...prev, trending: false }));
      }
    };

    fetchForYou();
    fetchTravelBuddies();
    fetchTrending();
  }, [recsEnabled]);

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">{t('discover.title')}</h1>
        <p className="text-gray-600">{t('discover.subtitle')}</p>
      </div>

      {/* Region Tabs */}
      <Tabs value={selectedRegion} onValueChange={setSelectedRegion} className="mb-8">
        <TabsList className="grid w-full grid-cols-3 lg:grid-cols-9 mb-6">
          {bdRegions.map((region) => (
            <TabsTrigger key={region.id} value={region.id} className="text-xs lg:text-sm">
              <span className="hidden lg:inline">{region.name}</span>
              <span className="lg:hidden">{region.name.split(' ')[0]}</span>
            </TabsTrigger>
          ))}
        </TabsList>

        {bdRegions.map((region) => (
          <TabsContent key={region.id} value={region.id} className="space-y-8">
            {/* Weather & Safety Advisory */}
            <div>
              <WeatherAdvisoryCard 
                coordinates={{ lat: 23.6850, lng: 90.3563 }} // Dhaka coordinates as regional center
                className="mb-4"
                showForecast={true}
              />
            </div>

            {/* Tourist Activity Heatmap */}
            <div>
              <h2 className="text-2xl font-bold mb-4">
                {t('discover.heatmap.title', { region: region.name })}
              </h2>
              <TouristHeatmap 
                selectedRegion={region.id}
                height="400px"
                showControls={false}
                className="mb-6"
              />
            </div>

            {/* Curated Destinations */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold">
                  {t('discover.destinations.title')}
                </h2>
                <Badge variant="outline">
                  {filteredDestinations.length} {t('discover.destinations.title').toLowerCase()}
                </Badge>
              </div>
              
              {filteredDestinations.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {filteredDestinations.slice(0, 6).map((destination) => (
                    <BDDestinationCard key={destination.slug} destination={destination} />
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <MapPin className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>{t('discover.destinations.noDestinations')}</p>
                </div>
              )}
            </div>

            {/* Cultural Festivals */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold">
                  {t('discover.festivals.title')}
                </h2>
                <Badge variant="outline">
                  {filteredFestivals.length} festivals
                </Badge>
              </div>
              
              {filteredFestivals.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {filteredFestivals.slice(0, 6).map((festival) => (
                    <BDFestivalCard key={festival.slug} festival={festival} />
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <Star className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>{t('discover.festivals.noFestivals')}</p>
                </div>
              )}
            </div>
          </TabsContent>
        ))}
      </Tabs>

      {/* Personalized Recommendations (if enabled) */}
      {recsEnabled && (
        <div className="border-t pt-8">
          <h2 className="text-2xl font-bold mb-6">{t('discover.personalized.title')}</h2>
          
          <Carousel 
            title={t('discover.personalized.recommendedForYou')} 
            items={forYouItems} 
            type="items" 
            loading={loading.forYou}
          />
          
          <Carousel 
            title={t('discover.personalized.travelBuddies')} 
            items={travelBuddies} 
            type="users" 
            loading={loading.buddies}
          />
          
          <Carousel 
            title={t('discover.personalized.trendingNearYou')} 
            items={trendingItems} 
            type="items" 
            loading={loading.trending}
          />
        </div>
      )}
    </div>
  );
}

// Mock data for fallback
const mockItems: RecommendedItem[] = [
  {
    id: '1',
    title: 'Cozy Mountain Cabin',
    description: 'Perfect retreat in the mountains with stunning views',
    imageUrl: '/api/placeholder/300/200',
    price: 120,
    rating: 4.8,
    location: 'Aspen, CO',
    type: 'stay'
  },
  {
    id: '2',
    title: 'City Food Tour',
    description: 'Explore local cuisine with expert guides',
    imageUrl: '/api/placeholder/300/200',
    price: 85,
    rating: 4.9,
    location: 'Portland, OR',
    type: 'tour'
  },
  {
    id: '3',
    title: 'Sunset Sailing',
    description: 'Romantic sailing experience at golden hour',
    imageUrl: '/api/placeholder/300/200',
    price: 150,
    rating: 4.7,
    location: 'San Diego, CA',
    type: 'experience'
  }
];

const mockUsers: RecommendedUser[] = [
  {
    id: '1',
    username: 'wanderlust_sarah',
    displayName: 'Sarah Johnson',
    avatarUrl: '/api/placeholder/64/64',
    bio: 'Adventure seeker and photography enthusiast',
    followersCount: 1250,
    isVerified: true
  },
  {
    id: '2',
    username: 'mountain_mike',
    displayName: 'Mike Chen',
    avatarUrl: '/api/placeholder/64/64',
    bio: 'Hiking trails and hidden gems explorer',
    followersCount: 890,
    isVerified: false
  },
  {
    id: '3',
    username: 'foodie_travels',
    displayName: 'Emma Rodriguez',
    avatarUrl: '/api/placeholder/64/64',
    bio: 'Culinary adventures around the world',
    followersCount: 2100,
    isVerified: true
  }
];

const mockTrendingItems: RecommendedItem[] = [
  {
    id: '4',
    title: 'Beach House Getaway',
    description: 'Oceanfront property with private beach access',
    imageUrl: '/api/placeholder/300/200',
    price: 200,
    rating: 4.9,
    location: 'Malibu, CA',
    type: 'stay'
  },
  {
    id: '5',
    title: 'Wine Tasting Tour',
    description: 'Premium vineyard experience with tastings',
    imageUrl: '/api/placeholder/300/200',
    price: 95,
    rating: 4.6,
    location: 'Napa Valley, CA',
    type: 'tour'
  },
  {
    id: '6',
    title: 'Hot Air Balloon Ride',
    description: 'Breathtaking aerial views of the countryside',
    imageUrl: '/api/placeholder/300/200',
    price: 180,
    rating: 4.8,
    location: 'Sonoma, CA',
    type: 'experience'
  }
];