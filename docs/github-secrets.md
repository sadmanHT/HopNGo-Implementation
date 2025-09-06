# GitHub Secrets Configuration

This document outlines the required GitHub secrets for the CI/CD workflows in the HopNGo project.

## Required Secrets

### Container Registry (GHCR)

#### `GHCR_TOKEN`
- **Description**: GitHub Container Registry personal access token
- **Required for**: Docker image building and pushing to GHCR
- **Permissions needed**: 
  - `write:packages` - Push container images
  - `read:packages` - Pull container images
  - `delete:packages` - Delete container images (optional)
- **Setup**:
  1. Go to GitHub Settings > Developer settings > Personal access tokens > Tokens (classic)
  2. Generate new token with required permissions
  3. Add as repository secret named `GHCR_TOKEN`

### Optional Secrets

#### `STRIPE_PUBLISHABLE_KEY_TEST`
- **Description**: Stripe test publishable key for payment integration testing
- **Required for**: Integration tests involving payment flows
- **Setup**: Add your Stripe test publishable key from the Stripe Dashboard

#### `STRIPE_SECRET_KEY_TEST`
- **Description**: Stripe test secret key for payment integration testing
- **Required for**: Integration tests involving payment flows
- **Setup**: Add your Stripe test secret key from the Stripe Dashboard

#### `OPENAI_API_KEY`
- **Description**: OpenAI API key for AI service functionality
- **Required for**: AI service integration tests
- **Setup**: Add your OpenAI API key from the OpenAI platform

## Setting Up Secrets

### Repository Secrets
1. Navigate to your GitHub repository
2. Go to Settings > Secrets and variables > Actions
3. Click "New repository secret"
4. Add the secret name and value
5. Click "Add secret"

### Organization Secrets (Optional)
For multiple repositories, you can set up organization-level secrets:
1. Go to your GitHub organization settings
2. Navigate to Secrets and variables > Actions
3. Add secrets at the organization level
4. Configure repository access as needed

## Security Best Practices

1. **Principle of Least Privilege**: Only grant the minimum permissions required
2. **Regular Rotation**: Rotate tokens and keys regularly
3. **Environment Separation**: Use different keys for test and production environments
4. **Monitoring**: Monitor secret usage and access logs
5. **Audit**: Regularly audit which secrets are in use and remove unused ones

## Troubleshooting

### Common Issues

#### GHCR Authentication Failed
- Verify the `GHCR_TOKEN` has correct permissions
- Check if the token has expired
- Ensure the token is properly formatted (no extra spaces)

#### Docker Push Permission Denied
- Verify the token has `write:packages` permission
- Check if the repository/package exists and you have access
- Ensure the image name follows GHCR naming conventions

#### Integration Test Failures
- Verify optional secrets are set if tests require them
- Check if external service APIs are accessible
- Review test logs for specific error messages

## Workflow Dependencies

### CI Workflow (`ci.yml`)
- No secrets required for basic build and test operations
- Optional: External service keys for integration tests

### Docker Workflow (`docker.yml`)
- **Required**: `GHCR_TOKEN` for pushing images to GitHub Container Registry

### Integration Test Workflow (`it.yml`)
- **Optional**: Service-specific API keys for external integrations
- **Optional**: `STRIPE_*` keys for payment testing
- **Optional**: `OPENAI_API_KEY` for AI service testing

## Environment Variables vs Secrets

### Use Secrets For:
- API keys and tokens
- Passwords and credentials
- Private keys and certificates
- Any sensitive configuration values

### Use Environment Variables For:
- Public configuration values
- Feature flags
- Non-sensitive service URLs
- Build configuration options

---

*Last updated: $(date)*
*For questions or issues, please contact the DevOps team or create an issue in the repository.*