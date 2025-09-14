import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { generateListingMetadata, generateProductSchema } from '@/lib/seo';
import ListingDetailsClient from './ListingDetailsClient';



// Server-side data fetching
async function getListing(id: string) {
  try {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    const response = await fetch(`${baseUrl}/api/bookings/${id}`, {
      next: { revalidate: 300 }, // Revalidate every 5 minutes
    });
    
    if (!response.ok) {
      return null;
    }
    
    return response.json();
  } catch (error) {
    console.error('Error fetching listing:', error);
    return null;
  }
}

async function getReviews(id: string) {
  try {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    const response = await fetch(`${baseUrl}/api/bookings/${id}/reviews`, {
      next: { revalidate: 300 },
    });
    
    if (!response.ok) {
      return [];
    }
    
    return response.json();
  } catch (error) {
    console.error('Error fetching reviews:', error);
    return [];
  }
}

// Generate metadata for SEO
export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  const listing = await getListing(id);
  
  if (!listing) {
    return {
      title: 'Listing Not Found | HopNGo',
      description: 'The listing you are looking for could not be found.',
    };
  }
  
  return generateListingMetadata(listing);
}

export default async function ListingDetailsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [listing, reviews] = await Promise.all([
    getListing(id),
    getReviews(id),
  ]);
  
  if (!listing) {
    notFound();
  }
  
  // Generate JSON-LD schema
  const productSchema = generateProductSchema(listing);
  
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(productSchema),
        }}
      />
      <ListingDetailsClient listing={listing} reviews={reviews} />
    </>
  );
}