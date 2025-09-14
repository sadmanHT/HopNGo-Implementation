'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, ArrowRight, ArrowLeft, MapPin, Search, Zap, MessageCircle, Phone, Calendar } from 'lucide-react';
import { AccessibleModal } from '../ui/focus-management';
import { InteractiveButton } from '../ui/micro-interactions';

// Tour step interface
interface TourStep {
  id: string;
  title: string;
  description: string;
  target: string; // CSS selector for the element to highlight
  icon: React.ReactNode;
  position: 'top' | 'bottom' | 'left' | 'right' | 'center';
  action?: () => void; // Optional action to perform when step is shown
}

// Predefined tour steps for HopNGo features
const tourSteps: TourStep[] = [
  {
    id: 'welcome',
    title: 'Welcome to HopNGo! 🇧🇩',
    description: 'Discover the beauty of Bangladesh with our AI-powered travel platform. Let\'s take a quick tour of our amazing features!',
    target: 'body',
    icon: <MapPin className="w-6 h-6" />,
    position: 'center'
  },
  {
    id: 'visual-search',
    title: 'Visual Search Magic ✨',
    description: 'Upload any travel photo and our AI will find similar stunning destinations in Bangladesh. Perfect for when you see something beautiful and want to visit!',
    target: '[data-tour="visual-search"]',
    icon: <Search className="w-6 h-6" />,
    position: 'bottom'
  },
  {
    id: 'heatmap',
    title: 'Smart Heatmap 🗺️',
    description: 'See real-time popularity and crowd levels at different destinations. Plan your visit when it\'s less crowded or join the buzz when it\'s happening!',
    target: '[data-tour="heatmap"]',
    icon: <MapPin className="w-6 h-6" />,
    position: 'left'
  },
  {
    id: 'ai-planner',
    title: 'AI Trip Planner 🤖',
    description: 'Tell us your preferences and our AI creates personalized itineraries. From budget backpacking to luxury experiences - we\'ve got you covered!',
    target: '[data-tour="ai-planner"]',
    icon: <Zap className="w-6 h-6" />,
    position: 'top'
  },
  {
    id: 'one-click-booking',
    title: 'One-Click Booking ⚡',
    description: 'Found something you love? Book instantly with our streamlined process. Secure payments, instant confirmations, and digital tickets!',
    target: '[data-tour="booking"]',
    icon: <Calendar className="w-6 h-6" />,
    position: 'right'
  },
  {
    id: 'chat-support',
    title: 'Live Chat Support 💬',
    description: 'Connect directly with local providers and our support team. Get real-time answers, negotiate prices, and get insider tips!',
    target: '[data-tour="chat"]',
    icon: <MessageCircle className="w-6 h-6" />,
    position: 'top'
  },
  {
    id: 'emergency-support',
    title: 'Emergency Support 🆘',
    description: 'Travel with confidence! Our 24/7 emergency support helps with weather alerts, route changes, and any travel emergencies.',
    target: '[data-tour="emergency"]',
    icon: <Phone className="w-6 h-6" />,
    position: 'bottom'
  },
  {
    id: 'demo-mode',
    title: 'Try Demo Mode! 🎮',
    description: 'Want to explore without creating an account? Toggle demo mode to experience HopNGo with sample data and see all features in action!',
    target: '[data-tour="demo-toggle"]',
    icon: <Zap className="w-6 h-6" />,
    position: 'left'
  }
];

// Guided Tour Component
interface GuidedTourProps {
  isOpen: boolean;
  onClose: () => void;
  onComplete?: () => void;
  autoStart?: boolean;
  showDemoToggle?: boolean;
}

export const GuidedTour: React.FC<GuidedTourProps> = ({
  isOpen,
  onClose,
  onComplete,
  autoStart = false,
  showDemoToggle = true
}) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [highlightedElement, setHighlightedElement] = useState<Element | null>(null);
  const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });
  const overlayRef = useRef<HTMLDivElement>(null);

  const currentTourStep = tourSteps[currentStep];
  const isLastStep = currentStep === tourSteps.length - 1;
  const isFirstStep = currentStep === 0;

  // Calculate tooltip position based on highlighted element
  const calculateTooltipPosition = (element: Element, position: string) => {
    const rect = element.getBoundingClientRect();
    const tooltipWidth = 320;
    const tooltipHeight = 200;
    const offset = 20;

    switch (position) {
      case 'top':
        return {
          x: rect.left + rect.width / 2 - tooltipWidth / 2,
          y: rect.top - tooltipHeight - offset
        };
      case 'bottom':
        return {
          x: rect.left + rect.width / 2 - tooltipWidth / 2,
          y: rect.bottom + offset
        };
      case 'left':
        return {
          x: rect.left - tooltipWidth - offset,
          y: rect.top + rect.height / 2 - tooltipHeight / 2
        };
      case 'right':
        return {
          x: rect.right + offset,
          y: rect.top + rect.height / 2 - tooltipHeight / 2
        };
      case 'center':
      default:
        return {
          x: window.innerWidth / 2 - tooltipWidth / 2,
          y: window.innerHeight / 2 - tooltipHeight / 2
        };
    }
  };

  // Update highlighted element and tooltip position
  useEffect(() => {
    if (!isOpen || !currentTourStep) return;

    const element = document.querySelector(currentTourStep.target);
    if (element) {
      setHighlightedElement(element);
      const position = calculateTooltipPosition(element, currentTourStep.position);
      setTooltipPosition(position);

      // Execute step action if provided
      if (currentTourStep.action) {
        currentTourStep.action();
      }

      // Scroll element into view
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [currentStep, isOpen, currentTourStep]);

  // Handle next step
  const handleNext = () => {
    if (isLastStep) {
      handleComplete();
    } else {
      setCurrentStep(prev => prev + 1);
    }
  };

  // Handle previous step
  const handlePrevious = () => {
    if (!isFirstStep) {
      setCurrentStep(prev => prev - 1);
    }
  };

  // Handle tour completion
  const handleComplete = () => {
    onComplete?.();
    onClose();
    // Store completion in localStorage
    localStorage.setItem('hopngo-tour-completed', 'true');
  };

  // Handle skip tour
  const handleSkip = () => {
    onClose();
    localStorage.setItem('hopngo-tour-skipped', 'true');
  };

  // Auto-start tour for new users
  useEffect(() => {
    if (autoStart && !localStorage.getItem('hopngo-tour-completed') && !localStorage.getItem('hopngo-tour-skipped')) {
      // Auto-start after a short delay
      const timer = setTimeout(() => {
        // Tour will be opened by parent component
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [autoStart]);

  if (!isOpen) return null;

  return (
    <>
      {/* Overlay with highlight cutout */}
      <motion.div
        ref={overlayRef}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 pointer-events-none"
        style={{
          background: highlightedElement && currentTourStep.position !== 'center'
            ? `radial-gradient(circle at ${highlightedElement.getBoundingClientRect().left + highlightedElement.getBoundingClientRect().width / 2}px ${highlightedElement.getBoundingClientRect().top + highlightedElement.getBoundingClientRect().height / 2}px, transparent 60px, rgba(0, 0, 0, 0.7) 80px)`
            : 'rgba(0, 0, 0, 0.7)'
        }}
      />

      {/* Highlighted element border */}
      {highlightedElement && currentTourStep.position !== 'center' && (
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="fixed z-50 pointer-events-none border-4 border-blue-500 rounded-lg shadow-lg"
          style={{
            left: highlightedElement.getBoundingClientRect().left - 4,
            top: highlightedElement.getBoundingClientRect().top - 4,
            width: highlightedElement.getBoundingClientRect().width + 8,
            height: highlightedElement.getBoundingClientRect().height + 8,
          }}
        />
      )}

      {/* Tour tooltip */}
      <motion.div
        initial={{ opacity: 0, scale: 0.8, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.8, y: 20 }}
        className="fixed z-50 bg-white rounded-xl shadow-2xl p-6 max-w-sm pointer-events-auto"
        style={{
          left: Math.max(16, Math.min(tooltipPosition.x, window.innerWidth - 336)),
          top: Math.max(16, Math.min(tooltipPosition.y, window.innerHeight - 216))
        }}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-100 rounded-lg text-blue-600">
              {currentTourStep.icon}
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">{currentTourStep.title}</h3>
              <p className="text-sm text-gray-500">
                Step {currentStep + 1} of {tourSteps.length}
              </p>
            </div>
          </div>
          <button
            onClick={handleSkip}
            className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
            aria-label="Skip tour"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <p className="text-gray-700 mb-6 leading-relaxed">
          {currentTourStep.description}
        </p>

        {/* Progress bar */}
        <div className="mb-6">
          <div className="flex justify-between text-xs text-gray-500 mb-2">
            <span>Progress</span>
            <span>{Math.round(((currentStep + 1) / tourSteps.length) * 100)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <motion.div
              className="bg-blue-600 h-2 rounded-full"
              initial={{ width: 0 }}
              animate={{ width: `${((currentStep + 1) / tourSteps.length) * 100}%` }}
              transition={{ duration: 0.3 }}
            />
          </div>
        </div>

        {/* Navigation */}
        <div className="flex justify-between items-center">
          <InteractiveButton
            variant="ghost"
            size="sm"
            onClick={handlePrevious}
            disabled={isFirstStep}
            className="flex items-center space-x-2"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Previous</span>
          </InteractiveButton>

          <div className="flex space-x-2">
            <InteractiveButton
              variant="ghost"
              size="sm"
              onClick={handleSkip}
            >
              Skip Tour
            </InteractiveButton>
            <InteractiveButton
              variant="primary"
              size="sm"
              onClick={handleNext}
              className="flex items-center space-x-2"
            >
              <span>{isLastStep ? 'Finish' : 'Next'}</span>
              {!isLastStep && <ArrowRight className="w-4 h-4" />}
            </InteractiveButton>
          </div>
        </div>
      </motion.div>
    </>
  );
};

// Tour trigger component
interface TourTriggerProps {
  onStart: () => void;
  className?: string;
  children?: React.ReactNode;
}

export const TourTrigger: React.FC<TourTriggerProps> = ({
  onStart,
  className = '',
  children
}) => {
  const hasCompletedTour = typeof window !== 'undefined' && localStorage.getItem('hopngo-tour-completed');
  const hasSkippedTour = typeof window !== 'undefined' && localStorage.getItem('hopngo-tour-skipped');

  if (hasCompletedTour || hasSkippedTour) {
    return null;
  }

  return (
    <InteractiveButton
      variant="primary"
      size="sm"
      onClick={onStart}
      className={`${className} animate-pulse`}
    >
      {children || (
        <>
          <Zap className="w-4 h-4 mr-2" />
          Take Tour
        </>
      )}
    </InteractiveButton>
  );
};

// Hook for managing tour state
export const useGuidedTour = () => {
  const [isTourOpen, setIsTourOpen] = useState(false);
  const [hasCompletedTour, setHasCompletedTour] = useState(false);

  useEffect(() => {
    const completed = localStorage.getItem('hopngo-tour-completed');
    const skipped = localStorage.getItem('hopngo-tour-skipped');
    setHasCompletedTour(!!completed || !!skipped);
  }, []);

  const startTour = () => setIsTourOpen(true);
  const closeTour = () => setIsTourOpen(false);
  const resetTour = () => {
    localStorage.removeItem('hopngo-tour-completed');
    localStorage.removeItem('hopngo-tour-skipped');
    setHasCompletedTour(false);
  };

  return {
    isTourOpen,
    hasCompletedTour,
    startTour,
    closeTour,
    resetTour
  };
};

export default GuidedTour;