import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { socialApi, CreatePostRequest, PaginatedResponse } from '../../services/social';

// Define and export types
interface Post {
  id: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  content?: string;
  mediaUrls?: string[];
  location?: {
    name: string;
    coordinates: [number, number];
    address?: string;
  };
  tags?: string[];
  likes: number;
  commentsCount: number;
  isLiked?: boolean;
  isBookmarked?: boolean;
  createdAt: string;
  updatedAt: string;
}

export type { Post };
export type { Comment, UserProfile };

interface Comment {
  id: string;
  postId: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  content: string;
  createdAt: string;
}

interface UserProfile {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  bio?: string;
  followersCount: number;
  followingCount: number;
  postsCount: number;
  isFollowing?: boolean;
}

interface SocialState {
  // Posts state
  posts: Post[];
  currentPost: Post | null;
  isLoadingPosts: boolean;
  isCreatingPost: boolean;
  feedPage: number;
  hasMorePosts: boolean;
  
  // User interactions
  likedPosts: Set<string>;
  bookmarkedPosts: Set<string>;
  
  // Comments
  comments: Record<string, Comment[]>;
  isLoadingComments: Record<string, boolean>;
  
  // Profile
  userProfile: UserProfile | null;
  isLoadingProfile: boolean;
  
  // Actions
  createPost: (postData: CreatePostRequest) => Promise<void>;
  loadFeed: (refresh?: boolean) => Promise<void>;
  loadMorePosts: () => Promise<void>;
  likePost: (postId: string) => Promise<void>;
  unlikePost: (postId: string) => Promise<void>;
  bookmarkPost: (postId: string) => Promise<void>;
  unbookmarkPost: (postId: string) => Promise<void>;
  addComment: (postId: string, content: string) => Promise<void>;
  loadComments: (postId: string) => Promise<void>;
  loadUserProfile: (userId: string) => Promise<void>;
  followUser: (userId: string) => Promise<void>;
  unfollowUser: (userId: string) => Promise<void>;
  clearSocialData: () => void;
  setCurrentPost: (post: Post | null) => void;
}



export const useSocialStore = create<SocialState>()(
  persist(
    (set, get) => ({
      // Initial state
      posts: [],
      currentPost: null,
      isLoadingPosts: false,
      isCreatingPost: false,
      feedPage: 0,
      hasMorePosts: true,
      likedPosts: new Set(),
      bookmarkedPosts: new Set(),
      comments: {},
      isLoadingComments: {},
      userProfile: null,
      isLoadingProfile: false,

      // Actions
      createPost: async (postData: CreatePostRequest) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) throw new Error('User not authenticated');

        set({ isCreatingPost: true });
        try {
          const newPost = await socialApi.createPost(postData);
          set((state) => ({
            posts: [newPost, ...state.posts],
            isCreatingPost: false,
          }));
        } catch (error) {
          set({ isCreatingPost: false });
          throw error;
        }
      },

      loadFeed: async (refresh = false) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) return;

        set({ isLoadingPosts: true });
        try {
          const page = refresh ? 0 : get().feedPage;
          const response = await socialApi.getFeed(page, 20);
          
          set((state) => ({
            posts: refresh ? response.items : [...state.posts, ...response.items],
            feedPage: page,
            hasMorePosts: response.hasMore,
            isLoadingPosts: false,
          }));
        } catch (error) {
          set({ isLoadingPosts: false });
          console.error('Failed to load feed:', error);
        }
      },

      loadMorePosts: async () => {
        const { hasMorePosts, isLoadingPosts, feedPage } = get();
        if (!hasMorePosts || isLoadingPosts) return;

        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) return;

        set({ isLoadingPosts: true });
        try {
          const nextPage = feedPage + 1;
          const response = await socialApi.getFeed(nextPage, 20);
          
          set((state) => ({
            posts: [...state.posts, ...response.items],
            feedPage: nextPage,
            hasMorePosts: response.hasMore,
            isLoadingPosts: false,
          }));
        } catch (error) {
          set({ isLoadingPosts: false });
          console.error('Failed to load more posts:', error);
        }
      },

      likePost: async (postId: string) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) return;

        // Optimistic update
        set((state) => ({
          likedPosts: new Set([...state.likedPosts, postId]),
          posts: state.posts.map(post => 
            post.id === postId ? { ...post, likes: post.likes + 1 } : post
          ),
        }));

        try {
          // TODO: Implement API call for liking post
          // await socialApi.likePost(postId);
        } catch (error) {
          // Revert optimistic update on error
          set((state) => {
            const newLikedPosts = new Set(state.likedPosts);
            newLikedPosts.delete(postId);
            return {
              likedPosts: newLikedPosts,
              posts: state.posts.map(post => 
                post.id === postId ? { ...post, likes: post.likes - 1 } : post
              ),
            };
          });
          console.error('Failed to like post:', error);
        }
      },

      unlikePost: async (postId: string) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) return;

        // Optimistic update
        set((state) => {
          const newLikedPosts = new Set(state.likedPosts);
          newLikedPosts.delete(postId);
          return {
            likedPosts: newLikedPosts,
            posts: state.posts.map(post => 
              post.id === postId ? { ...post, likes: Math.max(0, post.likes - 1) } : post
            ),
          };
        });

        try {
          // TODO: Implement API call for unliking post
          // await socialApi.unlikePost(postId);
        } catch (error) {
          // Revert optimistic update on error
          set((state) => ({
            likedPosts: new Set([...state.likedPosts, postId]),
            posts: state.posts.map(post => 
              post.id === postId ? { ...post, likes: post.likes + 1 } : post
            ),
          }));
          console.error('Failed to unlike post:', error);
        }
      },

      bookmarkPost: async (postId: string) => {
        set((state) => ({
          bookmarkedPosts: new Set([...state.bookmarkedPosts, postId]),
        }));
        // TODO: Implement API call for bookmarking
      },

      unbookmarkPost: async (postId: string) => {
        set((state) => {
          const newBookmarkedPosts = new Set(state.bookmarkedPosts);
          newBookmarkedPosts.delete(postId);
          return { bookmarkedPosts: newBookmarkedPosts };
        });
        // TODO: Implement API call for unbookmarking
      },

      addComment: async (postId: string, content: string) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) return;

        const newComment: Comment = {
          id: Date.now().toString(), // Temporary ID
          postId,
          userId: user.id,
          userName: user.name,
          userAvatar: user.avatar,
          content,
          createdAt: new Date().toISOString(),
        };

        // Optimistic update
        set((state) => ({
          comments: {
            ...state.comments,
            [postId]: [...(state.comments[postId] || []), newComment],
          },
          posts: state.posts.map(post => 
            post.id === postId ? { ...post, commentsCount: post.commentsCount + 1 } : post
          ),
        }));

        try {
          // TODO: Implement API call for adding comment
          // const savedComment = await socialApi.addComment(postId, { content });
          // Update with real comment data
        } catch (error) {
          console.error('Failed to add comment:', error);
        }
      },

      loadComments: async (postId: string) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!user || !token) return;

        set((state) => ({
          isLoadingComments: { ...state.isLoadingComments, [postId]: true },
        }));

        try {
          // TODO: Implement API call for loading comments
          // const comments = await socialApi.getComments(postId);
          // For now, use empty array
          const comments: Comment[] = [];
          
          set((state) => ({
            comments: { ...state.comments, [postId]: comments },
            isLoadingComments: { ...state.isLoadingComments, [postId]: false },
          }));
        } catch (error) {
          set((state) => ({
            isLoadingComments: { ...state.isLoadingComments, [postId]: false },
          }));
          console.error('Failed to load comments:', error);
        }
      },

      loadUserProfile: async (userId: string) => {
        const { user, token } = (await import('./auth')).useAuthStore.getState();
        if (!token) return;

        set({ isLoadingProfile: true });
        try {
          // TODO: Implement API call for user profile
          // const profile = await socialApi.getUserProfile(userId);
          // For now, use mock data
          const profile: UserProfile = {
            id: userId,
            name: 'User Name',
            email: 'user@example.com',
            followersCount: 0,
            followingCount: 0,
            postsCount: 0,
          };
          
          set({ userProfile: profile, isLoadingProfile: false });
        } catch (error) {
          set({ isLoadingProfile: false });
          console.error('Failed to load user profile:', error);
        }
      },

      followUser: async (userId: string) => {
        // TODO: Implement follow functionality
        set((state) => ({
          userProfile: state.userProfile ? {
            ...state.userProfile,
            isFollowing: true,
            followersCount: state.userProfile.followersCount + 1,
          } : null,
        }));
      },

      unfollowUser: async (userId: string) => {
        // TODO: Implement unfollow functionality
        set((state) => ({
          userProfile: state.userProfile ? {
            ...state.userProfile,
            isFollowing: false,
            followersCount: Math.max(0, state.userProfile.followersCount - 1),
          } : null,
        }));
      },

      setCurrentPost: (post: Post | null) => {
        set({ currentPost: post });
      },

      clearSocialData: () => {
        set({
          posts: [],
          currentPost: null,
          feedPage: 0,
          hasMorePosts: true,
          likedPosts: new Set(),
          bookmarkedPosts: new Set(),
          comments: {},
          userProfile: null,
        });
      },
    }),
    {
      name: 'social-storage',
      partialize: (state) => ({
        likedPosts: Array.from(state.likedPosts),
        bookmarkedPosts: Array.from(state.bookmarkedPosts),
      }),
      onRehydrateStorage: () => (state) => {
        if (state) {
          // Convert arrays back to Sets after rehydration
          state.likedPosts = new Set(state.likedPosts as any);
          state.bookmarkedPosts = new Set(state.bookmarkedPosts as any);
        }
      },
    }
  )
);