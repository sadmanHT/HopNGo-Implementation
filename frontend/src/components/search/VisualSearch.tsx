'use client';

import { useState, useRef } from 'react';
import { Camera, Upload, X, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import Image from 'next/image';

interface VisualSearchProps {
  onSearch: (imageFile: File, query?: string) => void;
  className?: string;
}

export function VisualSearch({ onSearch, className }: VisualSearchProps) {
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file && file.type.startsWith('image/')) {
      setSelectedImage(file);
      
      // Create preview URL
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSearch = async () => {
    if (!selectedImage) return;
    
    setIsAnalyzing(true);
    try {
      await onSearch(selectedImage);
    } finally {
      setIsAnalyzing(false);
    }
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

  return (
    <Card className={cn("w-full max-w-md", className)}>
      <CardContent className="p-6">
        <div className="text-center space-y-4">
          <div className="flex items-center justify-center space-x-2 mb-4">
            <Camera className="h-5 w-5 text-blue-600" />
            <h3 className="text-lg font-semibold">Visual Search</h3>
          </div>
          
          {!imagePreview ? (
            <div 
              onClick={triggerFileInput}
              className="border-2 border-dashed border-gray-300 rounded-lg p-8 cursor-pointer hover:border-blue-400 transition-colors"
            >
              <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p className="text-sm text-gray-600 mb-2">
                Upload an image to search for similar places
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
                  width={300}
                  height={200}
                  className="rounded-lg object-cover w-full h-48"
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
                onClick={handleSearch}
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
                    Search Similar Places
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
          
          {!imagePreview && (
            <Button 
              onClick={triggerFileInput}
              variant="outline"
              className="w-full"
            >
              <Upload className="h-4 w-4 mr-2" />
              Choose Image
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}