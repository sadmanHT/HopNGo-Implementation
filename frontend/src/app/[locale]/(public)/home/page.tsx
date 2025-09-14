'use client';

import Link from 'next/link';

// Force dynamic rendering
export const dynamic = 'force-dynamic';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import { useTranslation } from '@/lib/i18n';
import { useAuthStore } from '@/lib/state/auth';
import { useFeatureFlag } from '@/lib/flags';
import React from 'react';
import { useEffect, useState } from 'react';
import recommendationService, { RecommendedItem } from '@/services/recommendations';
import { Loader2, MapPin, Star } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

interface HomePageProps {
  params: Promise<{ locale: string }>;
}

export default function HomePage({ params }: HomePageProps) {
  const [locale, setLocale] = useState<string>('');
  const { isAuthenticated, user } = useAuthStore();
  
  useEffect(() => {
    params.then(({ locale }) => setLocale(locale));
  }, [params]);
  const recsEnabled = useFeatureFlag('recs_v1');
  const [recommendations, setRecommendations] = useState<RecommendedItem[]>([]);
  const [recommendationsLoading, setRecommendationsLoading] = useState(false);
  const [t, setT] = useState<any>({
    'auth.login': 'Login',
    'auth.register': 'Register',
    'home.hero.title': 'Discover Amazing',
    'home.hero.titleHighlight': 'Adventures',
    'home.hero.subtitle': 'Connect with fellow travelers and explore the world together',
    'home.hero.startAdventure': 'Start Your Adventure',
    'home.hero.signIn': 'Sign In',
    'home.features.title': 'Everything You Need',
    'home.features.subtitle': 'Powerful features to enhance your travel experience',
    'home.features.discover.title': 'Discover Places',
    'home.features.discover.description': 'Find amazing destinations and hidden gems',
    'home.features.plan.title': 'Plan Trips',
    'home.features.plan.description': 'Create detailed itineraries with ease',
    'home.features.connect.title': 'Connect',
    'home.features.connect.description': 'Meet like-minded travelers',
    'home.features.market.title': 'Marketplace',
    'home.features.market.description': 'Book accommodations and experiences',
    'home.features.chat.title': 'Chat',
    'home.features.chat.description': 'Communicate with other travelers',
    'home.features.mobile.title': 'Mobile App',
    'home.features.mobile.description': 'Take HopNGo with you anywhere',
    'home.footer.copyright': '© 2024 HopNGo. All rights reserved.'
  });

  useEffect(() => {
    const loadTranslations = async () => {
      try {
        const { t: translations } = await useTranslation(locale);
        setT(translations);
      } catch (error) {
        console.error('Failed to load translations:', error);
        // Keep default translations
      }
    };
    loadTranslations();
  }, [locale]);

  useEffect(() => {
    const fetchRecommendations = async () => {
      if (!isAuthenticated || !user || !recsEnabled) return;
      
      try {
        setRecommendationsLoading(true);
        const data = await recommendationService.getHomeRecommendations({ limit: 6 });
        setRecommendations(data.recommendations || []);
      } catch (error) {
        console.error('Failed to fetch recommendations:', error);
        // Fallback to mock data
        setRecommendations(mockRecommendations);
      } finally {
        setRecommendationsLoading(false);
      }
    };

    fetchRecommendations();
  }, [isAuthenticated, user, recsEnabled]);
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-gray-900">HopNGo</h1>
            </div>
            <div className="flex items-center space-x-4">
              <LanguageSwitcher />
              <Link href="/login">
                <Button variant="outline">{t['auth.login'] || 'Login'}</Button>
              </Link>
              <Link href="/register">
                <Button>{t['auth.register'] || 'Register'}</Button>
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center">
          <h1 className="text-4xl font-bold text-gray-900 sm:text-5xl md:text-6xl">
            {t['home.hero.title'] || 'Discover Amazing'}{' '}
            <span className="text-blue-600">{t['home.hero.titleHighlight'] || 'Adventures'}</span>
          </h1>
          <p className="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
            {t['home.hero.subtitle'] || 'Connect with fellow travelers and explore the world together'}
          </p>
          <div className="mt-5 max-w-md mx-auto sm:flex sm:justify-center md:mt-8">
            <div className="rounded-md shadow">
              <Link href="/register">
                <Button size="lg" className="w-full sm:w-auto">
                  {t['home.hero.startAdventure'] || 'Start Your Adventure'}
                </Button>
              </Link>
            </div>
            <div className="mt-3 rounded-md shadow sm:mt-0 sm:ml-3">
              <Link href="/login">
                <Button variant="outline" size="lg" className="w-full sm:w-auto">
                  {t['home.hero.signIn'] || 'Sign In'}
                </Button>
              </Link>
            </div>
          </div>
        </div>

        {/* Features Section */}
        <div className="mt-20">
          <div className="text-center">
            <h2 className="text-3xl font-bold text-gray-900">{t['home.features.title'] || 'Everything You Need'}</h2>
            <p className="mt-4 text-lg text-gray-600">
              {t['home.features.subtitle'] || 'Powerful features to enhance your travel experience'}
            </p>
          </div>

          <div className="mt-12 grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center mr-3">
                    🗺️
                  </div>
                  {t['home.features.discover.title'] || 'Discover Places'}
                </CardTitle>
                <CardDescription>
                  {t['home.features.discover.description'] || 'Find amazing destinations and hidden gems'}
                </CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center mr-3">
                    📅
                  </div>
                  {t['home.features.plan.title'] || 'Plan Trips'}
                </CardTitle>
                <CardDescription>
                  {t['home.features.plan.description'] || 'Create detailed itineraries with ease'}
                </CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center mr-3">
                    👥
                  </div>
                  {t['home.features.connect.title'] || 'Connect'}
                </CardTitle>
                <CardDescription>
                  {t['home.features.connect.description'] || 'Meet like-minded travelers'}
                </CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <div className="w-8 h-8 bg-yellow-100 rounded-lg flex items-center justify-center mr-3">
                    🛒
                  </div>
                  {t['home.features.market.title'] || 'Marketplace'}
                </CardTitle>
                <CardDescription>
                  {t['home.features.market.description'] || 'Book accommodations and experiences'}
                </CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <div className="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center mr-3">
                    💬
                  </div>
                  {t['home.features.chat.title'] || 'Chat'}
                </CardTitle>
                <CardDescription>
                  {t['home.features.chat.description'] || 'Communicate with other travelers'}
                </CardDescription>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center">
                  <div className="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center mr-3">
                    📱
                  </div>
                  {t['home.features.mobile.title'] || 'Mobile App'}
                </CardTitle>
                <CardDescription>
                  {t['home.features.mobile.description'] || 'Take HopNGo with you anywhere'}
                </CardDescription>
              </CardHeader>
            </Card>
          </div>
        </div>

        {/* Recommendations Section for Logged-in Users */}
        {isAuthenticated && recsEnabled && (
          <div className="mt-20">
            <div className="text-center">
              <h2 className="text-3xl font-bold text-gray-900">
                Because you saved...
              </h2>
              <p className="mt-4 text-lg text-gray-600">
                Personalized recommendations based on your interests
              </p>
            </div>

            <div className="mt-12">
              {recommendationsLoading ? (
                <div className="flex justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
                </div>
              ) : recommendations.length > 0 ? (
                <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                  {recommendations.map((item, index) => (
                    <RecommendationCard key={item.id} item={item} position={index} />
                  ))}
                </div>
              ) : (
                <div className="text-center py-12">
                  <p className="text-gray-500">
                    Start saving places to get personalized recommendations!
                  </p>
                  <Link href="/discover" className="mt-4 inline-block">
                    <Button>Explore Places</Button>
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center text-gray-500">
            <p>{t['home.footer.copyright'] || '© 2024 HopNGo. All rights reserved.'}</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

function RecommendationCard({ item, position }: { item: RecommendedItem; position: number }) {
  React.useEffect(() => {
    // Track impression when component mounts
    recommendationService.trackImpression({
      recommendationId: item.id,
      algorithm: 'home_recommendations',
      position: position,
      context: 'home'
    });
  }, [item.id, position]);

  const handleClick = () => {
    // Track recommendation click
    recommendationService.trackClick({
      recommendationId: item.id,
      algorithm: 'home_recommendations',
      position: position,
      context: 'home'
    });
  };

  return (
    <Link href={`/places/${item.id}`} onClick={handleClick}>
      <Card className="hover:shadow-lg transition-shadow cursor-pointer h-full">
        <div className="aspect-[4/3] relative overflow-hidden rounded-t-lg">
          <img 
            src={item.imageUrl} 
            alt={item.title}
            className="w-full h-full object-cover"
          />
          <Badge className="absolute top-3 right-3 capitalize">
            {item.type}
          </Badge>
        </div>
        <CardContent className="p-4">
          <h3 className="font-semibold text-lg mb-2 line-clamp-1">{item.title}</h3>
          <p className="text-gray-600 text-sm mb-3 line-clamp-2">{item.description}</p>
          
          <div className="flex justify-between items-center text-sm">
            <div className="flex items-center gap-1 text-gray-500">
              {item.location && (
                <>
                  <MapPin className="h-4 w-4" />
                  <span className="line-clamp-1">{item.location}</span>
                </>
              )}
            </div>
            {item.rating && (
              <div className="flex items-center gap-1">
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                <span className="font-medium">{item.rating}</span>
              </div>
            )}
          </div>
          
          {item.price && (
            <div className="mt-3 text-lg font-bold text-blue-600">
              ${item.price}/night
            </div>
          )}
        </CardContent>
      </Card>
    </Link>
  );
}

// Mock data for fallback
const mockRecommendations: RecommendedItem[] = [
  {
    id: 'rec-1',
    title: 'Cozy Mountain Cabin',
    description: 'Perfect retreat in the mountains with stunning views',
    imageUrl: '/api/placeholder/400/300',
    price: 120,
    rating: 4.8,
    location: 'Rocky Mountains',
    type: 'stay'
  },
  {
    id: 'rec-2',
    title: 'City Food Tour',
    description: 'Discover the best local cuisine with expert guides',
    imageUrl: '/api/placeholder/400/300',
    price: 65,
    rating: 4.9,
    location: 'Downtown',
    type: 'tour'
  },
  {
    id: 'rec-3',
    title: 'Beachfront Villa',
    description: 'Luxury accommodation steps away from pristine beaches',
    imageUrl: '/api/placeholder/400/300',
    price: 250,
    rating: 4.7,
    location: 'Coastal Area',
    type: 'stay'
  },
  {
    id: 'rec-4',
    title: 'Photography Workshop',
    description: 'Learn advanced photography techniques in scenic locations',
    imageUrl: '/api/placeholder/400/300',
    price: 85,
    rating: 4.6,
    location: 'Nature Reserve',
    type: 'experience'
  },
  {
    id: 'rec-5',
    title: 'Historic District Walk',
    description: 'Explore centuries of history with knowledgeable guides',
    imageUrl: '/api/placeholder/400/300',
    price: 35,
    rating: 4.5,
    location: 'Old Town',
    type: 'tour'
  },
  {
    id: 'rec-6',
    title: 'Luxury Spa Resort',
    description: 'Rejuvenate with world-class spa treatments and amenities',
    imageUrl: '/api/placeholder/400/300',
    price: 180,
    rating: 4.9,
    location: 'Wellness Valley',
    type: 'stay'
  }
];