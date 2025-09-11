# HopNGo Analytics Service

The Analytics Service provides comprehensive provider performance tracking, metrics collection, and reporting capabilities for the HopNGo platform.

## Overview

This service collects and analyzes provider performance data including:
- Booking conversion rates
- Response times and SLA compliance
- User engagement metrics (impressions, views, clicks)
- Revenue and booking trends
- Provider performance comparisons

## Architecture

### Database Views

The service uses materialized views for efficient data aggregation:

- **provider_daily_metrics**: Daily aggregated metrics per provider
- **provider_weekly_metrics**: Weekly aggregated metrics per provider
- **provider_monthly_metrics**: Monthly aggregated metrics per provider
- **provider_sla_compliance**: SLA compliance tracking
- **provider_conversion_funnel**: Conversion funnel analysis

### Event Processing

The service processes events from:
- **booking-service**: Booking creation, cart additions
- **social-service**: Post impressions and views
- **provider-service**: Provider updates and status changes

## API Endpoints

### Provider Analytics Summary

```http
GET /api/v1/provider/{providerId}/analytics/summary
```

**Query Parameters:**
- `startDate` (optional): Start date for metrics (ISO 8601)
- `endDate` (optional): End date for metrics (ISO 8601)
- `period` (optional): Aggregation period (daily, weekly, monthly)

**Response:**
```json
{
  "providerId": "provider-123",
  "period": {
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-01-31T23:59:59Z"
  },
  "metrics": {
    "totalBookings": 150,
    "totalRevenue": 45000.00,
    "conversionRate": 0.12,
    "averageResponseTime": 2.5,
    "slaCompliance": 0.95,
    "impressions": 12500,
    "views": 1250,
    "clickThroughRate": 0.10
  },
  "trends": {
    "bookingsGrowth": 0.15,
    "revenueGrowth": 0.22,
    "conversionTrend": 0.05
  }
}
```

### Provider Analytics Trends

```http
GET /api/v1/provider/{providerId}/analytics/trends
```

**Query Parameters:**
- `startDate` (required): Start date for trend analysis
- `endDate` (required): End date for trend analysis
- `granularity` (optional): Data granularity (hourly, daily, weekly)
- `metrics` (optional): Comma-separated list of metrics to include

**Response:**
```json
{
  "providerId": "provider-123",
  "granularity": "daily",
  "dataPoints": [
    {
      "date": "2024-01-01T00:00:00Z",
      "bookings": 5,
      "revenue": 1500.00,
      "impressions": 400,
      "views": 40,
      "responseTime": 2.1,
      "slaCompliance": 0.98
    }
  ]
}
```

### Record Analytics Event

```http
POST /api/v1/analytics/events
```

**Request Body:**
```json
{
  "eventType": "booking_created",
  "providerId": "provider-123",
  "userId": "user-456",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {
    "bookingId": "booking-789",
    "amount": 299.99,
    "source": "mobile_app"
  }
}
```

### Export Analytics Data

```http
GET /api/v1/provider/{providerId}/analytics/export
```

**Query Parameters:**
- `format` (required): Export format (csv, json, xlsx)
- `startDate` (required): Start date for export
- `endDate` (required): End date for export
- `metrics` (optional): Specific metrics to include

**Response:** File download with requested format

## Event Types

### Booking Events
- `listing_impression`: User views listing in search results
- `listing_detail_view`: User views detailed listing page
- `add_to_cart`: User adds listing to cart
- `booking_created`: Booking is successfully created
- `booking_cancelled`: Booking is cancelled

### Provider Events
- `provider_response`: Provider responds to booking request
- `provider_status_change`: Provider availability status changes
- `provider_profile_update`: Provider updates profile information

### Social Events
- `post_impression`: User sees post in feed
- `post_detail_view`: User views detailed post
- `post_interaction`: User likes, comments, or shares post

## SLA Metrics

### Response Time SLA
- **Target**: < 4 hours for booking responses
- **Measurement**: Time from booking request to provider response
- **Compliance**: Percentage of responses within SLA

### Availability SLA
- **Target**: > 95% uptime for provider listings
- **Measurement**: Time listings are active vs. total time
- **Compliance**: Percentage of time meeting availability target

## Configuration

### Environment Variables

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/analytics
SPRING_DATASOURCE_USERNAME=analytics_user
SPRING_DATASOURCE_PASSWORD=analytics_password

# Kafka Configuration
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPRING_KAFKA_CONSUMER_GROUP_ID=analytics-service

# Redis Configuration (for caching)
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Metrics Configuration
ANALYTICS_SLA_RESPONSE_TIME_HOURS=4
ANALYTICS_SLA_AVAILABILITY_THRESHOLD=0.95
ANALYTICS_CACHE_TTL_MINUTES=15
```

### Application Properties

```yaml
spring:
  application:
    name: analytics-service
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: ${SPRING_KAFKA_CONSUMER_GROUP_ID}
      auto-offset-reset: earliest
  redis:
    host: ${SPRING_REDIS_HOST}
    port: ${SPRING_REDIS_PORT}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

analytics:
  sla:
    response-time-hours: ${ANALYTICS_SLA_RESPONSE_TIME_HOURS:4}
    availability-threshold: ${ANALYTICS_SLA_AVAILABILITY_THRESHOLD:0.95}
  cache:
    ttl-minutes: ${ANALYTICS_CACHE_TTL_MINUTES:15}
```

## Usage Examples

### Getting Provider Summary

```bash
curl -X GET "http://localhost:8080/api/v1/provider/provider-123/analytics/summary?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Accept: application/json"
```

### Recording an Event

```bash
curl -X POST "http://localhost:8080/api/v1/analytics/events" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "booking_created",
    "providerId": "provider-123",
    "userId": "user-456",
    "timestamp": "2024-01-15T10:30:00Z",
    "metadata": {
      "bookingId": "booking-789",
      "amount": 299.99
    }
  }'
```

### Exporting Data as CSV

```bash
curl -X GET "http://localhost:8080/api/v1/provider/provider-123/analytics/export?format=csv&startDate=2024-01-01&endDate=2024-01-31" \
  -H "Accept: text/csv" \
  -o provider_analytics.csv
```

## Monitoring and Alerting

### Health Checks

The service provides health check endpoints:
- `/actuator/health`: Overall service health
- `/actuator/health/db`: Database connectivity
- `/actuator/health/kafka`: Kafka connectivity
- `/actuator/health/redis`: Redis connectivity

### Metrics

Prometheus metrics are available at `/actuator/prometheus`:
- `analytics_events_processed_total`: Total events processed
- `analytics_api_requests_total`: Total API requests
- `analytics_export_requests_total`: Total export requests
- `analytics_sla_compliance_ratio`: Current SLA compliance ratio

### Logging

Structured logging is configured with the following levels:
- `ERROR`: System errors and exceptions
- `WARN`: SLA violations and performance issues
- `INFO`: Event processing and API requests
- `DEBUG`: Detailed processing information

## Development

### Running Locally

```bash
# Start dependencies
docker-compose up -d postgres kafka redis

# Run the application
./mvnw spring-boot:run
```

### Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify -P integration-tests

# Generate test coverage report
./mvnw jacoco:report
```

### Database Migrations

Database schema is managed using Flyway migrations in `src/main/resources/db/migration/`.

## Troubleshooting

### Common Issues

1. **High Memory Usage**: Check view refresh frequency and consider partitioning large tables
2. **Slow Queries**: Review database indexes and query execution plans
3. **Event Processing Lag**: Monitor Kafka consumer lag and scale consumers if needed
4. **Cache Misses**: Verify Redis connectivity and cache TTL configuration

### Performance Tuning

- Adjust materialized view refresh schedules based on data volume
- Configure appropriate database connection pool sizes
- Tune Kafka consumer batch sizes and processing intervals
- Optimize Redis cache expiration policies

## Security

### Authentication

API endpoints require JWT authentication with appropriate scopes:
- `analytics:read`: Read access to analytics data
- `analytics:write`: Write access for event recording
- `analytics:export`: Export access for data downloads

### Data Privacy

Personal data is handled according to privacy regulations:
- User IDs are hashed in analytics storage
- Sensitive metadata is encrypted
- Data retention policies are enforced
- Export functionality includes data anonymization options