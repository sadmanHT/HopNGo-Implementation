import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

// Types
export interface FeatureFlag {
  id: number;
  key: string;
  description?: string;
  enabled: boolean;
  payload?: Record<string, any>;
  created_at: string;
  updated_at: string;
}

export interface Experiment {
  id: number;
  key: string;
  description?: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  traffic_pct: number;
  variants: ExperimentVariant[];
  created_at: string;
  updated_at: string;
}

export interface ExperimentVariant {
  id: number;
  name: string;
  weight_pct: number;
  payload?: Record<string, any>;
  created_at: string;
  updated_at: string;
}

export interface Assignment {
  id: number;
  experiment_key: string;
  user_id: string;
  variant_name: string;
  assigned_at: string;
}

// Store state interface
interface FlagsStore {
  // Feature flags
  flags: Record<string, FeatureFlag>;
  flagsLoading: boolean;
  flagsError: string | null;
  
  // Experiments
  experiments: Record<string, Experiment>;
  experimentsLoading: boolean;
  experimentsError: string | null;
  
  // User assignments
  assignments: Record<string, Assignment>;
  assignmentsLoading: boolean;
  assignmentsError: string | null;
  
  // Actions
  fetchFlags: () => Promise<void>;
  fetchExperiments: () => Promise<void>;
  fetchUserAssignments: (userId: string) => Promise<void>;
  assignUserToExperiment: (experimentKey: string, userId: string) => Promise<Assignment | null>;
  
  // Getters
  isFeatureEnabled: (key: string) => boolean;
  getFeaturePayload: (key: string) => Record<string, any> | null;
  getUserVariant: (experimentKey: string, userId: string) => string | null;
  getVariantPayload: (experimentKey: string, userId: string) => Record<string, any> | null;
}

// API base URL
const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const CONFIG_API = `${API_BASE}/api/v1/config`;

// Zustand store
export const useFlagsStore = create<FlagsStore>()(devtools((set, get) => ({
  // Initial state
  flags: {},
  flagsLoading: false,
  flagsError: null,
  
  experiments: {},
  experimentsLoading: false,
  experimentsError: null,
  
  assignments: {},
  assignmentsLoading: false,
  assignmentsError: null,
  
  // Actions
  fetchFlags: async () => {
    set({ flagsLoading: true, flagsError: null });
    
    try {
      const response = await fetch(`${CONFIG_API}/feature-flags`);
      if (!response.ok) {
        throw new Error(`Failed to fetch flags: ${response.statusText}`);
      }
      
      const data = await response.json();
      const flags = data.content || data; // Handle paginated or direct response
      
      const flagsMap = Array.isArray(flags) 
        ? flags.reduce((acc: Record<string, FeatureFlag>, flag: FeatureFlag) => {
            acc[flag.key] = flag;
            return acc;
          }, {})
        : {};
      
      set({ flags: flagsMap, flagsLoading: false });
    } catch (error) {
      set({ 
        flagsError: error instanceof Error ? error.message : 'Unknown error',
        flagsLoading: false 
      });
    }
  },
  
  fetchExperiments: async () => {
    set({ experimentsLoading: true, experimentsError: null });
    
    try {
      const response = await fetch(`${CONFIG_API}/experiments/active`);
      if (!response.ok) {
        throw new Error(`Failed to fetch experiments: ${response.statusText}`);
      }
      
      const experiments = await response.json();
      
      const experimentsMap = Array.isArray(experiments)
        ? experiments.reduce((acc: Record<string, Experiment>, exp: Experiment) => {
            acc[exp.key] = exp;
            return acc;
          }, {})
        : {};
      
      set({ experiments: experimentsMap, experimentsLoading: false });
    } catch (error) {
      set({ 
        experimentsError: error instanceof Error ? error.message : 'Unknown error',
        experimentsLoading: false 
      });
    }
  },
  
  fetchUserAssignments: async (userId: string) => {
    set({ assignmentsLoading: true, assignmentsError: null });
    
    try {
      const response = await fetch(`${CONFIG_API}/experiments/assignments/${userId}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch assignments: ${response.statusText}`);
      }
      
      const assignments = await response.json();
      
      const assignmentsMap = Array.isArray(assignments)
        ? assignments.reduce((acc: Record<string, Assignment>, assignment: Assignment) => {
            acc[assignment.experiment_key] = assignment;
            return acc;
          }, {})
        : {};
      
      set({ assignments: assignmentsMap, assignmentsLoading: false });
    } catch (error) {
      set({ 
        assignmentsError: error instanceof Error ? error.message : 'Unknown error',
        assignmentsLoading: false 
      });
    }
  },
  
  assignUserToExperiment: async (experimentKey: string, userId: string) => {
    try {
      const response = await fetch(`${CONFIG_API}/experiments/${experimentKey}/assign`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ userId }),
      });
      
      if (!response.ok) {
        throw new Error(`Failed to assign user: ${response.statusText}`);
      }
      
      const assignment = await response.json();
      
      // Update assignments in store
      set((state) => ({
        assignments: {
          ...state.assignments,
          [experimentKey]: assignment,
        },
      }));
      
      return assignment;
    } catch (error) {
      console.error('Error assigning user to experiment:', error);
      return null;
    }
  },
  
  // Getters
  isFeatureEnabled: (key: string) => {
    const { flags } = get();
    return flags[key]?.enabled || false;
  },
  
  getFeaturePayload: (key: string) => {
    const { flags } = get();
    return flags[key]?.payload || null;
  },
  
  getUserVariant: (experimentKey: string, userId: string) => {
    const { assignments } = get();
    return assignments[experimentKey]?.variant_name || null;
  },
  
  getVariantPayload: (experimentKey: string, userId: string) => {
    const { experiments, assignments } = get();
    const assignment = assignments[experimentKey];
    const experiment = experiments[experimentKey];
    
    if (!assignment || !experiment) {
      return null;
    }
    
    const variant = experiment.variants.find(v => v.name === assignment.variant_name);
    return variant?.payload || null;
  },
}), {
  name: 'flags-store',
}));

// React hooks for easier usage
export const useFeatureFlag = (key: string) => {
  const isEnabled = useFlagsStore(state => state.isFeatureEnabled(key));
  const payload = useFlagsStore(state => state.getFeaturePayload(key));
  const loading = useFlagsStore(state => state.flagsLoading);
  const error = useFlagsStore(state => state.flagsError);
  
  return { isEnabled, payload, loading, error };
};

export const useExperiment = (experimentKey: string, userId?: string) => {
  const variant = useFlagsStore(state => state.getUserVariant(experimentKey, userId || ''));
  const payload = useFlagsStore(state => state.getVariantPayload(experimentKey, userId || ''));
  const assignUser = useFlagsStore(state => state.assignUserToExperiment);
  const loading = useFlagsStore(state => state.experimentsLoading || state.assignmentsLoading);
  const error = useFlagsStore(state => state.experimentsError || state.assignmentsError);
  
  // Simple trackEvent function for experiment tracking
  const trackEvent = (eventName: string, properties?: Record<string, any>) => {
    // In a real implementation, this would send to analytics service
    console.log(`Experiment Event: ${eventName}`, { experimentKey, variant, ...properties });
  };
  
  return { variant, payload, assignUser, loading, error, trackEvent };
};

// Initialize function to be called on app startup
export const initializeFlags = async (userId?: string) => {
  const store = useFlagsStore.getState();
  
  // Fetch flags and experiments in parallel
  await Promise.all([
    store.fetchFlags(),
    store.fetchExperiments(),
  ]);
  
  // Fetch user assignments if userId is provided
  if (userId) {
    await store.fetchUserAssignments(userId);
  }
};