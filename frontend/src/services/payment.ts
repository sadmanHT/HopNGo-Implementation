import { ApiResponse } from '../lib/api/types';

export interface PaymentMethod {
  id: string;
  type: 'card' | 'paypal' | 'bank_transfer';
  last4?: string;
  brand?: string;
  expiryMonth?: number;
  expiryYear?: number;
  isDefault: boolean;
  createdAt: string;
}

export interface PaymentIntent {
  id: string;
  amount: number;
  currency: string;
  status: 'requires_payment_method' | 'requires_confirmation' | 'requires_action' | 'processing' | 'succeeded' | 'canceled';
  clientSecret: string;
  orderId: string;
}

export interface CreatePaymentIntentRequest {
  orderId: string;
  amount: number;
  currency: string;
  paymentMethodId?: string;
  returnUrl?: string;
}

export interface ProcessPaymentRequest {
  paymentIntentId: string;
  paymentMethodId: string;
  billingDetails?: {
    name: string;
    email: string;
    address: {
      line1: string;
      line2?: string;
      city: string;
      state: string;
      postalCode: string;
      country: string;
    };
  };
}

class PaymentService {
  private baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  async getPaymentMethods(): Promise<PaymentMethod[]> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/methods`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch payment methods: ${response.statusText}`);
      }

      const data: ApiResponse<PaymentMethod[]> = await response.json();
      return data.data || [];
    } catch (error) {
      console.error('Error fetching payment methods:', error);
      throw error;
    }
  }

  async addPaymentMethod(paymentMethodData: Omit<PaymentMethod, 'id' | 'createdAt'>): Promise<PaymentMethod> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/methods`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(paymentMethodData),
      });

      if (!response.ok) {
        throw new Error(`Failed to add payment method: ${response.statusText}`);
      }

      const data: ApiResponse<PaymentMethod> = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error adding payment method:', error);
      throw error;
    }
  }

  async removePaymentMethod(paymentMethodId: string): Promise<void> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/methods/${paymentMethodId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to remove payment method: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error removing payment method:', error);
      throw error;
    }
  }

  async createPaymentIntent(request: CreatePaymentIntentRequest): Promise<PaymentIntent> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/intents`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to create payment intent: ${response.statusText}`);
      }

      const data: ApiResponse<PaymentIntent> = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error creating payment intent:', error);
      throw error;
    }
  }

  async confirmPayment(request: ProcessPaymentRequest): Promise<PaymentIntent> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/confirm`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to confirm payment: ${response.statusText}`);
      }

      const data: ApiResponse<PaymentIntent> = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error confirming payment:', error);
      throw error;
    }
  }

  async getPaymentIntent(paymentIntentId: string): Promise<PaymentIntent> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/intents/${paymentIntentId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch payment intent: ${response.statusText}`);
      }

      const data: ApiResponse<PaymentIntent> = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error fetching payment intent:', error);
      throw error;
    }
  }

  async cancelPaymentIntent(paymentIntentId: string): Promise<void> {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/payments/intents/${paymentIntentId}/cancel`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to cancel payment intent: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error canceling payment intent:', error);
      throw error;
    }
  }
}

export const paymentService = new PaymentService();
export default paymentService;