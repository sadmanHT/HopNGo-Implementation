import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween, randomItem, randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const connectionTime = new Trend('connection_time');
const messageLatency = new Trend('message_latency');
const messagesReceived = new Counter('messages_received');
const messagesSent = new Counter('messages_sent');
const connectionDrops = new Counter('connection_drops');
const reconnections = new Counter('reconnections');
const chatRoomJoins = new Counter('chat_room_joins');
const typingEvents = new Counter('typing_events');

// Test configuration
export const options = {
  scenarios: {
    // Normal chat activity
    normal_chat: {
      executor: 'constant-vus',
      vus: 100,
      duration: '10m',
    },
    // Peak chat activity (events, group chats)
    peak_chat: {
      executor: 'ramping-vus',
      startVUs: 50,
      stages: [
        { duration: '1m', target: 100 },
        { duration: '2m', target: 300 },
        { duration: '5m', target: 500 }, // Peak concurrent users
        { duration: '3m', target: 800 }, // Event spike
        { duration: '2m', target: 400 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 0 },
      ],
    },
    // Connection stress test
    connection_stress: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 }, // Rapid connections
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    'errors': ['rate<0.01'], // Less than 1% error rate
    'connection_time': ['p(95)<2000'], // Connection under 2s
    'message_latency': ['p(95)<100'], // Message delivery under 100ms
    'message_latency': ['avg<50'], // Average message latency under 50ms
    'ws_connecting': ['avg<1000'], // WebSocket connection time
    'ws_msgs_received': ['count>0'], // Should receive messages
  },
};

// Test data
const chatRoomTypes = ['public', 'private', 'group', 'event', 'support'];
const messageTypes = ['text', 'emoji', 'image', 'location', 'booking_share', 'recommendation'];
const userRoles = ['traveler', 'local_guide', 'business_owner', 'admin', 'moderator'];

const sampleMessages = [
  'Hey! How\'s your trip going?',
  'Just booked an amazing restaurant!',
  'Anyone been to the local market?',
  'Thanks for the recommendation! 🙏',
  'The sunset view here is incredible',
  'Looking for travel buddies for tomorrow',
  'Just shared my itinerary with you',
  'This place is a hidden gem!',
  'Weather looks perfect for hiking',
  'Great meeting you today!',
  'Check out this amazing food spot',
  'Anyone up for exploring the old town?',
  'Just finished the walking tour - highly recommend!',
  'The local guide was fantastic',
  'Booking confirmed! See you there 🎉',
];

const emojiReactions = ['👍', '❤️', '😍', '🔥', '👏', '🙌', '😂', '🤩', '✨', '🌟'];

const userProfiles = [
  { id: 'user_001', name: 'Alex', role: 'traveler', location: 'New York' },
  { id: 'user_002', name: 'Maria', role: 'local_guide', location: 'Barcelona' },
  { id: 'user_003', name: 'John', role: 'business_owner', location: 'Tokyo' },
  { id: 'user_004', name: 'Sophie', role: 'traveler', location: 'Paris' },
  { id: 'user_005', name: 'Carlos', role: 'local_guide', location: 'Mexico City' },
  { id: 'user_006', name: 'Emma', role: 'traveler', location: 'London' },
  { id: 'user_007', name: 'Raj', role: 'business_owner', location: 'Mumbai' },
  { id: 'user_008', name: 'Lisa', role: 'moderator', location: 'San Francisco' },
];

function generateChatRoom() {
  const roomType = randomItem(chatRoomTypes);
  const roomId = `room_${randomString(8)}`;
  
  return {
    id: roomId,
    type: roomType,
    name: `${roomType}_${randomString(4)}`,
    maxUsers: roomType === 'private' ? 2 : randomIntBetween(5, 50),
    topic: roomType === 'event' ? 'Food Festival Discussion' : 
           roomType === 'support' ? 'Customer Support' : 
           'General Chat',
  };
}

function generateMessage(user, roomId) {
  const messageType = randomItem(messageTypes);
  
  let content;
  switch (messageType) {
    case 'text':
      content = {
        text: randomItem(sampleMessages),
      };
      break;
    case 'emoji':
      content = {
        text: randomItem(emojiReactions).repeat(randomIntBetween(1, 3)),
      };
      break;
    case 'image':
      content = {
        text: 'Check out this photo!',
        image: {
          url: `https://example.com/images/${randomString(10)}.jpg`,
          caption: 'Amazing view from my trip',
        },
      };
      break;
    case 'location':
      content = {
        text: 'I\'m here right now!',
        location: {
          lat: (Math.random() * 180 - 90).toFixed(6),
          lng: (Math.random() * 360 - 180).toFixed(6),
          name: 'Cool Spot',
        },
      };
      break;
    case 'booking_share':
      content = {
        text: 'Just booked this experience!',
        booking: {
          id: `booking_${randomString(8)}`,
          title: 'City Walking Tour',
          date: new Date(Date.now() + randomIntBetween(1, 30) * 24 * 60 * 60 * 1000).toISOString(),
        },
      };
      break;
    case 'recommendation':
      content = {
        text: 'You should definitely try this place!',
        recommendation: {
          type: 'restaurant',
          name: 'Local Favorite Bistro',
          rating: (Math.random() * 2 + 3).toFixed(1), // 3.0-5.0
        },
      };
      break;
  }
  
  return {
    id: `msg_${randomString(12)}`,
    roomId: roomId,
    userId: user.id,
    userName: user.name,
    userRole: user.role,
    type: messageType,
    content: content,
    timestamp: new Date().toISOString(),
    metadata: {
      userLocation: user.location,
      clientType: 'k6-test',
    },
  };
}

function generateTypingEvent(user, roomId) {
  return {
    type: 'typing',
    roomId: roomId,
    userId: user.id,
    userName: user.name,
    isTyping: Math.random() > 0.3, // 70% typing, 30% stopped typing
    timestamp: new Date().toISOString(),
  };
}

export function setup() {
  console.log('Setting up WebSocket chat load test...');
  
  const wsUrl = __ENV.WS_URL || 'ws://localhost:8080/ws/chat';
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  
  return {
    wsUrl: wsUrl,
    baseUrl: baseUrl,
    authToken: __ENV.AUTH_TOKEN || null,
    enableTyping: __ENV.ENABLE_TYPING !== 'false',
    messageFrequency: parseInt(__ENV.MESSAGE_FREQUENCY) || 5, // seconds
  };
}

export default function(data) {
  const user = randomItem(userProfiles);
  const chatRoom = generateChatRoom();
  
  const wsUrl = `${data.wsUrl}?userId=${user.id}&roomId=${chatRoom.id}`;
  
  const params = {
    headers: {
      'User-Agent': 'k6-websocket-test/1.0',
      'X-User-ID': user.id,
      'X-User-Role': user.role,
    },
  };
  
  if (data.authToken) {
    params.headers['Authorization'] = `Bearer ${data.authToken}`;
  }
  
  let messagesSentCount = 0;
  let messagesReceivedCount = 0;
  let lastMessageTime = 0;
  let connectionStartTime = Date.now();
  let isConnected = false;
  
  const response = ws.connect(wsUrl, params, function(socket) {
    socket.on('open', function() {
      const connectionDuration = Date.now() - connectionStartTime;
      connectionTime.add(connectionDuration);
      isConnected = true;
      
      console.log(`Connected to chat room ${chatRoom.id} as ${user.name}`);
      
      // Join the chat room
      const joinMessage = {
        type: 'join',
        roomId: chatRoom.id,
        userId: user.id,
        userName: user.name,
        userRole: user.role,
        timestamp: new Date().toISOString(),
      };
      
      socket.send(JSON.stringify(joinMessage));
      chatRoomJoins.add(1);
    });
    
    socket.on('message', function(message) {
      messagesReceivedCount++;
      messagesReceived.add(1);
      
      try {
        const data = JSON.parse(message);
        
        // Calculate message latency for our own messages
        if (data.userId === user.id && data.clientTimestamp) {
          const latency = Date.now() - new Date(data.clientTimestamp).getTime();
          messageLatency.add(latency);
        }
        
        // Validate message structure
        check(data, {
          'message has required fields': (msg) => 
            msg.type && msg.roomId && msg.userId && msg.timestamp,
          'message timestamp is valid': (msg) => 
            !isNaN(new Date(msg.timestamp).getTime()),
          'message room matches': (msg) => 
            msg.roomId === chatRoom.id,
        });
        
        // Sometimes react to messages from others
        if (data.userId !== user.id && data.type === 'message' && Math.random() > 0.8) {
          const reaction = {
            type: 'reaction',
            messageId: data.id,
            roomId: chatRoom.id,
            userId: user.id,
            reaction: randomItem(emojiReactions),
            timestamp: new Date().toISOString(),
          };
          
          socket.send(JSON.stringify(reaction));
        }
        
      } catch (e) {
        console.error(`Failed to parse message: ${e.message}`);
        errorRate.add(1);
      }
    });
    
    socket.on('close', function() {
      if (isConnected) {
        connectionDrops.add(1);
        console.log(`Connection to room ${chatRoom.id} closed`);
      }
      isConnected = false;
    });
    
    socket.on('error', function(e) {
      console.error(`WebSocket error in room ${chatRoom.id}: ${e.error()}`);
      errorRate.add(1);
    });
    
    // Message sending loop
    const messageInterval = setInterval(() => {
      if (!isConnected) {
        clearInterval(messageInterval);
        return;
      }
      
      // Send typing indicator occasionally
      if (data.enableTyping && Math.random() > 0.7) {
        const typingEvent = generateTypingEvent(user, chatRoom.id);
        socket.send(JSON.stringify(typingEvent));
        typingEvents.add(1);
        
        // Stop typing after a short delay
        setTimeout(() => {
          if (isConnected) {
            const stopTyping = { ...typingEvent, isTyping: false };
            socket.send(JSON.stringify(stopTyping));
          }
        }, randomIntBetween(1000, 3000));
      }
      
      // Send a message
      const message = generateMessage(user, chatRoom.id);
      message.clientTimestamp = new Date().toISOString();
      
      socket.send(JSON.stringify(message));
      messagesSentCount++;
      messagesSent.add(1);
      lastMessageTime = Date.now();
      
    }, data.messageFrequency * 1000 + randomIntBetween(-2000, 2000)); // Add some jitter
    
    // Keep connection alive for test duration
    sleep(randomIntBetween(30, 120)); // Stay connected for 30s-2min
    
    // Clean up
    clearInterval(messageInterval);
    
    // Send leave message
    if (isConnected) {
      const leaveMessage = {
        type: 'leave',
        roomId: chatRoom.id,
        userId: user.id,
        timestamp: new Date().toISOString(),
      };
      
      socket.send(JSON.stringify(leaveMessage));
    }
  });
  
  // Validate WebSocket connection
  check(response, {
    'websocket connection successful': (r) => r && r.url,
    'websocket connected to correct room': (r) => r && r.url.includes(chatRoom.id),
  });
  
  if (!response || !response.url) {
    errorRate.add(1);
    console.error(`Failed to connect to WebSocket: ${wsUrl}`);
  }
  
  // Brief pause between connections in stress test
  sleep(randomIntBetween(1, 3));
}

export function teardown(data) {
  console.log('WebSocket chat load test completed');
  console.log(`Typing events enabled: ${data.enableTyping}`);
  console.log(`Message frequency: ${data.messageFrequency}s`);
}

export function handleSummary(data) {
  const summary = {
    testType: 'WebSocket Chat Load Test',
    timestamp: new Date().toISOString(),
    duration: data.state.testRunDurationMs,
    scenarios: Object.keys(options.scenarios),
    metrics: {
      ws_sessions: data.metrics.ws_sessions,
      ws_connecting: data.metrics.ws_connecting,
      ws_msgs_sent: data.metrics.ws_msgs_sent,
      ws_msgs_received: data.metrics.ws_msgs_received,
      connection_time: data.metrics.connection_time,
      message_latency: data.metrics.message_latency,
      messages_sent: data.metrics.messages_sent,
      messages_received: data.metrics.messages_received,
      connection_drops: data.metrics.connection_drops,
      reconnections: data.metrics.reconnections,
      chat_room_joins: data.metrics.chat_room_joins,
      typing_events: data.metrics.typing_events,
      errors: data.metrics.errors,
    },
    thresholds: data.thresholds,
    chatMetrics: {
      avgConnectionTime: data.metrics.connection_time ? 
        data.metrics.connection_time.avg.toFixed(2) + 'ms' : 'N/A',
      avgMessageLatency: data.metrics.message_latency ? 
        data.metrics.message_latency.avg.toFixed(2) + 'ms' : 'N/A',
      messageDeliveryRate: data.metrics.messages_sent && data.metrics.messages_received ? 
        (data.metrics.messages_received.count / data.metrics.messages_sent.count * 100).toFixed(2) + '%' : 'N/A',
      connectionDropRate: data.metrics.connection_drops && data.metrics.ws_sessions ? 
        (data.metrics.connection_drops.count / data.metrics.ws_sessions.count * 100).toFixed(2) + '%' : '0%',
      typingEventRate: data.metrics.typing_events && data.metrics.messages_sent ? 
        (data.metrics.typing_events.count / data.metrics.messages_sent.count * 100).toFixed(2) + '%' : '0%',
    },
  };
  
  return {
    'websocket-chat-summary.json': JSON.stringify(summary, null, 2),
    stdout: `
=== WebSocket Chat Load Test Results ===
WebSocket Sessions: ${data.metrics.ws_sessions ? data.metrics.ws_sessions.count : 0}
Messages Sent: ${data.metrics.messages_sent ? data.metrics.messages_sent.count : 0}
Messages Received: ${data.metrics.messages_received ? data.metrics.messages_received.count : 0}
Message Delivery Rate: ${data.metrics.messages_sent && data.metrics.messages_received ? (data.metrics.messages_received.count / data.metrics.messages_sent.count * 100).toFixed(2) : 'N/A'}%
Avg Connection Time: ${data.metrics.connection_time ? data.metrics.connection_time.avg.toFixed(2) : 'N/A'}ms
Avg Message Latency: ${data.metrics.message_latency ? data.metrics.message_latency.avg.toFixed(2) : 'N/A'}ms
Connection Drops: ${data.metrics.connection_drops ? data.metrics.connection_drops.count : 0}
Chat Room Joins: ${data.metrics.chat_room_joins ? data.metrics.chat_room_joins.count : 0}
Typing Events: ${data.metrics.typing_events ? data.metrics.typing_events.count : 0}
Error Rate: ${data.metrics.errors ? (data.metrics.errors.rate * 100).toFixed(2) : 0}%
========================================
`
  };
}