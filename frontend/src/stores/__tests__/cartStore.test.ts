import { renderHook, act } from '@testing-library/react';
import { useCartStore } from '../cartStore';
import { shoppingService } from '../../services/shoppingService';

// Mock the shopping service
jest.mock('../../services/shoppingService', () => ({
  shoppingService: {
    addToCart: jest.fn(),
    updateCartItem: jest.fn(),
    removeFromCart: jest.fn(),
    clearCart: jest.fn(),
    getCartItems: jest.fn(),
  },
}));

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

describe('CartStore', () => {
  beforeEach(() => {
    // Reset all mocks
    jest.clearAllMocks();
    
    // Reset store state
    useCartStore.getState().clearCart();
    
    // Mock localStorage to return empty cart
    localStorageMock.getItem.mockReturnValue(null);
  });

  describe('Initial State', () => {
    it('should have empty cart initially', () => {
      const { result } = renderHook(() => useCartStore());
      
      expect(result.current.items).toEqual([]);
      expect(result.current.totalItems).toBe(0);
      expect(result.current.totalPrice).toBe(0);
      expect(result.current.isAddingToCart).toBe(false);
    });

    it('should load cart from localStorage on initialization', () => {
      const savedCart = {
        items: [
          {
            id: '1',
            gearId: 'gear-1',
            name: 'Test Backpack',
            price: 50,
            type: 'rent' as const,
            quantity: 1,
            imageUrl: '/test.jpg',
            category: 'backpacks',
            rentalDuration: 3,
          },
        ],
        totalItems: 1,
        totalPrice: 150,
      };
      
      localStorageMock.getItem.mockReturnValue(JSON.stringify(savedCart));
      
      const { result } = renderHook(() => useCartStore());
      
      expect(result.current.items).toHaveLength(1);
      expect(result.current.items[0].name).toBe('Test Backpack');
      expect(result.current.totalItems).toBe(1);
    });
  });

  describe('Adding Items', () => {
    it('should add new item to cart', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Tent',
        price: 80,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/tent.jpg',
        category: 'tents',
        rentalDuration: 5,
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      await act(async () => {
        await result.current.addItem(mockItem);
      });

      expect(result.current.items).toHaveLength(1);
      expect(result.current.items[0].name).toBe('Test Tent');
      expect(result.current.totalItems).toBe(1);
      expect(result.current.totalPrice).toBe(400); // 80 * 5 days
      expect(shoppingService.addToCart).toHaveBeenCalledWith(expect.objectContaining(mockItem));
    });

    it('should update quantity if item already exists', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Backpack',
        price: 50,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/backpack.jpg',
        category: 'backpacks',
        rentalDuration: 2,
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });
      (shoppingService.updateCartItem as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      // Add item first time
      await act(async () => {
        await result.current.addItem(mockItem);
      });

      // Add same item again
      await act(async () => {
        await result.current.addItem(mockItem);
      });

      expect(result.current.items).toHaveLength(1);
      expect(result.current.items[0].quantity).toBe(2);
      expect(result.current.totalItems).toBe(2);
      expect(result.current.totalPrice).toBe(200); // 50 * 2 days * 2 quantity
    });

    it('should handle sale items correctly', async () => {
      const mockItem = {
        gearId: 'gear-2',
        name: 'Test Boots',
        price: 120,
        type: 'sale' as const,
        quantity: 1,
        imageUrl: '/boots.jpg',
        category: 'footwear',
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      await act(async () => {
        await result.current.addItem(mockItem);
      });

      expect(result.current.items[0].type).toBe('sale');
      expect(result.current.totalPrice).toBe(120); // No rental duration for sale items
    });

    it('should handle API errors gracefully', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Item',
        price: 50,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/test.jpg',
        category: 'accessories',
        rentalDuration: 1,
      };

      (shoppingService.addToCart as jest.Mock).mockRejectedValue(new Error('API Error'));

      const { result } = renderHook(() => useCartStore());

      await act(async () => {
        await expect(result.current.addItem(mockItem)).rejects.toThrow('API Error');
      });

      expect(result.current.items).toHaveLength(0);
    });
  });

  describe('Updating Items', () => {
    it('should update item quantity', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Item',
        price: 30,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/test.jpg',
        category: 'accessories',
        rentalDuration: 2,
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });
      (shoppingService.updateCartItem as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      // Add item first
      await act(async () => {
        await result.current.addItem(mockItem);
      });

      const itemId = result.current.items[0].id;

      // Update quantity
      await act(async () => {
        await result.current.updateQuantity(itemId, 3);
      });

      expect(result.current.items[0].quantity).toBe(3);
      expect(result.current.totalItems).toBe(3);
      expect(result.current.totalPrice).toBe(180); // 30 * 2 days * 3 quantity
    });

    it('should update rental duration', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Item',
        price: 40,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/test.jpg',
        category: 'accessories',
        rentalDuration: 2,
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });
      (shoppingService.updateCartItem as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      // Add item first
      await act(async () => {
        await result.current.addItem(mockItem);
      });

      const itemId = result.current.items[0].id;

      // Update rental duration
      await act(async () => {
        await result.current.updateRentalDuration(itemId, 5);
      });

      expect(result.current.items[0].rentalDuration).toBe(5);
      expect(result.current.totalPrice).toBe(200); // 40 * 5 days * 1 quantity
    });

    it('should remove item when quantity is set to 0', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Item',
        price: 25,
        type: 'rent' as const,
        quantity: 2,
        imageUrl: '/test.jpg',
        category: 'accessories',
        rentalDuration: 1,
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });
      (shoppingService.removeFromCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      // Add item first
      await act(async () => {
        await result.current.addItem(mockItem);
      });

      const itemId = result.current.items[0].id;

      // Set quantity to 0
      await act(async () => {
        await result.current.updateQuantity(itemId, 0);
      });

      expect(result.current.items).toHaveLength(0);
      expect(result.current.totalItems).toBe(0);
      expect(result.current.totalPrice).toBe(0);
    });
  });

  describe('Removing Items', () => {
    it('should remove item from cart', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Item',
        price: 60,
        type: 'sale' as const,
        quantity: 1,
        imageUrl: '/test.jpg',
        category: 'electronics',
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });
      (shoppingService.removeFromCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      // Add item first
      await act(async () => {
        await result.current.addItem(mockItem);
      });

      const itemId = result.current.items[0].id;

      // Remove item
      await act(async () => {
        await result.current.removeItem(itemId);
      });

      expect(result.current.items).toHaveLength(0);
      expect(result.current.totalItems).toBe(0);
      expect(result.current.totalPrice).toBe(0);
    });
  });

  describe('Clearing Cart', () => {
    it('should clear all items from cart', async () => {
      const mockItems = [
        {
          gearId: 'gear-1',
          name: 'Item 1',
          price: 30,
          type: 'rent' as const,
          quantity: 1,
          imageUrl: '/item1.jpg',
          category: 'backpacks',
          rentalDuration: 2,
        },
        {
          gearId: 'gear-2',
          name: 'Item 2',
          price: 50,
          type: 'sale' as const,
          quantity: 1,
          imageUrl: '/item2.jpg',
          category: 'tents',
        },
      ];

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });
      (shoppingService.clearCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      // Add items
      for (const item of mockItems) {
        await act(async () => {
          await result.current.addItem(item);
        });
      }

      expect(result.current.items).toHaveLength(2);

      // Clear cart
      await act(async () => {
        await result.current.clearCart();
      });

      expect(result.current.items).toHaveLength(0);
      expect(result.current.totalItems).toBe(0);
      expect(result.current.totalPrice).toBe(0);
    });
  });

  describe('Persistence', () => {
    it('should save cart to localStorage when items change', async () => {
      const mockItem = {
        gearId: 'gear-1',
        name: 'Test Item',
        price: 45,
        type: 'rent' as const,
        quantity: 1,
        imageUrl: '/test.jpg',
        category: 'cooking',
        rentalDuration: 3,
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      await act(async () => {
        await result.current.addItem(mockItem);
      });

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'hopngo-cart',
        expect.stringContaining('Test Item')
      );
    });

    it('should handle localStorage errors gracefully', () => {
      localStorageMock.setItem.mockImplementation(() => {
        throw new Error('localStorage error');
      });

      const { result } = renderHook(() => useCartStore());

      // Should not throw error
      expect(() => {
        act(() => {
          result.current.addItem({
            gearId: 'gear-1',
            name: 'Test Item',
            price: 30,
            type: 'sale' as const,
            quantity: 1,
            imageUrl: '/test.jpg',
            category: 'accessories',
          });
        });
      }).not.toThrow();
    });
  });

  describe('Calculated Values', () => {
    it('should calculate totals correctly for mixed cart', async () => {
      const rentItem = {
        gearId: 'gear-1',
        name: 'Rent Item',
        price: 20,
        type: 'rent' as const,
        quantity: 2,
        imageUrl: '/rent.jpg',
        category: 'tents',
        rentalDuration: 4,
      };

      const saleItem = {
        gearId: 'gear-2',
        name: 'Sale Item',
        price: 100,
        type: 'sale' as const,
        quantity: 1,
        imageUrl: '/sale.jpg',
        category: 'electronics',
      };

      (shoppingService.addToCart as jest.Mock).mockResolvedValue({ success: true });

      const { result } = renderHook(() => useCartStore());

      await act(async () => {
        await result.current.addItem(rentItem);
        await result.current.addItem(saleItem);
      });

      expect(result.current.totalItems).toBe(3); // 2 + 1
      expect(result.current.totalPrice).toBe(260); // (20 * 4 * 2) + (100 * 1)
    });
  });
});