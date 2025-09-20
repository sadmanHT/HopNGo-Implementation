'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { toast } from '@/hooks/use-toast';
import { Copy, Share2, Users, MousePointer, Trophy, Coins, Plus, ExternalLink } from 'lucide-react';
import { referralService, ReferralStats, ReferralResponse } from '@/lib/services/referral';
import { useAuthStore } from '@/lib/state/auth';

interface ReferralDashboardProps {
  className?: string;
}

export function ReferralDashboard({ className }: ReferralDashboardProps) {
  const { user } = useAuthStore();
  const [stats, setStats] = useState<ReferralStats | null>(null);
  const [referrals, setReferrals] = useState<ReferralResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newCampaign, setNewCampaign] = useState('');

  useEffect(() => {
    if (user?.id) {
      loadReferralData();
    }
  }, [user?.id]);

  const loadReferralData = async () => {
    if (!user?.id) return;

    setLoading(true);
    try {
      const [statsResponse, referralsResponse] = await Promise.all([
        referralService.getReferralStats(user.id),
        referralService.getUserReferrals(user.id)
      ]);

      if (statsResponse.success) {
        setStats(statsResponse.data);
      }

      if (referralsResponse.success) {
        setReferrals(referralsResponse.data);
      }
    } catch (error) {
      console.error('Error loading referral data:', error);
      toast({
        title: 'Error',
        description: 'Failed to load referral data',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const createReferral = async () => {
    if (!user?.id) return;

    setCreating(true);
    try {
      const response = await referralService.createReferral({
        userId: user.id,
        campaign: newCampaign || 'default'
      });

      if (response.success) {
        toast({
          title: 'Success',
          description: 'New referral code created successfully!',
        });
        setNewCampaign('');
        loadReferralData();
      } else {
        throw new Error(response.message || response.errors?.[0] || 'Unknown error');
      }
    } catch (error) {
      console.error('Error creating referral:', error);
      toast({
        title: 'Error',
        description: 'Failed to create referral code',
        variant: 'destructive',
      });
    } finally {
      setCreating(false);
    }
  };

  const copyReferralUrl = (referralCode: string) => {
    const url = referralService.generateReferralUrl(referralCode);
    navigator.clipboard.writeText(url);
    toast({
      title: 'Copied!',
      description: 'Referral URL copied to clipboard',
    });
  };

  const shareReferral = async (referralCode: string) => {
    const url = referralService.generateReferralUrl(referralCode);
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
        copyReferralUrl(referralCode);
      }
    } else {
      copyReferralUrl(referralCode);
    }
  };

  if (loading) {
    return (
      <div className={`space-y-6 ${className}`}>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-4 bg-gray-200 rounded mb-2"></div>
                <div className="h-8 bg-gray-200 rounded"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <Users className="h-4 w-4 text-blue-600" />
              <span className="text-sm font-medium text-gray-600">Total Referrals</span>
            </div>
            <div className="text-2xl font-bold">{stats?.totalReferrals || 0}</div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <MousePointer className="h-4 w-4 text-green-600" />
              <span className="text-sm font-medium text-gray-600">Total Clicks</span>
            </div>
            <div className="text-2xl font-bold">{stats?.totalClicks || 0}</div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <Trophy className="h-4 w-4 text-orange-600" />
              <span className="text-sm font-medium text-gray-600">Conversions</span>
            </div>
            <div className="text-2xl font-bold">{stats?.totalConversions || 0}</div>
            <div className="text-xs text-gray-500">
              {stats?.conversionRate ? `${stats.conversionRate.toFixed(1)}% rate` : '0% rate'}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <Coins className="h-4 w-4 text-yellow-600" />
              <span className="text-sm font-medium text-gray-600">Points Earned</span>
            </div>
            <div className="text-2xl font-bold">{stats?.totalPointsEarned || 0}</div>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="referrals" className="w-full">
        <TabsList>
          <TabsTrigger value="referrals">My Referral Codes</TabsTrigger>
          <TabsTrigger value="rewards">Rewards Program</TabsTrigger>
        </TabsList>

        <TabsContent value="referrals" className="space-y-4">
          {/* Create New Referral */}
          <Card>
            <CardHeader>
              <CardTitle>Create New Referral Code</CardTitle>
              <CardDescription>
                Generate a new referral code for specific campaigns or sharing channels.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex space-x-2">
                <Input
                  placeholder="Campaign name (optional)"
                  value={newCampaign}
                  onChange={(e) => setNewCampaign(e.target.value)}
                  className="flex-1"
                />
                <Button onClick={createReferral} disabled={creating}>
                  <Plus className="h-4 w-4 mr-2" />
                  {creating ? 'Creating...' : 'Create'}
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Existing Referrals */}
          <div className="grid gap-4">
            {referrals.map((referral) => (
              <Card key={referral.referralCode}>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <div className="space-y-2">
                      <div className="flex items-center space-x-2">
                        <code className="px-2 py-1 bg-gray-100 rounded text-sm font-mono">
                          {referral.referralCode}
                        </code>
                        <Badge variant={referral.status === 'ACTIVE' ? 'default' : 'secondary'}>
                          {referral.status}
                        </Badge>
                      </div>
                      <div className="text-sm text-gray-600">
                        Campaign: {referral.campaign} • 
                        Clicks: {referral.clickCount} • 
                        Conversions: {referral.conversionCount}
                      </div>
                    </div>
                    <div className="flex space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => copyReferralUrl(referral.referralCode!)}
                      >
                        <Copy className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => shareReferral(referral.referralCode!)}
                      >
                        <Share2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {referrals.length === 0 && (
            <Card>
              <CardContent className="p-12 text-center">
                <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No referral codes yet</h3>
                <p className="text-gray-600 mb-4">
                  Create your first referral code to start earning rewards!
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="rewards" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Referral Rewards Program</CardTitle>
              <CardDescription>
                Earn points for every successful referral and unlock amazing rewards.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-4">
                  <h4 className="font-medium">How to Earn Points</h4>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="text-sm">Friend signs up</span>
                      <Badge variant="secondary">+100 points</Badge>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="text-sm">Friend makes first booking</span>
                      <Badge variant="secondary">+200 points</Badge>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="text-sm">Friend subscribes to newsletter</span>
                      <Badge variant="secondary">+50 points</Badge>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                      <span className="text-sm">Friend makes a purchase</span>
                      <Badge variant="secondary">+150 points</Badge>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <h4 className="font-medium">Redeem Rewards</h4>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center p-3 bg-blue-50 rounded">
                      <span className="text-sm">$5 Travel Credit</span>
                      <Badge variant="outline">500 points</Badge>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-blue-50 rounded">
                      <span className="text-sm">$10 Travel Credit</span>
                      <Badge variant="outline">1000 points</Badge>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-blue-50 rounded">
                      <span className="text-sm">$25 Travel Credit</span>
                      <Badge variant="outline">2500 points</Badge>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-blue-50 rounded">
                      <span className="text-sm">Premium Membership (1 month)</span>
                      <Badge variant="outline">5000 points</Badge>
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="font-medium">Your Current Balance</h4>
                    <p className="text-2xl font-bold text-blue-600">{stats?.totalPointsEarned || 0} points</p>
                  </div>
                  <Button variant="outline">
                    <ExternalLink className="h-4 w-4 mr-2" />
                    View Rewards Store
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

export default ReferralDashboard;