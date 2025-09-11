'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/stores/authStore';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import { formatCurrency, formatDate } from '@/lib/formatting';
import { normalizeForSearch, generateSearchVariations } from '@/lib/transliteration';
import { toast } from '@/hooks/use-toast';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs';
import {
  Plus,
  Search,
  MapPin,
  Calendar,
  Users,
  DollarSign,
  Clock,
  Eye,
  History,
  RotateCcw,
  Trash2,
} from 'lucide-react';

interface Destination {
  city: string;
  country: string;
}

interface Itinerary {
  id: string;
  title: string;
  destinations: Destination[];
  startDate: string;
  endDate: string;
  budget: number;
  currency: string;
  groupSize: number;
  status: 'draft' | 'published' | 'completed';
  createdAt: string;
  updatedAt: string;
}

interface ItineraryVersion {
  id: string;
  version: number;
  title: string;
  destinations: Destination[];
  startDate: string;
  endDate: string;
  budget: number;
  currency: string;
  groupSize: number;
  createdAt: string;
  isCurrent: boolean;
}

interface TripsClientProps {
  translations: Record<string, string>;
}

export function TripsClient({ translations }: TripsClientProps) {
  const t = (key: string, params?: Record<string, any>) => {
    let translation = translations[key] || key;
    if (params) {
      Object.entries(params).forEach(([paramKey, value]) => {
        translation = translation.replace(`{{${paramKey}}}`, String(value));
      });
    }
    return translation;
  };
  const router = useRouter();
  const { user, token } = useAuthStore();
  const [itineraries, setItineraries] = useState<Itinerary[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [selectedItinerary, setSelectedItinerary] = useState<Itinerary | null>(null);
  const [itineraryVersions, setItineraryVersions] = useState<ItineraryVersion[]>([]);
  const [isLoadingVersions, setIsLoadingVersions] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newItinerary, setNewItinerary] = useState({
    title: '',
    destinations: [{ city: '', country: '' }],
    startDate: '',
    endDate: '',
    budget: 0,
    currency: 'BDT',
    groupSize: 1,
  });

  useEffect(() => {
    if (user && token) {
      fetchItineraries();
    } else {
      setIsLoading(false);
    }
  }, [user, token]);

  const fetchItineraries = async () => {
    try {
      setIsLoading(true);
      const response = await fetch('/api/v1/trip-planning/itineraries', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setItineraries(data.content || []);
      }
    } catch (error) {
      console.error('Error fetching itineraries:', error);
      toast({
        title: t['trips.error'],
        description: t['trips.fetchError'],
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  };

  const fetchItineraryVersions = async (itineraryId: string) => {
    try {
      setIsLoadingVersions(true);
      const response = await fetch(`/api/v1/trip-planning/itineraries/${itineraryId}/versions`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setItineraryVersions(data.content || []);
      }
    } catch (error) {
      console.error('Error fetching itinerary versions:', error);
    } finally {
      setIsLoadingVersions(false);
    }
  };

  const revertToVersion = async (itineraryId: string, version: ItineraryVersion) => {
    try {
      const response = await fetch(`/api/v1/trip-planning/itineraries/${itineraryId}/revert`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ versionId: version.id }),
      });
      
      if (response.ok) {
        toast({
          title: t['trips.success'],
          description: t['trips.revertSuccess'].replace('{version}', version.version.toString()),
        });
        fetchItineraries();
        fetchItineraryVersions(itineraryId);
      } else {
        throw new Error('Failed to revert');
      }
    } catch (error) {
      console.error('Error reverting to version:', error);
      toast({
        title: t['trips.error'],
        description: t['trips.revertError'],
        variant: 'destructive',
      });
    }
  };

  const createItinerary = async () => {
    try {
      const response = await fetch('/api/v1/trip-planning/itineraries', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(newItinerary),
      });
      
      if (response.ok) {
        toast({
          title: t['trips.success'],
          description: t['trips.createSuccess'],
        });
        setShowCreateForm(false);
        setNewItinerary({
          title: '',
          destinations: [{ city: '', country: '' }],
          startDate: '',
          endDate: '',
          budget: 0,
          currency: 'BDT',
          groupSize: 1,
        });
        fetchItineraries();
      } else {
        throw new Error('Failed to create itinerary');
      }
    } catch (error) {
      console.error('Error creating itinerary:', error);
      toast({
        title: t['trips.error'],
        description: t['trips.createError'],
        variant: 'destructive',
      });
    }
  };

  const addDestination = () => {
    setNewItinerary(prev => ({
      ...prev,
      destinations: [...prev.destinations, { city: '', country: '' }]
    }));
  };

  const removeDestination = (index: number) => {
    setNewItinerary(prev => ({
      ...prev,
      destinations: prev.destinations.filter((_, i) => i !== index)
    }));
  };

  const updateDestination = (index: number, field: 'city' | 'country', value: string) => {
    setNewItinerary(prev => ({
      ...prev,
      destinations: prev.destinations.map((dest, i) => 
        i === index ? { ...dest, [field]: value } : dest
      )
    }));
  };

  const filteredItineraries = itineraries.filter(itinerary => {
    if (!searchTerm) return true;
    
    const normalizedSearch = normalizeForSearch(searchTerm);
    const searchVariations = generateSearchVariations(searchTerm);
    
    const searchableText = [
      itinerary.title,
      ...itinerary.destinations.map(d => `${d.city} ${d.country}`)
    ].join(' ').toLowerCase();
    
    return searchVariations.some(variation => 
      searchableText.includes(variation.toLowerCase())
    ) || searchableText.includes(normalizedSearch);
  });

  const calculateDays = (startDate: string, endDate: string) => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
        <header className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center h-16">
              <div className="flex items-center">
                <Link href="/" className="text-2xl font-bold text-indigo-600">
                  HopNGo
                </Link>
              </div>
              <div className="flex items-center space-x-4">
                <LanguageSwitcher />
              </div>
            </div>
          </div>
        </header>
        
        <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
          <Card className="w-full max-w-md">
            <CardHeader className="text-center">
              <CardTitle className="text-2xl">{t['trips.pleaseLogin']}</CardTitle>
            </CardHeader>
            <CardContent className="text-center">
              <p className="text-gray-600 mb-6">{t['trips.loginRequired']}</p>
              <Link href="/auth/login">
                <Button className="w-full">{t['auth.goToLogin']}</Button>
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <Link href="/" className="text-2xl font-bold text-indigo-600">
                HopNGo
              </Link>
            </div>
            <div className="flex items-center space-x-4">
              <LanguageSwitcher />
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{t['trips.title']}</h1>
            <p className="text-gray-600 mt-2">{t['trips.subtitle']}</p>
          </div>
          <Button onClick={() => setShowCreateForm(true)} className="flex items-center gap-2">
            <Plus className="h-4 w-4" />
            {t['trips.createNew']}
          </Button>
        </div>

        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <Input
              type="text"
              placeholder={t['trips.searchPlaceholder']}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <Card key={i} className="animate-pulse">
                <CardHeader>
                  <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                  <div className="h-3 bg-gray-200 rounded w-1/2 mt-2"></div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    <div className="h-3 bg-gray-200 rounded"></div>
                    <div className="h-3 bg-gray-200 rounded w-5/6"></div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : filteredItineraries.length === 0 ? (
          <Card className="text-center py-12">
            <CardContent>
              <MapPin className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                {searchTerm ? t['trips.noResults'] : t['trips.noItineraries']}
              </h3>
              <p className="text-gray-600 mb-6">
                {searchTerm ? t['trips.tryDifferentSearch'] : t['trips.createFirst']}
              </p>
              {!searchTerm && (
                <Button onClick={() => setShowCreateForm(true)}>
                  {t['trips.createNew']}
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredItineraries.map((itinerary) => (
              <Card key={itinerary.id} className="hover:shadow-lg transition-shadow">
                <CardHeader>
                  <div className="flex justify-between items-start">
                    <CardTitle className="text-lg">{itinerary.title}</CardTitle>
                    <Badge variant={itinerary.status === 'published' ? 'default' : 'secondary'}>
                      {t[`trips.status.${itinerary.status}`]}
                    </Badge>
                  </div>
                  <div className="flex items-center text-sm text-gray-600">
                    <MapPin className="h-4 w-4 mr-1" />
                    {itinerary.destinations.map(d => d.city).join(', ')}
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center text-gray-600">
                        <Calendar className="h-4 w-4 mr-1" />
                        {t['trips.daysCount'].replace('{count}', calculateDays(itinerary.startDate, itinerary.endDate).toString())}
                      </div>
                      <div className="flex items-center text-gray-600">
                        <Users className="h-4 w-4 mr-1" />
                        {itinerary.groupSize}
                      </div>
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <DollarSign className="h-4 w-4 mr-1" />
                      {formatCurrency(itinerary.budget, itinerary.currency)}
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <Clock className="h-4 w-4 mr-1" />
                      {formatDate(itinerary.updatedAt, 'relative')}
                    </div>
                  </div>
                  <div className="flex gap-2 mt-4">
                    <Dialog>
                      <DialogTrigger asChild>
                        <Button 
                          variant="outline" 
                          size="sm" 
                          className="flex-1"
                          onClick={() => {
                            setSelectedItinerary(itinerary);
                            fetchItineraryVersions(itinerary.id);
                          }}
                        >
                          <Eye className="h-4 w-4 mr-1" />
                          {t['trips.viewDetails']}
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
                        <DialogHeader>
                          <DialogTitle>{selectedItinerary?.title}</DialogTitle>
                        </DialogHeader>
                        {selectedItinerary && (
                          <Tabs defaultValue="details" className="w-full">
                            <TabsList className="grid w-full grid-cols-2">
                              <TabsTrigger value="details">{t['trips.details']}</TabsTrigger>
                              <TabsTrigger value="versions">{t['trips.versions']}</TabsTrigger>
                            </TabsList>
                            <TabsContent value="details" className="space-y-4">
                              <div className="grid grid-cols-2 gap-4">
                                <div>
                                  <Label className="text-sm font-medium">{t['trips.destinations']}</Label>
                                  <div className="mt-1">
                                    {selectedItinerary.destinations.map((dest, index) => (
                                      <div key={index} className="text-sm text-gray-600">
                                        {dest.city}, {dest.country}
                                      </div>
                                    ))}
                                  </div>
                                </div>
                                <div>
                                  <Label className="text-sm font-medium">{t['trips.duration']}</Label>
                                  <div className="text-sm text-gray-600 mt-1">
                                    {formatDate(selectedItinerary.startDate)} - {formatDate(selectedItinerary.endDate)}
                                    <br />
                                    ({t['trips.daysCount'].replace('{count}', calculateDays(selectedItinerary.startDate, selectedItinerary.endDate).toString())})
                                  </div>
                                </div>
                                <div>
                                  <Label className="text-sm font-medium">{t['trips.budget']}</Label>
                                  <div className="text-sm text-gray-600 mt-1">
                                    {formatCurrency(selectedItinerary.budget, selectedItinerary.currency)}
                                  </div>
                                </div>
                                <div>
                                  <Label className="text-sm font-medium">{t['trips.groupSize']}</Label>
                                  <div className="text-sm text-gray-600 mt-1">
                                    {selectedItinerary.groupSize} {t['trips.people']}
                                  </div>
                                </div>
                              </div>
                              <Separator />
                              <div>
                                <Label className="text-sm font-medium">{t['trips.route']}</Label>
                                <div className="mt-2 space-y-2">
                                  {selectedItinerary.destinations.map((dest, index) => (
                                    <div key={index} className="flex items-center text-sm">
                                      <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-xs font-medium mr-3">
                                        {index + 1}
                                      </div>
                                      <span>{dest.city}, {dest.country}</span>
                                    </div>
                                  ))}
                                </div>
                              </div>
                            </TabsContent>
                            <TabsContent value="versions" className="space-y-4">
                              <div className="flex items-center gap-2 mb-4">
                                <History className="h-4 w-4" />
                                <h3 className="font-medium">{t['trips.versionHistory']}</h3>
                              </div>
                              {isLoadingVersions ? (
                                <div className="space-y-2">
                                  {[...Array(3)].map((_, i) => (
                                    <div key={i} className="animate-pulse">
                                      <div className="h-16 bg-gray-200 rounded"></div>
                                    </div>
                                  ))}
                                </div>
                              ) : itineraryVersions.length === 0 ? (
                                <p className="text-gray-600 text-center py-8">
                                  {t['trips.noVersionHistory']}
                                </p>
                              ) : (
                                <ScrollArea className="h-64">
                                  <div className="space-y-2">
                                    {itineraryVersions.map((version) => (
                                      <div key={version.id} className="border rounded-lg p-3">
                                        <div className="flex justify-between items-start">
                                          <div className="flex-1">
                                            <div className="flex items-center gap-2 mb-1">
                                              <span className="font-medium">
                                                {t['trips.versionNumber'].replace('{version}', version.version.toString())}
                                              </span>
                                              {version.isCurrent && (
                                                <Badge variant="default" className="text-xs">
                                                  {t['trips.current']}
                                                </Badge>
                                              )}
                                            </div>
                                            <p className="text-sm text-gray-600 mb-1">{version.title}</p>
                                            <p className="text-xs text-gray-500">
                                              {formatDate(version.createdAt, 'relative')}
                                            </p>
                                          </div>
                                          {!version.isCurrent && (
                                            <Button
                                              variant="outline"
                                              size="sm"
                                              onClick={() => revertToVersion(selectedItinerary.id, version)}
                                              className="ml-2"
                                            >
                                              <RotateCcw className="h-3 w-3 mr-1" />
                                              {t['trips.revert']}
                                            </Button>
                                          )}
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                </ScrollArea>
                              )}
                            </TabsContent>
                          </Tabs>
                        )}
                      </DialogContent>
                    </Dialog>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Create Itinerary Dialog */}
        <Dialog open={showCreateForm} onOpenChange={setShowCreateForm}>
          <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>{t['trips.createNew']}</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label htmlFor="title">{t['trips.title']}</Label>
                <Input
                  id="title"
                  value={newItinerary.title}
                  onChange={(e) => setNewItinerary(prev => ({ ...prev, title: e.target.value }))}
                  placeholder={t['trips.titlePlaceholder']}
                />
              </div>
              
              <div>
                <Label>{t['trips.destinations']}</Label>
                <div className="space-y-2 mt-2">
                  {newItinerary.destinations.map((dest, index) => (
                    <div key={index} className="flex gap-2 items-center">
                      <Input
                        placeholder={t['common.city']}
                        value={dest.city}
                        onChange={(e) => updateDestination(index, 'city', e.target.value)}
                        className="flex-1"
                      />
                      <Input
                        placeholder={t['common.country']}
                        value={dest.country}
                        onChange={(e) => updateDestination(index, 'country', e.target.value)}
                        className="flex-1"
                      />
                      {newItinerary.destinations.length > 1 && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => removeDestination(index)}
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
                    className="w-full"
                  >
                    <Plus className="h-4 w-4 mr-1" />
                    {t['trips.addDestination']}
                  </Button>
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="startDate">{t['trips.startDate']}</Label>
                  <Input
                    id="startDate"
                    type="date"
                    value={newItinerary.startDate}
                    onChange={(e) => setNewItinerary(prev => ({ ...prev, startDate: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="endDate">{t['trips.endDate']}</Label>
                  <Input
                    id="endDate"
                    type="date"
                    value={newItinerary.endDate}
                    onChange={(e) => setNewItinerary(prev => ({ ...prev, endDate: e.target.value }))}
                  />
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="budget">{t['trips.budget']}</Label>
                  <Input
                    id="budget"
                    type="number"
                    value={newItinerary.budget}
                    onChange={(e) => setNewItinerary(prev => ({ ...prev, budget: Number(e.target.value) }))}
                    placeholder="0"
                  />
                </div>
                <div>
                  <Label htmlFor="groupSize">{t['trips.groupSize']}</Label>
                  <Input
                    id="groupSize"
                    type="number"
                    min="1"
                    value={newItinerary.groupSize}
                    onChange={(e) => setNewItinerary(prev => ({ ...prev, groupSize: Number(e.target.value) }))}
                  />
                </div>
              </div>
              
              <div className="flex gap-2 pt-4">
                <Button onClick={createItinerary} className="flex-1">
                  {t['trips.generatePlan']}
                </Button>
                <Button variant="outline" onClick={() => setShowCreateForm(false)}>
                  {t['common.cancel']}
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </main>
    </div>
  );
}