import { Pact, Matchers } from '@pact-foundation/pact';
import path from 'path';
import { AuthService } from '../../services/auth';

const { like, eachLike } = Matchers;

describe('Auth Service Consumer Tests', () => {
  const provider = new Pact({
    consumer: 'frontend',
    provider: 'auth-service',
    port: 1234,
    log: path.resolve(process.cwd(), 'logs', 'pact.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'INFO',
  });

  beforeAll(() => provider.setup());
  afterEach(() => provider.verify());
  afterAll(() => provider.finalize());

  describe('POST /api/v1/auth/login', () => {
    it('should login successfully with valid credentials', async () => {
      // Arrange
      const loginRequest = {
        email: 'test@example.com',
        password: 'password123'
      };

      const expectedResponse = {
        token: like('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'),
        refreshToken: like('refresh_token_123'),
        user: {
          id: like('user-123'),
          email: like('test@example.com'),
          firstName: like('John'),
          lastName: like('Doe'),
          role: like('CUSTOMER')
        }
      };

      await provider.addInteraction({
        state: 'user exists with valid credentials',
        uponReceiving: 'a login request with valid credentials',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/login',
          headers: {
            'Content-Type': 'application/json'
          },
          body: loginRequest
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      // Act
      const authService = new AuthService('http://localhost:1234');
      const response = await authService.login(loginRequest.email, loginRequest.password);

      // Assert
      expect(response.token).toBeDefined();
      expect(response.user.email).toBe('test@example.com');
      expect(response.user.role).toBe('CUSTOMER');
    });

    it('should return 401 for invalid credentials', async () => {
      // Arrange
      const loginRequest = {
        email: 'test@example.com',
        password: 'wrongpassword'
      };

      await provider.addInteraction({
        state: 'user exists but password is incorrect',
        uponReceiving: 'a login request with invalid credentials',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/login',
          headers: {
            'Content-Type': 'application/json'
          },
          body: loginRequest
        },
        willRespondWith: {
          status: 401,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            error: like('Invalid credentials'),
            message: like('Email or password is incorrect')
          }
        }
      });

      // Act & Assert
      const authService = new AuthService('http://localhost:1234');
      await expect(authService.login(loginRequest.email, loginRequest.password))
        .rejects.toThrow('Invalid credentials');
    });
  });

  describe('POST /api/v1/auth/register', () => {
    it('should register a new user successfully', async () => {
      // Arrange
      const registerRequest = {
        email: 'newuser@example.com',
        password: 'password123',
        firstName: 'Jane',
        lastName: 'Smith',
        role: 'CUSTOMER'
      };

      const expectedResponse = {
        user: {
          id: like('user-456'),
          email: like('newuser@example.com'),
          firstName: like('Jane'),
          lastName: like('Smith'),
          role: like('CUSTOMER')
        },
        message: like('User registered successfully')
      };

      await provider.addInteraction({
        state: 'email is not already registered',
        uponReceiving: 'a registration request with valid data',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/register',
          headers: {
            'Content-Type': 'application/json'
          },
          body: registerRequest
        },
        willRespondWith: {
          status: 201,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      // Act
      const authService = new AuthService('http://localhost:1234');
      const response = await authService.register(registerRequest);

      // Assert
      expect(response.user.email).toBe('newuser@example.com');
      expect(response.user.firstName).toBe('Jane');
      expect(response.message).toBe('User registered successfully');
    });
  });
});