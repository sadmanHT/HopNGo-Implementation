import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface CartItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  image?: string;
  category?: string;
}

interface CartState {
  items: CartItem[];
  total: number;
  itemCount: number;
  addItem: (item: Omit<CartItem, 'quantity'> & { quantity?: number }) => void;
  removeItem: (id: string) => void;
  updateQuantity: (id: string, quantity: number) => void;
  clearCart: () => void;
  getItem: (id: string) => CartItem | undefined;
}

const calculateTotal = (items: CartItem[]): number => {
  return items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
};

const calculateItemCount = (items: CartItem[]): number => {
  return items.reduce((sum, item) => sum + item.quantity, 0);
};

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      total: 0,
      itemCount: 0,

      addItem: (item) => {
        const { items } = get();
        const existingItem = items.find(i => i.id === item.id);
        
        let newItems: CartItem[];
        if (existingItem) {
          newItems = items.map(i => 
            i.id === item.id 
              ? { ...i, quantity: i.quantity + (item.quantity || 1) }
              : i
          );
        } else {
          newItems = [...items, { ...item, quantity: item.quantity || 1 }];
        }
        
        set({
          items: newItems,
          total: calculateTotal(newItems),
          itemCount: calculateItemCount(newItems)
        });
      },

      removeItem: (id) => {
        const { items } = get();
        const newItems = items.filter(item => item.id !== id);
        
        set({
          items: newItems,
          total: calculateTotal(newItems),
          itemCount: calculateItemCount(newItems)
        });
      },

      updateQuantity: (id, quantity) => {
        if (quantity <= 0) {
          get().removeItem(id);
          return;
        }
        
        const { items } = get();
        const newItems = items.map(item => 
          item.id === id ? { ...item, quantity } : item
        );
        
        set({
          items: newItems,
          total: calculateTotal(newItems),
          itemCount: calculateItemCount(newItems)
        });
      },

      clearCart: () => {
        set({
          items: [],
          total: 0,
          itemCount: 0
        });
      },

      getItem: (id) => {
        const { items } = get();
        return items.find(item => item.id === id);
      }
    }),
    {
      name: 'cart-storage',
      partialize: (state) => ({
        items: state.items,
        total: state.total,
        itemCount: state.itemCount
      })
    }
  )
);