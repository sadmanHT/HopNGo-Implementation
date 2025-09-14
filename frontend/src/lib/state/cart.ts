import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { shoppingService, CartItem, Cart, TravelGear } from '../../services/shopping';
import { 
  validateCartInventory, 
  checkGearInventory, 
  refreshCartInventory,
  InventoryConflict 
} from '../../utils/inventoryValidator';

interface CartState {
  // Cart data
  cart: Cart | null;
  items: CartItem[];
  totalItems: number;
  subtotal: number;
  tax: number;
  shipping: number;
  total: number;
  currency: string;
  
  // Loading states
  isLoading: boolean;
  isAddingItem: boolean;
  isUpdatingItem: boolean;
  isRemovingItem: boolean;
  isValidatingInventory: boolean;
  
  // Error handling
  error: string | null;
  inventoryConflicts: InventoryConflict[];
  lastInventoryCheck: string | null;
  
  // Actions
  loadCart: () => Promise<void>;
  addToCart: (item: {
    gearId: string;
    quantity: number;
    type: 'rent' | 'purchase';
    rentDuration?: { startDate: string; endDate: string };
  }) => Promise<void>;
  updateCartItem: (itemId: string, updates: {
    quantity?: number;
    rentDuration?: { startDate: string; endDate: string };
  }) => Promise<void>;
  removeFromCart: (itemId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  
  // Inventory validation
  validateInventory: () => Promise<void>;
  refreshInventory: () => Promise<void>;
  resolveInventoryConflict: (itemId: string, action: 'remove' | 'reduce' | 'keep') => Promise<void>;
  
  // Utility functions
  getItemById: (itemId: string) => CartItem | undefined;
  getItemByGearId: (gearId: string) => CartItem | undefined;
  calculateTotals: () => void;
  
  // Optimistic updates
  optimisticAddItem: (gear: TravelGear, quantity: number, type: 'rent' | 'purchase', rentDuration?: { startDate: string; endDate: string }) => void;
  optimisticUpdateItem: (itemId: string, updates: { quantity?: number; rentDuration?: { startDate: string; endDate: string; days?: number } }) => void;
  optimisticRemoveItem: (itemId: string) => void;
  
  // Reset functions
  resetError: () => void;
  resetCart: () => void;
}

const TAX_RATE = 0.08; // 8% tax rate
const SHIPPING_THRESHOLD = 100; // Free shipping over $100
const SHIPPING_COST = 15; // $15 shipping fee

const calculateItemTotal = (item: CartItem): number => {
  if (item.type === 'rent' && item.rentDuration) {
    return item.gear.rentPrice! * item.quantity * item.rentDuration.days;
  }
  return item.gear.price * item.quantity;
};

const generateCartItemId = (): string => {
  return `cart_item_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      // Initial state
      cart: null,
      items: [],
      totalItems: 0,
      subtotal: 0,
      tax: 0,
      shipping: 0,
      total: 0,
      currency: 'USD',
      
      isLoading: false,
      isAddingItem: false,
      isUpdatingItem: false,
      isRemovingItem: false,
      isValidatingInventory: false,
      
      error: null,
      inventoryConflicts: [],
      lastInventoryCheck: null,
      
      // Load cart from server
      loadCart: async () => {
        set({ isLoading: true, error: null });
        try {
          const cart = await shoppingService.getCart();
          set({
            cart,
            items: cart.items,
            totalItems: cart.totalItems,
            subtotal: cart.subtotal,
            tax: cart.tax,
            shipping: cart.shipping,
            total: cart.total,
            currency: cart.currency,
            isLoading: false,
          });
        } catch (error) {
          console.error('Failed to load cart:', error);
          set({ 
            error: 'Failed to load cart. Please try again.',
            isLoading: false 
          });
        }
      },
      
      // Add item to cart
      addToCart: async (itemData) => {
        const { gearId, quantity, type, rentDuration } = itemData;
        
        set({ isAddingItem: true, error: null });
        
        try {
          // First, get gear details for inventory validation
          const gear = await shoppingService.getGearById(gearId);
          
          // Validate inventory before adding
          const inventoryCheck = await checkGearInventory(gear, quantity, type, rentDuration);
          
          if (!inventoryCheck.available) {
            set({ 
              error: inventoryCheck.message || 'Item is not available in the requested quantity',
              isAddingItem: false 
            });
            return;
          }
          
          // Check if item already exists in cart
          const existingItem = get().getItemByGearId(gearId);
          
          if (existingItem && existingItem.type === type) {
            // Validate total quantity (existing + new)
            const totalQuantity = existingItem.quantity + quantity;
            const totalInventoryCheck = await checkGearInventory(gear, totalQuantity, type, rentDuration);
            
            if (!totalInventoryCheck.available) {
              set({ 
                error: `Cannot add ${quantity} more items. ${totalInventoryCheck.message}`,
                isAddingItem: false 
              });
              return;
            }
            
            // Update existing item quantity
            await get().updateCartItem(existingItem.id, {
              quantity: totalQuantity,
              rentDuration: rentDuration || existingItem.rentDuration,
            });
          } else {
            // Add new item
            const updatedCart = await shoppingService.addToCart(itemData);
            set({
              cart: updatedCart,
              items: updatedCart.items,
              totalItems: updatedCart.totalItems,
              subtotal: updatedCart.subtotal,
              tax: updatedCart.tax,
              shipping: updatedCart.shipping,
              total: updatedCart.total,
            });
          }
        } catch (error) {
          console.error('Failed to add item to cart:', error);
          
          // Handle specific inventory errors
          if (error && typeof error === 'object' && 'response' in error) {
            const axiosError = error as any;
            if (axiosError.response?.status === 409) {
              set({ error: 'This item is no longer available in the requested quantity.' });
            } else if (axiosError.response?.status === 400) {
              set({ error: axiosError.response.data?.message || 'Invalid item configuration.' });
            } else {
              set({ error: 'Failed to add item to cart. Please try again.' });
            }
          } else {
            set({ error: 'Failed to add item to cart. Please try again.' });
          }
          
          // Revert optimistic update if it was applied
          await get().loadCart();
        } finally {
          set({ isAddingItem: false });
        }
      },
      
      // Update cart item
      updateCartItem: async (itemId, updates) => {
        set({ isUpdatingItem: true, error: null });
        
        // Store original item for rollback
        const originalItem = get().getItemById(itemId);
        if (!originalItem) {
          set({ error: 'Item not found in cart', isUpdatingItem: false });
          return;
        }
        
        // Apply optimistic update
        get().optimisticUpdateItem(itemId, updates);
        
        try {
          const updatedCart = await shoppingService.updateCartItem(itemId, updates);
          set({
            cart: updatedCart,
            items: updatedCart.items,
            totalItems: updatedCart.totalItems,
            subtotal: updatedCart.subtotal,
            tax: updatedCart.tax,
            shipping: updatedCart.shipping,
            total: updatedCart.total,
          });
        } catch (error) {
          console.error('Failed to update cart item:', error);
          set({ error: 'Failed to update item. Please try again.' });
          
          // Revert optimistic update
          await get().loadCart();
        } finally {
          set({ isUpdatingItem: false });
        }
      },
      
      // Remove item from cart
      removeFromCart: async (itemId) => {
        set({ isRemovingItem: true, error: null });
        
        // Store original items for rollback
        const originalItems = [...get().items];
        
        // Apply optimistic update
        get().optimisticRemoveItem(itemId);
        
        try {
          const updatedCart = await shoppingService.removeFromCart(itemId);
          set({
            cart: updatedCart,
            items: updatedCart.items,
            totalItems: updatedCart.totalItems,
            subtotal: updatedCart.subtotal,
            tax: updatedCart.tax,
            shipping: updatedCart.shipping,
            total: updatedCart.total,
          });
        } catch (error) {
          console.error('Failed to remove item from cart:', error);
          set({ 
            error: 'Failed to remove item. Please try again.',
            items: originalItems,
          });
          get().calculateTotals();
        } finally {
          set({ isRemovingItem: false });
        }
      },
      
      // Clear entire cart
      clearCart: async () => {
        set({ isLoading: true, error: null });
        
        try {
          await shoppingService.clearCart();
          set({
            cart: null,
            items: [],
            totalItems: 0,
            subtotal: 0,
            tax: 0,
            shipping: 0,
            total: 0,
            inventoryConflicts: [],
            lastInventoryCheck: null,
          });
        } catch (error) {
          console.error('Failed to clear cart:', error);
          set({ error: 'Failed to clear cart. Please try again.' });
        } finally {
          set({ isLoading: false });
        }
      },
      
      // Validate inventory for all cart items
      validateInventory: async () => {
        const { items } = get();
        
        if (items.length === 0) {
          set({ inventoryConflicts: [], lastInventoryCheck: new Date().toISOString() });
          return;
        }
        
        set({ isValidatingInventory: true, error: null });
        
        try {
          const validationResult = await validateCartInventory(items);
          set({
            inventoryConflicts: validationResult.conflicts,
            lastInventoryCheck: new Date().toISOString(),
            error: validationResult.conflicts.length > 0 ? 'Some items in your cart have inventory conflicts.' : null
          });
        } catch (error) {
          console.error('Failed to validate inventory:', error);
          set({ error: 'Failed to validate inventory. Please try again.' });
        } finally {
          set({ isValidatingInventory: false });
        }
      },
      
      // Refresh cart with latest inventory data
      refreshInventory: async () => {
        const { items } = get();
        
        if (items.length === 0) return;
        
        set({ isValidatingInventory: true, error: null });
        
        try {
          const updatedItems = await refreshCartInventory(items);
          
          // Update cart with refreshed data
          const updatedCart = await shoppingService.getCart();
          
          set({
            cart: updatedCart,
            items: updatedCart.items,
            totalItems: updatedCart.totalItems,
            subtotal: updatedCart.subtotal,
            tax: updatedCart.tax,
            shipping: updatedCart.shipping,
            total: updatedCart.total,
            lastInventoryCheck: new Date().toISOString(),
          });
          
          // Validate after refresh
          await get().validateInventory();
        } catch (error) {
          console.error('Failed to refresh inventory:', error);
          set({ error: 'Failed to refresh inventory. Please try again.' });
        } finally {
          set({ isValidatingInventory: false });
        }
      },
      
      // Resolve inventory conflicts
      resolveInventoryConflict: async (itemId: string, action: 'remove' | 'reduce' | 'keep') => {
        const { inventoryConflicts } = get();
        const conflict = inventoryConflicts.find(c => c.itemId === itemId);
        
        if (!conflict) return;
        
        set({ isUpdatingItem: true, error: null });
        
        try {
          switch (action) {
            case 'remove':
              await get().removeFromCart(itemId);
              break;
              
            case 'reduce':
              if (conflict.availableQuantity > 0) {
                await get().updateCartItem(itemId, {
                  quantity: conflict.availableQuantity
                });
              } else {
                await get().removeFromCart(itemId);
              }
              break;
              
            case 'keep':
              // User chooses to keep the item as is
              // Remove from conflicts but don't change the item
              break;
          }
          
          // Remove resolved conflict
          const updatedConflicts = inventoryConflicts.filter(c => c.itemId !== itemId);
          set({ 
            inventoryConflicts: updatedConflicts,
            error: updatedConflicts.length > 0 ? 'Some items in your cart have inventory conflicts.' : null
          });
        } catch (error) {
          console.error('Failed to resolve inventory conflict:', error);
          set({ error: 'Failed to resolve inventory conflict. Please try again.' });
        } finally {
          set({ isUpdatingItem: false });
        }
      },
      
      // Utility functions
      getItemById: (itemId) => {
        return get().items.find(item => item.id === itemId);
      },
      
      getItemByGearId: (gearId) => {
        return get().items.find(item => item.gearId === gearId);
      },
      
      calculateTotals: () => {
        const { items } = get();
        
        const subtotal = items.reduce((sum, item) => sum + calculateItemTotal(item), 0);
        const tax = subtotal * TAX_RATE;
        const shipping = subtotal >= SHIPPING_THRESHOLD ? 0 : SHIPPING_COST;
        const total = subtotal + tax + shipping;
        const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
        
        set({
          subtotal: Math.round(subtotal * 100) / 100,
          tax: Math.round(tax * 100) / 100,
          shipping: Math.round(shipping * 100) / 100,
          total: Math.round(total * 100) / 100,
          totalItems,
        });
      },
      
      // Optimistic updates
      optimisticAddItem: (gear, quantity, type, rentDuration) => {
        const newItem: CartItem = {
          id: generateCartItemId(),
          gearId: gear.id,
          gear,
          quantity,
          type,
          rentDuration: rentDuration ? {
            ...rentDuration,
            days: Math.ceil(
              (new Date(rentDuration.endDate).getTime() - new Date(rentDuration.startDate).getTime()) / (1000 * 60 * 60 * 24)
            ),
          } : undefined,
          price: type === 'rent' ? gear.rentPrice! : gear.price,
          totalPrice: 0, // Will be calculated
          addedAt: new Date().toISOString(),
        };
        
        newItem.totalPrice = calculateItemTotal(newItem);
        
        set(state => ({
          items: [...state.items, newItem],
        }));
        
        get().calculateTotals();
      },
      
      optimisticUpdateItem: (itemId, updates) => {
        set(state => ({
          items: state.items.map(item => {
            if (item.id === itemId) {
              let updatedItem = { ...item };
              
              // Update quantity if provided
              if (updates.quantity !== undefined) {
                updatedItem.quantity = updates.quantity;
              }
              
              // Update rentDuration if provided
              if (updates.rentDuration) {
                const days = updates.rentDuration.days || Math.ceil(
                  (new Date(updates.rentDuration.endDate).getTime() - new Date(updates.rentDuration.startDate).getTime()) / (1000 * 60 * 60 * 24)
                );
                updatedItem.rentDuration = {
                  startDate: updates.rentDuration.startDate,
                  endDate: updates.rentDuration.endDate,
                  days: days,
                };
              }
              
              updatedItem.totalPrice = calculateItemTotal(updatedItem);
              return updatedItem;
            }
            return item;
          }),
        }));
        
        get().calculateTotals();
      },
      
      optimisticRemoveItem: (itemId) => {
        set(state => ({
          items: state.items.filter(item => item.id !== itemId),
        }));
        
        get().calculateTotals();
      },
      
      // Reset functions
      resetError: () => set({ error: null }),
      
      resetCart: () => set({
        cart: null,
        items: [],
        totalItems: 0,
        subtotal: 0,
        tax: 0,
        shipping: 0,
        total: 0,
        error: null,
      }),
    }),
    {
      name: 'hopngo-cart-storage',
      partialize: (state) => ({
        items: state.items,
        totalItems: state.totalItems,
        subtotal: state.subtotal,
        tax: state.tax,
        shipping: state.shipping,
        total: state.total,
        currency: state.currency,
      }),
    }
  )
);

// Export cart store hook
export default useCartStore;