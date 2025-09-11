'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Search, BookOpen, MessageCircle, Phone, Mail, ExternalLink } from 'lucide-react';
import Link from 'next/link';

interface HelpArticle {
  id: string;
  slug: string;
  title: string;
  excerpt: string;
  tags: string[];
  viewCount: number;
  category: string;
}

const POPULAR_ARTICLES: HelpArticle[] = [
  {
    id: '1',
    slug: 'booking-issues',
    title: 'Common Booking Problems and Solutions',
    excerpt: 'Learn how to resolve the most common issues when making bookings on HopNGo.',
    tags: ['booking', 'troubleshooting'],
    viewCount: 1250,
    category: 'Booking'
  },
  {
    id: '2',
    slug: 'payment-problems',
    title: 'Payment Issues and Refunds',
    excerpt: 'Get help with payment failures, refunds, and billing questions.',
    tags: ['payment', 'refunds', 'billing'],
    viewCount: 980,
    category: 'Payment'
  },
  {
    id: '3',
    slug: 'login-issues',
    title: 'Account Access and Login Problems',
    excerpt: 'Troubleshoot login issues, password resets, and account verification.',
    tags: ['login', 'account', 'password'],
    viewCount: 750,
    category: 'Account'
  },
  {
    id: '4',
    slug: 'common-issues',
    title: 'Frequently Asked Questions',
    excerpt: 'Find answers to the most commonly asked questions about HopNGo.',
    tags: ['faq', 'general'],
    viewCount: 2100,
    category: 'General'
  }
];

const CATEGORIES = [
  { name: 'All', count: 24 },
  { name: 'Booking', count: 8 },
  { name: 'Payment', count: 6 },
  { name: 'Account', count: 5 },
  { name: 'General', count: 5 }
];

export default function HelpPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [articles, setArticles] = useState<HelpArticle[]>(POPULAR_ARTICLES);
  const [isLoading, setIsLoading] = useState(false);

  const filteredArticles = articles.filter(article => {
    const matchesSearch = searchQuery === '' || 
      article.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      article.excerpt.toLowerCase().includes(searchQuery.toLowerCase()) ||
      article.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
    
    const matchesCategory = selectedCategory === 'All' || article.category === selectedCategory;
    
    return matchesSearch && matchesCategory;
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    // Simulate API call
    setTimeout(() => {
      setIsLoading(false);
    }, 500);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">
              How can we help you?
            </h1>
            <p className="text-lg text-gray-600 mb-8">
              Search our help center or browse popular topics below
            </p>
            
            {/* Search Bar */}
            <form onSubmit={handleSearch} className="max-w-2xl mx-auto">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                <Input
                  type="text"
                  placeholder="Search for help articles..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 pr-4 py-3 text-lg"
                />
                <Button 
                  type="submit" 
                  className="absolute right-2 top-1/2 transform -translate-y-1/2"
                  disabled={isLoading}
                >
                  {isLoading ? 'Searching...' : 'Search'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Sidebar */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Categories</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {CATEGORIES.map((category) => (
                  <button
                    key={category.name}
                    onClick={() => setSelectedCategory(category.name)}
                    className={`w-full text-left px-3 py-2 rounded-md transition-colors ${
                      selectedCategory === category.name
                        ? 'bg-blue-100 text-blue-700'
                        : 'hover:bg-gray-100'
                    }`}
                  >
                    <div className="flex justify-between items-center">
                      <span>{category.name}</span>
                      <Badge variant="secondary" className="text-xs">
                        {category.count}
                      </Badge>
                    </div>
                  </button>
                ))}
              </CardContent>
            </Card>

            {/* Contact Support */}
            <Card className="mt-6">
              <CardHeader>
                <CardTitle className="text-lg">Need More Help?</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Link href="/support/contact">
                  <Button variant="outline" className="w-full justify-start">
                    <MessageCircle className="h-4 w-4 mr-2" />
                    Contact Support
                    <ExternalLink className="h-3 w-3 ml-auto" />
                  </Button>
                </Link>
                <Button variant="outline" className="w-full justify-start">
                  <Phone className="h-4 w-4 mr-2" />
                  Call Us: +1 (555) 123-4567
                </Button>
                <Button variant="outline" className="w-full justify-start">
                  <Mail className="h-4 w-4 mr-2" />
                  Email: support@hopngo.com
                </Button>
              </CardContent>
            </Card>
          </div>

          {/* Main Content */}
          <div className="lg:col-span-3">
            {searchQuery && (
              <div className="mb-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-2">
                  Search Results for "{searchQuery}"
                </h2>
                <p className="text-gray-600">
                  Found {filteredArticles.length} article{filteredArticles.length !== 1 ? 's' : ''}
                </p>
              </div>
            )}

            {!searchQuery && (
              <div className="mb-8">
                <h2 className="text-2xl font-bold text-gray-900 mb-4">
                  Popular Articles
                </h2>
              </div>
            )}

            {/* Articles Grid */}
            <div className="grid gap-6">
              {filteredArticles.map((article) => (
                <Card key={article.id} className="hover:shadow-md transition-shadow">
                  <CardContent className="p-6">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <Link href={`/help/${article.slug}`}>
                          <h3 className="text-lg font-semibold text-gray-900 hover:text-blue-600 mb-2">
                            {article.title}
                          </h3>
                        </Link>
                        <p className="text-gray-600 mb-3">
                          {article.excerpt}
                        </p>
                        <div className="flex items-center space-x-4">
                          <div className="flex flex-wrap gap-1">
                            {article.tags.map((tag) => (
                              <Badge key={tag} variant="secondary" className="text-xs">
                                {tag}
                              </Badge>
                            ))}
                          </div>
                          <span className="text-sm text-gray-500">
                            {article.viewCount.toLocaleString()} views
                          </span>
                        </div>
                      </div>
                      <BookOpen className="h-5 w-5 text-gray-400 ml-4" />
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {filteredArticles.length === 0 && (
              <div className="text-center py-12">
                <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  No articles found
                </h3>
                <p className="text-gray-600 mb-4">
                  Try adjusting your search terms or browse our categories.
                </p>
                <Button onClick={() => setSearchQuery('')}>
                  Clear Search
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}