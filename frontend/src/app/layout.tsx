import type { Metadata } from "next";
import { ToastProvider } from "@/components/ui/toast";
import { QueryProvider } from "@/providers/QueryProvider";
import { AnalyticsProvider } from "@/providers/AnalyticsProvider";
import { CookieBanner } from "@/components/ui/cookie-banner";
import { AccessibilityProvider } from "@/components/accessibility/AccessibilityProvider";
import "./globals.css";
import "@/styles/accessibility.css";

export const metadata: Metadata = {
  title: "HopNGo - Travel & Tourism Platform",
  description: "Your complete travel companion for Bangladesh - book trips, explore markets, plan itineraries",
  manifest: "/manifest.webmanifest",
  themeColor: "#10b981",
  viewport: {
    width: "device-width",
    initialScale: 1,
    maximumScale: 1,
    userScalable: false,
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "HopNGo",
  },
  formatDetection: {
    telephone: false,
  },
  openGraph: {
    type: "website",
    siteName: "HopNGo",
    title: "HopNGo - Travel & Tourism Platform",
    description: "Your complete travel companion for Bangladesh",
  },
  twitter: {
    card: "summary",
    title: "HopNGo - Travel & Tourism Platform",
    description: "Your complete travel companion for Bangladesh",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">
        <AccessibilityProvider>
          <QueryProvider>
            <AnalyticsProvider>
              <ToastProvider>
                {children}
                <CookieBanner />
              </ToastProvider>
            </AnalyticsProvider>
          </QueryProvider>
        </AccessibilityProvider>
      </body>
    </html>
  );
}
