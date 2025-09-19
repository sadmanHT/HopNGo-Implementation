'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Image from 'next/image';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  MapPin,
  TrendingUp,
  Sparkles,
  ShoppingBag,
  BookOpen,
  ArrowRight,
  Play,
  Heart,
  Star,
  Users,
  Clock,
  Camera,
  Navigation,
  Zap,
  Globe,
  Award,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { DestinationCard, DestinationGrid } from '@/components/bd/DestinationCard';

interface HeroSlide {
  id: string;
  title: string;
  titleBn: string;
  location: string;
  locationBn: string;
  description: string;
  descriptionBn: string;
  image: string;
  category: string;
  categoryBn: string;
}

interface TrendingDestination {
  id: string;
  name: string;
  nameBn: string;
  district: string;
  districtBn: string;
  image: string;
  rating: number;
  reviews: number;
  heatScore: number;
  category: string;
  categoryBn: string;
  price: string;
  priceBn: string;
}

interface HiddenGem {
  id: string;
  name: string;
  nameBn: string;
  location: string;
  locationBn: string;
  distance: string;
  distanceBn: string;
  image: string;
  description: string;
  descriptionBn: string;
  difficulty: 'easy' | 'moderate' | 'challenging';
  difficultyBn: string;
}

interface MarketPick {
  id: string;
  name: string;
  nameBn: string;
  category: string;
  categoryBn: string;
  price: number;
  originalPrice?: number;
  image: string;
  rating: number;
  reviews: number;
  badge?: string;
  badgeBn?: string;
}

interface Story {
  id: string;
  title: string;
  titleBn: string;
  author: string;
  authorBn: string;
  publishedAt: string;
  readTime: string;
  readTimeBn: string;
  image: string;
  excerpt: string;
  excerptBn: string;
  category: string;
  categoryBn: string;
}

const heroSlides: HeroSlide[] = [
  {
    id: '1',
    title: 'Sajek Valley',
    titleBn: 'সাজেক ভ্যালি',
    location: 'Rangamati, Chittagong Hill Tracts',
    locationBn: 'রাঙামাটি, পার্বত্য চট্টগ্রাম',
    description: 'Experience the queen of hills with breathtaking cloud-kissed peaks',
    descriptionBn: 'মেঘমালায় ঢাকা পাহাড়ের রানীর অভূতপূর্ব সৌন্দর্য উপভোগ করুন',
    image: '/images/hero/sajek-valley.jpg',
    category: 'Mountain',
    categoryBn: 'পাহাড়',
  },
  {
    id: '2',
    title: 'Srimangal Tea Gardens',
    titleBn: 'শ্রীমঙ্গল চা বাগান',
    location: 'Moulvibazar, Sylhet',
    locationBn: 'মৌলভীবাজার, সিলেট',
    description: 'Discover the tea capital of Bangladesh with endless green landscapes',
    descriptionBn: 'বাংলাদেশের চা রাজধানীর অসীম সবুজ প্রকৃতি আবিষ্কার করুন',
    image: '/images/hero/srimangal-tea.jpg',
    category: 'Nature',
    categoryBn: 'প্রকৃতি',
  },
  {
    id: '3',
    title: "Cox's Bazar Beach",
    titleBn: 'কক্সবাজার সমুদ্র সৈকত',
    location: "Cox's Bazar, Chittagong",
    locationBn: 'কক্সবাজার, চট্টগ্রাম',
    description: "World's longest natural sea beach with golden sand stretches",
    descriptionBn: 'বিশ্বের দীর্ঘতম প্রাকৃতিক সমুদ্র সৈকত ও সোনালী বালুর বিস্তৃতি',
    image: '/images/hero/coxs-bazar.jpg',
    category: 'Beach',
    categoryBn: 'সৈকত',
  },
  {
    id: '4',
    title: 'Bandarban Hills',
    titleBn: 'বান্দরবান পাহাড়',
    location: 'Bandarban, Chittagong Hill Tracts',
    locationBn: 'বান্দরবান, পার্বত্য চট্টগ্রাম',
    description: 'Adventure awaits in the highest peaks of Bangladesh',
    descriptionBn: 'বাংলাদেশের সর্বোচ্চ পর্বতশৃঙ্গে অ্যাডভেঞ্চারের অপেক্ষা',
    image: '/images/hero/bandarban.jpg',
    category: 'Adventure',
    categoryBn: 'অ্যাডভেঞ্চার',
  },
  {
    id: '5',
    title: 'Sundarbans Mangrove',
    titleBn: 'সুন্দরবন ম্যানগ্রোভ',
    location: 'Khulna & Barisal',
    locationBn: 'খুলনা ও বরিশাল',
    description: 'Explore the largest mangrove forest and home of Royal Bengal Tigers',
    descriptionBn: 'বিশ্বের বৃহত্তম ম্যানগ্রোভ বন ও রয়েল বেঙ্গল টাইগারের আবাসস্থল',
    image: '/images/hero/sundarbans.jpg',
    category: 'Wildlife',
    categoryBn: 'বন্যপ্রাণী',
  },
];

const trendingDestinations: TrendingDestination[] = [
  {
    id: '1',
    name: 'Kuakata Beach',
    nameBn: 'কুয়াকাটা সৈকত',
    district: 'Patuakhali',
    districtBn: 'পটুয়াখালী',
    image: '/images/destinations/kuakata.jpg',
    rating: 4.8,
    reviews: 1247,
    heatScore: 95,
    category: 'Beach',
    categoryBn: 'সৈকত',
    price: '৳2,500',
    priceBn: '২,৫০০ টাকা',
  },
  {
    id: '2',
    name: 'Ratargul Swamp Forest',
    nameBn: 'রাতারগুল জলাবন',
    district: 'Sylhet',
    districtBn: 'সিলেট',
    image: '/images/destinations/ratargul.jpg',
    rating: 4.6,
    reviews: 892,
    heatScore: 88,
    category: 'Nature',
    categoryBn: 'প্রকৃতি',
    price: '৳1,800',
    priceBn: '১,৮০০ টাকা',
  },
  {
    id: '3',
    name: 'Jaflong',
    nameBn: 'জাফলং',
    district: 'Sylhet',
    districtBn: 'সিলেট',
    image: '/images/destinations/jaflong.jpg',
    rating: 4.7,
    reviews: 1056,
    heatScore: 92,
    category: 'River',
    categoryBn: 'নদী',
    price: '৳2,200',
    priceBn: '২,২০০ টাকা',
  },
  {
    id: '4',
    name: 'Nilgiri Hills',
    nameBn: 'নীলগিরি পাহাড়',
    district: 'Bandarban',
    districtBn: 'বান্দরবান',
    image: '/images/destinations/nilgiri.jpg',
    rating: 4.9,
    reviews: 1389,
    heatScore: 97,
    category: 'Mountain',
    categoryBn: 'পাহাড়',
    price: '৳3,500',
    priceBn: '৩,৫০০ টাকা',
  },
];

const hiddenGems: HiddenGem[] = [
  {
    id: '1',
    name: 'Bichanakandi',
    nameBn: 'বিছানাকান্দি',
    location: 'Sylhet',
    locationBn: 'সিলেট',
    distance: '12 km from you',
    distanceBn: 'আপনার থেকে ১২ কিমি',
    image: '/images/gems/bichanakandi.jpg',
    description: 'Crystal clear water flowing through stone quarries',
    descriptionBn: 'পাথর খনির মধ্য দিয়ে প্রবাহিত স্বচ্ছ জলধারা',
    difficulty: 'easy',
    difficultyBn: 'সহজ',
  },
  {
    id: '2',
    name: 'Tanguar Haor',
    nameBn: 'টাঙ্গুয়ার হাওর',
    location: 'Sunamganj',
    locationBn: 'সুনামগঞ্জ',
    distance: '45 km from you',
    distanceBn: 'আপনার থেকে ৪৫ কিমি',
    image: '/images/gems/tanguar-haor.jpg',
    description: 'Wetland paradise with diverse bird species',
    descriptionBn: 'বিভিন্ন পাখির প্রজাতির জলাভূমি স্বর্গ',
    difficulty: 'moderate',
    difficultyBn: 'মাঝারি',
  },
  {
    id: '3',
    name: 'Nafakhum Waterfall',
    nameBn: 'নাফাখুম জলপ্রপাত',
    location: 'Bandarban',
    locationBn: 'বান্দরবান',
    distance: '78 km from you',
    distanceBn: 'আপনার থেকে ৭৮ কিমি',
    image: '/images/gems/nafakhum.jpg',
    description: 'Largest waterfall in Bangladesh hidden in hills',
    descriptionBn: 'পাহাড়ে লুকানো বাংলাদেশের বৃহত্তম জলপ্রপাত',
    difficulty: 'challenging',
    difficultyBn: 'কঠিন',
  },
];

const marketPicks: MarketPick[] = [
  {
    id: '1',
    name: 'Waterproof Hiking Backpack',
    nameBn: 'জলরোধী হাইকিং ব্যাকপ্যাক',
    category: 'Gear',
    categoryBn: 'সরঞ্জাম',
    price: 3500,
    originalPrice: 4200,
    image: '/images/gear/backpack.jpg',
    rating: 4.8,
    reviews: 234,
    badge: 'Best Seller',
    badgeBn: 'সর্বাধিক বিক্রিত',
  },
  {
    id: '2',
    name: 'Portable Water Filter',
    nameBn: 'বহনযোগ্য পানি ফিল্টার',
    category: 'Safety',
    categoryBn: 'নিরাপত্তা',
    price: 1800,
    image: '/images/gear/water-filter.jpg',
    rating: 4.6,
    reviews: 156,
  },
  {
    id: '3',
    name: 'Quick-Dry Travel Towel',
    nameBn: 'দ্রুত শুকানো ভ্রমণ তোয়ালে',
    category: 'Comfort',
    categoryBn: 'আরাম',
    price: 850,
    originalPrice: 1200,
    image: '/images/gear/towel.jpg',
    rating: 4.4,
    reviews: 89,
    badge: 'New',
    badgeBn: 'নতুন',
  },
  {
    id: '4',
    name: 'Solar Power Bank',
    nameBn: 'সোলার পাওয়ার ব্যাংক',
    category: 'Electronics',
    categoryBn: 'ইলেকট্রনিক্স',
    price: 2200,
    image: '/images/gear/power-bank.jpg',
    rating: 4.7,
    reviews: 178,
  },
];

const latestStories: Story[] = [
  {
    id: '1',
    title: 'Hidden Waterfalls of Chittagong Hill Tracts',
    titleBn: 'পার্বত্য চট্টগ্রামের গোপন জলপ্রপাত',
    author: 'Rashida Khatun',
    authorBn: 'রশিদা খাতুন',
    publishedAt: '2024-01-15',
    readTime: '8 min read',
    readTimeBn: '৮ মিনিট পড়া',
    image: '/images/stories/waterfalls.jpg',
    excerpt: 'Discover the untouched beauty of cascading waters nestled in the green hills...',
    excerptBn: 'সবুজ পাহাড়ে লুকানো জলপ্রপাতের অস্পৃশ্য সৌন্দর্য আবিষ্কার করুন...',
    category: 'Adventure',
    categoryBn: 'অ্যাডভেঞ্চার',
  },
  {
    id: '2',
    title: 'A Foodie\'s Guide to Old Dhaka',
    titleBn: 'পুরান ঢাকার খাদ্যপ্রেমীর গাইড',
    author: 'Karim Ahmed',
    authorBn: 'করিম আহমেদ',
    publishedAt: '2024-01-12',
    readTime: '12 min read',
    readTimeBn: '১২ মিনিট পড়া',
    image: '/images/stories/old-dhaka-food.jpg',
    excerpt: 'From biriyani to fuchka, explore the culinary treasures of historic Dhaka...',
    excerptBn: 'বিরিয়ানি থেকে ফুচকা, ঐতিহাসিক ঢাকার রন্ধনসম্পদ অন্বেষণ করুন...',
    category: 'Food',
    categoryBn: 'খাবার',
  },
  {
    id: '3',
    title: 'Sustainable Tourism in Sundarbans',
    titleBn: 'সুন্দরবনে টেকসই পর্যটন',
    author: 'Dr. Fatima Rahman',
    authorBn: 'ড. ফাতিমা রহমান',
    publishedAt: '2024-01-10',
    readTime: '15 min read',
    readTimeBn: '১৫ মিনিট পড়া',
    image: '/images/stories/sundarbans-eco.jpg',
    excerpt: 'How responsible travel can help preserve our precious mangrove ecosystem...',
    excerptBn: 'কীভাবে দায়িত্বশীল ভ্রমণ আমাদের মূল্যবান ম্যানগ্রোভ বাস্তুতন্ত্র রক্ষা করতে পারে...',
    category: 'Conservation',
    categoryBn: 'সংরক্ষণ',
  },
];

export default function HomePage() {
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(true);

  useEffect(() => {
    if (!isAutoPlaying) return;
    
    const interval = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % heroSlides.length);
    }, 5000);

    return () => clearInterval(interval);
  }, [isAutoPlaying]);

  const nextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % heroSlides.length);
    setIsAutoPlaying(false);
  };

  const prevSlide = () => {
    setCurrentSlide((prev) => (prev - 1 + heroSlides.length) % heroSlides.length);
    setIsAutoPlaying(false);
  };

  const goToSlide = (index: number) => {
    setCurrentSlide(index);
    setIsAutoPlaying(false);
  };

  const currentSlideData = heroSlides[currentSlide];

  return (
    <div className="min-h-screen bg-gradient-to-b from-bd-sand/20 to-white">
      {/* Hero Section */}
      <section className="relative h-screen overflow-hidden">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentSlide}
            className="absolute inset-0"
            initial={{ opacity: 0, scale: 1.1 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.7, ease: "easeInOut" }}
          >
            <div className="relative h-full">
              <Image
                src={currentSlideData.image}
                alt={currentSlideData.title}
                fill
                className="object-cover"
                priority
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent" />
            </div>
          </motion.div>
        </AnimatePresence>

        {/* Hero Content */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="text-center text-white max-w-4xl mx-auto px-6">
            <motion.div
              key={`content-${currentSlide}`}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
            >
              <Badge className="mb-4 bg-white/20 backdrop-blur-sm text-white border-white/30 hover:bg-white/30">
                <MapPin className="w-3 h-3 mr-1" />
                {currentSlideData.category} / {currentSlideData.categoryBn}
              </Badge>
              
              <h1 className="text-5xl md:text-7xl font-bold mb-4 bg-gradient-to-r from-white to-bd-sand bg-clip-text text-transparent">
                {currentSlideData.title}
              </h1>
              
              <h2 className="text-2xl md:text-3xl font-bengali font-medium mb-6 text-bd-sand">
                {currentSlideData.titleBn}
              </h2>
              
              <p className="text-lg md:text-xl mb-2 text-white/90">
                {currentSlideData.location}
              </p>
              
              <p className="text-base md:text-lg font-bengali mb-8 text-bd-sand/90">
                {currentSlideData.locationBn}
              </p>
              
              <p className="text-lg md:text-xl mb-4 max-w-2xl mx-auto text-white/80">
                {currentSlideData.description}
              </p>
              
              <p className="text-base md:text-lg font-bengali mb-12 max-w-2xl mx-auto text-bd-sand/80">
                {currentSlideData.descriptionBn}
              </p>
              
              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <Button size="lg" className="bg-bd-green hover:bg-bd-green/90 text-white px-8 py-3 text-lg">
                  <Navigation className="w-5 h-5 mr-2" />
                  Explore Now / এখনই অন্বেষণ করুন
                </Button>
                
                <Button size="lg" variant="outline" className="border-white/30 text-white hover:bg-white/10 px-8 py-3 text-lg backdrop-blur-sm">
                  <Play className="w-5 h-5 mr-2" />
                  Watch Video / ভিডিও দেখুন
                </Button>
              </div>
            </motion.div>
          </div>
        </div>

        {/* Navigation Controls */}
        <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 flex space-x-2">
          {heroSlides.map((_, index) => (
            <button
              key={index}
              onClick={() => goToSlide(index)}
              className={cn(
                "w-3 h-3 rounded-full transition-all duration-300",
                index === currentSlide
                  ? "bg-white scale-125"
                  : "bg-white/50 hover:bg-white/70"
              )}
            />
          ))}
        </div>

        {/* Arrow Navigation */}
        <button
          onClick={prevSlide}
          className="absolute left-6 top-1/2 transform -translate-y-1/2 w-12 h-12 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center text-white hover:bg-white/30 transition-all duration-200"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>
        
        <button
          onClick={nextSlide}
          className="absolute right-6 top-1/2 transform -translate-y-1/2 w-12 h-12 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center text-white hover:bg-white/30 transition-all duration-200"
        >
          <ChevronRight className="w-6 h-6" />
        </button>
      </section>

      {/* Trending Now Section */}
      <section className="py-20 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <div className="flex items-center justify-center mb-4">
              <TrendingUp className="w-8 h-8 text-bd-coral mr-3" />
              <h2 className="text-4xl md:text-5xl font-bold text-bd-slate">
                Trending Now in Bangladesh
              </h2>
            </div>
            <p className="text-xl font-bengali text-bd-slate/70 mb-2">
              বাংলাদেশে এখন ট্রেন্ডিং
            </p>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Discover the hottest destinations based on real-time activity and traveler preferences
            </p>
          </div>

          <DestinationGrid
            destinations={trendingDestinations.map((destination) => ({
              id: destination.id,
              title: destination.name,
              titleBn: destination.nameBn,
              location: destination.district,
              locationBn: destination.districtBn,
              district: destination.district,
              districtBn: destination.districtBn,
              image: destination.image,
              rating: destination.rating,
              reviewCount: destination.reviews,
              price: parseInt(destination.price.replace(/[^0-9]/g, '')),
              tags: [destination.category],
              href: `/destinations/${destination.id}`
            }))}
          />

          <div className="text-center mt-12">
            <Button size="lg" variant="outline" className="border-bd-green text-bd-green hover:bg-bd-green hover:text-white">
              View All Trending / সব ট্রেন্ডিং দেখুন
              <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
          </div>
        </div>
      </section>

      {/* Hidden Gems Section */}
      <section className="py-20 px-6 bg-gradient-to-r from-bd-sand/10 to-bd-teal/10">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <div className="flex items-center justify-center mb-4">
              <Sparkles className="w-8 h-8 text-bd-teal mr-3" />
              <h2 className="text-4xl md:text-5xl font-bold text-bd-slate">
                Hidden Gems Near You
              </h2>
            </div>
            <p className="text-xl font-bengali text-bd-slate/70 mb-2">
              আপনার কাছের লুকানো রত্ন
            </p>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Undiscovered treasures waiting to be explored, personalized based on your location
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {hiddenGems.map((gem, index) => (
              <motion.div
                key={gem.id}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                viewport={{ once: true }}
              >
                <Card className="group hover:shadow-xl transition-all duration-300 overflow-hidden border-0 bg-white/80 backdrop-blur-sm">
                  <div className="relative h-48 overflow-hidden">
                    <Image
                      src={gem.image}
                      alt={gem.name}
                      fill
                      className="object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
                    <div className="absolute top-4 left-4">
                      <Badge className={cn(
                        "text-xs",
                        gem.difficulty === 'easy' && "bg-green-100 text-green-800",
                        gem.difficulty === 'moderate' && "bg-yellow-100 text-yellow-800",
                        gem.difficulty === 'challenging' && "bg-red-100 text-red-800"
                      )}>
                        {gem.difficultyBn}
                      </Badge>
                    </div>
                    <div className="absolute bottom-4 left-4 text-white">
                      <div className="flex items-center text-sm">
                        <MapPin className="w-4 h-4 mr-1" />
                        {gem.distance}
                      </div>
                    </div>
                  </div>
                  
                  <CardContent className="p-6">
                    <h3 className="text-xl font-bold text-bd-slate mb-1">{gem.name}</h3>
                    <p className="text-lg font-bengali text-bd-slate/70 mb-2">{gem.nameBn}</p>
                    <p className="text-sm text-muted-foreground mb-1">{gem.location}</p>
                    <p className="text-sm font-bengali text-muted-foreground mb-4">{gem.locationBn}</p>
                    <p className="text-sm text-bd-slate/80 mb-2">{gem.description}</p>
                    <p className="text-sm font-bengali text-bd-slate/60 mb-4">{gem.descriptionBn}</p>
                    
                    <Button className="w-full bg-bd-teal hover:bg-bd-teal/90 text-white">
                      <Navigation className="w-4 h-4 mr-2" />
                      Discover / আবিষ্কার করুন
                    </Button>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Plan with AI Section */}
      <section className="py-20 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            viewport={{ once: true }}
            className="bg-gradient-to-br from-bd-green to-bd-teal rounded-3xl p-12 text-white relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-[url('/images/patterns/circuit.svg')] opacity-10" />
            <div className="relative z-10">
              <div className="flex items-center justify-center mb-6">
                <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center mb-4">
                  <Zap className="w-8 h-8 text-white" />
                </div>
              </div>
              
              <h2 className="text-4xl md:text-5xl font-bold mb-4">
                Plan with AI
              </h2>
              
              <p className="text-xl font-bengali mb-6">
                এআই দিয়ে পরিকল্পনা করুন
              </p>
              
              <p className="text-lg mb-8 opacity-90 max-w-2xl mx-auto">
                Let our intelligent travel assistant create personalized itineraries based on your preferences, budget, and time constraints
              </p>
              
              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <Button size="lg" className="bg-white text-bd-green hover:bg-white/90 px-8 py-3 text-lg font-semibold">
                  <Sparkles className="w-5 h-5 mr-2" />
                  Start Planning / পরিকল্পনা শুরু করুন
                </Button>
                
                <Button size="lg" variant="outline" className="border-white/30 text-white hover:bg-white/10 px-8 py-3 text-lg">
                  <Play className="w-5 h-5 mr-2" />
                  See How It Works / কীভাবে কাজ করে দেখুন
                </Button>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Gear Up Section */}
      <section className="py-20 px-6 bg-gradient-to-r from-bd-sunrise/10 to-bd-coral/10">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <div className="flex items-center justify-center mb-4">
              <ShoppingBag className="w-8 h-8 text-bd-sunrise mr-3" />
              <h2 className="text-4xl md:text-5xl font-bold text-bd-slate">
                Gear Up
              </h2>
            </div>
            <p className="text-xl font-bengali text-bd-slate/70 mb-2">
              সরঞ্জাম সংগ্রহ করুন
            </p>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Essential travel gear handpicked by experienced travelers and local experts
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {marketPicks.map((item, index) => (
              <motion.div
                key={item.id}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                viewport={{ once: true }}
              >
                <Card className="group hover:shadow-xl transition-all duration-300 overflow-hidden border-0 bg-white/80 backdrop-blur-sm">
                  <div className="relative h-48 overflow-hidden">
                    <Image
                      src={item.image}
                      alt={item.name}
                      fill
                      className="object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    {item.badge && (
                      <div className="absolute top-4 left-4">
                        <Badge className="bg-bd-coral text-white">
                          {item.badgeBn}
                        </Badge>
                      </div>
                    )}
                    <div className="absolute top-4 right-4">
                      <Button size="sm" variant="ghost" className="w-8 h-8 p-0 bg-white/20 backdrop-blur-sm hover:bg-white/30">
                        <Heart className="w-4 h-4 text-white" />
                      </Button>
                    </div>
                  </div>
                  
                  <CardContent className="p-4">
                    <div className="mb-2">
                      <Badge variant="outline" className="text-xs mb-2">
                        {item.categoryBn}
                      </Badge>
                    </div>
                    
                    <h3 className="font-semibold text-bd-slate mb-1 line-clamp-2">{item.name}</h3>
                    <p className="text-sm font-bengali text-bd-slate/70 mb-3 line-clamp-1">{item.nameBn}</p>
                    
                    <div className="flex items-center mb-3">
                      <div className="flex items-center">
                        {[...Array(5)].map((_, i) => (
                          <Star
                            key={i}
                            className={cn(
                              "w-3 h-3",
                              i < Math.floor(item.rating)
                                ? "text-bd-sunrise fill-current"
                                : "text-gray-300"
                            )}
                          />
                        ))}
                      </div>
                      <span className="text-xs text-muted-foreground ml-2">({item.reviews})</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="text-lg font-bold text-bd-green">৳{item.price.toLocaleString()}</span>
                        {item.originalPrice && (
                          <span className="text-sm text-muted-foreground line-through ml-2">
                            ৳{item.originalPrice.toLocaleString()}
                          </span>
                        )}
                      </div>
                      <Button size="sm" className="bg-bd-sunrise hover:bg-bd-sunrise/90 text-white">
                        Add / যোগ করুন
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>

          <div className="text-center mt-12">
            <Button size="lg" variant="outline" className="border-bd-sunrise text-bd-sunrise hover:bg-bd-sunrise hover:text-white">
              Browse All Gear / সব সরঞ্জাম ব্রাউজ করুন
              <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
          </div>
        </div>
      </section>

      {/* Latest Stories Section */}
      <section className="py-20 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <div className="flex items-center justify-center mb-4">
              <BookOpen className="w-8 h-8 text-bd-green mr-3" />
              <h2 className="text-4xl md:text-5xl font-bold text-bd-slate">
                Latest Stories
              </h2>
            </div>
            <p className="text-xl font-bengali text-bd-slate/70 mb-2">
              সর্বশেষ গল্প
            </p>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Real experiences and insights from fellow travelers exploring Bangladesh
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {latestStories.map((story, index) => (
              <motion.div
                key={story.id}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                viewport={{ once: true }}
              >
                <Card className="group hover:shadow-xl transition-all duration-300 overflow-hidden border-0 bg-white/80 backdrop-blur-sm h-full">
                  <div className="relative h-48 overflow-hidden">
                    <Image
                      src={story.image}
                      alt={story.title}
                      fill
                      className="object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
                    <div className="absolute top-4 left-4">
                      <Badge className="bg-bd-green text-white">
                        {story.categoryBn}
                      </Badge>
                    </div>
                  </div>
                  
                  <CardContent className="p-6 flex flex-col flex-1">
                    <div className="flex-1">
                      <h3 className="text-xl font-bold text-bd-slate mb-2 line-clamp-2">{story.title}</h3>
                      <p className="text-lg font-bengali text-bd-slate/70 mb-4 line-clamp-1">{story.titleBn}</p>
                      <p className="text-sm text-bd-slate/80 mb-4 line-clamp-3">{story.excerpt}</p>
                      <p className="text-sm font-bengali text-bd-slate/60 mb-6 line-clamp-2">{story.excerptBn}</p>
                    </div>
                    
                    <div className="flex items-center justify-between text-sm text-muted-foreground mb-4">
                      <div className="flex items-center">
                        <div className="w-8 h-8 bg-bd-green/10 rounded-full flex items-center justify-center mr-2">
                          <Users className="w-4 h-4 text-bd-green" />
                        </div>
                        <div>
                          <p className="font-medium text-bd-slate">{story.author}</p>
                          <p className="text-xs font-bengali">{story.authorBn}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="flex items-center">
                          <Clock className="w-3 h-3 mr-1" />
                          <span>{story.readTime}</span>
                        </div>
                        <p className="text-xs font-bengali">{story.readTimeBn}</p>
                      </div>
                    </div>
                    
                    <Button variant="outline" className="w-full border-bd-green text-bd-green hover:bg-bd-green hover:text-white">
                      <BookOpen className="w-4 h-4 mr-2" />
                      Read Story / গল্প পড়ুন
                    </Button>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>

          <div className="text-center mt-12">
            <Button size="lg" variant="outline" className="border-bd-green text-bd-green hover:bg-bd-green hover:text-white">
              View All Stories / সব গল্প দেখুন
              <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
          </div>
        </div>
      </section>

      {/* Newsletter Section */}
      <section className="py-20 px-6 bg-gradient-to-r from-bd-green to-bd-teal">
        <div className="max-w-4xl mx-auto text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            viewport={{ once: true }}
            className="text-white"
          >
            <div className="flex items-center justify-center mb-6">
              <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center">
                <Globe className="w-8 h-8 text-white" />
              </div>
            </div>
            
            <h2 className="text-4xl md:text-5xl font-bold mb-4">
              Stay Connected
            </h2>
            
            <p className="text-xl font-bengali mb-6">
              যোগাযোগে থাকুন
            </p>
            
            <p className="text-lg mb-8 opacity-90 max-w-2xl mx-auto">
              Get the latest travel insights, hidden gems, and exclusive offers delivered to your inbox
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 max-w-md mx-auto">
              <Input
                type="email"
                placeholder="Enter your email / আপনার ইমেইল দিন"
                className="flex-1 bg-white/10 border-white/20 text-white placeholder:text-white/60 focus:bg-white/20"
              />
              <Button className="bg-white text-bd-green hover:bg-white/90 px-8">
                Subscribe / সাবস্ক্রাইব করুন
              </Button>
            </div>
            
            <p className="text-sm opacity-70 mt-4">
              Join 50,000+ travelers exploring Bangladesh / ৫০,০০০+ ভ্রমণকারীর সাথে যোগ দিন
            </p>
          </motion.div>
        </div>
      </section>
    </div>
  );
}