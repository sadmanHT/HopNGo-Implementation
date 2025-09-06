const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080/api/v1';

export interface SignedUploadRequest {
  resourceType: 'image' | 'video';
  fileSize: number;
  folder?: string;
}

export interface SignedUploadResponse {
  signature: string;
  timestamp: string;
  apiKey: string;
  cloudName: string;
  uploadUrl: string;
  uploadPreset: string;
  params: Record<string, any>;
}

export interface MediaMeta {
  id: string;
  userId: string;
  publicId: string;
  url: string;
  secureUrl: string;
  width?: number;
  height?: number;
  bytes: number;
  format: string;
  duration?: number;
  resourceType: string;
  createdAt: string;
  updatedAt: string;
}

export interface QuotaResponse {
  remainingBytes: number;
  remainingMB: number;
  dailyLimitMB: number;
}

export interface TransformUrlRequest {
  publicId: string;
  resourceType: string;
  transformations: Record<string, any>;
}

export interface TransformUrlResponse {
  url: string;
}

class MediaAPI {
  private getHeaders(token: string) {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-ID': this.getUserIdFromToken(token)
    };
  }

  private getUserIdFromToken(token: string): string {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || payload.userId || '';
    } catch {
      return '';
    }
  }

  async getSignedUpload(token: string, request: SignedUploadRequest): Promise<SignedUploadResponse> {
    const response = await fetch(`${API_BASE_URL}/social/media/sign-upload`, {
      method: 'POST',
      headers: this.getHeaders(token),
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to get signed upload URL');
    }

    return response.json();
  }

  async saveMediaMeta(token: string, uploadResult: any): Promise<MediaMeta> {
    const response = await fetch(`${API_BASE_URL}/social/media/save-meta`, {
      method: 'POST',
      headers: this.getHeaders(token),
      body: JSON.stringify(uploadResult)
    });

    if (!response.ok) {
      throw new Error('Failed to save media metadata');
    }

    return response.json();
  }

  async getQuota(token: string): Promise<QuotaResponse> {
    const response = await fetch(`${API_BASE_URL}/social/media/quota`, {
      method: 'GET',
      headers: this.getHeaders(token)
    });

    if (!response.ok) {
      throw new Error('Failed to get quota information');
    }

    return response.json();
  }

  async getUserMedia(token: string): Promise<MediaMeta[]> {
    const response = await fetch(`${API_BASE_URL}/social/media/my-media`, {
      method: 'GET',
      headers: this.getHeaders(token)
    });

    if (!response.ok) {
      throw new Error('Failed to get user media');
    }

    return response.json();
  }

  async deleteMedia(token: string, publicId: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/social/media/${encodeURIComponent(publicId)}`, {
      method: 'DELETE',
      headers: this.getHeaders(token)
    });

    if (!response.ok) {
      throw new Error('Failed to delete media');
    }
  }

  async getTransformedUrl(request: TransformUrlRequest): Promise<TransformUrlResponse> {
    const response = await fetch(`${API_BASE_URL}/social/media/transform-url`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      throw new Error('Failed to get transformed URL');
    }

    return response.json();
  }

  // Direct upload to Cloudinary
  async uploadToCloudinary(
    file: File,
    signedUpload: SignedUploadResponse,
    onProgress?: (progress: number) => void
  ): Promise<any> {
    return new Promise((resolve, reject) => {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('api_key', signedUpload.apiKey);
      formData.append('timestamp', signedUpload.timestamp);
      formData.append('signature', signedUpload.signature);
      formData.append('upload_preset', signedUpload.uploadPreset);
      
      // Add additional params
      Object.entries(signedUpload.params).forEach(([key, value]) => {
        if (key !== 'timestamp' && key !== 'upload_preset') {
          formData.append(key, String(value));
        }
      });

      const xhr = new XMLHttpRequest();
      
      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable && onProgress) {
          const progress = (event.loaded / event.total) * 100;
          onProgress(progress);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          try {
            const result = JSON.parse(xhr.responseText);
            resolve(result);
          } catch (error) {
            reject(new Error('Invalid response from Cloudinary'));
          }
        } else {
          reject(new Error(`Upload failed with status ${xhr.status}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Upload failed'));
      });

      xhr.open('POST', signedUpload.uploadUrl);
      xhr.send(formData);
    });
  }
}

export const mediaAPI = new MediaAPI();