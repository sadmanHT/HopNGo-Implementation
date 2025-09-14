'use client';
import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Checkbox } from './ui/checkbox';
import { Calendar, MapPin, DollarSign, Users, Heart, Camera, Utensils, Mountain, Building, Waves, Loader2 } from 'lucide-react';

const TripPlanning = ({ onPlanGenerated }) => {
  const [tripData, setTripData] = useState({
    destination: '',
    startDate: '',
    endDate: '',
    budget: '',
    budgetType: 'total',
    travelers: 1,
    preferences: '',
    interests: [],
    accommodationType: '',
    transportPreference: ''
  });

  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState({});

  const interestOptions = [
    { id: 'culture', label: 'Culture & History', icon: Building },
    { id: 'adventure', label: 'Adventure & Sports', icon: Mountain },
    { id: 'food', label: 'Food & Dining', icon: Utensils },
    { id: 'nature', label: 'Nature & Wildlife', icon: Waves },
    { id: 'photography', label: 'Photography', icon: Camera },
    { id: 'relaxation', label: 'Relaxation & Wellness', icon: Heart }
  ];

  const handleInputChange = (field, value) => {
    setTripData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: null }));
    }
  };

  const handleInterestToggle = (interestId) => {
    setTripData(prev => ({
      ...prev,
      interests: prev.interests.includes(interestId)
        ? prev.interests.filter(id => id !== interestId)
        : [...prev.interests, interestId]
    }));
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!tripData.destination.trim()) {
      newErrors.destination = 'Destination is required';
    }
    if (!tripData.startDate) {
      newErrors.startDate = 'Start date is required';
    }
    if (!tripData.endDate) {
      newErrors.endDate = 'End date is required';
    }
    if (tripData.startDate && tripData.endDate && new Date(tripData.startDate) >= new Date(tripData.endDate)) {
      newErrors.endDate = 'End date must be after start date';
    }
    if (!tripData.budget || tripData.budget <= 0) {
      newErrors.budget = 'Budget must be a positive number';
    }
    if (tripData.interests.length === 0) {
      newErrors.interests = 'Please select at least one interest';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    
    try {
      // Simulate API call to generate trip plan
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      const mockItinerary = {
        destination: tripData.destination,
        duration: Math.ceil((new Date(tripData.endDate) - new Date(tripData.startDate)) / (1000 * 60 * 60 * 24)),
        budget: tripData.budget,
        interests: tripData.interests,
        activities: [
          {
            id: 1,
            day: 1,
            title: 'City Exploration',
            description: 'Explore the historic downtown area',
            time: '09:00 AM',
            duration: '3 hours',
            cost: 50,
            bookingAvailable: true
          },
          {
            id: 2,
            day: 1,
            title: 'Local Cuisine Tour',
            description: 'Taste authentic local dishes',
            time: '02:00 PM',
            duration: '2 hours',
            cost: 75,
            bookingAvailable: true
          }
        ],
        hotels: [
          {
            id: 1,
            name: 'Grand Plaza Hotel',
            rating: 4.5,
            pricePerNight: 120,
            amenities: ['WiFi', 'Pool', 'Gym'],
            bookingAvailable: true
          }
        ]
      };
      
      onPlanGenerated && onPlanGenerated(mockItinerary);
    } catch (error) {
      console.error('Error generating trip plan:', error);
      setErrors({ submit: 'Failed to generate trip plan. Please try again.' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-4xl mx-auto">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <MapPin className="h-6 w-6" />
          Plan Your Perfect Trip
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Destination and Dates */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="destination">Destination</Label>
              <Input
                id="destination"
                placeholder="Where do you want to go?"
                value={tripData.destination}
                onChange={(e) => handleInputChange('destination', e.target.value)}
                className={errors.destination ? 'border-red-500' : ''}
              />
              {errors.destination && <p className="text-sm text-red-500">{errors.destination}</p>}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="startDate">Start Date</Label>
              <Input
                id="startDate"
                type="date"
                value={tripData.startDate}
                onChange={(e) => handleInputChange('startDate', e.target.value)}
                className={errors.startDate ? 'border-red-500' : ''}
                min={new Date().toISOString().split('T')[0]}
              />
              {errors.startDate && <p className="text-sm text-red-500">{errors.startDate}</p>}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="endDate">End Date</Label>
              <Input
                id="endDate"
                type="date"
                value={tripData.endDate}
                onChange={(e) => handleInputChange('endDate', e.target.value)}
                className={errors.endDate ? 'border-red-500' : ''}
                min={tripData.startDate || new Date().toISOString().split('T')[0]}
              />
              {errors.endDate && <p className="text-sm text-red-500">{errors.endDate}</p>}
            </div>
          </div>

          {/* Budget and Travelers */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="budget">Budget ($)</Label>
              <div className="flex gap-2">
                <Input
                  id="budget"
                  type="number"
                  placeholder="1000"
                  value={tripData.budget}
                  onChange={(e) => handleInputChange('budget', e.target.value)}
                  className={errors.budget ? 'border-red-500' : ''}
                />
                <Select value={tripData.budgetType} onValueChange={(value) => handleInputChange('budgetType', value)}>
                  <SelectTrigger className="w-24">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="total">Total</SelectItem>
                    <SelectItem value="daily">Daily</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {errors.budget && <p className="text-sm text-red-500">{errors.budget}</p>}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="travelers">Travelers</Label>
              <Input
                id="travelers"
                type="number"
                min="1"
                max="20"
                value={tripData.travelers}
                onChange={(e) => handleInputChange('travelers', parseInt(e.target.value))}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="accommodation">Accommodation</Label>
              <Select value={tripData.accommodationType} onValueChange={(value) => handleInputChange('accommodationType', value)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="hotel">Hotel</SelectItem>
                  <SelectItem value="hostel">Hostel</SelectItem>
                  <SelectItem value="apartment">Apartment</SelectItem>
                  <SelectItem value="resort">Resort</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Interests */}
          <div className="space-y-3">
            <Label>Interests & Activities</Label>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {interestOptions.map((interest) => {
                const IconComponent = interest.icon;
                return (
                  <div key={interest.id} className="flex items-center space-x-2">
                    <Checkbox
                      id={interest.id}
                      checked={tripData.interests.includes(interest.id)}
                      onCheckedChange={() => handleInterestToggle(interest.id)}
                    />
                    <Label htmlFor={interest.id} className="flex items-center gap-2 cursor-pointer">
                      <IconComponent className="h-4 w-4" />
                      {interest.label}
                    </Label>
                  </div>
                );
              })}
            </div>
            {errors.interests && <p className="text-sm text-red-500">{errors.interests}</p>}
          </div>

          {/* Additional Preferences */}
          <div className="space-y-2">
            <Label htmlFor="preferences">Additional Preferences</Label>
            <Textarea
              id="preferences"
              placeholder="Any specific requirements, dietary restrictions, or special requests?"
              value={tripData.preferences}
              onChange={(e) => handleInputChange('preferences', e.target.value)}
              rows={3}
            />
          </div>

          {/* Submit Button */}
          <div className="flex flex-col gap-2">
            {errors.submit && <p className="text-sm text-red-500">{errors.submit}</p>}
            <Button type="submit" disabled={isLoading} className="w-full">
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Generating Your Trip Plan...
                </>
              ) : (
                'Generate AI Trip Plan'
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default TripPlanning;