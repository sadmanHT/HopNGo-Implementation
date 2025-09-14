import React, { useState } from 'react';
import LoadingSpinner from '../../ui/loading-spinner';

interface BankDetails {
  accountNumber: string;
  accountName: string;
  bankName: string;
  bankCode?: string;
  swiftCode?: string;
}

interface MobileMoneyDetails {
  phoneNumber: string;
  provider: string;
  accountName: string;
}

interface PayoutRequestData {
  amount: number;
  method: string;
  bankDetails?: BankDetails;
  mobileMoneyDetails?: MobileMoneyDetails;
}

interface PayoutRequestFormProps {
  availableBalance: number;
  currency: string;
  onSubmit: (data: PayoutRequestData) => Promise<void>;
  onCancel: () => void;
}

const PayoutRequestForm: React.FC<PayoutRequestFormProps> = ({
  availableBalance,
  currency,
  onSubmit,
  onCancel
}) => {
  const [loading, setLoading] = useState(false);
  const [method, setMethod] = useState<string>('BANK_TRANSFER');
  const [amount, setAmount] = useState<string>('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  // Bank transfer details
  const [bankDetails, setBankDetails] = useState<BankDetails>({
    accountNumber: '',
    accountName: '',
    bankName: '',
    bankCode: '',
    swiftCode: ''
  });
  
  // Mobile money details
  const [mobileMoneyDetails, setMobileMoneyDetails] = useState<MobileMoneyDetails>({
    phoneNumber: '',
    provider: '',
    accountName: ''
  });

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Validate amount
    const amountNum = parseFloat(amount);
    if (!amount || isNaN(amountNum)) {
      newErrors.amount = 'Please enter a valid amount';
    } else if (amountNum <= 0) {
      newErrors.amount = 'Amount must be greater than 0';
    } else if (amountNum > availableBalance) {
      newErrors.amount = 'Amount cannot exceed available balance';
    }

    // Validate payment method details
    if (method === 'BANK_TRANSFER') {
      if (!bankDetails.accountNumber.trim()) {
        newErrors.accountNumber = 'Account number is required';
      }
      if (!bankDetails.accountName.trim()) {
        newErrors.accountName = 'Account name is required';
      }
      if (!bankDetails.bankName.trim()) {
        newErrors.bankName = 'Bank name is required';
      }
    } else if (method === 'MOBILE_MONEY') {
      if (!mobileMoneyDetails.phoneNumber.trim()) {
        newErrors.phoneNumber = 'Phone number is required';
      }
      if (!mobileMoneyDetails.provider.trim()) {
        newErrors.provider = 'Mobile money provider is required';
      }
      if (!mobileMoneyDetails.accountName.trim()) {
        newErrors.accountName = 'Account name is required';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      const payoutData: PayoutRequestData = {
        amount: parseFloat(amount),
        method,
        ...(method === 'BANK_TRANSFER' && { bankDetails }),
        ...(method === 'MOBILE_MONEY' && { mobileMoneyDetails })
      };

      await onSubmit(payoutData);
    } catch (error) {
      console.error('Failed to submit payout request:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleBankDetailsChange = (field: keyof BankDetails, value: string) => {
    setBankDetails(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const handleMobileMoneyDetailsChange = (field: keyof MobileMoneyDetails, value: string) => {
    setMobileMoneyDetails(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const handleAmountChange = (value: string) => {
    setAmount(value);
    if (errors.amount) {
      setErrors(prev => ({ ...prev, amount: '' }));
    }
  };

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
        <div className="mt-3">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-medium text-gray-900">Request Payout</h3>
            <button
              onClick={onCancel}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Available Balance */}
          <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-green-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                      d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
              </svg>
              <div>
                <p className="text-sm font-medium text-green-900">Available Balance</p>
                <p className="text-lg font-bold text-green-900">
                  {formatCurrency(availableBalance, currency)}
                </p>
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Amount */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Payout Amount *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <span className="text-gray-500 sm:text-sm">{currency}</span>
                </div>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  max={availableBalance}
                  value={amount}
                  onChange={(e) => handleAmountChange(e.target.value)}
                  className={`block w-full pl-12 pr-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                    errors.amount ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="0.00"
                />
              </div>
              {errors.amount && (
                <p className="mt-1 text-sm text-red-600">{errors.amount}</p>
              )}
            </div>

            {/* Payment Method */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Payment Method *
              </label>
              <div className="grid grid-cols-2 gap-4">
                <label className="relative">
                  <input
                    type="radio"
                    name="method"
                    value="BANK_TRANSFER"
                    checked={method === 'BANK_TRANSFER'}
                    onChange={(e) => setMethod(e.target.value)}
                    className="sr-only"
                  />
                  <div className={`p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                    method === 'BANK_TRANSFER' 
                      ? 'border-blue-500 bg-blue-50' 
                      : 'border-gray-300 hover:border-gray-400'
                  }`}>
                    <div className="flex items-center">
                      <svg className="w-6 h-6 mr-3 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                              d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                      <div>
                        <p className="font-medium text-gray-900">Bank Transfer</p>
                        <p className="text-sm text-gray-500">Direct to bank account</p>
                      </div>
                    </div>
                  </div>
                </label>

                <label className="relative">
                  <input
                    type="radio"
                    name="method"
                    value="MOBILE_MONEY"
                    checked={method === 'MOBILE_MONEY'}
                    onChange={(e) => setMethod(e.target.value)}
                    className="sr-only"
                  />
                  <div className={`p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                    method === 'MOBILE_MONEY' 
                      ? 'border-blue-500 bg-blue-50' 
                      : 'border-gray-300 hover:border-gray-400'
                  }`}>
                    <div className="flex items-center">
                      <svg className="w-6 h-6 mr-3 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                              d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                      </svg>
                      <div>
                        <p className="font-medium text-gray-900">Mobile Money</p>
                        <p className="text-sm text-gray-500">MTN, Airtel, etc.</p>
                      </div>
                    </div>
                  </div>
                </label>
              </div>
            </div>

            {/* Bank Transfer Details */}
            {method === 'BANK_TRANSFER' && (
              <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                <h4 className="font-medium text-gray-900">Bank Account Details</h4>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Account Number *
                    </label>
                    <input
                      type="text"
                      value={bankDetails.accountNumber}
                      onChange={(e) => handleBankDetailsChange('accountNumber', e.target.value)}
                      className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                        errors.accountNumber ? 'border-red-300' : 'border-gray-300'
                      }`}
                      placeholder="Enter account number"
                    />
                    {errors.accountNumber && (
                      <p className="mt-1 text-sm text-red-600">{errors.accountNumber}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Account Name *
                    </label>
                    <input
                      type="text"
                      value={bankDetails.accountName}
                      onChange={(e) => handleBankDetailsChange('accountName', e.target.value)}
                      className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                        errors.accountName ? 'border-red-300' : 'border-gray-300'
                      }`}
                      placeholder="Enter account name"
                    />
                    {errors.accountName && (
                      <p className="mt-1 text-sm text-red-600">{errors.accountName}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Bank Name *
                    </label>
                    <input
                      type="text"
                      value={bankDetails.bankName}
                      onChange={(e) => handleBankDetailsChange('bankName', e.target.value)}
                      className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                        errors.bankName ? 'border-red-300' : 'border-gray-300'
                      }`}
                      placeholder="Enter bank name"
                    />
                    {errors.bankName && (
                      <p className="mt-1 text-sm text-red-600">{errors.bankName}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Bank Code (Optional)
                    </label>
                    <input
                      type="text"
                      value={bankDetails.bankCode}
                      onChange={(e) => handleBankDetailsChange('bankCode', e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Enter bank code"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Mobile Money Details */}
            {method === 'MOBILE_MONEY' && (
              <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                <h4 className="font-medium text-gray-900">Mobile Money Details</h4>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Phone Number *
                    </label>
                    <input
                      type="tel"
                      value={mobileMoneyDetails.phoneNumber}
                      onChange={(e) => handleMobileMoneyDetailsChange('phoneNumber', e.target.value)}
                      className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                        errors.phoneNumber ? 'border-red-300' : 'border-gray-300'
                      }`}
                      placeholder="Enter phone number"
                    />
                    {errors.phoneNumber && (
                      <p className="mt-1 text-sm text-red-600">{errors.phoneNumber}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Provider *
                    </label>
                    <select
                      value={mobileMoneyDetails.provider}
                      onChange={(e) => handleMobileMoneyDetailsChange('provider', e.target.value)}
                      className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                        errors.provider ? 'border-red-300' : 'border-gray-300'
                      }`}
                    >
                      <option value="">Select provider</option>
                      <option value="MTN">MTN Mobile Money</option>
                      <option value="AIRTEL">Airtel Money</option>
                      <option value="VODAFONE">Vodafone Cash</option>
                      <option value="TIGO">Tigo Cash</option>
                    </select>
                    {errors.provider && (
                      <p className="mt-1 text-sm text-red-600">{errors.provider}</p>
                    )}
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Account Name *
                    </label>
                    <input
                      type="text"
                      value={mobileMoneyDetails.accountName}
                      onChange={(e) => handleMobileMoneyDetailsChange('accountName', e.target.value)}
                      className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                        errors.accountName ? 'border-red-300' : 'border-gray-300'
                      }`}
                      placeholder="Enter account name"
                    />
                    {errors.accountName && (
                      <p className="mt-1 text-sm text-red-600">{errors.accountName}</p>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200">
              <button
                type="button"
                onClick={onCancel}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="inline-flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={loading}
              >
                {loading && <LoadingSpinner size="sm" className="mr-2" />}
                {loading ? 'Submitting...' : 'Submit Request'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default PayoutRequestForm;