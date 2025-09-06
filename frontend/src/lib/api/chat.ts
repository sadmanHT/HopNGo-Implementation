import axios from 'axios';
import { ApiResponse, PaginatedResponse } from './types';

// Helper functions for W3C trace context
function generateTraceId(): string {
  // Generate a 32-character hex string (128-bit)
  return Array.from({ length: 32 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

function generateSpanId(): string {
  // Generate a 16-character hex string (64-bit)
  return Array.from({ length: 16 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

// Chat API types
export interface Conversation {
  id: string;
  participantIds: string[];
  participantNames: string[];
  lastMessage?: {
    id: string;
    content: string;
    senderId: string;
    senderName?: string;
    timestamp: string;
    type?: string;
  };
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: string;
  type: 'text' | 'image' | 'file';
}

export interface CreateConversationRequest {
  participantIds: string[];
  initialMessage?: string;
}

export interface SendMessageRequest {
  content: string;
  type?: 'text' | 'image' | 'file';
}

// Create axios instance for chat API
const chatApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_CHAT_SERVICE_URL || 'http://localhost:8085/api',
  timeout: 10000,
});

// Add auth interceptor and trace headers
chatApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  // Add W3C trace context headers for distributed tracing
  const traceId = generateTraceId();
  const spanId = generateSpanId();
  const traceparent = `00-${traceId}-${spanId}-01`;
  
  config.headers['traceparent'] = traceparent;
  config.headers['tracestate'] = '';
  
  return config;
});

// Add response interceptor for error handling
chatApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired, redirect to login
      localStorage.removeItem('auth_token');
      window.location.href = '/auth/login';
    }
    return Promise.reject(error);
  }
);

// Chat API functions
export const chatApiService = {
  // Get all conversations for the current user
  async getConversations(): Promise<Conversation[]> {
    const response = await chatApi.get<ApiResponse<Conversation[]>>('/conversations');
    return response.data.data;
  },

  // Get a specific conversation by ID
  async getConversation(id: string): Promise<Conversation> {
    const response = await chatApi.get<ApiResponse<Conversation>>(`/conversations/${id}`);
    return response.data.data;
  },

  // Create a new conversation
  async createConversation(data: CreateConversationRequest): Promise<Conversation> {
    const response = await chatApi.post<ApiResponse<Conversation>>('/conversations', data);
    return response.data.data;
  },

  // Get messages for a conversation with pagination
  async getMessages(
    conversationId: string,
    page: number = 1,
    limit: number = 50
  ): Promise<PaginatedResponse<Message>> {
    const response = await chatApi.get<ApiResponse<PaginatedResponse<Message>>>(
      `/conversations/${conversationId}/messages`,
      {
        params: { page, limit }
      }
    );
    return response.data.data;
  },

  // Send a message to a conversation
  async sendMessage(
    conversationId: string,
    data: SendMessageRequest
  ): Promise<Message> {
    const response = await chatApi.post<ApiResponse<Message>>(
      `/conversations/${conversationId}/messages`,
      data
    );
    return response.data.data;
  },

  // Mark messages as read
  async markAsRead(conversationId: string): Promise<void> {
    await chatApi.put(`/conversations/${conversationId}/read`);
  },

  // Search conversations
  async searchConversations(query: string): Promise<Conversation[]> {
    const response = await chatApi.get<ApiResponse<Conversation[]>>('/conversations/search', {
      params: { q: query }
    });
    return response.data.data;
  },

  // Get online users (for presence)
  async getOnlineUsers(): Promise<string[]> {
    const response = await chatApi.get<ApiResponse<string[]>>('/users/online');
    return response.data.data;
  },

  // Send typing indicator
  async sendTypingIndicator(conversationId: string, isTyping: boolean): Promise<void> {
    await chatApi.post(`/conversations/${conversationId}/typing`, { isTyping });
  }
};

export default chatApiService;