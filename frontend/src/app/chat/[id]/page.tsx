'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useChatStore } from '@/lib/state/chat';
import { useAuthStore } from '@/lib/state';
import { 
  ArrowLeft, 
  Send, 
  Users, 
  Wifi, 
  WifiOff,
  MoreVertical 
} from 'lucide-react';
import { formatDistanceToNow, format, isToday, isYesterday } from 'date-fns';

interface ChatMessageProps {
  message: any;
  isOwn: boolean;
  showAvatar: boolean;
}

function ChatMessage({ message, isOwn, showAvatar }: ChatMessageProps) {
  const formatMessageTime = (timestamp: string) => {
    const date = new Date(timestamp);
    if (isToday(date)) {
      return format(date, 'HH:mm');
    } else if (isYesterday(date)) {
      return `Yesterday ${format(date, 'HH:mm')}`;
    } else {
      return format(date, 'MMM d, HH:mm');
    }
  };

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`flex ${isOwn ? 'flex-row-reverse' : 'flex-row'} items-end space-x-2 max-w-xs lg:max-w-md`}>
        {showAvatar && !isOwn && (
          <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-sm font-medium">
            {message.senderName.charAt(0).toUpperCase()}
          </div>
        )}
        
        <div className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'}`}>
          {showAvatar && (
            <span className="text-xs text-gray-500 mb-1 px-2">
              {isOwn ? 'You' : message.senderName}
            </span>
          )}
          
          <div
            className={`px-4 py-2 rounded-lg ${
              isOwn
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-900'
            } ${message.id.startsWith('temp_') ? 'opacity-70' : ''}`}
          >
            <p className="text-sm">{message.content}</p>
            <p className={`text-xs mt-1 ${
              isOwn ? 'text-blue-100' : 'text-gray-500'
            }`}>
              {formatMessageTime(message.timestamp)}
            </p>
          </div>
        </div>
        
        {showAvatar && isOwn && (
          <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-sm font-medium text-white">
            {message.senderName.charAt(0).toUpperCase()}
          </div>
        )}
      </div>
    </div>
  );
}

function TypingIndicator({ typingUsers }: { typingUsers: string[] }) {
  if (typingUsers.length === 0) return null;

  return (
    <div className="flex justify-start mb-4">
      <div className="flex items-center space-x-2">
        <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-sm font-medium">
          {typingUsers[0].charAt(0).toUpperCase()}
        </div>
        <div className="bg-gray-100 rounded-lg px-4 py-2">
          <div className="flex space-x-1">
            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ChatConversationPage() {
  const router = useRouter();
  const params = useParams();
  const conversationId = params.id as string;
  
  const { user, token } = useAuthStore();
  const {
    conversations,
    messagesByConvo,
    typingIndicators,
    connected,
    connecting,
    activeConversationId,
    onlineUsers,
    connect,
    setActiveConversation,
    fetchMessages,
    sendMessage,
    addMessage,
  } = useChatStore();
  
  const [messageInput, setMessageInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  
  const conversation = conversations.find(c => c.id === conversationId);
  const messages = messagesByConvo[conversationId] || [];
  const typingUsers = typingIndicators
    .filter(indicator => indicator.conversationId === conversationId && indicator.userId !== user?.id)
    .map(indicator => indicator.userName);

  useEffect(() => {
    // Redirect to login if not authenticated
    if (!user || !token) {
      router.push('/login');
      return;
    }

    // Connect to chat service
    if (!connected && !connecting) {
      connect();
    }

    // Set active conversation and fetch messages
    setActiveConversation(conversationId);
    fetchMessages(conversationId);

    return () => {
      // Clean up when leaving the conversation
      setActiveConversation(null);
    };
  }, [conversationId, user, token, connected, connecting, connect, setActiveConversation, fetchMessages, router]);

  useEffect(() => {
    // Scroll to bottom when new messages arrive
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, typingIndicators]);

  const handleSendMessage = () => {
    if (!messageInput.trim() || !connected) return;
    
    sendMessage(conversationId, messageInput.trim());
    setMessageInput('');
    setIsTyping(false);
    
    // Clear typing indicator
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = null;
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setMessageInput(value);
    
    // Handle typing indicators
    if (value.trim() && !isTyping) {
      setIsTyping(true);
      // Send typing indicator via WebSocket
      if (connected && user) {
        useChatStore.getState().client?.sendTypingIndicator?.(conversationId, true);
      }
    }
    
    // Clear existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    
    // Set new timeout to stop typing
    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false);
      // Send stop typing indicator
      if (connected && user) {
        useChatStore.getState().client?.sendTypingIndicator?.(conversationId, false);
      }
    }, 1000);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-xl font-semibold mb-2">Please log in to access chat</h2>
          <Button asChild>
            <Link href="/login">Go to Login</Link>
          </Button>
        </div>
      </div>
    );
  }

  if (!conversation) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-xl font-semibold mb-2">Conversation not found</h2>
          <Button asChild>
            <Link href="/chat">Back to Chat</Link>
          </Button>
        </div>
      </div>
    );
  }

  const otherParticipants = conversation.participantNames.filter(name => name !== `${user.firstName} ${user.lastName}`);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-4 flex-shrink-0">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => router.push('/chat')}
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
            
            <div className="flex items-center space-x-2">
              <Users className="h-5 w-5 text-gray-400" />
              <div className="flex-1">
                <h1 className="font-semibold text-gray-900">
                  {otherParticipants.join(', ') || 'You'}
                </h1>
                <p className="text-sm text-gray-500">
                  {conversation.participantIds.length} participant{conversation.participantIds.length !== 1 ? 's' : ''}
                </p>
              </div>
              <div className="flex items-center space-x-2">
                <div className="flex items-center space-x-1">
                  <div className={`w-2 h-2 rounded-full ${
                    conversation.participantIds.some(id => onlineUsers.includes(id)) 
                      ? 'bg-green-500' 
                      : 'bg-gray-400'
                  }`} />
                  <span className="text-xs text-gray-500">
                    {conversation.participantIds.filter(id => onlineUsers.includes(id)).length} online
                  </span>
                </div>
              </div>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            {/* Connection Status */}
            {connected ? (
              <div className="flex items-center space-x-1 text-green-600">
                <Wifi className="h-4 w-4" />
                <span className="text-sm font-medium hidden sm:inline">Connected</span>
              </div>
            ) : connecting ? (
              <div className="flex items-center space-x-1 text-yellow-600">
                <div className="animate-spin h-4 w-4 border-2 border-current border-t-transparent rounded-full" />
                <span className="text-sm font-medium hidden sm:inline">Connecting...</span>
              </div>
            ) : (
              <div className="flex items-center space-x-1 text-red-600">
                <WifiOff className="h-4 w-4" />
                <span className="text-sm font-medium hidden sm:inline">Disconnected</span>
              </div>
            )}
            
            <Button variant="ghost" size="icon">
              <MoreVertical className="h-5 w-5" />
            </Button>
          </div>
        </div>
      </div>

      {/* Messages Area */}
      <div className="flex-1 overflow-hidden">
        <div className="max-w-4xl mx-auto h-full flex flex-col">
          <div className="flex-1 overflow-y-auto px-4 py-6">
            {messages.length === 0 ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">
                    Start the conversation
                  </h3>
                  <p className="text-gray-500">
                    Send a message to begin chatting with {otherParticipants.join(', ')}
                  </p>
                </div>
              </div>
            ) : (
              <div>
                {messages.map((message, index) => {
                  const isOwn = message.senderId === user.id || message.senderName === 'You';
                  const prevMessage = messages[index - 1];
                  const showAvatar = !prevMessage || 
                    prevMessage.senderId !== message.senderId ||
                    new Date(message.timestamp).getTime() - new Date(prevMessage.timestamp).getTime() > 300000; // 5 minutes
                  
                  return (
                    <ChatMessage
                      key={message.id}
                      message={message}
                      isOwn={isOwn}
                      showAvatar={showAvatar}
                    />
                  );
                })}
                
                <TypingIndicator typingUsers={typingUsers} />
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Message Input */}
      <div className="bg-white border-t border-gray-200 px-4 py-4 flex-shrink-0">
        <div className="max-w-4xl mx-auto">
          <div className="flex items-end space-x-2">
            <div className="flex-1">
              <Input
                type="text"
                placeholder={connected ? "Type a message..." : "Connecting..."}
                value={messageInput}
                onChange={handleInputChange}
                onKeyPress={handleKeyPress}
                disabled={!connected}
                className="resize-none"
              />
            </div>
            <Button
              onClick={handleSendMessage}
              disabled={!messageInput.trim() || !connected}
              size="icon"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
          
          {typingUsers.length > 0 && (
            <p className="text-xs text-gray-500 mt-2">
              {typingUsers.length === 1 
                ? `${typingUsers[0]} is typing...`
                : `${typingUsers.slice(0, -1).join(', ')} and ${typingUsers[typingUsers.length - 1]} are typing...`
              }
            </p>
          )}
        </div>
      </div>
    </div>
  );
}