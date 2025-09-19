import { useState, useEffect } from 'react';
import { BDDestination } from '@/data/bd-destinations';

interface WeatherData {
  current: {
    temperature: number;
    feelsLike: number;
    humidity: number;
    windSpeed: number;
    windDirection: string;
    condition: string;
    icon: string;
    visibility: number;
    uvIndex: number;
  };
  forecast: Array<{
    date: string;
    maxTemp: number;
    minTemp: number;
    condition: string;
    icon: string;
    chanceOfRain: number;
    windSpeed: number;
  }>;
  location: string;
  timezone: string;
  lastUpdated: string;
}

interface SafetyAdvisory {
  type: 'info' | 'warning' | 'danger';
  message: string;
  source: 'weather' | 'seasonal' | 'ai';
  priority: number;
}

interface WeatherAdvisoryResult {
  weather: WeatherData | null;
  advisories: SafetyAdvisory[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8088';

export function useWeatherAdvisory(
  destination?: BDDestination,
  coordinates?: { lat: number; lng: number }
): WeatherAdvisoryResult {
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [advisories, setAdvisories] = useState<SafetyAdvisory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchWeatherData = async (lat: number, lng: number): Promise<WeatherData | null> => {
    try {
      const response = await fetch(
        `${AI_SERVICE_URL}/api/v1/ai/weather?lat=${lat}&lng=${lng}`,
        {
          headers: {
            'X-User-Id': 'anonymous', // In real app, get from auth context
            'Content-Type': 'application/json',
          },
        }
      );

      if (!response.ok) {
        throw new Error(`Weather API error: ${response.status}`);
      }

      return await response.json();
    } catch (err) {
      console.warn('Weather API unavailable, using fallback data:', err);
      return null;
    }
  };

  const generateSeasonalAdvisories = (dest?: BDDestination): SafetyAdvisory[] => {
    if (!dest) return [];

    const currentMonth = new Date().getMonth() + 1; // 1-12
    const advisories: SafetyAdvisory[] = [];

    // Monsoon season advisories (June-September)
    if (currentMonth >= 6 && currentMonth <= 9) {
      if (['srimangal', 'ratargul', 'sylhet'].includes(dest.slug)) {
        advisories.push({
          type: 'warning',
          message: 'Monsoon season: Heavy rainfall expected. Roads may be waterlogged and transportation delayed.',
          source: 'seasonal',
          priority: 2
        });
      }
      
      if (dest.slug === 'sundarbans') {
        advisories.push({
          type: 'danger',
          message: 'Cyclone season: Monitor weather updates closely. Consider postponing if severe weather warnings issued.',
          source: 'seasonal',
          priority: 1
        });
      }

      if (['coxs-bazar', 'kuakata', 'saint-martin'].includes(dest.slug)) {
        advisories.push({
          type: 'warning',
          message: 'Monsoon season: Beach activities may be restricted due to rough seas and strong currents.',
          source: 'seasonal',
          priority: 2
        });
      }
    }

    // Winter season advisories (December-February)
    if (currentMonth >= 12 || currentMonth <= 2) {
      if (['sajek-valley', 'bandarban', 'rangamati'].includes(dest.slug)) {
        advisories.push({
          type: 'info',
          message: 'Winter season: Pack warm clothes. Temperature can drop to 5-10°C at night in hill areas.',
          source: 'seasonal',
          priority: 3
        });
      }

      if (dest.slug === 'sundarbans') {
        advisories.push({
          type: 'info',
          message: 'Best time to visit: Clear skies and comfortable temperatures. Wildlife more active during cooler months.',
          source: 'seasonal',
          priority: 3
        });
      }
    }

    // Summer season advisories (March-May)
    if (currentMonth >= 3 && currentMonth <= 5) {
      advisories.push({
        type: 'warning',
        message: 'Summer season: High temperatures (35-40°C) and humidity. Stay hydrated and avoid midday sun.',
        source: 'seasonal',
        priority: 2
      });

      if (['coxs-bazar', 'kuakata'].includes(dest.slug)) {
        advisories.push({
          type: 'info',
          message: 'Peak beach season: Expect crowds. Book accommodations in advance.',
          source: 'seasonal',
          priority: 3
        });
      }
    }

    // Coastal tide warnings (year-round)
    if (['coxs-bazar', 'kuakata', 'saint-martin'].includes(dest.slug)) {
      advisories.push({
        type: 'info',
        message: 'Check tide schedules before beach activities. High tide times vary daily.',
        source: 'seasonal',
        priority: 3
      });
    }

    return advisories;
  };

  const generateWeatherAdvisories = (weatherData: WeatherData): SafetyAdvisory[] => {
    const advisories: SafetyAdvisory[] = [];
    const { current, forecast } = weatherData;

    // High temperature warning
    if (current.temperature > 35) {
      advisories.push({
        type: 'warning',
        message: `High temperature alert: ${current.temperature.toFixed(1)}°C. Stay hydrated and seek shade during peak hours.`,
        source: 'weather',
        priority: 2
      });
    }

    // High UV index warning
    if (current.uvIndex > 8) {
      advisories.push({
        type: 'warning',
        message: `Very high UV index: ${current.uvIndex.toFixed(1)}. Use sunscreen SPF 30+ and protective clothing.`,
        source: 'weather',
        priority: 2
      });
    }

    // High wind warning
    if (current.windSpeed > 25) {
      advisories.push({
        type: 'warning',
        message: `Strong winds: ${current.windSpeed.toFixed(1)} km/h. Be cautious with outdoor activities and water sports.`,
        source: 'weather',
        priority: 2
      });
    }

    // Rain forecast warning
    const rainDays = forecast.filter(day => day.chanceOfRain > 70).length;
    if (rainDays >= 3) {
      advisories.push({
        type: 'info',
        message: `Heavy rain expected for ${rainDays} days. Pack waterproof gear and plan indoor alternatives.`,
        source: 'weather',
        priority: 3
      });
    }

    // Low visibility warning
    if (current.visibility < 5) {
      advisories.push({
        type: 'warning',
        message: `Low visibility: ${current.visibility.toFixed(1)} km. Exercise caution when driving or hiking.`,
        source: 'weather',
        priority: 2
      });
    }

    return advisories;
  };

  const fetchData = async () => {
    if (!destination && !coordinates) return;

    setLoading(true);
    setError(null);

    try {
      let lat: number, lng: number;
      
      if (coordinates) {
        lat = coordinates.lat;
        lng = coordinates.lng;
      } else if (destination?.coordinates) {
        lat = destination.coordinates.lat;
        lng = destination.coordinates.lng;
      } else {
        throw new Error('No coordinates available');
      }

      // Fetch weather data
      const weatherData = await fetchWeatherData(lat, lng);
      setWeather(weatherData);

      // Generate advisories
      const seasonalAdvisories = generateSeasonalAdvisories(destination);
      const weatherAdvisories = weatherData ? generateWeatherAdvisories(weatherData) : [];
      
      // Combine and sort by priority
      const allAdvisories = [...seasonalAdvisories, ...weatherAdvisories]
        .sort((a, b) => a.priority - b.priority);
      
      setAdvisories(allAdvisories);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch weather advisory';
      setError(errorMessage);
      
      // Still provide seasonal advisories even if weather API fails
      const fallbackAdvisories = generateSeasonalAdvisories(destination);
      setAdvisories(fallbackAdvisories);
    } finally {
      setLoading(false);
    }
  };

  const refetch = () => {
    fetchData();
  };

  useEffect(() => {
    fetchData();
  }, [destination?.slug, coordinates?.lat, coordinates?.lng]);

  return {
    weather,
    advisories,
    loading,
    error,
    refetch
  };
}