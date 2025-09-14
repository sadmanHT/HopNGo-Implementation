'use client';

import React, { useState, useEffect } from 'react';
import { Search, Filter, X, Star, DollarSign, Calendar, MapPin } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Slider } from '../ui/slider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Checkbox } from '../ui/checkbox';
import { Badge } from '../ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Separator } from '../ui/separator';
import { formatCurrency } from '../../lib/utils';

export interface SearchFilters {
  query: string;
  category: string;
  priceRange: [number, number];
  availability: 'all' | 'available' | 'rental' | 'purchase';
  rating: number;
  location: string;
  features: string[];
  sortBy: 'relevance' | 'price_low' | 'price_high' | 'rating' | 'newest';
  dateRange?: {
    startDate: string;
    endDate: string;
  };
}

interface SearchFiltersProps {
  filters: SearchFilters;
  onFiltersChange: (filters: SearchFilters) => void;
  onSearch: () => void;
  isLoading?: boolean;
  className?: string;
}

const CATEGORIES = [
  { value: 'all', label: 'All Categories' },
  { value: 'backpacks', label: 'Backpacks' },
  { value: 'tents', label: 'Tents' },
  { value: 'sleeping-bags', label: 'Sleeping Bags' },
  { value: 'hiking-boots', label: 'Hiking Boots' },
  { value: 'cooking-gear', label: 'Cooking Gear' },
  { value: 'navigation', label: 'Navigation' },
  { value: 'safety', label: 'Safety Equipment' },
  { value: 'clothing', label: 'Outdoor Clothing' },
];

const FEATURES = [
  'Waterproof',
  'Lightweight',
  'Durable',
  'Compact',
  'Multi-season',
  'Quick Setup',
  'Breathable',
  'Insulated',
  'UV Protection',
  'Anti-microbial',
];

const SORT_OPTIONS = [
  { value: 'relevance', label: 'Most Relevant' },
  { value: 'price_low', label: 'Price: Low to High' },
  { value: 'price_high', label: 'Price: High to Low' },
  { value: 'rating', label: 'Highest Rated' },
  { value: 'newest', label: 'Newest First' },
];

export function SearchFilters({
  filters,
  onFiltersChange,
  onSearch,
  isLoading = false,
  className = ''
}: SearchFiltersProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [localFilters, setLocalFilters] = useState<SearchFilters>(filters);

  // Update local filters when props change
  useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  const updateFilter = <K extends keyof SearchFilters>(
    key: K,
    value: SearchFilters[K]
  ) => {
    const newFilters = { ...localFilters, [key]: value };
    setLocalFilters(newFilters);
    onFiltersChange(newFilters);
  };

  const handleFeatureToggle = (feature: string, checked: boolean) => {
    const newFeatures = checked
      ? [...localFilters.features, feature]
      : localFilters.features.filter(f => f !== feature);
    updateFilter('features', newFeatures);
  };

  const clearFilters = () => {
    const defaultFilters: SearchFilters = {
      query: '',
      category: 'all',
      priceRange: [0, 1000],
      availability: 'all',
      rating: 0,
      location: '',
      features: [],
      sortBy: 'relevance',
    };
    setLocalFilters(defaultFilters);
    onFiltersChange(defaultFilters);
  };

  const getActiveFiltersCount = () => {
    let count = 0;
    if (localFilters.query) count++;
    if (localFilters.category !== 'all') count++;
    if (localFilters.priceRange[0] > 0 || localFilters.priceRange[1] < 1000) count++;
    if (localFilters.availability !== 'all') count++;
    if (localFilters.rating > 0) count++;
    if (localFilters.location) count++;
    if (localFilters.features.length > 0) count++;
    if (localFilters.dateRange) count++;
    return count;
  };

  const activeFiltersCount = getActiveFiltersCount();

  return (
    <Card className={`w-full ${className}`}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Search className="h-5 w-5" />
            Search & Filters
            {activeFiltersCount > 0 && (
              <Badge variant="secondary" className="ml-2">
                {activeFiltersCount}
              </Badge>
            )}
          </CardTitle>
          <div className="flex items-center gap-2">
            {activeFiltersCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={clearFilters}
                className="text-gray-500 hover:text-gray-700"
              >
                Clear All
              </Button>
            )}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsExpanded(!isExpanded)}
              className="lg:hidden"
            >
              <Filter className="h-4 w-4" />
              {isExpanded ? 'Hide' : 'Show'} Filters
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className={`space-y-6 ${!isExpanded ? 'hidden lg:block' : ''}`}>
        {/* Search Query */}
        <div className="space-y-2">
          <Label htmlFor="search-query">Search</Label>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              id="search-query"
              placeholder="Search for gear..."
              value={localFilters.query}
              onChange={(e) => updateFilter('query', e.target.value)}
              className="pl-10"
              onKeyPress={(e) => e.key === 'Enter' && onSearch()}
            />
          </div>
        </div>

        {/* Category */}
        <div className="space-y-2">
          <Label>Category</Label>
          <Select
            value={localFilters.category}
            onValueChange={(value) => updateFilter('category', value)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {CATEGORIES.map((category) => (
                <SelectItem key={category.value} value={category.value}>
                  {category.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Price Range */}
        <div className="space-y-3">
          <Label className="flex items-center gap-2">
            <DollarSign className="h-4 w-4" />
            Price Range
          </Label>
          <div className="px-2">
            <Slider
              value={localFilters.priceRange}
              onValueChange={(value) => updateFilter('priceRange', value as [number, number])}
              max={1000}
              min={0}
              step={10}
              className="w-full"
            />
          </div>
          <div className="flex items-center justify-between text-sm text-gray-600">
            <span>{formatCurrency(localFilters.priceRange[0])}</span>
            <span>{formatCurrency(localFilters.priceRange[1])}</span>
          </div>
        </div>

        {/* Availability */}
        <div className="space-y-2">
          <Label>Availability</Label>
          <Select
            value={localFilters.availability}
            onValueChange={(value) => updateFilter('availability', value as SearchFilters['availability'])}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Items</SelectItem>
              <SelectItem value="available">Available Now</SelectItem>
              <SelectItem value="rental">Rental Only</SelectItem>
              <SelectItem value="purchase">Purchase Only</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Rating Filter */}
        <div className="space-y-3">
          <Label className="flex items-center gap-2">
            <Star className="h-4 w-4" />
            Minimum Rating
          </Label>
          <div className="flex items-center gap-2">
            {[1, 2, 3, 4, 5].map((rating) => (
              <Button
                key={rating}
                variant={localFilters.rating >= rating ? "default" : "outline"}
                size="sm"
                onClick={() => updateFilter('rating', rating === localFilters.rating ? 0 : rating)}
                className="p-2"
              >
                <Star className={`h-4 w-4 ${localFilters.rating >= rating ? 'fill-current' : ''}`} />
              </Button>
            ))}
            {localFilters.rating > 0 && (
              <span className="text-sm text-gray-600 ml-2">
                {localFilters.rating}+ stars
              </span>
            )}
          </div>
        </div>

        {/* Location */}
        <div className="space-y-2">
          <Label className="flex items-center gap-2">
            <MapPin className="h-4 w-4" />
            Location
          </Label>
          <Input
            placeholder="Enter city or region..."
            value={localFilters.location}
            onChange={(e) => updateFilter('location', e.target.value)}
          />
        </div>

        {/* Features */}
        <div className="space-y-3">
          <Label>Features</Label>
          <div className="grid grid-cols-2 gap-2">
            {FEATURES.map((feature) => (
              <div key={feature} className="flex items-center space-x-2">
                <Checkbox
                  id={`feature-${feature}`}
                  checked={localFilters.features.includes(feature)}
                  onCheckedChange={(checked) => handleFeatureToggle(feature, checked as boolean)}
                />
                <Label
                  htmlFor={`feature-${feature}`}
                  className="text-sm font-normal cursor-pointer"
                >
                  {feature}
                </Label>
              </div>
            ))}
          </div>
        </div>

        {/* Date Range for Rentals */}
        <div className="space-y-2">
          <Label className="flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            Rental Dates (Optional)
          </Label>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <Label htmlFor="start-date" className="text-xs text-gray-500">Start Date</Label>
              <Input
                id="start-date"
                type="date"
                value={localFilters.dateRange?.startDate || ''}
                onChange={(e) => updateFilter('dateRange', {
                  ...localFilters.dateRange,
                  startDate: e.target.value,
                  endDate: localFilters.dateRange?.endDate || ''
                })}
              />
            </div>
            <div>
              <Label htmlFor="end-date" className="text-xs text-gray-500">End Date</Label>
              <Input
                id="end-date"
                type="date"
                value={localFilters.dateRange?.endDate || ''}
                onChange={(e) => updateFilter('dateRange', {
                  ...localFilters.dateRange,
                  startDate: localFilters.dateRange?.startDate || '',
                  endDate: e.target.value
                })}
              />
            </div>
          </div>
        </div>

        <Separator />

        {/* Sort By */}
        <div className="space-y-2">
          <Label>Sort By</Label>
          <Select
            value={localFilters.sortBy}
            onValueChange={(value) => updateFilter('sortBy', value as SearchFilters['sortBy'])}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SORT_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Search Button */}
        <Button
          onClick={onSearch}
          disabled={isLoading}
          className="w-full"
          size="lg"
        >
          {isLoading ? (
            <>
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
              Searching...
            </>
          ) : (
            <>
              <Search className="h-4 w-4 mr-2" />
              Search Gear
            </>
          )}
        </Button>
      </CardContent>
    </Card>
  );
}

export default SearchFilters;