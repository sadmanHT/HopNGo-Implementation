'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../../../components/ui/card';
import { Button } from '../../../../../components/ui/button';
import { Input } from '../../../../../components/ui/input';
import { Badge } from '../../../../../components/ui/badge';
import { 
  Package, 
  Users, 
  DollarSign, 
  TrendingUp,
  Search,
  Filter,
  Plus,
  Edit,
  Trash2,
  Eye
} from 'lucide-react';
import { shoppingService, TravelGear } from '../../../../../services/shopping';
import { formatCurrency } from '../../../../../lib/utils';
import GearForm from '../../../../../components/admin/GearForm';

interface AdminStats {
  totalGear: number;
  totalUsers: number;
  totalRevenue: number;
  growthRate: number;
}

const GearManagement: React.FC = () => {
  const [gear, setGear] = useState<TravelGear[]>([]);
  const [filteredGear, setFilteredGear] = useState<TravelGear[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showGearForm, setShowGearForm] = useState(false);
  const [editingGear, setEditingGear] = useState<TravelGear | undefined>(undefined);
  const [stats, setStats] = useState<AdminStats>({
    totalGear: 0,
    totalUsers: 0,
    totalRevenue: 0,
    growthRate: 0
  });

  const categories = [
    'all',
    'backpacks',
    'tents', 
    'sleeping-bags',
    'hiking-boots',
    'camping-gear',
    'electronics',
    'clothing',
    'accessories'
  ];

  useEffect(() => {
    loadGear();
  }, []);

  useEffect(() => {
    filterGear();
  }, [gear, searchTerm, selectedCategory]);

  const loadGear = async () => {
    try {
      setIsLoading(true);
      setError(null);
      
      const gearResponse = await shoppingService.searchGear(
        '', // query string
        {
          category: ['all'],
          priceRange: { min: 0, max: 10000 },
          rating: 0
        }
      );
      
      const gearData = gearResponse.items;
      setGear(gearData);
      
      // Calculate stats
      const totalRevenue = gearData.reduce((sum, item) => sum + (item.price * (item.availability?.quantity || 0)), 0);
      setStats({
        totalGear: gearData.length,
        totalUsers: 1250, // Mock data
        totalRevenue,
        growthRate: 12.5 // Mock data
      });
      
    } catch (error) {
      console.error('Failed to load gear:', error);
      setError('Failed to load gear inventory');
    } finally {
      setIsLoading(false);
    }
  };

  const filterGear = () => {
    let filtered = gear;
    
    if (searchTerm) {
      filtered = filtered.filter(item => 
        item.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.brand?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.description.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    
    if (selectedCategory !== 'all') {
      filtered = filtered.filter(item => item.category === selectedCategory);
    }
    
    setFilteredGear(filtered);
  };

  const handleDeleteGear = async (gearId: string) => {
    if (window.confirm('Are you sure you want to delete this gear item?')) {
      try {
        await shoppingService.deleteGear(gearId);
        setGear(prev => prev.filter(item => item.id !== gearId));
      } catch (error) {
        console.error('Failed to delete gear:', error);
        setError('Failed to delete gear item');
      }
    }
  };

  const handleAddGear = () => {
    setEditingGear(undefined);
    setShowGearForm(true);
  };

  const handleEditGear = (gearItem: TravelGear) => {
    setEditingGear(gearItem);
    setShowGearForm(true);
  };

  const handleGearSaved = (savedGear: TravelGear) => {
    if (editingGear) {
      // Update existing gear
      setGear(prev => prev.map(item => 
        item.id === savedGear.id ? savedGear : item
      ));
    } else {
      // Add new gear
      setGear(prev => [savedGear, ...prev]);
    }
    
    setShowGearForm(false);
    setEditingGear(undefined);
  };

  const handleCancelGearForm = () => {
    setShowGearForm(false);
    setEditingGear(undefined);
  };

  const getStockStatus = (stock: number) => {
    if (stock === 0) return { label: 'Out of Stock', variant: 'destructive' as const };
    if (stock < 10) return { label: 'Low Stock', variant: 'secondary' as const };
    return { label: 'In Stock', variant: 'default' as const };
  };

  if (showGearForm) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <GearForm
          gear={editingGear}
          onSave={handleGearSaved}
          onCancel={handleCancelGearForm}
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto p-6 space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Gear Management</h1>
            <p className="text-gray-600 mt-2">
              Manage your travel gear inventory, pricing, and availability.
            </p>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Total Gear Items</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.totalGear}</p>
                </div>
                <Package className="h-8 w-8 text-blue-600" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Active Users</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.totalUsers.toLocaleString()}</p>
                </div>
                <Users className="h-8 w-8 text-green-600" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Inventory Value</p>
                  <p className="text-2xl font-bold text-gray-900">{formatCurrency(stats.totalRevenue)}</p>
                </div>
                <DollarSign className="h-8 w-8 text-purple-600" />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Growth Rate</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.growthRate}%</p>
                </div>
                <TrendingUp className="h-8 w-8 text-orange-600" />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Gear Inventory Management */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Gear Inventory Management</CardTitle>
              <Button 
                onClick={handleAddGear}
                className="flex items-center gap-2"
              >
                <Plus className="h-4 w-4" />
                Add New Gear
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {/* Search and Filter Controls */}
            <div className="flex flex-col sm:flex-row gap-4 mb-6">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                  <Input
                    placeholder="Search gear by name, brand, or description..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Filter className="h-4 w-4 text-gray-500" />
                <select
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {categories.map(category => (
                    <option key={category} value={category}>
                      {category === 'all' ? 'All Categories' : 
                       category.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Error Message */}
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
                {error}
              </div>
            )}

            {/* Loading State */}
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <span className="ml-3 text-gray-600">Loading gear inventory...</span>
              </div>
            ) : (
              /* Gear Table */
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="text-left py-3 px-4 font-medium text-gray-900">Gear</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">Category</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">Price</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">Stock</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">Status</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-900">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredGear.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="text-center py-8 text-gray-500">
                          {searchTerm || selectedCategory !== 'all' 
                            ? 'No gear items match your search criteria.'
                            : 'No gear items found. Add your first gear item to get started.'}
                        </td>
                      </tr>
                    ) : (
                      filteredGear.map((item) => {
                        const stockStatus = getStockStatus(item.availability?.quantity || 0);
                        return (
                          <tr key={item.id} className="border-b border-gray-100 hover:bg-gray-50">
                            <td className="py-4 px-4">
                              <div className="flex items-center space-x-3">
                                <img
                                  src={item.images[0] || '/placeholder-gear.jpg'}
                                  alt={item.title}
                                  className="h-12 w-12 rounded-lg object-cover"
                                />
                                <div>
                                  <p className="font-medium text-gray-900">{item.title}</p>
                                  <p className="text-sm text-gray-500">{item.brand}</p>
                                </div>
                              </div>
                            </td>
                            <td className="py-4 px-4">
                              <Badge variant="outline">
                                {item.category.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                              </Badge>
                            </td>
                            <td className="py-4 px-4">
                              <div>
                                <p className="font-medium">{formatCurrency(item.price)}</p>
                                {item.rentPrice && (
                                  <p className="text-sm text-gray-500">
                                    {formatCurrency(item.rentPrice)}/day
                                  </p>
                                )}
                              </div>
                            </td>
                            <td className="py-4 px-4">
                              <span className="font-medium">{item.availability?.quantity || 0}</span>
                            </td>
                            <td className="py-4 px-4">
                              <Badge variant={stockStatus.variant}>
                                {stockStatus.label}
                              </Badge>
                            </td>
                            <td className="py-4 px-4">
                              <div className="flex items-center space-x-2">
                                <Button variant="ghost" size="sm">
                                  <Eye className="h-4 w-4" />
                                </Button>
                                <Button 
                                  variant="ghost" 
                                  size="sm"
                                  onClick={() => handleEditGear(item)}
                                >
                                  <Edit className="h-4 w-4" />
                                </Button>
                                <Button 
                                  variant="ghost" 
                                  size="sm" 
                                  onClick={() => handleDeleteGear(item.id)}
                                  className="text-red-600 hover:text-red-700"
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default GearManagement;