import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useRouter } from 'next/navigation';
import { useCartStore } from '../../stores/cartStore';
import { shoppingService } from '../../services/shoppingService';
import CheckoutPage from '../../app/[locale]/checkout/page';
import OrderConfirmationPage from '../../app/[locale]/order-confirmation/page';

// Mock dependencies
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
  useParams: jest.fn(() => ({ locale: 'en' })),
}));

jest.mock('../../services/shoppingService');
jest.mock('../../stores/cartStore');

// Mock localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

const mockRouter = {
  push: jest.fn(),
  replace: jest.fn(),
  back: jest.fn(),
};

const mockShoppingService = shoppingService as jest.Mocked<typeof shoppingService>;
const mockUseCartStore = useCartStore as jest.MockedFunction<typeof useCartStore>;

describe('Order Flow Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    localStorageMock.getItem.mockReturnValue(null);
  });

  describe('Complete Purchase Flow', () => {
    const mockCartItems = [
      {
        id: 'cart-1',
        gearId: 'gear-1',
        name: 'Professional Hiking Backpack',
        price: 150,
        type: 'sale' as const,
        quantity: 1,
        imageUrl: '/backpack.jpg',
        category: 'backpacks',
        availableQuantity: 5,
      },
      {
        id: 'cart-2',
        gearId: 'gear-2',
        name: 'Camping Tent',
        price: 80,
        rentPrice: 25,
        type: 'rent' as const,
        quantity: 1,
        rentalDuration: 3,
        imageUrl: '/tent.jpg',
        category: 'tents',
        availableQuantity: 2,
      },
    ];

    const mockCartStore = {
      items: mockCartItems,
      totalItems: 2,
      totalPrice: 230, // 150 + (25 * 3 + 5 fee)
      addItem: jest.fn(),
      updateItem: jest.fn(),
      removeItem: jest.fn(),
      clearCart: jest.fn(),
      isLoading: false,
      error: null,
    };

    beforeEach(() => {
      mockUseCartStore.mockReturnValue(mockCartStore);
    });

    it('should complete full purchase flow from checkout to confirmation', async () => {
      // Mock successful order creation
      const mockOrder = {
        orderId: 'order-123',
        status: 'confirmed',
        totalAmount: 230,
        items: mockCartItems,
        createdAt: '2024-01-15T10:00:00Z',
        estimatedDelivery: '2024-01-20T10:00:00Z',
        shippingAddress: {
          street: '123 Main St',
          city: 'Dhaka',
          postalCode: '1000',
          country: 'Bangladesh',
        },
      };

      mockShoppingService.createOrder.mockResolvedValue(mockOrder);
      mockShoppingService.getOrderById.mockResolvedValue(mockOrder);

      // Render checkout page
      render(<CheckoutPage />);

      // Verify cart items are displayed
      expect(screen.getByText('Professional Hiking Backpack')).toBeInTheDocument();
      expect(screen.getByText('Camping Tent')).toBeInTheDocument();
      expect(screen.getByText('$230.00')).toBeInTheDocument();

      // Fill in shipping information
      fireEvent.change(screen.getByLabelText(/full name/i), {
        target: { value: 'John Doe' },
      });
      fireEvent.change(screen.getByLabelText(/email/i), {
        target: { value: 'john@example.com' },
      });
      fireEvent.change(screen.getByLabelText(/phone/i), {
        target: { value: '+8801234567890' },
      });
      fireEvent.change(screen.getByLabelText(/street address/i), {
        target: { value: '123 Main St' },
      });
      fireEvent.change(screen.getByLabelText(/city/i), {
        target: { value: 'Dhaka' },
      });
      fireEvent.change(screen.getByLabelText(/postal code/i), {
        target: { value: '1000' },
      });

      // Select payment method
      fireEvent.click(screen.getByLabelText(/credit card/i));

      // Fill in payment information
      fireEvent.change(screen.getByLabelText(/card number/i), {
        target: { value: '4111111111111111' },
      });
      fireEvent.change(screen.getByLabelText(/expiry date/i), {
        target: { value: '12/25' },
      });
      fireEvent.change(screen.getByLabelText(/cvv/i), {
        target: { value: '123' },
      });

      // Submit order
      fireEvent.click(screen.getByRole('button', { name: /place order/i }));

      // Wait for order creation
      await waitFor(() => {
        expect(mockShoppingService.createOrder).toHaveBeenCalledWith({
          paymentMethod: 'card',
          shippingAddress: {
            fullName: 'John Doe',
            email: 'john@example.com',
            phone: '+8801234567890',
            street: '123 Main St',
            city: 'Dhaka',
            postalCode: '1000',
            country: 'Bangladesh',
          },
          billingAddress: {
            fullName: 'John Doe',
            email: 'john@example.com',
            phone: '+8801234567890',
            street: '123 Main St',
            city: 'Dhaka',
            postalCode: '1000',
            country: 'Bangladesh',
          },
          paymentDetails: {
            cardNumber: '4111111111111111',
            expiryDate: '12/25',
            cvv: '123',
          },
        });
      });

      // Verify cart is cleared
      expect(mockCartStore.clearCart).toHaveBeenCalled();

      // Verify navigation to order confirmation
      expect(mockRouter.push).toHaveBeenCalledWith('/order-confirmation?orderId=order-123');
    });

    it('should handle payment failures gracefully', async () => {
      // Mock payment failure
      mockShoppingService.createOrder.mockRejectedValue({
        response: {
          status: 402,
          data: { error: 'Payment declined' },
        },
      });

      render(<CheckoutPage />);

      // Fill in required fields and submit
      fireEvent.change(screen.getByLabelText(/full name/i), {
        target: { value: 'John Doe' },
      });
      fireEvent.change(screen.getByLabelText(/email/i), {
        target: { value: 'john@example.com' },
      });
      fireEvent.change(screen.getByLabelText(/street address/i), {
        target: { value: '123 Main St' },
      });
      fireEvent.change(screen.getByLabelText(/city/i), {
        target: { value: 'Dhaka' },
      });
      fireEvent.change(screen.getByLabelText(/postal code/i), {
        target: { value: '1000' },
      });
      fireEvent.click(screen.getByLabelText(/credit card/i));
      fireEvent.change(screen.getByLabelText(/card number/i), {
        target: { value: '4000000000000002' }, // Declined card
      });
      fireEvent.change(screen.getByLabelText(/expiry date/i), {
        target: { value: '12/25' },
      });
      fireEvent.change(screen.getByLabelText(/cvv/i), {
        target: { value: '123' },
      });

      fireEvent.click(screen.getByRole('button', { name: /place order/i }));

      // Wait for error message
      await waitFor(() => {
        expect(screen.getByText(/payment declined/i)).toBeInTheDocument();
      });

      // Verify cart is not cleared on failure
      expect(mockCartStore.clearCart).not.toHaveBeenCalled();
      expect(mockRouter.push).not.toHaveBeenCalled();
    });

    it('should handle inventory conflicts during checkout', async () => {
      // Mock inventory error
      mockShoppingService.createOrder.mockRejectedValue({
        response: {
          status: 409,
          data: { 
            error: 'Inventory conflict',
            details: {
              gearId: 'gear-2',
              availableQuantity: 0,
              requestedQuantity: 1,
            },
          },
        },
      });

      render(<CheckoutPage />);

      // Fill in required fields and submit
      fireEvent.change(screen.getByLabelText(/full name/i), {
        target: { value: 'John Doe' },
      });
      fireEvent.change(screen.getByLabelText(/email/i), {
        target: { value: 'john@example.com' },
      });
      fireEvent.change(screen.getByLabelText(/street address/i), {
        target: { value: '123 Main St' },
      });
      fireEvent.change(screen.getByLabelText(/city/i), {
        target: { value: 'Dhaka' },
      });
      fireEvent.change(screen.getByLabelText(/postal code/i), {
        target: { value: '1000' },
      });
      fireEvent.click(screen.getByLabelText(/credit card/i));
      fireEvent.change(screen.getByLabelText(/card number/i), {
        target: { value: '4111111111111111' },
      });
      fireEvent.change(screen.getByLabelText(/expiry date/i), {
        target: { value: '12/25' },
      });
      fireEvent.change(screen.getByLabelText(/cvv/i), {
        target: { value: '123' },
      });

      fireEvent.click(screen.getByRole('button', { name: /place order/i }));

      // Wait for inventory error message
      await waitFor(() => {
        expect(screen.getByText(/inventory conflict/i)).toBeInTheDocument();
        expect(screen.getByText(/camping tent.*no longer available/i)).toBeInTheDocument();
      });

      // Verify user is prompted to update cart
      expect(screen.getByRole('button', { name: /update cart/i })).toBeInTheDocument();
    });
  });

  describe('Order Confirmation Flow', () => {
    const mockOrder = {
      orderId: 'order-123',
      status: 'confirmed',
      totalAmount: 230,
      items: [
        {
          id: 'cart-1',
          gearId: 'gear-1',
          name: 'Professional Hiking Backpack',
          price: 150,
          type: 'sale' as const,
          quantity: 1,
          imageUrl: '/backpack.jpg',
          category: 'backpacks',
        },
        {
          id: 'cart-2',
          gearId: 'gear-2',
          name: 'Camping Tent',
          price: 80,
          rentPrice: 25,
          type: 'rent' as const,
          quantity: 1,
          rentalDuration: 3,
          imageUrl: '/tent.jpg',
          category: 'tents',
          rentalStartDate: '2024-01-20',
          rentalEndDate: '2024-01-23',
        },
      ],
      createdAt: '2024-01-15T10:00:00Z',
      estimatedDelivery: '2024-01-20T10:00:00Z',
      shippingAddress: {
        street: '123 Main St',
        city: 'Dhaka',
        postalCode: '1000',
        country: 'Bangladesh',
      },
    };

    beforeEach(() => {
      // Mock URL search params
      Object.defineProperty(window, 'location', {
        value: {
          search: '?orderId=order-123',
        },
        writable: true,
      });

      mockShoppingService.getOrderById.mockResolvedValue(mockOrder);
    });

    it('should display order confirmation details correctly', async () => {
      render(<OrderConfirmationPage />);

      // Wait for order data to load
      await waitFor(() => {
        expect(screen.getByText('Order Confirmed!')).toBeInTheDocument();
      });

      // Verify order details
      expect(screen.getByText('Order #order-123')).toBeInTheDocument();
      expect(screen.getByText('$230.00')).toBeInTheDocument();
      expect(screen.getByText('Professional Hiking Backpack')).toBeInTheDocument();
      expect(screen.getByText('Camping Tent')).toBeInTheDocument();

      // Verify delivery information
      expect(screen.getByText(/estimated delivery/i)).toBeInTheDocument();
      expect(screen.getByText('January 20, 2024')).toBeInTheDocument();

      // Verify rental information
      expect(screen.getByText(/rental period/i)).toBeInTheDocument();
      expect(screen.getByText('Jan 20 - Jan 23, 2024')).toBeInTheDocument();
    });

    it('should allow users to submit reviews for purchased items', async () => {
      const mockReview = {
        id: 'review-1',
        gearId: 'gear-1',
        rating: 5,
        comment: 'Excellent backpack!',
        orderId: 'order-123',
        createdAt: '2024-01-16T10:00:00Z',
        user: {
          name: 'John Doe',
          avatar: '/avatar.jpg',
        },
      };

      mockShoppingService.submitReview.mockResolvedValue(mockReview);

      render(<OrderConfirmationPage />);

      // Wait for order data to load
      await waitFor(() => {
        expect(screen.getByText('Order Confirmed!')).toBeInTheDocument();
      });

      // Find and click review button for backpack
      const reviewButtons = screen.getAllByText(/write review/i);
      fireEvent.click(reviewButtons[0]);

      // Wait for review form to appear
      await waitFor(() => {
        expect(screen.getByText(/rate this item/i)).toBeInTheDocument();
      });

      // Submit review
      const stars = screen.getAllByRole('button', { name: /star/i });
      fireEvent.click(stars[4]); // 5 stars

      fireEvent.change(screen.getByPlaceholderText(/share your experience/i), {
        target: { value: 'Excellent backpack!' },
      });

      fireEvent.click(screen.getByRole('button', { name: /submit review/i }));

      // Wait for review submission
      await waitFor(() => {
        expect(mockShoppingService.submitReview).toHaveBeenCalledWith({
          gearId: 'gear-1',
          rating: 5,
          comment: 'Excellent backpack!',
          orderId: 'order-123',
        });
      });

      // Verify success message
      expect(screen.getByText(/review submitted successfully/i)).toBeInTheDocument();
    });

    it('should handle missing order ID gracefully', async () => {
      // Mock empty search params
      Object.defineProperty(window, 'location', {
        value: {
          search: '',
        },
        writable: true,
      });

      render(<OrderConfirmationPage />);

      // Should show error message
      await waitFor(() => {
        expect(screen.getByText(/order not found/i)).toBeInTheDocument();
      });

      // Should provide link to go back
      expect(screen.getByRole('link', { name: /back to marketplace/i })).toBeInTheDocument();
    });

    it('should handle order loading errors', async () => {
      mockShoppingService.getOrderById.mockRejectedValue({
        response: {
          status: 404,
          data: { error: 'Order not found' },
        },
      });

      render(<OrderConfirmationPage />);

      // Wait for error state
      await waitFor(() => {
        expect(screen.getByText(/order not found/i)).toBeInTheDocument();
      });

      // Should provide retry option
      expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
    });
  });

  describe('Cart Persistence', () => {
    it('should persist cart state across page refreshes', () => {
      const cartData = {
        items: [
          {
            id: 'cart-1',
            gearId: 'gear-1',
            name: 'Test Item',
            price: 50,
            type: 'sale' as const,
            quantity: 1,
          },
        ],
        totalItems: 1,
        totalPrice: 50,
      };

      // Mock localStorage with cart data
      localStorageMock.getItem.mockReturnValue(JSON.stringify(cartData));

      const mockCartStore = {
        ...cartData,
        addItem: jest.fn(),
        updateItem: jest.fn(),
        removeItem: jest.fn(),
        clearCart: jest.fn(),
        isLoading: false,
        error: null,
      };

      mockUseCartStore.mockReturnValue(mockCartStore);

      render(<CheckoutPage />);

      // Verify cart data is loaded from localStorage
      expect(localStorageMock.getItem).toHaveBeenCalledWith('hopngo-cart');
      expect(screen.getByText('Test Item')).toBeInTheDocument();
      expect(screen.getByText('$50.00')).toBeInTheDocument();
    });

    it('should handle corrupted localStorage data', () => {
      // Mock corrupted localStorage data
      localStorageMock.getItem.mockReturnValue('invalid-json');

      const mockCartStore = {
        items: [],
        totalItems: 0,
        totalPrice: 0,
        addItem: jest.fn(),
        updateItem: jest.fn(),
        removeItem: jest.fn(),
        clearCart: jest.fn(),
        isLoading: false,
        error: null,
      };

      mockUseCartStore.mockReturnValue(mockCartStore);

      render(<CheckoutPage />);

      // Should handle gracefully and show empty cart
      expect(screen.getByText(/your cart is empty/i)).toBeInTheDocument();
    });
  });
});