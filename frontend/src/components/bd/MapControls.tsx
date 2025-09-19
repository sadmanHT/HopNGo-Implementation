'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Slider } from '@/components/ui/slider';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  Clock,
  Calendar,
  MapPin,
  Filter,
  Layers,
  Settings,
  Eye,
  EyeOff,
  Zap,
  TrendingUp,
  Users,
  Star,
  DollarSign,
  ChevronDown,
  RefreshCw,
  Download,
  Share2,
  Maximize,
  Minimize,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface TimeRange {
  label: string;
  labelBn: string;
  value: string;
  hours: number;
}

interface MapLayer {
  id: string;
  name: string;
  nameBn: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  enabled: boolean;
}

interface FilterTag {
  id: string;
  name: string;
  nameBn: string;
  color: string;
  count?: number;
}

const timeRanges: TimeRange[] = [
  { label: '24 Hours', labelBn: '২৪ ঘন্টা', value: '24h', hours: 24 },
  { label: '72 Hours', labelBn: '৭২ ঘন্টা', value: '72h', hours: 72 },
  { label: '7 Days', labelBn: '৭ দিন', value: '7d', hours: 168 },
  { label: '30 Days', labelBn: '৩০ দিন', value: '30d', hours: 720 },
];

const mapLayers: MapLayer[] = [
  { id: 'heatmap', name: 'Activity Heatmap', nameBn: 'কার্যকলাপ হিটম্যাপ', icon: TrendingUp, color: 'text-bd-coral', enabled: true },
  { id: 'destinations', name: 'Destinations', nameBn: 'গন্তব্য', icon: MapPin, color: 'text-bd-green', enabled: true },
  { id: 'accommodations', name: 'Hotels & Stays', nameBn: 'হোটেল ও থাকার জায়গা', icon: Users, color: 'text-bd-teal', enabled: false },
  { id: 'reviews', name: 'Reviews & Ratings', nameBn: 'রিভিউ ও রেটিং', icon: Star, color: 'text-bd-sunrise', enabled: false },
  { id: 'pricing', name: 'Price Ranges', nameBn: 'মূল্য পরিসীমা', icon: DollarSign, color: 'text-bd-sand', enabled: false },
];

const filterTags: FilterTag[] = [
  { id: 'nature', name: 'Nature', nameBn: 'প্রকৃতি', color: 'bg-green-100 text-green-800', count: 245 },
  { id: 'beach', name: 'Beach', nameBn: 'সমুদ্র সৈকত', color: 'bg-blue-100 text-blue-800', count: 89 },
  { id: 'historical', name: 'Historical', nameBn: 'ঐতিহাসিক', color: 'bg-amber-100 text-amber-800', count: 156 },
  { id: 'adventure', name: 'Adventure', nameBn: 'অ্যাডভেঞ্চার', color: 'bg-red-100 text-red-800', count: 78 },
  { id: 'cultural', name: 'Cultural', nameBn: 'সাংস্কৃতিক', color: 'bg-purple-100 text-purple-800', count: 134 },
  { id: 'food', name: 'Food & Dining', nameBn: 'খাবার ও ডাইনিং', color: 'bg-orange-100 text-orange-800', count: 203 },
];

interface MapControlsProps {
  className?: string;
  isFullscreen?: boolean;
  onFullscreenToggle?: () => void;
  onTimeRangeChange?: (range: string) => void;
  onLayerToggle?: (layerId: string, enabled: boolean) => void;
  onPrecisionChange?: (precision: number) => void;
  onFilterToggle?: (tagId: string, enabled: boolean) => void;
  onRefresh?: () => void;
  onExport?: () => void;
  onShare?: () => void;
}

export function MapControls({
  className,
  isFullscreen = false,
  onFullscreenToggle,
  onTimeRangeChange,
  onLayerToggle,
  onPrecisionChange,
  onFilterToggle,
  onRefresh,
  onExport,
  onShare,
}: MapControlsProps) {
  const [selectedTimeRange, setSelectedTimeRange] = useState('24h');
  const [precision, setPrecision] = useState([75]);
  const [layers, setLayers] = useState(mapLayers);
  const [activeTags, setActiveTags] = useState<Set<string>>(new Set(['nature', 'beach']));
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);

  const handleTimeRangeChange = (range: string) => {
    setSelectedTimeRange(range);
    onTimeRangeChange?.(range);
  };

  const handleLayerToggle = (layerId: string) => {
    const updatedLayers = layers.map(layer => 
      layer.id === layerId ? { ...layer, enabled: !layer.enabled } : layer
    );
    setLayers(updatedLayers);
    const layer = updatedLayers.find(l => l.id === layerId);
    onLayerToggle?.(layerId, layer?.enabled || false);
  };

  const handlePrecisionChange = (value: number[]) => {
    setPrecision(value);
    onPrecisionChange?.(value[0]);
  };

  const handleTagToggle = (tagId: string) => {
    const newActiveTags = new Set(activeTags);
    if (newActiveTags.has(tagId)) {
      newActiveTags.delete(tagId);
    } else {
      newActiveTags.add(tagId);
    }
    setActiveTags(newActiveTags);
    onFilterToggle?.(tagId, newActiveTags.has(tagId));
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await onRefresh?.();
    setTimeout(() => setIsRefreshing(false), 1000);
  };

  const selectedRange = timeRanges.find(r => r.value === selectedTimeRange);
  const enabledLayersCount = layers.filter(l => l.enabled).length;

  return (
    <TooltipProvider>
      <div className={cn(
        "absolute top-4 left-4 z-10 space-y-3 max-w-sm",
        className
      )}>
        {/* Main Control Panel */}
        <motion.div
          className="bg-white/90 backdrop-blur-md rounded-2xl shadow-xl border border-white/20 p-4 space-y-4"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3 }}
        >
          {/* Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-br from-bd-green to-bd-teal rounded-lg flex items-center justify-center">
                <TrendingUp className="w-4 h-4 text-white" />
              </div>
              <div>
                <h3 className="font-semibold text-bd-slate text-sm">Map Controls</h3>
                <p className="text-xs text-muted-foreground font-bengali">মানচিত্র নিয়ন্ত্রণ</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-1">
              <Button
                size="sm"
                variant="ghost"
                className="h-8 w-8 p-0 hover:bg-bd-green/10"
                onClick={handleRefresh}
                disabled={isRefreshing}
              >
                <RefreshCw className={cn(
                  "h-4 w-4 text-bd-green",
                  isRefreshing && "animate-spin"
                )} />
              </Button>
              
              <Button
                size="sm"
                variant="ghost"
                className="h-8 w-8 p-0 hover:bg-bd-teal/10"
                onClick={() => setShowAdvanced(!showAdvanced)}
              >
                <Settings className="h-4 w-4 text-bd-teal" />
              </Button>
            </div>
          </div>

          {/* Time Range Selector */}
          <div className="space-y-2">
            <Label className="text-xs font-medium text-bd-slate">
              Time Range / সময় পরিসীমা
            </Label>
            <div className="grid grid-cols-2 gap-2">
              {timeRanges.map((range) => (
                <Button
                  key={range.value}
                  size="sm"
                  variant={selectedTimeRange === range.value ? "default" : "outline"}
                  className={cn(
                    "h-auto p-2 flex flex-col items-center space-y-1 transition-all duration-200",
                    selectedTimeRange === range.value
                      ? "bg-bd-green hover:bg-bd-green/90 text-white shadow-md"
                      : "hover:bg-bd-green/5 hover:border-bd-green/30"
                  )}
                  onClick={() => handleTimeRangeChange(range.value)}
                >
                  <span className="text-xs font-medium">{range.label}</span>
                  <span className="text-xs opacity-70 font-bengali">{range.labelBn}</span>
                </Button>
              ))}
            </div>
          </div>

          {/* Precision Slider */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label className="text-xs font-medium text-bd-slate">
                Precision / নির্ভুলতা
              </Label>
              <Badge variant="outline" className="text-xs px-2 py-0.5">
                {precision[0]}%
              </Badge>
            </div>
            <Slider
              value={precision}
              onValueChange={handlePrecisionChange}
              max={100}
              min={10}
              step={5}
              className="w-full"
            />
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Low</span>
              <span className="font-bengali">কম</span>
              <span>High</span>
              <span className="font-bengali">বেশি</span>
            </div>
          </div>

          {/* Layer Controls */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label className="text-xs font-medium text-bd-slate">
                Map Layers / মানচিত্র স্তর
              </Label>
              <Badge variant="secondary" className="text-xs">
                {enabledLayersCount} active
              </Badge>
            </div>
            <div className="space-y-2">
              {layers.map((layer) => {
                const Icon = layer.icon;
                return (
                  <div key={layer.id} className="flex items-center justify-between">
                    <div className="flex items-center space-x-2 flex-1">
                      <Icon className={cn("w-4 h-4", layer.color)} />
                      <div className="flex-1">
                        <div className="text-xs font-medium text-bd-slate">{layer.name}</div>
                        <div className="text-xs text-muted-foreground font-bengali">{layer.nameBn}</div>
                      </div>
                    </div>
                    <Switch
                      checked={layer.enabled}
                      onCheckedChange={() => handleLayerToggle(layer.id)}
                    />
                  </div>
                );
              })}
            </div>
          </div>
        </motion.div>

        {/* Filter Tags */}
        <motion.div
          className="bg-white/90 backdrop-blur-md rounded-2xl shadow-xl border border-white/20 p-4 space-y-3"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <div className="flex items-center space-x-2">
            <Filter className="w-4 h-4 text-bd-teal" />
            <Label className="text-xs font-medium text-bd-slate">
              Category Filters / বিভাগ ফিল্টার
            </Label>
          </div>
          
          <div className="flex flex-wrap gap-2">
            {filterTags.map((tag) => {
              const isActive = activeTags.has(tag.id);
              return (
                <Tooltip key={tag.id}>
                  <TooltipTrigger asChild>
                    <Button
                      size="sm"
                      variant="outline"
                      className={cn(
                        "h-auto p-2 flex flex-col items-center space-y-1 transition-all duration-200 border-2",
                        isActive
                          ? "border-bd-green bg-bd-green/10 text-bd-green shadow-sm"
                          : "hover:border-bd-green/30 hover:bg-bd-green/5"
                      )}
                      onClick={() => handleTagToggle(tag.id)}
                    >
                      <span className="text-xs font-medium">{tag.name}</span>
                      <span className="text-xs opacity-70 font-bengali">{tag.nameBn}</span>
                      {tag.count && (
                        <Badge variant="secondary" className="text-xs px-1.5 py-0.5">
                          {tag.count}
                        </Badge>
                      )}
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>{tag.count} locations found</p>
                  </TooltipContent>
                </Tooltip>
              );
            })}
          </div>
        </motion.div>

        {/* Advanced Controls */}
        <AnimatePresence>
          {showAdvanced && (
            <motion.div
              className="bg-white/90 backdrop-blur-md rounded-2xl shadow-xl border border-white/20 p-4 space-y-3"
              initial={{ opacity: 0, x: -20, height: 0 }}
              animate={{ opacity: 1, x: 0, height: 'auto' }}
              exit={{ opacity: 0, x: -20, height: 0 }}
              transition={{ duration: 0.3 }}
            >
              <div className="flex items-center space-x-2">
                <Zap className="w-4 h-4 text-bd-sunrise" />
                <Label className="text-xs font-medium text-bd-slate">
                  Advanced Options / উন্নত বিকল্প
                </Label>
              </div>
              
              <div className="grid grid-cols-2 gap-2">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-auto p-2 flex flex-col items-center space-y-1 hover:bg-bd-teal/5"
                      onClick={onExport}
                    >
                      <Download className="w-4 h-4 text-bd-teal" />
                      <span className="text-xs">Export</span>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>Export map data</p>
                  </TooltipContent>
                </Tooltip>
                
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-auto p-2 flex flex-col items-center space-y-1 hover:bg-bd-coral/5"
                      onClick={onShare}
                    >
                      <Share2 className="w-4 h-4 text-bd-coral" />
                      <span className="text-xs">Share</span>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>Share current view</p>
                  </TooltipContent>
                </Tooltip>
                
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-auto p-2 flex flex-col items-center space-y-1 hover:bg-bd-sunrise/5 col-span-2"
                      onClick={onFullscreenToggle}
                    >
                      {isFullscreen ? (
                        <Minimize className="w-4 h-4 text-bd-sunrise" />
                      ) : (
                        <Maximize className="w-4 h-4 text-bd-sunrise" />
                      )}
                      <span className="text-xs">
                        {isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
                      </span>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>{isFullscreen ? 'Exit fullscreen mode' : 'Enter fullscreen mode'}</p>
                  </TooltipContent>
                </Tooltip>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Status Indicator */}
        <motion.div
          className="bg-white/90 backdrop-blur-md rounded-xl shadow-lg border border-white/20 p-3"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, delay: 0.2 }}
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 bg-bd-green rounded-full animate-pulse" />
              <span className="text-xs text-bd-slate font-medium">Live Data</span>
            </div>
            <div className="text-xs text-muted-foreground">
              Updated {selectedRange?.labelBn}
            </div>
          </div>
        </motion.div>
      </div>
    </TooltipProvider>
  );
}