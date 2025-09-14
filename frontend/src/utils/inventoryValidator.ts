import { CartItem, TravelGear } from '../services/shopping';
import { shoppingService } from '../services/shopping';

/**
 * Utility functions for inventory validation and conflict resolution
 */

export interface InventoryConflict {
  itemId: string;
  gearId: string;
  gearName: string;
  requestedQuantity: number;
  availableQuantity: number;
  type: 'out_of_stock' | 'insufficient_quantity' | 'unavailable_dates' | 'rent' | 'purchase';
  message: string;
  suggestedAction: 'remove' | 'reduce_quantity' | 'change_dates';
  reason?: string;
}

export interface InventoryValidationResult {
  isValid: boolean;
  conflicts: InventoryConflict[];
  validItems: CartItem[];
  totalConflicts: number;
}

/**
 * Validates cart items against current inventory availability
 */
export async function validateCartInventory(cartItems: CartItem[]): Promise<InventoryValidationResult> {
  const conflicts: InventoryConflict[] = [];
  const validItems: CartItem[] = [];

  for (const item of cartItems) {
    try {
      // Check availability for each item
      const availability = await shoppingService.checkAvailability(
        item.gearId,
        item.type === 'rent' && item.rentDuration
          ? {
              startDate: item.rentDuration.startDate,
              endDate: item.rentDuration.endDate,
            }
          : undefined
      );

      if (!availability.available) {
        conflicts.push({
          itemId: item.id,
          gearId: item.gearId,
          gearName: item.gear.title,
          requestedQuantity: item.quantity,
          availableQuantity: 0,
          type: 'out_of_stock',
          message: `${item.gear.title} is currently out of stock`,
          suggestedAction: 'remove',
        });
      } else if (availability.quantity < item.quantity) {
        conflicts.push({
          itemId: item.id,
          gearId: item.gearId,
          gearName: item.gear.title,
          requestedQuantity: item.quantity,
          availableQuantity: availability.quantity,
          type: 'insufficient_quantity',
          message: `Only ${availability.quantity} units of ${item.gear.title} are available (requested: ${item.quantity})`,
          suggestedAction: 'reduce_quantity',
        });
      } else if (item.type === 'rent' && availability.nextAvailableDate) {
        conflicts.push({
          itemId: item.id,
          gearId: item.gearId,
          gearName: item.gear.title,
          requestedQuantity: item.quantity,
          availableQuantity: availability.quantity,
          type: 'unavailable_dates',
          message: `${item.gear.title} is not available for the selected dates. Next available: ${availability.nextAvailableDate}`,
          suggestedAction: 'change_dates',
        });
      } else {
        validItems.push(item);
      }
    } catch (error) {
      console.error(`Failed to validate inventory for item ${item.id}:`, error);
      // Assume item is valid if we can't check (network issues, etc.)
      validItems.push(item);
    }
  }

  return {
    isValid: conflicts.length === 0,
    conflicts,
    validItems,
    totalConflicts: conflicts.length,
  };
}

/**
 * Resolves inventory conflicts by applying suggested actions
 */
export function resolveInventoryConflicts(
  cartItems: CartItem[],
  conflicts: InventoryConflict[],
  resolutions: { [itemId: string]: 'remove' | 'reduce' | 'update_dates' }
): CartItem[] {
  const resolvedItems: CartItem[] = [];

  for (const item of cartItems) {
    const conflict = conflicts.find(c => c.itemId === item.id);
    const resolution = resolutions[item.id];

    if (!conflict) {
      // No conflict, keep item as is
      resolvedItems.push(item);
    } else if (resolution === 'remove') {
      // Skip this item (remove from cart)
      continue;
    } else if (resolution === 'reduce' && conflict.type === 'insufficient_quantity') {
      // Reduce quantity to available amount
      resolvedItems.push({
        ...item,
        quantity: conflict.availableQuantity,
      });
    } else if (resolution === 'update_dates' && conflict.type === 'unavailable_dates') {
      // Keep item but user needs to update dates manually
      resolvedItems.push(item);
    } else {
      // Default: keep item as is and let user handle manually
      resolvedItems.push(item);
    }
  }

  return resolvedItems;
}

/**
 * Checks if a gear item has sufficient inventory for the requested quantity
 */
export async function checkGearInventory(
  gear: TravelGear,
  quantity: number,
  type: 'rent' | 'purchase',
  rentDuration?: { startDate: string; endDate: string }
): Promise<{
  available: boolean;
  maxQuantity: number;
  message?: string;
}> {
  try {
    const availability = await shoppingService.checkAvailability(
      gear.id,
      type === 'rent' && rentDuration ? rentDuration : undefined
    );

    if (!availability.available) {
      return {
        available: false,
        maxQuantity: 0,
        message: `${gear.title} is currently out of stock`,
      };
    }

    if (availability.quantity < quantity) {
      return {
        available: false,
        maxQuantity: availability.quantity,
        message: `Only ${availability.quantity} units available (requested: ${quantity})`,
      };
    }

    return {
      available: true,
      maxQuantity: availability.quantity,
    };
  } catch (error) {
    console.error('Failed to check gear inventory:', error);
    // Return optimistic result if check fails
    return {
      available: true,
      maxQuantity: quantity,
      message: 'Unable to verify inventory. Proceeding optimistically.',
    };
  }
}

/**
 * Validates cart before checkout to prevent inventory conflicts
 */
export async function validateCheckoutInventory(cartItems: CartItem[]): Promise<{
  canProceed: boolean;
  issues: InventoryConflict[];
  warnings: string[];
}> {
  const validation = await validateCartInventory(cartItems);
  const warnings: string[] = [];

  // Check for items that might have stale data
  const staleThreshold = 5 * 60 * 1000; // 5 minutes
  const now = new Date().getTime();

  for (const item of cartItems) {
    const addedTime = new Date(item.addedAt).getTime();
    if (now - addedTime > staleThreshold) {
      warnings.push(
        `${item.gear.title} was added to cart ${Math.round((now - addedTime) / 60000)} minutes ago. Inventory may have changed.`
      );
    }
  }

  return {
    canProceed: validation.isValid,
    issues: validation.conflicts,
    warnings,
  };
}

/**
 * Refreshes cart items with latest inventory data
 */
export async function refreshCartInventory(cartItems: CartItem[]): Promise<{
  updatedItems: CartItem[];
  changes: { itemId: string; field: string; oldValue: any; newValue: any }[];
}> {
  const updatedItems: CartItem[] = [];
  const changes: { itemId: string; field: string; oldValue: any; newValue: any }[] = [];

  for (const item of cartItems) {
    try {
      // Fetch latest gear data
      const latestGear = await shoppingService.getGearById(item.gearId);
      const updatedItem = { ...item, gear: latestGear };

      // Check for price changes
      const currentPrice = item.type === 'rent' ? latestGear.rentPrice : latestGear.price;
      if (currentPrice !== item.price) {
        changes.push({
          itemId: item.id,
          field: 'price',
          oldValue: item.price,
          newValue: currentPrice,
        });
        updatedItem.price = currentPrice || item.price;
      }

      // Check availability changes
      if (latestGear.availability.inStock !== item.gear.availability.inStock) {
        changes.push({
          itemId: item.id,
          field: 'availability',
          oldValue: item.gear.availability.inStock,
          newValue: latestGear.availability.inStock,
        });
      }

      updatedItems.push(updatedItem);
    } catch (error) {
      console.error(`Failed to refresh item ${item.id}:`, error);
      // Keep original item if refresh fails
      updatedItems.push(item);
    }
  }

  return { updatedItems, changes };
}