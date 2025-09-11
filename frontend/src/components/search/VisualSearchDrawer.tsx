'use client';

import { useState, useRef, useCallback } from 'react';
import { Camera, Upload, X, Search, Grid, List, Filter, TrendingUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { cn } from '@/lib/utils';
import Image from 'next/image';
import { useSearchStore, SearchResult } from '@/lib/state/search';
import { useFeatureFlag } from '@/lib/flags';

interface VisualSearchDrawerProps {
  className?: string;
}

interface AnalyticsEvent {
  type: 'image_upload' | 'search_performed' | 'result_clicked' | 'view_changed';
  timestamp: Date;
  metadata?: Record<string, any>;
}

export function VisualSearchDrawer({ className }: VisualSearchDrawerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [analytics, setAnalytics] = useState<AnalyticsEvent[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dropZoneRef = useRef<HTMLDivElement>(null);
  
  const { isEnabled: isVisualSearchEnabled } = useFeatureFlag('visual_search_v2');
  const { results, isLoading, search } = useSearchStore();

  // Analytics tracking
  const trackEvent = useCallback((type: AnalyticsEvent['type'], metadata?: Record<string, any>) => {
    const event: AnalyticsEvent = {
      type,
      timestamp: new Date(),
      metadata
    };
    setAnalytics(prev => [...prev, event]);
    
    // In production, send to analytics service
    console.log('Visual Search Analytics:', event);
  }, []);

  // Drag and drop handlers
  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!dropZoneRef.current?.contains(e.relatedTarget as Node)) {
      setIsDragging(false);
    }
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files);
    const imageFile = files.find(file => file.type.startsWith('image/'));
    
    if (imageFile) {
      handleImageSelect(imageFile);
      trackEvent('image_upload', { method: 'drag_drop', fileSize: imageFile.size });
    }
  }, [trackEvent]);

  const handleImageSelect = (file: File) => {
    if (file && file.type.startsWith('image/')) {
      setSelectedImage(file);
      
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      handleImageSelect(file);
      trackEvent('image_upload', { method: 'file_input', fileSize: file.size });
    }
  };

  const handleVisualSearch = async () => {
    if (!selectedImage) return;
    
    setIsAnalyzing(true);
    trackEvent('search_performed', { imageSize: selectedImage.size });
    
    try {
      // Simulate visual search API call
      const formData = new FormData();
      formData.append('image', selectedImage);
      
      // Mock search terms based on image analysis
      const mockSearchTerms = [
        'beach resort tropical paradise',
        'mountain cabin forest retreat', 
        'city skyline urban adventure',
        'historic architecture cultural site',
        'countryside villa peaceful getaway'
      ];
      
      const randomTerm = mockSearchTerms[Math.floor(Math.random() * mockSearchTerms.length)];
      await search(randomTerm, {});
      
    } catch (error) {
      console.error('Visual search failed:', error);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleResultClick = (result: SearchResult) => {
    trackEvent('result_clicked', { 
      resultId: result.id, 
      resultType: result.type,
      score: result.score 
    });
  };

  const handleViewModeChange = (mode: 'grid' | 'list') => {
    setViewMode(mode);
    trackEvent('view_changed', { viewMode: mode });
  };

  const handleClear = () => {
    setSelectedImage(null);
    setImagePreview(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  if (!isVisualSearchEnabled) {
    return null;
  }

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        <Button 
          variant="outline" 
          size="sm" 
          className={cn("h-10", className)}
          title="Visual Search"
        >
          <Camera className="h-4 w-4 mr-2" />
          Visual Search
        </Button>
      </SheetTrigger>
      
      <SheetContent side="right" className="w-full sm:w-[600px] lg:w-[800px] p-0">
        <div className="flex flex-col h-full">
          <SheetHeader className="p-6 border-b">
            <SheetTitle className="flex items-center space-x-2">
              <Camera className="h-5 w-5 text-blue-600" />
              <span>Visual Search</span>
              <Badge variant="secondary" className="ml-2">Beta</Badge>
            </SheetTitle>
          </SheetHeader>
          
          <div className="flex-1 overflow-y-auto">
            <div className="p-6 space-y-6">
              {/* Image Upload Section */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Upload Image</CardTitle>
                </CardHeader>
                <CardContent>
                  {!imagePreview ? (
                    <div 
                      ref={dropZoneRef}
                      onClick={triggerFileInput}
                      onDragEnter={handleDragEnter}
                      onDragLeave={handleDragLeave}
                      onDragOver={handleDragOver}
                      onDrop={handleDrop}
                      className={cn(
                        "border-2 border-dashed rounded-lg p-8 cursor-pointer transition-colors text-center",
                        isDragging 
                          ? "border-blue-400 bg-blue-50" 
                          : "border-gray-300 hover:border-blue-400"
                      )}
                    >
                      <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                      <p className="text-sm text-gray-600 mb-2">
                        {isDragging 
                          ? "Drop your image here" 
                          : "Drag & drop an image or click to browse"
                        }
                      </p>
                      <p className="text-xs text-gray-500">
                        JPG, PNG, or GIF up to 10MB
                      </p>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="relative">
                        <Image
                          src={imagePreview}
                          alt="Selected image"
                          width={400}
                          height={300}
                          className="rounded-lg object-cover w-full h-64"
                        />
                        <Button
                          onClick={handleClear}
                          variant="outline"
                          size="sm"
                          className="absolute top-2 right-2 h-8 w-8 p-0"
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>
                      
                      <Button 
                        onClick={handleVisualSearch}
                        disabled={isAnalyzing}
                        className="w-full"
                      >
                        {isAnalyzing ? (
                          <>
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                            Analyzing Image...
                          </>
                        ) : (
                          <>
                            <Search className="h-4 w-4 mr-2" />
                            Find Similar Places
                          </>
                        )}
                      </Button>
                    </div>
                  )}
                  
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileSelect}
                    className="hidden"
                  />
                </CardContent>
              </Card>

              {/* Results Section */}
              {results.length > 0 && (
                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="text-lg">
                        Search Results ({results.length})
                      </CardTitle>
                      <div className="flex items-center space-x-2">
                        <Button
                          variant={viewMode === 'grid' ? 'default' : 'outline'}
                          size="sm"
                          onClick={() => handleViewModeChange('grid')}
                        >
                          <Grid className="h-4 w-4" />
                        </Button>
                        <Button
                          variant={viewMode === 'list' ? 'default' : 'outline'}
                          size="sm"
                          onClick={() => handleViewModeChange('list')}
                        >
                          <List className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className={cn(
                      viewMode === 'grid' 
                        ? "grid grid-cols-1 sm:grid-cols-2 gap-4" 
                        : "space-y-4"
                    )}>
                      {results.slice(0, 12).map((result) => (
                        <div 
                          key={result.id}
                          onClick={() => handleResultClick(result)}
                          className="cursor-pointer hover:shadow-md transition-shadow"
                        >
                          <Card className="h-full">
                            <CardContent className="p-4">
                              <div className="flex items-start space-x-3">
                                {result.images?.[0] && (
                                  <div className="relative w-16 h-16 flex-shrink-0">
                                    <Image
                                      src={result.images[0]}
                                      alt={result.title}
                                      fill
                                      className="object-cover rounded"
                                    />
                                  </div>
                                )}
                                <div className="flex-1 min-w-0">
                                  <h4 className="font-medium text-sm truncate">
                                    {result.title}
                                  </h4>
                                  <p className="text-xs text-muted-foreground line-clamp-2">
                                    {result.content}
                                  </p>
                                  <div className="flex items-center justify-between mt-2">
                                    <Badge variant="outline" className="text-xs">
                                      {result.type}
                                    </Badge>
                                    {result.score && (
                                      <Badge variant="secondary" className="text-xs">
                                        {Math.round(result.score * 100)}% match
                                      </Badge>
                                    )}
                                  </div>
                                </div>
                              </div>
                            </CardContent>
                          </Card>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Analytics Section */}
              {analytics.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg flex items-center space-x-2">
                      <TrendingUp className="h-5 w-5" />
                      <span>Search Analytics</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <span className="text-muted-foreground">Total Searches:</span>
                          <span className="ml-2 font-medium">
                            {analytics.filter(e => e.type === 'search_performed').length}
                          </span>
                        </div>
                        <div>
                          <span className="text-muted-foreground">Images Uploaded:</span>
                          <span className="ml-2 font-medium">
                            {analytics.filter(e => e.type === 'image_upload').length}
                          </span>
                        </div>
                        <div>
                          <span className="text-muted-foreground">Results Clicked:</span>
                          <span className="ml-2 font-medium">
                            {analytics.filter(e => e.type === 'result_clicked').length}
                          </span>
                        </div>
                        <div>
                          <span className="text-muted-foreground">View Changes:</span>
                          <span className="ml-2 font-medium">
                            {analytics.filter(e => e.type === 'view_changed').length}
                          </span>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}