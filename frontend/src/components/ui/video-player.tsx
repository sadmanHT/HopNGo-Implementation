'use client';

import { useState, useRef, useEffect } from 'react';
import { Play, Pause, Volume2, VolumeX, Maximize } from 'lucide-react';
import { OptimizedImage, getOptimizedCloudinaryUrl } from './optimized-image';
import { cn } from '@/lib/utils';

interface VideoPlayerProps {
  src: string;
  poster?: string;
  className?: string;
  autoPlay?: boolean;
  muted?: boolean;
  loop?: boolean;
  controls?: boolean;
  width?: number;
  height?: number;
  onLoad?: () => void;
  onError?: () => void;
}

export function VideoPlayer({
  src,
  poster,
  className,
  autoPlay = false,
  muted = false,
  loop = false,
  controls = true,
  width,
  height,
  onLoad,
  onError
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(muted);
  const [showControls, setShowControls] = useState(false);
  const [duration, setDuration] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const handleLoadedMetadata = () => {
      setDuration(video.duration);
      setIsLoading(false);
      onLoad?.();
    };

    const handleTimeUpdate = () => {
      setCurrentTime(video.currentTime);
    };

    const handlePlay = () => setIsPlaying(true);
    const handlePause = () => setIsPlaying(false);
    const handleError = () => {
      setHasError(true);
      setIsLoading(false);
      onError?.();
    };

    video.addEventListener('loadedmetadata', handleLoadedMetadata);
    video.addEventListener('timeupdate', handleTimeUpdate);
    video.addEventListener('play', handlePlay);
    video.addEventListener('pause', handlePause);
    video.addEventListener('error', handleError);

    return () => {
      video.removeEventListener('loadedmetadata', handleLoadedMetadata);
      video.removeEventListener('timeupdate', handleTimeUpdate);
      video.removeEventListener('play', handlePlay);
      video.removeEventListener('pause', handlePause);
      video.removeEventListener('error', handleError);
    };
  }, [onLoad, onError]);

  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;

    if (isPlaying) {
      video.pause();
    } else {
      video.play();
    }
  };

  const toggleMute = () => {
    const video = videoRef.current;
    if (!video) return;

    video.muted = !isMuted;
    setIsMuted(!isMuted);
  };

  const handleSeek = (e: React.MouseEvent<HTMLDivElement>) => {
    const video = videoRef.current;
    if (!video) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const newTime = (clickX / rect.width) * duration;
    video.currentTime = newTime;
  };

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const generatePosterUrl = (videoUrl: string) => {
    return getOptimizedCloudinaryUrl(videoUrl, {
      width: width || 400,
      height: height || 300,
      crop: 'fill',
      quality: 'auto',
      format: 'jpg'
    }).replace('/video/', '/video/so_0/');
  };

  if (hasError) {
    return (
      <div className={cn(
        'flex items-center justify-center bg-gray-100 text-gray-400 aspect-video',
        className
      )}>
        <span className="text-sm">Failed to load video</span>
      </div>
    );
  }

  return (
    <div 
      className={cn('relative group', className)}
      onMouseEnter={() => setShowControls(true)}
      onMouseLeave={() => setShowControls(false)}
    >
      {isLoading && (
        <div className="absolute inset-0 bg-gray-100 animate-pulse flex items-center justify-center">
          <span className="text-sm text-gray-500">Loading video...</span>
        </div>
      )}
      
      <video
        ref={videoRef}
        src={src}
        poster={poster || generatePosterUrl(src)}
        autoPlay={autoPlay}
        muted={isMuted}
        loop={loop}
        className={cn(
          'w-full h-full object-cover transition-opacity duration-300',
          isLoading ? 'opacity-0' : 'opacity-100'
        )}
        width={width}
        height={height}
      />

      {controls && (
        <div className={cn(
          'absolute inset-0 bg-black bg-opacity-0 transition-all duration-300',
          showControls || !isPlaying ? 'bg-opacity-20' : ''
        )}>
          {/* Play/Pause overlay */}
          <div 
            className="absolute inset-0 flex items-center justify-center cursor-pointer"
            onClick={togglePlay}
          >
            {!isPlaying && (
              <div className="bg-black bg-opacity-50 rounded-full p-4 transition-opacity duration-300">
                <Play className="w-8 h-8 text-white" />
              </div>
            )}
          </div>

          {/* Controls bar */}
          <div className={cn(
            'absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black to-transparent p-4 transition-opacity duration-300',
            showControls || !isPlaying ? 'opacity-100' : 'opacity-0'
          )}>
            {/* Progress bar */}
            <div 
              className="w-full h-1 bg-white bg-opacity-30 rounded-full mb-3 cursor-pointer"
              onClick={handleSeek}
            >
              <div 
                className="h-full bg-white rounded-full transition-all duration-100"
                style={{ width: `${(currentTime / duration) * 100}%` }}
              />
            </div>

            {/* Control buttons */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <button
                  onClick={togglePlay}
                  className="text-white hover:text-gray-300 transition-colors"
                >
                  {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5" />}
                </button>
                
                <button
                  onClick={toggleMute}
                  className="text-white hover:text-gray-300 transition-colors"
                >
                  {isMuted ? <VolumeX className="w-5 h-5" /> : <Volume2 className="w-5 h-5" />}
                </button>
                
                <span className="text-white text-sm">
                  {formatTime(currentTime)} / {formatTime(duration)}
                </span>
              </div>
              
              <button
                onClick={() => videoRef.current?.requestFullscreen()}
                className="text-white hover:text-gray-300 transition-colors"
              >
                <Maximize className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Utility function to extract video poster from Cloudinary
export function getVideoPosterUrl(
  videoUrl: string,
  options: {
    width?: number;
    height?: number;
    quality?: 'auto' | number;
    format?: 'jpg' | 'png' | 'webp';
    timeOffset?: number; // seconds
  } = {}
) {
  const {
    width = 400,
    height = 300,
    quality = 'auto',
    format = 'jpg',
    timeOffset = 0
  } = options;

  return getOptimizedCloudinaryUrl(videoUrl, {
    width,
    height,
    quality,
    format,
    crop: 'fill'
  }).replace('/video/upload/', `/video/upload/so_${timeOffset}/`);
}