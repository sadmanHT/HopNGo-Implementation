import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    
    // Basic validation
    if (!body.email || !body.password) {
      return NextResponse.json(
        { 
          error: 'Missing credentials',
          message: 'Email and password are required'
        },
        { status: 400 }
      );
    }

    // Mock user lookup and authentication
    const mockUser = {
      id: 'mock-user-123',
      email: body.email,
      firstName: 'Test',
      lastName: 'User',
      phone: '+1234567890',
      emailVerified: true,
      createdAt: '2024-01-01T00:00:00Z'
    };

    // Mock JWT token
    const mockToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtb2NrLXVzZXItMTIzIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNTY3MjMwNDAwLCJleHAiOjk5OTk5OTk5OTl9.mock-signature';

    return NextResponse.json({
      success: true,
      message: 'Login successful',
      user: mockUser,
      token: mockToken
    }, { status: 200 });

  } catch (error) {
    console.error('Login mock error:', error);
    return NextResponse.json(
      { 
        error: 'Internal server error',
        message: 'Something went wrong during login'
      },
      { status: 500 }
    );
  }
}