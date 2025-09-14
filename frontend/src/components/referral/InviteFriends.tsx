'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { toast } from '@/hooks/use-toast';
import { Copy, Share2, Users, Gift, Mail, MessageCircle, Facebook, Twitter, Linkedin } from 'lucide-react';
import { referralService, ReferralResponse } from '@/lib/services/referral';
import { useAuthStore } from '@/stores/authStore';

interface InviteFriendsProps {
  variant?: 'card' | 'button' | 'inline';
  className?: string;
  showRewards?: boolean;
}

export function InviteFriends({ variant = 'card', className, showRewards = true }: InviteFriendsProps) {
  const { user } = useAuthStore();
  const [referral, setReferral] = useState<ReferralResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (user?.id && isOpen) {
      loadOrCreateReferral();
    }
  }, [user?.id, isOpen]);

  const loadOrCreateReferral = async () => {
    if (!user?.id) return;

    setLoading(true);
    try {
      // First try to get existing referrals
      const referralsResponse = await referralService.getUserReferrals(user.id);
      
      if (referralsResponse.success && referralsResponse.data.length > 0) {
        // Use the first active referral
        const activeReferral = referralsResponse.data.find((r: ReferralResponse) => r.status === 'ACTIVE');
        if (activeReferral) {
          setReferral(activeReferral);
          return;
        }
      }

      // Create a new referral if none exists
      const createResponse = await referralService.createReferral({
        userId: user.id,
        campaign: 'invite_friends'
      });

      if (createResponse.success) {
        setReferral(createResponse.data);
      }
    } catch (error) {
      console.error('Error loading referral:', error);
      toast({
        title: 'Error',
        description: 'Failed to load referral code',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const copyReferralUrl = () => {
    if (!referral?.referralCode) return;
    
    const url = referralService.generateReferralUrl(referral.referralCode);
    navigator.clipboard.writeText(url);
    toast({
      title: 'Copied!',
      description: 'Referral link copied to clipboard',
    });
  };

  const shareViaEmail = () => {
    if (!referral?.referralCode) return;
    
    const url = referralService.generateReferralUrl(referral.referralCode);
    const subject = encodeURIComponent('Join me on HopNGo!');
    const body = encodeURIComponent(
      `Hi there!\n\nI've been using HopNGo to discover amazing travel experiences, and I thought you'd love it too!\n\nSign up with my referral link and we both get rewards:\n${url}\n\nHappy travels!`
    );
    
    window.open(`mailto:?subject=${subject}&body=${body}`);
  };

  const shareViaSMS = () => {
    if (!referral?.referralCode) return;
    
    const url = referralService.generateReferralUrl(referral.referralCode);
    const message = encodeURIComponent(
      `Check out HopNGo for amazing travel experiences! Sign up with my link and we both get rewards: ${url}`
    );
    
    window.open(`sms:?body=${message}`);
  };

  const shareViaSocial = (platform: 'facebook' | 'twitter' | 'linkedin') => {
    if (!referral?.referralCode) return;
    
    const url = referralService.generateReferralUrl(referral.referralCode);
    const text = 'Discover amazing travel experiences on HopNGo! Join me and get rewards:';
    
    let shareUrl = '';
    switch (platform) {
      case 'facebook':
        shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`;
        break;
      case 'twitter':
        shareUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`;
        break;
      case 'linkedin':
        shareUrl = `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`;
        break;
    }
    
    window.open(shareUrl, '_blank', 'width=600,height=400');
  };

  const shareNatively = async () => {
    if (!referral?.referralCode) return;
    
    const url = referralService.generateReferralUrl(referral.referralCode);
    const shareData = {
      title: 'Join HopNGo with my referral!',
      text: 'Discover amazing travel experiences and earn rewards when you sign up with my referral link.',
      url: url,
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
      } catch (error) {
        // Fallback to copy
        copyReferralUrl();
      }
    } else {
      copyReferralUrl();
    }
  };

  const InviteContent = () => (
    <div className="space-y-6">
      {loading ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="text-sm text-gray-600 mt-2">Loading your referral link...</p>
        </div>
      ) : referral ? (
        <>
          {/* Referral Link */}
          <div className="space-y-2">
            <label className="text-sm font-medium">Your Referral Link</label>
            <div className="flex space-x-2">
              <Input
                value={referralService.generateReferralUrl(referral.referralCode!)}
                readOnly
                className="flex-1"
              />
              <Button variant="outline" onClick={copyReferralUrl}>
                <Copy className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* Quick Share Options */}
          <div className="space-y-3">
            <label className="text-sm font-medium">Share with friends</label>
            <div className="grid grid-cols-2 gap-2">
              <Button variant="outline" onClick={shareViaEmail} className="justify-start">
                <Mail className="h-4 w-4 mr-2" />
                Email
              </Button>
              <Button variant="outline" onClick={shareViaSMS} className="justify-start">
                <MessageCircle className="h-4 w-4 mr-2" />
                SMS
              </Button>
              <Button variant="outline" onClick={() => shareViaSocial('facebook')} className="justify-start">
                <Facebook className="h-4 w-4 mr-2" />
                Facebook
              </Button>
              <Button variant="outline" onClick={() => shareViaSocial('twitter')} className="justify-start">
                <Twitter className="h-4 w-4 mr-2" />
                Twitter
              </Button>
            </div>
            <Button onClick={shareNatively} className="w-full">
              <Share2 className="h-4 w-4 mr-2" />
              Share Link
            </Button>
          </div>

          {/* Rewards Info */}
          {showRewards && (
            <div className="bg-blue-50 p-4 rounded-lg space-y-2">
              <div className="flex items-center space-x-2">
                <Gift className="h-5 w-5 text-blue-600" />
                <span className="font-medium text-blue-900">Earn Rewards</span>
              </div>
              <div className="text-sm text-blue-800 space-y-1">
                <div className="flex justify-between">
                  <span>Friend signs up:</span>
                  <Badge variant="secondary">+100 points</Badge>
                </div>
                <div className="flex justify-between">
                  <span>Friend makes first booking:</span>
                  <Badge variant="secondary">+200 points</Badge>
                </div>
              </div>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-2 gap-4 pt-4 border-t">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">{referral.clickCount}</div>
              <div className="text-sm text-gray-600">Clicks</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">{referral.conversionCount}</div>
              <div className="text-sm text-gray-600">Referrals</div>
            </div>
          </div>
        </>
      ) : (
        <div className="text-center py-8">
          <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600">Unable to load referral link</p>
          <Button onClick={loadOrCreateReferral} className="mt-4">
            Try Again
          </Button>
        </div>
      )}
    </div>
  );

  if (variant === 'button') {
    return (
      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogTrigger asChild>
          <Button className={className}>
            <Users className="h-4 w-4 mr-2" />
            Invite Friends
          </Button>
        </DialogTrigger>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Invite Friends</DialogTitle>
            <DialogDescription>
              Share HopNGo with your friends and earn rewards for every successful referral.
            </DialogDescription>
          </DialogHeader>
          <InviteContent />
        </DialogContent>
      </Dialog>
    );
  }

  if (variant === 'inline') {
    return (
      <div className={className}>
        <InviteContent />
      </div>
    );
  }

  // Default card variant
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Users className="h-5 w-5" />
          <span>Invite Friends</span>
        </CardTitle>
        <CardDescription>
          Share HopNGo with your friends and earn rewards for every successful referral.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Dialog open={isOpen} onOpenChange={setIsOpen}>
          <DialogTrigger asChild>
            <Button className="w-full">
              <Gift className="h-4 w-4 mr-2" />
              Start Inviting
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Invite Friends</DialogTitle>
              <DialogDescription>
                Share HopNGo with your friends and earn rewards for every successful referral.
              </DialogDescription>
            </DialogHeader>
            <InviteContent />
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  );
}

export default InviteFriends;