'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { MapPin, Layers, Clock, Tag, RefreshCw, Route, Save, Navigation, DollarSign, Eye, EyeOff, Filter, Car, Bike, PersonStanding } from 'lucide-react';
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

interface Waypoint {
  lat: number;
  lng: number;
  name?: string;
}

interface RouteData {
  geometry: string;
  distance: number;
  duration: number;
  distanceKm: number;
  durationMin: number;
}

interface SavedRoute {
  id: string;
  name: string;
  waypoints: Waypoint[];
  distanceKm: number;
  durationMin: number;
  mode: string;
  createdAt: string;
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

const TRANSPORT_MODES = [
  { value: 'driving', label: 'Driving', icon: '🚗' },
  { value: 'walking', label: 'Walking', icon: '🚶' },
  { value: 'cycling', label: 'Cycling', icon: '🚴' }
];

// Cost estimates per km (in local currency)
const COST_ESTIMATES = {
  driving: { fuel: 12, maintenance: 8, total: 20 }, // BDT per km
  walking: { calories: 50, time: 12, total: 0 }, // calories per km, minutes per km
  cycling: { calories: 30, time: 4, total: 2 } // calories per km, minutes per km, minimal cost
};

export default function MapPage() {
  const [heatmapData, setHeatmapData] = useState<HeatmapPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Heatmap controls state
  const [timeWindow, setTimeWindow] = useState('72');
  const [precision, setPrecision] = useState([6]);
  const [tagFilter, setTagFilter] = useState('');
  const [showHeatLayer, setShowHeatLayer] = useState(true);
  
  // Pro Mode state
  const [proMode, setProMode] = useState(false);
  const [waypoints, setWaypoints] = useState<Waypoint[]>([]);
  const [transportMode, setTransportMode] = useState('driving');
  const [routeData, setRouteData] = useState<RouteData | null>(null);
  const [routeLoading, setRouteLoading] = useState(false);
  const [savedRoutes, setSavedRoutes] = useState<SavedRoute[]>([]);
  const [routeName, setRouteName] = useState('');
  const [showSavedRoutes, setShowSavedRoutes] = useState(false);
  
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

  const calculateRoute = async () => {
    if (waypoints.length < 2) return;
    
    setRouteLoading(true);
    setError(null);
    
    try {
      const coordinates = waypoints.map(wp => `${wp.lng},${wp.lat}`).join(';');
      const params = new URLSearchParams({
        coordinates,
        profile: transportMode,
        geometries: 'geojson',
        steps: 'true',
        alternatives: 'false'
      });
      
      const response = await fetch(`http://localhost:8081/geo/route?${params}`);
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      if (data.routes && data.routes.length > 0) {
        const route = data.routes[0];
        setRouteData({
          geometry: JSON.stringify(route.geometry),
          distance: route.distance,
          duration: route.duration,
          distanceKm: route.distanceKm,
          durationMin: route.durationMin
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to calculate route');
      console.error('Error calculating route:', err);
    } finally {
      setRouteLoading(false);
    }
  };

  const saveRoute = async () => {
    if (!routeData || !routeName.trim() || waypoints.length < 2) return;
    
    try {
      const response = await fetch('http://localhost:8083/api/v1/saved-routes', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}` // Assuming JWT token
        },
        body: JSON.stringify({
          name: routeName.trim(),
          waypoints: waypoints,
          distanceKm: routeData.distanceKm,
          durationMin: routeData.durationMin,
          mode: transportMode
        })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const savedRoute = await response.json();
      setSavedRoutes(prev => [savedRoute, ...prev]);
      setRouteName('');
      alert('Route saved successfully!');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save route');
      console.error('Error saving route:', err);
    }
  };

  const loadSavedRoutes = async () => {
    try {
      const response = await fetch('http://localhost:8083/api/v1/saved-routes', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      setSavedRoutes(data.content || []);
    } catch (err) {
      console.error('Error loading saved routes:', err);
    }
  };

  const loadSavedRoute = (savedRoute: SavedRoute) => {
    setWaypoints(savedRoute.waypoints);
    setTransportMode(savedRoute.mode);
    setRouteData({
      geometry: '',
      distance: savedRoute.distanceKm * 1000,
      duration: savedRoute.durationMin * 60,
      distanceKm: savedRoute.distanceKm,
      durationMin: savedRoute.durationMin
    });
  };

  const addWaypoint = (lat: number, lng: number) => {
    const newWaypoint: Waypoint = { lat, lng, name: `Point ${waypoints.length + 1}` };
    setWaypoints(prev => [...prev, newWaypoint]);
  };

  const removeWaypoint = (index: number) => {
    setWaypoints(prev => prev.filter((_, i) => i !== index));
    setRouteData(null);
  };

  const clearRoute = () => {
    setWaypoints([]);
    setRouteData(null);
    setRouteName('');
  };

  const getCostEstimate = () => {
    if (!routeData) return null;
    
    const distance = routeData.distanceKm;
    
    if (transportMode === 'driving') {
      const costs = COST_ESTIMATES.driving;
      return {
        fuel: (costs.fuel * distance).toFixed(0),
        maintenance: (costs.maintenance * distance).toFixed(0),
        total: (costs.total * distance).toFixed(0)
      };
    } else {
      const costs = COST_ESTIMATES[transportMode as 'walking' | 'cycling'];
      return {
        calories: (costs.calories * distance).toFixed(0),
        time: routeData.durationMin,
        total: (costs.total * distance).toFixed(0)
      };
    }
  };

  useEffect(() => {
    fetchHeatmapData();
  }, [timeWindow, precision, tagFilter]);

  useEffect(() => {
    if (proMode && waypoints.length >= 2) {
      calculateRoute();
    }
  }, [waypoints, transportMode, proMode]);

  useEffect(() => {
    if (proMode && showSavedRoutes) {
      loadSavedRoutes();
    }
  }, [proMode, showSavedRoutes]);

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
          {/* Pro Mode Toggle */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm flex items-center justify-between">
                <span className="flex items-center">
                  <Route className="h-4 w-4 mr-2" />
                  Pro Mode
                </span>
                <Switch
                  checked={proMode}
                  onCheckedChange={setProMode}
                />
              </CardTitle>
            </CardHeader>
            {proMode && (
              <CardContent>
                <p className="text-xs text-muted-foreground">
                  Advanced routing with cost estimates and saved routes
                </p>
              </CardContent>
            )}
          </Card>

          {proMode ? (
            // Pro Mode Controls
            <>
              {/* Transportation Mode */}
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm flex items-center">
                    <Navigation className="h-4 w-4 mr-2" />
                    Transportation
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <Select value={transportMode} onValueChange={setTransportMode}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {TRANSPORT_MODES.map(mode => (
                        <SelectItem key={mode.value} value={mode.value}>
                          {mode.icon} {mode.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </CardContent>
              </Card>

              {/* Waypoints */}
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm flex items-center justify-between">
                    <span className="flex items-center">
                      <MapPin className="h-4 w-4 mr-2" />
                      Waypoints ({waypoints.length})
                    </span>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={clearRoute}
                      disabled={waypoints.length === 0}
                    >
                      Clear
                    </Button>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2 max-h-32 overflow-y-auto">
                    {waypoints.map((waypoint, index) => (
                      <div key={index} className="flex items-center justify-between text-xs bg-gray-50 p-2 rounded">
                        <span>
                          {waypoint.name || `Point ${index + 1}`}
                        </span>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => removeWaypoint(index)}
                          className="h-6 w-6 p-0"
                        >
                          ×
                        </Button>
                      </div>
                    ))}
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => addWaypoint(23.8103 + Math.random() * 0.01, 90.4125 + Math.random() * 0.01)}
                    className="w-full mt-2"
                  >
                    Add Sample Point
                  </Button>
                </CardContent>
              </Card>

              {/* Route Info */}
              {routeData && (
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm flex items-center">
                      <Route className="h-4 w-4 mr-2" />
                      Route Details
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span>Distance:</span>
                        <span className="font-medium">{routeData.distanceKm.toFixed(2)} km</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Duration:</span>
                        <span className="font-medium">{routeData.durationMin} min</span>
                      </div>
                      {routeLoading && (
                        <div className="text-center text-muted-foreground">
                          Calculating route...
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Cost Estimates */}
              {routeData && (
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm flex items-center">
                      <DollarSign className="h-4 w-4 mr-2" />
                      Cost Estimate
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    {(() => {
                      const costs = getCostEstimate();
                      if (!costs) return null;
                      
                      return (
                        <div className="space-y-2 text-sm">
                          {transportMode === 'driving' ? (
                            <>
                              <div className="flex justify-between">
                                <span>Fuel:</span>
                                <span className="font-medium">৳{costs.fuel}</span>
                              </div>
                              <div className="flex justify-between">
                                <span>Maintenance:</span>
                                <span className="font-medium">৳{costs.maintenance}</span>
                              </div>
                              <div className="flex justify-between border-t pt-2">
                                <span className="font-medium">Total:</span>
                                <span className="font-bold">৳{costs.total}</span>
                              </div>
                            </>
                          ) : (
                            <>
                              <div className="flex justify-between">
                                <span>Calories:</span>
                                <span className="font-medium">{costs.calories} cal</span>
                              </div>
                              <div className="flex justify-between">
                                <span>Time:</span>
                                <span className="font-medium">{costs.time} min</span>
                              </div>
                              {transportMode === 'cycling' && (
                                <div className="flex justify-between border-t pt-2">
                                  <span className="font-medium">Cost:</span>
                                  <span className="font-bold">৳{costs.total}</span>
                                </div>
                              )}
                            </>
                          )}
                        </div>
                      );
                    })()}
                  </CardContent>
                </Card>
              )}

              {/* Save Route */}
              {routeData && waypoints.length >= 2 && (
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm flex items-center">
                      <Save className="h-4 w-4 mr-2" />
                      Save Route
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <Input
                        placeholder="Route name"
                        value={routeName}
                        onChange={(e) => setRouteName(e.target.value)}
                      />
                      <Button
                        size="sm"
                        onClick={saveRoute}
                        disabled={!routeName.trim()}
                        className="w-full"
                      >
                        Save Route
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Saved Routes */}
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm flex items-center justify-between">
                    <span className="flex items-center">
                      <Save className="h-4 w-4 mr-2" />
                      Saved Routes
                    </span>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setShowSavedRoutes(!showSavedRoutes)}
                    >
                      {showSavedRoutes ? 'Hide' : 'Show'}
                    </Button>
                  </CardTitle>
                </CardHeader>
                {showSavedRoutes && (
                  <CardContent>
                    <div className="space-y-2 max-h-40 overflow-y-auto">
                      {savedRoutes.length === 0 ? (
                        <p className="text-xs text-muted-foreground text-center py-4">
                          No saved routes yet
                        </p>
                      ) : (
                        savedRoutes.map((route) => (
                          <div
                            key={route.id}
                            className="p-2 bg-gray-50 rounded cursor-pointer hover:bg-gray-100"
                            onClick={() => loadSavedRoute(route)}
                          >
                            <div className="text-xs font-medium">{route.name}</div>
                            <div className="text-xs text-muted-foreground">
                              {route.distanceKm.toFixed(1)}km • {route.durationMin}min • {route.mode}
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </CardContent>
                )}
              </Card>
            </>
          ) : (
            // Heatmap Controls
            <>
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
        </>
      )}
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

        {/* Interactive Map Area */}
        <div 
          className="h-full bg-gray-100 flex items-center justify-center relative cursor-crosshair"
          onClick={(e) => {
            if (proMode) {
              const rect = e.currentTarget.getBoundingClientRect();
              const x = e.clientX - rect.left;
              const y = e.clientY - rect.top;
              // Convert pixel coordinates to lat/lng (simplified)
              const lat = 23.8103 + (0.5 - y / rect.height) * 0.1;
              const lng = 90.4125 + (x / rect.width - 0.5) * 0.1;
              addWaypoint(lat, lng);
            }
          }}
        >
          <div className="text-center">
            <MapPin className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-600 mb-2">
              {proMode ? 'Click to Add Waypoints' : 'Interactive Map Coming Soon'}
            </h3>
            <p className="text-gray-500 mb-4">
              {proMode 
                ? 'Click anywhere on the map to add waypoints for routing'
                : 'This will display an interactive map with heatmap overlay'
              }
            </p>
            <div className="text-sm text-gray-600">
              <p>Current bounds: {bounds.minLat.toFixed(4)}, {bounds.minLng.toFixed(4)} to {bounds.maxLat.toFixed(4)}, {bounds.maxLng.toFixed(4)}</p>
            </div>
          </div>

          {/* Pro Mode Waypoint Visualization */}
          {proMode && waypoints.length > 0 && (
            <div className="absolute inset-4">
              {waypoints.map((waypoint, index) => {
                // Convert lat/lng to pixel coordinates (simplified)
                const x = ((waypoint.lng - 90.4125) / 0.1 + 0.5) * 100;
                const y = (0.5 - (waypoint.lat - 23.8103) / 0.1) * 100;
                return (
                  <div
                    key={index}
                    className="absolute w-8 h-8 bg-blue-500 text-white rounded-full flex items-center justify-center text-sm font-bold shadow-lg transform -translate-x-1/2 -translate-y-1/2"
                    style={{
                      left: `${Math.max(0, Math.min(100, x))}%`,
                      top: `${Math.max(0, Math.min(100, y))}%`
                    }}
                    title={`Waypoint ${index + 1}: ${waypoint.lat.toFixed(6)}, ${waypoint.lng.toFixed(6)}`}
                  >
                    {index + 1}
                  </div>
                );
              })}
              
              {/* Route Line Visualization */}
              {routeData && waypoints.length > 1 && (
                <svg className="absolute inset-0 w-full h-full pointer-events-none">
                  <polyline
                    points={waypoints.map((waypoint, index) => {
                      const x = ((waypoint.lng - 90.4125) / 0.1 + 0.5) * 100;
                      const y = (0.5 - (waypoint.lat - 23.8103) / 0.1) * 100;
                      return `${Math.max(0, Math.min(100, x))},${Math.max(0, Math.min(100, y))}`;
                    }).join(' ')}
                    fill="none"
                    stroke="#3b82f6"
                    strokeWidth="3"
                    strokeDasharray="5,5"
                    className="animate-pulse"
                  />
                </svg>
              )}
            </div>
          )}

          {/* Heatmap Data Visualization (Simple Grid) */}
          {!proMode && showHeatLayer && heatmapData.length > 0 && (
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