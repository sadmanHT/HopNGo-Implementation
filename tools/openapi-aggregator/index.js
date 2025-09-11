#!/usr/bin/env node

import axios from 'axios';
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
    url: 'http://localhost:8081/v3/api-docs',
    description: 'Authentication and user management'
  },
  {
    name: 'social-service', 
    url: 'http://localhost:8082/v3/api-docs',
    description: 'Social media and posts'
  },
  {
    name: 'booking-service',
    url: 'http://localhost:8083/v3/api-docs', 
    description: 'Booking and vendor management'
  },
  {
    name: 'chat-service',
    url: 'http://localhost:8085/v3/api-docs',
    description: 'Real-time chat and messaging'
  }
];

// Output directory
const outputDir = path.resolve(__dirname, '../../docs/openapi');

async function fetchOpenApiSpec(service) {
  try {
    console.log(chalk.blue(`📡 Fetching OpenAPI spec for ${service.name}...`));
    
    const response = await axios.get(service.url, {
      timeout: 10000,
      headers: {
        'Accept': 'application/json'
      }
    });
    
    if (response.status === 200 && response.data) {
      console.log(chalk.green(`✅ Successfully fetched ${service.name} spec`));
      return response.data;
    } else {
      throw new Error(`Invalid response: ${response.status}`);
    }
  } catch (error) {
    console.log(chalk.red(`❌ Failed to fetch ${service.name}: ${error.message}`));
    
    // Return a placeholder spec if service is not available
    return {
      openapi: '3.0.1',
      info: {
        title: `${service.name} API`,
        description: `${service.description} (Service unavailable)`,
        version: '1.0.0'
      },
      paths: {},
      components: {},
      servers: [{
        url: service.url.replace('/v3/api-docs', ''),
        description: 'Service endpoint'
      }]
    };
  }
}

async function generateIndexPage() {
  const indexContent = `# HopNGo API Documentation

This directory contains OpenAPI specifications for all HopNGo microservices.

## Services

${services.map(service => 
  `### ${service.name}
- **Description**: ${service.description}
- **OpenAPI Spec**: [${service.name}.json](./${service.name}.json)
- **Swagger UI**: [View API Docs](${service.url.replace('/v3/api-docs', '/swagger-ui/index.html')})
- **Service URL**: ${service.url.replace('/v3/api-docs', '')}
`
).join('\n')}

## Usage

### Local Development
1. Start all services using Docker Compose
2. Run the aggregator: \`npm run start\` from \`tools/openapi-aggregator\`
3. View individual service docs at their respective Swagger UI endpoints

### Generated Files
- Each service has its own \`.json\` file with the complete OpenAPI specification
- Use these files for SDK generation, testing, or documentation

### SDK Generation
Generate TypeScript SDKs using:
\`\`\`bash
npm run sdk:generate
\`\`\`

Last updated: ${new Date().toISOString()}
`;

  const indexPath = path.join(outputDir, 'README.md');
  await fs.writeFile(indexPath, indexContent);
  console.log(chalk.green(`📝 Generated index page: ${indexPath}`));
}

async function main() {
  console.log(chalk.cyan('🚀 HopNGo OpenAPI Aggregator'));
  console.log(chalk.cyan('================================'));
  
  // Ensure output directory exists
  await fs.ensureDir(outputDir);
  console.log(chalk.blue(`📁 Output directory: ${outputDir}`));
  
  // Fetch specs for all services
  const results = [];
  
  for (const service of services) {
    const spec = await fetchOpenApiSpec(service);
    
    // Write individual service spec
    const outputPath = path.join(outputDir, `${service.name}.json`);
    await fs.writeJson(outputPath, spec, { spaces: 2 });
    
    results.push({
      service: service.name,
      success: spec.paths && Object.keys(spec.paths).length > 0,
      path: outputPath
    });
  }
  
  // Generate index page
  await generateIndexPage();
  
  // Summary
  console.log(chalk.cyan('\n📊 Summary:'));
  results.forEach(result => {
    const status = result.success ? chalk.green('✅') : chalk.yellow('⚠️');
    console.log(`${status} ${result.service}: ${result.path}`);
  });
  
  const successCount = results.filter(r => r.success).length;
  console.log(chalk.cyan(`\n🎉 Aggregated ${successCount}/${services.length} services successfully!`));
  
  if (successCount < services.length) {
    console.log(chalk.yellow('\n💡 Some services may not be running. Start them with: docker-compose up -d'));
  }
}

// Handle errors gracefully
process.on('unhandledRejection', (error) => {
  console.error(chalk.red('❌ Unhandled error:'), error);
  process.exit(1);
});

// Run the aggregator
main().catch(error => {
  console.error(chalk.red('❌ Failed to aggregate OpenAPI specs:'), error);
  process.exit(1);
});