'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useErrorHandler } from '@/hooks/useErrorHandler';
import { SentryService } from '@/lib/sentry';
import { apiClient } from '@/lib/api-client-sentry';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import { FallbackUI } from '@/components/common/FallbackUI';
import { AlertTriangle, Bug, Zap, Clock, Database, CreditCard, Shield, Search, Map, Users } from 'lucide-react';

interface TestResult {
  type: string;
  success: boolean;
  message: string;
  timestamp: Date;
  errorId?: string;
}

const ErrorMonitoringTest: React.FC = () => {
  const [selectedErrorType, setSelectedErrorType] = useState<string>('');
  const [customMessage, setCustomMessage] = useState<string>('');
  const [testResults, setTestResults] = useState<TestResult[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const { handleError, handleApiError, handleFormError, handleAsyncError } = useErrorHandler();

  const errorTypes = [
    { value: 'ResourceNotFoundException', label: 'Resource Not Found', icon: AlertTriangle, color: 'orange' },
    { value: 'ServiceUnavailableException', label: 'Service Unavailable', icon: Database, color: 'red' },
    { value: 'PaymentException', label: 'Payment Error', icon: CreditCard, color: 'red' },
    { value: 'AuthenticationException', label: 'Authentication Error', icon: Shield, color: 'yellow' },
    { value: 'ValidationException', label: 'Validation Error', icon: AlertTriangle, color: 'orange' },
    { value: 'TimeoutException', label: 'Timeout Error', icon: Clock, color: 'red' },
    { value: 'MemoryException', label: 'Memory Error', icon: Zap, color: 'red' },
    { value: 'ConcurrencyException', label: 'Concurrency Error', icon: Users, color: 'yellow' },
    { value: 'RateLimitException', label: 'Rate Limit Error', icon: Shield, color: 'orange' },
  ];

  const featureErrors = [
    { value: 'payment', label: 'Payment Feature', icon: CreditCard },
    { value: 'booking', label: 'Booking Feature', icon: Users },
    { value: 'search', label: 'Search Feature', icon: Search },
    { value: 'auth', label: 'Authentication', icon: Shield },
    { value: 'map', label: 'Map Feature', icon: Map },
  ];

  const addTestResult = (result: TestResult) => {
    setTestResults(prev => [result, ...prev.slice(0, 9)]); // Keep last 10 results
  };

  const triggerBackendError = async () => {
    if (!selectedErrorType) return;
    
    setIsLoading(true);
    try {
      await apiClient.post('/api/test/error', {
        errorType: selectedErrorType
      });
      
      addTestResult({
        type: 'Backend Error',
        success: false,
        message: `Unexpected success for ${selectedErrorType}`,
        timestamp: new Date()
      });
    } catch (error: any) {
      await handleApiError(error, {
        component: 'error-monitoring-test',
        action: selectedErrorType
      });
      
      addTestResult({
        type: 'Backend Error',
        success: true,
        message: `Successfully triggered ${selectedErrorType}`,
        timestamp: new Date()
      });
    } finally {
      setIsLoading(false);
    }
  };

  const triggerFrontendError = async () => {
    setIsLoading(true);
    try {
      await handleError(
        new Error(customMessage || `Test frontend error: ${selectedErrorType}`),
        {
          component: 'frontend-error-test',
          action: selectedErrorType,
          additionalData: { userTriggered: true }
        }
      );
      
      addTestResult({
        type: 'Frontend Error',
        success: true,
        message: `Successfully captured frontend error`,
        timestamp: new Date()
      });
    } catch (error) {
      addTestResult({
        type: 'Frontend Error',
        success: false,
        message: `Failed to capture error: ${error}`,
        timestamp: new Date()
      });
    } finally {
      setIsLoading(false);
    }
  };

  const triggerFeatureError = async (feature: string) => {
    setIsLoading(true);
    try {
      await apiClient.post(`/api/test/error/feature/${feature}`, {
        errorType: 'BusinessException'
      });
      
      addTestResult({
        type: 'Feature Error',
        success: false,
        message: `Unexpected success for ${feature} feature`,
        timestamp: new Date()
      });
    } catch (error: any) {
      await handleApiError(error, {
        component: 'feature-error-test',
        action: feature,
        additionalData: { userTriggered: true }
      });
      
      addTestResult({
        type: 'Feature Error',
        success: true,
        message: `Successfully triggered ${feature} feature error`,
        timestamp: new Date()
      });
    } finally {
      setIsLoading(false);
    }
  };

  const triggerBulkErrors = async () => {
    setIsLoading(true);
    try {
      await apiClient.post('/api/test/error/bulk', null, {
        params: {
          count: 5,
          errorType: selectedErrorType || 'ServiceUnavailableException'
        }
      });
      
      addTestResult({
        type: 'Bulk Errors',
        success: false,
        message: 'Unexpected success for bulk errors',
        timestamp: new Date()
      });
    } catch (error: any) {
      await handleApiError(error, {
        component: 'bulk-error-test',
        action: selectedErrorType,
        additionalData: { bulkTest: true }
      });
      
      addTestResult({
        type: 'Bulk Errors',
        success: true,
        message: 'Successfully triggered bulk errors',
        timestamp: new Date()
      });
    } finally {
      setIsLoading(false);
    }
  };

  const triggerAsyncError = async () => {
    setIsLoading(true);
    
    const errorId = await handleAsyncError(
      async () => {
        // Simulate async operation that fails
        await new Promise(resolve => setTimeout(resolve, 1000));
        throw new Error(customMessage || 'Async operation failed');
      },
      {
        component: 'async-error-test',
        action: 'test-async-operation'
      }
    );
    
    addTestResult({
      type: 'Async Error',
      success: true,
      message: 'Successfully captured async error',
      timestamp: new Date()
    });
    
    setIsLoading(false);
  };

  const triggerFormError = async () => {
    await handleFormError(
      new Error(customMessage || 'Form validation failed'),
      'error-monitoring-test-form'
    );
    
    addTestResult({
      type: 'Form Error',
      success: true,
      message: 'Successfully captured form error',
      timestamp: new Date()
    });
  };

  const testSentryIntegration = async () => {
    // Test various Sentry features
    SentryService.setUser({
      id: 'test-user-123',
      email: 'test@example.com',
      username: 'testuser'
    });
    
    SentryService.setTags({ 'test-session': 'error-monitoring' });
    SentryService.setContext('test-info', {
      testType: 'integration-test',
      timestamp: new Date().toISOString()
    });
    
    SentryService.addBreadcrumb(
      'Starting Sentry integration test',
      'test',
      'info'
    );
    
    const errorId = await SentryService.captureMessage(
      'Sentry integration test message',
      'info',
      {
        extra: {
          testType: 'integration',
          userTriggered: true
        }
      }
    );
    
    addTestResult({
      type: 'Sentry Integration',
      success: true,
      message: 'Successfully tested Sentry integration',
      timestamp: new Date(),
      errorId
    });
  };

  const ComponentThatThrows: React.FC = () => {
    throw new Error(customMessage || 'Component error for testing Error Boundary');
  };

  const [showErrorComponent, setShowErrorComponent] = useState(false);

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="text-center">
        <h1 className="text-3xl font-bold mb-2">Error Monitoring Test Dashboard</h1>
        <p className="text-muted-foreground">
          Test Sentry integration, error handling, and monitoring systems
        </p>
        <Badge variant="outline" className="mt-2">
          Development Environment Only
        </Badge>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Error Configuration */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bug className="h-5 w-5" />
              Error Configuration
            </CardTitle>
            <CardDescription>
              Configure the type of error to trigger
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="error-type">Error Type</Label>
              <Select value={selectedErrorType} onValueChange={setSelectedErrorType}>
                <SelectTrigger>
                  <SelectValue placeholder="Select error type" />
                </SelectTrigger>
                <SelectContent>
                  {errorTypes.map((error) => {
                    const Icon = error.icon;
                    return (
                      <SelectItem key={error.value} value={error.value}>
                        <div className="flex items-center gap-2">
                          <Icon className="h-4 w-4" />
                          {error.label}
                        </div>
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>
            
            <div>
              <Label htmlFor="custom-message">Custom Error Message</Label>
              <Textarea
                id="custom-message"
                placeholder="Enter custom error message (optional)"
                value={customMessage}
                onChange={(e) => setCustomMessage(e.target.value)}
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        {/* Test Actions */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Zap className="h-5 w-5" />
              Test Actions
            </CardTitle>
            <CardDescription>
              Trigger different types of errors
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button 
              onClick={triggerBackendError} 
              disabled={!selectedErrorType || isLoading}
              className="w-full"
              variant="destructive"
            >
              Trigger Backend Error
            </Button>
            
            <Button 
              onClick={triggerFrontendError} 
              disabled={isLoading}
              className="w-full"
              variant="outline"
            >
              Trigger Frontend Error
            </Button>
            
            <Button 
              onClick={triggerAsyncError} 
              disabled={isLoading}
              className="w-full"
              variant="outline"
            >
              Trigger Async Error
            </Button>
            
            <Button 
              onClick={triggerFormError} 
              disabled={isLoading}
              className="w-full"
              variant="outline"
            >
              Trigger Form Error
            </Button>
            
            <Button 
              onClick={triggerBulkErrors} 
              disabled={!selectedErrorType || isLoading}
              className="w-full"
              variant="secondary"
            >
              Trigger Bulk Errors (5x)
            </Button>
            
            <Button 
              onClick={testSentryIntegration} 
              disabled={isLoading}
              className="w-full"
            >
              Test Sentry Integration
            </Button>
          </CardContent>
        </Card>

        {/* Feature-Specific Errors */}
        <Card>
          <CardHeader>
            <CardTitle>Feature-Specific Errors</CardTitle>
            <CardDescription>
              Test errors for specific application features
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-2">
              {featureErrors.map((feature) => {
                const Icon = feature.icon;
                return (
                  <Button
                    key={feature.value}
                    onClick={() => triggerFeatureError(feature.value)}
                    disabled={isLoading}
                    variant="outline"
                    size="sm"
                    className="flex items-center gap-2"
                  >
                    <Icon className="h-4 w-4" />
                    {feature.label}
                  </Button>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* Error Boundary Test */}
        <Card>
          <CardHeader>
            <CardTitle>Error Boundary Test</CardTitle>
            <CardDescription>
              Test React Error Boundary and Fallback UI
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button 
              onClick={() => setShowErrorComponent(!showErrorComponent)}
              variant={showErrorComponent ? "destructive" : "outline"}
              className="w-full"
            >
              {showErrorComponent ? 'Hide' : 'Show'} Error Component
            </Button>
            
            {showErrorComponent && (
              <ErrorBoundary>
                <ComponentThatThrows />
              </ErrorBoundary>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Test Results */}
      <Card>
        <CardHeader>
          <CardTitle>Test Results</CardTitle>
          <CardDescription>
            Recent error monitoring test results
          </CardDescription>
        </CardHeader>
        <CardContent>
          {testResults.length === 0 ? (
            <Alert>
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>
                No test results yet. Trigger some errors to see results here.
              </AlertDescription>
            </Alert>
          ) : (
            <div className="space-y-2">
              {testResults.map((result, index) => (
                <div 
                  key={index} 
                  className={`p-3 rounded-lg border ${
                    result.success 
                      ? 'bg-green-50 border-green-200 dark:bg-green-950 dark:border-green-800' 
                      : 'bg-red-50 border-red-200 dark:bg-red-950 dark:border-red-800'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Badge variant={result.success ? "default" : "destructive"}>
                        {result.type}
                      </Badge>
                      <span className="text-sm font-medium">{result.message}</span>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {result.timestamp.toLocaleTimeString()}
                    </span>
                  </div>
                  {result.errorId && (
                    <div className="mt-1 text-xs text-muted-foreground">
                      Error ID: {result.errorId}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

// Wrap the entire component in an Error Boundary for testing
const ErrorMonitoringTestPage: React.FC = () => {
  return (
    <ErrorBoundary>
      <ErrorMonitoringTest />
    </ErrorBoundary>
  );
};

export default ErrorMonitoringTestPage;