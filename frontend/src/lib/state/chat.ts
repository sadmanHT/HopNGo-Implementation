import { create } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';
import axios from 'axios';
import { ChatClient, createChatClient } from '../chat/client';

export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: string;
  type: 'text' | 'image' | 'file' | 'system';
}
import { chatApiService, type Conversation as ApiConversation, type Message as ApiMessage } from '../api/chat';

export interface Conversation {
  id: string;
  participantIds: string[];
  participantNames: string[];
  lastMessage?: {
    id: string;
    conversationId: string;
    senderId: string;
    senderName: string;
    content: string;
    timestamp: string;
    type: string;
  };
  lastActivity: string;
  unreadCount: number;
}

export interface TypingIndicator {
  conversationId: string;
  userId: string;
  userName: string;
  timestamp: number;
}

export interface ChatState {
  // Connection state
  client: ChatClient | null;
  connected: boolean;
  connecting: boolean;
  error: string | null;

  // Data
  conversations: Conversation[];
  messagesByConvo: Record<string, ChatMessage[]>;
  typingIndicators: TypingIndicator[];
  onlineUsers: string[];
  presenceInterval?: NodeJS.Timeout;

  // UI state
  activeConversationId: string | null;
  searchQuery: string;

  // Actions
  connect: () => Promise<void>;
  disconnect: () => void;
  fetchConversations: () => Promise<void>;
  fetchMessages: (conversationId: string) => Promise<void>;
  sendMessage: (conversationId: string, content: string) => Promise<void>;
  addMessage: (message: ChatMessage) => void;
  setActiveConversation: (conversationId: string | null) => void;
  setSearchQuery: (query: string) => void;
  addTypingIndicator: (indicator: TypingIndicator) => void;
  removeTypingIndicator: (conversationId: string, userId: string) => void;
  setOnlineUsers: (users: string[]) => void;
  startPresenceTracking: () => void;
  stopPresenceTracking: () => void;
  handleTokenRefresh: () => Promise<void>;
}

// Use the chat API service for REST calls

export const useChatStore = create<ChatState>((set, get) => ({
  // Initial state
  client: null,
  connected: false,
  connecting: false,
  error: null,
  conversations: [],
  messagesByConvo: {},
  typingIndicators: [],
  onlineUsers: [],
  activeConversationId: null,
  searchQuery: '',

  // Connection actions
  connect: async () => {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      set({ error: 'No auth token found' });
      return;
    }

    const client = createChatClient({
      url: process.env.NEXT_PUBLIC_CHAT_WS_URL || 'ws://localhost:8085/ws/chat',
      accessToken: token,
      onConnect: () => {
        set({ connected: true, connecting: false });
        get().fetchConversations();
      },
      onDisconnect: () => {
        set({ connected: false });
      },
      onMessage: (message) => {
        get().addMessage(message);
      },
      onError: (error) => {
        console.error('Chat client error:', error);
        
        // Handle token refresh requirement
        if (error.includes('Token refresh required') || error.includes('401') || error.includes('Unauthorized')) {
          get().handleTokenRefresh();
        } else {
          set({ connected: false, connecting: false, error: 'Connection error' });
        }
      },
      onReconnect: () => {
        set({ connecting: true });
      },
    });
    
    set({ client, connecting: true, error: null });
    
    try {
      await client.connect();
      get().startPresenceTracking();
    } catch (error) {
      console.error('Failed to connect to chat:', error);
      set({ connecting: false, error: 'Failed to connect to chat service' });
    }
  },

  disconnect: () => {
    const { client } = get();
    get().stopPresenceTracking();
    if (client) {
      client.disconnect();
    }
    set({ 
      connected: false, 
      connecting: false, 
      client: null,
      activeConversationId: null,
      typingIndicators: [],
    });
  },



  // Conversation actions
  fetchConversations: async () => {
    try {
      const apiConversations = await chatApiService.getConversations();
      const conversations: Conversation[] = apiConversations.map(conv => ({
        id: conv.id,
        participantIds: conv.participantIds,
        participantNames: conv.participantNames,
        lastMessage: conv.lastMessage ? {
           id: conv.lastMessage.id,
           conversationId: conv.id,
           senderId: conv.lastMessage.senderId,
           senderName: conv.lastMessage.senderName || 'Unknown',
           content: conv.lastMessage.content,
           timestamp: conv.lastMessage.timestamp,
           type: conv.lastMessage.type || 'text'
         } : undefined,
        lastActivity: conv.updatedAt,
        unreadCount: 0
      }));
      set({ conversations });
    } catch (error) {
      console.error('Failed to fetch conversations:', error);
    }
  },



  setActiveConversation: (conversationId: string | null) => {
    const { client, activeConversationId } = get();
    
    // Unsubscribe from previous conversation
    if (activeConversationId && client) {
      client.unsubscribeFromConversation(activeConversationId);
    }
    
    // Subscribe to new conversation
    if (conversationId && client?.isConnected()) {
      client.subscribeToConversation(conversationId);
    }
    
    set({ activeConversationId: conversationId });
  },

  setSearchQuery: (query: string) => {
    set({ searchQuery: query });
  },

  addTypingIndicator: (indicator: TypingIndicator) => {
    set(state => ({
      typingIndicators: [...state.typingIndicators.filter(
        t => t.userId !== indicator.userId
      ), indicator]
    }));
  },

  removeTypingIndicator: (conversationId: string, userId: string) => {
    set(state => ({
      typingIndicators: state.typingIndicators.filter(
        t => t.userId !== userId
      )
    }));
  },

  setOnlineUsers: (users: string[]) => {
    set({ onlineUsers: users });
  },

  // Presence simulation - fetch online users periodically
  startPresenceTracking: () => {
    const fetchOnlineUsers = async () => {
      try {
        const users = await chatApiService.getOnlineUsers();
        get().setOnlineUsers(users);
      } catch (error) {
        console.error('Failed to fetch online users:', error);
      }
    };

    // Fetch immediately
    fetchOnlineUsers();

    // Set up periodic fetching every 30 seconds
    const interval = setInterval(fetchOnlineUsers, 30000);
    
    // Store interval ID for cleanup
    set({ presenceInterval: interval });
  },

  stopPresenceTracking: () => {
    const state = get();
    if (state.presenceInterval) {
      clearInterval(state.presenceInterval);
      set({ presenceInterval: undefined });
    }
  },

  // Token refresh handling
  handleTokenRefresh: async () => {
    try {
      console.log('Attempting to refresh authentication token');
      
      // Try to get a fresh token from localStorage or refresh endpoint
      const refreshToken = localStorage.getItem('refresh_token');
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }
      
      // Call refresh endpoint (this would be implemented based on your auth service)
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      });
      
      if (!response.ok) {
        throw new Error('Token refresh failed');
      }
      
      const { accessToken } = await response.json();
      localStorage.setItem('auth_token', accessToken);
      
      // Update the chat client with new token and reconnect
      const { client } = get();
      if (client) {
        client.updateAccessToken(accessToken);
        await client.connect();
      }
      
      set({ error: null });
    } catch (error) {
      console.error('Token refresh failed:', error);
      // Redirect to login if refresh fails
      localStorage.removeItem('auth_token');
      localStorage.removeItem('refresh_token');
      window.location.href = '/auth/login';
    }
  },

  // Message actions
  fetchMessages: async (conversationId: string) => {
    try {
      const response = await chatApiService.getMessages(conversationId, 1, 50);
      const messages: ChatMessage[] = response.data.map(msg => ({
        id: msg.id,
        conversationId: msg.conversationId,
        senderId: msg.senderId,
        senderName: msg.senderName,
        content: msg.content,
        timestamp: msg.timestamp,
        type: msg.type
      }));
      
      set((state) => ({
        messagesByConvo: {
          ...state.messagesByConvo,
          [conversationId]: messages,
        },
      }));
    } catch (error) {
      console.error('Failed to fetch messages:', error);
    }
  },

  sendMessage: async (conversationId: string, content: string) => {
    const { client } = get();
    if (client?.isConnected()) {
      client.sendMessage(conversationId, content);
    }
  },

  addMessage: (message: ChatMessage) => {
    set((state) => {
      const messages = state.messagesByConvo[message.conversationId] || [];
      
      // Check if message already exists (avoid duplicates)
      const existingIndex = messages.findIndex(m => m.id === message.id);
      if (existingIndex !== -1) {
        return state; // Message already exists
      }
      
      // Remove optimistic message if this is the real one
      const filteredMessages = messages.filter(m => !m.id.startsWith('temp_'));
      
      const updatedMessages = [...filteredMessages, message].sort(
        (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );
      
      // Update conversation's last message
      const updatedConversations = state.conversations.map(conv => 
        conv.id === message.conversationId 
          ? { ...conv, lastMessage: message, lastActivity: message.timestamp }
          : conv
      );
      
      return {
        ...state,
        messagesByConvo: {
          ...state.messagesByConvo,
          [message.conversationId]: updatedMessages,
        },
        conversations: updatedConversations,
      };
    });
  },


}));