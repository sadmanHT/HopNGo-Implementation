'use client';

import React from 'react';
import { AlertTriangle, X, Minus, Trash2 } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { useCartStore } from '../../lib/state/cart';
import { InventoryConflict } from '../../utils/inventoryValidator';

interface InventoryConflictNotificationProps {
  conflicts: InventoryConflict[];
  onClose?: () => void;
  className?: string;
}

export function InventoryConflictNotification({
  conflicts,
  onClose,
  className = ''
}: InventoryConflictNotificationProps) {
  const { resolveInventoryConflict, isUpdatingItem } = useCartStore();

  if (conflicts.length === 0) return null;

  const handleResolveConflict = async (itemId: string, action: 'remove' | 'reduce' | 'keep') => {
    await resolveInventoryConflict(itemId, action);
  };

  return (
    <Card className={`border-orange-200 bg-orange-50 ${className}`}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-orange-600" />
            <CardTitle className="text-lg text-orange-800">
              Inventory Conflicts Detected
            </CardTitle>
          </div>
          {onClose && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="h-8 w-8 p-0 text-orange-600 hover:bg-orange-100"
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
        <p className="text-sm text-orange-700">
          Some items in your cart are no longer available in the requested quantities.
        </p>
      </CardHeader>
      
      <CardContent className="space-y-4">
        {conflicts.map((conflict) => (
          <div
            key={conflict.itemId}
            className="flex items-center justify-between p-3 bg-white rounded-lg border border-orange-200"
          >
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <h4 className="font-medium text-gray-900">
                  {conflict.gearName}
                </h4>
                <Badge variant="outline" className="text-xs">
                  {conflict.type === 'rent' ? 'Rental' : 'Purchase'}
                </Badge>
              </div>
              
              <div className="text-sm text-gray-600 space-y-1">
                <p>
                  <span className="font-medium">Requested:</span> {conflict.requestedQuantity}
                </p>
                <p>
                  <span className="font-medium">Available:</span> {conflict.availableQuantity}
                </p>
                {conflict.reason && (
                  <p className="text-orange-600">
                    <span className="font-medium">Reason:</span> {conflict.reason}
                  </p>
                )}
              </div>
            </div>
            
            <div className="flex items-center gap-2 ml-4">
              {conflict.availableQuantity > 0 && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleResolveConflict(conflict.itemId, 'reduce')}
                  disabled={isUpdatingItem}
                  className="text-blue-600 border-blue-200 hover:bg-blue-50"
                >
                  <Minus className="h-4 w-4 mr-1" />
                  Reduce to {conflict.availableQuantity}
                </Button>
              )}
              
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleResolveConflict(conflict.itemId, 'remove')}
                disabled={isUpdatingItem}
                className="text-red-600 border-red-200 hover:bg-red-50"
              >
                <Trash2 className="h-4 w-4 mr-1" />
                Remove
              </Button>
              
              <Button
                variant="ghost"
                size="sm"
                onClick={() => handleResolveConflict(conflict.itemId, 'keep')}
                disabled={isUpdatingItem}
                className="text-gray-600 hover:bg-gray-100"
              >
                Keep
              </Button>
            </div>
          </div>
        ))}
        
        <div className="pt-2 border-t border-orange-200">
          <p className="text-xs text-orange-600">
            <strong>Note:</strong> Keeping items with conflicts may cause issues during checkout.
            We recommend resolving all conflicts before proceeding.
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

export default InventoryConflictNotification;