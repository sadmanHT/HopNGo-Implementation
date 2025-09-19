'use client';

import React, { useEffect, useRef, useState } from 'react';
import mapboxgl from 'mapbox-gl';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { useSocialStore } from '../../lib/state';
import { MapPin, Users, Camera, TrendingUp, Filter } from 'lucide-react';
import { Post } from '../../lib/state/social';
import { getBDDestinationsByRegion, bdDestinations } from '../../data/bd-destinations';

// Set Mapbox access token
mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN || '';

interface TouristHeatmapProps {
  className?: string;
  height?: string;
  showControls?: boolean;
  selectedRegion?: string;
}

interface HeatmapData {
  coordinates: [number, number];
  weight: number;
  posts: Post[];
  location: string;
}

interface MapFilters {
  timeRange: '24h' | '7d' | '30d' | 'all';
  postType: 'all' | 'photos' | 'text';
  minActivity: number;
  region: string;
}

export const TouristHeatmap: React.FC<TouristHeatmapProps> = ({
  className,
  height = '500px',
  showControls = true,
  selectedRegion = 'all'
}) => {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<mapboxgl.Map | null>(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState<HeatmapData | null>(null);
  const [filters, setFilters] = useState<MapFilters>({
    timeRange: '7d',
    postType: 'all',
    minActivity: 1,
    region: selectedRegion
  });
  const [heatmapData, setHeatmapData] = useState<HeatmapData[]>([]);
  const [stats, setStats] = useState({
    totalPosts: 0,
    activeLocations: 0,
    topLocation: ''
  });

  const { posts, loadFeed } = useSocialStore();

  useEffect(() => {
    if (!mapContainer.current || map.current) return;

    // Initialize map
    map.current = new mapboxgl.Map({
      container: mapContainer.current,
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [0, 20], // Default center
      zoom: 2,
      attributionControl: false
    });

    map.current.on('load', () => {
      setIsLoaded(true);
      setupHeatmapLayer();
      setupMarkers();
    });

    // Add navigation controls
    map.current.addControl(new mapboxgl.NavigationControl(), 'top-right');
    map.current.addControl(new mapboxgl.FullscreenControl(), 'top-right');

    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (posts.length > 0) {
      processHeatmapData();
    }
  }, [posts, filters]);

  useEffect(() => {
    setFilters(prev => ({ ...prev, region: selectedRegion }));
  }, [selectedRegion]);

  useEffect(() => {
    if (isLoaded && heatmapData.length > 0) {
      updateHeatmapLayer();
      updateMarkers();
    }
  }, [heatmapData, isLoaded]);

  const processHeatmapData = () => {
    const now = new Date();
    const timeRangeMs = {
      '24h': 24 * 60 * 60 * 1000,
      '7d': 7 * 24 * 60 * 60 * 1000,
      '30d': 30 * 24 * 60 * 60 * 1000,
      'all': Infinity
    }[filters.timeRange];

    // Get region-specific destinations for filtering
    const regionDestinations = filters.region === 'all' 
      ? bdDestinations 
      : getBDDestinationsByRegion(filters.region);

    // Filter posts based on criteria
    const filteredPosts = posts.filter(post => {
      // Time filter
      const postAge = now.getTime() - new Date(post.createdAt).getTime();
      if (postAge > timeRangeMs) return false;

      // Post type filter
      if (filters.postType === 'photos' && (!post.mediaUrls || post.mediaUrls.length === 0)) {
        return false;
      }
      if (filters.postType === 'text' && post.mediaUrls && post.mediaUrls.length > 0) {
        return false;
      }

      // Must have location
      if (!post.location || !post.location.coordinates) return false;

      // Region filter - check if post location is near any destination in the selected region
      if (filters.region !== 'all' && regionDestinations.length > 0) {
        const postLat = post.location.coordinates[1];
        const postLng = post.location.coordinates[0];
        
        // Check if post is within ~50km of any destination in the region
        const isInRegion = regionDestinations.some(dest => {
          const destLat = dest.coordinates.lat;
          const destLng = dest.coordinates.lng;
          const distance = Math.sqrt(
            Math.pow(postLat - destLat, 2) + Math.pow(postLng - destLng, 2)
          );
          return distance < 0.5; // Approximately 50km in degrees
        });
        
        if (!isInRegion) return false;
      }

      return true;
    });

    // Group posts by location
    const locationGroups = new Map<string, Post[]>();
    filteredPosts.forEach(post => {
      if (post.location?.coordinates) {
        const key = `${post.location.coordinates[0].toFixed(4)},${post.location.coordinates[1].toFixed(4)}`;
        if (!locationGroups.has(key)) {
          locationGroups.set(key, []);
        }
        locationGroups.get(key)!.push(post);
      }
    });

    // Convert to heatmap data
    const data: HeatmapData[] = [];
    locationGroups.forEach((posts, key) => {
      if (posts.length >= filters.minActivity) {
        const [lng, lat] = key.split(',').map(Number);
        const weight = Math.log(posts.length + 1) * 10; // Logarithmic scaling
        
        data.push({
          coordinates: [lng, lat],
          weight,
          posts,
          location: posts[0].location?.name || `${lat.toFixed(2)}, ${lng.toFixed(2)}`
        });
      }
    });

    setHeatmapData(data);

    // Update stats
    const topLocation = data.reduce((max, current) => 
      current.posts.length > max.posts.length ? current : max, 
      data[0] || { posts: [], location: 'None' }
    );

    setStats({
      totalPosts: filteredPosts.length,
      activeLocations: data.length,
      topLocation: topLocation?.location || 'None'
    });
  };

  const setupHeatmapLayer = () => {
    if (!map.current) return;

    // Add heatmap source
    map.current.addSource('heatmap-data', {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: []
      }
    });

    // Add heatmap layer
    map.current.addLayer({
      id: 'heatmap-layer',
      type: 'heatmap',
      source: 'heatmap-data',
      maxzoom: 15,
      paint: {
        'heatmap-weight': {
          property: 'weight',
          type: 'exponential',
          stops: [
            [1, 0],
            [100, 1]
          ]
        },
        'heatmap-intensity': [
          'interpolate',
          ['linear'],
          ['zoom'],
          11, 1,
          15, 3
        ] as any,
        'heatmap-color': [
          'interpolate',
          ['linear'],
          ['heatmap-density'],
          0, 'rgba(33,102,172,0)',
          0.2, 'rgb(103,169,207)',
          0.4, 'rgb(209,229,240)',
          0.6, 'rgb(253,219,199)',
          0.8, 'rgb(239,138,98)',
          1, 'rgb(178,24,43)'
        ],
        'heatmap-radius': {
          stops: [
            [11, 15],
            [15, 20]
          ]
        },
        'heatmap-opacity': [
          'interpolate',
          ['linear'],
          ['zoom'],
          14, 1,
          15, 0
        ] as any
      }
    });

    // Add circle layer for high zoom levels
    map.current.addLayer({
      id: 'heatmap-points',
      type: 'circle',
      source: 'heatmap-data',
      minzoom: 14,
      paint: {
        'circle-radius': {
          property: 'weight',
          type: 'exponential',
          stops: [
            [1, 4],
            [100, 20]
          ]
        },
        'circle-color': {
          property: 'weight',
          type: 'exponential',
          stops: [
            [1, 'rgba(33,102,172,0.8)'],
            [100, 'rgba(178,24,43,0.8)']
          ]
        },
        'circle-stroke-color': 'white',
        'circle-stroke-width': 1,
        'circle-opacity': {
          stops: [
            [14, 0],
            [15, 1]
          ]
        }
      }
    });
  };

  const updateHeatmapLayer = () => {
    if (!map.current || !map.current.getSource('heatmap-data')) return;

    const features = heatmapData.map(data => ({
      type: 'Feature' as const,
      properties: {
        weight: data.weight,
        postCount: data.posts.length,
        location: data.location
      },
      geometry: {
        type: 'Point' as const,
        coordinates: data.coordinates
      }
    }));

    (map.current.getSource('heatmap-data') as mapboxgl.GeoJSONSource).setData({
      type: 'FeatureCollection',
      features
    });
  };

  const setupMarkers = () => {
    if (!map.current) return;

    // Add click handler for points
    map.current.on('click', 'heatmap-points', (e) => {
      if (e.features && e.features[0]) {
        const feature = e.features[0];
        const coordinates = (feature.geometry as any).coordinates.slice();
        const properties = feature.properties;
        
        // Find corresponding heatmap data
        const locationData = heatmapData.find(data => 
          Math.abs(data.coordinates[0] - coordinates[0]) < 0.001 &&
          Math.abs(data.coordinates[1] - coordinates[1]) < 0.001
        );
        
        if (locationData) {
          setSelectedLocation(locationData);
          
          // Create popup
          new mapboxgl.Popup()
            .setLngLat(coordinates)
            .setHTML(`
              <div class="p-3">
                <h3 class="font-semibold text-sm mb-2">${properties?.location || 'Unknown Location'}</h3>
                <p class="text-xs text-gray-600 mb-2">${properties?.postCount || 0} posts</p>
                <div class="text-xs">
                  <div class="flex items-center gap-1 mb-1">
                    <span>📍</span>
                    <span>${coordinates[1].toFixed(4)}, ${coordinates[0].toFixed(4)}</span>
                  </div>
                </div>
              </div>
            `)
            .addTo(map.current!);
        }
      }
    });

    // Change cursor on hover
    map.current.on('mouseenter', 'heatmap-points', () => {
      if (map.current) {
        map.current.getCanvas().style.cursor = 'pointer';
      }
    });

    map.current.on('mouseleave', 'heatmap-points', () => {
      if (map.current) {
        map.current.getCanvas().style.cursor = '';
      }
    });
  };

  const updateMarkers = () => {
    // Markers are handled by the heatmap-points layer
  };

  const handleFilterChange = (key: keyof MapFilters, value: any) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const fitToData = () => {
    if (!map.current || heatmapData.length === 0) return;

    const bounds = new mapboxgl.LngLatBounds();
    heatmapData.forEach(data => {
      bounds.extend(data.coordinates);
    });

    map.current.fitBounds(bounds, {
      padding: 50,
      maxZoom: 10
    });
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center space-x-2">
              <Camera className="h-4 w-4 text-blue-500" />
              <div>
                <p className="text-2xl font-bold">{stats.totalPosts}</p>
                <p className="text-xs text-muted-foreground">Total Posts</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center space-x-2">
              <MapPin className="h-4 w-4 text-green-500" />
              <div>
                <p className="text-2xl font-bold">{stats.activeLocations}</p>
                <p className="text-xs text-muted-foreground">Active Locations</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center space-x-2">
              <TrendingUp className="h-4 w-4 text-orange-500" />
              <div>
                <p className="text-sm font-bold truncate">{stats.topLocation}</p>
                <p className="text-xs text-muted-foreground">Top Location</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Controls */}
      {showControls && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Filter className="h-4 w-4" />
              Filters
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-4">
              {/* Time Range */}
              <div className="space-y-2">
                <label className="text-sm font-medium">Time Range</label>
                <div className="flex gap-2">
                  {(['24h', '7d', '30d', 'all'] as const).map(range => (
                    <Button
                      key={range}
                      variant={filters.timeRange === range ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handleFilterChange('timeRange', range)}
                    >
                      {range === 'all' ? 'All Time' : range}
                    </Button>
                  ))}
                </div>
              </div>

              {/* Post Type */}
              <div className="space-y-2">
                <label className="text-sm font-medium">Post Type</label>
                <div className="flex gap-2">
                  {(['all', 'photos', 'text'] as const).map(type => (
                    <Button
                      key={type}
                      variant={filters.postType === type ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => handleFilterChange('postType', type)}
                    >
                      {type.charAt(0).toUpperCase() + type.slice(1)}
                    </Button>
                  ))}
                </div>
              </div>

              {/* Actions */}
              <div className="space-y-2">
                <label className="text-sm font-medium">Actions</label>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={fitToData}>
                    <MapPin className="h-4 w-4 mr-2" />
                    Fit to Data
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => loadFeed()}>
                    <Users className="h-4 w-4 mr-2" />
                    Refresh
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Map */}
      <Card>
        <CardContent className="p-0">
          <div 
            ref={mapContainer} 
            className="w-full rounded-lg overflow-hidden"
            style={{ height }}
          />
          {!isLoaded && (
            <div className="absolute inset-0 flex items-center justify-center bg-gray-100 rounded-lg">
              <div className="text-center space-y-2">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
                <p className="text-sm text-muted-foreground">Loading map...</p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Selected Location Details */}
      {selectedLocation && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <MapPin className="h-4 w-4" />
              {selectedLocation.location}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <Badge variant="secondary">
                  {selectedLocation.posts.length} posts
                </Badge>
                <span>
                  {selectedLocation.coordinates[1].toFixed(4)}, {selectedLocation.coordinates[0].toFixed(4)}
                </span>
              </div>
              
              {/* Recent posts preview */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                {selectedLocation.posts.slice(0, 4).map((post, index) => (
                  <div key={post.id} className="space-y-1">
                    {post.mediaUrls && post.mediaUrls[0] ? (
                      <div className="aspect-square rounded-lg overflow-hidden">
                        <img
                          src={post.mediaUrls[0]}
                          alt={`Post ${index + 1}`}
                          className="w-full h-full object-cover"
                        />
                      </div>
                    ) : (
                      <div className="aspect-square rounded-lg bg-gray-100 flex items-center justify-center">
                        <span className="text-2xl">📝</span>
                      </div>
                    )}
                    <p className="text-xs text-muted-foreground truncate">
                      {post.content?.substring(0, 30) || 'No content'}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};