'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useChatStore } from '@/lib/state/chat';
import { useAuthStore } from '@/lib/state';
import { MessageCircle, Plus, Search, Users, Wifi, WifiOff } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

export default function ChatPage() {
  const router = useRouter();
  const { user, token } = useAuthStore();
  const {
    conversations,
    connected,
    connecting,
    connect,
    fetchConversations,
    setActiveConversation,
  } = useChatStore();
  
  const [searchQuery, setSearchQuery] = useState('');
  const [showNewChatDialog, setShowNewChatDialog] = useState(false);

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

    // Fetch conversations
    fetchConversations();
  }, [user, token, connected, connecting, connect, fetchConversations, router]);

  const filteredConversations = conversations.filter(conv =>
    conv.participantNames.some(name => 
      name.toLowerCase().includes(searchQuery.toLowerCase())
    )
  );

  const handleConversationClick = (conversationId: string) => {
    setActiveConversation(conversationId);
    router.push(`/chat/${conversationId}`);
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

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-4">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <MessageCircle className="h-8 w-8 text-blue-600" />
            <h1 className="text-2xl font-bold text-gray-900">Chat</h1>
          </div>
          
          {/* Connection Status */}
          <div className="flex items-center space-x-2">
            {connected ? (
              <div className="flex items-center space-x-1 text-green-600">
                <Wifi className="h-4 w-4" />
                <span className="text-sm font-medium">Connected</span>
              </div>
            ) : connecting ? (
              <div className="flex items-center space-x-1 text-yellow-600">
                <div className="animate-spin h-4 w-4 border-2 border-current border-t-transparent rounded-full" />
                <span className="text-sm font-medium">Connecting...</span>
              </div>
            ) : (
              <div className="flex items-center space-x-1 text-red-600">
                <WifiOff className="h-4 w-4" />
                <span className="text-sm font-medium">Disconnected</span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 py-6">
        {/* Search and New Chat */}
        <div className="flex items-center space-x-4 mb-6">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              type="text"
              placeholder="Search conversations..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
          <Button
            onClick={() => setShowNewChatDialog(true)}
            className="flex items-center space-x-2"
          >
            <Plus className="h-4 w-4" />
            <span>New Chat</span>
          </Button>
        </div>

        {/* Conversations List */}
        <div className="space-y-3">
          {filteredConversations.length === 0 ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12">
                <MessageCircle className="h-12 w-12 text-gray-400 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  {searchQuery ? 'No conversations found' : 'No conversations yet'}
                </h3>
                <p className="text-gray-500 text-center mb-4">
                  {searchQuery 
                    ? 'Try adjusting your search terms'
                    : 'Start a new conversation to begin chatting'
                  }
                </p>
                {!searchQuery && (
                  <Button onClick={() => setShowNewChatDialog(true)}>
                    Start New Chat
                  </Button>
                )}
              </CardContent>
            </Card>
          ) : (
            filteredConversations.map((conversation) => (
              <Card
                key={conversation.id}
                className="cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => handleConversationClick(conversation.id)}
              >
                <CardContent className="p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-1">
                        <Users className="h-4 w-4 text-gray-400" />
                        <h3 className="font-medium text-gray-900">
                          {conversation.participantNames
                            .filter(name => name !== `${user.firstName} ${user.lastName}`)
                            .join(', ') || 'You'}
                        </h3>
                      </div>
                      
                      {conversation.lastMessage && (
                        <p className="text-sm text-gray-600 truncate">
                          <span className="font-medium">
                            {conversation.lastMessage.senderName}:
                          </span>{' '}
                          {conversation.lastMessage.content}
                        </p>
                      )}
                      
                      <p className="text-xs text-gray-400 mt-1">
                        {conversation.lastActivity && 
                          formatDistanceToNow(new Date(conversation.lastActivity), { addSuffix: true })
                        }
                      </p>
                    </div>
                    
                    {conversation.unreadCount > 0 && (
                      <div className="bg-blue-600 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                        {conversation.unreadCount}
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      </div>

      {/* New Chat Dialog - Simple version for now */}
      {showNewChatDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Start New Conversation</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-gray-600">
                New conversation feature will be implemented soon. For now, conversations are created automatically when messages are sent.
              </p>
              <div className="flex justify-end space-x-2">
                <Button
                  variant="outline"
                  onClick={() => setShowNewChatDialog(false)}
                >
                  Close
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}