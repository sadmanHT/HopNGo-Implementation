import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { 
  Cloud, 
  Sun, 
  CloudRain, 
  Wind, 
  Thermometer, 
  Eye, 
  Droplets,
  AlertTriangle,
  Shield,
  Info,
  RefreshCw
} from 'lucide-react';
import { BDDestination } from '@/data/bd-destinations';
import { useWeatherAdvisory } from '@/hooks/useWeatherAdvisory';
import { Button } from '@/components/ui/button';

interface WeatherAdvisoryCardProps {
  destination?: BDDestination;
  coordinates?: { lat: number; lng: number };
  className?: string;
  showForecast?: boolean;
  compact?: boolean;
}

export function WeatherAdvisoryCard({
  destination,
  coordinates,
  className = '',
  showForecast = true,
  compact = false
}: WeatherAdvisoryCardProps) {
  const { weather, advisories, loading, error, refetch } = useWeatherAdvisory(destination, coordinates);

  const getWeatherIcon = (condition: string) => {
    const lowerCondition = condition.toLowerCase();
    if (lowerCondition.includes('rain') || lowerCondition.includes('shower')) {
      return <CloudRain className="h-5 w-5" />;
    }
    if (lowerCondition.includes('cloud')) {
      return <Cloud className="h-5 w-5" />;
    }
    if (lowerCondition.includes('sun') || lowerCondition.includes('clear')) {
      return <Sun className="h-5 w-5" />;
    }
    return <Cloud className="h-5 w-5" />;
  };

  const getAdvisoryIcon = (type: string, source: string) => {
    if (source === 'weather') return <Cloud className="h-4 w-4" />;
    if (source === 'seasonal') return <Thermometer className="h-4 w-4" />;
    if (source === 'ai') return <Sun className="h-4 w-4" />;
    
    switch (type) {
      case 'danger':
        return <AlertTriangle className="h-4 w-4" />;
      case 'warning':
        return <Shield className="h-4 w-4" />;
      case 'info':
      default:
        return <Info className="h-4 w-4" />;
    }
  };

  const getAdvisoryVariant = (type: string) => {
    switch (type) {
      case 'danger':
        return 'destructive';
      case 'warning':
        return 'default';
      case 'info':
      default:
        return 'secondary';
    }
  };

  if (loading) {
    return (
      <Card className={className}>
        <CardHeader className={compact ? 'pb-2' : 'pb-4'}>
          <CardTitle className={compact ? 'text-base' : 'text-lg'}>Weather & Safety</CardTitle>
        </CardHeader>
        <CardContent className={compact ? 'pt-0' : 'pt-0'}>
          <div className="flex items-center space-x-2 text-sm text-muted-foreground">
            <Cloud className="h-4 w-4 animate-pulse" />
            <span>Loading weather information...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error && !weather && advisories.length === 0) {
    return (
      <Card className={className}>
        <CardHeader className={compact ? 'pb-2' : 'pb-4'}>
          <div className="flex items-center justify-between">
            <CardTitle className={compact ? 'text-base' : 'text-lg'}>Weather & Safety</CardTitle>
            <Button variant="ghost" size="sm" onClick={refetch}>
              <RefreshCw className="h-4 w-4" />
            </Button>
          </div>
        </CardHeader>
        <CardContent className={compact ? 'pt-0' : 'pt-0'}>
          <Alert variant="default">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              Weather service temporarily unavailable. {advisories.length > 0 && 'Showing seasonal advisories only.'}
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader className={compact ? 'pb-2' : 'pb-4'}>
        <div className="flex items-center justify-between">
          <CardTitle className={compact ? 'text-base' : 'text-lg'}>Weather & Safety</CardTitle>
          {weather && (
            <Button variant="ghost" size="sm" onClick={refetch}>
              <RefreshCw className="h-4 w-4" />
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className={compact ? 'pt-0 space-y-3' : 'pt-0 space-y-4'}>
        {/* Current Weather */}
        {weather && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                {getWeatherIcon(weather.current.condition)}
                <div>
                  <div className="font-semibold">{weather.current.temperature.toFixed(1)}°C</div>
                  <div className="text-sm text-muted-foreground">{weather.current.condition}</div>
                </div>
              </div>
              <div className="text-right text-sm text-muted-foreground">
                <div>Feels like {weather.current.feelsLike.toFixed(1)}°C</div>
                <div>Updated: {new Date(weather.lastUpdated).toLocaleTimeString()}</div>
              </div>
            </div>

            {!compact && (
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div className="flex items-center space-x-2">
                  <Droplets className="h-4 w-4 text-blue-500" />
                  <span>Humidity: {weather.current.humidity}%</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Wind className="h-4 w-4 text-gray-500" />
                  <span>Wind: {weather.current.windSpeed.toFixed(1)} km/h</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Eye className="h-4 w-4 text-gray-500" />
                  <span>Visibility: {weather.current.visibility.toFixed(1)} km</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Sun className="h-4 w-4 text-yellow-500" />
                  <span>UV Index: {weather.current.uvIndex.toFixed(1)}</span>
                </div>
              </div>
            )}

            {/* 3-day forecast */}
            {showForecast && !compact && weather.forecast && (
              <div className="space-y-2">
                <h4 className="font-medium text-sm">3-Day Forecast</h4>
                <div className="grid grid-cols-3 gap-2">
                  {weather.forecast.slice(0, 3).map((day, index) => (
                    <div key={index} className="text-center p-2 bg-muted/50 rounded">
                      <div className="text-xs text-muted-foreground mb-1">
                        {new Date(day.date).toLocaleDateString('en-US', { weekday: 'short' })}
                      </div>
                      {getWeatherIcon(day.condition)}
                      <div className="text-xs mt-1">
                        <div className="font-medium">{day.maxTemp.toFixed(0)}°</div>
                        <div className="text-muted-foreground">{day.minTemp.toFixed(0)}°</div>
                      </div>
                      {day.chanceOfRain > 30 && (
                        <div className="text-xs text-blue-600 mt-1">
                          {day.chanceOfRain}% rain
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Safety Advisories */}
        {advisories.length > 0 && (
          <div className="space-y-2">
            <h4 className="font-medium text-sm flex items-center space-x-2">
              <Shield className="h-4 w-4" />
              <span>Safety Advisories</span>
            </h4>
            <div className="space-y-2">
              {advisories.slice(0, compact ? 2 : 5).map((advisory, index) => (
                <Alert key={index} variant={getAdvisoryVariant(advisory.type) as any} className="py-2">
                  <div className="flex items-start gap-2">
                    {getAdvisoryIcon(advisory.type, advisory.source)}
                    <div className="flex-1">
                      <AlertDescription className="text-sm">
                        {advisory.message}
                      </AlertDescription>
                      <div className="flex items-center space-x-2 mt-1">
                        <Badge variant="outline" className="text-xs">
                          {advisory.source === 'weather' ? 'Live Weather' : 
                           advisory.source === 'seasonal' ? 'Seasonal' : 
                           advisory.source === 'ai' ? 'AI Advisory' : advisory.source}
                        </Badge>
                      </div>
                    </div>
                  </div>
                </Alert>
              ))}
              {advisories.length > (compact ? 2 : 5) && (
                <div className="text-xs text-muted-foreground text-center">
                  +{advisories.length - (compact ? 2 : 5)} more advisories
                </div>
              )}
            </div>
          </div>
        )}

        {/* No data message */}
        {!weather && advisories.length === 0 && !loading && (
          <div className="text-center text-sm text-muted-foreground py-4">
            <Info className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p>No weather or safety information available</p>
            <p className="text-xs mt-1">Check back later or try refreshing</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}