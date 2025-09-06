'use client';

import { useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useSearchStore, SearchResult } from '@/lib/state/search';
import { SearchBar } from '@/components/search/SearchBar';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { 
  MapPin, 
  Calendar, 
  DollarSign, 
  MessageSquare, 
  Heart,
  Share2,
  Filter,
  Loader2,
  Search as SearchIcon
} from 'lucide-react';
import { cn } from '@/lib/utils';
import Link from 'next/link';
import Image from 'next/image';

function SearchResultCard({ result }: { result: SearchResult }) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const formatPrice = (price: number, currency: string = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(price);
  };

  if (result.type === 'post') {
    return (
      <Card className="hover:shadow-md transition-shadow">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center space-x-3">
              <Avatar className="h-10 w-10">
                <AvatarImage src={result.author?.avatar} />
                <AvatarFallback>
                  {result.author?.name?.charAt(0) || 'U'}
                </AvatarFallback>
              </Avatar>
              <div>
                <p className="font-medium text-sm">{result.author?.name}</p>
                <p className="text-xs text-muted-foreground">
                  {formatDate(result.createdAt)}
                </p>
              </div>
            </div>
            <Badge variant="secondary" className="text-xs">
              Post
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="pt-0">
          <Link href={`/social/posts/${result.id}`} className="block">
            <h3 className="font-semibold text-lg mb-2 hover:text-blue-600 transition-colors">
              {result.title}
            </h3>
            <p className="text-muted-foreground text-sm mb-3 line-clamp-3">
              {result.content}
            </p>
            {result.location && (
              <div className="flex items-center text-xs text-muted-foreground mb-3">
                <MapPin className="h-3 w-3 mr-1" />
                {result.location.city}, {result.location.country}
              </div>
            )}
            {result.images && result.images.length > 0 && (
              <div className="grid grid-cols-2 gap-2 mb-3">
                {result.images.slice(0, 4).map((image, index) => (
                  <div key={index} className="relative aspect-video rounded-md overflow-hidden">
                    <Image
                      src={image}
                      alt={`Post image ${index + 1}`}
                      fill
                      className="object-cover"
                    />
                  </div>
                ))}
              </div>
            )}
          </Link>
          <div className="flex items-center justify-between pt-2 border-t">
            <div className="flex items-center space-x-4 text-xs text-muted-foreground">
              <button className="flex items-center space-x-1 hover:text-red-500 transition-colors">
                <Heart className="h-4 w-4" />
                <span>Like</span>
              </button>
              <button className="flex items-center space-x-1 hover:text-blue-500 transition-colors">
                <MessageSquare className="h-4 w-4" />
                <span>Comment</span>
              </button>
              <button className="flex items-center space-x-1 hover:text-green-500 transition-colors">
                <Share2 className="h-4 w-4" />
                <span>Share</span>
              </button>
            </div>
            {result.score && (
              <Badge variant="outline" className="text-xs">
                {Math.round(result.score * 100)}% match
              </Badge>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  // Listing card
  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <Avatar className="h-10 w-10">
              <AvatarImage src={result.author?.avatar} />
              <AvatarFallback>
                {result.author?.name?.charAt(0) || 'H'}
              </AvatarFallback>
            </Avatar>
            <div>
              <p className="font-medium text-sm">{result.author?.name}</p>
              <p className="text-xs text-muted-foreground">
                Listed {formatDate(result.createdAt)}
              </p>
            </div>
          </div>
          <Badge variant="secondary" className="text-xs">
            Listing
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        <Link href={`/places/${result.id}`} className="block">
          <h3 className="font-semibold text-lg mb-2 hover:text-blue-600 transition-colors">
            {result.title}
          </h3>
          <p className="text-muted-foreground text-sm mb-3 line-clamp-2">
            {result.content}
          </p>
          {result.location && (
            <div className="flex items-center text-sm text-muted-foreground mb-3">
              <MapPin className="h-4 w-4 mr-1" />
              {result.location.city}, {result.location.country}
            </div>
          )}
          {result.images && result.images.length > 0 && (
            <div className="relative aspect-video rounded-md overflow-hidden mb-3">
              <Image
                src={result.images[0]}
                alt={result.title}
                fill
                className="object-cover"
              />
              {result.images.length > 1 && (
                <div className="absolute top-2 right-2 bg-black/50 text-white text-xs px-2 py-1 rounded">
                  +{result.images.length - 1} more
                </div>
              )}
            </div>
          )}
          {result.price && (
            <div className="flex items-center justify-between">
              <div className="flex items-center text-lg font-semibold text-green-600">
                <DollarSign className="h-4 w-4 mr-1" />
                {formatPrice(result.price, result.currency)}
                <span className="text-sm text-muted-foreground ml-1">/ night</span>
              </div>
              {result.score && (
                <Badge variant="outline" className="text-xs">
                  {Math.round(result.score * 100)}% match
                </Badge>
              )}
            </div>
          )}
        </Link>
      </CardContent>
    </Card>
  );
}

function EmptyState({ type }: { type: string }) {
  return (
    <div className="text-center py-12">
      <SearchIcon className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
      <h3 className="text-lg font-medium text-muted-foreground mb-2">
        No {type} found
      </h3>
      <p className="text-sm text-muted-foreground">
        Try adjusting your search terms or filters
      </p>
    </div>
  );
}

export default function SearchPage() {
  const searchParams = useSearchParams();
  const initialQuery = searchParams.get('q') || '';
  
  const {
    query,
    results,
    filters,
    isLoading,
    error,
    totalResults,
    hasMore,
    search,
    loadMore,
    setQuery
  } = useSearchStore();

  const [activeTab, setActiveTab] = useState('all');

  useEffect(() => {
    if (initialQuery && initialQuery !== query) {
      setQuery(initialQuery);
      search(initialQuery, filters);
    }
  }, [initialQuery, query, search, setQuery, filters]);

  const postResults = results.filter(r => r.type === 'post');
  const listingResults = results.filter(r => r.type === 'listing');

  const getTabResults = (tab: string) => {
    switch (tab) {
      case 'posts':
        return postResults;
      case 'listings':
        return listingResults;
      default:
        return results;
    }
  };

  const getTabCount = (tab: string) => {
    switch (tab) {
      case 'posts':
        return postResults.length;
      case 'listings':
        return listingResults.length;
      default:
        return results.length;
    }
  };

  const tabResults = getTabResults(activeTab);

  return (
    <div className="container mx-auto px-4 py-6 max-w-6xl">
      {/* Search Header */}
      <div className="mb-8">
        <SearchBar className="mb-4" />
        
        {query && (
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold mb-1">
                Search Results
              </h1>
              <p className="text-muted-foreground">
                {isLoading ? 'Searching...' : `${totalResults} results for "${query}"`}
              </p>
            </div>
            <Button variant="outline" size="sm">
              <Filter className="h-4 w-4 mr-2" />
              Advanced Filters
            </Button>
          </div>
        )}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-6">
          <p className="text-red-800 text-sm">{error}</p>
        </div>
      )}

      {query && (
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-3 mb-6">
            <TabsTrigger value="all" className="flex items-center space-x-2">
              <span>All</span>
              <Badge variant="secondary" className="text-xs">
                {getTabCount('all')}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="posts" className="flex items-center space-x-2">
              <span>Posts</span>
              <Badge variant="secondary" className="text-xs">
                {getTabCount('posts')}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="listings" className="flex items-center space-x-2">
              <span>Listings</span>
              <Badge variant="secondary" className="text-xs">
                {getTabCount('listings')}
              </Badge>
            </TabsTrigger>
          </TabsList>

          <TabsContent value={activeTab}>
            {isLoading && results.length === 0 ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              </div>
            ) : tabResults.length === 0 ? (
              <EmptyState type={activeTab === 'all' ? 'results' : activeTab} />
            ) : (
              <div className="space-y-4">
                <div className="grid gap-4">
                  {tabResults.map((result) => (
                    <SearchResultCard key={`${result.type}-${result.id}`} result={result} />
                  ))}
                </div>
                
                {hasMore && (
                  <div className="flex justify-center pt-6">
                    <Button 
                      onClick={loadMore} 
                      disabled={isLoading}
                      variant="outline"
                    >
                      {isLoading ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Loading...
                        </>
                      ) : (
                        'Load More Results'
                      )}
                    </Button>
                  </div>
                )}
              </div>
            )}
          </TabsContent>
        </Tabs>
      )}

      {!query && (
        <div className="text-center py-12">
          <SearchIcon className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
          <h2 className="text-xl font-semibold mb-2">Search HopNGo</h2>
          <p className="text-muted-foreground mb-6">
            Find posts, listings, and places from our community
          </p>
          <div className="flex flex-wrap justify-center gap-2">
            <Badge variant="outline">Travel tips</Badge>
            <Badge variant="outline">Accommodations</Badge>
            <Badge variant="outline">Local experiences</Badge>
            <Badge variant="outline">Hidden gems</Badge>
          </div>
        </div>
      )}
    </div>
  );
}