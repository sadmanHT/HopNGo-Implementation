'use client';

import { useState, useEffect } from 'react';
import { Search, Grid, List, SortAsc, SortDesc, Filter } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { useExperiment } from '@/lib/flags';
import { cn } from '@/lib/utils';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

interface BookingSearchExperimentProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  className?: string;
}

type SortOption = 'date' | 'price' | 'status' | 'title';
type ViewMode = 'grid' | 'list';

export function BookingSearchExperiment({ 
  searchQuery, 
  onSearchChange, 
  className 
}: BookingSearchExperimentProps) {
  const [sortBy, setSortBy] = useState<SortOption>('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  // A/B experiment for booking search layout
  const { variant, trackEvent } = useExperiment('booking-search-layout');

  useEffect(() => {
    // Track experiment exposure
    trackEvent('search_interface_viewed', {
      variant,
      timestamp: new Date().toISOString()
    });
  }, [variant, trackEvent]);

  const handleSearchChange = (value: string) => {
    onSearchChange(value);
    
    // Track search interaction
    trackEvent('search_query_entered', {
      variant,
      query_length: value.length,
      has_query: value.length > 0
    });
  };

  const handleSortChange = (newSortBy: SortOption) => {
    setSortBy(newSortBy);
    
    // Track sorting interaction
    trackEvent('search_sorted', {
      variant,
      sort_by: newSortBy,
      sort_order: sortOrder
    });
  };

  const handleViewModeChange = (newViewMode: ViewMode) => {
    setViewMode(newViewMode);
    
    // Track view mode change
    trackEvent('view_mode_changed', {
      variant,
      view_mode: newViewMode
    });
  };

  const toggleSortOrder = () => {
    const newOrder = sortOrder === 'asc' ? 'desc' : 'asc';
    setSortOrder(newOrder);
    
    trackEvent('sort_order_toggled', {
      variant,
      sort_by: sortBy,
      new_order: newOrder
    });
  };

  // Render different layouts based on experiment variant
  if (variant === 'compact') {
    // Variant A: Compact horizontal layout
    return (
      <Card className={cn("mb-6", className)}>
        <CardContent className="p-4">
          <div className="flex items-center space-x-3">
            {/* Search Input */}
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                placeholder="Quick search bookings..."
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                className="pl-10 h-9"
              />
            </div>
            
            {/* Sort */}
            <Select value={sortBy} onValueChange={handleSortChange}>
              <SelectTrigger className="w-32 h-9">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="date">Date</SelectItem>
                <SelectItem value="price">Price</SelectItem>
                <SelectItem value="status">Status</SelectItem>
                <SelectItem value="title">Title</SelectItem>
              </SelectContent>
            </Select>
            
            {/* Sort Order */}
            <Button
              variant="outline"
              size="sm"
              onClick={toggleSortOrder}
              className="h-9 w-9 p-0"
            >
              {sortOrder === 'asc' ? <SortAsc className="h-4 w-4" /> : <SortDesc className="h-4 w-4" />}
            </Button>
            
            {/* View Mode */}
            <div className="flex border rounded-md">
              <Button
                variant={viewMode === 'list' ? 'default' : 'ghost'}
                size="sm"
                onClick={() => handleViewModeChange('list')}
                className="h-9 rounded-r-none"
              >
                <List className="h-4 w-4" />
              </Button>
              <Button
                variant={viewMode === 'grid' ? 'default' : 'ghost'}
                size="sm"
                onClick={() => handleViewModeChange('grid')}
                className="h-9 rounded-l-none"
              >
                <Grid className="h-4 w-4" />
              </Button>
            </div>
          </div>
          
          {/* Experiment Indicator */}
          <div className="mt-2 flex justify-end">
            <Badge variant="outline" className="text-xs">
              Compact Layout • Variant A
            </Badge>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Variant B: Enhanced vertical layout with advanced features
  return (
    <Card className={cn("mb-6", className)}>
      <CardContent className="p-6">
        <div className="space-y-4">
          {/* Search Header */}
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold">Search & Filter Bookings</h3>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
            >
              <Filter className="h-4 w-4 mr-2" />
              Advanced
            </Button>
          </div>
          
          {/* Main Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <Input
              placeholder="Search by title, description, service type, or booking ID..."
              value={searchQuery}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="pl-10 h-11"
            />
          </div>
          
          {/* Controls Row */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              {/* Sort Controls */}
              <div className="flex items-center space-x-2">
                <span className="text-sm text-gray-600">Sort by:</span>
                <Select value={sortBy} onValueChange={handleSortChange}>
                  <SelectTrigger className="w-36">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="date">Date Created</SelectItem>
                    <SelectItem value="price">Price</SelectItem>
                    <SelectItem value="status">Status</SelectItem>
                    <SelectItem value="title">Title</SelectItem>
                  </SelectContent>
                </Select>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={toggleSortOrder}
                >
                  {sortOrder === 'asc' ? (
                    <><SortAsc className="h-4 w-4 mr-1" /> Ascending</>
                  ) : (
                    <><SortDesc className="h-4 w-4 mr-1" /> Descending</>
                  )}
                </Button>
              </div>
            </div>
            
            {/* View Mode */}
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-600">View:</span>
              <div className="flex border rounded-md">
                <Button
                  variant={viewMode === 'list' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => handleViewModeChange('list')}
                  className="rounded-r-none"
                >
                  <List className="h-4 w-4 mr-1" />
                  List
                </Button>
                <Button
                  variant={viewMode === 'grid' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => handleViewModeChange('grid')}
                  className="rounded-l-none"
                >
                  <Grid className="h-4 w-4 mr-1" />
                  Grid
                </Button>
              </div>
            </div>
          </div>
          
          {/* Advanced Filters */}
          {showAdvancedFilters && (
            <div className="border-t pt-4 space-y-3">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="text-sm font-medium mb-1 block">Status</label>
                  <Select>
                    <SelectTrigger>
                      <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Statuses</SelectItem>
                      <SelectItem value="pending">Pending</SelectItem>
                      <SelectItem value="confirmed">Confirmed</SelectItem>
                      <SelectItem value="cancelled">Cancelled</SelectItem>
                      <SelectItem value="completed">Completed</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                
                <div>
                  <label className="text-sm font-medium mb-1 block">Service Type</label>
                  <Select>
                    <SelectTrigger>
                      <SelectValue placeholder="All services" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Services</SelectItem>
                      <SelectItem value="transport">Transport</SelectItem>
                      <SelectItem value="accommodation">Accommodation</SelectItem>
                      <SelectItem value="activity">Activity</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                
                <div>
                  <label className="text-sm font-medium mb-1 block">Price Range</label>
                  <Select>
                    <SelectTrigger>
                      <SelectValue placeholder="Any price" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">Any Price</SelectItem>
                      <SelectItem value="0-50">$0 - $50</SelectItem>
                      <SelectItem value="50-200">$50 - $200</SelectItem>
                      <SelectItem value="200+">$200+</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>
          )}
          
          {/* Experiment Indicator */}
          <div className="flex justify-end">
            <Badge variant="outline" className="text-xs">
              Enhanced Layout • Variant B
            </Badge>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}