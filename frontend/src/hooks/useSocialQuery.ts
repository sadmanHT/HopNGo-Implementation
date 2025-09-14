'use client';

import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';
import { socialApi, CreatePostRequest, UpdatePostRequest, CreateCommentRequest, UpdateProfileRequest } from '../services/social';
import { Post, Comment, UserProfile } from '../lib/state/social';
import { useSocialStore } from '../lib/state';
import { toast } from 'sonner';

// Query Keys
export const socialQueryKeys = {
  all: ['social'] as const,
  posts: () => [...socialQueryKeys.all, 'posts'] as const,
  post: (id: string) => [...socialQueryKeys.posts(), id] as const,
  feed: (type: string) => [...socialQueryKeys.posts(), 'feed', type] as const,
  userPosts: (userId: string) => [...socialQueryKeys.posts(), 'user', userId] as const,
  comments: (postId: string) => [...socialQueryKeys.all, 'comments', postId] as const,
  users: () => [...socialQueryKeys.all, 'users'] as const,
  user: (id: string) => [...socialQueryKeys.users(), id] as const,
  followers: (userId: string) => [...socialQueryKeys.users(), userId, 'followers'] as const,
  following: (userId: string) => [...socialQueryKeys.users(), userId, 'following'] as const,
  heatmap: (filters: any) => [...socialQueryKeys.all, 'heatmap', filters] as const,
};

// Posts Hooks
export const usePosts = (userId?: string) => {
  return useInfiniteQuery({
    queryKey: userId ? socialQueryKeys.userPosts(userId) : socialQueryKeys.posts(),
    queryFn: async ({ pageParam = 1 }) => {
      return await socialApi.getPosts(pageParam, 20, userId);
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      return lastPage.hasMore ? lastPage.page + 1 : undefined;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
};

export const usePost = (postId: string) => {
  return useQuery({
    queryKey: socialQueryKeys.post(postId),
    queryFn: () => socialApi.getPost(postId),
    enabled: !!postId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useFeed = (type: 'following' | 'trending' | 'nearby' = 'following') => {
  return useInfiniteQuery({
    queryKey: socialQueryKeys.feed(type),
    queryFn: async ({ pageParam = 1 }) => {
      return await socialApi.getFeed(pageParam, 20, type);
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      return lastPage.hasMore ? lastPage.page + 1 : undefined;
    },
    staleTime: 2 * 60 * 1000, // 2 minutes for feed
    gcTime: 5 * 60 * 1000,
  });
};

export const useCreatePost = () => {
  const queryClient = useQueryClient();
  const { createPost } = useSocialStore();

  return useMutation({
    mutationFn: (data: CreatePostRequest) => socialApi.createPost(data),
    onMutate: async (newPost) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: socialQueryKeys.posts() });
      await queryClient.cancelQueries({ queryKey: socialQueryKeys.feed('following') });

      // Optimistic update - create temporary post
      const optimisticPost: Post = {
        id: `temp-${Date.now()}`,
        userId: 'current-user', // TODO: Get from auth
        userName: 'You',
        userAvatar: '/api/users/current/avatar',
        content: newPost.content,
        mediaUrls: newPost.mediaUrls,
        location: newPost.location,
        tags: newPost.tags || [],
        likes: 0,
        commentsCount: 0,
        isLiked: false,
        isBookmarked: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      // Update Zustand store
      createPost(optimisticPost);

      // Update React Query cache
      queryClient.setQueryData(socialQueryKeys.posts(), (old: any) => {
        if (!old) return { pages: [{ items: [optimisticPost], hasMore: false }], pageParams: [1] };
        
        const newPages = [...old.pages];
        newPages[0] = {
          ...newPages[0],
          items: [optimisticPost, ...newPages[0].items],
        };
        
        return { ...old, pages: newPages };
      });

      return { optimisticPost };
    },
    onSuccess: (newPost, variables, context) => {
      // Update the posts array with the real post
      if (context?.optimisticPost) {
        const { posts } = useSocialStore.getState();
        const updatedPosts = posts.map(post => 
          post.id === context.optimisticPost.id ? newPost : post
        );
        useSocialStore.setState({ posts: updatedPosts });
      }
      
      // Invalidate and refetch
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.posts() });
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.feed('following') });
      
      toast.success('Post created successfully!');
    },
    onError: (error, variables, context) => {
      // Remove optimistic post on error
      if (context?.optimisticPost) {
        const { posts } = useSocialStore.getState();
        const filteredPosts = posts.filter(post => post.id !== context.optimisticPost.id);
        useSocialStore.setState({ posts: filteredPosts });
      }
      
      // Revert React Query cache
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.posts() });
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.feed('following') });
      
      toast.error('Failed to create post. Please try again.');
      console.error('Create post error:', error);
    },
  });
};

// Note: updatePost functionality is not implemented in the social store
// This hook is commented out until the store method is added
// export const useUpdatePost = () => {
//   const queryClient = useQueryClient();
//   return useMutation({
//     mutationFn: ({ postId, data }: { postId: string; data: UpdatePostRequest }) =>
//       socialApi.updatePost(postId, data),
//     onSuccess: (updatedPost) => {
//       // Update posts in store manually
//       const { posts } = useSocialStore.getState();
//       const updatedPosts = posts.map(post =>
//         post.id === updatedPost.id ? updatedPost : post
//       );
//       useSocialStore.setState({ posts: updatedPosts });
//
//       // Update React Query cache
//       queryClient.setQueryData(socialQueryKeys.post(updatedPost.id), updatedPost);
//       queryClient.invalidateQueries({ queryKey: socialQueryKeys.posts() });
//
//       toast.success('Post updated successfully!');
//     },
//     onError: (error) => {
//       toast.error('Failed to update post.');
//       console.error('Update post error:', error);
//     },
//   });
// };

export const useDeletePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (postId: string) => socialApi.deletePost(postId),
    onSuccess: (_, postId) => {
      // Remove from Zustand store manually
      const { posts } = useSocialStore.getState();
      const filteredPosts = posts.filter(post => post.id !== postId);
      useSocialStore.setState({ posts: filteredPosts });
      
      // Update React Query cache
      queryClient.removeQueries({ queryKey: socialQueryKeys.post(postId) });
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.posts() });
      
      toast.success('Post deleted successfully!');
    },
    onError: (error) => {
      toast.error('Failed to delete post.');
      console.error('Delete post error:', error);
    },
  });
};

export const useLikePost = () => {
  const queryClient = useQueryClient();
  const { likePost } = useSocialStore();

  return useMutation({
    mutationFn: (postId: string) => socialApi.likePost(postId),
    onMutate: async (postId) => {
      // Optimistic update
      likePost(postId);
      
      // Update React Query cache optimistically
      queryClient.setQueryData(socialQueryKeys.post(postId), (old: Post | undefined) => {
        if (!old) return old;
        return {
          ...old,
          isLiked: !old.isLiked,
          likes: old.isLiked ? old.likes - 1 : old.likes + 1,
        };
      });
    },
    onError: (error, postId) => {
      // Revert optimistic update
      likePost(postId); // Toggle back
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.post(postId) });
      
      toast.error('Failed to like post.');
      console.error('Like post error:', error);
    },
  });
};

export const useBookmarkPost = () => {
  const queryClient = useQueryClient();
  const { bookmarkPost } = useSocialStore();

  return useMutation({
    mutationFn: (postId: string) => socialApi.bookmarkPost(postId),
    onMutate: async (postId) => {
      // Optimistic update
      bookmarkPost(postId);
      
      // Update React Query cache optimistically
      queryClient.setQueryData(socialQueryKeys.post(postId), (old: Post | undefined) => {
        if (!old) return old;
        return {
          ...old,
          isBookmarked: !old.isBookmarked,
        };
      });
    },
    onError: (error, postId) => {
      // Revert optimistic update
      bookmarkPost(postId); // Toggle back
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.post(postId) });
      
      toast.error('Failed to bookmark post.');
      console.error('Bookmark post error:', error);
    },
  });
};

// Comments Hooks
export const useComments = (postId: string) => {
  return useInfiniteQuery({
    queryKey: socialQueryKeys.comments(postId),
    queryFn: async ({ pageParam = 1 }) => {
      return await socialApi.getComments(postId, pageParam, 20);
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage: any) => {
      return lastPage.hasMore ? lastPage.nextPage : undefined;
    },
    enabled: !!postId,
  });
};

export const useCreateComment = () => {
  const queryClient = useQueryClient();
  const { addComment } = useSocialStore();

  return useMutation({
    mutationFn: ({ postId, data }: { postId: string; data: CreateCommentRequest }) => 
      socialApi.createComment(postId, data),
    onSuccess: (newComment, { postId }) => {
      // Update Zustand store
      addComment(postId, newComment.content);
      
      // Invalidate comments query
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.comments(postId) });
      
      // Update post comment count
      queryClient.setQueryData(socialQueryKeys.post(postId), (old: Post | undefined) => {
        if (!old) return old;
        return { ...old, commentsCount: old.commentsCount + 1 };
      });
      
      toast.success('Comment added successfully!');
    },
    onError: (error) => {
      toast.error('Failed to add comment.');
      console.error('Create comment error:', error);
    },
  });
};

// Users Hooks
export const useUserProfile = (userId: string) => {
  return useQuery({
    queryKey: socialQueryKeys.user(userId),
    queryFn: () => socialApi.getUserProfile(userId),
    enabled: !!userId,
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
};

export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => socialApi.updateProfile(data),
    onSuccess: (updatedProfile) => {
      // Update React Query cache
      queryClient.setQueryData(socialQueryKeys.user(updatedProfile.id), updatedProfile);
      
      toast.success('Profile updated successfully!');
    },
    onError: (error) => {
      toast.error('Failed to update profile.');
      console.error('Update profile error:', error);
    },
  });
};

export const useFollowUser = () => {
  const queryClient = useQueryClient();
  const { followUser } = useSocialStore();

  return useMutation({
    mutationFn: (userId: string) => socialApi.followUser(userId),
    onMutate: async (userId) => {
      // Optimistic update
      followUser(userId);
      
      // Update user profile cache
      queryClient.setQueryData(socialQueryKeys.user(userId), (old: UserProfile | undefined) => {
        if (!old) return old;
        return {
          ...old,
          isFollowing: true,
          followersCount: old.followersCount + 1,
        };
      });
    },
    onError: (error, userId) => {
      // Revert optimistic update
      const { unfollowUser } = useSocialStore.getState();
      unfollowUser(userId);
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.user(userId) });
      
      toast.error('Failed to follow user.');
      console.error('Follow user error:', error);
    },
  });
};

export const useUnfollowUser = () => {
  const queryClient = useQueryClient();
  const { unfollowUser } = useSocialStore();

  return useMutation({
    mutationFn: (userId: string) => socialApi.unfollowUser(userId),
    onMutate: async (userId) => {
      // Optimistic update
      unfollowUser(userId);
      
      // Update user profile cache
      queryClient.setQueryData(socialQueryKeys.user(userId), (old: UserProfile | undefined) => {
        if (!old) return old;
        return {
          ...old,
          isFollowing: false,
          followersCount: Math.max(0, old.followersCount - 1),
        };
      });
    },
    onError: (error, userId) => {
      // Revert optimistic update
      const { followUser } = useSocialStore.getState();
      followUser(userId);
      queryClient.invalidateQueries({ queryKey: socialQueryKeys.user(userId) });
      
      toast.error('Failed to unfollow user.');
      console.error('Unfollow user error:', error);
    },
  });
};

// Location Hooks
export const useLocationHeatmap = (filters?: {
  bounds?: { north: number; south: number; east: number; west: number };
  timeRange?: '24h' | '7d' | '30d' | 'all';
}) => {
  return useQuery({
    queryKey: socialQueryKeys.heatmap(filters),
    queryFn: () => socialApi.getLocationHeatmap(filters?.bounds, filters?.timeRange),
    staleTime: 15 * 60 * 1000, // 15 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });
};

// Utility Hooks
export const useInvalidateAllSocialQueries = () => {
  const queryClient = useQueryClient();
  
  return () => {
    queryClient.invalidateQueries({ queryKey: socialQueryKeys.all });
  };
};

export const usePrefetchUserProfile = () => {
  const queryClient = useQueryClient();
  
  return (userId: string) => {
    queryClient.prefetchQuery({
      queryKey: socialQueryKeys.user(userId),
      queryFn: () => socialApi.getUserProfile(userId),
      staleTime: 10 * 60 * 1000,
    });
  };
};