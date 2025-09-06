'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { MapPin, Layers, Clock, Tag, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';

interface HeatmapPoint {
  geohash: string;
  lat: number;
  lng: number;
  weight: number;
  tagsTop: string[];
}

interface HeatmapResponse {
  points: HeatmapPoint[];
}

const TIME_WINDOWS = [
  { value: '24', label: '24 hours' },
  { value: '72', label: '3 days' },
  { value: '168', label: '7 days' }
];

const PRECISION_LEVELS = [
  { value: 5, label: 'City level (5)' },
  { value: 6, label: 'District level (6)' },
  { value: 7, label: 'Neighborhood level (7)' }
];

export default function MapPage() {
  const [heatmapData, setHeatmapData] = useState<HeatmapPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Heatmap controls state
  const [timeWindow, setTimeWindow] = useState('72');
  const [precision, setPrecision] = useState([6]);
  const [tagFilter, setTagFilter] = useState('');
  const [showHeatLayer, setShowHeatLayer] = useState(true);
  
  // Map bounds (default to a sample area - can be updated based on user location)
  const [bounds, setBounds] = useState({
    minLat: 40.7128,
    minLng: -74.0060,
    maxLat: 40.7589,
    maxLng: -73.9441
  });

  const fetchHeatmapData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const bbox = `${bounds.minLat},${bounds.minLng},${bounds.maxLat},${bounds.maxLng}`;
      const params = new URLSearchParams({
        bbox,
        precision: precision[0].toString(),
        sinceHours: timeWindow,
        ...(tagFilter && { tag: tagFilter })
      });
      
      const response = await fetch(`http://localhost:8082/social/heatmap?${params}`);
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data: HeatmapResponse = await response.json();
      setHeatmapData(data.points || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch heatmap data');
      console.error('Error fetching heatmap data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHeatmapData();
  }, [timeWindow, precision, tagFilter]);

  const handleRefresh = () => {
    fetchHeatmapData();
  };

  const getHeatmapColor = (weight: number, maxWeight: number) => {
    const intensity = Math.min(weight / maxWeight, 1);
    if (intensity < 0.2) return 'rgba(0, 255, 0, 0.6)';
    if (intensity < 0.4) return 'rgba(255, 255, 0, 0.6)';
    if (intensity < 0.6) return 'rgba(255, 165, 0, 0.6)';
    if (intensity < 0.8) return 'rgba(255, 69, 0, 0.6)';
    return 'rgba(255, 0, 0, 0.8)';
  };

  const maxWeight = Math.max(...heatmapData.map(point => point.weight), 1);

  return (
    <div className="flex h-full">
      {/* Controls Panel */}
      <div className="w-80 bg-white border-r border-gray-200 p-4 overflow-y-auto">
        <div className="space-y-6">
          <div>
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Layers className="h-5 w-5 mr-2" />
              Heatmap Controls
            </h2>
          </div>

          {/* Heat Layer Toggle */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm flex items-center">
                <MapPin className="h-4 w-4 mr-2" />
                Heat Layer
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Button
                variant={showHeatLayer ? "default" : "outline"}
                size="sm"
                onClick={() => setShowHeatLayer(!showHeatLayer)}
                className="w-full"
              >
                {showHeatLayer ? 'Hide' : 'Show'} Heat Layer
              </Button>
            </CardContent>
          </Card>

          {/* Time Window Control */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm flex items-center">
                <Clock className="h-4 w-4 mr-2" />
                Time Window
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Select value={timeWindow} onValueChange={setTimeWindow}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {TIME_WINDOWS.map(window => (
                    <SelectItem key={window.value} value={window.value}>
                      {window.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </CardContent>
          </Card>

          {/* Precision Slider */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">
                Precision Level: {PRECISION_LEVELS.find(p => p.value === precision[0])?.label}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Slider
                value={precision}
                onValueChange={setPrecision}
                min={5}
                max={7}
                step={1}
                className="w-full"
              />
              <div className="flex justify-between text-xs text-muted-foreground mt-2">
                <span>City (5)</span>
                <span>District (6)</span>
                <span>Neighborhood (7)</span>
              </div>
            </CardContent>
          </Card>

          {/* Tag Filter */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm flex items-center">
                <Tag className="h-4 w-4 mr-2" />
                Tag Filter
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Input
                placeholder="Filter by tag (optional)"
                value={tagFilter}
                onChange={(e) => setTagFilter(e.target.value)}
              />
              {tagFilter && (
                <Badge variant="secondary" className="mt-2">
                  Filtering by: {tagFilter}
                </Badge>
              )}
            </CardContent>
          </Card>

          {/* Refresh Button */}
          <Button
            onClick={handleRefresh}
            disabled={loading}
            className="w-full"
            variant="outline"
          >
            <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
            {loading ? 'Loading...' : 'Refresh Data'}
          </Button>

          {/* Stats */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Statistics</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span>Data Points:</span>
                  <span className="font-medium">{heatmapData.length}</span>
                </div>
                <div className="flex justify-between">
                  <span>Max Weight:</span>
                  <span className="font-medium">{maxWeight.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Time Window:</span>
                  <span className="font-medium">{TIME_WINDOWS.find(w => w.value === timeWindow)?.label}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Map Area */}
      <div className="flex-1 relative">
        {error && (
          <div className="absolute top-4 left-4 right-4 z-10">
            <Card className="border-red-200 bg-red-50">
              <CardContent className="p-4">
                <p className="text-red-800 text-sm">{error}</p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Placeholder Map Area */}
        <div className="h-full bg-gray-100 flex items-center justify-center relative">
          <div className="text-center">
            <MapPin className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-600 mb-2">
              Interactive Map Coming Soon
            </h3>
            <p className="text-gray-500 mb-4">
              This will display an interactive map with heatmap overlay
            </p>
            <div className="text-sm text-gray-600">
              <p>Current bounds: {bounds.minLat.toFixed(4)}, {bounds.minLng.toFixed(4)} to {bounds.maxLat.toFixed(4)}, {bounds.maxLng.toFixed(4)}</p>
            </div>
          </div>

          {/* Heatmap Data Visualization (Simple Grid) */}
          {showHeatLayer && heatmapData.length > 0 && (
            <div className="absolute inset-4 overflow-hidden">
              <div className="grid grid-cols-8 gap-1 h-full">
                {heatmapData.slice(0, 64).map((point, index) => (
                  <div
                    key={point.geohash}
                    className="rounded-sm flex items-center justify-center text-xs font-medium text-white"
                    style={{
                      backgroundColor: getHeatmapColor(point.weight, maxWeight)
                    }}
                    title={`Geohash: ${point.geohash}\nWeight: ${point.weight.toFixed(2)}\nTags: ${point.tagsTop.join(', ')}`}
                  >
                    {point.weight.toFixed(1)}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}