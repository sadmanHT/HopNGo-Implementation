import { Pact, Matchers } from '@pact-foundation/pact';
import path from 'path';
import { BookingService } from '../../services/booking';

const { like, eachLike, term } = Matchers;

describe('Booking Service Consumer Tests', () => {
  const provider = new Pact({
    consumer: 'frontend',
    provider: 'booking-service',
    port: 1236,
    log: path.resolve(process.cwd(), 'logs', 'pact.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'INFO',
  });

  beforeAll(() => provider.setup());
  afterEach(() => provider.verify());
  afterAll(() => provider.finalize());

  describe('GET /api/v1/listings/search', () => {
    it('should search listings successfully with location and dates', async () => {
      // Arrange
      const searchParams = {
        location: 'Paris, France',
        checkIn: '2024-02-15',
        checkOut: '2024-02-18',
        guests: '2',
        category: 'ACCOMMODATION'
      };

      const expectedResponse = {
        content: eachLike({
          id: like('listing-123'),
          title: like('Cozy Apartment in Montmartre'),
          description: like('Beautiful apartment with Eiffel Tower view'),
          category: like('ACCOMMODATION'),
          basePrice: like(120.00),
          currency: like('USD'),
          maxGuests: like(4),
          address: like('18th Arrondissement, Paris, France'),
          latitude: like(48.8866),
          longitude: like(2.3431),
          amenities: eachLike('WiFi'),
          images: eachLike('https://example.com/apartment1.jpg'),
          vendor: {
            id: like('vendor-456'),
            businessName: like('Paris Stays'),
            rating: like(4.8)
          },
          availability: {
            available: like(true),
            pricePerNight: like(120.00),
            totalPrice: like(360.00)
          }
        }),
        pageable: {
          pageNumber: like(0),
          pageSize: like(20),
          totalElements: like(25),
          totalPages: like(2)
        }
      };

      await provider.addInteraction({
        state: 'listings exist for the search criteria',
        uponReceiving: 'a search request for listings',
        withRequest: {
          method: 'GET',
          path: '/api/v1/listings/search',
          query: searchParams
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
      const bookingService = new BookingService('http://localhost:1236');
      const response = await bookingService.searchListings(searchParams);

      // Assert
      expect(response.content).toBeDefined();
      expect(response.content.length).toBeGreaterThan(0);
      expect(response.content[0].title).toBe('Cozy Apartment in Montmartre');
      expect(response.content[0].category).toBe('ACCOMMODATION');
      expect(response.content[0].availability.available).toBe(true);
    });

    it('should return empty results when no listings match criteria', async () => {
      // Arrange
      const searchParams = {
        location: 'Remote Island',
        checkIn: '2024-12-25',
        checkOut: '2024-12-26',
        guests: '10',
        category: 'ACCOMMODATION'
      };

      const expectedResponse = {
        content: [],
        pageable: {
          pageNumber: like(0),
          pageSize: like(20),
          totalElements: like(0),
          totalPages: like(0)
        }
      };

      await provider.addInteraction({
        state: 'no listings exist for the search criteria',
        uponReceiving: 'a search request with no matching results',
        withRequest: {
          method: 'GET',
          path: '/api/v1/listings/search',
          query: searchParams
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
      const bookingService = new BookingService('http://localhost:1236');
      const response = await bookingService.searchListings(searchParams);

      // Assert
      expect(response.content).toEqual([]);
      expect(response.pageable.totalElements).toBe(0);
    });
  });

  describe('POST /api/v1/bookings', () => {
    it('should create a booking successfully', async () => {
      // Arrange
      const bookingRequest = {
        listingId: 'listing-123',
        startDate: '2024-02-15',
        endDate: '2024-02-18',
        numberOfGuests: 2,
        specialRequests: 'Late check-in please'
      };

      const expectedResponse = {
        id: like('booking-789'),
        listingId: like('listing-123'),
        userId: like('user-456'),
        startDate: term({
          matcher: '\\d{4}-\\d{2}-\\d{2}',
          generate: '2024-02-15'
        }),
        endDate: term({
          matcher: '\\d{4}-\\d{2}-\\d{2}',
          generate: '2024-02-18'
        }),
        numberOfGuests: like(2),
        totalPrice: like(360.00),
        status: like('PENDING'),
        specialRequests: like('Late check-in please'),
        createdAt: term({
          matcher: '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*',
          generate: '2024-01-15T14:30:00Z'
        })
      };

      await provider.addInteraction({
        state: 'listing is available for the requested dates',
        uponReceiving: 'a booking creation request',
        withRequest: {
          method: 'POST',
          path: '/api/v1/bookings',
          headers: {
            'Content-Type': 'application/json',
            'X-User-ID': like('user-456'),
            'X-User-Role': like('CUSTOMER')
          },
          body: bookingRequest
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
      const bookingService = new BookingService('http://localhost:1236');
      const response = await bookingService.createBooking(bookingRequest, 'user-456');

      // Assert
      expect(response.id).toBeDefined();
      expect(response.listingId).toBe('listing-123');
      expect(response.numberOfGuests).toBe(2);
      expect(response.status).toBe('PENDING');
      expect(response.specialRequests).toBe('Late check-in please');
    });

    it('should return 400 when listing is not available', async () => {
      // Arrange
      const bookingRequest = {
        listingId: 'listing-unavailable',
        startDate: '2024-02-15',
        endDate: '2024-02-18',
        numberOfGuests: 2,
        specialRequests: null
      };

      await provider.addInteraction({
        state: 'listing is not available for the requested dates',
        uponReceiving: 'a booking request for unavailable dates',
        withRequest: {
          method: 'POST',
          path: '/api/v1/bookings',
          headers: {
            'Content-Type': 'application/json',
            'X-User-ID': like('user-456'),
            'X-User-Role': like('CUSTOMER')
          },
          body: bookingRequest
        },
        willRespondWith: {
          status: 400,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            error: like('Booking not available'),
            message: like('The selected dates are not available for this listing')
          }
        }
      });

      // Act & Assert
      const bookingService = new BookingService('http://localhost:1236');
      await expect(bookingService.createBooking(bookingRequest, 'user-456'))
        .rejects.toThrow('Booking not available');
    });
  });
});