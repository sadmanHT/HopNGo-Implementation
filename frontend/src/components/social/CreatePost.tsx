'use client';

import React, { useState, useRef } from 'react';
import { Card, CardContent } from '../ui/card';
import { Button } from '../ui/button';
import { Textarea } from '../ui/textarea';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { useSocialStore } from '../../lib/state';
import { imageUploadService, compressImage, UploadProgress } from '../../services/imageUpload';
import { MapPin, Image as ImageIcon, X, Loader2, Tag } from 'lucide-react';
import Image from 'next/image';

interface Location {
  latitude: number;
  longitude: number;
  address: string;
}

interface CreatePostProps {
  onPostCreated?: () => void;
  className?: string;
}

export const CreatePost: React.FC<CreatePostProps> = ({ onPostCreated, className }) => {
  const { createPost, isCreatingPost } = useSocialStore();
  const [content, setContent] = useState('');
  const [selectedImages, setSelectedImages] = useState<File[]>([]);
  const [imagePreviewUrls, setImagePreviewUrls] = useState<string[]>([]);
  const [uploadProgress, setUploadProgress] = useState<Record<string, number>>({});
  const [location, setLocation] = useState<Location | null>(null);
  const [isGettingLocation, setIsGettingLocation] = useState(false);
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [error, setError] = useState<string | null>(null);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const maxImages = 4;
  const maxContentLength = 500;

  const handleImageSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    
    if (selectedImages.length + files.length > maxImages) {
      setError(`You can only upload up to ${maxImages} images`);
      return;
    }

    // Validate and compress images
    const processImages = async () => {
      const validImages: File[] = [];
      const newPreviewUrls: string[] = [];

      for (const file of files) {
        try {
          // Validate file type and size
          if (!file.type.startsWith('image/')) {
            setError('Please select only image files');
            continue;
          }

          if (file.size > 10 * 1024 * 1024) { // 10MB limit
            setError('Image size should be less than 10MB');
            continue;
          }

          // Compress image
          const compressedFile = await compressImage(file, 1920, 0.8);
          validImages.push(compressedFile);
          
          // Create preview URL
          const previewUrl = URL.createObjectURL(compressedFile);
          newPreviewUrls.push(previewUrl);
        } catch (error) {
          console.error('Error processing image:', error);
          setError('Error processing image');
        }
      }

      setSelectedImages(prev => [...prev, ...validImages]);
      setImagePreviewUrls(prev => [...prev, ...newPreviewUrls]);
      setError(null);
    };

    processImages();
  };

  const removeImage = (index: number) => {
    // Revoke object URL to prevent memory leaks
    URL.revokeObjectURL(imagePreviewUrls[index]);
    
    setSelectedImages(prev => prev.filter((_, i) => i !== index));
    setImagePreviewUrls(prev => prev.filter((_, i) => i !== index));
  };

  const getCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by this browser');
      return;
    }

    setIsGettingLocation(true);
    setError(null);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords;
        
        try {
          // Reverse geocoding to get address
          const response = await fetch(
            `https://api.mapbox.com/geocoding/v5/mapbox.places/${longitude},${latitude}.json?access_token=${process.env.NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN}`
          );
          
          if (response.ok) {
            const data = await response.json();
            const address = data.features[0]?.place_name || `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
            
            setLocation({ latitude, longitude, address });
          } else {
            setLocation({
              latitude,
              longitude,
              address: `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
            });
          }
        } catch (error) {
          console.error('Error getting address:', error);
          setLocation({
            latitude,
            longitude,
            address: `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
          });
        } finally {
          setIsGettingLocation(false);
        }
      },
      (error) => {
        console.error('Error getting location:', error);
        setError('Unable to get your location. Please check your browser settings.');
        setIsGettingLocation(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000 // 5 minutes
      }
    );
  };

  const addTag = () => {
    const trimmedTag = tagInput.trim().toLowerCase();
    if (trimmedTag && !tags.includes(trimmedTag) && tags.length < 10) {
      setTags(prev => [...prev, trimmedTag]);
      setTagInput('');
    }
  };

  const removeTag = (tagToRemove: string) => {
    setTags(prev => prev.filter(tag => tag !== tagToRemove));
  };

  const handleTagInputKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      addTag();
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    
    if (!content.trim() && selectedImages.length === 0) {
      setError('Please add some content or images to your post');
      return;
    }

    if (content.length > maxContentLength) {
      setError(`Content must be less than ${maxContentLength} characters`);
      return;
    }

    setError(null);

    try {
      // Upload images first
      let mediaUrls: string[] = [];
      
      if (selectedImages.length > 0) {
        const uploadPromises = selectedImages.map(async (file, index) => {
          const fileName = `${file.name}_${index}`;
          
          return imageUploadService.uploadImage(file, {
            folder: 'social-posts',
            onProgress: (progress) => {
              setUploadProgress(prev => ({
                ...prev,
                [fileName]: progress.percentage
              }));
            }
          });
        });

        const uploadResults = await Promise.all(uploadPromises);
        mediaUrls = uploadResults.map(result => result.url);
      }

      // Create post
      await createPost({
        content: content.trim(),
        location: location ? {
          name: location.address,
          coordinates: [location.longitude, location.latitude] as [number, number],
          address: location.address
        } : undefined,
        mediaUrls,
        tags
      });

      // Reset form
      setContent('');
      setSelectedImages([]);
      setImagePreviewUrls(prev => {
        prev.forEach(url => URL.revokeObjectURL(url));
        return [];
      });
      setLocation(null);
      setTags([]);
      setTagInput('');
      setUploadProgress({});
      
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }

      onPostCreated?.();
    } catch (error) {
      console.error('Error creating post:', error);
      setError(error instanceof Error ? error.message : 'Failed to create post');
    }
  };

  const isUploading = Object.keys(uploadProgress).length > 0;
  const canSubmit = !isCreatingPost && !isUploading && (content.trim() || selectedImages.length > 0);

  return (
    <Card className={className}>
      <CardContent className="p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Content Input */}
          <div className="space-y-2">
            <Textarea
              placeholder="What's on your mind?"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="min-h-[100px] resize-none border-0 p-0 text-lg placeholder:text-muted-foreground focus-visible:ring-0"
              maxLength={maxContentLength}
            />
            <div className="flex justify-between items-center text-sm text-muted-foreground">
              <span>{content.length}/{maxContentLength}</span>
            </div>
          </div>

          {/* Image Previews */}
          {imagePreviewUrls.length > 0 && (
            <div className="grid grid-cols-2 gap-2">
              {imagePreviewUrls.map((url, index) => (
                <div key={index} className="relative group">
                  <div className="relative aspect-square rounded-lg overflow-hidden">
                    <Image
                      src={url}
                      alt={`Preview ${index + 1}`}
                      fill
                      className="object-cover"
                    />
                    {uploadProgress[`${selectedImages[index]?.name}_${index}`] && (
                      <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                        <div className="text-white text-sm">
                          {Math.round(uploadProgress[`${selectedImages[index]?.name}_${index}`])}%
                        </div>
                      </div>
                    )}
                  </div>
                  <Button
                    type="button"
                    variant="destructive"
                    size="sm"
                    className="absolute top-2 right-2 h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-opacity"
                    onClick={() => removeImage(index)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          )}

          {/* Location Display */}
          {location && (
            <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
              <MapPin className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm">{location.address}</span>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-6 w-6 p-0 ml-auto"
                onClick={() => setLocation(null)}
              >
                <X className="h-3 w-3" />
              </Button>
            </div>
          )}

          {/* Tags Display */}
          {tags.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {tags.map((tag) => (
                <div key={tag} className="flex items-center gap-1 bg-primary/10 text-primary px-2 py-1 rounded-full text-sm">
                  <Tag className="h-3 w-3" />
                  <span>#{tag}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-4 w-4 p-0 hover:bg-transparent"
                    onClick={() => removeTag(tag)}
                  >
                    <X className="h-2 w-2" />
                  </Button>
                </div>
              ))}
            </div>
          )}

          {/* Tag Input */}
          <div className="flex gap-2">
            <Input
              placeholder="Add tags (press Enter or comma to add)"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyPress={handleTagInputKeyPress}
              className="flex-1"
            />
            <Button type="button" variant="outline" size="sm" onClick={addTag} disabled={!tagInput.trim() || tags.length >= 10}>
              Add Tag
            </Button>
          </div>

          {/* Error Message */}
          {error && (
            <div className="text-sm text-destructive bg-destructive/10 p-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex items-center justify-between pt-4 border-t">
            <div className="flex items-center gap-2">
              {/* Image Upload */}
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                disabled={selectedImages.length >= maxImages}
                className="text-muted-foreground hover:text-foreground"
              >
                <ImageIcon className="h-4 w-4" />
                <span className="sr-only">Add images</span>
              </Button>
              
              {/* Location */}
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={getCurrentLocation}
                disabled={isGettingLocation}
                className="text-muted-foreground hover:text-foreground"
              >
                {isGettingLocation ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <MapPin className="h-4 w-4" />
                )}
                <span className="sr-only">Add location</span>
              </Button>
            </div>

            {/* Submit Button */}
            <Button
              type="submit"
              disabled={!canSubmit}
              className="min-w-[100px]"
            >
              {isCreatingPost ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  Posting...
                </>
              ) : (
                'Post'
              )}
            </Button>
          </div>

          {/* Hidden File Input */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            onChange={handleImageSelect}
            className="hidden"
          />
        </form>
      </CardContent>
    </Card>
  );
};