"use client"

import { useState } from 'react'

interface ReportModalState {
  isOpen: boolean
  contentType: 'post' | 'review' | 'listing' | null
  contentId: string | null
}

export function useReportModal() {
  const [state, setState] = useState<ReportModalState>({
    isOpen: false,
    contentType: null,
    contentId: null,
  })

  const openReportModal = (
    contentType: 'post' | 'review' | 'listing',
    contentId: string
  ) => {
    setState({
      isOpen: true,
      contentType,
      contentId,
    })
  }

  const closeReportModal = () => {
    setState({
      isOpen: false,
      contentType: null,
      contentId: null,
    })
  }

  return {
    ...state,
    openReportModal,
    closeReportModal,
  }
}