'use client';

import { useState, useEffect } from 'react';
import { useAuthStore } from '@/lib/state';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Progress } from '@/components/ui/progress';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  CheckCircle,
  Clock,
  XCircle,
  Upload,
  FileText,
  AlertCircle,
  Shield,
  User,
  Building,
  CreditCard
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface KycRequest {
  id: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'UNDER_REVIEW';
  submittedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
  documents: {
    identityDocument?: string;
    businessLicense?: string;
    addressProof?: string;
  };
}

interface DocumentUpload {
  file: File | null;
  preview: string | null;
  uploaded: boolean;
}

export default function ProviderVerificationPage() {
  const { user, token } = useAuthStore();
  const [kycRequest, setKycRequest] = useState<KycRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState('status');
  
  // Document upload states
  const [documents, setDocuments] = useState<{
    identityDocument: DocumentUpload;
    businessLicense: DocumentUpload;
    addressProof: DocumentUpload;
  }>({
    identityDocument: { file: null, preview: null, uploaded: false },
    businessLicense: { file: null, preview: null, uploaded: false },
    addressProof: { file: null, preview: null, uploaded: false }
  });
  
  const [businessInfo, setBusinessInfo] = useState({
    businessName: '',
    businessType: '',
    registrationNumber: '',
    address: '',
    description: ''
  });

  useEffect(() => {
    fetchKycStatus();
  }, []);

  const fetchKycStatus = async () => {
    try {
      const response = await fetch('/api/auth/kyc/status', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setKycRequest(data);
        if (data && data.status === 'PENDING') {
          setActiveTab('status');
        }
      }
    } catch (error) {
      console.error('Error fetching KYC status:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = (documentType: keyof typeof documents, file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      setDocuments(prev => ({
        ...prev,
        [documentType]: {
          file,
          preview: e.target?.result as string,
          uploaded: false
        }
      }));
    };
    reader.readAsDataURL(file);
  };

  const uploadDocument = async (documentType: keyof typeof documents) => {
    const document = documents[documentType];
    if (!document.file) return;

    const formData = new FormData();
    formData.append('file', document.file);
    formData.append('documentType', documentType);

    try {
      const response = await fetch('/api/auth/kyc/upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      if (response.ok) {
        setDocuments(prev => ({
          ...prev,
          [documentType]: {
            ...prev[documentType],
            uploaded: true
          }
        }));
      }
    } catch (error) {
      console.error('Error uploading document:', error);
    }
  };

  const submitKycRequest = async () => {
    setSubmitting(true);
    try {
      const response = await fetch('/api/auth/kyc', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          businessInfo,
          documents: {
            identityDocument: documents.identityDocument.uploaded,
            businessLicense: documents.businessLicense.uploaded,
            addressProof: documents.addressProof.uploaded
          }
        })
      });

      if (response.ok) {
        await fetchKycStatus();
        setActiveTab('status');
      }
    } catch (error) {
      console.error('Error submitting KYC request:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'REJECTED':
        return <XCircle className="h-5 w-5 text-red-500" />;
      case 'UNDER_REVIEW':
        return <Clock className="h-5 w-5 text-yellow-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      case 'UNDER_REVIEW':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getVerificationProgress = () => {
    if (!kycRequest) return 0;
    switch (kycRequest.status) {
      case 'PENDING':
      case 'UNDER_REVIEW':
        return 50;
      case 'APPROVED':
        return 100;
      case 'REJECTED':
        return 25;
      default:
        return 0;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="mb-8">
        <div className="flex items-center space-x-3 mb-4">
          <Shield className="h-8 w-8 text-blue-600" />
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Provider Verification</h1>
            <p className="text-gray-600">Complete your verification to start offering services</p>
          </div>
        </div>
        
        {kycRequest && (
          <div className="mb-6">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Verification Progress</span>
              <span className="text-sm text-gray-500">{getVerificationProgress()}%</span>
            </div>
            <Progress value={getVerificationProgress()} className="h-2" />
          </div>
        )}
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="status">Status</TabsTrigger>
          <TabsTrigger value="documents" disabled={kycRequest?.status === 'APPROVED'}>
            Documents
          </TabsTrigger>
          <TabsTrigger value="business" disabled={kycRequest?.status === 'APPROVED'}>
            Business Info
          </TabsTrigger>
        </TabsList>

        <TabsContent value="status" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <User className="h-5 w-5" />
                <span>Verification Status</span>
              </CardTitle>
              <CardDescription>
                Track your provider verification progress
              </CardDescription>
            </CardHeader>
            <CardContent>
              {kycRequest ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="flex items-center space-x-3">
                      {getStatusIcon(kycRequest.status)}
                      <div>
                        <p className="font-medium">Current Status</p>
                        <p className="text-sm text-gray-600">
                          Submitted on {new Date(kycRequest.submittedAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <Badge className={cn('px-3 py-1', getStatusColor(kycRequest.status))}>
                      {kycRequest.status.replace('_', ' ')}
                    </Badge>
                  </div>

                  {kycRequest.status === 'REJECTED' && kycRequest.rejectionReason && (
                    <Alert>
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>
                        <strong>Rejection Reason:</strong> {kycRequest.rejectionReason}
                      </AlertDescription>
                    </Alert>
                  )}

                  {kycRequest.status === 'APPROVED' && (
                    <Alert>
                      <CheckCircle className="h-4 w-4" />
                      <AlertDescription>
                        Congratulations! Your provider verification has been approved. You can now create listings and offer services.
                      </AlertDescription>
                    </Alert>
                  )}

                  {kycRequest.status === 'UNDER_REVIEW' && (
                    <Alert>
                      <Clock className="h-4 w-4" />
                      <AlertDescription>
                        Your verification is currently under review. We'll notify you once the review is complete.
                      </AlertDescription>
                    </Alert>
                  )}
                </div>
              ) : (
                <div className="text-center py-8">
                  <Shield className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">
                    Start Your Verification
                  </h3>
                  <p className="text-gray-600 mb-4">
                    Complete the verification process to become a trusted provider
                  </p>
                  <Button onClick={() => setActiveTab('documents')}>
                    Begin Verification
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <FileText className="h-5 w-5" />
                <span>Required Documents</span>
              </CardTitle>
              <CardDescription>
                Upload the required documents for verification
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {Object.entries({
                identityDocument: 'Identity Document (Passport/National ID)',
                businessLicense: 'Business License',
                addressProof: 'Address Proof'
              }).map(([key, label]) => {
                const doc = documents[key as keyof typeof documents];
                return (
                  <div key={key} className="border rounded-lg p-4">
                    <div className="flex items-center justify-between mb-3">
                      <Label className="text-sm font-medium">{label}</Label>
                      {doc.uploaded && (
                        <Badge className="bg-green-100 text-green-800">
                          <CheckCircle className="h-3 w-3 mr-1" />
                          Uploaded
                        </Badge>
                      )}
                    </div>
                    
                    <div className="space-y-3">
                      <Input
                        type="file"
                        accept="image/*,.pdf"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            handleFileUpload(key as keyof typeof documents, file);
                          }
                        }}
                      />
                      
                      {doc.preview && (
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">
                            {doc.file?.name}
                          </span>
                          <Button
                            size="sm"
                            onClick={() => uploadDocument(key as keyof typeof documents)}
                            disabled={doc.uploaded}
                          >
                            <Upload className="h-4 w-4 mr-1" />
                            Upload
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="business" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Building className="h-5 w-5" />
                <span>Business Information</span>
              </CardTitle>
              <CardDescription>
                Provide details about your business
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="businessName">Business Name</Label>
                  <Input
                    id="businessName"
                    value={businessInfo.businessName}
                    onChange={(e) => setBusinessInfo(prev => ({ ...prev, businessName: e.target.value }))}
                    placeholder="Enter your business name"
                  />
                </div>
                
                <div>
                  <Label htmlFor="businessType">Business Type</Label>
                  <Input
                    id="businessType"
                    value={businessInfo.businessType}
                    onChange={(e) => setBusinessInfo(prev => ({ ...prev, businessType: e.target.value }))}
                    placeholder="e.g., Restaurant, Hotel, Tour Guide"
                  />
                </div>
                
                <div>
                  <Label htmlFor="registrationNumber">Registration Number</Label>
                  <Input
                    id="registrationNumber"
                    value={businessInfo.registrationNumber}
                    onChange={(e) => setBusinessInfo(prev => ({ ...prev, registrationNumber: e.target.value }))}
                    placeholder="Business registration number"
                  />
                </div>
              </div>
              
              <div>
                <Label htmlFor="address">Business Address</Label>
                <Textarea
                  id="address"
                  value={businessInfo.address}
                  onChange={(e) => setBusinessInfo(prev => ({ ...prev, address: e.target.value }))}
                  placeholder="Enter your complete business address"
                  rows={3}
                />
              </div>
              
              <div>
                <Label htmlFor="description">Business Description</Label>
                <Textarea
                  id="description"
                  value={businessInfo.description}
                  onChange={(e) => setBusinessInfo(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="Describe your business and services"
                  rows={4}
                />
              </div>
              
              <div className="flex justify-end pt-4">
                <Button
                  onClick={submitKycRequest}
                  disabled={submitting || !Object.values(documents).every(doc => doc.uploaded)}
                  className="min-w-32"
                >
                  {submitting ? (
                    <div className="flex items-center space-x-2">
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      <span>Submitting...</span>
                    </div>
                  ) : (
                    'Submit for Review'
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}