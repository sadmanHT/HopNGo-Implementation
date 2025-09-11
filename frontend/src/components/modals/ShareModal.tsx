"use client"

import React, { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
// Switch component not available, using checkbox instead
import { Share2, Copy, Check, Globe, Link, Lock } from 'lucide-react'
import { useToast } from '@/components/ui/toast'
import { useAuthStore } from '@/lib/state'

interface ShareModalProps {
  isOpen: boolean
  onClose: () => void
  itineraryId: string
  itineraryTitle: string
}

type ShareVisibility = 'PRIVATE' | 'LINK' | 'PUBLIC'

interface ShareResponse {
  shareId: string
  token: string
  shareUrl: string
  visibility: ShareVisibility
  canComment: boolean
  createdAt: string
}

const VISIBILITY_OPTIONS = [
  {
    value: 'PRIVATE' as ShareVisibility,
    label: 'Private',
    description: 'Only you can see this itinerary',
    icon: Lock
  },
  {
    value: 'LINK' as ShareVisibility,
    label: 'Anyone with link',
    description: 'Anyone with the link can view',
    icon: Link
  },
  {
    value: 'PUBLIC' as ShareVisibility,
    label: 'Public',
    description: 'Anyone can find and view',
    icon: Globe
  }
]

export function ShareModal({
  isOpen,
  onClose,
  itineraryId,
  itineraryTitle,
}: ShareModalProps) {
  const [visibility, setVisibility] = useState<ShareVisibility>('LINK')
  const [canComment, setCanComment] = useState(true)
  const [shareUrl, setShareUrl] = useState('')
  const [isSharing, setIsSharing] = useState(false)
  const [isShared, setIsShared] = useState(false)
  const [copied, setCopied] = useState(false)
  const { addToast } = useToast()
  const { user, token } = useAuthStore()

  const handleShare = async () => {
    if (!user || !token) {
      addToast('Please log in to share itineraries', 'error')
      return
    }

    setIsSharing(true)

    try {
      const response = await fetch(`http://localhost:8087/api/trips/${itineraryId}/share`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user.id,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          visibility,
          canComment
        })
      })

      if (!response.ok) {
        throw new Error('Failed to share itinerary')
      }

      const shareData: ShareResponse = await response.json()
      setShareUrl(shareData.shareUrl)
      setIsShared(true)
      addToast('Itinerary shared successfully!', 'success')
    } catch (error) {
      console.error('Error sharing itinerary:', error)
      addToast('Failed to share itinerary. Please try again.', 'error')
    } finally {
      setIsSharing(false)
    }
  }

  const handleCopyLink = async () => {
    if (!shareUrl) return

    try {
      await navigator.clipboard.writeText(shareUrl)
      setCopied(true)
      addToast('Link copied to clipboard!', 'success')
      setTimeout(() => setCopied(false), 2000)
    } catch (error) {
      console.error('Failed to copy link:', error)
      addToast('Failed to copy link', 'error')
    }
  }

  const handleRemoveSharing = async () => {
    if (!user || !token) return

    try {
      const response = await fetch(`http://localhost:8087/api/trips/${itineraryId}/share`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': user.id
        }
      })

      if (response.ok) {
        setIsShared(false)
        setShareUrl('')
        addToast('Sharing removed successfully', 'success')
      }
    } catch (error) {
      console.error('Error removing sharing:', error)
      addToast('Failed to remove sharing', 'error')
    }
  }

  const handleClose = () => {
    if (!isSharing) {
      setVisibility('LINK')
      setCanComment(true)
      setShareUrl('')
      setIsShared(false)
      setCopied(false)
      onClose()
    }
  }

  const selectedOption = VISIBILITY_OPTIONS.find(option => option.value === visibility)
  const IconComponent = selectedOption?.icon || Share2

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Share2 className="h-5 w-5 text-blue-500" />
            Share Itinerary
          </DialogTitle>
          <DialogDescription>
            Share "{itineraryTitle}" with others
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {!isShared ? (
            <>
              <div className="space-y-2">
                <Label htmlFor="visibility">Who can access</Label>
                <Select value={visibility} onValueChange={(value: ShareVisibility) => setVisibility(value)}>
                  <SelectTrigger>
                    <SelectValue>
                      <div className="flex items-center gap-2">
                        <IconComponent className="h-4 w-4" />
                        {selectedOption?.label}
                      </div>
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {VISIBILITY_OPTIONS.map((option) => {
                      const OptionIcon = option.icon
                      return (
                        <SelectItem key={option.value} value={option.value}>
                          <div className="flex items-center gap-2">
                            <OptionIcon className="h-4 w-4" />
                            <div>
                              <div className="font-medium">{option.label}</div>
                              <div className="text-xs text-muted-foreground">{option.description}</div>
                            </div>
                          </div>
                        </SelectItem>
                      )
                    })}
                  </SelectContent>
                </Select>
              </div>

              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label htmlFor="comments">Allow comments</Label>
                  <div className="text-sm text-muted-foreground">
                    Let others comment on your itinerary
                  </div>
                </div>
                <input
                  type="checkbox"
                  id="comments"
                  checked={canComment}
                  onChange={(e) => setCanComment(e.target.checked)}
                  className="h-4 w-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2"
                />
              </div>
            </>
          ) : (
            <>
              <div className="space-y-2">
                <Label>Share link</Label>
                <div className="flex gap-2">
                  <Input
                    value={shareUrl}
                    readOnly
                    className="flex-1"
                  />
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleCopyLink}
                    className="shrink-0"
                  >
                    {copied ? (
                      <Check className="h-4 w-4" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>

              <div className="text-sm text-muted-foreground">
                <div className="flex items-center gap-2 mb-1">
                  <IconComponent className="h-4 w-4" />
                  {selectedOption?.label}
                </div>
                <div>Comments: {canComment ? 'Enabled' : 'Disabled'}</div>
              </div>
            </>
          )}
        </div>

        <DialogFooter>
          {!isShared ? (
            <>
              <Button
                variant="outline"
                onClick={handleClose}
                disabled={isSharing}
              >
                Cancel
              </Button>
              <Button
                onClick={handleShare}
                disabled={isSharing}
                className="bg-blue-600 hover:bg-blue-700"
              >
                {isSharing ? 'Sharing...' : 'Share Itinerary'}
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outline"
                onClick={handleRemoveSharing}
                className="text-red-600 hover:text-red-700"
              >
                Remove Sharing
              </Button>
              <Button onClick={handleClose}>
                Done
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}