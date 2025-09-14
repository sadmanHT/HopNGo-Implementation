'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Badge } from '../../../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../../components/ui/select';
import { Checkbox } from '../../../components/ui/checkbox';
import { Slider } from '../../../components/ui/slider';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/ui/tabs';
import { Avatar, AvatarFallback, AvatarImage } from '../../../components/ui/avatar';
import { Separator } from '../../../components/ui/separator';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import RatingReview from '../../../components/marketplace/RatingReview';
import SearchFilters, { SearchFilters as SearchFiltersType } from '../../../components/marketplace/SearchFilters';
import { 
  Search, 
  Filter, 
  Grid, 
  List, 
  Star, 
  MapPin, 
  Calendar, 
  ShoppingCart, 
  Heart,
  Eye,
  Package,
  Truck,
  Shield,
  ChevronDown,
  X,
  Loader2,
  Plus
} from 'lucide-react';
import { shoppingService, TravelGear, MarketplaceFilters, SearchResponse } from '../../../services/shopping';
import { useCartStore } from '../../../lib/state';
import { useRouter } from 'next/navigation';

interface FilterState extends MarketplaceFilters {
  priceRange: { min: number; max: number };
}

const INITIAL_FILTERS: FilterState = {
  category: [],
  type: undefined,
  priceRange: { min: 0, max: 1000 },
  condition: [],
  location: '',
  availability: true,
  rating: 0,
  tags: [],
  sortBy: 'newest',
  sortOrder: 'desc',
};

const CATEGORIES = [
  { id: 'gear', name: 'Travel Gear', count: 0 },
  { id: 'equipment', name: 'Equipment', count: 0 },
  { id: 'accessories', name: 'Accessories', count: 0 },
];

const CONDITIONS = [
  { id: 'new', name: 'New', count: 0 },
  { id: 'like-new', name: 'Like New', count: 0 },
  { id: 'good', name: 'Good', count: 0 },
  { id: 'fair', name: 'Fair', count: 0 },
];

const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest First' },
  { value: 'price', label: 'Price: Low to High' },
  { value: 'rating', label: 'Highest Rated' },
  { value: 'popular', label: 'Most Popular' },
];

const MarketplacePage = () => {
  const router = useRouter();
  const { addToCart, isAddingItem } = useCartStore();
  
  // State management
  const [gearItems, setGearItems] = useState<TravelGear[]>([]);
  const [filteredItems, setFilteredItems] = useState<TravelGear[]>([]);
  const [searchResponse, setSearchResponse] = useState<SearchResponse<TravelGear> | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedReviews, setExpandedReviews] = useState<{ [key: string]: boolean }>({});
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [searchFilters, setSearchFilters] = useState<SearchFiltersType>({
    query: '',
    category: 'all',
    priceRange: [0, 1000],
    availability: 'all',
    rating: 0,
    location: '',
    features: [],
    sortBy: 'relevance',
  });
  
  // Search and filter function
  const searchGear = useCallback(async (resetPage = false) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const currentPage = resetPage ? 0 : page;
      const response = await shoppingService.searchGear(
        searchFilters.query || undefined,
        {
          category: searchFilters.category !== 'all' ? [searchFilters.category] : [],
          priceRange: { min: searchFilters.priceRange[0], max: searchFilters.priceRange[1] },
          availability: searchFilters.availability === 'available',
          rating: searchFilters.rating,
          location: searchFilters.location,
          tags: searchFilters.features,
          sortBy: searchFilters.sortBy === 'price_low' || searchFilters.sortBy === 'price_high' ? 'price' : 
                  searchFilters.sortBy === 'relevance' ? 'popular' : 
                  searchFilters.sortBy as 'rating' | 'newest' | 'popular',
          sortOrder: 'desc',
        },
        currentPage,
        20
      );
      
      if (resetPage) {
        setGearItems(response.items);
        setPage(0);
      } else {
        setGearItems(prev => [...prev, ...response.items]);
      }
      
      setSearchResponse(response);
      setHasMore(response.hasMore);
      
      // Track search analytics
      if (searchFilters.query) {
        // TODO: Track search query analytics
      }
    } catch (error) {
      console.error('Search failed:', error);
      setError('Failed to load travel gear. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }, [searchFilters, page]);
  
  // Initialize search from URL params
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('q') || '';
    const category = urlParams.get('category') || '';
    
    if (query || category) {
      const newFilters = {
        ...searchFilters,
        query,
        category: category || 'all'
      };
      setSearchFilters(newFilters);
      handleSearch(newFilters);
    } else {
      // Load initial gear without filters
      handleSearch(searchFilters);
    }
  }, []);
  
  // Handle filter changes
  const updateFilter = (key: keyof SearchFiltersType, value: any) => {
    setSearchFilters(prev => ({ ...prev, [key]: value }));
  };
  
  const toggleArrayFilter = (key: 'features', value: string) => {
    setSearchFilters(prev => ({
      ...prev,
      [key]: prev[key]?.includes(value)
        ? prev[key].filter(item => item !== value)
        : [...(prev[key] || []), value]
    }));
  };
  
  const clearFilters = () => {
    setSearchFilters({
      query: '',
      category: 'all',
      priceRange: [0, 1000],
      availability: 'all',
      rating: 0,
      location: '',
      features: [],
      sortBy: 'relevance',
    });
  };
  
  // Handle add to cart
  const handleAddToCart = async (gear: TravelGear, type: 'rent' | 'purchase') => {
    try {
      await addToCart({
        gearId: gear.id,
        quantity: 1,
        type,
        rentDuration: type === 'rent' ? {
          startDate: new Date().toISOString().split('T')[0],
          endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        } : undefined,
      });
      
      // Show success feedback
      // TODO: Add toast notification
    } catch (error) {
      console.error('Failed to add to cart:', error);
      // TODO: Show error toast
    }
  };
  
  const toggleReviews = (gearId: string) => {
    setExpandedReviews(prev => ({
      ...prev,
      [gearId]: !prev[gearId]
    }));
  };

  const handleGearClick = (gearId: string) => {
    router.push(`/marketplace/gear/${gearId}`);
  };
  


  // Handle search with filters
  const handleSearch = async (filters: SearchFiltersType) => {
    setIsSearching(true);
    setSearchFilters(filters);
    try {
      const response = await shoppingService.searchGear(
        filters.query || undefined,
        {
          category: filters.category !== 'all' ? [filters.category] : [],
          priceRange: { min: filters.priceRange[0], max: filters.priceRange[1] },
          availability: filters.availability === 'available',
          rating: filters.rating,
          location: filters.location,
          tags: filters.features,
          sortBy: filters.sortBy === 'price_low' || filters.sortBy === 'price_high' ? 'price' : 
                  filters.sortBy === 'relevance' ? 'popular' : 
                  filters.sortBy as 'rating' | 'newest' | 'popular',
          sortOrder: 'desc',
        },
        0,
        20
      );
      
      setGearItems(response.items);
      setFilteredItems(response.items);
      setSearchResponse(response);
      setHasMore(response.hasMore);
    } catch (error) {
      console.error('Search failed:', error);
      setError('Failed to search travel gear. Please try again.');
    } finally {
      setIsSearching(false);
    }
  };
  
  // Toggle favorite
  const toggleFavorite = (gearId: string) => {
    setFavorites(prev => {
      const newFavorites = new Set(prev);
      if (newFavorites.has(gearId)) {
        newFavorites.delete(gearId);
      } else {
        newFavorites.add(gearId);
      }
      return newFavorites;
    });
  };
  
  // Load more items
  const loadMore = () => {
    if (!isLoading && hasMore) {
      setPage(prev => prev + 1);
      searchGear(false);
    }
  };
  
  // Render gear card
  const renderGearCard = (gear: TravelGear) => (
    <Card 
      key={gear.id} 
      className="group hover:shadow-lg transition-shadow cursor-pointer"
      onClick={() => handleGearClick(gear.id)}
    >
      <div className="relative">
        <div 
          className="aspect-square bg-gray-100 rounded-t-lg overflow-hidden"
        >
          {gear.images.length > 0 ? (
            <img 
              src={gear.images[0]} 
              alt={gear.title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <Package className="w-12 h-12 text-gray-400" />
            </div>
          )}
        </div>
        
        <Button
          variant="ghost"
          size="sm"
          className="absolute top-2 right-2 bg-white/80 hover:bg-white"
          onClick={(e) => {
            e.stopPropagation();
            toggleFavorite(gear.id);
          }}
        >
          <Heart 
            className={`w-4 h-4 ${
              favorites.has(gear.id) ? 'fill-red-500 text-red-500' : 'text-gray-600'
            }`} 
          />
        </Button>
        
        {!gear.availability.inStock && (
          <Badge className="absolute top-2 left-2 bg-red-500">
            Out of Stock
          </Badge>
        )}
        
        {gear.type === 'both' && (
          <Badge className="absolute bottom-2 left-2 bg-blue-500">
            Rent & Buy
          </Badge>
        )}
      </div>
      
      <CardContent className="p-4">
        <div className="space-y-2">
          <div className="flex items-start justify-between">
            <h3 
              className="font-semibold text-sm line-clamp-2 cursor-pointer hover:text-blue-600"
              onClick={() => handleGearClick(gear.id)}
            >
              {gear.title}
            </h3>
            <Badge variant="outline" className="text-xs">
              {gear.condition}
            </Badge>
          </div>
          
          <div className="flex items-center space-x-1">
            <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
            <span className="text-xs text-gray-600">
              {gear.ratings.average.toFixed(1)} ({gear.ratings.count})
            </span>
          </div>
          
          {/* Reviews Toggle */}
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              toggleReviews(gear.id);
            }}
            className="w-full text-xs mt-2"
          >
            <Star className="w-3 h-3 mr-1" />
            {expandedReviews[gear.id] ? 'Hide Reviews' : 'View Reviews'}
          </Button>
          
          {/* Reviews Section */}
          {expandedReviews[gear.id] && (
            <div className="mt-2 pt-2 border-t">
              <RatingReview
                gearId={gear.id}
                gear={gear}
                canReview={false}
                showReviews={true}
              />
            </div>
          )}
          
          <div className="flex items-center space-x-1 text-xs text-gray-500">
            <MapPin className="w-3 h-3" />
            <span>{gear.location.city}, {gear.location.country}</span>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <div className="font-bold text-lg">
                ${gear.price}
                {gear.type !== 'sale' && gear.rentPrice && (
                  <span className="text-sm text-gray-500 ml-1">
                    / ${gear.rentPrice}/day
                  </span>
                )}
              </div>
            </div>
            
            <div className="flex space-x-1">
              {(gear.type === 'rent' || gear.type === 'both') && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleAddToCart(gear, 'rent');
                  }}
                  disabled={!gear.availability.inStock || isAddingItem}
                >
                  <Calendar className="w-3 h-3 mr-1" />
                  Rent
                </Button>
              )}
              
              {(gear.type === 'sale' || gear.type === 'both') && (
                <Button
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleAddToCart(gear, 'purchase');
                  }}
                  disabled={!gear.availability.inStock || isAddingItem}
                >
                  <ShoppingCart className="w-3 h-3 mr-1" />
                  Buy
                </Button>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
  
  // Render list view item
  const renderListItem = (gear: TravelGear) => (
    <Card key={gear.id} className="hover:shadow-md transition-shadow">
      <CardContent className="p-4">
        <div className="flex space-x-4">
          <div 
            className="w-24 h-24 bg-gray-100 rounded-lg overflow-hidden cursor-pointer"
            onClick={() => handleGearClick(gear.id)}
          >
            {gear.images.length > 0 ? (
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
          
          <div className="flex-1 space-y-2">
            <div className="flex items-start justify-between">
              <h3 
                className="font-semibold cursor-pointer hover:text-blue-600"
                onClick={() => handleGearClick(gear.id)}
              >
                {gear.title}
              </h3>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => toggleFavorite(gear.id)}
              >
                <Heart 
                  className={`w-4 h-4 ${
                    favorites.has(gear.id) ? 'fill-red-500 text-red-500' : 'text-gray-600'
                  }`} 
                />
              </Button>
            </div>
            
            <p className="text-sm text-gray-600 line-clamp-2">{gear.description}</p>
            
            <div className="flex items-center space-x-4 text-sm text-gray-500">
              <div className="flex items-center space-x-1">
              <Star className="w-3 h-3 fill-yellow-400 text-yellow-400" />
              <span>{gear.ratings.average.toFixed(1)} ({gear.ratings.count})</span>
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  toggleReviews(gear.id);
                }}
                className="text-xs ml-2"
              >
                {expandedReviews[gear.id] ? 'Hide Reviews' : 'View Reviews'}
              </Button>
            </div>
            
            {/* Reviews Section */}
            {expandedReviews[gear.id] && (
              <div className="mt-2 pt-2 border-t">
                <RatingReview
                  gearId={gear.id}
                  gear={gear}
                  canReview={false}
                  showReviews={true}
                />
              </div>
            )}
              <div className="flex items-center space-x-1">
                <MapPin className="w-3 h-3" />
                <span>{gear.location.city}</span>
              </div>
              <Badge variant="outline">{gear.condition}</Badge>
            </div>
            
            <div className="flex items-center justify-between">
              <div>
                <div className="font-bold text-lg">
                  ${gear.price}
                  {gear.type !== 'sale' && gear.rentPrice && (
                    <span className="text-sm text-gray-500 ml-1">
                      / ${gear.rentPrice}/day
                    </span>
                  )}
                </div>
              </div>
              
              <div className="flex space-x-2">
                {(gear.type === 'rent' || gear.type === 'both') && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleAddToCart(gear, 'rent')}
                    disabled={!gear.availability.inStock || isAddingItem}
                  >
                    <Calendar className="w-3 h-3 mr-1" />
                    Rent
                  </Button>
                )}
                
                {(gear.type === 'sale' || gear.type === 'both') && (
                  <Button
                    size="sm"
                    onClick={() => handleAddToCart(gear, 'purchase')}
                    disabled={!gear.availability.inStock || isAddingItem}
                  >
                    <ShoppingCart className="w-3 h-3 mr-1" />
                    Buy
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
  
  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Travel Gear Marketplace</h1>
          <p className="text-gray-600">Rent or buy quality travel gear from fellow adventurers</p>
        </div>
        <div className="mt-4 md:mt-0">
          <Button onClick={() => router.push('/cart')} variant="outline" className="mr-2">
            <ShoppingCart className="h-4 w-4 mr-2" />
            Cart
          </Button>
          <Button onClick={() => router.push('/marketplace/list')}>
            List Your Gear
          </Button>
        </div>
      </div>

      {/* Category Navigation */}
      <div className="mb-8">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Browse by Category</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4">
          {[
            { id: 'backpacks', name: 'Backpacks', icon: '🎒' },
            { id: 'tents', name: 'Tents', icon: '⛺' },
            { id: 'clothing', name: 'Clothing', icon: '👕' },
            { id: 'footwear', name: 'Footwear', icon: '👟' },
            { id: 'electronics', name: 'Electronics', icon: '📱' },
            { id: 'cooking', name: 'Cooking', icon: '🍳' },
            { id: 'navigation', name: 'Navigation', icon: '🧭' },
            { id: 'accessories', name: 'Accessories', icon: '🔧' }
          ].map((category) => (
            <Card
              key={category.id}
              className="cursor-pointer hover:shadow-md transition-shadow p-4 text-center"
              onClick={() => router.push(`/marketplace/${category.id}`)}
            >
              <div className="text-2xl mb-2">{category.icon}</div>
              <p className="text-sm font-medium text-gray-900">{category.name}</p>
            </Card>
          ))}
        </div>
      </div>
      
      {/* Search Filters */}
       <div className="mb-6">
         <SearchFilters
           filters={searchFilters}
           onFiltersChange={setSearchFilters}
           onSearch={() => handleSearch(searchFilters)}
           isLoading={isSearching}
         />
        
        {/* View Mode Toggle */}
        <div className="flex justify-end mt-4">
          <div className="flex border rounded-md">
            <Button
              variant={viewMode === 'grid' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('grid')}
            >
              <Grid className="w-4 h-4" />
            </Button>
            <Button
              variant={viewMode === 'list' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('list')}
            >
              <List className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>
      
      {/* Results Summary */}
      {searchResponse && (
        <div className="mb-4 flex items-center justify-between">
          <p className="text-sm text-gray-600">
            Showing {filteredItems.length} of {searchResponse.total} results
          </p>
          {(searchFilters.category !== 'all' || searchFilters.query || searchFilters.features.length > 0) && (
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-500">Active filters:</span>
              {searchFilters.category !== 'all' && (
                <Badge variant="secondary">
                  {searchFilters.category}
                </Badge>
              )}
              {searchFilters.query && (
                <Badge variant="secondary">
                  "{searchFilters.query}"
                </Badge>
              )}
              {searchFilters.features.map(feature => (
                <Badge key={feature} variant="secondary">
                  {feature}
                </Badge>
              ))}
            </div>
          )}
        </div>
      )}
      
      {/* Error State */}
      {error && (
        <Alert className="mb-6">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      
      {/* Results */}
      <div className="space-y-6">
        {viewMode === 'grid' ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredItems.map(renderGearCard)}
          </div>
        ) : (
          <div className="space-y-4">
            {filteredItems.map(renderListItem)}
          </div>
        )}
        
        {/* Loading State */}
        {(isLoading || isSearching) && (
          <div className="flex justify-center py-8">
            <Loader2 className="w-6 h-6 animate-spin" />
          </div>
        )}
        
        {/* Load More */}
        {!isLoading && !isSearching && hasMore && filteredItems.length > 0 && (
          <div className="flex justify-center py-6">
            <Button onClick={loadMore} variant="outline">
              Load More
            </Button>
          </div>
        )}
        
        {/* Empty State */}
        {!isLoading && !isSearching && filteredItems.length === 0 && (
          <div className="text-center py-12">
            <Package className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No gear found</h3>
            <p className="text-gray-500 mb-4">
              {searchFilters.query || searchFilters.category !== 'all' || searchFilters.features.length > 0
                ? 'Try adjusting your search or filters'
                : 'Be the first to list your travel gear!'}
            </p>
            <Button onClick={() => handleSearch({
              query: '',
              category: 'all',
              priceRange: [0, 1000],
              availability: 'all',
              rating: 0,
              location: '',
              features: [],
              sortBy: 'relevance',
            })} variant="outline">
              Clear Search & Filters
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

export default MarketplacePage;