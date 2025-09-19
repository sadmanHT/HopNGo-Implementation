'use client';

import React from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { MapPin, Star, Heart, Share2, Calendar, Users } from 'lucide-react';
import { cn } from '@/lib/utils';

interface DestinationCardProps {
  id: string;
  title: string;
  titleBn?: string;
  location: string;
  locationBn?: string;
  district: string;
  districtBn?: string;
  image: string;
  rating?: number;
  reviewCount?: number;
  price?: number;
  duration?: string;
  groupSize?: number;
  tags?: string[];
  isFavorite?: boolean;
  className?: string;
  onFavoriteToggle?: (id: string) => void;
  onShare?: (id: string) => void;
}

export function DestinationCard({
  id,
  title,
  titleBn,
  location,
  locationBn,
  district,
  districtBn,
  image,
  rating = 0,
  reviewCount = 0,
  price,
  duration,
  groupSize,
  tags = [],
  isFavorite = false,
  className,
  onFavoriteToggle,
  onShare,
}: DestinationCardProps) {
  const handleFavoriteClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onFavoriteToggle?.(id);
  };

  const handleShareClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onShare?.(id);
  };

  return (
    <Link href={`/destinations/${id}`} className="block group">
      <Card className={cn(
        "overflow-hidden border-0 shadow-md hover:shadow-xl transition-all duration-300",
        "group-hover:scale-[1.01] group-hover:-translate-y-1",
        "bg-white/90 backdrop-blur-sm",
        className
      )}>
        <div className="relative aspect-[4/3] overflow-hidden">
          <Image
            src={image}
            alt={titleBn ? `${title} (${titleBn})` : title}
            fill
            className="object-cover transition-transform duration-500 group-hover:scale-110"
            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          />
          
          {/* Gradient Overlay */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
          
          {/* Location Chip */}
          <div className="absolute top-3 left-3">
            <Badge 
              variant="secondary" 
              className="bg-white/90 backdrop-blur-sm text-bd-slate border-0 shadow-sm font-bengali"
            >
              <MapPin className="w-3 h-3 mr-1" />
              <span className="font-medium">
                {districtBn && (
                  <>
                    <span className="font-bengali">{districtBn}</span>
                    <span className="text-xs text-muted-foreground ml-1">({district})</span>
                  </>
                )}
                {!districtBn && district}
              </span>
            </Badge>
          </div>
          
          {/* Action Buttons */}
          <div className="absolute top-3 right-3 flex space-x-2">
            <Button
              size="sm"
              variant="ghost"
              className="h-8 w-8 p-0 bg-white/90 backdrop-blur-sm hover:bg-white hover:scale-110 transition-all duration-200"
              onClick={handleShareClick}
              aria-label="Share destination"
            >
              <Share2 className="h-4 w-4 text-bd-slate" />
            </Button>
            <Button
              size="sm"
              variant="ghost"
              className={cn(
                "h-8 w-8 p-0 bg-white/90 backdrop-blur-sm hover:scale-110 transition-all duration-200",
                isFavorite ? "text-bd-coral hover:bg-bd-coral/10" : "hover:bg-white"
              )}
              onClick={handleFavoriteClick}
              aria-label={isFavorite ? "Remove from favorites" : "Add to favorites"}
            >
              <Heart className={cn(
                "h-4 w-4",
                isFavorite ? "fill-current text-bd-coral" : "text-bd-slate"
              )} />
            </Button>
          </div>
          
          {/* Rating Badge */}
          {rating > 0 && (
            <div className="absolute bottom-3 left-3">
              <Badge className="bg-bd-green hover:bg-bd-green/90 text-white border-0 shadow-sm">
                <Star className="w-3 h-3 mr-1 fill-current" />
                <span className="font-medium">{rating.toFixed(1)}</span>
                {reviewCount > 0 && (
                  <span className="text-xs ml-1">({reviewCount})</span>
                )}
              </Badge>
            </div>
          )}
        </div>
        
        <CardContent className="p-4 space-y-3">
          {/* Title */}
          <div className="space-y-1">
            <h3 className="font-semibold text-lg text-bd-slate line-clamp-1 group-hover:text-bd-green transition-colors">
              {title}
            </h3>
            {titleBn && (
              <p className="text-sm text-muted-foreground font-bengali line-clamp-1">
                {titleBn}
              </p>
            )}
          </div>
          
          {/* Location */}
          <div className="flex items-center text-sm text-muted-foreground">
            <MapPin className="w-4 h-4 mr-1 flex-shrink-0" />
            <span className="line-clamp-1">
              {locationBn ? (
                <>
                  <span className="font-bengali">{locationBn}</span>
                  <span className="ml-1">({location})</span>
                </>
              ) : (
                location
              )}
            </span>
          </div>
          
          {/* Tags */}
          {tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {tags.slice(0, 3).map((tag, index) => (
                <Badge 
                  key={index} 
                  variant="outline" 
                  className="text-xs px-2 py-0.5 border-bd-teal/30 text-bd-teal hover:bg-bd-teal/10"
                >
                  {tag}
                </Badge>
              ))}
              {tags.length > 3 && (
                <Badge variant="outline" className="text-xs px-2 py-0.5 text-muted-foreground">
                  +{tags.length - 3}
                </Badge>
              )}
            </div>
          )}
          
          {/* Meta Info */}
          <div className="flex items-center justify-between pt-2 border-t border-border/50">
            <div className="flex items-center space-x-4 text-xs text-muted-foreground">
              {duration && (
                <div className="flex items-center">
                  <Calendar className="w-3 h-3 mr-1" />
                  <span>{duration}</span>
                </div>
              )}
              {groupSize && (
                <div className="flex items-center">
                  <Users className="w-3 h-3 mr-1" />
                  <span>{groupSize}+ people</span>
                </div>
              )}
            </div>
            
            {price && (
              <div className="text-right">
                <div className="text-sm font-semibold text-bd-green">
                  ৳{price.toLocaleString()}
                </div>
                <div className="text-xs text-muted-foreground">
                  per person
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

// Grid container for destination cards
interface DestinationGridProps {
  destinations: Array<DestinationCardProps>;
  className?: string;
  onFavoriteToggle?: (id: string) => void;
  onShare?: (id: string) => void;
}

export function DestinationGrid({ 
  destinations, 
  className, 
  onFavoriteToggle, 
  onShare 
}: DestinationGridProps) {
  return (
    <div className={cn(
      "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6",
      className
    )}>
      {destinations.map((destination) => (
        <DestinationCard
          key={destination.id}
          {...destination}
          onFavoriteToggle={onFavoriteToggle}
          onShare={onShare}
        />
      ))}
    </div>
  );
}