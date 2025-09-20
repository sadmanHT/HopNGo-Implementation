import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    
    // Basic validation
    if (!body.email || !body.password || !body.firstName || !body.lastName) {
      return NextResponse.json(
        { 
          error: 'Missing required fields',
          message: 'Email, password, first name, and last name are required'
        },
        { status: 400 }
      );
    }

    // Mock successful registration
    const mockUser = {
      id: 'mock-user-123',
      email: body.email,
      firstName: body.firstName,
      lastName: body.lastName,
      phone: body.phone || null,
      dateOfBirth: body.dateOfBirth || null,
      createdAt: new Date().toISOString(),
      emailVerified: false
    };

    // Mock JWT token
    const mockToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtb2NrLXVzZXItMTIzIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNTY3MjMwNDAwLCJleHAiOjk5OTk5OTk5OTl9.mock-signature';

    return NextResponse.json({
      success: true,
      message: 'Registration successful! Please check your email to verify your account.',
      user: mockUser,
      token: mockToken
    }, { status: 201 });

  } catch (error) {
    console.error('Registration mock error:', error);
    return NextResponse.json(
      { 
        error: 'Internal server error',
        message: 'Something went wrong during registration'
      },
      { status: 500 }
    );
  }
}