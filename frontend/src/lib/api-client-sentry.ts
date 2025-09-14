import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { SentryService } from './sentry';

/**
 * Enhanced API client with Sentry error tracking
 */
export class SentryApiClient {
  private client: AxiosInstance;
  private baseURL: string;

  constructor(baseURL: string, config?: AxiosRequestConfig) {
    this.baseURL = baseURL;
    this.client = axios.create({
      baseURL,
      timeout: 30000,
      ...config,
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        // Add breadcrumb for API request
        SentryService.addBreadcrumb(
          `API Request: ${config.method?.toUpperCase()} ${config.url}`,
          'http',
          'info',
          {
            url: config.url,
            method: config.method,
            headers: this.sanitizeHeaders(config.headers),
          }
        );

        // Add request timestamp for performance tracking
        config.metadata = { startTime: Date.now() };

        return config;
      },
      (error) => {
        SentryService.captureException(error, {
          tags: { errorType: 'request_setup_error' },
        });
        return Promise.reject(error);
      }
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => {
        // Calculate request duration
        const duration = Date.now() - (response.config.metadata?.startTime || 0);

        // Add success breadcrumb
        SentryService.addBreadcrumb(
          `API Response: ${response.status} ${response.config.method?.toUpperCase()} ${response.config.url}`,
          'http',
          'info',
          {
            status: response.status,
            duration,
            url: response.config.url,
            method: response.config.method,
          }
        );

        // Track slow requests
        if (duration > 5000) {
          SentryService.captureMessage(
            `Slow API request detected: ${response.config.method?.toUpperCase()} ${response.config.url}`,
            'warning',
            {
              tags: {
                slowRequest: 'true',
                endpoint: response.config.url || 'unknown',
              },
              extra: {
                duration,
                status: response.status,
              },
            }
          );
        }

        return response;
      },
      (error: AxiosError) => {
        this.handleApiError(error);
        return Promise.reject(error);
      }
    );
  }

  private handleApiError(error: AxiosError) {
    const config = error.config;
    const response = error.response;
    const duration = config?.metadata?.startTime 
      ? Date.now() - config.metadata.startTime 
      : undefined;

    // Determine error severity based on status code
    let level: 'error' | 'warning' | 'info' = 'error';
    if (response?.status && response.status >= 400 && response.status < 500) {
      level = 'warning'; // Client errors are usually less critical
    }

    // Don't report certain expected errors
    const ignoredStatuses = [401, 403, 404];
    if (response?.status && ignoredStatuses.includes(response.status)) {
      // Still add breadcrumb but don't capture as error
      SentryService.addBreadcrumb(
        `API Error: ${response.status} ${config?.method?.toUpperCase()} ${config?.url}`,
        'http',
        'warning',
        {
          status: response.status,
          url: config?.url,
          method: config?.method,
          duration,
        }
      );
      return;
    }

    // Capture API error with context
    SentryService.captureApiError(error, {
      url: config?.url || 'unknown',
      method: config?.method || 'unknown',
      status: response?.status,
      requestData: this.sanitizeData(config?.data),
      responseData: this.sanitizeData(response?.data),
    });

    // Add error breadcrumb
    SentryService.addBreadcrumb(
      `API Error: ${response?.status || 'Network'} ${config?.method?.toUpperCase()} ${config?.url}`,
      'http',
      level,
      {
        status: response?.status,
        statusText: response?.statusText,
        url: config?.url,
        method: config?.method,
        duration,
        errorMessage: error.message,
      }
    );
  }

  private sanitizeHeaders(headers: any): any {
    if (!headers) return headers;

    const sanitized = { ...headers };
    const sensitiveHeaders = ['authorization', 'cookie', 'x-api-key', 'x-auth-token'];

    Object.keys(sanitized).forEach(key => {
      if (sensitiveHeaders.some(sensitive => key.toLowerCase().includes(sensitive))) {
        sanitized[key] = '***';
      }
    });

    return sanitized;
  }

  private sanitizeData(data: any): any {
    if (!data || typeof data !== 'object') {
      return data;
    }

    const sensitiveKeys = [
      'password', 'token', 'key', 'secret', 'auth', 'authorization',
      'cookie', 'session', 'csrf', 'api_key', 'access_token', 'refresh_token',
      'credit_card', 'card_number', 'cvv', 'ssn'
    ];

    const sanitized = Array.isArray(data) ? [...data] : { ...data };

    Object.keys(sanitized).forEach(key => {
      const lowerKey = key.toLowerCase();
      if (sensitiveKeys.some(sensitive => lowerKey.includes(sensitive))) {
        sanitized[key] = '***';
      } else if (typeof sanitized[key] === 'object' && sanitized[key] !== null) {
        sanitized[key] = this.sanitizeData(sanitized[key]);
      }
    });

    return sanitized;
  }

  // HTTP methods with Sentry integration
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return SentryService.measureAsync(
      `GET ${url}`,
      () => this.client.get<T>(url, config),
      'http.client'
    );
  }

  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return SentryService.measureAsync(
      `POST ${url}`,
      () => this.client.post<T>(url, data, config),
      'http.client'
    );
  }

  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return SentryService.measureAsync(
      `PUT ${url}`,
      () => this.client.put<T>(url, data, config),
      'http.client'
    );
  }

  async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return SentryService.measureAsync(
      `PATCH ${url}`,
      () => this.client.patch<T>(url, data, config),
      'http.client'
    );
  }

  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return SentryService.measureAsync(
      `DELETE ${url}`,
      () => this.client.delete<T>(url, config),
      'http.client'
    );
  }

  // Get the underlying axios instance if needed
  getClient(): AxiosInstance {
    return this.client;
  }

  // Update base URL
  setBaseURL(baseURL: string) {
    this.baseURL = baseURL;
    this.client.defaults.baseURL = baseURL;
  }

  // Set default headers
  setDefaultHeaders(headers: Record<string, string>) {
    Object.assign(this.client.defaults.headers, headers);
  }

  // Set authorization header
  setAuthToken(token: string) {
    this.client.defaults.headers.Authorization = `Bearer ${token}`;
  }

  // Clear authorization header
  clearAuthToken() {
    delete this.client.defaults.headers.Authorization;
  }
}

// Create default API client instance
export const apiClient = new SentryApiClient(
  process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1'
);

// Export types
export type { AxiosRequestConfig, AxiosResponse, AxiosError };

// Extend AxiosRequestConfig to include metadata
declare module 'axios' {
  interface AxiosRequestConfig {
    metadata?: {
      startTime: number;
    };
  }
}