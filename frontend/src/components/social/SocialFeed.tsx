'use client';

import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader } from '../ui/card';
import { Button } from '../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { useSocialStore } from '../../lib/state';
import { Heart, MessageCircle, Bookmark, MapPin, MoreHorizontal, Share, Tag } from 'lucide-react';
import Image from 'next/image';
import { formatDistanceToNow } from 'date-fns';
import { Post } from '../../lib/state/social';

interface PostCardProps {
  post: Post;
  onCommentClick?: (post: Post) => void;
  onShareClick?: (post: Post) => void;
}

const PostCard: React.FC<PostCardProps> = ({ post, onCommentClick, onShareClick }) => {
  const {
    likedPosts,
    bookmarkedPosts,
    likePost,
    unlikePost,
    bookmarkPost,
    unbookmarkPost,
    setCurrentPost
  } = useSocialStore();

  const isLiked = likedPosts.has(post.id);
  const isBookmarked = bookmarkedPosts.has(post.id);

  const handleLike = async () => {
    try {
      if (isLiked) {
        await unlikePost(post.id);
      } else {
        await likePost(post.id);
      }
    } catch (error) {
      console.error('Error toggling like:', error);
    }
  };

  const handleBookmark = async () => {
    try {
      if (isBookmarked) {
        await unbookmarkPost(post.id);
      } else {
        await bookmarkPost(post.id);
      }
    } catch (error) {
      console.error('Error toggling bookmark:', error);
    }
  };

  const handleComment = () => {
    setCurrentPost(post);
    onCommentClick?.(post);
  };

  const handleShare = () => {
    onShareClick?.(post);
  };

  const formatDate = (dateString: string) => {
    try {
      return formatDistanceToNow(new Date(dateString), { addSuffix: true });
    } catch {
      return 'Unknown time';
    }
  };

  const getUserInitials = (userId: string) => {
    return userId.substring(0, 2).toUpperCase();
  };

  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Avatar className="h-10 w-10">
              <AvatarImage src={`/api/users/${post.userId}/avatar`} alt="User avatar" />
              <AvatarFallback>{getUserInitials(post.userId)}</AvatarFallback>
            </Avatar>
            <div className="flex flex-col">
              <span className="font-semibold text-sm">User {post.userId.substring(0, 8)}</span>
              <span className="text-xs text-muted-foreground">{formatDate(post.createdAt)}</span>
            </div>
          </div>
          <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>

      <CardContent className="pt-0">
        {/* Post Content */}
        {post.content && (
          <div className="mb-4">
            <p className="text-sm leading-relaxed whitespace-pre-wrap">{post.content}</p>
          </div>
        )}

        {/* Tags */}
        {post.tags && post.tags.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-4">
            {post.tags.map((tag: string) => (
              <div key={tag} className="flex items-center gap-1 bg-primary/10 text-primary px-2 py-1 rounded-full text-xs">
                <Tag className="h-3 w-3" />
                <span>#{tag}</span>
              </div>
            ))}
          </div>
        )}

        {/* Media */}
        {post.mediaUrls && post.mediaUrls.length > 0 && (
          <div className="mb-4">
            {post.mediaUrls.length === 1 ? (
              <div className="relative aspect-video rounded-lg overflow-hidden">
                <Image
                  src={post.mediaUrls[0]}
                  alt="Post image"
                  fill
                  className="object-cover"
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                />
              </div>
            ) : (
              <div className={`grid gap-2 rounded-lg overflow-hidden ${
                post.mediaUrls.length === 2 ? 'grid-cols-2' : 
                post.mediaUrls.length === 3 ? 'grid-cols-2' :
                'grid-cols-2'
              }`}>
                {post.mediaUrls.map((url: string, index: number) => (
                  <div 
                    key={index} 
                    className={`relative aspect-square ${
                      post.mediaUrls && post.mediaUrls.length === 3 && index === 0 ? 'row-span-2' : ''
                    }`}
                  >
                    <Image
                      src={url}
                      alt={`Post image ${index + 1}`}
                      fill
                      className="object-cover"
                      sizes="(max-width: 768px) 50vw, (max-width: 1200px) 25vw, 16vw"
                    />
                    {post.mediaUrls && post.mediaUrls.length > 4 && index === 3 && (
                      <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                        <span className="text-white font-semibold text-lg">
                          +{post.mediaUrls.length - 4}
                        </span>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Location */}
        {post.location && (
          <div className="flex items-center gap-2 mb-4 text-sm text-muted-foreground">
            <MapPin className="h-4 w-4" />
            <span>{post.location.address}</span>
          </div>
        )}

        {/* Engagement Stats */}
        <div className="flex items-center justify-between text-sm text-muted-foreground mb-3">
          <div className="flex items-center gap-4">
            {post.likes > 0 && (
              <span>{post.likes} {post.likes === 1 ? 'like' : 'likes'}</span>
            )}
            {post.commentsCount > 0 && (
              <span>{post.commentsCount} {post.commentsCount === 1 ? 'comment' : 'comments'}</span>
            )}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-between pt-3 border-t">
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={handleLike}
              className={`flex items-center gap-2 ${isLiked ? 'text-red-500 hover:text-red-600' : 'text-muted-foreground hover:text-foreground'}`}
            >
              <Heart className={`h-4 w-4 ${isLiked ? 'fill-current' : ''}`} />
              <span className="text-sm">Like</span>
            </Button>

            <Button
              variant="ghost"
              size="sm"
              onClick={handleComment}
              className="flex items-center gap-2 text-muted-foreground hover:text-foreground"
            >
              <MessageCircle className="h-4 w-4" />
              <span className="text-sm">Comment</span>
            </Button>

            <Button
              variant="ghost"
              size="sm"
              onClick={handleShare}
              className="flex items-center gap-2 text-muted-foreground hover:text-foreground"
            >
              <Share className="h-4 w-4" />
              <span className="text-sm">Share</span>
            </Button>
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={handleBookmark}
            className={`${isBookmarked ? 'text-blue-500 hover:text-blue-600' : 'text-muted-foreground hover:text-foreground'}`}
          >
            <Bookmark className={`h-4 w-4 ${isBookmarked ? 'fill-current' : ''}`} />
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

interface SocialFeedProps {
  className?: string;
  onCommentClick?: (post: Post) => void;
  onShareClick?: (post: Post) => void;
}

export const SocialFeed: React.FC<SocialFeedProps> = ({ 
  className, 
  onCommentClick, 
  onShareClick 
}) => {
  const {
    posts,
    isLoadingPosts,
    hasMorePosts,
    loadFeed,
    loadMorePosts
  } = useSocialStore();

  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    // Load initial feed
    loadFeed();
  }, [loadFeed]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await loadFeed(true); // Refresh from beginning
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleLoadMore = async () => {
    if (!isLoadingPosts && hasMorePosts) {
      await loadMorePosts();
    }
  };

  // Infinite scroll effect
  useEffect(() => {
    const handleScroll = () => {
      if (
        window.innerHeight + document.documentElement.scrollTop >=
        document.documentElement.offsetHeight - 1000
      ) {
        handleLoadMore();
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isLoadingPosts, hasMorePosts]);

  if (posts.length === 0 && !isLoadingPosts) {
    return (
      <div className={`flex flex-col items-center justify-center py-12 ${className}`}>
        <div className="text-center space-y-4">
          <div className="text-6xl">📱</div>
          <h3 className="text-lg font-semibold">No posts yet</h3>
          <p className="text-muted-foreground max-w-sm">
            Be the first to share something! Create a post to get the conversation started.
          </p>
          <Button onClick={handleRefresh} variant="outline">
            Refresh Feed
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Refresh Button */}
      <div className="flex justify-center">
        <Button
          onClick={handleRefresh}
          disabled={isRefreshing}
          variant="outline"
          size="sm"
        >
          {isRefreshing ? 'Refreshing...' : 'Refresh Feed'}
        </Button>
      </div>

      {/* Posts */}
      <div className="space-y-6">
        {posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            onCommentClick={onCommentClick}
            onShareClick={onShareClick}
          />
        ))}
      </div>

      {/* Loading More */}
      {isLoadingPosts && (
        <div className="flex justify-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      )}

      {/* Load More Button */}
      {!isLoadingPosts && hasMorePosts && (
        <div className="flex justify-center py-4">
          <Button onClick={handleLoadMore} variant="outline">
            Load More Posts
          </Button>
        </div>
      )}

      {/* End of Feed */}
      {!hasMorePosts && posts.length > 0 && (
        <div className="text-center py-8 text-muted-foreground">
          <p>You've reached the end of your feed!</p>
        </div>
      )}
    </div>
  );
};