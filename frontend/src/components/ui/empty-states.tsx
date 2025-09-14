'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import { InteractiveButton } from './micro-interactions';

// Base empty state component
interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
    variant?: 'primary' | 'secondary';
  };
  secondaryAction?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
  illustration?: React.ReactNode;
}

export function EmptyState({
  icon,
  title,
  description,
  action,
  secondaryAction,
  className,
  illustration,
}: EmptyStateProps) {
  return (
    <motion.div
      className={cn(
        'flex flex-col items-center justify-center text-center p-8 space-y-6',
        className
      )}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
    >
      {illustration || (
        <motion.div
          className="text-gray-400 dark:text-gray-600"
          initial={{ scale: 0.8 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, duration: 0.3 }}
        >
          {icon || (
            <svg
              className="w-16 h-16 mx-auto"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
          )}
        </motion.div>
      )}
      
      <div className="space-y-2 max-w-md">
        <motion.h3
          className="text-lg font-semibold text-gray-900 dark:text-gray-100"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3, duration: 0.3 }}
        >
          {title}
        </motion.h3>
        <motion.p
          className="text-gray-600 dark:text-gray-400"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4, duration: 0.3 }}
        >
          {description}
        </motion.p>
      </div>
      
      {(action || secondaryAction) && (
        <motion.div
          className="flex flex-col sm:flex-row gap-3"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.3 }}
        >
          {action && (
            <InteractiveButton
              variant={action.variant || 'primary'}
              onClick={action.onClick}
            >
              {action.label}
            </InteractiveButton>
          )}
          {secondaryAction && (
            <InteractiveButton
              variant="ghost"
              onClick={secondaryAction.onClick}
            >
              {secondaryAction.label}
            </InteractiveButton>
          )}
        </motion.div>
      )}
    </motion.div>
  );
}

// Travel-specific empty states
export function NoTripsEmptyState({ onCreateTrip }: { onCreateTrip: () => void }) {
  const TravelIllustration = () => (
    <motion.svg
      className="w-24 h-24 text-blue-400"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
      initial={{ pathLength: 0 }}
      animate={{ pathLength: 1 }}
      transition={{ duration: 2, ease: 'easeInOut' }}
    >
      <motion.path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.5}
        d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
        initial={{ pathLength: 0 }}
        animate={{ pathLength: 1 }}
        transition={{ duration: 1.5, ease: 'easeInOut' }}
      />
    </motion.svg>
  );

  return (
    <EmptyState
      illustration={<TravelIllustration />}
      title="Ready for your next adventure?"
      description="Start planning your dream trip! Create your first itinerary and discover amazing destinations."
      action={{
        label: "Plan Your First Trip",
        onClick: onCreateTrip,
        variant: "primary"
      }}
      secondaryAction={{
        label: "Browse Destinations",
        onClick: () => window.location.href = '/destinations'
      }}
    />
  );
}

export function NoPhotosEmptyState({ onUploadPhoto }: { onUploadPhoto: () => void }) {
  const CameraIllustration = () => (
    <motion.div
      className="relative"
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <motion.svg
        className="w-20 h-20 text-gray-400"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
        />
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
        />
      </motion.svg>
      <motion.div
        className="absolute -top-1 -right-1 w-3 h-3 bg-blue-500 rounded-full"
        animate={{ scale: [1, 1.2, 1] }}
        transition={{ duration: 2, repeat: Infinity }}
      />
    </motion.div>
  );

  return (
    <EmptyState
      illustration={<CameraIllustration />}
      title="Capture your travel memories"
      description="Upload your first travel photo and start building your visual journey collection."
      action={{
        label: "Upload Travel Photo",
        onClick: onUploadPhoto,
        variant: "primary"
      }}
      secondaryAction={{
        label: "Browse Gallery",
        onClick: () => window.location.href = '/gallery'
      }}
    />
  );
}

export function NoBookingsEmptyState({ onBrowseServices }: { onBrowseServices: () => void }) {
  return (
    <EmptyState
      icon={
        <motion.svg
          className="w-16 h-16"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          initial={{ rotate: -10 }}
          animate={{ rotate: 0 }}
          transition={{ duration: 0.5, type: 'spring' }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M8 7V3a2 2 0 012-2h4a2 2 0 012 2v4m-6 0V6a2 2 0 012-2h4a2 2 0 012 2v1m-6 0h8m-8 0H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V9a2 2 0 00-2-2h-2"
          />
        </motion.svg>
      }
      title="No bookings yet"
      description="Discover amazing travel services and experiences. Book your first adventure today!"
      action={{
        label: "Browse Services",
        onClick: onBrowseServices,
        variant: "primary"
      }}
      secondaryAction={{
        label: "View Popular Destinations",
        onClick: () => window.location.href = '/popular'
      }}
    />
  );
}

export function NoSearchResultsEmptyState({ 
  query, 
  onClearSearch, 
  onBrowseAll 
}: { 
  query: string; 
  onClearSearch: () => void;
  onBrowseAll: () => void;
}) {
  return (
    <EmptyState
      icon={
        <motion.svg
          className="w-16 h-16"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          animate={{ rotate: [0, 10, -10, 0] }}
          transition={{ duration: 2, repeat: Infinity, repeatDelay: 3 }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </motion.svg>
      }
      title={`No results for "${query}"`}
      description="Try adjusting your search terms or browse our popular destinations and services."
      action={{
        label: "Clear Search",
        onClick: onClearSearch,
        variant: "primary"
      }}
      secondaryAction={{
        label: "Browse All",
        onClick: onBrowseAll
      }}
    />
  );
}

export function NoMessagesEmptyState({ onStartConversation }: { onStartConversation: () => void }) {
  return (
    <EmptyState
      icon={
        <motion.svg
          className="w-16 h-16"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          initial={{ scale: 0.8 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.5, type: 'spring' }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
          />
        </motion.svg>
      }
      title="No messages yet"
      description="Start a conversation with travel providers or fellow travelers to get recommendations and tips."
      action={{
        label: "Start Conversation",
        onClick: onStartConversation,
        variant: "primary"
      }}
    />
  );
}

export function NoFavoritesEmptyState({ onBrowse }: { onBrowse: () => void }) {
  return (
    <EmptyState
      icon={
        <motion.svg
          className="w-16 h-16"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          animate={{ 
            scale: [1, 1.1, 1],
            rotate: [0, -5, 5, 0]
          }}
          transition={{ 
            duration: 2, 
            repeat: Infinity, 
            repeatDelay: 3,
            ease: 'easeInOut'
          }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
          />
        </motion.svg>
      }
      title="No favorites yet"
      description="Discover amazing destinations and services. Save your favorites for quick access later!"
      action={{
        label: "Explore Destinations",
        onClick: onBrowse,
        variant: "primary"
      }}
    />
  );
}

// Offline state component
export function OfflineEmptyState({ onRetry }: { onRetry: () => void }) {
  return (
    <EmptyState
      icon={
        <motion.svg
          className="w-16 h-16 text-orange-500"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 2, repeat: Infinity }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192L5.636 18.364M12 2.25a9.75 9.75 0 100 19.5 9.75 9.75 0 000-19.5z"
          />
        </motion.svg>
      }
      title="You're offline"
      description="Check your internet connection and try again. Some features may be limited while offline."
      action={{
        label: "Try Again",
        onClick: onRetry,
        variant: "primary"
      }}
      secondaryAction={{
        label: "View Cached Content",
        onClick: () => window.location.href = '/offline'
      }}
    />
  );
}

// Error state component
export function ErrorEmptyState({ 
  title = "Something went wrong",
  description = "We encountered an error while loading this content. Please try again.",
  onRetry 
}: { 
  title?: string;
  description?: string;
  onRetry: () => void;
}) {
  return (
    <EmptyState
      icon={
        <motion.svg
          className="w-16 h-16 text-red-500"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          initial={{ scale: 0.8, rotate: -10 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 0.5, type: 'spring' }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          />
        </motion.svg>
      }
      title={title}
      description={description}
      action={{
        label: "Try Again",
        onClick: onRetry,
        variant: "primary"
      }}
      secondaryAction={{
        label: "Go Home",
        onClick: () => window.location.href = '/'
      }}
    />
  );
}

// Loading state with skeleton
export function LoadingEmptyState({ message = "Loading..." }: { message?: string }) {
  return (
    <motion.div
      className="flex flex-col items-center justify-center text-center p-8 space-y-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      <motion.div
        className="w-12 h-12 border-4 border-blue-200 border-t-blue-600 rounded-full"
        animate={{ rotate: 360 }}
        transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
      />
      <motion.p
        className="text-gray-600 dark:text-gray-400"
        animate={{ opacity: [0.5, 1, 0.5] }}
        transition={{ duration: 1.5, repeat: Infinity }}
      >
        {message}
      </motion.p>
    </motion.div>
  );
}

export default {
  EmptyState,
  NoTripsEmptyState,
  NoPhotosEmptyState,
  NoBookingsEmptyState,
  NoSearchResultsEmptyState,
  NoMessagesEmptyState,
  NoFavoritesEmptyState,
  OfflineEmptyState,
  ErrorEmptyState,
  LoadingEmptyState,
};