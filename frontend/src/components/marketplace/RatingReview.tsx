'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Star, ThumbsUp, ThumbsDown, Flag, CheckCircle } from 'lucide-react';
import { shoppingService, type Review, type TravelGear } from '@/services/shopping';
import { format } from 'date-fns';

interface RatingReviewProps {
  gearId: string;
  gear?: TravelGear;
  orderId?: string;
  canReview?: boolean;
  showReviews?: boolean;
}

interface ReviewFormData {
  rating: number;
  title: string;
  comment: string;
}

const RatingReview: React.FC<RatingReviewProps> = ({
  gearId,
  gear,
  orderId,
  canReview = false,
  showReviews = true
}) => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  
  const [reviewForm, setReviewForm] = useState<ReviewFormData>({
    rating: 0,
    title: '',
    comment: ''
  });

  // Load reviews on component mount
  useEffect(() => {
    if (showReviews) {
      loadReviews();
    }
  }, [gearId, showReviews]);

  const loadReviews = async () => {
    setIsLoading(true);
    try {
      const gearReviews = await shoppingService.getGearReviews(gearId);
      setReviews(gearReviews.items);
    } catch (error) {
      console.error('Failed to load reviews:', error);
      setError('Failed to load reviews');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRatingClick = (rating: number) => {
    setReviewForm(prev => ({ ...prev, rating }));
  };

  const handleInputChange = (field: keyof ReviewFormData, value: string) => {
    setReviewForm(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmitReview = async () => {
    if (!reviewForm.rating || !reviewForm.title.trim() || !reviewForm.comment.trim()) {
      setError('Please provide a rating, title, and comment');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const newReview = await shoppingService.createReview({
        gearId,
        orderId: orderId || '',
        rating: reviewForm.rating,
        title: reviewForm.title.trim(),
        comment: reviewForm.comment.trim(),

      });

      // Add new review to the list
      setReviews(prev => [newReview, ...prev]);
      
      // Reset form and show success
      setReviewForm({
        rating: 0,
        title: '',
        comment: ''
      });
      setShowReviewForm(false);
      setSuccess(true);
      
      // Hide success message after 3 seconds
      setTimeout(() => setSuccess(false), 3000);
    } catch (error) {
      console.error('Failed to submit review:', error);
      setError('Failed to submit review. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderStars = (rating: number, interactive = false, size = 'w-5 h-5') => {
    return (
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            className={`${size} ${
              star <= rating
                ? 'fill-yellow-400 text-yellow-400'
                : 'text-gray-300'
            } ${
              interactive ? 'cursor-pointer hover:text-yellow-400' : ''
            }`}
            onClick={interactive ? () => handleRatingClick(star) : undefined}
          />
        ))}
      </div>
    );
  };

  const calculateAverageRating = () => {
    if (reviews.length === 0) return 0;
    const sum = reviews.reduce((acc, review) => acc + review.rating, 0);
    return sum / reviews.length;
  };

  const getRatingDistribution = () => {
    const distribution = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    reviews.forEach(review => {
      distribution[review.rating as keyof typeof distribution]++;
    });
    return distribution;
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="animate-pulse space-y-4">
            <div className="h-4 bg-gray-200 rounded w-1/4"></div>
            <div className="h-20 bg-gray-200 rounded"></div>
            <div className="h-20 bg-gray-200 rounded"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Success Message */}
      {success && (
        <Card className="border-green-200 bg-green-50">
          <CardContent className="p-4">
            <div className="flex items-center gap-2 text-green-800">
              <CheckCircle className="h-5 w-5" />
              <span className="font-medium">Review submitted successfully!</span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Error Message */}
      {error && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-4">
            <div className="flex items-center gap-2 text-red-800">
              <Flag className="h-5 w-5" />
              <span className="font-medium">{error}</span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Review Summary */}
      {showReviews && reviews.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span>Customer Reviews</span>
              <Badge variant="secondary">{reviews.length} reviews</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid md:grid-cols-2 gap-6">
              {/* Average Rating */}
              <div className="space-y-4">
                <div className="text-center">
                  <div className="text-4xl font-bold text-gray-900">
                    {calculateAverageRating().toFixed(1)}
                  </div>
                  <div className="flex justify-center mt-2">
                    {renderStars(Math.round(calculateAverageRating()))}
                  </div>
                  <div className="text-sm text-gray-600 mt-1">
                    Based on {reviews.length} reviews
                  </div>
                </div>
              </div>

              {/* Rating Distribution */}
              <div className="space-y-2">
                {Object.entries(getRatingDistribution())
                  .reverse()
                  .map(([rating, count]) => {
                    const percentage = reviews.length > 0 ? (count / reviews.length) * 100 : 0;
                    return (
                      <div key={rating} className="flex items-center gap-2 text-sm">
                        <span className="w-8">{rating}★</span>
                        <div className="flex-1 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-yellow-400 h-2 rounded-full"
                            style={{ width: `${percentage}%` }}
                          />
                        </div>
                        <span className="w-8 text-gray-600">{count}</span>
                      </div>
                    );
                  })}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Write Review Button */}
      {canReview && !showReviewForm && (
        <Card>
          <CardContent className="p-6">
            <div className="text-center">
              <h3 className="text-lg font-semibold mb-2">Share Your Experience</h3>
              <p className="text-gray-600 mb-4">
                Help other travelers by writing a review for {gear?.title || 'this item'}
              </p>
              <Button onClick={() => setShowReviewForm(true)}>
                Write a Review
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Review Form */}
      {showReviewForm && (
        <Card>
          <CardHeader>
            <CardTitle>Write Your Review</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Rating */}
            <div>
              <label className="block text-sm font-medium mb-2">Rating *</label>
              {renderStars(reviewForm.rating, true, 'w-8 h-8')}
            </div>

            {/* Title */}
            <div>
              <label className="block text-sm font-medium mb-2">Review Title *</label>
              <input
                type="text"
                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Summarize your experience"
                value={reviewForm.title}
                onChange={(e) => handleInputChange('title', e.target.value)}
                maxLength={100}
              />
            </div>

            {/* Comment */}
            <div>
              <label className="block text-sm font-medium mb-2">Your Review *</label>
              <Textarea
                className="min-h-[120px]"
                placeholder="Tell us about your experience with this item..."
                value={reviewForm.comment}
                onChange={(e) => handleInputChange('comment', e.target.value)}
                maxLength={1000}
              />
            </div>



            {/* Action Buttons */}
            <div className="flex gap-3 pt-4">
              <Button
                onClick={handleSubmitReview}
                disabled={isSubmitting || !reviewForm.rating || !reviewForm.title.trim() || !reviewForm.comment.trim()}
                className="flex-1"
              >
                {isSubmitting ? 'Submitting...' : 'Submit Review'}
              </Button>
              <Button
                variant="outline"
                onClick={() => setShowReviewForm(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Reviews List */}
      {showReviews && reviews.length > 0 && (
        <div className="space-y-4">
          <h3 className="text-lg font-semibold">All Reviews</h3>
          {reviews.map((review) => (
            <Card key={review.id}>
              <CardContent className="p-6">
                <div className="space-y-4">
                  {/* Review Header */}
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <Avatar className="h-10 w-10">
                        <AvatarImage src={review.user.avatar} />
                        <AvatarFallback>
                          {review.user.name?.charAt(0).toUpperCase() || 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium">{review.user.name || 'Anonymous'}</div>
                        <div className="text-sm text-gray-600">
                          {format(new Date(review.createdAt), 'MMM d, yyyy')}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {renderStars(review.rating)}
                      <Badge variant="outline">{review.rating}/5</Badge>
                    </div>
                  </div>

                  {/* Review Content */}
                  <div>
                    <h4 className="font-semibold mb-2">{review.title}</h4>
                    <p className="text-gray-700 leading-relaxed">{review.comment}</p>
                  </div>



                  {/* Verified Purchase Badge */}
                  {review.orderId && (
                    <div className="pt-2">
                      <Badge variant="secondary" className="text-xs">
                        <CheckCircle className="h-3 w-3 mr-1" />
                        Verified Purchase
                      </Badge>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* No Reviews Message */}
      {showReviews && reviews.length === 0 && (
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-gray-500">
              <Star className="h-12 w-12 mx-auto mb-4 text-gray-300" />
              <h3 className="text-lg font-medium mb-2">No reviews yet</h3>
              <p>Be the first to share your experience with this item!</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default RatingReview;