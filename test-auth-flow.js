// Test script to verify authentication flow
const axios = require('axios');

const API_BASE_URL = 'http://localhost:8081/api/v1';

async function testAuthFlow() {
  console.log('🧪 Testing Authentication Flow...\n');
  
  try {
    // Test 1: Register a new user
    console.log('1️⃣ Testing user registration...');
    const timestamp = Date.now();
    const registerData = {
      email: `test${timestamp}@example.com`,
      password: 'password123',
      firstName: 'Test',
      lastName: 'User'
    };
    
    const registerResponse = await axios.post(`${API_BASE_URL}/auth/register`, registerData);
    console.log('✅ Registration successful:', {
      user: registerResponse.data.user?.email,
      hasAccessToken: !!registerResponse.data.accessToken,
      hasRefreshToken: !!registerResponse.data.refreshToken
    });
    
    const { accessToken, refreshToken } = registerResponse.data;
    
    // Test 2: Get user profile
    console.log('\n2️⃣ Testing user profile retrieval...');
    const profileResponse = await axios.get(`${API_BASE_URL}/auth/profile`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });
    console.log('✅ Profile retrieval successful:', {
      user: profileResponse.data.email,
      name: profileResponse.data.name,
      role: profileResponse.data.role
    });
    
    // Test 3: Refresh token
    console.log('\n3️⃣ Testing token refresh...');
    const refreshResponse = await axios.post(`${API_BASE_URL}/auth/refresh`, {
      refreshToken
    });
    console.log('✅ Token refresh successful:', {
      hasNewAccessToken: !!refreshResponse.data.accessToken,
      hasNewRefreshToken: !!refreshResponse.data.refreshToken
    });
    
    // Test 4: Login with existing user
    console.log('\n4️⃣ Testing user login...');
    const loginData = {
      email: `test${timestamp}@example.com`,
      password: 'password123'
    };
    
    const loginResponse = await axios.post(`${API_BASE_URL}/auth/login`, loginData);
    console.log('✅ Login successful:', {
      user: loginResponse.data.user?.email,
      hasAccessToken: !!loginResponse.data.accessToken,
      hasRefreshToken: !!loginResponse.data.refreshToken
    });
    
    console.log('\n🎉 All authentication tests passed!');
    
  } catch (error) {
    console.error('❌ Authentication test failed:', {
      status: error.response?.status,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    });
  }
}

// Run the test
testAuthFlow();
