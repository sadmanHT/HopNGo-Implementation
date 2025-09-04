import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User } from '../api/types';

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

interface AuthActions {
  setAuth: (user: User, token: string, refreshToken: string) => void;
  clearAuth: () => void;
  setUser: (user: User) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  updateToken: (token: string, refreshToken?: string) => void;
}

type AuthStore = AuthState & AuthActions;

const initialState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
};

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      ...initialState,

      setAuth: (user, token, refreshToken) => {
        set({
          user,
          token,
          refreshToken,
          isAuthenticated: true,
          error: null,
        });
        // Store token in localStorage for API client
        localStorage.setItem('auth_token', token);
        localStorage.setItem('refresh_token', refreshToken);
      },

      clearAuth: () => {
        set(initialState);
        // Clear tokens from localStorage
        localStorage.removeItem('auth_token');
        localStorage.removeItem('refresh_token');
      },

      setUser: (user) => {
        set({ user });
      },

      setLoading: (isLoading) => {
        set({ isLoading });
      },

      setError: (error) => {
        set({ error });
      },

      updateToken: (token, refreshToken) => {
        set((state) => ({
          token,
          refreshToken: refreshToken || state.refreshToken,
        }));
        localStorage.setItem('auth_token', token);
        if (refreshToken) {
          localStorage.setItem('refresh_token', refreshToken);
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);