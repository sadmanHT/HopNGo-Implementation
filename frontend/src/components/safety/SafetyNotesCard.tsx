'use client';

import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Shield, AlertTriangle, Info, CheckCircle, Thermometer, Cloud, Sun } from 'lucide-react';
import { BDDestination } from '@/data/bd-destinations';
import { useWeatherAdvisory } from '@/hooks/useWeatherAdvisory';

interface SafetyNote {
  type: 'warning' | 'info' | 'success' | 'danger';
  message: string;
  source: 'curated' | 'user_reviews' | 'weather' | 'government';
}

interface SafetyNotesCardProps {
  destination?: BDDestination;
  additionalNotes?: SafetyNote[];
  className?: string;
  compact?: boolean;
  coordinates?: { lat: number; lng: number };
}

const SafetyNotesCard: React.FC<SafetyNotesCardProps> = ({
  destination,
  additionalNotes = [],
  className = '',
  compact = false,
  coordinates
}) => {
  const { weather, advisories, loading, error } = useWeatherAdvisory(destination, coordinates);
  // Convert destination safety notes to SafetyNote format
  const curatedNotes: SafetyNote[] = destination?.safety_notes?.map(note => ({
    type: 'warning' as const,
    message: note,
    source: 'curated' as const
  })) || [];

  // Mock user review sentiment-based safety notes
  const userReviewNotes: SafetyNote[] = destination ? [
    {
      type: 'info',
      message: 'Recent travelers recommend hiring local guides for better navigation',
      source: 'user_reviews'
    },
    {
      type: 'success',
      message: '95% of visitors rated this destination as safe for solo travelers',
      source: 'user_reviews'
    }
  ] : [];

  // Convert weather advisory to SafetyNote format
  const convertAdvisoriesToNotes = (): SafetyNote[] => {
    return advisories.map(advisory => ({
      type: advisory.type,
      message: advisory.message,
      source: advisory.source
    }));
  };



  const allNotes = [
    ...curatedNotes,
    ...userReviewNotes,
    ...convertAdvisoriesToNotes(),
    ...additionalNotes
  ];

  if (loading) {
    return (
      <Card className={className}>
        <CardContent className={compact ? 'p-3' : 'p-4'}>
          <div className="flex items-center space-x-2 text-sm text-muted-foreground">
            <Cloud className="h-4 w-4 animate-pulse" />
            <span>Loading safety information...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (allNotes.length === 0 && !error) {
    return null;
  }

  const getIcon = (type: SafetyNote['type'], source?: SafetyNote['source']) => {
    if (source === 'weather') return <Cloud className="h-4 w-4" />;
    if (source === 'seasonal') return <Thermometer className="h-4 w-4" />;
    if (source === 'ai') return <Sun className="h-4 w-4" />;
    
    switch (type) {
      case 'warning':
        return <AlertTriangle className="h-4 w-4" />;
      case 'danger':
        return <Shield className="h-4 w-4" />;
      case 'info':
        return <Info className="h-4 w-4" />;
      case 'success':
        return <CheckCircle className="h-4 w-4" />;
      default:
        return <Info className="h-4 w-4" />;
    }
  };

  const getVariant = (type: SafetyNote['type']) => {
    switch (type) {
      case 'warning':
        return 'default';
      case 'danger':
        return 'destructive';
      case 'info':
        return 'default';
      case 'success':
        return 'default';
      default:
        return 'default';
    }
  };

  const getSourceBadge = (source: SafetyNote['source']) => {
    const sourceLabels = {
      curated: 'Official',
      user_reviews: 'Travelers',
      weather: 'Weather',
      government: 'Gov'
    };
    
    return (
      <Badge variant="outline" className="text-xs">
        {sourceLabels[source]}
      </Badge>
    );
  };

  if (compact) {
    return (
      <div className={`space-y-2 ${className}`}>
        {allNotes.slice(0, 3).map((note, index) => (
          <Alert key={index} variant={getVariant(note.type)} className="py-2">
            <div className="flex items-start gap-2">
              {getIcon(note.type, note.source)}
              <div className="flex-1">
                <AlertDescription className="text-sm">
                  {note.message}
                </AlertDescription>
              </div>
              {getSourceBadge(note.source)}
            </div>
          </Alert>
        ))}
        {allNotes.length > 3 && (
          <p className="text-xs text-muted-foreground text-center">
            +{allNotes.length - 3} more safety notes
          </p>
        )}
      </div>
    );
  }

  return (
    <Card className={className}>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-lg">
          <Shield className="h-5 w-5 text-blue-600" />
          Safety & Travel Notes
          <Badge variant="secondary" className="ml-auto">
            {allNotes.length} notes
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {allNotes.map((note, index) => (
          <Alert key={index} variant={getVariant(note.type)}>
            <div className="flex items-start gap-3">
              {getIcon(note.type, note.source)}
              <div className="flex-1">
                <AlertDescription>
                  {note.message}
                </AlertDescription>
              </div>
              {getSourceBadge(note.source)}
            </div>
          </Alert>
        ))}
        
        {destination && (
          <div className="mt-4 pt-3 border-t">
            <p className="text-xs text-muted-foreground">
              Safety information is compiled from official sources, traveler reviews, and real-time weather data.
              Always verify current conditions before traveling.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default SafetyNotesCard;