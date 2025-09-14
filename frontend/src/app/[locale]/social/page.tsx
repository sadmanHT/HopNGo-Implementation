'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/ui/tabs';
import { CreatePost } from '../../../components/social/CreatePost';
import { SocialFeed } from '../../../components/social/SocialFeed';
// import { TouristHeatmap } from '../../../components/social/TouristHeatmap';
import { Users, Map, Plus, TrendingUp } from 'lucide-react';
import { useSocialStore } from '../../../lib/state';

export default function SocialPage() {
  const [activeTab, setActiveTab] = useState('feed');
  const [showCreatePost, setShowCreatePost] = useState(false);
  const { posts } = useSocialStore();

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold">Social Hub</h1>
          <p className="text-muted-foreground mt-1">
            Connect with fellow travelers and discover amazing destinations
          </p>
        </div>
        
        <Button 
          onClick={() => setShowCreatePost(true)}
          className="flex items-center gap-2"
        >
          <Plus className="h-4 w-4" />
          Create Post
        </Button>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center space-x-2">
              <Users className="h-4 w-4 text-blue-500" />
              <div>
                <p className="text-2xl font-bold">{posts.length}</p>
                <p className="text-xs text-muted-foreground">Total Posts</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center space-x-2">
              <Map className="h-4 w-4 text-green-500" />
              <div>
                <p className="text-2xl font-bold">
                  {posts.filter(p => p.location).length}
                </p>
                <p className="text-xs text-muted-foreground">Locations Shared</p>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="pt-4">
            <div className="flex items-center space-x-2">
              <TrendingUp className="h-4 w-4 text-orange-500" />
              <div>
                <p className="text-2xl font-bold">
                  {posts.reduce((sum, p) => sum + p.likes, 0)}
                </p>
                <p className="text-xs text-muted-foreground">Total Likes</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="feed" className="flex items-center gap-2">
            <Users className="h-4 w-4" />
            Social Feed
          </TabsTrigger>
          <TabsTrigger value="map" className="flex items-center gap-2">
            <Map className="h-4 w-4" />
            Tourist Heatmap
          </TabsTrigger>
        </TabsList>

        {/* Social Feed Tab */}
        <TabsContent value="feed" className="space-y-6">
          {showCreatePost && (
            <Card>
              <CardHeader>
                <CardTitle>Create New Post</CardTitle>
              </CardHeader>
              <CardContent>
                <CreatePost
                  onPostCreated={() => setShowCreatePost(false)}
                />
              </CardContent>
            </Card>
          )}
          
          <SocialFeed />
        </TabsContent>

        {/* Tourist Heatmap Tab */}
        <TabsContent value="map" className="space-y-6">
          <div className="flex items-center justify-center h-96 bg-gray-100 rounded-lg">
            <p className="text-gray-500">Tourist Heatmap (Mapbox integration pending)</p>
          </div>
        </TabsContent>
      </Tabs>

      {/* Empty State */}
      {posts.length === 0 && (
        <Card className="mt-8">
          <CardContent className="pt-8">
            <div className="text-center space-y-4">
              <div className="text-6xl">🌍</div>
              <h3 className="text-xl font-semibold">Welcome to Social Hub!</h3>
              <p className="text-muted-foreground max-w-md mx-auto">
                Start sharing your travel experiences and connect with fellow adventurers. 
                Create your first post to get started!
              </p>
              <Button 
                onClick={() => setShowCreatePost(true)}
                className="mt-4"
              >
                <Plus className="h-4 w-4 mr-2" />
                Create Your First Post
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}