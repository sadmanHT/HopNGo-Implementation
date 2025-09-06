'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardAction } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import ItineraryTimeline from '@/components/ItineraryTimeline';
import { useAuthStore } from '@/lib/state';
import { 
  MapPin, 
  Plus, 
  Search, 
  Calendar, 
  DollarSign, 
  Eye, 
  Edit, 
  Trash2,
  Plane,
  Clock
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

// Trip types based on backend API
interface TripItinerary {
  id: string;
  userId: string;
  title: string;
  days: number;
  budget: number; // in cents
  origin: {
    city: string;
    country: string;
  };
  destinations: Array<{
    city: string;
    country: string;
  }>;
  plan?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

interface TripPlanRequest {
  title: string;
  days: number;
  budget: number;
  origin: {
    city: string;
    country: string;
  };
  destinations: Array<{
    city: string;
    country: string;
  }>;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export default function TripsPage() {
  const router = useRouter();
  const { user, token } = useAuthStore();
  
  const [itineraries, setItineraries] = useState<TripItinerary[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [showPlanningForm, setShowPlanningForm] = useState(false);
  const [selectedItinerary, setSelectedItinerary] = useState<TripItinerary | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  
  // Planning form state
  const [planForm, setPlanForm] = useState<TripPlanRequest>({
    title: '',
    days: 7,
    budget: 200000, // $2000 in cents
    origin: { city: '', country: '' },
    destinations: [{ city: '', country: '' }]
  });

  useEffect(() => {
    if (!user || !token) {
      router.push('/login');
      return;
    }
    fetchItineraries();
  }, [user, token, router]);

  const fetchItineraries = async () => {
    try {
      setLoading(true);
      const response = await fetch('http://localhost:8087/api/trips?page=0&size=20', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || '',
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data: PaginatedResponse<TripItinerary> = await response.json();
        setItineraries(data.content);
      }
    } catch (error) {
      console.error('Failed to fetch itineraries:', error);
    } finally {
      setLoading(false);
    }
  };

  const createTripPlan = async () => {
    try {
      const response = await fetch('http://localhost:8087/api/trips/plan', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || '',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(planForm)
      });
      
      if (response.ok) {
        const newItinerary: TripItinerary = await response.json();
        setItineraries(prev => [newItinerary, ...prev]);
        setShowPlanningForm(false);
        setPlanForm({
          title: '',
          days: 7,
          budget: 200000,
          origin: { city: '', country: '' },
          destinations: [{ city: '', country: '' }]
        });
      }
    } catch (error) {
      console.error('Failed to create trip plan:', error);
    }
  };

  const deleteItinerary = async (id: string) => {
    try {
      const response = await fetch(`http://localhost:8087/api/trips/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user?.id || ''
        }
      });
      
      if (response.ok) {
        setItineraries(prev => prev.filter(item => item.id !== id));
      }
    } catch (error) {
      console.error('Failed to delete itinerary:', error);
    }
  };

  const addDestination = () => {
    setPlanForm(prev => ({
      ...prev,
      destinations: [...prev.destinations, { city: '', country: '' }]
    }));
  };

  const removeDestination = (index: number) => {
    setPlanForm(prev => ({
      ...prev,
      destinations: prev.destinations.filter((_, i) => i !== index)
    }));
  };

  const updateDestination = (index: number, field: 'city' | 'country', value: string) => {
    setPlanForm(prev => ({
      ...prev,
      destinations: prev.destinations.map((dest, i) => 
        i === index ? { ...dest, [field]: value } : dest
      )
    }));
  };

  const filteredItineraries = itineraries.filter(itinerary =>
    itinerary.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    itinerary.destinations.some(dest => 
      dest.city.toLowerCase().includes(searchQuery.toLowerCase()) ||
      dest.country.toLowerCase().includes(searchQuery.toLowerCase())
    )
  );

  const formatBudget = (cents: number) => {
    return `$${(cents / 100).toLocaleString()}`;
  };

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-xl font-semibold mb-2">Please log in to access trips</h2>
          <Button asChild>
            <Link href="/login">Go to Login</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Plane className="h-8 w-8 text-blue-600" />
            <h1 className="text-2xl font-bold text-gray-900">Trip Planning</h1>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 py-6">
        {/* Search and New Trip */}
        <div className="flex items-center space-x-4 mb-6">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              type="text"
              placeholder="Search trips by title or destination..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
          <Button
            onClick={() => setShowPlanningForm(true)}
            className="flex items-center space-x-2"
          >
            <Plus className="h-4 w-4" />
            <span>Plan New Trip</span>
          </Button>
        </div>

        {/* Itineraries Grid */}
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin h-8 w-8 border-2 border-blue-600 border-t-transparent rounded-full" />
          </div>
        ) : filteredItineraries.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Plane className="h-12 w-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                {searchQuery ? 'No trips found' : 'No trips planned yet'}
              </h3>
              <p className="text-gray-500 text-center mb-4">
                {searchQuery 
                  ? 'Try adjusting your search terms'
                  : 'Start planning your next adventure'
                }
              </p>
              {!searchQuery && (
                <Button onClick={() => setShowPlanningForm(true)}>
                  Plan Your First Trip
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredItineraries.map((itinerary) => (
              <Card key={itinerary.id} className="hover:shadow-lg transition-shadow">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <CardTitle className="text-lg mb-2">{itinerary.title}</CardTitle>
                      <div className="space-y-1 text-sm text-gray-600">
                        <div className="flex items-center space-x-1">
                          <MapPin className="h-4 w-4" />
                          <span>
                            {itinerary.destinations.map(dest => `${dest.city}, ${dest.country}`).join(' → ')}
                          </span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <Calendar className="h-4 w-4" />
                          <span>{itinerary.days} days</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <DollarSign className="h-4 w-4" />
                          <span>{formatBudget(itinerary.budget)}</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <Clock className="h-4 w-4" />
                          <span>{formatDistanceToNow(new Date(itinerary.createdAt), { addSuffix: true })}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setSelectedItinerary(itinerary);
                        setShowDetails(true);
                      }}
                      className="flex-1"
                    >
                      <Eye className="h-4 w-4 mr-1" />
                      View Details
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => deleteItinerary(itinerary.id)}
                      className="text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Planning Form Modal */}
      {showPlanningForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <CardHeader>
              <CardTitle>Plan New Trip</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Trip Title</label>
                <Input
                  value={planForm.title}
                  onChange={(e) => setPlanForm(prev => ({ ...prev, title: e.target.value }))}
                  placeholder="Enter trip title"
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Days</label>
                  <Input
                    type="number"
                    min="1"
                    max="30"
                    value={planForm.days}
                    onChange={(e) => setPlanForm(prev => ({ ...prev, days: parseInt(e.target.value) || 1 }))}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Budget ($)</label>
                  <Input
                    type="number"
                    min="0"
                    value={planForm.budget / 100}
                    onChange={(e) => setPlanForm(prev => ({ ...prev, budget: (parseFloat(e.target.value) || 0) * 100 }))}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Origin</label>
                <div className="grid grid-cols-2 gap-2">
                  <Input
                    value={planForm.origin.city}
                    onChange={(e) => setPlanForm(prev => ({ ...prev, origin: { ...prev.origin, city: e.target.value } }))}
                    placeholder="City"
                  />
                  <Input
                    value={planForm.origin.country}
                    onChange={(e) => setPlanForm(prev => ({ ...prev, origin: { ...prev.origin, country: e.target.value } }))}
                    placeholder="Country"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Destinations</label>
                {planForm.destinations.map((dest, index) => (
                  <div key={index} className="flex items-center space-x-2 mb-2">
                    <Input
                      value={dest.city}
                      onChange={(e) => updateDestination(index, 'city', e.target.value)}
                      placeholder="City"
                      className="flex-1"
                    />
                    <Input
                      value={dest.country}
                      onChange={(e) => updateDestination(index, 'country', e.target.value)}
                      placeholder="Country"
                      className="flex-1"
                    />
                    {planForm.destinations.length > 1 && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => removeDestination(index)}
                        className="text-red-600"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                ))}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={addDestination}
                  className="mt-2"
                >
                  <Plus className="h-4 w-4 mr-1" />
                  Add Destination
                </Button>
              </div>

              <div className="flex items-center space-x-2 pt-4">
                <Button onClick={createTripPlan} className="flex-1">
                  Generate Trip Plan
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowPlanningForm(false)}
                >
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Trip Details Modal */}
      {showDetails && selectedItinerary && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <Card className="w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <CardHeader>
              <CardTitle>{selectedItinerary.title}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="flex items-center space-x-2">
                  <Calendar className="h-5 w-5 text-gray-500" />
                  <span>{selectedItinerary.days} days</span>
                </div>
                <div className="flex items-center space-x-2">
                  <DollarSign className="h-5 w-5 text-gray-500" />
                  <span>{formatBudget(selectedItinerary.budget)}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <MapPin className="h-5 w-5 text-gray-500" />
                  <span>{selectedItinerary.destinations.length} destinations</span>
                </div>
              </div>

              <div>
                <h3 className="font-semibold mb-2">Route</h3>
                <div className="text-sm text-gray-600">
                  <span className="font-medium">From:</span> {selectedItinerary.origin.city}, {selectedItinerary.origin.country}
                </div>
                <div className="text-sm text-gray-600 mt-1">
                  <span className="font-medium">To:</span> {selectedItinerary.destinations.map(dest => `${dest.city}, ${dest.country}`).join(' → ')}
                </div>
              </div>

              {selectedItinerary.plan && (
                <div>
                  <h3 className="font-semibold mb-2">Generated Itinerary</h3>
                  <ItineraryTimeline plan={selectedItinerary.plan} />
                </div>
              )}

              <div className="flex items-center space-x-2 pt-4">
                <Button
                  onClick={() => setShowDetails(false)}
                  className="flex-1"
                >
                  Close
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}