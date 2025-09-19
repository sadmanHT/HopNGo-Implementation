'use client';

import { useState, useRef, useEffect } from 'react';
import { Search, X, Filter, Camera, Languages } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { useSearchStore } from '@/lib/state/search';
import { cn } from '@/lib/utils';
import { useRouter } from 'next/navigation';
import { useFeatureFlag } from '@/lib/flags';
import { VisualSearch } from './VisualSearch';
import { VisualSearchDrawer } from './VisualSearchDrawer';
import { useTransliterationSearch, containsBangla, containsEnglish } from '@/utils/transliteration';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';

interface SearchBarProps {
  className?: string;
  placeholder?: string;
  showFilters?: boolean;
}

export function SearchBar({ 
  className, 
  placeholder = "Search posts, listings, places...",
  showFilters = true 
}: SearchBarProps) {
  const router = useRouter();
  const [localQuery, setLocalQuery] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [showVisualSearch, setShowVisualSearch] = useState(false);
  const [isTransliterating, setIsTransliterating] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);
  
  // Transliteration hook
  const { searchWithTransliteration, createSmartSearchQuery } = useTransliterationSearch();
  
  // Feature flags for visual search
  const { isEnabled: isVisualSearchEnabled } = useFeatureFlag('visual-search');
  const { isEnabled: isVisualSearchV2Enabled } = useFeatureFlag('visual_search_v2');
  
  const {
    query,
    suggestions,
    filters,
    isLoading,
    setQuery,
    setOpen,
    search,
    getSuggestions,
    clearResults
  } = useSearchStore();

  // Debounced suggestions with transliteration
  useEffect(() => {
    const timer = setTimeout(() => {
      if (localQuery.length >= 2) {
        // Generate transliterated suggestions
        const smartQuery = createSmartSearchQuery(localQuery);
        const enhancedSuggestions = [
          localQuery,
          ...smartQuery.variations.filter(v => v !== localQuery && v.trim().length > 0)
        ];
        
        // Get regular suggestions and merge with transliterated ones
        getSuggestions(localQuery);
        setShowSuggestions(true);
      } else {
        setShowSuggestions(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [localQuery, getSuggestions, createSmartSearchQuery]);

  // Handle clicks outside to close suggestions
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = async (searchQuery: string) => {
    if (!searchQuery.trim()) return;
    
    setQuery(searchQuery);
    setLocalQuery(searchQuery);
    setShowSuggestions(false);
    setIsTransliterating(true);
    
    try {
      // Use transliteration-enhanced search
      await searchWithTransliteration(searchQuery, async (searchTerms: string[]) => {
        // Navigate to search results page
        router.push('/search');
        
        // Perform the search with all variations
        const results = await search(searchTerms.join(' OR '), filters);
        setOpen(true);
        return results;
      });
    } catch (error) {
      console.error('Search failed:', error);
      // Fallback to regular search
      router.push('/search');
      await search(searchQuery, filters);
      setOpen(true);
    } finally {
      setIsTransliterating(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalQuery(value);
    
    // Update placeholder based on detected language
    const hasEnglish = containsEnglish(value);
    const hasBangla = containsBangla(value);
    
    if (hasBangla && !hasEnglish) {
      // Bangla input detected
    } else if (hasEnglish && !hasBangla) {
      // English input detected  
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSearch(localQuery);
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
      inputRef.current?.blur();
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    handleSearch(suggestion);
  };

  const handleClear = () => {
    setLocalQuery('');
    setQuery('');
    clearResults();
    setShowSuggestions(false);
    inputRef.current?.focus();
  };

  const handleVisualSearch = async (imageFile: File) => {
    // In a real implementation, this would upload the image to a visual search API
    // For demo purposes, we'll simulate a search based on image analysis
    const mockSearchTerms = [
      'beach resort tropical paradise',
      'mountain cabin forest retreat', 
      'city skyline urban adventure',
      'historic architecture cultural site',
      'countryside villa peaceful getaway'
    ];
    
    const randomTerm = mockSearchTerms[Math.floor(Math.random() * mockSearchTerms.length)];
    await handleSearch(randomTerm);
    setShowVisualSearch(false);
  };

  const getFilterCount = () => {
    let count = 0;
    if (filters.type && filters.type !== 'all') count++;
    if (filters.location) count++;
    if (filters.priceRange?.min !== undefined || filters.priceRange?.max !== undefined) count++;
    if (filters.dateRange?.from || filters.dateRange?.to) count++;
    return count;
  };

  const filterCount = getFilterCount();

  return (
    <div className={cn("relative w-full max-w-2xl", className)}>
      <div className="relative flex items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            ref={inputRef}
            type="text"
            placeholder={placeholder}
            value={localQuery}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            onFocus={() => {
              if (localQuery.length >= 2) {
                setShowSuggestions(true);
              }
            }}
            className={cn(
              "pl-10 pr-10 h-10",
              containsBangla(localQuery) && "font-bengali",
              isTransliterating && "animate-pulse"
            )}
            disabled={isLoading || isTransliterating}
          />
          {/* Language indicator */}
          {localQuery && (containsBangla(localQuery) || containsEnglish(localQuery)) && (
            <div className="absolute right-12 top-1/2 -translate-y-1/2">
              <Languages className={cn(
                "h-3 w-3",
                containsBangla(localQuery) && containsEnglish(localQuery) 
                  ? "text-blue-500" 
                  : containsBangla(localQuery) 
                    ? "text-green-500" 
                    : "text-gray-500",
                isTransliterating && "animate-spin"
              )} />
            </div>
          )}
          {localQuery && (
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClear}
              className="absolute right-2 top-1/2 h-6 w-6 -translate-y-1/2 p-0 hover:bg-transparent"
              disabled={isTransliterating}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
        
        <div className="flex items-center space-x-2 ml-2">
          {/* Visual Search Drawer (V2) */}
          {isVisualSearchV2Enabled && (
            <VisualSearchDrawer />
          )}
          
          {/* Legacy Visual Search Button */}
          {isVisualSearchEnabled && !isVisualSearchV2Enabled && (
            <Dialog open={showVisualSearch} onOpenChange={setShowVisualSearch}>
              <DialogTrigger asChild>
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="h-10"
                  title="Visual Search"
                >
                  <Camera className="h-4 w-4" />
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Visual Search</DialogTitle>
                </DialogHeader>
                <VisualSearch 
                  onSearch={handleVisualSearch}
                  className="border-0 shadow-none"
                />
              </DialogContent>
            </Dialog>
          )}
          
          {/* Filters */}
          {showFilters && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="h-10 relative"
                >
                  <Filter className="h-4 w-4 mr-1" />
                  Filters
                  {filterCount > 0 && (
                    <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center">
                      {filterCount}
                    </span>
                  )}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuItem onClick={() => router.push('/search/filters')}>
                  Advanced Filters
                </DropdownMenuItem>
                {filterCount > 0 && (
                  <DropdownMenuItem 
                    onClick={() => {
                      // Reset filters logic would go here
                      // For now, just navigate to clear filters
                      router.push('/search');
                    }}
                    className="text-red-600"
                  >
                    Clear Filters ({filterCount})
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </div>

      {/* Suggestions Dropdown */}
      {showSuggestions && (suggestions.length > 0 || localQuery.length >= 2) && (
        <div 
          ref={suggestionsRef}
          className="absolute top-full left-0 right-0 z-50 mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-60 overflow-y-auto"
        >
          {/* Original suggestions */}
          {suggestions.map((suggestion, index) => (
            <button
              key={`original-${index}`}
              onClick={() => handleSuggestionClick(suggestion)}
              className="w-full px-4 py-2 text-left hover:bg-gray-50 focus:bg-gray-50 focus:outline-none first:rounded-t-md last:rounded-b-md"
            >
              <div className="flex items-center">
                <Search className="h-4 w-4 text-muted-foreground mr-3" />
                <span className="text-sm">{suggestion}</span>
              </div>
            </button>
          ))}
          
          {/* Transliterated suggestions */}
          {localQuery.length >= 2 && (() => {
            const smartQuery = createSmartSearchQuery(localQuery);
            const transliteratedSuggestions = smartQuery.variations.filter(
              v => v !== localQuery && v.trim().length > 0 && !suggestions.includes(v)
            );
            
            return transliteratedSuggestions.map((suggestion, index) => (
              <button
                key={`transliterated-${index}`}
                onClick={() => handleSuggestionClick(suggestion)}
                className="w-full px-4 py-2 text-left hover:bg-blue-50 focus:bg-blue-50 focus:outline-none border-l-2 border-l-blue-200"
              >
                <div className="flex items-center">
                  <Languages className="h-4 w-4 text-blue-500 mr-3" />
                  <span className={cn(
                    "text-sm",
                    containsBangla(suggestion) && "font-bengali"
                  )}>
                    {suggestion}
                  </span>
                  <span className="ml-auto text-xs text-blue-500">
                    {containsBangla(suggestion) ? 'বাংলা' : 'English'}
                  </span>
                </div>
              </button>
            ));
          })()}
        </div>
      )}
    </div>
  );
}