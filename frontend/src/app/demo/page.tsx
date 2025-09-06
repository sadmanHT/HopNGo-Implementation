"use client"

import React from 'react'
import { PostCard } from '@/components/PostCard'
import { ReviewCard } from '@/components/ReviewCard'
import { ListingCard } from '@/components/ListingCard'
import { ToastProvider } from '@/components/ui/toast'

// Mock data for demonstration
const mockPost = {
  id: '1',
  content: 'Just had an amazing experience at the local farmers market! The fresh produce and friendly vendors made my morning perfect. Highly recommend checking it out if you\'re in the area. 🌱',
  author: {
    id: 'user1',
    name: 'Sarah Johnson',
    avatar: undefined,
  },
  createdAt: '2024-01-15T10:30:00Z',
  likes: 24,
  comments: 8,
  isLiked: false,
}

const mockReview = {
  id: '2',
  content: 'Excellent product quality and fast shipping! The item arrived exactly as described and the packaging was very secure. Would definitely recommend this seller to others.',
  rating: 5,
  author: {
    id: 'user2',
    name: 'Mike Chen',
    avatar: undefined,
  },
  createdAt: '2024-01-14T15:45:00Z',
  productId: 'prod1',
  productName: 'Wireless Bluetooth Headphones',
}

const mockListing = {
  id: '3',
  title: 'Vintage Leather Backpack - Excellent Condition',
  description: 'Beautiful vintage leather backpack in excellent condition. Perfect for daily use or travel. Has multiple compartments and adjustable straps. Only selling because I got a new one as a gift.',
  price: 85.00,
  currency: 'USD',
  category: 'Fashion & Accessories',
  location: {
    city: 'San Francisco',
    country: 'USA',
  },
  images: ['https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=400'],
  author: {
    id: 'user3',
    name: 'Emma Wilson',
    avatar: undefined,
  },
  createdAt: '2024-01-13T09:20:00Z',
  status: 'ACTIVE' as const,
  tags: ['vintage', 'leather', 'backpack', 'travel'],
}

function DemoPageContent() {
  const handlePostLike = (postId: string) => {
    console.log('Liked post:', postId)
  }

  const handlePostComment = (postId: string) => {
    console.log('Comment on post:', postId)
  }

  const handlePostShare = (postId: string) => {
    console.log('Share post:', postId)
  }

  const handleReviewHelpful = (reviewId: string) => {
    console.log('Review helpful:', reviewId)
  }

  const handleListingContact = (listingId: string) => {
    console.log('Contact seller for listing:', listingId)
  }

  const handleListingFavorite = (listingId: string) => {
    console.log('Favorite listing:', listingId)
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4 space-y-8">
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold text-gray-900">
            Content Reporting Demo
          </h1>
          <p className="text-gray-600">
            Click the three-dot menu on any content card to report inappropriate content
          </p>
        </div>

        <div className="grid gap-8 md:grid-cols-1 lg:grid-cols-2">
          {/* Social Post */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-800">Social Post</h2>
            <PostCard
              post={mockPost}
              onLike={handlePostLike}
              onComment={handlePostComment}
              onShare={handlePostShare}
            />
          </div>

          {/* Product Review */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-800">Product Review</h2>
            <ReviewCard
              review={mockReview}
              onHelpful={handleReviewHelpful}
            />
          </div>

          {/* Marketplace Listing */}
          <div className="space-y-4 md:col-span-1 lg:col-span-2">
            <h2 className="text-xl font-semibold text-gray-800">Marketplace Listing</h2>
            <div className="max-w-md mx-auto">
              <ListingCard
                listing={mockListing}
                onContact={handleListingContact}
                onFavorite={handleListingFavorite}
              />
            </div>
          </div>
        </div>

        <div className="text-center text-sm text-gray-500 space-y-2">
          <p>
            This demo showcases the report functionality for different content types.
          </p>
          <p>
            Reports are sent to the respective backend services for moderation review.
          </p>
        </div>
      </div>


    </div>
  )
}

export default function DemoPage() {
  return (
    <ToastProvider>
      <DemoPageContent />
    </ToastProvider>
  )
}