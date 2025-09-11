"use client"

import React from 'react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { MoreHorizontal, Flag, Star } from 'lucide-react'
import { ReportModal } from '@/components/modals/ReportModal'
import { useReportModal } from '@/hooks/useReportModal'
import { useToast } from '@/components/ui/toast'
import { VerifiedBadge } from '@/components/ui/verified-badge'

interface ReviewCardProps {
  review: {
    id: string
    content: string
    rating: number
    author: {
      id: string
      name: string
      avatar?: string
      isVerified?: boolean
    }
    createdAt: string
    productId?: string
    productName?: string
    vendorResponse?: {
      id: string
      content: string
      createdAt: string
    }
  }
  onHelpful?: (reviewId: string) => void
}

export function ReviewCard({ review, onHelpful }: ReviewCardProps) {
  const { isOpen, contentType, contentId, openReportModal, closeReportModal } = useReportModal()
  const { addToast } = useToast()

  const handleReport = () => {
    openReportModal('review', review.id)
  }

  const handleReportSubmit = (reason: string, details?: string) => {
    addToast('Review reported successfully', 'success')
    // Additional handling can be added here
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  const renderStars = (rating: number) => {
    return Array.from({ length: 5 }, (_, index) => (
      <Star
        key={index}
        className={`h-4 w-4 ${
          index < rating
            ? 'fill-yellow-400 text-yellow-400'
            : 'text-gray-300'
        }`}
      />
    ))
  }

  return (
    <>
      <Card className="p-4 space-y-4">
        {/* Review Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center">
              {review.author.avatar ? (
                <img
                  src={review.author.avatar}
                  alt={review.author.name}
                  className="w-10 h-10 rounded-full object-cover"
                />
              ) : (
                <span className="text-sm font-medium text-gray-600">
                  {review.author.name.charAt(0).toUpperCase()}
                </span>
              )}
            </div>
            <div>
              <div className="flex items-center space-x-1">
                <h3 className="font-semibold text-sm">{review.author.name}</h3>
                <VerifiedBadge isVerified={review.author.isVerified} size="sm" variant="minimal" />
              </div>
              <div className="flex items-center space-x-2">
                <div className="flex items-center space-x-1">
                  {renderStars(review.rating)}
                </div>
                <span className="text-xs text-gray-500">{formatDate(review.createdAt)}</span>
              </div>
            </div>
          </div>

          {/* Review Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleReport} className="text-red-600">
                <Flag className="h-4 w-4 mr-2" />
                Report Review
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Product Info (if available) */}
        {review.productName && (
          <div className="text-sm text-gray-600">
            Review for: <span className="font-medium">{review.productName}</span>
          </div>
        )}

        {/* Review Content */}
        <div className="space-y-3">
          <p className="text-sm leading-relaxed">{review.content}</p>
        </div>

        {/* Review Actions */}
        <div className="flex items-center justify-between pt-2 border-t">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onHelpful?.(review.id)}
            className="text-gray-500 hover:text-gray-700"
          >
            <span className="text-xs">Was this helpful?</span>
          </Button>
        </div>
      </Card>

      {/* Report Modal */}
      <ReportModal
        isOpen={isOpen}
        onClose={closeReportModal}
        contentType={contentType || 'review'}
        contentId={contentId || ''}
        onReport={handleReportSubmit}
      />
    </>
  )
}