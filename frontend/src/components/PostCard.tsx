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
import { MoreHorizontal, Flag, Heart, MessageCircle, Share } from 'lucide-react'
import { ReportModal } from '@/components/modals/ReportModal'
import { useReportModal } from '@/hooks/useReportModal'
import { useToast } from '@/components/ui/toast'
import { VerifiedBadge } from '@/components/ui/verified-badge'

interface PostCardProps {
  post: {
    id: string
    content: string
    author: {
      id: string
      name: string
      avatar?: string
      isVerified?: boolean
    }
    createdAt: string
    likes: number
    comments: number
    isLiked?: boolean
  }
  onLike?: (postId: string) => void
  onComment?: (postId: string) => void
  onShare?: (postId: string) => void
}

export function PostCard({ post, onLike, onComment, onShare }: PostCardProps) {
  const { isOpen, contentType, contentId, openReportModal, closeReportModal } = useReportModal()
  const { addToast } = useToast()

  const handleReport = () => {
    openReportModal('post', post.id)
  }

  const handleReportSubmit = (reason: string, details?: string) => {
    addToast('Post reported successfully', 'success')
    // Additional handling can be added here
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <>
      <Card className="p-4 space-y-4">
        {/* Post Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center">
              {post.author.avatar ? (
                <img
                  src={post.author.avatar}
                  alt={post.author.name}
                  className="w-10 h-10 rounded-full object-cover"
                />
              ) : (
                <span className="text-sm font-medium text-gray-600">
                  {post.author.name.charAt(0).toUpperCase()}
                </span>
              )}
            </div>
            <div>
              <div className="flex items-center space-x-1">
                <h3 className="font-semibold text-sm">{post.author.name}</h3>
                <VerifiedBadge isVerified={post.author.isVerified} size="sm" variant="minimal" />
              </div>
              <p className="text-xs text-gray-500">{formatDate(post.createdAt)}</p>
            </div>
          </div>

          {/* Post Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleReport} className="text-red-600">
                <Flag className="h-4 w-4 mr-2" />
                Report Post
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Post Content */}
        <div className="space-y-3">
          <p className="text-sm leading-relaxed">{post.content}</p>
        </div>

        {/* Post Actions */}
        <div className="flex items-center justify-between pt-2 border-t">
          <div className="flex items-center space-x-4">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onLike?.(post.id)}
              className={`flex items-center space-x-1 ${
                post.isLiked ? 'text-red-500' : 'text-gray-500'
              }`}
            >
              <Heart className={`h-4 w-4 ${post.isLiked ? 'fill-current' : ''}`} />
              <span className="text-xs">{post.likes}</span>
            </Button>

            <Button
              variant="ghost"
              size="sm"
              onClick={() => onComment?.(post.id)}
              className="flex items-center space-x-1 text-gray-500"
            >
              <MessageCircle className="h-4 w-4" />
              <span className="text-xs">{post.comments}</span>
            </Button>

            <Button
              variant="ghost"
              size="sm"
              onClick={() => onShare?.(post.id)}
              className="flex items-center space-x-1 text-gray-500"
            >
              <Share className="h-4 w-4" />
              <span className="text-xs">Share</span>
            </Button>
          </div>
        </div>
      </Card>

      {/* Report Modal */}
      <ReportModal
        isOpen={isOpen}
        onClose={closeReportModal}
        contentType={contentType || 'post'}
        contentId={contentId || ''}
        onReport={handleReportSubmit}
      />
    </>
  )
}