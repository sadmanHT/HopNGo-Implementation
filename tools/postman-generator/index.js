#!/usr/bin/env node

import { convert } from 'openapi-to-postman';
import fs from 'fs-extra';
import path from 'path';
import chalk from 'chalk';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Service configuration
const services = [
  {
    name: 'auth-service',
    specPath: '../../docs/openapi/auth-service.json',
    port: '8081',
    description: 'Authentication and user management'
  },
  {
    name: 'social-service',
    specPath: '../../docs/openapi/social-service.json',
    port: '8082', 
    description: 'Social media and posts'
  },
  {
    name: 'booking-service',
    specPath: '../../docs/openapi/booking-service.json',
    port: '8083',
    description: 'Booking and vendor management'
  },
  {
    name: 'chat-service',
    specPath: '../../docs/openapi/chat-service.json',
    port: '8085',
    description: 'Real-time messaging'
  }
];

// Output directory
const outputDir = path.resolve(__dirname, '../../docs/postman');

async function generateCollection(service) {
  return new Promise(async (resolve) => {
    try {
      console.log(chalk.blue(`📡 Generating Postman collection for ${service.name}...`));
      
      const specPath = path.resolve(__dirname, service.specPath);
      
      // Check if spec file exists
      if (!await fs.pathExists(specPath)) {
        throw new Error(`OpenAPI spec not found: ${specPath}`);
      }
      
      // Read the OpenAPI spec
      const spec = await fs.readJson(specPath);
      
      // Convert OpenAPI to Postman collection
      convert(
        { type: 'json', data: spec },
        {
          folderStrategy: 'Tags',
          requestParametersResolution: 'Example',
          exampleParametersResolution: 'Example',
          includeAuthInfoInExample: true,
          stackLimit: 50,
          optimizeConversion: true,
          requestNameSource: 'Fallback',
          indentCharacter: ' '
        },
        (error, conversionResult) => {
          if (error) {
            console.log(chalk.red(`❌ Failed to convert ${service.name}: ${error.message}`));
            resolve({ service: service.name, success: false, error: error.message });
            return;
          }
          
          if (!conversionResult.result) {
            console.log(chalk.red(`❌ No result for ${service.name}`));
            resolve({ service: service.name, success: false, error: 'No conversion result' });
            return;
          }
          
          // Enhance the collection with additional metadata
          const collection = conversionResult.output[0].data;
          
          // Update collection info
          collection.info.name = `HopNGo ${service.name.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}`;
          collection.info.description = `${service.description}\n\nGenerated from OpenAPI specification.\n\nBase URL: http://localhost:${service.port}`;
          collection.info.schema = 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json';
          
          // Add variables for the collection
          collection.variable = [
            {
              key: 'baseUrl',
              value: `http://localhost:${service.port}`,
              type: 'string'
            },
            {
              key: 'gatewayUrl', 
              value: 'http://localhost:8080/api/v1',
              type: 'string'
            },
            {
              key: 'authToken',
              value: '',
              type: 'string'
            }
          ];
          
          // Update all requests to use variables
          const updateRequests = (items) => {
            items.forEach(item => {
              if (item.request) {
                // Update URL to use variable
                if (typeof item.request.url === 'string') {
                  item.request.url = item.request.url.replace(
                    new RegExp(`http://localhost:${service.port}`, 'g'),
                    '{{baseUrl}}'
                  );
                } else if (item.request.url && item.request.url.raw) {
                  item.request.url.raw = item.request.url.raw.replace(
                    new RegExp(`http://localhost:${service.port}`, 'g'),
                    '{{baseUrl}}'
                  );
                }
                
                // Add auth header if not present
                if (!item.request.header) {
                  item.request.header = [];
                }
                
                const hasAuthHeader = item.request.header.some(h => 
                  h.key && h.key.toLowerCase() === 'authorization'
                );
                
                if (!hasAuthHeader) {
                  item.request.header.push({
                    key: 'Authorization',
                    value: 'Bearer {{authToken}}',
                    type: 'text',
                    disabled: true
                  });
                }
              }
              
              if (item.item) {
                updateRequests(item.item);
              }
            });
          };
          
          if (collection.item) {
            updateRequests(collection.item);
          }
          
          resolve({
            service: service.name,
            success: true,
            collection,
            warnings: conversionResult.output[0].reason || []
          });
        }
      );
    } catch (error) {
      console.log(chalk.red(`❌ Failed to generate collection for ${service.name}: ${error.message}`));
      resolve({ service: service.name, success: false, error: error.message });
    }
  });
}

async function generateEnvironment() {
  const environment = {
    id: 'hopngo-local-env',
    name: 'HopNGo Local Development',
    values: [
      {
        key: 'gatewayUrl',
        value: 'http://localhost:8080/api/v1',
        type: 'default',
        enabled: true
      },
      {
        key: 'authServiceUrl',
        value: 'http://localhost:8081',
        type: 'default',
        enabled: true
      },
      {
        key: 'socialServiceUrl',
        value: 'http://localhost:8082',
        type: 'default',
        enabled: true
      },
      {
        key: 'bookingServiceUrl',
        value: 'http://localhost:8083',
        type: 'default',
        enabled: true
      },
      {
        key: 'chatServiceUrl',
        value: 'http://localhost:8085',
        type: 'default',
        enabled: true
      },
      {
        key: 'authToken',
        value: '',
        type: 'secret',
        enabled: true
      },
      {
        key: 'userId',
        value: '',
        type: 'default',
        enabled: true
      }
    ],
    _postman_variable_scope: 'environment',
    _postman_exported_at: new Date().toISOString(),
    _postman_exported_using: 'HopNGo Postman Generator'
  };
  
  const envPath = path.join(outputDir, 'HopNGo-Local.postman_environment.json');
  await fs.writeJson(envPath, environment, { spaces: 2 });
  
  console.log(chalk.green(`🌍 Generated environment: ${envPath}`));
  return envPath;
}

async function generateReadme() {
  const readmeContent = `# HopNGo Postman Collections

This directory contains Postman collections and environments for all HopNGo microservices.

## Collections

${services.map(service => 
  `### ${service.name.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
- **File**: \`${service.name}.postman_collection.json\`
- **Description**: ${service.description}
- **Service URL**: http://localhost:${service.port}
- **Swagger UI**: http://localhost:${service.port}/swagger-ui/index.html
`
).join('\n')}

## Environment

- **File**: \`HopNGo-Local.postman_environment.json\`
- **Description**: Local development environment with all service URLs

## Setup Instructions

### 1. Import Collections
1. Open Postman
2. Click "Import" button
3. Select all \`.postman_collection.json\` files from this directory
4. Collections will appear in your Postman workspace

### 2. Import Environment
1. In Postman, click "Import" button
2. Select \`HopNGo-Local.postman_environment.json\`
3. Select the "HopNGo Local Development" environment from the dropdown

### 3. Authentication Setup
1. Start the auth service: \`docker-compose up -d auth-service\`
2. Use the "Auth Service" collection to login:
   - Send POST request to \`/auth/login\`
   - Copy the JWT token from the response
3. Set the \`authToken\` environment variable:
   - Click the environment dropdown
   - Click the eye icon next to "HopNGo Local Development"
   - Set the \`authToken\` value to your JWT token

### 4. Using the Collections

#### Direct Service Access
- Use \`{{authServiceUrl}}\`, \`{{socialServiceUrl}}\`, etc. for direct service calls
- Each service runs on its own port

#### Via API Gateway
- Use \`{{gatewayUrl}}\` for requests through the API gateway
- Gateway routes requests to appropriate services
- Gateway URL: \`http://localhost:8080/api/v1\`

## Collection Features

### Variables
Each collection includes these variables:
- \`baseUrl\`: Direct service URL
- \`gatewayUrl\`: API Gateway URL  
- \`authToken\`: JWT authentication token

### Authentication
- Authorization headers are pre-configured but disabled by default
- Enable the "Authorization" header in requests that require authentication
- Token uses the \`{{authToken}}\` variable

### Request Organization
- Requests are organized by OpenAPI tags
- Each endpoint includes example request/response data
- Path and query parameters are pre-filled with example values

## Troubleshooting

### Services Not Responding
1. Ensure all services are running: \`docker-compose up -d\`
2. Check service health: \`docker-compose ps\`
3. View service logs: \`docker-compose logs <service-name>\`

### Authentication Errors
1. Verify the auth service is running
2. Check that your JWT token is valid and not expired
3. Ensure the \`authToken\` environment variable is set correctly

### Collection Import Issues
1. Ensure you're using Postman v9.0 or later
2. Try importing one collection at a time
3. Check that the JSON files are valid

## Development Workflow

1. **Start Services**: \`docker-compose up -d\`
2. **Authenticate**: Use auth service to get JWT token
3. **Set Token**: Update \`authToken\` environment variable
4. **Test Endpoints**: Use collections to test API functionality
5. **Debug Issues**: Check service logs and responses

## Updating Collections

To regenerate collections after API changes:

\`\`\`bash
# Update OpenAPI specs
cd tools/openapi-aggregator
npm run start

# Regenerate Postman collections
cd ../postman-generator
npm run start
\`\`\`

Last updated: ${new Date().toISOString()}
`;

  const readmePath = path.join(outputDir, 'README.md');
  await fs.writeFile(readmePath, readmeContent);
  console.log(chalk.green(`📝 Generated README: ${readmePath}`));
}

async function main() {
  console.log(chalk.cyan('🚀 HopNGo Postman Generator'));
  console.log(chalk.cyan('============================'));
  
  // Ensure output directory exists
  await fs.ensureDir(outputDir);
  console.log(chalk.blue(`📁 Output directory: ${outputDir}`));
  
  const results = [];
  
  // Generate collections for all services
  for (const service of services) {
    const result = await generateCollection(service);
    
    if (result.success) {
      // Write collection file
      const collectionPath = path.join(outputDir, `${service.name}.postman_collection.json`);
      await fs.writeJson(collectionPath, result.collection, { spaces: 2 });
      console.log(chalk.green(`✅ Generated collection: ${collectionPath}`));
      
      if (result.warnings && result.warnings.length > 0) {
        console.log(chalk.yellow(`⚠️  Warnings for ${service.name}:`));
        result.warnings.forEach(warning => {
          console.log(chalk.yellow(`   - ${warning}`));
        });
      }
    }
    
    results.push(result);
  }
  
  // Generate environment file
  await generateEnvironment();
  
  // Generate README
  await generateReadme();
  
  // Summary
  console.log(chalk.cyan('\n📊 Summary:'));
  results.forEach(result => {
    const status = result.success ? chalk.green('✅') : chalk.red('❌');
    console.log(`${status} ${result.service}: ${result.success ? 'Generated' : result.error}`);
  });
  
  const successCount = results.filter(r => r.success).length;
  console.log(chalk.cyan(`\n🎉 Generated ${successCount}/${services.length} Postman collections!`));
  
  if (successCount > 0) {
    console.log(chalk.green('\n💡 Next steps:'));
    console.log(chalk.gray('1. Import collections into Postman'));
    console.log(chalk.gray('2. Import the environment file'));
    console.log(chalk.gray('3. Set your JWT token in the authToken variable'));
    console.log(chalk.gray('4. Start testing your APIs!'));
  }
}

// Handle errors gracefully
process.on('unhandledRejection', (error) => {
  console.error(chalk.red('❌ Unhandled error:'), error);
  process.exit(1);
});

// Run the generator
main().catch(error => {
  console.error(chalk.red('❌ Failed to generate Postman collections:'), error);
  process.exit(1);
});