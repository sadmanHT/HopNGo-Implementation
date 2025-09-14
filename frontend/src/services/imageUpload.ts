export interface UploadResult {
  url: string;
  publicId?: string;
  fileName: string;
  size: number;
  format: string;
}

export interface UploadProgress {
  loaded: number;
  total: number;
  percentage: number;
}

export interface UploadOptions {
  folder?: string;
  maxSize?: number; // in bytes
  allowedFormats?: string[];
  onProgress?: (progress: UploadProgress) => void;
}

// Cloudinary configuration
interface CloudinaryConfig {
  cloudName: string;
  uploadPreset: string;
  apiKey?: string;
}

// Firebase configuration
interface FirebaseConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
}

export class ImageUploadService {
  private provider: 'cloudinary' | 'firebase';
  private cloudinaryConfig?: CloudinaryConfig;
  private firebaseConfig?: FirebaseConfig;

  constructor(
    provider: 'cloudinary' | 'firebase' = 'cloudinary',
    config?: CloudinaryConfig | FirebaseConfig
  ) {
    this.provider = provider;
    
    if (provider === 'cloudinary') {
      this.cloudinaryConfig = config as CloudinaryConfig || {
        cloudName: process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || '',
        uploadPreset: process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || '',
        apiKey: process.env.NEXT_PUBLIC_CLOUDINARY_API_KEY,
      };
    } else {
      this.firebaseConfig = config as FirebaseConfig || {
        apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY || '',
        authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN || '',
        projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID || '',
        storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET || '',
        messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID || '',
        appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID || '',
      };
    }
  }

  async uploadImage(file: File, options: UploadOptions = {}): Promise<UploadResult> {
    // Validate file
    this.validateFile(file, options);

    if (this.provider === 'cloudinary') {
      return this.uploadToCloudinary(file, options);
    } else {
      return this.uploadToFirebase(file, options);
    }
  }

  async uploadMultipleImages(
    files: File[],
    options: UploadOptions = {}
  ): Promise<UploadResult[]> {
    const uploadPromises = files.map(file => this.uploadImage(file, options));
    return Promise.all(uploadPromises);
  }

  private validateFile(file: File, options: UploadOptions): void {
    const { maxSize = 5 * 1024 * 1024, allowedFormats = ['jpg', 'jpeg', 'png', 'gif', 'webp'] } = options;

    // Check file size
    if (file.size > maxSize) {
      throw new Error(`File size exceeds maximum allowed size of ${maxSize / (1024 * 1024)}MB`);
    }

    // Check file format
    const fileExtension = file.name.split('.').pop()?.toLowerCase();
    if (!fileExtension || !allowedFormats.includes(fileExtension)) {
      throw new Error(`File format not allowed. Allowed formats: ${allowedFormats.join(', ')}`);
    }

    // Check if it's actually an image
    if (!file.type.startsWith('image/')) {
      throw new Error('File must be an image');
    }
  }

  private async uploadToCloudinary(file: File, options: UploadOptions): Promise<UploadResult> {
    if (!this.cloudinaryConfig?.cloudName || !this.cloudinaryConfig?.uploadPreset) {
      throw new Error('Cloudinary configuration is missing');
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', this.cloudinaryConfig.uploadPreset);
    
    if (options.folder) {
      formData.append('folder', options.folder);
    }

    // Add transformation parameters for optimization
    formData.append('quality', 'auto');
    formData.append('fetch_format', 'auto');

    try {
      const response = await fetch(
        `https://api.cloudinary.com/v1_1/${this.cloudinaryConfig.cloudName}/image/upload`,
        {
          method: 'POST',
          body: formData,
        }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error?.message || 'Upload failed');
      }

      const result = await response.json();
      
      return {
        url: result.secure_url,
        publicId: result.public_id,
        fileName: file.name,
        size: result.bytes,
        format: result.format,
      };
    } catch (error) {
      console.error('Cloudinary upload error:', error);
      throw new Error('Failed to upload image to Cloudinary');
    }
  }

  private async uploadToFirebase(file: File, options: UploadOptions): Promise<UploadResult> {
    // Firebase Storage implementation
    // Note: This requires Firebase SDK to be installed and initialized
    try {
      // Dynamic import to avoid bundling Firebase if not used
      const { initializeApp } = await import('firebase/app');
      const { getStorage, ref, uploadBytesResumable, getDownloadURL } = await import('firebase/storage');

      if (!this.firebaseConfig) {
        throw new Error('Firebase configuration is missing');
      }

      // Initialize Firebase
      const app = initializeApp(this.firebaseConfig);
      const storage = getStorage(app);

      // Create storage reference
      const fileName = `${Date.now()}_${file.name}`;
      const folder = options.folder || 'social-posts';
      const storageRef = ref(storage, `${folder}/${fileName}`);

      // Upload file with progress tracking
      const uploadTask = uploadBytesResumable(storageRef, file);

      return new Promise((resolve, reject) => {
        uploadTask.on(
          'state_changed',
          (snapshot) => {
            if (options.onProgress) {
              const progress = {
                loaded: snapshot.bytesTransferred,
                total: snapshot.totalBytes,
                percentage: (snapshot.bytesTransferred / snapshot.totalBytes) * 100,
              };
              options.onProgress(progress);
            }
          },
          (error) => {
            console.error('Firebase upload error:', error);
            reject(new Error('Failed to upload image to Firebase'));
          },
          async () => {
            try {
              const downloadURL = await getDownloadURL(uploadTask.snapshot.ref);
              resolve({
                url: downloadURL,
                fileName: file.name,
                size: file.size,
                format: file.type.split('/')[1],
              });
            } catch (error) {
              reject(error);
            }
          }
        );
      });
    } catch (error) {
      console.error('Firebase upload error:', error);
      throw new Error('Failed to upload image to Firebase. Make sure Firebase is properly configured.');
    }
  }

  // Utility method to generate optimized image URLs (Cloudinary only)
  getOptimizedUrl(
    originalUrl: string,
    options: {
      width?: number;
      height?: number;
      quality?: number;
      format?: 'auto' | 'jpg' | 'png' | 'webp';
    } = {}
  ): string {
    if (this.provider !== 'cloudinary' || !originalUrl.includes('cloudinary.com')) {
      return originalUrl;
    }

    const { width, height, quality = 'auto', format = 'auto' } = options;
    
    // Extract the public ID from the URL
    const urlParts = originalUrl.split('/');
    const uploadIndex = urlParts.findIndex(part => part === 'upload');
    if (uploadIndex === -1) return originalUrl;

    // Build transformation string
    const transformations = [];
    if (width) transformations.push(`w_${width}`);
    if (height) transformations.push(`h_${height}`);
    if (quality) transformations.push(`q_${quality}`);
    if (format) transformations.push(`f_${format}`);
    
    if (transformations.length === 0) return originalUrl;

    // Insert transformations into URL
    const transformationString = transformations.join(',');
    urlParts.splice(uploadIndex + 1, 0, transformationString);
    
    return urlParts.join('/');
  }

  // Delete image (Cloudinary only)
  async deleteImage(publicId: string): Promise<boolean> {
    if (this.provider !== 'cloudinary' || !this.cloudinaryConfig?.apiKey) {
      console.warn('Image deletion is only supported for Cloudinary with API key');
      return false;
    }

    try {
      // Note: This requires server-side implementation for security
      // Client-side deletion is not recommended for production
      console.warn('Image deletion should be implemented on the server side');
      return false;
    } catch (error) {
      console.error('Failed to delete image:', error);
      return false;
    }
  }
}

// Default instance
export const imageUploadService = new ImageUploadService();

// Helper function to compress image before upload
export const compressImage = (file: File, maxWidth: number = 1920, quality: number = 0.8): Promise<File> => {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d')!;
    const img = new Image();

    img.onload = () => {
      // Calculate new dimensions
      const ratio = Math.min(maxWidth / img.width, maxWidth / img.height);
      const width = img.width * ratio;
      const height = img.height * ratio;

      // Set canvas dimensions
      canvas.width = width;
      canvas.height = height;

      // Draw and compress
      ctx.drawImage(img, 0, 0, width, height);
      
      canvas.toBlob(
        (blob) => {
          if (blob) {
            const compressedFile = new File([blob], file.name, {
              type: file.type,
              lastModified: Date.now(),
            });
            resolve(compressedFile);
          } else {
            resolve(file); // Return original if compression fails
          }
        },
        file.type,
        quality
      );
    };

    img.src = URL.createObjectURL(file);
  });
};