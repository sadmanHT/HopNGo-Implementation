'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Play, 
  Settings, 
  Eye, 
  Zap, 
  Smartphone, 
  Users, 
  MapPin, 
  Calendar,
  MessageCircle,
  Phone,
  Search,
  BarChart3,
  Sparkles,
  ChevronRight,
  ExternalLink
} from 'lucide-react';
import { InteractiveButton } from '../ui/micro-interactions';
import { AccessibleModal } from '../ui/focus-management';
import { GuidedTour, useGuidedTour, TourTrigger } from '../onboarding/guided-tour';
import { DemoToggle, useDemoMode } from './demo-toggle';
import { PerformanceDashboard, usePerformanceMode, PerformanceIndicator } from '../performance/performance-mode';
import { OfflineDemoDashboard, useOfflineDemo } from '../offline/offline-demo';

// Demo feature interface
interface DemoFeature {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  status: 'active' | 'inactive' | 'loading';
  component?: React.ComponentType;
}

// Demo showcase component
interface DemoShowcaseProps {
  className?: string;
  variant?: 'full' | 'compact' | 'floating';
}

export const DemoShowcase: React.FC<DemoShowcaseProps> = ({
  className = '',
  variant = 'full'
}) => {
  const [activeTab, setActiveTab] = useState('overview');
  const [showSettings, setShowSettings] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  
  // Demo hooks
  const { isTourOpen, startTour, closeTour, hasCompletedTour } = useGuidedTour();
  const { isDemoMode, demoData } = useDemoMode();
  const { isPerformanceMode, metrics } = usePerformanceMode();
  const { isOfflineMode, offlineStatus, downloadedItineraries } = useOfflineDemo();

  // Demo features configuration
  const demoFeatures: DemoFeature[] = [
    {
      id: 'guided-tour',
      title: 'Guided Tour',
      description: 'Interactive walkthrough of key features',
      icon: <Eye className="w-5 h-5" />,
      status: hasCompletedTour ? 'active' : 'inactive'
    },
    {
      id: 'demo-data',
      title: 'Sample Data',
      description: 'Curated Bangladesh tourism content',
      icon: <Users className="w-5 h-5" />,
      status: isDemoMode ? 'active' : 'inactive'
    },
    {
      id: 'performance',
      title: 'Performance Mode',
      description: 'Optimized loading and caching',
      icon: <Zap className="w-5 h-5" />,
      status: isPerformanceMode ? 'active' : 'inactive'
    },
    {
      id: 'offline-pwa',
      title: 'Offline PWA',
      description: 'Progressive web app capabilities',
      icon: <Smartphone className="w-5 h-5" />,
      status: isOfflineMode ? 'active' : 'inactive'
    }
  ];

  // Key features showcase
  const keyFeatures = [
    {
      id: 'visual-search',
      title: 'Visual Search',
      description: 'AI-powered image recognition for destinations',
      icon: <Search className="w-6 h-6" />,
      color: 'bg-blue-500'
    },
    {
      id: 'heatmap',
      title: 'Smart Heatmap',
      description: 'Real-time crowd and popularity data',
      icon: <BarChart3 className="w-6 h-6" />,
      color: 'bg-green-500'
    },
    {
      id: 'ai-planner',
      title: 'AI Trip Planner',
      description: 'Personalized itinerary generation',
      icon: <Sparkles className="w-6 h-6" />,
      color: 'bg-purple-500'
    },
    {
      id: 'booking',
      title: 'One-Click Booking',
      description: 'Streamlined reservation process',
      icon: <Calendar className="w-6 h-6" />,
      color: 'bg-orange-500'
    },
    {
      id: 'chat',
      title: 'Live Chat',
      description: 'Direct provider communication',
      icon: <MessageCircle className="w-6 h-6" />,
      color: 'bg-pink-500'
    },
    {
      id: 'emergency',
      title: 'Emergency Support',
      description: '24/7 travel assistance',
      icon: <Phone className="w-6 h-6" />,
      color: 'bg-red-500'
    }
  ];

  // Floating variant
  if (variant === 'floating') {
    return (
      <>
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className={`fixed bottom-6 left-6 z-40 ${className}`}
        >
          <div className="bg-white rounded-full shadow-lg border border-gray-200 p-2">
            <InteractiveButton
              variant="primary"
              size="sm"
              onClick={() => setIsExpanded(!isExpanded)}
              className="rounded-full w-12 h-12 flex items-center justify-center"
              aria-label="Demo controls"
            >
              <Play className="w-5 h-5" />
            </InteractiveButton>
          </div>
        </motion.div>

        <AnimatePresence>
          {isExpanded && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.8, y: 20 }}
              className="fixed bottom-20 left-6 z-40 bg-white rounded-xl shadow-2xl border border-gray-200 p-4 w-80"
            >
              <h3 className="font-semibold text-gray-900 mb-3">Demo Controls</h3>
              <div className="space-y-3">
                <TourTrigger onStart={startTour} className="w-full" />
                <DemoToggle variant="compact" showLabel={true} />
                <div className="flex space-x-2">
                  <InteractiveButton
                    variant="ghost"
                    size="sm"
                    onClick={() => setShowSettings(true)}
                    className="flex-1"
                  >
                    <Settings className="w-4 h-4 mr-1" />
                    Settings
                  </InteractiveButton>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </>
    );
  }

  // Compact variant
  if (variant === 'compact') {
    return (
      <div className={`bg-white rounded-lg border border-gray-200 p-4 ${className}`}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-gray-900">Demo Mode</h3>
          <InteractiveButton
            variant="ghost"
            size="sm"
            onClick={() => setShowSettings(true)}
          >
            <Settings className="w-4 h-4" />
          </InteractiveButton>
        </div>
        
        <div className="grid grid-cols-2 gap-3">
          {demoFeatures.map(feature => (
            <div key={feature.id} className="flex items-center space-x-2 p-2 rounded-lg bg-gray-50">
              <div className={`p-1 rounded ${
                feature.status === 'active' ? 'text-green-600' : 'text-gray-400'
              }`}>
                {feature.icon}
              </div>
              <span className="text-sm font-medium text-gray-700">{feature.title}</span>
            </div>
          ))}
        </div>
        
        <div className="mt-4 pt-4 border-t border-gray-200">
          <TourTrigger onStart={startTour} className="w-full" />
        </div>
      </div>
    );
  }

  // Full variant (default)
  return (
    <>
      <div className={`bg-white rounded-xl border border-gray-200 ${className}`}>
        {/* Header */}
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-3 bg-gradient-to-r from-blue-500 to-purple-600 rounded-xl text-white">
                <Play className="w-6 h-6" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900">HopNGo Demo Showcase</h2>
                <p className="text-gray-600">Experience Bangladesh tourism like never before</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-3">
              <TourTrigger onStart={startTour} />
              <InteractiveButton
                variant="ghost"
                onClick={() => setShowSettings(true)}
              >
                <Settings className="w-5 h-5" />
              </InteractiveButton>
            </div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-6">
            {[
              { id: 'overview', label: 'Overview' },
              { id: 'features', label: 'Features' },
              { id: 'performance', label: 'Performance' },
              { id: 'offline', label: 'Offline' }
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <div className="p-6">
          <AnimatePresence mode="wait">
            {activeTab === 'overview' && (
              <motion.div
                key="overview"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                {/* Demo Status */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  {demoFeatures.map(feature => (
                    <div key={feature.id} className="p-4 border border-gray-200 rounded-lg">
                      <div className="flex items-center space-x-3 mb-2">
                        <div className={`p-2 rounded-lg ${
                          feature.status === 'active' 
                            ? 'bg-green-100 text-green-600' 
                            : 'bg-gray-100 text-gray-400'
                        }`}>
                          {feature.icon}
                        </div>
                        <div className={`w-2 h-2 rounded-full ${
                          feature.status === 'active' ? 'bg-green-500' : 'bg-gray-300'
                        }`} />
                      </div>
                      <h4 className="font-medium text-gray-900">{feature.title}</h4>
                      <p className="text-sm text-gray-600 mt-1">{feature.description}</p>
                    </div>
                  ))}
                </div>

                {/* Demo Data Stats */}
                {isDemoMode && (
                  <div className="bg-blue-50 p-6 rounded-xl">
                    <h3 className="font-semibold text-blue-900 mb-4">Demo Data Overview</h3>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                      <div className="text-center">
                        <div className="text-2xl font-bold text-blue-600">{demoData.users}</div>
                        <div className="text-sm text-blue-700">Demo Users</div>
                      </div>
                      <div className="text-center">
                        <div className="text-2xl font-bold text-blue-600">{demoData.listings}</div>
                        <div className="text-sm text-blue-700">Listings</div>
                      </div>
                      <div className="text-center">
                        <div className="text-2xl font-bold text-blue-600">{demoData.itineraries}</div>
                        <div className="text-sm text-blue-700">Itineraries</div>
                      </div>
                      <div className="text-center">
                        <div className="text-2xl font-bold text-blue-600">{demoData.conversations}</div>
                        <div className="text-sm text-blue-700">Conversations</div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Quick Actions */}
                <div className="bg-gray-50 p-6 rounded-xl">
                  <h3 className="font-semibold text-gray-900 mb-4">Quick Actions</h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <InteractiveButton
                      variant="primary"
                      onClick={startTour}
                      className="flex items-center justify-center space-x-2"
                    >
                      <Eye className="w-4 h-4" />
                      <span>Start Guided Tour</span>
                    </InteractiveButton>
                    
                    <DemoToggle variant="full" showStats={false} />
                    
                    <InteractiveButton
                      variant="ghost"
                      onClick={() => setShowSettings(true)}
                      className="flex items-center justify-center space-x-2"
                    >
                      <Settings className="w-4 h-4" />
                      <span>Demo Settings</span>
                    </InteractiveButton>
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === 'features' && (
              <motion.div
                key="features"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
              >
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {keyFeatures.map(feature => (
                    <motion.div
                      key={feature.id}
                      className="p-6 border border-gray-200 rounded-xl hover:shadow-lg transition-shadow cursor-pointer"
                      whileHover={{ scale: 1.02 }}
                      data-tour={feature.id}
                    >
                      <div className={`w-12 h-12 ${feature.color} rounded-xl flex items-center justify-center text-white mb-4`}>
                        {feature.icon}
                      </div>
                      <h3 className="font-semibold text-gray-900 mb-2">{feature.title}</h3>
                      <p className="text-gray-600 text-sm mb-4">{feature.description}</p>
                      <div className="flex items-center text-blue-600 text-sm font-medium">
                        <span>Try it now</span>
                        <ChevronRight className="w-4 h-4 ml-1" />
                      </div>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}

            {activeTab === 'performance' && (
              <motion.div
                key="performance"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
              >
                <PerformanceDashboard />
              </motion.div>
            )}

            {activeTab === 'offline' && (
              <motion.div
                key="offline"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
              >
                <OfflineDemoDashboard />
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Guided Tour */}
      <GuidedTour
        isOpen={isTourOpen}
        onClose={closeTour}
        autoStart={false}
        showDemoToggle={true}
      />

      {/* Performance Indicator */}
      <PerformanceIndicator />

      {/* Demo Settings Modal */}
      <AccessibleModal
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        title="Demo Settings"
      >
        <div className="p-6 space-y-6">
          <div>
            <h3 className="font-medium text-gray-900 mb-3">Demo Mode</h3>
            <DemoToggle variant="full" showStats={true} />
          </div>
          
          <div>
            <h3 className="font-medium text-gray-900 mb-3">Performance</h3>
            <PerformanceDashboard className="border-0 p-0" />
          </div>
          
          <div>
            <h3 className="font-medium text-gray-900 mb-3">Offline Mode</h3>
            <OfflineDemoDashboard className="border-0 p-0" />
          </div>
          
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <InteractiveButton
              variant="ghost"
              onClick={() => setShowSettings(false)}
            >
              Close
            </InteractiveButton>
            <InteractiveButton
              variant="primary"
              onClick={() => {
                setShowSettings(false);
                startTour();
              }}
            >
              <Eye className="w-4 h-4 mr-2" />
              Start Tour
            </InteractiveButton>
          </div>
        </div>
      </AccessibleModal>
    </>
  );
};

export default DemoShowcase;