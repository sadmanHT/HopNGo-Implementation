import React, { useEffect, useRef } from 'react';
import Layout from '@theme/Layout';
import BrowserOnly from '@docusaurus/BrowserOnly';

function RedocComponent() {
  const redocRef = useRef(null);

  useEffect(() => {
    // Dynamically import Redoc to avoid SSR issues
    import('redoc').then(({ init }) => {
      if (redocRef.current) {
        init(
          '../openapi/auth-service.json',
          {
            theme: {
              colors: {
                primary: {
                  main: '#667eea'
                }
              },
              typography: {
                fontSize: '14px',
                lineHeight: '1.5em',
                code: {
                  fontSize: '13px'
                }
              },
              sidebar: {
                width: '300px'
              }
            },
            scrollYOffset: 60,
            hideDownloadButton: false,
            disableSearch: false,
            expandResponses: '200,201',
            jsonSampleExpandLevel: 2,
            hideSchemaPattern: true,
            showExtensions: true,
            sortPropsAlphabetically: true,
            payloadSampleIdx: 0
          },
          redocRef.current
        );
      }
    }).catch(error => {
      console.error('Failed to load Redoc:', error);
      // Fallback content
      if (redocRef.current) {
        redocRef.current.innerHTML = `
          <div style="padding: 2rem; text-align: center;">
            <h2>API Documentation Unavailable</h2>
            <p>Unable to load the OpenAPI specification. Please ensure the service is running and try again.</p>
            <p>Alternative: <a href="http://localhost:8081/swagger-ui/index.html" target="_blank">View Swagger UI</a></p>
          </div>
        `;
      }
    });
  }, []);

  return <div ref={redocRef} />;
}

export default function AuthServiceApi() {
  return (
    <Layout
      title="Auth Service API"
      description="Authentication and user management API documentation">
      <div style={{ padding: '1rem 0' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '0 1rem' }}>
          <div style={{ marginBottom: '2rem', textAlign: 'center' }}>
            <h1>Auth Service API</h1>
            <p style={{ fontSize: '1.1rem', color: 'var(--ifm-color-emphasis-700)' }}>
              Authentication and user management service for HopNGo platform
            </p>
            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap', marginTop: '1rem' }}>
              <a 
                href="http://localhost:8081/swagger-ui/index.html" 
                target="_blank"
                className="button button--secondary button--sm"
              >
                Swagger UI
              </a>
              <a 
                href="../openapi/auth-service.json" 
                target="_blank"
                className="button button--secondary button--sm"
              >
                OpenAPI JSON
              </a>
              <a 
                href="http://localhost:8081" 
                target="_blank"
                className="button button--secondary button--sm"
              >
                Service Endpoint
              </a>
            </div>
          </div>
        </div>
        
        <BrowserOnly fallback={<div>Loading API documentation...</div>}>
          {() => <RedocComponent />}
        </BrowserOnly>
      </div>
    </Layout>
  );
}