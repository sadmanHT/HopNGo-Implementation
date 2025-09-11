'use client';

import { useState } from 'react';
import { MediaUpload } from '@/components/ui/media-upload';
import { mediaAPI, MediaMeta } from '@/lib/api/media';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Trash2, Download, Eye } from 'lucide-react';
import { toast } from '@/components/ui/toast';
import { OptimizedImage, getOptimizedCloudinaryUrl } from '@/components/ui/optimized-image';
import { VideoPlayer, getVideoPosterUrl } from '@/components/ui/video-player';

export default function MediaTestPage() {
  const [userMedia, setUserMedia] = useState<MediaMeta[]>([]);
  const [quota, setQuota] = useState<{ used: number; limit: number } | null>(null);
  const [loading, setLoading] = useState(false);

  const handleUploadSuccess = async (mediaMeta: MediaMeta) => {
    toast.success('Media uploaded successfully!');
    await loadUserMedia();
    await loadQuota();
  };

  const handleUploadError = (error: string) => {
    toast.error(`Upload failed: ${error}`);
  };

  const loadUserMedia = async () => {
    try {
      setLoading(true);
      // For demo purposes, using a mock token - in real app, get from auth context
      const token = 'demo-token';
      const media = await mediaAPI.getUserMedia(token);
      setUserMedia(media);
    } catch (error) {
      toast.error('Failed to load user media');
    } finally {
      setLoading(false);
    }
  };

  const loadQuota = async () => {
    try {
      // For demo purposes, using a mock token - in real app, get from auth context
      const token = 'demo-token';
      const quotaInfo = await mediaAPI.getQuota(token);
      setQuota({ used: quotaInfo.dailyLimitMB * 1024 * 1024 - quotaInfo.remainingBytes, limit: quotaInfo.dailyLimitMB * 1024 * 1024 });
    } catch (error) {
      toast.error('Failed to load quota information');
    }
  };

  const handleDelete = async (publicId: string) => {
    try {
      // For demo purposes, using a mock token - in real app, get from auth context
      const token = 'demo-token';
      await mediaAPI.deleteMedia(token, publicId);
      toast.success('Media deleted successfully');
      await loadUserMedia();
      await loadQuota();
    } catch (error) {
      toast.error('Failed to delete media');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getTransformedUrl = (url: string, transformations: Record<string, any>) => {
    return mediaAPI.getTransformedUrl({ publicId: url, resourceType: 'image', transformations }).then(res => res.url);
  };

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold">Media Upload Test</h1>
        <div className="flex gap-2">
          <Button onClick={loadUserMedia} disabled={loading}>
            Refresh Media
          </Button>
          <Button onClick={loadQuota} variant="outline">
            Check Quota
          </Button>
        </div>
      </div>

      {quota && (
        <Card>
          <CardHeader>
            <CardTitle>Daily Quota</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <div className="flex-1">
                <div className="w-full bg-gray-200 rounded-full h-2.5">
                  <div 
                    className="bg-blue-600 h-2.5 rounded-full" 
                    style={{ width: `${(quota.used / quota.limit) * 100}%` }}
                  ></div>
                </div>
              </div>
              <div className="text-sm text-gray-600">
                {formatFileSize(quota.used)} / {formatFileSize(quota.limit)}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Upload Media</CardTitle>
        </CardHeader>
        <CardContent>
          <MediaUpload
            onUploadComplete={handleUploadSuccess}
            onUploadError={handleUploadError}
            maxSize={50 * 1024 * 1024} // 50MB
            accept="image/*,video/*"
            folder="test-uploads"
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Your Media ({userMedia.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-8">Loading...</div>
          ) : userMedia.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              No media uploaded yet. Try uploading some files above!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {userMedia.map((media) => (
                <Card key={media.id} className="overflow-hidden">
                  <div className="aspect-video bg-gray-100 relative">
                    {media.resourceType === 'image' ? (
                      <OptimizedImage
                        src={getOptimizedCloudinaryUrl(media.secureUrl, {
                          width: 400,
                          height: 300,
                          crop: 'fill',
                          quality: 'auto',
                          format: 'auto'
                        })}
                        alt={`Uploaded media: ${media.publicId}`}
                        width={400}
                        height={300}
                        fill
                        sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                        className="rounded-t-lg"
                        placeholder="blur"
                        blurDataURL="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAhEAACAQMDBQAAAAAAAAAAAAABAgMABAUGIWGRkqGx0f/EABUBAQEAAAAAAAAAAAAAAAAAAAMF/8QAGhEAAgIDAAAAAAAAAAAAAAAAAAECEgMRkf/aAAwDAQACEQMRAD8AltJagyeH0AthI5xdrLcNM91BF5pX2HaH9bcfaSXWGaRmknyJckliyjqTzSlT54b6bk+h0R//2Q=="
                      />
                    ) : (
                      <VideoPlayer
                        src={media.secureUrl}
                        poster={getVideoPosterUrl(media.secureUrl, {
                          width: 400,
                          height: 300,
                          timeOffset: 1
                        })}
                        className="w-full h-full rounded-t-lg"
                        width={400}
                        height={300}
                        controls
                      />
                    )}
                    <Badge 
                      className="absolute top-2 right-2" 
                      variant={media.resourceType === 'image' ? 'default' : 'secondary'}
                    >
                      {media.resourceType}
                    </Badge>
                  </div>
                  <CardContent className="p-4">
                    <div className="space-y-2">
                      <div className="flex justify-between items-start">
                        <div className="text-sm font-medium truncate">
                          {media.publicId}
                        </div>
                      </div>
                      <div className="text-xs text-gray-500 space-y-1">
                        <div>Size: {formatFileSize(media.bytes)}</div>
                        <div>Format: {media.format}</div>
                        {media.width && media.height && (
                          <div>Dimensions: {media.width}×{media.height}</div>
                        )}
                        {media.duration && (
                          <div>Duration: {media.duration}s</div>
                        )}
                        <div>Uploaded: {new Date(media.createdAt).toLocaleDateString()}</div>
                      </div>
                      <div className="flex gap-2 pt-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => window.open(media.secureUrl, '_blank')}
                        >
                          <Eye className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            const a = document.createElement('a');
                            a.href = media.secureUrl;
                            a.download = media.publicId;
                            a.click();
                          }}
                        >
                          <Download className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => handleDelete(media.publicId)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}