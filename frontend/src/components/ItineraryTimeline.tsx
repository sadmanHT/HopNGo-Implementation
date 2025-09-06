'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  MapPin,
  Clock,
  DollarSign,
  Camera,
  Utensils,
  Bed,
  Car,
  Star,
  Info
} from 'lucide-react';

interface ItineraryTimelineProps {
  plan: Record<string, any>;
}

interface DayPlan {
  day: number;
  date?: string;
  activities: Activity[];
  accommodation?: Accommodation;
  transportation?: Transportation;
  totalCost?: number;
}

interface Activity {
  time?: string;
  name: string;
  type: 'sightseeing' | 'dining' | 'activity' | 'shopping' | 'rest' | 'other';
  location?: string;
  description?: string;
  cost?: number;
  duration?: string;
  rating?: number;
}

interface Accommodation {
  name: string;
  type: string;
  location?: string;
  cost?: number;
  rating?: number;
}

interface Transportation {
  type: string;
  from?: string;
  to?: string;
  cost?: number;
  duration?: string;
}

const getActivityIcon = (type: string) => {
  switch (type) {
    case 'sightseeing':
      return <Camera className="h-4 w-4" />;
    case 'dining':
      return <Utensils className="h-4 w-4" />;
    case 'accommodation':
      return <Bed className="h-4 w-4" />;
    case 'transportation':
      return <Car className="h-4 w-4" />;
    default:
      return <MapPin className="h-4 w-4" />;
  }
};

const getActivityColor = (type: string) => {
  switch (type) {
    case 'sightseeing':
      return 'bg-blue-100 text-blue-800';
    case 'dining':
      return 'bg-orange-100 text-orange-800';
    case 'activity':
      return 'bg-green-100 text-green-800';
    case 'shopping':
      return 'bg-purple-100 text-purple-800';
    case 'rest':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-indigo-100 text-indigo-800';
  }
};

const formatCost = (cost?: number) => {
  if (!cost) return null;
  return `$${cost.toLocaleString()}`;
};

const renderStars = (rating?: number) => {
  if (!rating) return null;
  return (
    <div className="flex items-center space-x-1">
      {[...Array(5)].map((_, i) => (
        <Star
          key={i}
          className={`h-3 w-3 ${
            i < rating ? 'text-yellow-400 fill-current' : 'text-gray-300'
          }`}
        />
      ))}
      <span className="text-xs text-gray-600 ml-1">{rating}/5</span>
    </div>
  );
};

export default function ItineraryTimeline({ plan }: ItineraryTimelineProps) {
  // Handle different plan structures from AI service
  const parsePlan = (planData: Record<string, any>): DayPlan[] => {
    // If plan has a 'days' array
    if (planData.days && Array.isArray(planData.days)) {
      return planData.days.map((day: any, index: number) => ({
        day: index + 1,
        date: day.date,
        activities: day.activities || [],
        accommodation: day.accommodation,
        transportation: day.transportation,
        totalCost: day.totalCost
      }));
    }
    
    // If plan has day keys like 'day1', 'day2', etc.
    const dayKeys = Object.keys(planData).filter(key => key.startsWith('day'));
    if (dayKeys.length > 0) {
      return dayKeys.map((key, index) => {
        const dayData = planData[key];
        return {
          day: index + 1,
          date: dayData.date,
          activities: dayData.activities || [],
          accommodation: dayData.accommodation,
          transportation: dayData.transportation,
          totalCost: dayData.totalCost
        };
      });
    }
    
    // If plan has itinerary array
    if (planData.itinerary && Array.isArray(planData.itinerary)) {
      return planData.itinerary.map((day: any, index: number) => ({
        day: index + 1,
        date: day.date,
        activities: day.activities || day.events || [],
        accommodation: day.accommodation || day.hotel,
        transportation: day.transportation || day.transport,
        totalCost: day.totalCost || day.cost
      }));
    }
    
    // Fallback: try to extract any meaningful structure
    return [];
  };

  const days = parsePlan(plan);

  if (days.length === 0) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <div className="text-center">
            <Info className="h-8 w-8 text-gray-400 mx-auto mb-2" />
            <p className="text-gray-600">No detailed itinerary available</p>
            <div className="bg-gray-50 p-4 rounded-lg mt-4">
              <pre className="text-xs whitespace-pre-wrap text-left">
                {JSON.stringify(plan, null, 2)}
              </pre>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {days.map((day, dayIndex) => (
        <Card key={dayIndex} className="overflow-hidden">
          <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50">
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center space-x-2">
                <div className="bg-blue-600 text-white rounded-full w-8 h-8 flex items-center justify-center text-sm font-bold">
                  {day.day}
                </div>
                <span>Day {day.day}</span>
                {day.date && (
                  <span className="text-sm text-gray-600 font-normal">({day.date})</span>
                )}
              </CardTitle>
              {day.totalCost && (
                <Badge variant="secondary" className="flex items-center space-x-1">
                  <DollarSign className="h-3 w-3" />
                  <span>{formatCost(day.totalCost)}</span>
                </Badge>
              )}
            </div>
          </CardHeader>
          <CardContent className="p-0">
            {/* Activities Timeline */}
            <div className="relative">
              {day.activities.map((activity, activityIndex) => (
                <div key={activityIndex} className="relative flex items-start space-x-4 p-4 border-b border-gray-100 last:border-b-0">
                  {/* Timeline dot */}
                  <div className="flex-shrink-0 mt-1">
                    <div className="w-8 h-8 bg-white border-2 border-blue-200 rounded-full flex items-center justify-center">
                      {getActivityIcon(activity.type)}
                    </div>
                  </div>
                  
                  {/* Timeline line */}
                  {activityIndex < day.activities.length - 1 && (
                    <div className="absolute left-7 top-12 w-0.5 h-full bg-gray-200" />
                  )}
                  
                  {/* Activity content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-1">
                          <h4 className="font-medium text-gray-900">{activity.name}</h4>
                          <Badge className={getActivityColor(activity.type)}>
                            {activity.type}
                          </Badge>
                          {activity.time && (
                            <div className="flex items-center space-x-1 text-sm text-gray-500">
                              <Clock className="h-3 w-3" />
                              <span>{activity.time}</span>
                            </div>
                          )}
                        </div>
                        
                        {activity.location && (
                          <div className="flex items-center space-x-1 text-sm text-gray-600 mb-1">
                            <MapPin className="h-3 w-3" />
                            <span>{activity.location}</span>
                          </div>
                        )}
                        
                        {activity.description && (
                          <p className="text-sm text-gray-600 mb-2">{activity.description}</p>
                        )}
                        
                        <div className="flex items-center space-x-4">
                          {activity.duration && (
                            <span className="text-xs text-gray-500">
                              Duration: {activity.duration}
                            </span>
                          )}
                          {activity.rating && renderStars(activity.rating)}
                        </div>
                      </div>
                      
                      {activity.cost && (
                        <div className="flex items-center space-x-1 text-sm font-medium text-green-600">
                          <DollarSign className="h-3 w-3" />
                          <span>{formatCost(activity.cost)}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
            
            {/* Accommodation & Transportation */}
            {(day.accommodation || day.transportation) && (
              <div className="bg-gray-50 p-4 border-t">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {day.accommodation && (
                    <div className="flex items-start space-x-3">
                      <Bed className="h-5 w-5 text-gray-500 mt-0.5" />
                      <div>
                        <h5 className="font-medium text-sm">{day.accommodation.name}</h5>
                        <p className="text-xs text-gray-600">{day.accommodation.type}</p>
                        {day.accommodation.location && (
                          <p className="text-xs text-gray-500">{day.accommodation.location}</p>
                        )}
                        <div className="flex items-center space-x-2 mt-1">
                          {day.accommodation.rating && renderStars(day.accommodation.rating)}
                          {day.accommodation.cost && (
                            <span className="text-xs text-green-600">
                              {formatCost(day.accommodation.cost)}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                  
                  {day.transportation && (
                    <div className="flex items-start space-x-3">
                      <Car className="h-5 w-5 text-gray-500 mt-0.5" />
                      <div>
                        <h5 className="font-medium text-sm">{day.transportation.type}</h5>
                        {(day.transportation.from || day.transportation.to) && (
                          <p className="text-xs text-gray-600">
                            {day.transportation.from} → {day.transportation.to}
                          </p>
                        )}
                        <div className="flex items-center space-x-2 mt-1">
                          {day.transportation.duration && (
                            <span className="text-xs text-gray-500">
                              {day.transportation.duration}
                            </span>
                          )}
                          {day.transportation.cost && (
                            <span className="text-xs text-green-600">
                              {formatCost(day.transportation.cost)}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}