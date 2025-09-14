'use client';
import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Separator } from './ui/separator';
import { MapPin, Clock, DollarSign, Star, Calendar, Users, Wifi, Car, Utensils, Camera, Heart } from 'lucide-react';

const ItineraryDisplay = ({ itinerary, onBookActivity, onBookHotel }) => {
  const [selectedDay, setSelectedDay] = useState(1);
  const [bookingLoading, setBookingLoading] = useState({});

  if (!itinerary) {
    return (
      <Card className="w-full max-w-4xl mx-auto">
        <CardContent className="p-8 text-center">
          <p className="text-gray-500">No itinerary generated yet. Please plan your trip first.</p>
        </CardContent>
      </Card>
    );
  }

  const handleBookActivity = async (activity) => {
    setBookingLoading(prev => ({ ...prev, [`activity-${activity.id}`]: true }));
    try {
      await onBookActivity(activity);
    } finally {
      setBookingLoading(prev => ({ ...prev, [`activity-${activity.id}`]: false }));
    }
  };

  const handleBookHotel = async (hotel) => {
    setBookingLoading(prev => ({ ...prev, [`hotel-${hotel.id}`]: true }));
    try {
      await onBookHotel(hotel);
    } finally {
      setBookingLoading(prev => ({ ...prev, [`hotel-${hotel.id}`]: false }));
    }
  };

  const getInterestIcon = (interest) => {
    const icons = {
      culture: Building,
      adventure: Mountain,
      food: Utensils,
      nature: Waves,
      photography: Camera,
      relaxation: Heart
    };
    return icons[interest] || MapPin;
  };

  const dayActivities = itinerary.activities?.filter(activity => activity.day === selectedDay) || [];
  const totalDays = Math.max(...(itinerary.activities?.map(a => a.day) || [1]));

  return (
    <div className="w-full max-w-6xl mx-auto space-y-6">
      {/* Trip Overview */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="h-6 w-6" />
            Your AI-Generated Trip to {itinerary.destination}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-gray-500" />
              <span>{itinerary.duration} days</span>
            </div>
            <div className="flex items-center gap-2">
              <DollarSign className="h-4 w-4 text-gray-500" />
              <span>${itinerary.budget} budget</span>
            </div>
            <div className="flex items-center gap-2">
              <Users className="h-4 w-4 text-gray-500" />
              <span>{itinerary.travelers || 1} travelers</span>
            </div>
            <div className="flex gap-1">
              {itinerary.interests?.map(interest => {
                const IconComponent = getInterestIcon(interest);
                return (
                  <Badge key={interest} variant="secondary" className="flex items-center gap-1">
                    <IconComponent className="h-3 w-3" />
                    {interest}
                  </Badge>
                );
              })}
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Daily Itinerary */}
        <div className="lg:col-span-2 space-y-4">
          {/* Day Selector */}
          <Card>
            <CardHeader>
              <CardTitle>Daily Itinerary</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex gap-2 mb-4">
                {Array.from({ length: totalDays }, (_, i) => i + 1).map(day => (
                  <Button
                    key={day}
                    variant={selectedDay === day ? "default" : "outline"}
                    size="sm"
                    onClick={() => setSelectedDay(day)}
                  >
                    Day {day}
                  </Button>
                ))}
              </div>
              
              {/* Activities for Selected Day */}
              <div className="space-y-4">
                {dayActivities.length > 0 ? (
                  dayActivities.map((activity, index) => (
                    <div key={activity.id}>
                      <Card className="border-l-4 border-l-blue-500">
                        <CardContent className="p-4">
                          <div className="flex justify-between items-start mb-2">
                            <div>
                              <h4 className="font-semibold text-lg">{activity.title}</h4>
                              <p className="text-gray-600 mb-2">{activity.description}</p>
                              <div className="flex items-center gap-4 text-sm text-gray-500">
                                <div className="flex items-center gap-1">
                                  <Clock className="h-4 w-4" />
                                  {activity.time}
                                </div>
                                <div className="flex items-center gap-1">
                                  <Calendar className="h-4 w-4" />
                                  {activity.duration}
                                </div>
                                <div className="flex items-center gap-1">
                                  <DollarSign className="h-4 w-4" />
                                  ${activity.cost}
                                </div>
                              </div>
                            </div>
                            {activity.bookingAvailable && (
                              <Button
                                size="sm"
                                onClick={() => handleBookActivity(activity)}
                                disabled={bookingLoading[`activity-${activity.id}`]}
                              >
                                {bookingLoading[`activity-${activity.id}`] ? 'Booking...' : 'Book Now'}
                              </Button>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                      {index < dayActivities.length - 1 && <Separator className="my-2" />}
                    </div>
                  ))
                ) : (
                  <p className="text-gray-500 text-center py-8">No activities planned for this day.</p>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Hotels & Accommodation */}
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Recommended Hotels</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {itinerary.hotels?.length > 0 ? (
                itinerary.hotels.map(hotel => (
                  <Card key={hotel.id} className="border">
                    <CardContent className="p-4">
                      <div className="space-y-3">
                        <div>
                          <h4 className="font-semibold">{hotel.name}</h4>
                          <div className="flex items-center gap-1 mt-1">
                            {Array.from({ length: 5 }, (_, i) => (
                              <Star
                                key={i}
                                className={`h-4 w-4 ${
                                  i < Math.floor(hotel.rating)
                                    ? 'text-yellow-400 fill-current'
                                    : 'text-gray-300'
                                }`}
                              />
                            ))}
                            <span className="text-sm text-gray-600 ml-1">
                              {hotel.rating}
                            </span>
                          </div>
                        </div>
                        
                        <div className="text-lg font-semibold text-green-600">
                          ${hotel.pricePerNight}/night
                        </div>
                        
                        <div className="flex flex-wrap gap-1">
                          {hotel.amenities?.map(amenity => (
                            <Badge key={amenity} variant="outline" className="text-xs">
                              {amenity}
                            </Badge>
                          ))}
                        </div>
                        
                        {hotel.bookingAvailable && (
                          <Button
                            className="w-full"
                            onClick={() => handleBookHotel(hotel)}
                            disabled={bookingLoading[`hotel-${hotel.id}`]}
                          >
                            {bookingLoading[`hotel-${hotel.id}`] ? 'Booking...' : 'Book Hotel'}
                          </Button>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                ))
              ) : (
                <p className="text-gray-500 text-center py-4">No hotels recommended.</p>
              )}
            </CardContent>
          </Card>

          {/* Trip Summary */}
          <Card>
            <CardHeader>
              <CardTitle>Trip Summary</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span>Activities:</span>
                  <span>{itinerary.activities?.length || 0}</span>
                </div>
                <div className="flex justify-between">
                  <span>Estimated Cost:</span>
                  <span className="font-semibold">
                    ${(itinerary.activities?.reduce((sum, a) => sum + a.cost, 0) || 0) + 
                      (itinerary.hotels?.[0]?.pricePerNight * itinerary.duration || 0)}
                  </span>
                </div>
                <Separator />
                <div className="flex justify-between font-semibold">
                  <span>Total Budget:</span>
                  <span>${itinerary.budget}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default ItineraryDisplay;