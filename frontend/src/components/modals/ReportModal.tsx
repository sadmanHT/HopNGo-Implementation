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
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { AlertTriangle } from 'lucide-react'
import { useToast } from '@/components/ui/toast'

interface ReportModalProps {
  isOpen: boolean
  onClose: () => void
  contentType: 'post' | 'review' | 'listing'
  contentId: string
  onReport?: (reason: string, details?: string) => void
}

const REPORT_REASONS = [
  { value: 'spam', label: 'Spam or unwanted content' },
  { value: 'harassment', label: 'Harassment or bullying' },
  { value: 'hate_speech', label: 'Hate speech or discrimination' },
  { value: 'violence', label: 'Violence or threats' },
  { value: 'nsfw', label: 'Adult or inappropriate content' },
  { value: 'misinformation', label: 'False or misleading information' },
  { value: 'copyright', label: 'Copyright infringement' },
  { value: 'privacy', label: 'Privacy violation' },
  { value: 'other', label: 'Other' },
]

export function ReportModal({
  isOpen,
  onClose,
  contentType,
  contentId,
  onReport,
}: ReportModalProps) {
  const [reason, setReason] = useState('')
  const [details, setDetails] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const { addToast } = useToast()

  const handleSubmit = async () => {
    if (!reason) {
      addToast('Please select a reason for reporting', 'error')
      return
    }

    if (reason.length < 3) {
      addToast('Reason must be at least 3 characters long', 'error')
      return
    }

    if (details.length > 500) {
      addToast('Details must be less than 500 characters', 'error')
      return
    }

    setIsSubmitting(true)

    try {
      // Call the API based on content type
      let endpoint = ''
      switch (contentType) {
        case 'post':
          endpoint = `/api/social/posts/${contentId}/flag`
          break
        case 'review':
          endpoint = `/api/market/reviews/${contentId}/flag`
          break
        case 'listing':
          endpoint = `/api/market/listings/${contentId}/flag`
          break
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          reason,
          details: details || undefined,
        }),
      })

      if (!response.ok) {
        throw new Error('Failed to submit report')
      }

      addToast('Report submitted successfully. Thank you for helping keep our community safe.', 'success')
      
      // Call the optional callback
      onReport?.(reason, details)
      
      // Reset form and close modal
      setReason('')
      setDetails('')
      onClose()
    } catch (error) {
      console.error('Error submitting report:', error)
      addToast('Failed to submit report. Please try again.', 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleClose = () => {
    if (!isSubmitting) {
      setReason('')
      setDetails('')
      onClose()
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-red-500" />
            Report {contentType === 'post' ? 'Post' : contentType === 'review' ? 'Review' : 'Listing'}
          </DialogTitle>
          <DialogDescription>
            Help us maintain a safe and respectful community by reporting inappropriate content.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="reason">Reason for reporting *</Label>
            <Select value={reason} onValueChange={setReason}>
              <SelectTrigger>
                <SelectValue placeholder="Select a reason" />
              </SelectTrigger>
              <SelectContent>
                {REPORT_REASONS.map((reasonOption) => (
                  <SelectItem key={reasonOption.value} value={reasonOption.value}>
                    {reasonOption.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="details">Additional details (optional)</Label>
            <Textarea
              id="details"
              placeholder="Provide any additional context that might help us understand the issue..."
              value={details}
              onChange={(e) => setDetails(e.target.value)}
              maxLength={500}
              rows={3}
            />
            <p className="text-xs text-muted-foreground">
              {details.length}/500 characters
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!reason || isSubmitting}
            className="bg-red-600 hover:bg-red-700"
          >
            {isSubmitting ? 'Submitting...' : 'Submit Report'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}