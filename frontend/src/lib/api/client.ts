import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081';

// Helper functions for W3C trace context
function generateTraceId(): string {
  // Generate a 32-character hex string (128-bit)
  return Array.from({ length: 32 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

function generateSpanId(): string {
  // Generate a 16-character hex string (64-bit)
  return Array.from({ length: 16 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

// Create axios instance with default configuration
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token and trace headers
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Add W3C trace context headers for distributed tracing
    // Generate a simple trace ID and span ID for frontend requests
    const traceId = generateTraceId();
    const spanId = generateSpanId();
    const traceparent = `00-${traceId}-${spanId}-01`;
    
    config.headers['traceparent'] = traceparent;
    config.headers['tracestate'] = '';
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;