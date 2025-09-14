'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  ArrowLeft,
  Filter,
  Grid,
  List,
  Star,
  MapPin,
  Calendar,
  ShoppingCart,
  Search,
  SlidersHorizontal
} from 'lucide-react';
import { shoppingService } from '@/services/shopping';
import { useCartStore } from '@/stores/cartStore';
import RatingReview from '@/components/marketplace/RatingReview';
import { cn } from '@/lib/utils';

interface CategoryPageProps {}

const CATEGORY_NAMES: Record<string, string> = {
  'backpacks': 'Backpacks & Bags',
  'tents': 'Tents & Shelters',
  'clothing': 'Outdoor Clothing',
  'footwear': 'Footwear',
  'electronics': 'Electronics & Gadgets',
  'cooking': 'Cooking & Food',
  'navigation': 'Navigation & Safety',
  'accessories': 'Accessories'
};

const SORT_OPTIONS = [
  { value: 'relevance', label: 'Most Relevant' },
  { value: 'price-low', label: 'Price: Low to High' },
  { value: 'price-high', label: 'Price: High to Low' },
  { value: 'rating', label: 'Highest Rated' },
  { value: 'newest', label: 'Newest First' }
];

export default function CategoryPage({}: CategoryPageProps) {
  const params = useParams();
  const router = useRouter();
  const category = params?.category as string;
  const { addItem } = useCartStore();
  
  const [gearItems, setGearItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('relevance');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [expandedReviews, setExpandedReviews] = useState<Record<string, boolean>>({});
  const [filters, setFilters] = useState({
    priceRange: { min: 0, max: 1000 },
    availability: 'all', // 'all', 'rent', 'sale'
    rating: 0
  });

  useEffect(() => {
    loadCategoryGear();
  }, [category, sortBy, searchQuery]);

  const loadCategoryGear = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await shoppingService.searchGear(
        searchQuery,
        {
          category: [category],
          type: 'both',
          priceRange: { min: filters.priceRange.min, max: filters.priceRange.max },
          availability: filters.availability === 'all' ? undefined : filters.availability === 'available',
          rating: filters.rating,
          sortBy: sortBy as 'price' | 'rating' | 'newest' | 'popular',
        },
        0,
        20
      );
      
      setGearItems(response.items || []);
    } catch (err) {
      console.error('Error loading category gear:', err);
      setError('Failed to load gear items. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = async (gear: any, type: 'rent' | 'sale') => {
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

  const toggleReviews = (gearId: string) => {
    setExpandedReviews(prev => ({
      ...prev,
      [gearId]: !prev[gearId]
    }));
  };

  const categoryName = CATEGORY_NAMES[category] || category;

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-64 bg-gray-200 rounded-lg"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.back()}
            className="flex items-center space-x-2"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back</span>
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{categoryName}</h1>
            <p className="text-gray-600">{gearItems.length} items available</p>
          </div>
        </div>
        
        <div className="flex items-center space-x-2">
          <Button
            variant={viewMode === 'grid' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('grid')}
          >
            <Grid className="h-4 w-4" />
          </Button>
          <Button
            variant={viewMode === 'list' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('list')}
          >
            <List className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Search and Filters */}
      <div className="flex flex-col md:flex-row gap-4 mb-6">
        <div className="flex-1">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder={`Search ${categoryName.toLowerCase()}...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>
        
        <div className="flex gap-2">
          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-48">
              <SelectValue placeholder="Sort by" />
            </SelectTrigger>
            <SelectContent>
              {SORT_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          
          <Button variant="outline" size="sm">
            <SlidersHorizontal className="h-4 w-4 mr-2" />
            Filters
          </Button>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">{error}</p>
          <Button
            variant="outline"
            size="sm"
            onClick={loadCategoryGear}
            className="mt-2"
          >
            Try Again
          </Button>
        </div>
      )}

      {/* Gear Grid/List */}
      {gearItems.length === 0 && !loading ? (
        <div className="text-center py-12">
          <ShoppingCart className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No items found</h3>
          <p className="text-gray-600">Try adjusting your search or filters</p>
        </div>
      ) : (
        <div className={cn(
          viewMode === 'grid'
            ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6'
            : 'space-y-4'
        )}>
          {gearItems.map((gear) => (
            <Card key={gear.id} className={cn(
              "overflow-hidden hover:shadow-lg transition-shadow cursor-pointer",
              viewMode === 'list' && "flex flex-row"
            )}>
              <div className={cn(
                viewMode === 'list' ? "w-48 flex-shrink-0" : "aspect-square"
              )}>
                <img
                  src={gear.images?.[0] || '/placeholder-gear.jpg'}
                  alt={gear.name}
                  className="w-full h-full object-cover"
                />
              </div>
              
              <CardContent className={cn(
                "p-4",
                viewMode === 'list' && "flex-1 flex flex-col justify-between"
              )}>
                <div>
                  <div className="flex items-start justify-between mb-2">
                    <h3 className="font-semibold text-gray-900 line-clamp-2">{gear.name}</h3>
                    <Badge variant="secondary" className="ml-2">
                      {gear.condition}
                    </Badge>
                  </div>
                  
                  <div className="flex items-center gap-1 mb-2">
                    <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                    <span className="text-sm font-medium">{gear.rating}</span>
                    <span className="text-sm text-gray-500">({gear.reviewCount})</span>
                  </div>
                  
                  <div className="flex items-center text-sm text-gray-500 mb-3">
                    <MapPin className="h-4 w-4 mr-1" />
                    <span>{gear.location}</span>
                  </div>
                  
                  <p className="text-sm text-gray-600 line-clamp-2 mb-4">
                    {gear.description}
                  </p>
                </div>
                
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    {gear.availableForRent && (
                      <div className="text-sm">
                        <span className="font-semibold text-green-600">${gear.rentPrice}</span>
                        <span className="text-gray-500">/day</span>
                      </div>
                    )}
                    {gear.availableForSale && (
                      <div className="text-sm">
                        <span className="font-semibold text-blue-600">${gear.price}</span>
                        <span className="text-gray-500"> to buy</span>
                      </div>
                    )}
                  </div>
                  
                  <div className="flex gap-2">
                    {gear.availableForRent && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAddToCart(gear, 'rent');
                        }}
                        disabled={isAddingToCart}
                        className="flex-1"
                      >
                        <Calendar className="h-4 w-4 mr-1" />
                        Rent
                      </Button>
                    )}
                    {gear.availableForSale && (
                      <Button
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAddToCart(gear, 'sale');
                        }}
                        disabled={isAddingToCart}
                        className="flex-1"
                      >
                        <ShoppingCart className="h-4 w-4 mr-1" />
                        Buy
                      </Button>
                    )}
                  </div>
                  
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleReviews(gear.id);
                    }}
                    className="w-full text-sm"
                  >
                    <Star className="h-4 w-4 mr-1" />
                    {expandedReviews[gear.id] ? 'Hide Reviews' : 'View Reviews'}
                  </Button>
                  
                  {expandedReviews[gear.id] && (
                    <div className="mt-4 pt-4 border-t">
                      <RatingReview
                        gearId={gear.id}
                        gear={gear}
                        canReview={false}
                        showReviews={true}
                      />
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}