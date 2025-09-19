'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Facebook,
  Instagram,
  Twitter,
  Youtube,
  Mail,
  Phone,
  MapPin,
  Send,
  Heart,
  Globe,
  Shield,
  Award,
  Users,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { toast } from '@/hooks/use-toast';

interface FooterLink {
  name: string;
  nameBn?: string;
  href: string;
}

interface FooterSection {
  title: string;
  titleBn?: string;
  links: FooterLink[];
}

const footerSections: FooterSection[] = [
  {
    title: 'Explore',
    titleBn: 'অন্বেষণ',
    links: [
      { name: 'Destinations', nameBn: 'গন্তব্য', href: '/destinations' },
      { name: 'Experiences', nameBn: 'অভিজ্ঞতা', href: '/experiences' },
      { name: 'Tours', nameBn: 'ট্যুর', href: '/tours' },
      { name: 'Hotels', nameBn: 'হোটেল', href: '/hotels' },
      { name: 'Local Guides', nameBn: 'স্থানীয় গাইড', href: '/guides' },
    ],
  },
  {
    title: 'Services',
    titleBn: 'সেবা',
    links: [
      { name: 'Trip Planning', nameBn: 'ভ্রমণ পরিকল্পনা', href: '/trip-planning' },
      { name: 'Marketplace', nameBn: 'বাজার', href: '/marketplace' },
      { name: 'Travel Insurance', nameBn: 'ভ্রমণ বীমা', href: '/insurance' },
      { name: 'Visa Support', nameBn: 'ভিসা সহায়তা', href: '/visa' },
      { name: 'Emergency Help', nameBn: 'জরুরি সাহায্য', href: '/emergency' },
    ],
  },
  {
    title: 'Community',
    titleBn: 'সম্প্রদায়',
    links: [
      { name: 'Travel Stories', nameBn: 'ভ্রমণ কাহিনী', href: '/stories' },
      { name: 'Forums', nameBn: 'ফোরাম', href: '/forums' },
      { name: 'Events', nameBn: 'ইভেন্ট', href: '/events' },
      { name: 'Travel Tips', nameBn: 'ভ্রমণ টিপস', href: '/tips' },
      { name: 'Photo Contest', nameBn: 'ছবি প্রতিযোগিতা', href: '/contest' },
    ],
  },
  {
    title: 'Support',
    titleBn: 'সহায়তা',
    links: [
      { name: 'Help Center', nameBn: 'সাহায্য কেন্দ্র', href: '/help' },
      { name: 'Contact Us', nameBn: 'যোগাযোগ', href: '/contact' },
      { name: 'Safety Guidelines', nameBn: 'নিরাপত্তা নির্দেশিকা', href: '/safety' },
      { name: 'Terms of Service', nameBn: 'সেবার শর্তাবলী', href: '/terms' },
      { name: 'Privacy Policy', nameBn: 'গোপনীয়তা নীতি', href: '/privacy' },
    ],
  },
];

const socialLinks = [
  { name: 'Facebook', icon: Facebook, href: 'https://facebook.com/hopngo', color: 'hover:text-blue-600' },
  { name: 'Instagram', icon: Instagram, href: 'https://instagram.com/hopngo', color: 'hover:text-pink-600' },
  { name: 'Twitter', icon: Twitter, href: 'https://twitter.com/hopngo', color: 'hover:text-blue-400' },
  { name: 'YouTube', icon: Youtube, href: 'https://youtube.com/hopngo', color: 'hover:text-red-600' },
];

const popularDestinations = [
  { name: 'Cox\'s Bazar', nameBn: 'কক্সবাজার', href: '/destinations/coxs-bazar' },
  { name: 'Sundarbans', nameBn: 'সুন্দরবন', href: '/destinations/sundarbans' },
  { name: 'Sajek Valley', nameBn: 'সাজেক ভ্যালি', href: '/destinations/sajek' },
  { name: 'Srimangal', nameBn: 'শ্রীমঙ্গল', href: '/destinations/srimangal' },
  { name: 'Bandarban', nameBn: 'বান্দরবান', href: '/destinations/bandarban' },
  { name: 'Rangamati', nameBn: 'রাঙামাটি', href: '/destinations/rangamati' },
];

interface BdFooterProps {
  className?: string;
}

export function BdFooter({ className }: BdFooterProps) {
  const [email, setEmail] = useState('');
  const [isSubscribing, setIsSubscribing] = useState(false);

  const handleNewsletterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;

    setIsSubscribing(true);
    
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      toast({
        title: "Subscription Successful! / সাবস্ক্রিপশন সফল!",
        description: "You'll receive travel updates and exclusive offers. / আপনি ভ্রমণ আপডেট এবং বিশেষ অফার পাবেন।",
      });
      
      setEmail('');
    } catch (error) {
      toast({
        title: "Subscription Failed / সাবস্ক্রিপশন ব্যর্থ",
        description: "Please try again later. / পরে আবার চেষ্টা করুন।",
        variant: "destructive",
      });
    } finally {
      setIsSubscribing(false);
    }
  };

  return (
    <footer className={cn(
      "bg-gradient-to-br from-bd-slate via-bd-slate/95 to-bd-slate/90 text-white relative overflow-hidden",
      className
    )}>
      {/* Background Pattern */}
      <div className="absolute inset-0 opacity-5">
        <div className="absolute inset-0" style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
        }} />
      </div>

      <div className="relative">
        {/* Main Footer Content */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-8">
            {/* Brand Section */}
            <div className="lg:col-span-2 space-y-6">
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-gradient-to-br from-bd-green to-bd-teal rounded-xl flex items-center justify-center shadow-lg">
                  <span className="text-white font-bold text-xl font-bengali">হ</span>
                </div>
                <div>
                  <div className="text-2xl font-bold">HopNGo</div>
                  <div className="text-sm text-gray-300 font-bengali">বাংলাদেশের ভ্রমণ সাথী</div>
                </div>
              </div>
              
              <p className="text-gray-300 text-sm leading-relaxed">
                Discover the beauty of Bangladesh with local insights, authentic experiences, and trusted travel services.
              </p>
              
              <p className="text-gray-300 text-sm leading-relaxed font-bengali">
                স্থানীয় অন্তর্দৃষ্টি, প্রামাণিক অভিজ্ঞতা এবং বিশ্বস্ত ভ্রমণ সেবার সাথে বাংলাদেশের সৌন্দর্য আবিষ্কার করুন।
              </p>

              {/* Trust Badges */}
              <div className="flex items-center space-x-4 pt-2">
                <div className="flex items-center space-x-1 text-xs text-gray-400">
                  <Shield className="w-4 h-4 text-bd-green" />
                  <span>Secure</span>
                </div>
                <div className="flex items-center space-x-1 text-xs text-gray-400">
                  <Award className="w-4 h-4 text-bd-sunrise" />
                  <span>Verified</span>
                </div>
                <div className="flex items-center space-x-1 text-xs text-gray-400">
                  <Users className="w-4 h-4 text-bd-teal" />
                  <span>10K+ Travelers</span>
                </div>
              </div>
            </div>

            {/* Footer Sections */}
            {footerSections.map((section, index) => (
              <div key={section.title} className="space-y-4">
                <h3 className="font-semibold text-white">
                  {section.title}
                  {section.titleBn && (
                    <span className="block text-sm text-gray-300 font-bengali mt-1">
                      {section.titleBn}
                    </span>
                  )}
                </h3>
                <ul className="space-y-2">
                  {section.links.map((link) => (
                    <li key={link.href}>
                      <Link
                        href={link.href}
                        className="text-gray-300 hover:text-white text-sm transition-colors duration-200 hover:translate-x-1 inline-block"
                      >
                        {link.name}
                        {link.nameBn && (
                          <span className="block text-xs text-gray-400 font-bengali">
                            {link.nameBn}
                          </span>
                        )}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>

          {/* Newsletter Section */}
          <div className="mt-12 pt-8 border-t border-gray-700">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
              <div>
                <h3 className="text-xl font-semibold text-white mb-2">
                  Stay Updated / আপডেট থাকুন
                </h3>
                <p className="text-gray-300 text-sm">
                  Get travel tips, destination guides, and exclusive offers delivered to your inbox.
                </p>
                <p className="text-gray-300 text-sm font-bengali mt-1">
                  ভ্রমণ টিপস, গন্তব্য গাইড এবং বিশেষ অফার আপনার ইনবক্সে পান।
                </p>
              </div>
              
              <form onSubmit={handleNewsletterSubmit} className="flex space-x-2">
                <div className="flex-1">
                  <Input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Enter your email / ইমেইল দিন"
                    className="bg-white/10 border-white/20 text-white placeholder:text-gray-400 focus:bg-white/20 focus:border-bd-green"
                    required
                  />
                </div>
                <Button
                  type="submit"
                  disabled={isSubscribing}
                  className="bg-bd-green hover:bg-bd-green/90 text-white px-6"
                >
                  {isSubscribing ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Send className="w-4 h-4 mr-2" />
                      Subscribe
                    </>
                  )}
                </Button>
              </form>
            </div>
          </div>

          {/* Popular Destinations */}
          <div className="mt-8 pt-8 border-t border-gray-700">
            <h3 className="text-lg font-semibold text-white mb-4">
              Popular Destinations / জনপ্রিয় গন্তব্য
            </h3>
            <div className="flex flex-wrap gap-2">
              {popularDestinations.map((destination) => (
                <Link key={destination.href} href={destination.href}>
                  <Badge 
                    variant="outline" 
                    className="border-white/20 text-gray-300 hover:bg-white/10 hover:text-white transition-colors duration-200 cursor-pointer"
                  >
                    {destination.name}
                    <span className="ml-1 text-xs font-bengali">({destination.nameBn})</span>
                  </Badge>
                </Link>
              ))}
            </div>
          </div>
        </div>

        {/* Bottom Footer */}
        <div className="border-t border-gray-700 bg-black/20">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            <div className="flex flex-col md:flex-row items-center justify-between space-y-4 md:space-y-0">
              {/* Copyright */}
              <div className="text-center md:text-left">
                <p className="text-gray-400 text-sm">
                  © 2024 HopNGo. All rights reserved. Made with{' '}
                  <Heart className="inline w-4 h-4 text-bd-coral mx-1" />
                  in Bangladesh
                </p>
                <p className="text-gray-400 text-xs font-bengali mt-1">
                  © ২০২৪ হপএনগো। সকল অধিকার সংরক্ষিত। বাংলাদেশে ভালোবাসা দিয়ে তৈরি
                </p>
              </div>

              {/* Social Links */}
              <div className="flex items-center space-x-4">
                <span className="text-gray-400 text-sm mr-2">Follow us:</span>
                {socialLinks.map((social) => {
                  const Icon = social.icon;
                  return (
                    <Link
                      key={social.name}
                      href={social.href}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={cn(
                        "text-gray-400 transition-all duration-200 hover:scale-110",
                        social.color
                      )}
                      aria-label={`Follow us on ${social.name}`}
                    >
                      <Icon className="w-5 h-5" />
                    </Link>
                  );
                })}
              </div>

              {/* Contact Info */}
              <div className="flex items-center space-x-4 text-gray-400 text-sm">
                <div className="flex items-center space-x-1">
                  <Phone className="w-4 h-4" />
                  <span>+880 1234-567890</span>
                </div>
                <div className="flex items-center space-x-1">
                  <Mail className="w-4 h-4" />
                  <span>hello@hopngo.com</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}