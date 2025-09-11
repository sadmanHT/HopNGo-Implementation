'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import ItineraryTimeline from '@/components/ItineraryTimeline';
import { 
  MapPin, 
  Calendar, 
  DollarSign, 
  Clock,
  MessageCircle,
  Send,
  User
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

interface SharedItinerary {
  id: string;
  title: string;
  days: number;
  budget: number;
  origin: {
    city: string;
    country: string;
  };
  destinations: Array<{
    city: string;
    country: string;
  }>;
  plan?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
  shareInfo: {
    visibility: 'PRIVATE' | 'LINK' | 'PUBLIC';
    canComment: boolean;
  };
}

interface Comment {
  id: string;
  itineraryId: string;
  authorUserId: string;
  message: string;
  createdAt: string;
}

export default function SharedItineraryPage() {
  const params = useParams();
  const token = params?.token as string;
  
  const [itinerary, setItinerary] = useState<SharedItinerary | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newComment, setNewComment] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);

  const formatBudget = (budget: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(budget / 100);
  };

  const fetchSharedItinerary = async () => {
    try {
      const response = await fetch(`/api/trips/share/${token}`);
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error('Shared itinerary not found or no longer available');
        }
        throw new Error('Failed to load shared itinerary');
      }
      const data = await response.json();
      setItinerary(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    }
  };

  const fetchComments = async () => {
    if (!itinerary) return;
    
    try {
      const response = await fetch(`/api/trips/${itinerary.id}/comments`);
      if (response.ok) {
        const data = await response.json();
        setComments(data);
      }
    } catch (err) {
      console.error('Failed to load comments:', err);
    }
  };

  const submitComment = async () => {
    if (!newComment.trim() || !itinerary || submittingComment) return;
    
    setSubmittingComment(true);
    try {
      const response = await fetch(`/api/trips/${itinerary.id}/comments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message: newComment.trim() }),
      });
      
      if (response.ok) {
        setNewComment('');
        await fetchComments(); // Refresh comments
      } else {
        throw new Error('Failed to submit comment');
      }
    } catch (err) {
      console.error('Error submitting comment:', err);
      alert('Failed to submit comment. Please try again.');
    } finally {
      setSubmittingComment(false);
    }
  };

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      await fetchSharedItinerary();
      setLoading(false);
    };
    
    if (token) {
      loadData();
    }
  }, [token]);

  useEffect(() => {
    if (itinerary && itinerary.shareInfo.canComment) {
      fetchComments();
    }
  }, [itinerary]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading shared itinerary...</p>
        </div>
      </div>
    );
  }

  if (error || !itinerary) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Card className="w-full max-w-md">
          <CardContent className="text-center py-8">
            <div className="text-red-500 text-6xl mb-4">⚠️</div>
            <h2 className="text-xl font-semibold mb-2">Itinerary Not Available</h2>
            <p className="text-gray-600 mb-4">
              {error || 'This shared itinerary could not be found or is no longer available.'}
            </p>
            <Button onClick={() => window.location.href = '/trips'} variant="outline">
              Go to Trips
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-3xl font-bold text-gray-900">{itinerary.title}</h1>
            <div className="text-sm text-gray-500">
              Shared itinerary
            </div>
          </div>
          
          {/* Trip Info */}
          <div className="flex flex-wrap gap-4 text-sm text-gray-600">
            <div className="flex items-center space-x-1">
              <MapPin className="h-4 w-4" />
              <span>
                {itinerary.destinations.map(dest => `${dest.city}, ${dest.country}`).join(' → ')}
              </span>
            </div>
            <div className="flex items-center space-x-1">
              <Calendar className="h-4 w-4" />
              <span>{itinerary.days} days</span>
            </div>
            <div className="flex items-center space-x-1">
              <DollarSign className="h-4 w-4" />
              <span>{formatBudget(itinerary.budget)}</span>
            </div>
            <div className="flex items-center space-x-1">
              <Clock className="h-4 w-4" />
              <span>Created {formatDistanceToNow(new Date(itinerary.createdAt), { addSuffix: true })}</span>
            </div>
          </div>
        </div>

        {/* Itinerary Content */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Itinerary Details</CardTitle>
          </CardHeader>
          <CardContent>
            <ItineraryTimeline plan={itinerary.plan || {}} />
          </CardContent>
        </Card>

        {/* Comments Section */}
        {itinerary.shareInfo.canComment && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <MessageCircle className="h-5 w-5" />
                <span>Comments ({comments.length})</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Add Comment Form */}
              <div className="space-y-3">
                <Textarea
                  placeholder="Add a comment..."
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  className="min-h-[80px]"
                />
                <div className="flex justify-end">
                  <Button
                    onClick={submitComment}
                    disabled={!newComment.trim() || submittingComment}
                    size="sm"
                  >
                    <Send className="h-4 w-4 mr-1" />
                    {submittingComment ? 'Posting...' : 'Post Comment'}
                  </Button>
                </div>
              </div>

              {/* Comments List */}
              <div className="space-y-4 border-t pt-4">
                {comments.length === 0 ? (
                  <p className="text-gray-500 text-center py-4">
                    No comments yet. Be the first to comment!
                  </p>
                ) : (
                  comments.map((comment) => (
                    <div key={comment.id} className="flex space-x-3">
                      <div className="flex-shrink-0">
                        <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                          <User className="h-4 w-4 text-gray-500" />
                        </div>
                      </div>
                      <div className="flex-1">
                        <div className="bg-gray-50 rounded-lg p-3">
                          <p className="text-sm text-gray-900">{comment.message}</p>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          {formatDistanceToNow(new Date(comment.createdAt), { addSuffix: true })}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}