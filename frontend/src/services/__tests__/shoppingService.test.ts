import { shoppingService } from '../shoppingService';
import { apiClient } from '../api';

// Mock the API client
jest.mock('../api', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;

describe('ShoppingService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('searchGear', () => {
    it('should search gear with filters', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              id: '1',
              name: 'Test Backpack',
              price: 50,
              rentPrice: 15,
              category: 'backpacks',
              rating: 4.5,
              reviewCount: 10,
            },
          ],
          total: 1,
          page: 1,
          limit: 20,
        },
      };

      mockApiClient.get.mockResolvedValue(mockResponse);

      const filters = {
        category: 'backpacks',
        query: 'hiking',
        sortBy: 'price-low',
        filters: {
          minPrice: 0,
          maxPrice: 100,
          availability: 'all',
          minRating: 4,
        },
      };

      const result = await shoppingService.searchGear(filters);

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/marketplace/search', {
        params: expect.objectContaining({
          category: 'backpacks',
          q: 'hiking',
          sortBy: 'price-low',
          minPrice: 0,
          maxPrice: 100,
          availability: 'all',
          minRating: 4,
        }),
      });

      expect(result).toEqual(mockResponse.data);
    });

    it('should handle search without filters', async () => {
      const mockResponse = {
        data: {
          items: [],
          total: 0,
          page: 1,
          limit: 20,
        },
      };

      mockApiClient.get.mockResolvedValue(mockResponse);

      const result = await shoppingService.searchGear({});

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/marketplace/search', {
        params: {},
      });

      expect(result).toEqual(mockResponse.data);
    });

    it('should handle API errors', async () => {
      mockApiClient.get.mockRejectedValue(new Error('Network error'));

      await expect(shoppingService.searchGear({})).rejects.toThrow('Network error');
    });
  });

  describe('getGearById', () => {
    it('should fetch gear by ID', async () => {
      const mockGear = {
        id: '1',
        name: 'Test Tent',
        description: 'A great tent for camping',
        price: 200,
        rentPrice: 25,
        category: 'tents',
        images: ['/tent1.jpg', '/tent2.jpg'],
        rating: 4.8,
        reviewCount: 15,
        availableForRent: true,
        availableForSale: true,
        availableQuantity: 3,
        condition: 'excellent',
        location: 'Dhaka, Bangladesh',
        owner: {
          id: 'owner-1',
          name: 'John Doe',
          memberSince: '2022',
        },
        features: ['Waterproof', 'Easy setup', 'Lightweight'],
        brand: 'OutdoorPro',
        model: 'Explorer 2000',
        weight: '2.5kg',
        dimensions: '200x150x120cm',
      };

      mockApiClient.get.mockResolvedValue({ data: mockGear });

      const result = await shoppingService.getGearById('1');

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/marketplace/gear/1');
      expect(result).toEqual(mockGear);
    });

    it('should handle gear not found', async () => {
      mockApiClient.get.mockRejectedValue({
        response: { status: 404 },
        message: 'Gear not found',
      });

      await expect(shoppingService.getGearById('999')).rejects.toMatchObject({
        response: { status: 404 },
      });
    });
  });

  describe('getRelatedGear', () => {
    it('should fetch related gear', async () => {
      const mockResponse = {
        data: {
          items: [
            {
              id: '2',
              name: 'Similar Backpack',
              price: 45,
              category: 'backpacks',
            },
            {
              id: '3',
              name: 'Another Backpack',
              price: 60,
              category: 'backpacks',
            },
          ],
        },
      };

      mockApiClient.get.mockResolvedValue(mockResponse);

      const result = await shoppingService.getRelatedGear('1');

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/marketplace/gear/1/related');
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('Cart Operations', () => {
    describe('addToCart', () => {
      it('should add item to cart', async () => {
        const cartItem = {
          gearId: '1',
          name: 'Test Item',
          price: 30,
          type: 'rent' as const,
          quantity: 1,
          imageUrl: '/test.jpg',
          category: 'backpacks',
          rentalDuration: 3,
        };

        const mockResponse = {
          data: {
            success: true,
            cartItem: { ...cartItem, id: 'cart-1' },
          },
        };

        mockApiClient.post.mockResolvedValue(mockResponse);

        const result = await shoppingService.addToCart(cartItem);

        expect(mockApiClient.post).toHaveBeenCalledWith('/api/cart/items', cartItem);
        expect(result).toEqual(mockResponse.data);
      });

      it('should handle inventory errors', async () => {
        const cartItem = {
          gearId: '1',
          name: 'Out of Stock Item',
          price: 30,
          type: 'sale' as const,
          quantity: 5,
          imageUrl: '/test.jpg',
          category: 'electronics',
        };

        mockApiClient.post.mockRejectedValue({
          response: {
            status: 400,
            data: { error: 'Insufficient inventory' },
          },
        });

        await expect(shoppingService.addToCart(cartItem)).rejects.toMatchObject({
          response: {
            status: 400,
            data: { error: 'Insufficient inventory' },
          },
        });
      });
    });

    describe('updateCartItem', () => {
      it('should update cart item', async () => {
        const updates = {
          quantity: 2,
          rentalDuration: 5,
        };

        const mockResponse = {
          data: {
            success: true,
            cartItem: {
              id: 'cart-1',
              quantity: 2,
              rentalDuration: 5,
            },
          },
        };

        mockApiClient.put.mockResolvedValue(mockResponse);

        const result = await shoppingService.updateCartItem('cart-1', updates);

        expect(mockApiClient.put).toHaveBeenCalledWith('/api/cart/items/cart-1', updates);
        expect(result).toEqual(mockResponse.data);
      });
    });

    describe('removeFromCart', () => {
      it('should remove item from cart', async () => {
        const mockResponse = {
          data: { success: true },
        };

        mockApiClient.delete.mockResolvedValue(mockResponse);

        const result = await shoppingService.removeFromCart('cart-1');

        expect(mockApiClient.delete).toHaveBeenCalledWith('/api/cart/items/cart-1');
        expect(result).toEqual(mockResponse.data);
      });
    });

    describe('getCartItems', () => {
      it('should fetch cart items', async () => {
        const mockResponse = {
          data: {
            items: [
              {
                id: 'cart-1',
                gearId: '1',
                name: 'Cart Item 1',
                quantity: 1,
              },
              {
                id: 'cart-2',
                gearId: '2',
                name: 'Cart Item 2',
                quantity: 2,
              },
            ],
            totalItems: 3,
            totalPrice: 150,
          },
        };

        mockApiClient.get.mockResolvedValue(mockResponse);

        const result = await shoppingService.getCartItems();

        expect(mockApiClient.get).toHaveBeenCalledWith('/api/cart');
        expect(result).toEqual(mockResponse.data);
      });
    });

    describe('clearCart', () => {
      it('should clear all cart items', async () => {
        const mockResponse = {
          data: { success: true },
        };

        mockApiClient.delete.mockResolvedValue(mockResponse);

        const result = await shoppingService.clearCart();

        expect(mockApiClient.delete).toHaveBeenCalledWith('/api/cart');
        expect(result).toEqual(mockResponse.data);
      });
    });
  });

  describe('Order Operations', () => {
    describe('createOrder', () => {
      it('should create order from cart', async () => {
        const orderData = {
          paymentMethod: 'card',
          shippingAddress: {
            street: '123 Main St',
            city: 'Dhaka',
            postalCode: '1000',
            country: 'Bangladesh',
          },
          billingAddress: {
            street: '123 Main St',
            city: 'Dhaka',
            postalCode: '1000',
            country: 'Bangladesh',
          },
        };

        const mockResponse = {
          data: {
            orderId: 'order-123',
            status: 'pending',
            totalAmount: 200,
            items: [
              {
                gearId: '1',
                name: 'Test Item',
                quantity: 1,
                price: 200,
              },
            ],
          },
        };

        mockApiClient.post.mockResolvedValue(mockResponse);

        const result = await shoppingService.createOrder(orderData);

        expect(mockApiClient.post).toHaveBeenCalledWith('/api/orders', orderData);
        expect(result).toEqual(mockResponse.data);
      });

      it('should handle payment processing errors', async () => {
        const orderData = {
          paymentMethod: 'card',
          shippingAddress: {
            street: '123 Main St',
            city: 'Dhaka',
            postalCode: '1000',
            country: 'Bangladesh',
          },
        };

        mockApiClient.post.mockRejectedValue({
          response: {
            status: 402,
            data: { error: 'Payment declined' },
          },
        });

        await expect(shoppingService.createOrder(orderData)).rejects.toMatchObject({
          response: {
            status: 402,
            data: { error: 'Payment declined' },
          },
        });
      });
    });

    describe('getOrderById', () => {
      it('should fetch order by ID', async () => {
        const mockOrder = {
          id: 'order-123',
          status: 'confirmed',
          totalAmount: 150,
          items: [
            {
              gearId: '1',
              name: 'Order Item',
              quantity: 1,
              price: 150,
            },
          ],
          createdAt: '2024-01-15T10:00:00Z',
        };

        mockApiClient.get.mockResolvedValue({ data: mockOrder });

        const result = await shoppingService.getOrderById('order-123');

        expect(mockApiClient.get).toHaveBeenCalledWith('/api/orders/order-123');
        expect(result).toEqual(mockOrder);
      });
    });

    describe('getUserOrders', () => {
      it('should fetch user orders', async () => {
        const mockResponse = {
          data: {
            orders: [
              {
                id: 'order-1',
                status: 'completed',
                totalAmount: 100,
              },
              {
                id: 'order-2',
                status: 'pending',
                totalAmount: 200,
              },
            ],
            total: 2,
          },
        };

        mockApiClient.get.mockResolvedValue(mockResponse);

        const result = await shoppingService.getUserOrders();

        expect(mockApiClient.get).toHaveBeenCalledWith('/api/orders');
        expect(result).toEqual(mockResponse.data);
      });
    });
  });

  describe('Review Operations', () => {
    describe('submitReview', () => {
      it('should submit gear review', async () => {
        const reviewData = {
          gearId: '1',
          rating: 5,
          comment: 'Excellent gear!',
          orderId: 'order-123',
        };

        const mockResponse = {
          data: {
            id: 'review-1',
            ...reviewData,
            createdAt: '2024-01-15T10:00:00Z',
            user: {
              name: 'John Doe',
              avatar: '/avatar.jpg',
            },
          },
        };

        mockApiClient.post.mockResolvedValue(mockResponse);

        const result = await shoppingService.submitReview(reviewData);

        expect(mockApiClient.post).toHaveBeenCalledWith('/api/reviews', reviewData);
        expect(result).toEqual(mockResponse.data);
      });

      it('should handle duplicate review errors', async () => {
        const reviewData = {
          gearId: '1',
          rating: 4,
          comment: 'Good gear',
          orderId: 'order-123',
        };

        mockApiClient.post.mockRejectedValue({
          response: {
            status: 409,
            data: { error: 'Review already exists for this order' },
          },
        });

        await expect(shoppingService.submitReview(reviewData)).rejects.toMatchObject({
          response: {
            status: 409,
            data: { error: 'Review already exists for this order' },
          },
        });
      });
    });

    describe('getGearReviews', () => {
      it('should fetch gear reviews', async () => {
        const mockResponse = {
          data: {
            reviews: [
              {
                id: 'review-1',
                rating: 5,
                comment: 'Great product!',
                user: { name: 'Alice' },
                createdAt: '2024-01-15T10:00:00Z',
              },
              {
                id: 'review-2',
                rating: 4,
                comment: 'Good quality',
                user: { name: 'Bob' },
                createdAt: '2024-01-14T15:30:00Z',
              },
            ],
            averageRating: 4.5,
            totalReviews: 2,
            ratingDistribution: {
              5: 1,
              4: 1,
              3: 0,
              2: 0,
              1: 0,
            },
          },
        };

        mockApiClient.get.mockResolvedValue(mockResponse);

        const result = await shoppingService.getGearReviews('1');

        expect(mockApiClient.get).toHaveBeenCalledWith('/api/gear/1/reviews');
        expect(result).toEqual(mockResponse.data);
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      mockApiClient.get.mockRejectedValue(new Error('Network Error'));

      await expect(shoppingService.searchGear({})).rejects.toThrow('Network Error');
    });

    it('should handle server errors', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 500,
          data: { error: 'Internal Server Error' },
        },
      });

      await expect(shoppingService.searchGear({})).rejects.toMatchObject({
        response: {
          status: 500,
        },
      });
    });

    it('should handle authentication errors', async () => {
      mockApiClient.post.mockRejectedValue({
        response: {
          status: 401,
          data: { error: 'Unauthorized' },
        },
      });

      const cartItem = {
        gearId: '1',
        name: 'Test Item',
        price: 30,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/test.jpg',
        category: 'backpacks',
        rentalDuration: 3,
      };

      await expect(shoppingService.addToCart(cartItem)).rejects.toMatchObject({
        response: {
          status: 401,
        },
      });
    });
  });
});