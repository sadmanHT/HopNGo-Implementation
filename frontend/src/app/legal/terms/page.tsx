import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Calendar, FileText, Scale } from 'lucide-react';

export const metadata: Metadata = {
  title: 'Terms of Service | HopNGo',
  description: 'Terms of Service and User Agreement for HopNGo travel platform',
  robots: 'index, follow',
};

export default function TermsOfServicePage() {
  const lastUpdated = '2024-01-15';

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-12">
      <div className="container mx-auto px-4 max-w-4xl">
        {/* Header */}
        <div className="mb-8">
          <Link href="/" className="inline-flex items-center text-blue-600 hover:text-blue-800 mb-4">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Home
          </Link>
          
          <div className="flex items-center gap-4 mb-4">
            <div className="p-3 bg-blue-100 rounded-lg">
              <Scale className="w-8 h-8 text-blue-600" />
            </div>
            <div>
              <h1 className="text-4xl font-bold text-gray-900">Terms of Service</h1>
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
                  This is placeholder content for legal review. The actual terms of service should be 
                  drafted and reviewed by qualified legal counsel before publication.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Terms Content */}
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">User Agreement</CardTitle>
          </CardHeader>
          <CardContent className="prose prose-gray max-w-none">
            <div className="space-y-8">
              <section>
                <h2 className="text-xl font-semibold mb-4">1. Acceptance of Terms</h2>
                <p className="text-gray-700 leading-relaxed">
                  By accessing and using HopNGo ("the Service"), you accept and agree to be bound by the terms 
                  and provision of this agreement. If you do not agree to abide by the above, please do not use this service.
                </p>
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should review and expand this section to include 
                  proper legal language regarding acceptance, modifications, and enforceability.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">2. Description of Service</h2>
                <p className="text-gray-700 leading-relaxed">
                  HopNGo is a travel platform that connects travelers with accommodation providers, 
                  transportation services, and local experiences.
                </p>
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Detailed service description, limitations, and disclaimers 
                  should be added here after legal review.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">3. User Accounts and Registration</h2>
                <p className="text-gray-700 leading-relaxed">
                  Users must provide accurate and complete information when creating an account.
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>Users are responsible for maintaining account security</li>
                  <li>One account per person or entity</li>
                  <li>Users must be 18 years or older, or have parental consent</li>
                </ul>
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should expand on account terms, 
                  age requirements, and verification procedures.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">4. Booking and Payment Terms</h2>
                <p className="text-gray-700 leading-relaxed">
                  <strong>[PLACEHOLDER]</strong> - This section should include:
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>Booking confirmation and modification policies</li>
                  <li>Payment processing and refund terms</li>
                  <li>Cancellation policies</li>
                  <li>Dispute resolution procedures</li>
                  <li>Third-party provider relationships</li>
                </ul>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">5. User Conduct and Prohibited Activities</h2>
                <p className="text-gray-700 leading-relaxed">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should define:
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>Acceptable use policies</li>
                  <li>Prohibited activities and content</li>
                  <li>Consequences for violations</li>
                  <li>Reporting mechanisms</li>
                </ul>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">6. Intellectual Property Rights</h2>
                <p className="text-gray-700 leading-relaxed">
                  <strong>[PLACEHOLDER]</strong> - This section should address:
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>HopNGo's intellectual property rights</li>
                  <li>User-generated content licensing</li>
                  <li>Third-party content and trademarks</li>
                  <li>DMCA compliance procedures</li>
                </ul>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">7. Privacy and Data Protection</h2>
                <p className="text-gray-700 leading-relaxed">
                  Your privacy is important to us. Please review our{' '}
                  <Link href="/legal/privacy" className="text-blue-600 hover:text-blue-800 underline">
                    Privacy Policy
                  </Link>
                  {' '}to understand how we collect, use, and protect your information.
                </p>
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Cross-reference with privacy policy and data protection regulations.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">8. Disclaimers and Limitation of Liability</h2>
                <p className="text-gray-700 leading-relaxed">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel must draft comprehensive disclaimers including:
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>Service availability disclaimers</li>
                  <li>Third-party provider disclaimers</li>
                  <li>Limitation of liability clauses</li>
                  <li>Force majeure provisions</li>
                </ul>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">9. Termination</h2>
                <p className="text-gray-700 leading-relaxed">
                  <strong>[PLACEHOLDER]</strong> - Define termination conditions:
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>User-initiated account termination</li>
                  <li>HopNGo-initiated termination for cause</li>
                  <li>Effect of termination on existing bookings</li>
                  <li>Data retention after termination</li>
                </ul>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">10. Governing Law and Dispute Resolution</h2>
                <p className="text-gray-700 leading-relaxed">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should specify:
                </p>
                <ul className="list-disc list-inside text-gray-700 mt-3 space-y-2">
                  <li>Governing jurisdiction</li>
                  <li>Dispute resolution procedures</li>
                  <li>Arbitration clauses (if applicable)</li>
                  <li>Class action waivers (if applicable)</li>
                </ul>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">11. Changes to Terms</h2>
                <p className="text-gray-700 leading-relaxed">
                  HopNGo reserves the right to modify these terms at any time. Users will be notified 
                  of significant changes via email or platform notification.
                </p>
                <p className="text-gray-700 leading-relaxed mt-3">
                  <strong>[PLACEHOLDER]</strong> - Legal counsel should define notification procedures 
                  and effective dates for changes.
                </p>
              </section>

              <section>
                <h2 className="text-xl font-semibold mb-4">12. Contact Information</h2>
                <p className="text-gray-700 leading-relaxed">
                  For questions about these Terms of Service, please contact us at:
                </p>
                <div className="bg-gray-50 p-4 rounded-lg mt-3">
                  <p className="text-gray-700">
                    <strong>Email:</strong> legal@hopngo.com<br />
                    <strong>Address:</strong> [PLACEHOLDER - Legal address]<br />
                    <strong>Phone:</strong> [PLACEHOLDER - Contact number]
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
            <Link href="/legal/privacy" className="text-blue-600 hover:text-blue-800 underline">
              Privacy Policy
            </Link>
            {' '}and{' '}
            <Link href="/legal/cookies" className="text-blue-600 hover:text-blue-800 underline">
              Cookie Policy
            </Link>
          </div>
          <Button asChild>
            <Link href="/">
              Return to HopNGo
            </Link>
          </Button>
        </div>
      </div>
    </div>
  );
}