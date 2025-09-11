import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { ToastProvider } from "@/components/ui/toast";
import { QueryProvider } from "@/providers/QueryProvider";
import { AnalyticsProvider } from "@/providers/AnalyticsProvider";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

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
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <QueryProvider>
          <AnalyticsProvider>
            <ToastProvider>
              {children}
            </ToastProvider>
          </AnalyticsProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
