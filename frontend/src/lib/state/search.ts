import { create } from 'zustand';
import { SearchAPI } from '../api/search';

export interface SearchResult {
  id: string;
  type: 'post' | 'listing';
  title: string;
  content: string;
  author?: {
    id: string;
    name: string;
    avatar?: string;
  };
  location?: {
    city: string;
    country: string;
  };
  price?: number;
  currency?: string;
  images?: string[];
  createdAt: string;
  score?: number;
}

export interface SearchFilters {
  type?: 'all' | 'posts' | 'listings';
  location?: string;
  priceRange?: {
    min?: number;
    max?: number;
  };
  dateRange?: {
    from?: string;
    to?: string;
  };
}

interface SearchState {
  query: string;
  results: SearchResult[];
  filters: SearchFilters;
  isLoading: boolean;
  error: string | null;
  isOpen: boolean;
  suggestions: string[];
  totalResults: number;
  currentPage: number;
  hasMore: boolean;
}

interface SearchActions {
  setQuery: (query: string) => void;
  setFilters: (filters: SearchFilters) => void;
  setResults: (results: SearchResult[], total: number, hasMore: boolean) => void;
  appendResults: (results: SearchResult[], hasMore: boolean) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  setOpen: (open: boolean) => void;
  setSuggestions: (suggestions: string[]) => void;
  clearResults: () => void;
  search: (query: string, filters?: SearchFilters, page?: number) => Promise<void>;
  loadMore: () => Promise<void>;
  getSuggestions: (query: string) => Promise<void>;
}

type SearchStore = SearchState & SearchActions;

const initialState: SearchState = {
  query: '',
  results: [],
  filters: { type: 'all' },
  isLoading: false,
  error: null,
  isOpen: false,
  suggestions: [],
  totalResults: 0,
  currentPage: 0,
  hasMore: false,
};

export const useSearchStore = create<SearchStore>()((set, get) => ({
  ...initialState,

  setQuery: (query) => set({ query }),
  
  setFilters: (filters) => set({ filters }),
  
  setResults: (results, totalResults, hasMore) => set({ 
    results, 
    totalResults, 
    hasMore,
    currentPage: 1 
  }),
  
  appendResults: (newResults, hasMore) => set((state) => ({
    results: [...state.results, ...newResults],
    hasMore,
    currentPage: state.currentPage + 1
  })),
  
  setLoading: (isLoading) => set({ isLoading }),
  
  setError: (error) => set({ error }),
  
  setOpen: (isOpen) => set({ isOpen }),
  
  setSuggestions: (suggestions) => set({ suggestions }),
  
  clearResults: () => set({ 
    results: [], 
    totalResults: 0, 
    currentPage: 0, 
    hasMore: false,
    error: null 
  }),

  search: async (query, filters = {}, page = 1) => {
    const state = get();
    const searchFilters = { ...state.filters, ...filters };
    
    set({ isLoading: true, error: null });
    
    try {
      const response = await SearchAPI.search({
        q: query,
        page,
        size: 20,
        ...searchFilters
      });
      
      const { results, total, hasMore } = response;
      
      if (page === 1) {
        set({ 
          results, 
          totalResults: total, 
          hasMore,
          currentPage: 1,
          query,
          filters: searchFilters
        });
      } else {
        get().appendResults(results, hasMore);
      }
    } catch (error: any) {
      set({ 
        error: error.response?.data?.message || 'Search failed. Please try again.',
        results: [],
        totalResults: 0,
        hasMore: false
      });
    } finally {
      set({ isLoading: false });
    }
  },

  loadMore: async () => {
    const { query, filters, currentPage, hasMore, isLoading } = get();
    
    if (!hasMore || isLoading || !query) return;
    
    await get().search(query, filters, currentPage + 1);
  },

  getSuggestions: async (query) => {
    if (!query || query.length < 2) {
      set({ suggestions: [] });
      return;
    }
    
    try {
      const response = await SearchAPI.getSuggestions(query);
      set({ suggestions: response.suggestions || [] });
    } catch (error) {
      // Silently fail for suggestions
      set({ suggestions: [] });
    }
  },
}));