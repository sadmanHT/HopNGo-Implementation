"use client"

import React from 'react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { LazyCard, LazyAvatar } from '@/components/ui/lazy-image'
import { MoreHorizontal, Flag, MapPin, Clock } from 'lucide-react'
import { ReportModal } from '@/components/modals/ReportModal'
import { useReportModal } from '@/hooks/useReportModal'
import { useToast } from '@/components/ui/toast'
import { VerifiedBadge } from '@/components/ui/verified-badge'

interface ListingCardProps {
  listing: {
    id: string
    title: string
    description: string
    price: number
    currency: string
    category: string
    location?: {
      city: string
      country: string
    }
    images?: string[]
    author: {
      id: string
      name: string
      avatar?: string
      isVerified?: boolean
    }
    createdAt: string
    status: 'ACTIVE' | 'SOLD' | 'INACTIVE'
    tags?: string[]
  }
  onContact?: (listingId: string) => void
  onFavorite?: (listingId: string) => void
}

export function ListingCard({ listing, onContact, onFavorite }: ListingCardProps) {
  const { isOpen, contentType, contentId, openReportModal, closeReportModal } = useReportModal()
  const { addToast } = useToast()

  const handleReport = () => {
    openReportModal('listing', listing.id)
  }

  const handleReportSubmit = (reason: string, details?: string) => {
    addToast('Listing reported successfully', 'success')
    // Additional handling can be added here
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    })
  }

  const formatPrice = (price: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(price)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800'
      case 'SOLD':
        return 'bg-gray-100 text-gray-800'
      case 'INACTIVE':
        return 'bg-yellow-100 text-yellow-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  return (
    <>
      <Card className="overflow-hidden hover:shadow-lg transition-shadow">
        {/* Listing Image */}
        {listing.images && listing.images.length > 0 && (
          <div className="relative">
            <LazyCard
              src={listing.images[0]}
              alt={listing.title}
              sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
              className="w-full"
            />
            <Badge className={`absolute top-2 right-2 ${getStatusColor(listing.status)}`}>
              {listing.status}
            </Badge>
          </div>
        )}

        <div className="p-4 space-y-4">
          {/* Listing Header */}
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <h3 className="font-semibold text-lg line-clamp-2">{listing.title}</h3>
              <p className="text-2xl font-bold text-primary mt-1">
                {formatPrice(listing.price, listing.currency)}
              </p>
            </div>

            {/* Listing Menu */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={handleReport} className="text-red-600">
                  <Flag className="h-4 w-4 mr-2" />
                  Report Listing
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {/* Category and Location */}
          <div className="flex items-center justify-between text-sm text-gray-600">
            <Badge variant="secondary">{listing.category}</Badge>
            {listing.location && (
              <div className="flex items-center space-x-1">
                <MapPin className="h-3 w-3" />
                <span>{listing.location.city}, {listing.location.country}</span>
              </div>
            )}
          </div>

          {/* Description */}
          <p className="text-sm text-gray-600 line-clamp-2">{listing.description}</p>

          {/* Tags */}
          {listing.tags && listing.tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {listing.tags.slice(0, 3).map((tag, index) => (
                <Badge key={index} variant="outline" className="text-xs">
                  {tag}
                </Badge>
              ))}
              {listing.tags.length > 3 && (
                <Badge variant="outline" className="text-xs">
                  +{listing.tags.length - 3} more
                </Badge>
              )}
            </div>
          )}

          {/* Author and Date */}
          <div className="flex items-center justify-between pt-2 border-t">
            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center">
                {listing.author.avatar ? (
                  <LazyAvatar
                    src={listing.author.avatar}
                    alt={listing.author.name}
                    size={24}
                    className="w-6 h-6"
                  />
                ) : (
                  <span className="text-xs font-medium text-gray-600">
                    {listing.author.name.charAt(0).toUpperCase()}
                  </span>
                )}
              </div>
              <div className="flex items-center space-x-1">
                <span className="text-xs text-gray-500">{listing.author.name}</span>
                <VerifiedBadge isVerified={listing.author.isVerified} size="sm" variant="minimal" />
              </div>
            </div>

            <div className="flex items-center space-x-1 text-xs text-gray-500">
              <Clock className="h-3 w-3" />
              <span>{formatDate(listing.createdAt)}</span>
            </div>
          </div>

          {/* Actions */}
          {listing.status === 'ACTIVE' && (
            <div className="flex space-x-2">
              <Button
                onClick={() => onContact?.(listing.id)}
                className="flex-1"
                size="sm"
              >
                Contact Seller
              </Button>
              <Button
                onClick={() => onFavorite?.(listing.id)}
                variant="outline"
                size="sm"
              >
                Save
              </Button>
            </div>
          )}
        </div>
      </Card>

      {/* Report Modal */}
      <ReportModal
        isOpen={isOpen}
        onClose={closeReportModal}
        contentType={contentType || 'listing'}
        contentId={contentId || ''}
        onReport={handleReportSubmit}
      />
    </>
  )
}