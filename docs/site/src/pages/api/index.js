import React from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import styles from './index.module.css';

const services = [
  {
    name: 'Auth Service',
    description: 'Authentication and user management',
    path: '/api/auth-service',
    port: '8081',
    features: ['JWT Authentication', 'User Registration', 'Password Reset', 'Role Management']
  },
  {
    name: 'Booking Service', 
    description: 'Booking and vendor management',
    path: '/api/booking-service',
    port: '8083',
    features: ['Listing Search', 'Booking Creation', 'Payment Processing', 'Vendor Management']
  },
  {
    name: 'Social Service',
    description: 'Social media and posts',
    path: '/api/social-service', 
    port: '8082',
    features: ['Post Creation', 'User Feeds', 'Media Uploads', 'Social Interactions']
  },
  {
    name: 'Chat Service',
    description: 'Real-time messaging',
    path: '/api/chat-service',
    port: '8085', 
    features: ['WebSocket Messaging', 'Chat Rooms', 'Message History', 'Real-time Notifications']
  }
];

function ServiceCard({service}) {
  return (
    <div className={styles.serviceCard}>
      <div className={styles.serviceHeader}>
        <h3>{service.name}</h3>
        <span className={styles.servicePort}>:{service.port}</span>
      </div>
      <p className={styles.serviceDescription}>{service.description}</p>
      
      <div className={styles.serviceFeatures}>
        <h4>Key Features:</h4>
        <ul>
          {service.features.map((feature, index) => (
            <li key={index}>{feature}</li>
          ))}
        </ul>
      </div>
      
      <div className={styles.serviceLinks}>
        <Link
          className="button button--primary button--sm"
          to={service.path}>
          View API Docs
        </Link>
        <Link
          className="button button--secondary button--sm"
          href={`http://localhost:${service.port}/swagger-ui/index.html`}
          target="_blank">
          Swagger UI
        </Link>
      </div>
    </div>
  );
}

export default function ApiIndex() {
  const {siteConfig} = useDocusaurusContext();
  
  return (
    <Layout
      title="API Reference"
      description="Complete API documentation for all HopNGo microservices">
      <div className={styles.apiContainer}>
        <div className={styles.apiHeader}>
          <h1>HopNGo API Reference</h1>
          <p className={styles.apiSubtitle}>
            Complete documentation for all HopNGo microservices. Each service exposes a REST API
            with OpenAPI 3.0 specifications.
          </p>
        </div>
        
        <div className={styles.quickStart}>
          <h2>Quick Start</h2>
          <div className={styles.codeBlock}>
            <h3>Base URLs</h3>
            <ul>
              <li><strong>Gateway:</strong> <code>http://localhost:8080/api/v1</code></li>
              <li><strong>Direct Service Access:</strong> <code>http://localhost:&lt;port&gt;</code></li>
            </ul>
            
            <h3>Authentication</h3>
            <p>Most endpoints require JWT authentication. Include the token in the Authorization header:</p>
            <pre>
              <code>Authorization: Bearer &lt;your-jwt-token&gt;</code>
            </pre>
            
            <h3>Content Type</h3>
            <p>All requests should use JSON content type:</p>
            <pre>
              <code>Content-Type: application/json</code>
            </pre>
          </div>
        </div>
        
        <div className={styles.servicesGrid}>
          <h2>Services</h2>
          <div className={styles.servicesContainer}>
            {services.map((service, index) => (
              <ServiceCard key={index} service={service} />
            ))}
          </div>
        </div>
        
        <div className={styles.sdkSection}>
          <h2>SDKs & Tools</h2>
          <div className={styles.toolsGrid}>
            <div className={styles.toolCard}>
              <h3>TypeScript SDK</h3>
              <p>Auto-generated TypeScript client with full type safety</p>
              <Link className="button button--primary" to="/docs/development/sdk">
                Learn More
              </Link>
            </div>
            <div className={styles.toolCard}>
              <h3>Postman Collection</h3>
              <p>Ready-to-use Postman collections for all services</p>
              <Link className="button button--primary" href="/postman">
                Download
              </Link>
            </div>
            <div className={styles.toolCard}>
              <h3>OpenAPI Specs</h3>
              <p>Raw OpenAPI 3.0 specifications in JSON format</p>
              <Link className="button button--primary" href="/openapi">
                Browse Specs
              </Link>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}