"use client"

import { ToastProvider } from './toast'

// Export ToastProvider as Toaster for compatibility
export const Toaster = ToastProvider

// Re-export everything from toast for convenience
export * from './toast'