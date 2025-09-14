'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Wifi, 
  WifiOff, 
  Download, 
  Trash2, 
  MapPin, 
  Calendar, 
  Users, 
  DollarSign,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  Smartphone
} from 'lucide-react';
import { InteractiveButton } from '../ui/micro-interactions';
import { AccessibleModal } from '../ui/focus-management';

// Offline itinerary interface
interface OfflineItinerary {
  id: string;
  title: string;
  description: string;
  duration: number;
  estimatedCost: number;
  difficulty: 'Easy' | 'Moderate' | 'Challenging';
  destinations: string[];
  downloadedAt: Date;
  size: number; // in MB
  thumbnail: string;
  days: {
    dayNumber: number;
    title: string;
    activities: {
      title: string;
      location: string;
      time: string;
      cost: number;
    }[];
  }[];
}

// Offline status interface
interface OfflineStatus {
  isOnline: boolean;
  lastSync: Date | null;
  totalStorage: number;
  usedStorage: number;
  downloadedItineraries: number;
}

// Sample offline itineraries
const sampleItineraries: OfflineItinerary[] = [
  {
    id: 'offline-1',
    title: 'Cox\'s Bazar Beach Paradise',
    description: 'Explore the world\'s longest natural sea beach with stunning sunsets and local seafood.',
    duration: 3,
    estimatedCost: 15000,
    difficulty: 'Easy',
    destinations: ['Cox\'s Bazar', 'Inani Beach', 'Himchari'],
    downloadedAt: new Date(),
    size: 25.4,
    thumbnail: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
    days: [
      {
        dayNumber: 1,
        title: 'Arrival & Beach Exploration',
        activities: [
          { title: 'Check-in Hotel', location: 'Cox\'s Bazar', time: '2:00 PM', cost: 0 },
          { title: 'Beach Walk', location: 'Cox\'s Bazar Beach', time: '4:00 PM', cost: 0 },
          { title: 'Sunset Viewing', location: 'Laboni Beach', time: '6:00 PM', cost: 0 }
        ]
      },
      {
        dayNumber: 2,
        title: 'Adventure Day',
        activities: [
          { title: 'Inani Beach Visit', location: 'Inani Beach', time: '9:00 AM', cost: 500 },
          { title: 'Himchari Waterfall', location: 'Himchari', time: '2:00 PM', cost: 300 },
          { title: 'Local Seafood Dinner', location: 'Cox\'s Bazar', time: '7:00 PM', cost: 800 }
        ]
      },
      {
        dayNumber: 3,
        title: 'Departure',
        activities: [
          { title: 'Souvenir Shopping', location: 'Cox\'s Bazar Market', time: '10:00 AM', cost: 1000 },
          { title: 'Departure', location: 'Cox\'s Bazar', time: '2:00 PM', cost: 0 }
        ]
      }
    ]
  },
  {
    id: 'offline-2',
    title: 'Sajek Valley Adventure',
    description: 'Experience the Queen of Hills with breathtaking cloud views and tribal culture.',
    duration: 4,
    estimatedCost: 25000,
    difficulty: 'Moderate',
    destinations: ['Sajek Valley', 'Ruilui Para', 'Konglak Para'],
    downloadedAt: new Date(),
    size: 32.1,
    thumbnail: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
    days: [
      {
        dayNumber: 1,
        title: 'Journey to Sajek',
        activities: [
          { title: 'Travel to Sajek', location: 'Dhaka to Sajek', time: '6:00 AM', cost: 2000 },
          { title: 'Check-in Resort', location: 'Sajek Valley', time: '4:00 PM', cost: 0 },
          { title: 'Sunset Point Visit', location: 'Sajek Valley', time: '6:00 PM', cost: 0 }
        ]
      },
      {
        dayNumber: 2,
        title: 'Cloud Hunting',
        activities: [
          { title: 'Sunrise Viewing', location: 'Sajek Peak', time: '5:30 AM', cost: 0 },
          { title: 'Ruilui Para Visit', location: 'Ruilui Para', time: '10:00 AM', cost: 500 },
          { title: 'Tribal Cultural Show', location: 'Sajek Valley', time: '7:00 PM', cost: 300 }
        ]
      },
      {
        dayNumber: 3,
        title: 'Exploration Day',
        activities: [
          { title: 'Konglak Para Trek', location: 'Konglak Para', time: '8:00 AM', cost: 800 },
          { title: 'Local Lunch', location: 'Tribal Village', time: '1:00 PM', cost: 400 },
          { title: 'Helicopter View Point', location: 'Sajek Valley', time: '4:00 PM', cost: 1000 }
        ]
      },
      {
        dayNumber: 4,
        title: 'Return Journey',
        activities: [
          { title: 'Final Sunrise', location: 'Sajek Peak', time: '5:30 AM', cost: 0 },
          { title: 'Return to Dhaka', location: 'Sajek to Dhaka', time: '10:00 AM', cost: 2000 }
        ]
      }
    ]
  },
  {
    id: 'offline-3',
    title: 'Srimangal Tea Garden Tour',
    description: 'Discover the tea capital of Bangladesh with lush green gardens and wildlife.',
    duration: 2,
    estimatedCost: 12000,
    difficulty: 'Easy',
    destinations: ['Srimangal', 'Lawachara Forest', 'Tea Gardens'],
    downloadedAt: new Date(),
    size: 18.7,
    thumbnail: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
    days: [
      {
        dayNumber: 1,
        title: 'Tea Garden Experience',
        activities: [
          { title: 'Arrival in Srimangal', location: 'Srimangal', time: '10:00 AM', cost: 1500 },
          { title: 'Tea Garden Tour', location: 'Malnichhara Tea Garden', time: '2:00 PM', cost: 300 },
          { title: 'Tea Tasting Session', location: 'Tea Factory', time: '4:00 PM', cost: 200 }
        ]
      },
      {
        dayNumber: 2,
        title: 'Wildlife & Departure',
        activities: [
          { title: 'Lawachara Forest Trek', location: 'Lawachara National Park', time: '8:00 AM', cost: 500 },
          { title: 'Wildlife Spotting', location: 'Lawachara Forest', time: '10:00 AM', cost: 0 },
          { title: 'Return Journey', location: 'Srimangal to Dhaka', time: '3:00 PM', cost: 1500 }
        ]
      }
    ]
  }
];

// Offline demo context
interface OfflineDemoContextType {
  isOfflineMode: boolean;
  offlineStatus: OfflineStatus;
  downloadedItineraries: OfflineItinerary[];
  toggleOfflineMode: () => void;
  downloadItinerary: (itinerary: OfflineItinerary) => Promise<void>;
  removeItinerary: (id: string) => void;
  syncData: () => Promise<void>;
}

const OfflineDemoContext = React.createContext<OfflineDemoContextType | null>(null);

// Offline demo provider
interface OfflineDemoProviderProps {
  children: React.ReactNode;
}

export const OfflineDemoProvider: React.FC<OfflineDemoProviderProps> = ({ children }) => {
  const [isOfflineMode, setIsOfflineMode] = useState(false);
  const [downloadedItineraries, setDownloadedItineraries] = useState<OfflineItinerary[]>([]);
  const [offlineStatus, setOfflineStatus] = useState<OfflineStatus>({
    isOnline: navigator.onLine,
    lastSync: null,
    totalStorage: 100, // MB
    usedStorage: 0,
    downloadedItineraries: 0
  });

  // Monitor online/offline status
  useEffect(() => {
    const handleOnline = () => {
      setOfflineStatus(prev => ({ ...prev, isOnline: true }));
    };
    
    const handleOffline = () => {
      setOfflineStatus(prev => ({ ...prev, isOnline: false }));
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Load offline data from localStorage
  useEffect(() => {
    const savedOfflineMode = localStorage.getItem('hopngo-offline-mode');
    const savedItineraries = localStorage.getItem('hopngo-offline-itineraries');
    const savedStatus = localStorage.getItem('hopngo-offline-status');

    if (savedOfflineMode === 'true') {
      setIsOfflineMode(true);
    }
    if (savedItineraries) {
      const itineraries = JSON.parse(savedItineraries);
      setDownloadedItineraries(itineraries);
    }
    if (savedStatus) {
      const status = JSON.parse(savedStatus);
      setOfflineStatus(prev => ({ ...prev, ...status, isOnline: navigator.onLine }));
    }
  }, []);

  // Save to localStorage
  useEffect(() => {
    localStorage.setItem('hopngo-offline-mode', isOfflineMode.toString());
    localStorage.setItem('hopngo-offline-itineraries', JSON.stringify(downloadedItineraries));
    
    const usedStorage = downloadedItineraries.reduce((total, item) => total + item.size, 0);
    const updatedStatus = {
      ...offlineStatus,
      usedStorage,
      downloadedItineraries: downloadedItineraries.length
    };
    
    setOfflineStatus(updatedStatus);
    localStorage.setItem('hopngo-offline-status', JSON.stringify(updatedStatus));
  }, [isOfflineMode, downloadedItineraries]);

  // Toggle offline mode
  const toggleOfflineMode = useCallback(() => {
    setIsOfflineMode(prev => !prev);
  }, []);

  // Download itinerary for offline use
  const downloadItinerary = useCallback(async (itinerary: OfflineItinerary) => {
    // Simulate download process
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const updatedItinerary = {
      ...itinerary,
      downloadedAt: new Date()
    };
    
    setDownloadedItineraries(prev => {
      const exists = prev.find(item => item.id === itinerary.id);
      if (exists) {
        return prev.map(item => item.id === itinerary.id ? updatedItinerary : item);
      }
      return [...prev, updatedItinerary];
    });
  }, []);

  // Remove downloaded itinerary
  const removeItinerary = useCallback((id: string) => {
    setDownloadedItineraries(prev => prev.filter(item => item.id !== id));
  }, []);

  // Sync data when online
  const syncData = useCallback(async () => {
    if (!offlineStatus.isOnline) return;
    
    // Simulate sync process
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    setOfflineStatus(prev => ({
      ...prev,
      lastSync: new Date()
    }));
  }, [offlineStatus.isOnline]);

  return (
    <OfflineDemoContext.Provider value={{
      isOfflineMode,
      offlineStatus,
      downloadedItineraries,
      toggleOfflineMode,
      downloadItinerary,
      removeItinerary,
      syncData
    }}>
      {children}
    </OfflineDemoContext.Provider>
  );
};

// Hook to use offline demo context
export const useOfflineDemo = () => {
  const context = React.useContext(OfflineDemoContext);
  if (!context) {
    throw new Error('useOfflineDemo must be used within an OfflineDemoProvider');
  }
  return context;
};

// Offline demo dashboard
interface OfflineDemoDashboardProps {
  className?: string;
}

export const OfflineDemoDashboard: React.FC<OfflineDemoDashboardProps> = ({
  className = ''
}) => {
  const {
    isOfflineMode,
    offlineStatus,
    downloadedItineraries,
    toggleOfflineMode,
    downloadItinerary,
    removeItinerary,
    syncData
  } = useOfflineDemo();
  
  const [isDownloading, setIsDownloading] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [showAvailable, setShowAvailable] = useState(false);

  const handleDownload = async (itinerary: OfflineItinerary) => {
    setIsDownloading(itinerary.id);
    await downloadItinerary(itinerary);
    setIsDownloading(null);
  };

  const handleSync = async () => {
    setIsSyncing(true);
    await syncData();
    setIsSyncing(false);
  };

  const storagePercentage = (offlineStatus.usedStorage / offlineStatus.totalStorage) * 100;

  return (
    <div className={`bg-white rounded-xl border border-gray-200 p-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className={`p-2 rounded-lg ${
            isOfflineMode ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-600'
          }`}>
            <Smartphone className="w-6 h-6" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">Offline Demo Wallet</h3>
            <div className="flex items-center space-x-2 text-sm">
              {offlineStatus.isOnline ? (
                <><Wifi className="w-4 h-4 text-green-600" /><span className="text-green-600">Online</span></>
              ) : (
                <><WifiOff className="w-4 h-4 text-red-600" /><span className="text-red-600">Offline</span></>
              )}
            </div>
          </div>
        </div>
        
        <InteractiveButton
          variant={isOfflineMode ? 'secondary' : 'primary'}
          onClick={toggleOfflineMode}
        >
          {isOfflineMode ? 'Disable PWA' : 'Enable PWA'}
        </InteractiveButton>
      </div>

      {/* Storage Status */}
      {isOfflineMode && (
        <div className="mb-6 p-4 bg-gray-50 rounded-lg">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-700">Storage Usage</span>
            <span className="text-sm text-gray-500">
              {offlineStatus.usedStorage.toFixed(1)} / {offlineStatus.totalStorage} MB
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <motion.div
              className={`h-2 rounded-full ${
                storagePercentage > 80 ? 'bg-red-500' : storagePercentage > 60 ? 'bg-yellow-500' : 'bg-blue-500'
              }`}
              initial={{ width: 0 }}
              animate={{ width: `${storagePercentage}%` }}
              transition={{ duration: 0.5 }}
            />
          </div>
        </div>
      )}

      {/* Sync Status */}
      {isOfflineMode && offlineStatus.isOnline && (
        <div className="mb-6 flex items-center justify-between p-3 bg-green-50 rounded-lg">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 text-green-600" />
            <span className="text-sm text-green-700">
              {offlineStatus.lastSync 
                ? `Last synced: ${offlineStatus.lastSync.toLocaleTimeString()}`
                : 'Ready to sync'
              }
            </span>
          </div>
          <InteractiveButton
            variant="ghost"
            size="sm"
            onClick={handleSync}
            disabled={isSyncing}
            className="text-green-600 hover:text-green-700"
          >
            {isSyncing ? (
              <><RefreshCw className="w-4 h-4 animate-spin mr-1" />Syncing</>
            ) : (
              <><RefreshCw className="w-4 h-4 mr-1" />Sync Now</>
            )}
          </InteractiveButton>
        </div>
      )}

      {/* Downloaded Itineraries */}
      {isOfflineMode && (
        <div className="mb-6">
          <div className="flex items-center justify-between mb-4">
            <h4 className="font-medium text-gray-900">
              Downloaded Itineraries ({downloadedItineraries.length})
            </h4>
            <InteractiveButton
              variant="ghost"
              size="sm"
              onClick={() => setShowAvailable(true)}
            >
              <Download className="w-4 h-4 mr-1" />
              Download More
            </InteractiveButton>
          </div>
          
          {downloadedItineraries.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <Smartphone className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <p>No offline itineraries yet</p>
              <p className="text-sm">Download some for offline access!</p>
            </div>
          ) : (
            <div className="space-y-3">
              {downloadedItineraries.map(itinerary => (
                <motion.div
                  key={itinerary.id}
                  className="flex items-center space-x-4 p-4 border border-gray-200 rounded-lg"
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                >
                  <img
                    src={itinerary.thumbnail}
                    alt={itinerary.title}
                    className="w-16 h-16 object-cover rounded-lg"
                  />
                  <div className="flex-1">
                    <h5 className="font-medium text-gray-900">{itinerary.title}</h5>
                    <div className="flex items-center space-x-4 text-sm text-gray-500 mt-1">
                      <span className="flex items-center">
                        <Calendar className="w-3 h-3 mr-1" />
                        {itinerary.duration} days
                      </span>
                      <span className="flex items-center">
                        <DollarSign className="w-3 h-3 mr-1" />
                        ৳{itinerary.estimatedCost.toLocaleString()}
                      </span>
                      <span>{itinerary.size.toFixed(1)} MB</span>
                    </div>
                  </div>
                  <InteractiveButton
                    variant="ghost"
                    size="sm"
                    onClick={() => removeItinerary(itinerary.id)}
                    className="text-red-600 hover:text-red-700"
                  >
                    <Trash2 className="w-4 h-4" />
                  </InteractiveButton>
                </motion.div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Available Itineraries Modal */}
      <AccessibleModal
        isOpen={showAvailable}
        onClose={() => setShowAvailable(false)}
        title="Available for Download"
      >
        <div className="p-6">
          <div className="space-y-4">
            {sampleItineraries.map(itinerary => {
              const isDownloaded = downloadedItineraries.some(item => item.id === itinerary.id);
              const isCurrentlyDownloading = isDownloading === itinerary.id;
              
              return (
                <div key={itinerary.id} className="flex items-center space-x-4 p-4 border border-gray-200 rounded-lg">
                  <img
                    src={itinerary.thumbnail}
                    alt={itinerary.title}
                    className="w-16 h-16 object-cover rounded-lg"
                  />
                  <div className="flex-1">
                    <h5 className="font-medium text-gray-900">{itinerary.title}</h5>
                    <p className="text-sm text-gray-600 mt-1">{itinerary.description}</p>
                    <div className="flex items-center space-x-4 text-sm text-gray-500 mt-2">
                      <span className="flex items-center">
                        <Calendar className="w-3 h-3 mr-1" />
                        {itinerary.duration} days
                      </span>
                      <span className="flex items-center">
                        <DollarSign className="w-3 h-3 mr-1" />
                        ৳{itinerary.estimatedCost.toLocaleString()}
                      </span>
                      <span>{itinerary.size.toFixed(1)} MB</span>
                    </div>
                  </div>
                  <InteractiveButton
                    variant={isDownloaded ? 'secondary' : 'primary'}
                    size="sm"
                    onClick={() => !isDownloaded && handleDownload(itinerary)}
                    disabled={isDownloaded || isCurrentlyDownloading}
                  >
                    {isCurrentlyDownloading ? (
                      <><RefreshCw className="w-4 h-4 animate-spin mr-1" />Downloading</>
                    ) : isDownloaded ? (
                      <><CheckCircle className="w-4 h-4 mr-1" />Downloaded</>
                    ) : (
                      <><Download className="w-4 h-4 mr-1" />Download</>
                    )}
                  </InteractiveButton>
                </div>
              );
            })}
          </div>
        </div>
      </AccessibleModal>

      {/* PWA Installation Prompt */}
      {!isOfflineMode && (
        <div className="bg-blue-50 p-4 rounded-lg">
          <h4 className="font-medium text-blue-900 mb-2">Progressive Web App Features:</h4>
          <ul className="space-y-1 text-sm text-blue-800">
            <li>• Download itineraries for offline access</li>
            <li>• Work without internet connection</li>
            <li>• Install as native app on mobile</li>
            <li>• Automatic background sync when online</li>
            <li>• Reduced data usage</li>
          </ul>
        </div>
      )}
    </div>
  );
};

export default OfflineDemoDashboard;