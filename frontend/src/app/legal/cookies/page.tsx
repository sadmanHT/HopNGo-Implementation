'use client';

import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { ArrowLeft, Calendar, FileText, Cookie, Settings, Shield, BarChart, Target } from 'lucide-react';

export default function CookiePolicyPage() {
  const lastUpdated = '2024-01-15';

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-amber-100 py-12">
      <div className="container mx-auto px-4 max-w-4xl">
        {/* Header */}
        <div className="mb-8">
          <Link href="/" className="inline-flex items-center text-orange-600 hover:text-orange-800 mb-4">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Home
          </Link>
          
          <div className="flex items-center gap-4 mb-4">
            <div className="p-3 bg-orange-100 rounded-lg">
              <Cookie className="w-8 h-8 text-orange-600" />
            </div>
            <div>
              <h1 className="text-4xl font-bold text-gray-900">Cookie Policy</h1>
              <div className="flex items-center text-gray-600 mt-2">
                <Calendar className="w-4 h-4 mr-2" />
                <span>Last updated: {lastUpdated}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Notice */}
        <Card className="mb-8 border-amber-200 bg-amber-50">
          <CardContent className="pt-6">
            <div className="flex items-start gap-3">
              <FileText className="w-5 h-5 text-amber-600 mt-0.5" />
              <div>
                <h3 className="font-semibold text-amber-800 mb-2">Legal Review Required</h3>
                <p className="text-amber-700 text-sm">
                  This is placeholder content for legal review. The actual cookie policy should be 
                  drafted and reviewed by qualified legal counsel and privacy experts before publication.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Cookie Preferences Panel */}
        <Card className="mb-8 border-blue-200 bg-blue-50">
          <CardHeader>
            <div className="flex items-center gap-2">
              <Settings className="w-5 h-5 text-blue-600" />
              <CardTitle className="text-xl text-blue-800">Cookie Preferences</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-blue-700 mb-4">
              Manage your cookie preferences below. Changes will take effect immediately.
            </p>
            
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-white rounded-lg border">
                <div className="flex items-start gap-3">
                  <Shield className="w-5 h-5 text-green-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-gray-800">Essential Cookies</h3>
                    <p className="text-sm text-gray-600">Required for basic site functionality</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-500">Always On</span>
                  <Switch checked disabled />
                </div>
              </div>
              
              <div className="flex items-center justify-between p-4 bg-white rounded-lg border">
                <div className="flex items-start gap-3">
                  <BarChart className="w-5 h-5 text-blue-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-gray-800">Analytics Cookies</h3>
                    <p className="text-sm text-gray-600">Help us understand how you use our site</p>
                  </div>
                </div>
                <Switch defaultChecked />
              </div>
              
              <div className="flex items-center justify-between p-4 bg-white rounded-lg border">
                <div className="flex items-start gap-3">
                  <Target className="w-5 h-5 text-purple-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-gray-800">Marketing Cookies</h3>
                    <p className="text-sm text-gray-600">Used for personalized advertising</p>
                  </div>
                </div>
                <Switch />
              </div>
              
              <div className="flex items-center justify-between p-4 bg-white rounded-lg border">
                <div className="flex items-start gap-3">
                  <Settings className="w-5 h-5 text-gray-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-gray-800">Functional Cookies</h3>
                    <p className="text-sm text-gray-600">Remember your preferences and settings</p>
                  </div>
                </div>
                <Switch defaultChecked />
              </div>
            </div>
            
            <div className="mt-4 flex gap-2">
              <Button size="sm">
                Save Preferences
              </Button>
              <Button size="sm" variant="outline">
                Accept All
              </Button>
              <Button size="sm" variant="outline">
                Reject All
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Cookie Policy Content */}
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">About Cookies and Tracking</CardTitle>
          </CardHeader>
          <CardContent className="prose prose-gray max-w-none">
            <div className="space-y-8">
              <section>
                <h2 className="text-xl font-semibold mb-4">1. What Are Cookies?</h2>
                <p className="text-gray-700 leading-relaxed">
                  Cookies are small text files that are stored on your device when you visit our website. 
                  They help us provide you with a better experience by remembering your preferences and 
                  understanding how you use our site.
                </p>
                
                <div className="mt-4 bg-gray-50 p-4 rounded-lg">
                  <h3 className="font-semibold text-gray-800 mb-2">Types of Cookies We Use</h3>
                  <ul className="list-disc list-inside text-gray-700 space-y-1">
                    <li><strong>Session Cookies:</strong> Temporary cookies that expire when you close your browser</li>
                    <li><strong>Persistent Cookies:</strong> Remain on your device for a set period or until deleted</li>
                    <li><strong>First-Party Cookies:</strong> Set directly by HopNGo</li>
                    <li><strong>Third-Party Cookies:</strong> Set by our partners and service providers</li>
                  </ul>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should provide detailed technical 
                  explanations and compliance with ePrivacy Directive requirements.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">2. Cookie Categories</h2>
                
                <div className="space-y-6">
                  <div className="border-l-4 border-green-500 pl-4">
                    <div className="flex items-center gap-2 mb-2">
                      <Shield className="w-5 h-5 text-green-600" />
                      <h3 className="font-semibold text-gray-800">Essential Cookies</h3>
                      <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
                        Always Active
                      </Badge>
                    </div>
                    <p className="text-gray-700 text-sm mb-2">
                      These cookies are necessary for the website to function and cannot be switched off. 
                      They are usually only set in response to actions made by you.
                    </p>
                    <div className="bg-green-50 p-3 rounded text-sm">
                      <strong>Examples:</strong> Authentication, security, load balancing, form submissions
                    </div>
                  </div>
                  
                  <div className="border-l-4 border-blue-500 pl-4">
                    <div className="flex items-center gap-2 mb-2">
                      <BarChart className="w-5 h-5 text-blue-600" />
                      <h3 className="font-semibold text-gray-800">Analytics Cookies</h3>
                      <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                        Optional
                      </Badge>
                    </div>
                    <p className="text-gray-700 text-sm mb-2">
                      These cookies help us understand how visitors interact with our website by 
                      collecting and reporting information anonymously.
                    </p>
                    <div className="bg-blue-50 p-3 rounded text-sm">
                      <strong>Examples:</strong> Google Analytics, page views, user journeys, performance metrics
                    </div>
                  </div>
                  
                  <div className="border-l-4 border-purple-500 pl-4">
                    <div className="flex items-center gap-2 mb-2">
                      <Target className="w-5 h-5 text-purple-600" />
                      <h3 className="font-semibold text-gray-800">Marketing Cookies</h3>
                      <Badge variant="outline" className="bg-purple-50 text-purple-700 border-purple-200">
                        Optional
                      </Badge>
                    </div>
                    <p className="text-gray-700 text-sm mb-2">
                      These cookies are used to deliver advertisements more relevant to you and your interests. 
                      They may be set by us or third-party providers.
                    </p>
                    <div className="bg-purple-50 p-3 rounded text-sm">
                      <strong>Examples:</strong> Facebook Pixel, Google Ads, retargeting, conversion tracking
                    </div>
                  </div>
                  
                  <div className="border-l-4 border-gray-500 pl-4">
                    <div className="flex items-center gap-2 mb-2">
                      <Settings className="w-5 h-5 text-gray-600" />
                      <h3 className="font-semibold text-gray-800">Functional Cookies</h3>
                      <Badge variant="outline" className="bg-gray-50 text-gray-700 border-gray-200">
                        Optional
                      </Badge>
                    </div>
                    <p className="text-gray-700 text-sm mb-2">
                      These cookies enable enhanced functionality and personalization, such as 
                      remembering your preferences and settings.
                    </p>
                    <div className="bg-gray-50 p-3 rounded text-sm">
                      <strong>Examples:</strong> Language preferences, currency settings, chat widgets
                    </div>
                  </div>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-4">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should provide detailed cookie 
                  categorization and consent requirements for each category.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">3. Third-Party Cookies and Services</h2>
                <p className="text-gray-700 leading-relaxed">
                  We work with trusted third-party services that may set their own cookies on your device.
                </p>
                
                <div className="mt-4 overflow-x-auto">
                  <table className="w-full border-collapse border border-gray-300 text-sm">
                    <thead>
                      <tr className="bg-gray-50">
                        <th className="border border-gray-300 p-2 text-left">Service</th>
                        <th className="border border-gray-300 p-2 text-left">Purpose</th>
                        <th className="border border-gray-300 p-2 text-left">Category</th>
                        <th className="border border-gray-300 p-2 text-left">Privacy Policy</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td className="border border-gray-300 p-2">Google Analytics</td>
                        <td className="border border-gray-300 p-2">Website analytics and performance</td>
                        <td className="border border-gray-300 p-2">Analytics</td>
                        <td className="border border-gray-300 p-2">
                          <Link href="https://policies.google.com/privacy" className="text-blue-600 hover:underline" target="_blank">
                            View Policy
                          </Link>
                        </td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2">Stripe</td>
                        <td className="border border-gray-300 p-2">Payment processing and fraud prevention</td>
                        <td className="border border-gray-300 p-2">Essential</td>
                        <td className="border border-gray-300 p-2">
                          <Link href="https://stripe.com/privacy" className="text-blue-600 hover:underline" target="_blank">
                            View Policy
                          </Link>
                        </td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2">Intercom</td>
                        <td className="border border-gray-300 p-2">Customer support chat</td>
                        <td className="border border-gray-300 p-2">Functional</td>
                        <td className="border border-gray-300 p-2">
                          <Link href="https://www.intercom.com/legal/privacy" className="text-blue-600 hover:underline" target="_blank">
                            View Policy
                          </Link>
                        </td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2">Facebook Pixel</td>
                        <td className="border border-gray-300 p-2">Advertising and conversion tracking</td>
                        <td className="border border-gray-300 p-2">Marketing</td>
                        <td className="border border-gray-300 p-2">
                          <Link href="https://www.facebook.com/privacy/policy" className="text-blue-600 hover:underline" target="_blank">
                            View Policy
                          </Link>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should maintain comprehensive list 
                  of all third-party services and their data processing agreements.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">4. Managing Your Cookie Preferences</h2>
                <p className="text-gray-700 leading-relaxed">
                  You have several options for managing cookies:
                </p>
                
                <div className="mt-4 space-y-4">
                  <div className="bg-blue-50 p-4 rounded-lg">
                    <h3 className="font-semibold text-blue-800 mb-2">HopNGo Cookie Settings</h3>
                    <p className="text-blue-700 text-sm mb-2">
                      Use our cookie preference center (above) to control which categories of cookies you allow.
                    </p>
                    <Button size="sm" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
                      Manage Preferences
                    </Button>
                  </div>
                  
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h3 className="font-semibold text-gray-800 mb-2">Browser Settings</h3>
                    <p className="text-gray-700 text-sm mb-2">
                      Most browsers allow you to control cookies through their settings:
                    </p>
                    <ul className="list-disc list-inside text-gray-700 text-sm space-y-1">
                      <li><Link href="https://support.google.com/chrome/answer/95647" className="text-blue-600 hover:underline" target="_blank">Chrome Cookie Settings</Link></li>
                      <li><Link href="https://support.mozilla.org/en-US/kb/enhanced-tracking-protection-firefox-desktop" className="text-blue-600 hover:underline" target="_blank">Firefox Cookie Settings</Link></li>
                      <li><Link href="https://support.apple.com/guide/safari/manage-cookies-and-website-data-sfri11471/mac" className="text-blue-600 hover:underline" target="_blank">Safari Cookie Settings</Link></li>
                      <li><Link href="https://support.microsoft.com/en-us/microsoft-edge/delete-cookies-in-microsoft-edge-63947406-40ac-c3b8-57b9-2a946a29ae09" className="text-blue-600 hover:underline" target="_blank">Edge Cookie Settings</Link></li>
                    </ul>
                  </div>
                  
                  <div className="bg-yellow-50 p-4 rounded-lg">
                    <h3 className="font-semibold text-yellow-800 mb-2">Opt-Out Tools</h3>
                    <p className="text-yellow-700 text-sm mb-2">
                      Industry opt-out tools for advertising cookies:
                    </p>
                    <ul className="list-disc list-inside text-yellow-700 text-sm space-y-1">
                      <li><Link href="https://optout.aboutads.info/" className="text-blue-600 hover:underline" target="_blank">Digital Advertising Alliance Opt-Out</Link></li>
                      <li><Link href="https://www.youronlinechoices.com/" className="text-blue-600 hover:underline" target="_blank">Your Online Choices (EU)</Link></li>
                      <li><Link href="https://tools.google.com/dlpage/gaoptout" className="text-blue-600 hover:underline" target="_blank">Google Analytics Opt-Out</Link></li>
                    </ul>
                  </div>
                </div>
                
                <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
                  <h3 className="font-semibold text-red-800 mb-2">⚠️ Important Note</h3>
                  <p className="text-red-700 text-sm">
                    Disabling certain cookies may affect the functionality of our website. 
                    Essential cookies cannot be disabled as they are necessary for basic site operation.
                  </p>
                </div>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">5. Cookie Retention and Expiration</h2>
                <p className="text-gray-700 leading-relaxed">
                  Different cookies have different lifespans:
                </p>
                
                <div className="mt-4 overflow-x-auto">
                  <table className="w-full border-collapse border border-gray-300 text-sm">
                    <thead>
                      <tr className="bg-gray-50">
                        <th className="border border-gray-300 p-2 text-left">Cookie Type</th>
                        <th className="border border-gray-300 p-2 text-left">Retention Period</th>
                        <th className="border border-gray-300 p-2 text-left">Purpose</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td className="border border-gray-300 p-2">Session Cookies</td>
                        <td className="border border-gray-300 p-2">Until browser closes</td>
                        <td className="border border-gray-300 p-2">Authentication, shopping cart</td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2">Preference Cookies</td>
                        <td className="border border-gray-300 p-2">1 year</td>
                        <td className="border border-gray-300 p-2">Language, currency, settings</td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2">Analytics Cookies</td>
                        <td className="border border-gray-300 p-2">26 months</td>
                        <td className="border border-gray-300 p-2">Usage statistics, performance</td>
                      </tr>
                      <tr>
                        <td className="border border-gray-300 p-2">Marketing Cookies</td>
                        <td className="border border-gray-300 p-2">90 days - 2 years</td>
                        <td className="border border-gray-300 p-2">Advertising, retargeting</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should verify retention periods 
                  align with data minimization principles and legal requirements.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">6. Updates to This Cookie Policy</h2>
                <p className="text-gray-700 leading-relaxed">
                  We may update this Cookie Policy from time to time to reflect changes in our 
                  practices or for other operational, legal, or regulatory reasons.
                </p>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should define update notification 
                  procedures and effective dates.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">7. Contact Us</h2>
                <p className="text-gray-700 leading-relaxed">
                  If you have questions about our use of cookies or this Cookie Policy, please contact us:
                </p>
                
                <div className="bg-gray-50 p-4 rounded-lg mt-3">
                  <p className="text-gray-700">
                    <strong>Email:</strong> privacy@hopngo.com<br />
                    <strong>Subject Line:</strong> Cookie Policy Inquiry<br />
                    <strong>Response Time:</strong> Within 5 business days
                  </p>
                </div>
              </section>
            </div>
          </CardContent>
        </Card>

        {/* Footer Actions */}
        <div className="mt-8 flex flex-col sm:flex-row gap-4 justify-between items-center">
          <div className="text-sm text-gray-600">
            Also see our{' '}
            <Link href="/legal/terms" className="text-orange-600 hover:text-orange-800 underline">
              Terms of Service
            </Link>
            {' '}and{' '}
            <Link href="/legal/privacy" className="text-orange-600 hover:text-orange-800 underline">
              Privacy Policy
            </Link>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
              Manage Cookies
            </Button>
            <Button asChild>
              <Link href="/">
                Return to HopNGo
              </Link>
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}