'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Alert, AlertDescription } from '../ui/alert';
import { Checkbox } from '../ui/checkbox';
import { Badge } from '../ui/badge';
import { 
  Save, 
  X, 
  Upload, 
  AlertCircle, 
  CheckCircle,
  Loader2,
  Plus,
  Trash2
} from 'lucide-react';
import { shoppingService, TravelGear } from '../../services/shopping';

interface GearFormProps {
  gear?: TravelGear;
  onSave: (gear: TravelGear) => void;
  onCancel: () => void;
}

interface GearFormData {
  name: string;
  description: string;
  category: string;
  price: number;
  rentPrice?: number;
  stock: number;
  image: string;
  features: string[];
  specifications: Record<string, string>;
  brand: string;
  weight?: string;
  dimensions?: string;
  material?: string;
  isRentable: boolean;
  isAvailable: boolean;
}

interface ValidationErrors {
  [key: string]: string;
}

const categories = [
  'backpacks',
  'tents', 
  'sleeping-bags',
  'hiking-boots',
  'camping-gear',
  'electronics',
  'clothing',
  'accessories'
];

const GearForm: React.FC<GearFormProps> = ({ gear, onSave, onCancel }) => {
  const [formData, setFormData] = useState<GearFormData>({
    name: '',
    description: '',
    category: 'backpacks',
    price: 0,
    rentPrice: 0,
    stock: 0,
    image: '',
    features: [],
    specifications: {},
    brand: '',
    weight: '',
    dimensions: '',
    material: '',
    isRentable: false,
    isAvailable: true
  });

  const [newFeature, setNewFeature] = useState('');
  const [newSpecKey, setNewSpecKey] = useState('');
  const [newSpecValue, setNewSpecValue] = useState('');
  const [isLoading, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});

  useEffect(() => {
    if (gear) {
      setFormData({
        name: gear.title,
        description: gear.description,
        category: gear.category,
        price: gear.price,
        rentPrice: gear.rentPrice || 0,
        stock: 0, // Default stock value
        image: gear.images?.[0] || '',
        features: [], // Default empty features
        specifications: gear.specifications || {},
        brand: gear.brand || '',
        weight: '', // Default empty weight
        dimensions: '', // Default empty dimensions
        material: '', // Default empty material
        isRentable: !!gear.rentPrice,
        isAvailable: true // Default available
      });
    }
  }, [gear]);

  const handleInputChange = (field: keyof GearFormData, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    
    // Clear validation error when user starts typing
    if (validationErrors[field]) {
      setValidationErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const addFeature = () => {
    if (newFeature.trim() && !formData.features.includes(newFeature.trim())) {
      setFormData(prev => ({
        ...prev,
        features: [...prev.features, newFeature.trim()]
      }));
      setNewFeature('');
    }
  };

  const removeFeature = (index: number) => {
    setFormData(prev => ({
      ...prev,
      features: prev.features.filter((_, i) => i !== index)
    }));
  };

  const addSpecification = () => {
    if (newSpecKey.trim() && newSpecValue.trim()) {
      setFormData(prev => ({
        ...prev,
        specifications: {
          ...prev.specifications,
          [newSpecKey.trim()]: newSpecValue.trim()
        }
      }));
      setNewSpecKey('');
      setNewSpecValue('');
    }
  };

  const removeSpecification = (key: string) => {
    setFormData(prev => {
      const newSpecs = { ...prev.specifications };
      delete newSpecs[key];
      return { ...prev, specifications: newSpecs };
    });
  };

  const validateForm = (): boolean => {
    const errors: ValidationErrors = {};

    if (!formData.name.trim()) {
      errors.name = 'Gear name is required';
    }

    if (!formData.description.trim()) {
      errors.description = 'Description is required';
    }

    if (!formData.category) {
      errors.category = 'Category is required';
    }

    if (formData.price <= 0) {
      errors.price = 'Price must be greater than 0';
    }

    if (formData.isRentable && (!formData.rentPrice || formData.rentPrice <= 0)) {
      errors.rentPrice = 'Rent price is required when item is rentable';
    }

    if (formData.stock < 0) {
      errors.stock = 'Stock cannot be negative';
    }

    if (!formData.image.trim()) {
      errors.image = 'Image URL is required';
    }

    if (!formData.brand.trim()) {
      errors.brand = 'Brand is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(false);

    try {
      const gearData: Partial<TravelGear> = {
        title: formData.name,
        description: formData.description,
        category: formData.category as 'gear' | 'equipment' | 'accessories',
        price: formData.price,
        rentPrice: formData.isRentable ? formData.rentPrice : undefined,
        // stock: formData.stock, // Not available in TravelGear type
        images: formData.image ? [formData.image] : [],
        // features: formData.features, // Not available in TravelGear type
        specifications: formData.specifications,
        brand: formData.brand,
        // weight: formData.weight || undefined, // Not available in TravelGear type
        // dimensions: formData.dimensions || undefined, // Not available in TravelGear type
        // material: formData.material || undefined, // Not available in TravelGear type
        // isAvailable: formData.isAvailable, // Not available in TravelGear type
        ratings: gear?.ratings || { average: 0, count: 0, breakdown: {} },
        // reviewCount: gear?.reviewCount || 0 // Not available in TravelGear type
      };

      let savedGear: TravelGear;
      
      if (gear) {
        // Update existing gear
        savedGear = await shoppingService.updateGear(gear.id, gearData);
      } else {
        // Create new gear
        savedGear = await shoppingService.createGear(gearData);
      }

      setSuccess(true);
      setTimeout(() => {
        onSave(savedGear);
      }, 1500);

    } catch (error) {
      console.error('Failed to save gear:', error);
      setError('Failed to save gear. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  if (success) {
    return (
      <Card className="w-full max-w-2xl mx-auto">
        <CardContent className="p-8 text-center">
          <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">
            {gear ? 'Gear Updated!' : 'Gear Created!'}
          </h2>
          <p className="text-gray-600">
            {gear ? 'The gear item has been successfully updated.' : 'The new gear item has been successfully created.'}
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="w-full max-w-4xl mx-auto">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{gear ? 'Edit Gear' : 'Add New Gear'}</CardTitle>
          <Button variant="ghost" onClick={onCancel}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="name">Gear Name *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => handleInputChange('name', e.target.value)}
                className={validationErrors.name ? 'border-red-500' : ''}
              />
              {validationErrors.name && (
                <p className="text-sm text-red-500">{validationErrors.name}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="brand">Brand *</Label>
              <Input
                id="brand"
                value={formData.brand}
                onChange={(e) => handleInputChange('brand', e.target.value)}
                className={validationErrors.brand ? 'border-red-500' : ''}
              />
              {validationErrors.brand && (
                <p className="text-sm text-red-500">{validationErrors.brand}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description *</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => handleInputChange('description', e.target.value)}
              rows={3}
              className={validationErrors.description ? 'border-red-500' : ''}
            />
            {validationErrors.description && (
              <p className="text-sm text-red-500">{validationErrors.description}</p>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="space-y-2">
              <Label htmlFor="category">Category *</Label>
              <Select value={formData.category} onValueChange={(value) => handleInputChange('category', value)}>
                <SelectTrigger className={validationErrors.category ? 'border-red-500' : ''}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {categories.map(category => (
                    <SelectItem key={category} value={category}>
                      {category.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {validationErrors.category && (
                <p className="text-sm text-red-500">{validationErrors.category}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="price">Price ($) *</Label>
              <Input
                id="price"
                type="number"
                min="0"
                step="0.01"
                value={formData.price}
                onChange={(e) => handleInputChange('price', parseFloat(e.target.value) || 0)}
                className={validationErrors.price ? 'border-red-500' : ''}
              />
              {validationErrors.price && (
                <p className="text-sm text-red-500">{validationErrors.price}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="stock">Stock Quantity *</Label>
              <Input
                id="stock"
                type="number"
                min="0"
                value={formData.stock}
                onChange={(e) => handleInputChange('stock', parseInt(e.target.value) || 0)}
                className={validationErrors.stock ? 'border-red-500' : ''}
              />
              {validationErrors.stock && (
                <p className="text-sm text-red-500">{validationErrors.stock}</p>
              )}
            </div>
          </div>

          {/* Rental Options */}
          <div className="space-y-4">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="isRentable"
                checked={formData.isRentable}
                onCheckedChange={(checked) => handleInputChange('isRentable', checked)}
              />
              <Label htmlFor="isRentable">Available for rental</Label>
            </div>

            {formData.isRentable && (
              <div className="space-y-2">
                <Label htmlFor="rentPrice">Rent Price ($/day) *</Label>
                <Input
                  id="rentPrice"
                  type="number"
                  min="0"
                  step="0.01"
                  value={formData.rentPrice}
                  onChange={(e) => handleInputChange('rentPrice', parseFloat(e.target.value) || 0)}
                  className={validationErrors.rentPrice ? 'border-red-500' : ''}
                />
                {validationErrors.rentPrice && (
                  <p className="text-sm text-red-500">{validationErrors.rentPrice}</p>
                )}
              </div>
            )}
          </div>

          {/* Image */}
          <div className="space-y-2">
            <Label htmlFor="image">Image URL *</Label>
            <Input
              id="image"
              value={formData.image}
              onChange={(e) => handleInputChange('image', e.target.value)}
              placeholder="https://example.com/image.jpg"
              className={validationErrors.image ? 'border-red-500' : ''}
            />
            {validationErrors.image && (
              <p className="text-sm text-red-500">{validationErrors.image}</p>
            )}
            {formData.image && (
              <div className="mt-2">
                <img
                  src={formData.image}
                  alt="Preview"
                  className="h-32 w-32 object-cover rounded-lg border"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                  }}
                />
              </div>
            )}
          </div>

          {/* Physical Specifications */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="space-y-2">
              <Label htmlFor="weight">Weight</Label>
              <Input
                id="weight"
                value={formData.weight}
                onChange={(e) => handleInputChange('weight', e.target.value)}
                placeholder="e.g., 2.5 lbs"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="dimensions">Dimensions</Label>
              <Input
                id="dimensions"
                value={formData.dimensions}
                onChange={(e) => handleInputChange('dimensions', e.target.value)}
                placeholder="e.g., 20x12x8 inches"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="material">Material</Label>
              <Input
                id="material"
                value={formData.material}
                onChange={(e) => handleInputChange('material', e.target.value)}
                placeholder="e.g., Ripstop Nylon"
              />
            </div>
          </div>

          {/* Features */}
          <div className="space-y-4">
            <Label>Features</Label>
            <div className="flex gap-2">
              <Input
                value={newFeature}
                onChange={(e) => setNewFeature(e.target.value)}
                placeholder="Add a feature..."
                onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addFeature())}
              />
              <Button type="button" onClick={addFeature}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            <div className="flex flex-wrap gap-2">
              {formData.features.map((feature, index) => (
                <Badge key={index} variant="secondary" className="flex items-center gap-1">
                  {feature}
                  <button
                    type="button"
                    onClick={() => removeFeature(index)}
                    className="ml-1 hover:text-red-600"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
            </div>
          </div>

          {/* Specifications */}
          <div className="space-y-4">
            <Label>Specifications</Label>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
              <Input
                value={newSpecKey}
                onChange={(e) => setNewSpecKey(e.target.value)}
                placeholder="Specification name..."
              />
              <Input
                value={newSpecValue}
                onChange={(e) => setNewSpecValue(e.target.value)}
                placeholder="Specification value..."
              />
              <Button type="button" onClick={addSpecification}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            <div className="space-y-2">
              {Object.entries(formData.specifications).map(([key, value]) => (
                <div key={key} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                  <span><strong>{key}:</strong> {value}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => removeSpecification(key)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>
          </div>

          {/* Availability */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="isAvailable"
              checked={formData.isAvailable}
              onCheckedChange={(checked) => handleInputChange('isAvailable', checked)}
            />
            <Label htmlFor="isAvailable">Available for purchase/rental</Label>
          </div>

          {/* Error Alert */}
          {error && (
            <Alert className="border-red-200 bg-red-50">
              <AlertCircle className="h-4 w-4 text-red-600" />
              <AlertDescription className="text-red-800">{error}</AlertDescription>
            </Alert>
          )}

          {/* Submit Buttons */}
          <div className="flex justify-end space-x-4">
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Saving...
                </>
              ) : (
                <>
                  <Save className="mr-2 h-4 w-4" />
                  {gear ? 'Update Gear' : 'Create Gear'}
                </>
              )}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default GearForm;