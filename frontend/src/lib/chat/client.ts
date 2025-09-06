import { Client, IMessage, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: string;
  type: 'text' | 'system';
}

export interface ChatClientConfig {
  url: string;
  accessToken: string;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onMessage?: (message: ChatMessage) => void;
  onError?: (error: any) => void;
  onReconnect?: () => void;
}

export class ChatClient {
  private client: Client;
  private config: ChatClientConfig;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000; // Start with 1 second
  private maxReconnectDelay = 30000; // Max 30 seconds
  private subscriptions = new Map<string, any>();
  private isConnecting = false;
  private reconnectTimer?: NodeJS.Timeout;

  constructor(config: ChatClientConfig) {
    this.config = config;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
    this.maxReconnectDelay = 30000;
    this.client = new Client(this.getStompConfig());
  }

  private getStompConfig(): StompConfig {
    return {
      webSocketFactory: () => new SockJS(this.config.url),
      connectHeaders: {
        Authorization: `Bearer ${this.config.accessToken}`,
      },
      debug: (str) => {
        if (process.env.NODE_ENV === 'development') {
          console.log('STOMP Debug:', str);
        }
      },
      reconnectDelay: 0, // We handle reconnection manually
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        console.log('Connected to chat service:', frame);
        this.reconnectAttempts = 0; // Reset on successful connection
        this.reconnectDelay = 1000; // Reset delay
        this.isConnecting = false;
        this.config.onConnect?.();
      },
      onDisconnect: (frame) => {
        console.log('Disconnected from chat service:', frame);
        this.isConnecting = false;
        this.config.onDisconnect?.();
        this.handleReconnection();
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        const errorMessage = frame.headers.message;
        
        // Check if it's an authentication error
        if (errorMessage?.includes('401') || errorMessage?.includes('Unauthorized')) {
          this.handleTokenRefresh();
        } else {
          this.config.onError?.(`STOMP error: ${errorMessage}`);
          this.handleReconnection();
        }
        this.isConnecting = false;
      },
      onWebSocketError: (error) => {
        console.error('WebSocket error:', error);
        this.config.onError?.('WebSocket connection error');
        this.isConnecting = false;
        this.handleReconnection();
      },
    };
  }

  private handleReconnection() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts || this.isConnecting) {
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1), this.maxReconnectDelay);
    
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
    
    this.reconnectTimer = setTimeout(() => {
      if (!this.client.connected && !this.isConnecting) {
        this.config.onReconnect?.();
        this.connect();
      }
    }, delay);
  }

  private handleTokenRefresh() {
    console.log('Authentication error detected, attempting token refresh');
    // Clear any pending reconnection
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
    
    // Notify the application that token refresh is needed
    this.config.onError?.('Token refresh required');
  }

  connect(): void {
    if (this.client.connected || this.isConnecting) {
      return;
    }

    this.isConnecting = true;
    this.client.activate();
  }

  disconnect(): void {
    // Clear any pending reconnection timer
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
    
    this.reconnectAttempts = this.maxReconnectAttempts; // Prevent reconnection
    this.subscriptions.clear();
    this.client.deactivate();
  }

  updateAccessToken(accessToken: string): void {
    this.config.accessToken = accessToken;
    
    // Update the client configuration with new token
    this.client = new Client(this.getStompConfig());
    
    // Reconnect with new token if currently connected
    if (this.client.connected) {
      this.disconnect();
      setTimeout(() => this.connect(), 100); // Small delay to ensure clean disconnect
    }
  }

  subscribeToConversation(conversationId: string): void {
    if (!this.client.connected) {
      console.warn('Cannot subscribe: client not connected');
      return;
    }

    const destination = `/topic/conversations.${conversationId}`;
    
    if (this.subscriptions.has(destination)) {
      return; // Already subscribed
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.config.onMessage?.(chatMessage);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
    console.log(`Subscribed to conversation: ${conversationId}`);
  }

  unsubscribeFromConversation(conversationId: string): void {
    const destination = `/topic/conversations.${conversationId}`;
    const subscription = this.subscriptions.get(destination);
    
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log(`Unsubscribed from conversation: ${conversationId}`);
    }
  }

  sendMessage(conversationId: string, content: string): void {
    if (!this.client.connected) {
      console.warn('Cannot send message: client not connected');
      return;
    }

    const message = {
      conversationId,
      content,
      timestamp: new Date().toISOString(),
    };

    this.client.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify(message),
    });
  }

  sendTypingIndicator(conversationId: string, isTyping: boolean): void {
    if (!this.client.connected) {
      console.warn('Cannot send typing indicator: client not connected');
      return;
    }

    const indicator = {
      conversationId,
      isTyping,
      timestamp: new Date().toISOString(),
    };

    this.client.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify(indicator),
    });
  }

  createConversation(participantIds: string[]): void {
    if (!this.client.connected) {
      console.warn('Cannot create conversation: client not connected');
      return;
    }

    const request = {
      participantIds,
      timestamp: new Date().toISOString(),
    };

    this.client.publish({
      destination: '/app/chat.createConversation',
      body: JSON.stringify(request),
    });
  }

  isConnected(): boolean {
    return this.client.connected;
  }

  getConnectionState(): 'connected' | 'connecting' | 'disconnected' {
    if (this.client.connected) return 'connected';
    if (this.isConnecting) return 'connecting';
    return 'disconnected';
  }
}

// Singleton instance
let chatClientInstance: ChatClient | null = null;

export const createChatClient = (config: ChatClientConfig): ChatClient => {
  if (chatClientInstance) {
    chatClientInstance.disconnect();
  }
  chatClientInstance = new ChatClient(config);
  return chatClientInstance;
};

export const getChatClient = (): ChatClient | null => {
  return chatClientInstance;
};