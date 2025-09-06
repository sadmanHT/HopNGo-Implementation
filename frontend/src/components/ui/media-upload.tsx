'use client';

import React, { useState, useRef, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Upload,
  X,
  RefreshCw,
  CheckCircle,
  AlertCircle,
  Image,
  Video,
  File
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { mediaAPI, SignedUploadRequest, MediaMeta } from '@/lib/api/media';
import { useAuthStore } from '@/lib/state';

export interface MediaUploadProps {
  accept?: string;
  maxSize?: number; // in bytes
  resourceType?: 'image' | 'video';
  folder?: string;
  onUploadComplete?: (media: MediaMeta) => void;
  onUploadError?: (error: string) => void;
  className?: string;
  multiple?: boolean;
}

interface UploadState {
  file: File | null;
  progress: number;
  status: 'idle' | 'uploading' | 'success' | 'error';
  error?: string;
  media?: MediaMeta;
  retryCount: number;
}

const MAX_RETRY_ATTEMPTS = 3;

export function MediaUpload({
  accept = 'image/*,video/*',
  maxSize = 50 * 1024 * 1024, // 50MB default
  resourceType,
  folder,
  onUploadComplete,
  onUploadError,
  className,
  multiple = false
}: MediaUploadProps) {
  const { token } = useAuthStore();
  const [uploads, setUploads] = useState<UploadState[]>([]);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const detectResourceType = (file: File): 'image' | 'video' => {
    if (resourceType) return resourceType;
    return file.type.startsWith('image/') ? 'image' : 'video';
  };

  const validateFile = (file: File): string | null => {
    if (file.size > maxSize) {
      return `File size exceeds ${Math.round(maxSize / (1024 * 1024))}MB limit`;
    }
    
    const detectedType = detectResourceType(file);
    if (detectedType === 'image' && !file.type.startsWith('image/')) {
      return 'Please select an image file';
    }
    if (detectedType === 'video' && !file.type.startsWith('video/')) {
      return 'Please select a video file';
    }
    
    return null;
  };

  const uploadFile = useCallback(async (file: File, uploadIndex: number) => {
    if (!token) {
      setUploads(prev => prev.map((upload, i) => 
        i === uploadIndex ? { ...upload, status: 'error', error: 'Authentication required' } : upload
      ));
      return;
    }

    const validation = validateFile(file);
    if (validation) {
      setUploads(prev => prev.map((upload, i) => 
        i === uploadIndex ? { ...upload, status: 'error', error: validation } : upload
      ));
      return;
    }

    try {
      // Update status to uploading
      setUploads(prev => prev.map((upload, i) => 
        i === uploadIndex ? { ...upload, status: 'uploading', progress: 0, error: undefined } : upload
      ));

      // Get signed upload URL
      const signedUpload = await mediaAPI.getSignedUpload(token, {
        resourceType: detectResourceType(file),
        fileSize: file.size,
        folder
      });

      // Upload to Cloudinary
      const uploadResult = await mediaAPI.uploadToCloudinary(
        file,
        signedUpload,
        (progress) => {
          setUploads(prev => prev.map((upload, i) => 
            i === uploadIndex ? { ...upload, progress } : upload
          ));
        }
      );

      // Save metadata
      const media = await mediaAPI.saveMediaMeta(token, uploadResult);

      // Update status to success
      setUploads(prev => prev.map((upload, i) => 
        i === uploadIndex ? { ...upload, status: 'success', progress: 100, media } : upload
      ));

      onUploadComplete?.(media);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Upload failed';
      
      setUploads(prev => prev.map((upload, i) => 
        i === uploadIndex ? { ...upload, status: 'error', error: errorMessage } : upload
      ));
      
      onUploadError?.(errorMessage);
    }
  }, [token, maxSize, resourceType, folder, onUploadComplete, onUploadError]);

  const retryUpload = useCallback(async (uploadIndex: number) => {
    const upload = uploads[uploadIndex];
    if (!upload.file || upload.retryCount >= MAX_RETRY_ATTEMPTS) return;

    setUploads(prev => prev.map((upload, i) => 
      i === uploadIndex ? { ...upload, retryCount: upload.retryCount + 1 } : upload
    ));

    await uploadFile(upload.file, uploadIndex);
  }, [uploads, uploadFile]);

  const handleFileSelect = useCallback((files: FileList) => {
    const fileArray = Array.from(files);
    const newUploads: UploadState[] = fileArray.map(file => ({
      file,
      progress: 0,
      status: 'idle',
      retryCount: 0
    }));

    if (multiple) {
      setUploads(prev => [...prev, ...newUploads]);
    } else {
      setUploads(newUploads);
    }

    // Start uploads
    const startIndex = multiple ? uploads.length : 0;
    fileArray.forEach((file, index) => {
      uploadFile(file, startIndex + index);
    });
  }, [multiple, uploads.length, uploadFile]);

  const removeUpload = useCallback((index: number) => {
    setUploads(prev => prev.filter((_, i) => i !== index));
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileSelect(files);
    }
  }, [handleFileSelect]);

  const getFileIcon = (file: File) => {
    if (file.type.startsWith('image/')) return <Image className="h-4 w-4" />;
    if (file.type.startsWith('video/')) return <Video className="h-4 w-4" />;
    return <File className="h-4 w-4" />;
  };

  const getStatusIcon = (status: UploadState['status']) => {
    switch (status) {
      case 'uploading':
        return <RefreshCw className="h-4 w-4 animate-spin" />;
      case 'success':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'error':
        return <AlertCircle className="h-4 w-4 text-red-500" />;
      default:
        return null;
    }
  };

  return (
    <div className={cn('space-y-4', className)}>
      {/* Upload Area */}
      <Card
        className={cn(
          'border-2 border-dashed transition-colors cursor-pointer',
          isDragOver ? 'border-primary bg-primary/5' : 'border-muted-foreground/25',
          'hover:border-primary/50'
        )}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <CardContent className="flex flex-col items-center justify-center py-8 text-center">
          <Upload className={cn('h-8 w-8 mb-4', isDragOver ? 'text-primary' : 'text-muted-foreground')} />
          <p className="text-sm font-medium mb-2">
            {isDragOver ? 'Drop files here' : 'Click to upload or drag and drop'}
          </p>
          <p className="text-xs text-muted-foreground">
            {resourceType === 'image' ? 'Images only' : resourceType === 'video' ? 'Videos only' : 'Images and videos'}
            {' • Max size: '}{Math.round(maxSize / (1024 * 1024))}MB
          </p>
        </CardContent>
      </Card>

      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        multiple={multiple}
        className="hidden"
        onChange={(e) => e.target.files && handleFileSelect(e.target.files)}
      />

      {/* Upload List */}
      {uploads.length > 0 && (
        <div className="space-y-2">
          {uploads.map((upload, index) => (
            <Card key={index} className="p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center space-x-2">
                  {upload.file && getFileIcon(upload.file)}
                  <span className="text-sm font-medium truncate">
                    {upload.file?.name}
                  </span>
                  <Badge variant={upload.status === 'success' ? 'default' : upload.status === 'error' ? 'destructive' : 'secondary'}>
                    {upload.status}
                  </Badge>
                </div>
                <div className="flex items-center space-x-2">
                  {getStatusIcon(upload.status)}
                  {upload.status === 'error' && upload.retryCount < MAX_RETRY_ATTEMPTS && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => retryUpload(index)}
                    >
                      <RefreshCw className="h-3 w-3 mr-1" />
                      Retry
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => removeUpload(index)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </div>
              </div>
              
              {upload.status === 'uploading' && (
                <Progress value={upload.progress} className="mb-2" />
              )}
              
              {upload.error && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>{upload.error}</AlertDescription>
                </Alert>
              )}
              
              {upload.media && (
                <div className="text-xs text-muted-foreground">
                  {upload.media.width && upload.media.height && (
                    <span>{upload.media.width}×{upload.media.height} • </span>
                  )}
                  {Math.round(upload.media.bytes / 1024)}KB • {upload.media.format.toUpperCase()}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}