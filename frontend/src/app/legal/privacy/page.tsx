import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, Calendar, FileText, Shield, Eye, Database, Cookie } from 'lucide-react';

export const metadata: Metadata = {
  title: 'Privacy Policy | HopNGo',
  description: 'Privacy Policy and Data Protection information for HopNGo travel platform',
  robots: 'index, follow',
};

export default function PrivacyPolicyPage() {
  const lastUpdated = '2024-01-15';

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-emerald-100 py-12">
      <div className="container mx-auto px-4 max-w-4xl">
        {/* Header */}
        <div className="mb-8">
          <Link href="/" className="inline-flex items-center text-green-600 hover:text-green-800 mb-4">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Home
          </Link>
          
          <div className="flex items-center gap-4 mb-4">
            <div className="p-3 bg-green-100 rounded-lg">
              <Shield className="w-8 h-8 text-green-600" />
            </div>
            <div>
              <h1 className="text-4xl font-bold text-gray-900">Privacy Policy</h1>
              <div className="flex items-center text-gray-600 mt-2">
                <Calendar className="w-4 h-4 mr-2" />
                <span>Last updated: {lastUpdated}</span>
              </div>
            </div>
          </div>

          {/* Compliance Badges */}
          <div className="flex flex-wrap gap-2 mt-4">
            <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
              GDPR Compliant
            </Badge>
            <Badge variant="outline" className="bg-purple-50 text-purple-700 border-purple-200">
              CCPA Compliant
            </Badge>
            <Badge variant="outline" className="bg-orange-50 text-orange-700 border-orange-200">
              SOC 2 Type II
            </Badge>
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
                  This is placeholder content for legal review. The actual privacy policy should be 
                  drafted and reviewed by qualified legal counsel and privacy experts before publication.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Quick Summary */}
        <Card className="mb-8 border-green-200 bg-green-50">
          <CardHeader>
            <CardTitle className="text-xl text-green-800">Privacy at a Glance</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid md:grid-cols-3 gap-4">
              <div className="flex items-start gap-3">
                <Eye className="w-5 h-5 text-green-600 mt-1" />
                <div>
                  <h4 className="font-semibold text-green-800">Transparency</h4>
                  <p className="text-sm text-green-700">We clearly explain what data we collect and why</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Database className="w-5 h-5 text-green-600 mt-1" />
                <div>
                  <h4 className="font-semibold text-green-800">Control</h4>
                  <p className="text-sm text-green-700">You can access, export, or delete your data anytime</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Shield className="w-5 h-5 text-green-600 mt-1" />
                <div>
                  <h4 className="font-semibold text-green-800">Security</h4>
                  <p className="text-sm text-green-700">Your data is encrypted and securely stored</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Privacy Policy Content */}
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">Data Protection and Privacy</CardTitle>
          </CardHeader>
          <CardContent className="prose prose-gray max-w-none">
            <div className="space-y-8">
              <section>
                <h2 className="text-xl font-semibold mb-4">1. Information We Collect</h2>
                <p className="text-gray-700 leading-relaxed">
                  We collect information you provide directly to us, information we obtain automatically 
                  when you use our services, and information from third parties.
                </p>
                
                <div className="mt-4 space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-800 mb-2">Information You Provide</h3>
                    <ul className="list-disc list-inside text-gray-700 space-y-1">
                      <li>Account registration information (name, email, phone)</li>
                      <li>Profile information and preferences</li>
                      <li>Booking and payment information</li>
                      <li>Communications with customer support</li>
                      <li>Reviews and user-generated content</li>
                    </ul>
                  </div>
                  
                  <div>
                    <h3 className="font-semibold text-gray-800 mb-2">Information We Collect Automatically</h3>
                    <ul className="list-disc list-inside text-gray-700 space-y-1">
                      <li>Device and browser information</li>
                      <li>IP address and location data</li>
                      <li>Usage patterns and analytics</li>
                      <li>Cookies and similar technologies</li>
                    </ul>
                  </div>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should provide comprehensive data 
                  collection details and categorization for GDPR/CCPA compliance.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">2. How We Use Your Information</h2>
                <p className="text-gray-700 leading-relaxed">
                  We use the information we collect to provide, maintain, and improve our services.
                </p>
                
                <div className="mt-4">
                  <h3 className="font-semibold text-gray-800 mb-2">Primary Uses</h3>
                  <ul className="list-disc list-inside text-gray-700 space-y-1">
                    <li>Process bookings and transactions</li>
                    <li>Provide customer support</li>
                    <li>Send service-related communications</li>
                    <li>Improve and personalize user experience</li>
                    <li>Ensure platform security and prevent fraud</li>
                    <li>Comply with legal obligations</li>
                  </ul>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should define lawful bases for 
                  processing under GDPR (consent, contract, legitimate interest, etc.).
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">3. Information Sharing and Disclosure</h2>
                <p className="text-gray-700 leading-relaxed">
                  We do not sell your personal information. We may share your information in limited circumstances:
                </p>
                
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li><strong>Service Providers:</strong> Third-party vendors who help us operate our platform</li>
                  <li><strong>Business Partners:</strong> Hotels, airlines, and other travel providers for bookings</li>
                  <li><strong>Legal Requirements:</strong> When required by law or to protect our rights</li>
                  <li><strong>Business Transfers:</strong> In connection with mergers or acquisitions</li>
                  <li><strong>Consent:</strong> When you explicitly consent to sharing</li>
                </ul>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should detail data sharing agreements, 
                  international transfers, and adequacy decisions.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">4. Cookies and Tracking Technologies</h2>
                <p className="text-gray-700 leading-relaxed">
                  We use cookies and similar technologies to enhance your experience. You can control 
                  cookie preferences through our{' '}
                  <Link href="/legal/cookies" className="text-green-600 hover:text-green-800 underline">
                    Cookie Policy
                  </Link>.
                </p>
                
                <div className="mt-4 bg-gray-50 p-4 rounded-lg">
                  <div className="flex items-center gap-2 mb-2">
                    <Cookie className="w-4 h-4 text-gray-600" />
                    <h3 className="font-semibold text-gray-800">Cookie Categories</h3>
                  </div>
                  <ul className="list-disc list-inside text-gray-700 space-y-1">
                    <li><strong>Essential:</strong> Required for basic site functionality</li>
                    <li><strong>Analytics:</strong> Help us understand how you use our site</li>
                    <li><strong>Marketing:</strong> Used for personalized advertising (with consent)</li>
                    <li><strong>Functional:</strong> Remember your preferences and settings</li>
                  </ul>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should align with cookie banner implementation 
                  and consent management requirements.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">5. Data Security</h2>
                <p className="text-gray-700 leading-relaxed">
                  We implement appropriate technical and organizational measures to protect your personal information:
                </p>
                
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-1">
                  <li>Encryption in transit and at rest</li>
                  <li>Regular security assessments and audits</li>
                  <li>Access controls and authentication</li>
                  <li>Employee training and confidentiality agreements</li>
                  <li>Incident response and breach notification procedures</li>
                </ul>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should detail specific security measures 
                  and compliance certifications.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">6. Your Rights and Choices</h2>
                <p className="text-gray-700 leading-relaxed">
                  You have several rights regarding your personal information:
                </p>
                
                <div className="mt-4 grid md:grid-cols-2 gap-4">
                  <div className="bg-blue-50 p-4 rounded-lg">
                    <h3 className="font-semibold text-blue-800 mb-2">Access & Portability</h3>
                    <ul className="text-sm text-blue-700 space-y-1">
                      <li>• Request a copy of your data</li>
                      <li>• Download your information</li>
                      <li>• Transfer data to another service</li>
                    </ul>
                  </div>
                  
                  <div className="bg-red-50 p-4 rounded-lg">
                    <h3 className="font-semibold text-red-800 mb-2">Deletion & Correction</h3>
                    <ul className="text-sm text-red-700 space-y-1">
                      <li>• Delete your account and data</li>
                      <li>• Correct inaccurate information</li>
                      <li>• Restrict processing</li>
                    </ul>
                  </div>
                </div>
                
                <div className="mt-4 bg-green-50 p-4 rounded-lg">
                  <h3 className="font-semibold text-green-800 mb-2">Exercise Your Rights</h3>
                  <p className="text-sm text-green-700 mb-2">
                    You can exercise these rights through your account settings or by contacting us:
                  </p>
                  <div className="flex flex-col sm:flex-row gap-2">
                    <Button size="sm" asChild>
                      <Link href="/account/settings">
                        Account Settings
                      </Link>
                    </Button>
                    <Button size="sm" variant="outline" asChild>
                      <Link href="mailto:privacy@hopngo.com">
                        Contact Privacy Team
                      </Link>
                    </Button>
                  </div>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should detail specific procedures for 
                  rights requests and response timeframes.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">7. Data Retention</h2>
                <p className="text-gray-700 leading-relaxed">
                  We retain your personal information only as long as necessary for the purposes outlined 
                  in this policy or as required by law.
                </p>
                
                <div className="mt-4 bg-gray-50 p-4 rounded-lg">
                  <h3 className="font-semibold text-gray-800 mb-2">Retention Periods</h3>
                  <ul className="list-disc list-inside text-gray-700 space-y-1">
                    <li><strong>Account Data:</strong> Until account deletion + 30 days</li>
                    <li><strong>Booking Records:</strong> 7 years for tax/legal compliance</li>
                    <li><strong>Support Tickets:</strong> 3 years after resolution</li>
                    <li><strong>Marketing Data:</strong> Until consent withdrawal</li>
                    <li><strong>Analytics Data:</strong> 26 months (aggregated/anonymized)</li>
                  </ul>
                </div>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should align retention periods with 
                  business needs and legal requirements.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">8. International Data Transfers</h2>
                <p className="text-gray-700 leading-relaxed">
                  We may transfer your personal information to countries outside your residence. 
                  We ensure appropriate safeguards are in place for such transfers.
                </p>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should detail transfer mechanisms 
                  (adequacy decisions, SCCs, BCRs) and data localization requirements.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">9. Children's Privacy</h2>
                <p className="text-gray-700 leading-relaxed">
                  Our services are not intended for children under 16. We do not knowingly collect 
                  personal information from children under 16 without parental consent.
                </p>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should define age limits and 
                  parental consent procedures based on applicable laws.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">10. Changes to This Policy</h2>
                <p className="text-gray-700 leading-relaxed">
                  We may update this privacy policy from time to time. We will notify you of 
                  significant changes via email or platform notification.
                </p>
                
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should define notification procedures 
                  and effective dates for policy changes.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">11. Contact Information</h2>
                <p className="text-gray-700 leading-relaxed">
                  For questions about this Privacy Policy or our data practices, please contact us:
                </p>
                
                <div className="bg-gray-50 p-4 rounded-lg mt-3">
                  <div className="grid md:grid-cols-2 gap-4">
                    <div>
                      <h4 className="font-semibold text-gray-800 mb-2">Privacy Team</h4>
                      <p className="text-gray-700 text-sm">
                        <strong>Email:</strong> privacy@hopngo.com<br />
                        <strong>Response Time:</strong> Within 30 days<br />
                        <strong>Languages:</strong> English, Spanish, French
                      </p>
                    </div>
                    <div>
                      <h4 className="font-semibold text-gray-800 mb-2">Data Protection Officer</h4>
                      <p className="text-gray-700 text-sm">
                        <strong>Email:</strong> dpo@hopngo.com<br />
                        <strong>Address:</strong> [PLACEHOLDER - DPO address]<br />
                        <strong>Phone:</strong> [PLACEHOLDER - DPO phone]
                      </p>
                    </div>
                  </div>
                </div>
              </section>
            </div>
          </CardContent>
        </Card>

        {/* Footer Actions */}
        <div className="mt-8 flex flex-col sm:flex-row gap-4 justify-between items-center">
          <div className="text-sm text-gray-600">
            Also see our{' '}
            <Link href="/legal/terms" className="text-green-600 hover:text-green-800 underline">
              Terms of Service
            </Link>
            {' '}and{' '}
            <Link href="/legal/cookies" className="text-green-600 hover:text-green-800 underline">
              Cookie Policy
            </Link>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" asChild>
              <Link href="/account/settings">
                Manage Data
              </Link>
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