'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play, Database, Users, Eye, EyeOff, Sparkles, Info } from 'lucide-react';
import { InteractiveButton } from '../ui/micro-interactions';
import { AccessibleModal } from '../ui/focus-management';

// Demo mode context
interface DemoContextType {
  isDemoMode: boolean;
  toggleDemoMode: () => void;
  demoData: {
    users: number;
    listings: number;
    itineraries: number;
    conversations: number;
  };
}

const DemoContext = React.createContext<DemoContextType | null>(null);

// Demo mode provider
interface DemoProviderProps {
  children: React.ReactNode;
}

export const DemoProvider: React.FC<DemoProviderProps> = ({ children }) => {
  const [isDemoMode, setIsDemoMode] = useState(false);
  const [demoData] = useState({
    users: 25,
    listings: 150,
    itineraries: 12,
    conversations: 45
  });

  // Load demo mode state from localStorage
  useEffect(() => {
    const savedDemoMode = localStorage.getItem('hopngo-demo-mode');
    if (savedDemoMode === 'true') {
      setIsDemoMode(true);
    }
  }, []);

  // Save demo mode state to localStorage
  useEffect(() => {
    localStorage.setItem('hopngo-demo-mode', isDemoMode.toString());
    
    // Add demo mode class to body for styling
    if (isDemoMode) {
      document.body.classList.add('demo-mode');
    } else {
      document.body.classList.remove('demo-mode');
    }
  }, [isDemoMode]);

  const toggleDemoMode = () => {
    setIsDemoMode(prev => !prev);
  };

  return (
    <DemoContext.Provider value={{
      isDemoMode,
      toggleDemoMode,
      demoData
    }}>
      {children}
    </DemoContext.Provider>
  );
};

// Hook to use demo context
export const useDemoMode = () => {
  const context = React.useContext(DemoContext);
  if (!context) {
    throw new Error('useDemoMode must be used within a DemoProvider');
  }
  return context;
};

// Demo toggle component
interface DemoToggleProps {
  className?: string;
  showLabel?: boolean;
  showStats?: boolean;
  variant?: 'compact' | 'full' | 'floating';
}

export const DemoToggle: React.FC<DemoToggleProps> = ({
  className = '',
  showLabel = true,
  showStats = false,
  variant = 'compact'
}) => {
  const { isDemoMode, toggleDemoMode, demoData } = useDemoMode();
  const [showInfo, setShowInfo] = useState(false);

  const handleToggle = () => {
    toggleDemoMode();
    
    // Show brief notification
    const message = isDemoMode 
      ? 'Switched to live data' 
      : 'Switched to demo mode with sample data';
    
    // You can integrate with your notification system here
    console.log(message);
  };

  if (variant === 'floating') {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        className={`fixed bottom-6 right-6 z-40 ${className}`}
        data-tour="demo-toggle"
      >
        <div className="bg-white rounded-full shadow-lg border border-gray-200 p-2">
          <InteractiveButton
            variant={isDemoMode ? 'primary' : 'ghost'}
            size="sm"
            onClick={handleToggle}
            className="rounded-full w-12 h-12 flex items-center justify-center"
            aria-label={isDemoMode ? 'Exit demo mode' : 'Enter demo mode'}
          >
            {isDemoMode ? (
              <Play className="w-5 h-5" />
            ) : (
              <Database className="w-5 h-5" />
            )}
          </InteractiveButton>
        </div>
        
        {/* Floating tooltip */}
        <AnimatePresence>
          {isDemoMode && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className="absolute right-16 top-1/2 transform -translate-y-1/2 bg-blue-600 text-white px-3 py-2 rounded-lg text-sm whitespace-nowrap shadow-lg"
            >
              Demo Mode Active
              <div className="absolute right-0 top-1/2 transform translate-x-1 -translate-y-1/2 w-0 h-0 border-l-4 border-l-blue-600 border-t-4 border-t-transparent border-b-4 border-b-transparent" />
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    );
  }

  if (variant === 'full') {
    return (
      <div className={`bg-white rounded-xl border border-gray-200 p-6 ${className}`} data-tour="demo-toggle">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className={`p-2 rounded-lg ${
              isDemoMode ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-600'
            }`}>
              {isDemoMode ? <Play className="w-6 h-6" /> : <Database className="w-6 h-6" />}
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">
                {isDemoMode ? 'Demo Mode Active' : 'Live Data Mode'}
              </h3>
              <p className="text-sm text-gray-500">
                {isDemoMode 
                  ? 'Using sample data for demonstration'
                  : 'Using real-time data from the platform'
                }
              </p>
            </div>
          </div>
          
          <InteractiveButton
            variant="ghost"
            size="sm"
            onClick={() => setShowInfo(true)}
            className="p-2"
            aria-label="More information"
          >
            <Info className="w-4 h-4" />
          </InteractiveButton>
        </div>

        {showStats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-gray-900">{demoData.users}</div>
              <div className="text-xs text-gray-500">Demo Users</div>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-gray-900">{demoData.listings}</div>
              <div className="text-xs text-gray-500">Listings</div>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-gray-900">{demoData.itineraries}</div>
              <div className="text-xs text-gray-500">Itineraries</div>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-gray-900">{demoData.conversations}</div>
              <div className="text-xs text-gray-500">Conversations</div>
            </div>
          </div>
        )}

        <InteractiveButton
          variant={isDemoMode ? 'secondary' : 'primary'}
          onClick={handleToggle}
          className="w-full flex items-center justify-center space-x-2"
        >
          {isDemoMode ? (
            <>
              <EyeOff className="w-4 h-4" />
              <span>Exit Demo Mode</span>
            </>
          ) : (
            <>
              <Eye className="w-4 h-4" />
              <span>Try Demo Mode</span>
            </>
          )}
        </InteractiveButton>

        {/* Info Modal */}
        <AccessibleModal
          isOpen={showInfo}
          onClose={() => setShowInfo(false)}
          title="Demo Mode Information"
        >
          <div className="p-6">
            <div className="flex items-center space-x-3 mb-4">
              <Sparkles className="w-8 h-8 text-blue-600" />
              <h3 className="text-xl font-semibold text-gray-900">What is Demo Mode?</h3>
            </div>
            
            <div className="space-y-4 text-gray-700">
              <p>
                Demo mode lets you explore HopNGo with curated sample data showcasing 
                the best of Bangladesh tourism without affecting real user data.
              </p>
              
              <div className="bg-blue-50 p-4 rounded-lg">
                <h4 className="font-medium text-blue-900 mb-2">Demo Features:</h4>
                <ul className="space-y-1 text-sm text-blue-800">
                  <li>• Curated posts from iconic Bangladesh destinations</li>
                  <li>• Sample accommodations and tour listings</li>
                  <li>• Pre-built itinerary templates</li>
                  <li>• Simulated chat conversations</li>
                  <li>• Realistic user interactions</li>
                </ul>
              </div>
              
              <div className="bg-amber-50 p-4 rounded-lg">
                <h4 className="font-medium text-amber-900 mb-2">Perfect for:</h4>
                <ul className="space-y-1 text-sm text-amber-800">
                  <li>• Investors and stakeholders</li>
                  <li>• Hackathon demonstrations</li>
                  <li>• Feature showcases</li>
                  <li>• User onboarding</li>
                </ul>
              </div>
            </div>
            
            <div className="flex justify-end mt-6">
              <InteractiveButton
                variant="primary"
                onClick={() => setShowInfo(false)}
              >
                Got it!
              </InteractiveButton>
            </div>
          </div>
        </AccessibleModal>
      </div>
    );
  }

  // Compact variant (default)
  return (
    <div className={`flex items-center space-x-3 ${className}`} data-tour="demo-toggle">
      {showLabel && (
        <span className="text-sm font-medium text-gray-700">
          {isDemoMode ? 'Demo Mode' : 'Live Data'}
        </span>
      )}
      
      <motion.button
        onClick={handleToggle}
        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
          isDemoMode ? 'bg-blue-600' : 'bg-gray-200'
        }`}
        whileTap={{ scale: 0.95 }}
        aria-label={isDemoMode ? 'Exit demo mode' : 'Enter demo mode'}
      >
        <motion.span
          className={`inline-block h-4 w-4 transform rounded-full bg-white shadow-lg transition-transform ${
            isDemoMode ? 'translate-x-6' : 'translate-x-1'
          }`}
          layout
        />
      </motion.button>
      
      {isDemoMode && (
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="flex items-center space-x-1 text-blue-600"
        >
          <Sparkles className="w-4 h-4" />
          {showLabel && <span className="text-xs font-medium">Sample Data</span>}
        </motion.div>
      )}
    </div>
  );
};

// Demo banner component
export const DemoBanner: React.FC = () => {
  const { isDemoMode, toggleDemoMode } = useDemoMode();
  const [isVisible, setIsVisible] = useState(true);

  if (!isDemoMode || !isVisible) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: -50 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -50 }}
      className="bg-gradient-to-r from-blue-600 to-purple-600 text-white px-4 py-2 relative"
    >
      <div className="flex items-center justify-center space-x-4 max-w-4xl mx-auto">
        <Sparkles className="w-5 h-5" />
        <span className="text-sm font-medium">
          You're in Demo Mode - Exploring with sample Bangladesh tourism data
        </span>
        <InteractiveButton
          variant="ghost"
          size="sm"
          onClick={toggleDemoMode}
          className="text-white hover:bg-white/20 text-xs"
        >
          Exit Demo
        </InteractiveButton>
        <button
          onClick={() => setIsVisible(false)}
          className="text-white/80 hover:text-white"
          aria-label="Hide banner"
        >
          ×
        </button>
      </div>
    </motion.div>
  );
};

export default DemoToggle;