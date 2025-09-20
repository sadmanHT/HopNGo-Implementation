'use client';

import React, { useRef, useEffect, useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import { Play, Pause, Volume2, VolumeX, Maximize } from 'lucide-react';
import Image from 'next/image';
import { cn } from '@/lib/utils';

interface VideoPlayerProps {
  src: string;
  poster?: string;
  className?: string;
  autoPlay?: boolean;
  muted?: boolean;
  loop?: boolean;
  controls?: boolean;
  onPlay?: () => void;
  onPause?: () => void;
  onEnded?: () => void;
  inFeed?: boolean; // Special behavior for social feed
}

export const VideoPlayer: React.FC<VideoPlayerProps> = ({
  src,
  poster,
  className,
  autoPlay = false,
  muted = true,
  loop = false,
  controls = true,
  onPlay,
  onPause,
  onEnded,
  inFeed = false,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(muted);
  const [showControls, setShowControls] = useState(!inFeed);
  const [isInViewport, setIsInViewport] = useState(false);
  const [hasStarted, setHasStarted] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);

  // Intersection Observer for viewport detection
  useEffect(() => {
    if (!containerRef.current || !inFeed) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        setIsInViewport(entry.isIntersecting);
        
        if (entry.isIntersecting && autoPlay && !hasStarted) {
          handlePlay();
          setHasStarted(true);
        } else if (!entry.isIntersecting && isPlaying) {
          handlePause();
        }
      },
      {
        threshold: 0.5, // Video must be 50% visible
      }
    );

    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, [autoPlay, hasStarted, isPlaying, inFeed]);

  // Video event handlers
  const handlePlay = useCallback(() => {
    if (videoRef.current) {
      videoRef.current.play();
      setIsPlaying(true);
      onPlay?.();
    }
  }, [onPlay]);

  const handlePause = useCallback(() => {
    if (videoRef.current) {
      videoRef.current.pause();
      setIsPlaying(false);
      onPause?.();
    }
  }, [onPause]);

  const togglePlay = () => {
    if (isPlaying) {
      handlePause();
    } else {
      handlePlay();
    }
  };

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted;
      setIsMuted(!isMuted);
    }
  };

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      const current = videoRef.current.currentTime;
      const total = videoRef.current.duration;
      setCurrentTime(current);
      setProgress((current / total) * 100);
    }
  };

  const handleLoadedMetadata = () => {
    if (videoRef.current) {
      setDuration(videoRef.current.duration);
    }
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
    if (!time || isNaN(time)) return '0:00';
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const handleFullscreen = () => {
    if (videoRef.current) {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        videoRef.current.requestFullscreen();
      }
    }
  };

  // Click to unmute in feed mode
  const handleVideoClick = () => {
    if (inFeed) {
      if (isMuted) {
        toggleMute();
      } else {
        togglePlay();
      }
    } else {
      togglePlay();
    }
  };

  return (
    <div
      ref={containerRef}
      className={cn(
        "relative group overflow-hidden rounded-lg bg-black",
        inFeed ? "cursor-pointer" : "",
        className
      )}
      onMouseEnter={() => setShowControls(true)}
      onMouseLeave={() => setShowControls(!inFeed)}
    >
      {/* Video Element */}
      <video
        ref={videoRef}
        src={src}
        poster={poster}
        muted={isMuted}
        loop={loop}
        playsInline
        className="w-full h-full object-cover"
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={() => {
          setIsPlaying(false);
          onEnded?.();
        }}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onClick={handleVideoClick}
      />

      {/* Poster overlay when not started */}
      {!hasStarted && poster && (
        <div className="absolute inset-0">
          <Image
            src={poster}
            alt="Video poster"
            fill
            className="object-cover"
            sizes="(max-width: 768px) 100vw, 50vw"
          />
        </div>
      )}

      {/* Play button overlay */}
      {(!isPlaying || !hasStarted) && (
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="absolute inset-0 flex items-center justify-center"
        >
          <motion.button
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.9 }}
            onClick={handleVideoClick}
            className="w-16 h-16 bg-white/90 rounded-full flex items-center justify-center shadow-lg backdrop-blur-sm"
          >
            <Play className="w-6 h-6 text-black ml-1" fill="currentColor" />
          </motion.button>
        </motion.div>
      )}

      {/* Mute indicator for feed videos */}
      {inFeed && isMuted && isPlaying && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="absolute top-4 right-4 bg-black/50 rounded-full p-2"
        >
          <VolumeX className="w-4 h-4 text-white" />
        </motion.div>
      )}

      {/* Controls overlay */}
      {controls && showControls && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"
        >
          {/* Progress bar */}
          <div className="absolute bottom-16 left-4 right-4">
            <div
              className="w-full h-1 bg-white/30 rounded-full cursor-pointer group"
              onClick={handleSeek}
            >
              <div
                className="h-full bg-white rounded-full transition-all duration-100 group-hover:bg-blue-400"
                style={{ width: `${(currentTime / duration) * 100}%` }}
              />
            </div>
          </div>

          {/* Control buttons */}
          <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <motion.button
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={togglePlay}
                className="text-white hover:text-blue-400 transition-colors"
              >
                {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5" />}
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={toggleMute}
                className="text-white hover:text-blue-400 transition-colors"
              >
                {isMuted ? <VolumeX className="w-5 h-5" /> : <Volume2 className="w-5 h-5" />}
              </motion.button>

              <span className="text-white text-sm font-medium">
                {formatTime(currentTime)} / {formatTime(duration)}
              </span>
            </div>

            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={handleFullscreen}
              className="text-white hover:text-blue-400 transition-colors"
            >
              <Maximize className="w-5 h-5" />
            </motion.button>
          </div>
        </motion.div>
      )}
    </div>
  );
};



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

  // Convert Cloudinary video URL to poster image URL
  if (videoUrl.includes('cloudinary.com')) {
    return videoUrl
      .replace('/video/upload/', `/video/upload/w_${width},h_${height},c_fill,q_${quality},f_${format},so_${timeOffset}/`)
      .replace(/\.[^.]+$/, `.${format}`);
  }

  return videoUrl;
}