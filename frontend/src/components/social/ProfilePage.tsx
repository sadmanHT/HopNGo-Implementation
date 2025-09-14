'use client';

import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader } from '../ui/card';
import { Button } from '../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { Badge } from '../ui/badge';
import { useSocialStore, useAuthStore } from '../../lib/state';
import { SocialFeed } from './SocialFeed';
import { MapPin, Calendar, Link as LinkIcon, Users, Heart, Bookmark, Settings, UserPlus, UserMinus } from 'lucide-react';
import { Post } from '../../lib/state/social';

interface ProfilePageProps {
  userId?: string; // If not provided, shows current user's profile
  className?: string;
}

interface ProfileStats {
  posts: number;
  followers: number;
  following: number;
}

interface UserPosts {
  posts: Post[];
  likedPosts: Post[];
  bookmarkedPosts: Post[];
}

export const ProfilePage: React.FC<ProfilePageProps> = ({ userId, className }) => {
  const { user: currentUser } = useAuthStore();
  const {
    userProfile,
    isLoadingProfile,
    loadUserProfile,
    followUser,
    unfollowUser,
    posts
  } = useSocialStore();

  const [activeTab, setActiveTab] = useState('posts');
  const [userPosts, setUserPosts] = useState<UserPosts>({
    posts: [],
    likedPosts: [],
    bookmarkedPosts: []
  });
  const [isFollowing, setIsFollowing] = useState(false);
  const [stats, setStats] = useState<ProfileStats>({
    posts: 0,
    followers: 0,
    following: 0
  });

  const targetUserId = userId || currentUser?.id;
  const isOwnProfile = !userId || userId === currentUser?.id;

  useEffect(() => {
    if (targetUserId) {
      loadUserProfile(targetUserId);
      loadUserPosts(targetUserId);
    }
  }, [targetUserId, loadUserProfile]);

  useEffect(() => {
    if (userProfile) {
      setStats({
        posts: userProfile.postsCount,
        followers: userProfile.followersCount,
        following: userProfile.followingCount
      });
      setIsFollowing(userProfile.isFollowing || false);
    }
  }, [userProfile]);

  const loadUserPosts = async (userId: string) => {
    // Filter posts by user ID from the global posts state
    const userSpecificPosts = posts.filter(post => post.userId === userId);
    
    // TODO: Load liked and bookmarked posts from API
    setUserPosts({
      posts: userSpecificPosts,
      likedPosts: [], // TODO: Implement
      bookmarkedPosts: [] // TODO: Implement
    });
  };

  const handleFollow = async () => {
    if (!targetUserId) return;
    
    try {
      if (isFollowing) {
        await unfollowUser(targetUserId);
        setStats(prev => ({ ...prev, followers: prev.followers - 1 }));
      } else {
        await followUser(targetUserId);
        setStats(prev => ({ ...prev, followers: prev.followers + 1 }));
      }
      setIsFollowing(!isFollowing);
    } catch (error) {
      console.error('Error toggling follow:', error);
    }
  };

  const formatNumber = (num: number): string => {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    }
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
  };

  const getUserInitials = (name: string): string => {
    return name
      .split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  };

  if (isLoadingProfile) {
    return (
      <div className={`flex justify-center items-center py-12 ${className}`}>
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!userProfile && !isLoadingProfile) {
    return (
      <div className={`flex flex-col items-center justify-center py-12 ${className}`}>
        <div className="text-center space-y-4">
          <div className="text-6xl">👤</div>
          <h3 className="text-lg font-semibold">User not found</h3>
          <p className="text-muted-foreground">
            The user you're looking for doesn't exist or has been removed.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`max-w-4xl mx-auto space-y-6 ${className}`}>
      {/* Profile Header */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-6">
            {/* Avatar and Basic Info */}
            <div className="flex flex-col items-center md:items-start">
              <Avatar className="h-24 w-24 md:h-32 md:w-32">
                <AvatarImage 
                  src={userProfile?.avatar || `/api/users/${targetUserId}/avatar`} 
                  alt={`${userProfile?.name || 'User'}'s avatar`} 
                />
                <AvatarFallback className="text-lg md:text-xl">
                  {getUserInitials(userProfile?.name || 'User')}
                </AvatarFallback>
              </Avatar>
              
              {/* Action Buttons */}
              <div className="flex gap-2 mt-4">
                {isOwnProfile ? (
                  <Button variant="outline" size="sm">
                    <Settings className="h-4 w-4 mr-2" />
                    Edit Profile
                  </Button>
                ) : (
                  <Button 
                    onClick={handleFollow}
                    variant={isFollowing ? "outline" : "default"}
                    size="sm"
                  >
                    {isFollowing ? (
                      <>
                        <UserMinus className="h-4 w-4 mr-2" />
                        Unfollow
                      </>
                    ) : (
                      <>
                        <UserPlus className="h-4 w-4 mr-2" />
                        Follow
                      </>
                    )}
                  </Button>
                )}
              </div>
            </div>

            {/* Profile Details */}
            <div className="flex-1 space-y-4">
              <div>
                <h1 className="text-2xl font-bold">{userProfile?.name || 'User'}</h1>
                <p className="text-muted-foreground">@{targetUserId?.substring(0, 8) || 'user'}</p>
              </div>

              {/* Bio */}
              {userProfile?.bio && (
                <p className="text-sm leading-relaxed">{userProfile.bio}</p>
              )}

              {/* Additional Info */}
              <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
                <div className="flex items-center gap-1">
                  <Calendar className="h-4 w-4" />
                  <span>Joined March 2024</span>
                </div>
                <div className="flex items-center gap-1">
                  <MapPin className="h-4 w-4" />
                  <span>Location</span>
                </div>
                <div className="flex items-center gap-1">
                  <LinkIcon className="h-4 w-4" />
                  <a href="#" className="hover:underline">website.com</a>
                </div>
              </div>

              {/* Stats */}
              <div className="flex gap-6">
                <div className="text-center">
                  <div className="font-bold text-lg">{formatNumber(stats.posts)}</div>
                  <div className="text-sm text-muted-foreground">Posts</div>
                </div>
                <div className="text-center cursor-pointer hover:underline">
                  <div className="font-bold text-lg">{formatNumber(stats.followers)}</div>
                  <div className="text-sm text-muted-foreground">Followers</div>
                </div>
                <div className="text-center cursor-pointer hover:underline">
                  <div className="font-bold text-lg">{formatNumber(stats.following)}</div>
                  <div className="text-sm text-muted-foreground">Following</div>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Content Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="posts" className="flex items-center gap-2">
            <Users className="h-4 w-4" />
            Posts
          </TabsTrigger>
          {isOwnProfile && (
            <>
              <TabsTrigger value="liked" className="flex items-center gap-2">
                <Heart className="h-4 w-4" />
                Liked
              </TabsTrigger>
              <TabsTrigger value="bookmarked" className="flex items-center gap-2">
                <Bookmark className="h-4 w-4" />
                Saved
              </TabsTrigger>
            </>
          )}
        </TabsList>

        {/* Posts Tab */}
        <TabsContent value="posts" className="mt-6">
          {userPosts.posts.length > 0 ? (
            <div className="space-y-6">
              {userPosts.posts.map((post) => (
                <Card key={post.id}>
                  <CardContent className="pt-6">
                    {/* Post content preview */}
                    <div className="space-y-4">
                      {post.content && (
                        <p className="text-sm leading-relaxed line-clamp-3">{post.content}</p>
                      )}
                      
                      {post.mediaUrls && post.mediaUrls.length > 0 && (
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                          {post.mediaUrls.slice(0, 6).map((url: string, index: number) => (
                            <div key={index} className="relative aspect-square rounded-lg overflow-hidden">
                              <img
                                src={url}
                                alt={`Post image ${index + 1}`}
                                className="w-full h-full object-cover"
                              />
                            </div>
                          ))}
                        </div>
                      )}
                      
                      <div className="flex items-center justify-between text-sm text-muted-foreground">
                        <div className="flex items-center gap-4">
                          <span className="flex items-center gap-1">
                            <Heart className="h-4 w-4" />
                            {post.likes}
                          </span>
                          <span className="flex items-center gap-1">
                            <Users className="h-4 w-4" />
                            {post.commentsCount}
                          </span>
                        </div>
                        <span>{new Date(post.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">📝</div>
              <h3 className="text-lg font-semibold mb-2">
                {isOwnProfile ? "You haven't posted anything yet" : "No posts yet"}
              </h3>
              <p className="text-muted-foreground">
                {isOwnProfile 
                  ? "Share your first post to get started!" 
                  : "This user hasn't shared any posts yet."}
              </p>
            </div>
          )}
        </TabsContent>

        {/* Liked Posts Tab (Own Profile Only) */}
        {isOwnProfile && (
          <TabsContent value="liked" className="mt-6">
            {userPosts.likedPosts.length > 0 ? (
              <SocialFeed className="" />
            ) : (
              <div className="text-center py-12">
                <div className="text-6xl mb-4">❤️</div>
                <h3 className="text-lg font-semibold mb-2">No liked posts yet</h3>
                <p className="text-muted-foreground">
                  Posts you like will appear here.
                </p>
              </div>
            )}
          </TabsContent>
        )}

        {/* Bookmarked Posts Tab (Own Profile Only) */}
        {isOwnProfile && (
          <TabsContent value="bookmarked" className="mt-6">
            {userPosts.bookmarkedPosts.length > 0 ? (
              <SocialFeed className="" />
            ) : (
              <div className="text-center py-12">
                <div className="text-6xl mb-4">🔖</div>
                <h3 className="text-lg font-semibold mb-2">No saved posts yet</h3>
                <p className="text-muted-foreground">
                  Posts you bookmark will appear here.
                </p>
              </div>
            )}
          </TabsContent>
        )}
      </Tabs>
    </div>
  );
};