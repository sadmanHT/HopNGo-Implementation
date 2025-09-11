# JWT Key Management and Rotation

This document describes the JWT key management system for the HopNGo authentication service, including key generation, rotation, and deployment procedures.

## Overview

The HopNGo auth service uses RSA key pairs for JWT token signing and verification. The system supports multiple key loading methods with the following priority:

1. **Environment Variables** (Production) - Highest priority
2. **File-based Keys** (Legacy/Development) - Medium priority  
3. **Temporary Generated Keys** (Development only) - Lowest priority

## Key Loading Priority

### 1. Environment Variables (Recommended for Production)

The service first attempts to load keys from environment variables:

```bash
JWT_RSA_PRIVATE_KEY=<base64-encoded-private-key-without-headers>
JWT_RSA_PUBLIC_KEY=<base64-encoded-public-key-without-headers>
```

### 2. File-based Keys (Legacy Support)

If environment variables are not set, the service falls back to file-based keys:

```yaml
jwt:
  private-key-path: ${JWT_PRIVATE_KEY_PATH:classpath:keys/private_key.pem}
  public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:keys/public_key.pem}
```

### 3. Temporary Keys (Development Only)

If neither environment variables nor valid files are found, the service generates temporary keys:

```
⚠️  WARNING: Temporary keys are generated for development only!
```

## Key Rotation Scripts

Two key rotation scripts are provided for different environments:

### Linux/macOS Script

```bash
./scripts/rotate-jwt-keys.sh
```

### Windows PowerShell Script

```powershell
.\scripts\rotate-jwt-keys.ps1
```

### Script Features

- **Automatic Backup**: Existing keys are backed up with timestamps
- **Key Generation**: Creates new 2048-bit RSA key pairs
- **Key Validation**: Verifies generated keys are valid and match
- **Multiple Output Formats**: Generates files for different deployment scenarios
- **Deployment Instructions**: Provides step-by-step deployment guidance

### Generated Files

After running the rotation script, the following files are created:

```
keys/
├── private_key.pem              # RSA private key (PEM format)
├── public_key.pem               # RSA public key (PEM format)
├── jwt-keys.env                 # Environment variables (Docker/Linux)
├── jwt-keys.ps1                 # PowerShell environment loader
├── jwt-keys-secret.yaml         # Kubernetes secret manifest
└── backup/
    ├── private_key_YYYYMMDD_HHMMSS.pem
    └── public_key_YYYYMMDD_HHMMSS.pem
```

## Deployment Methods

### Docker Compose

1. Copy environment variables from `keys/jwt-keys.env`
2. Add to your `docker-compose.yml`:

```yaml
services:
  auth-service:
    environment:
      - JWT_RSA_PRIVATE_KEY=${JWT_RSA_PRIVATE_KEY}
      - JWT_RSA_PUBLIC_KEY=${JWT_RSA_PUBLIC_KEY}
```

### Kubernetes

1. Apply the generated secret:

```bash
kubectl apply -f keys/jwt-keys-secret.yaml
```

2. Reference in your deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  template:
    spec:
      containers:
      - name: auth-service
        envFrom:
        - secretRef:
            name: jwt-keys
```

### Local Development

#### PowerShell (Windows)

```powershell
# Load environment variables
. .\keys\jwt-keys.ps1

# Run the application
.\mvnw spring-boot:run
```

#### Bash (Linux/macOS)

```bash
# Load environment variables
source keys/jwt-keys.env

# Run the application
./mvnw spring-boot:run
```

#### IDE Configuration

Add environment variables to your IDE's run configuration:

- **IntelliJ IDEA**: Run Configuration → Environment Variables
- **VS Code**: `.vscode/launch.json` env section
- **Eclipse**: Run Configuration → Environment tab

## Security Best Practices

### Key Security

- ✅ **Never commit private keys to version control**
- ✅ **Use different keys for different environments**
- ✅ **Store keys in secure secret management systems**
- ✅ **Rotate keys regularly (every 90 days recommended)**
- ✅ **Monitor authentication failures after rotation**

### Environment-Specific Keys

| Environment | Key Storage Method | Rotation Frequency |
|-------------|-------------------|--------------------|
| Development | Local files or temp keys | As needed |
| Staging | Environment variables | Monthly |
| Production | Kubernetes secrets/Vault | Every 90 days |

### Access Control

- Limit access to private keys to essential personnel only
- Use role-based access control (RBAC) in Kubernetes
- Audit key access and rotation activities
- Implement key escrow for disaster recovery

## Rolling Deployment Strategy

When rotating keys in production, follow this zero-downtime strategy:

### Phase 1: Preparation

1. Generate new key pair using rotation script
2. Test key pair validity
3. Prepare deployment manifests/configurations

### Phase 2: Deployment

1. **Deploy new keys** to all instances (without restart)
2. **Rolling restart** of auth-service instances one by one
3. **Verify** JWT validation works across all services
4. **Monitor** for authentication errors

### Phase 3: Validation

1. Test token generation with new keys
2. Verify token validation across all services
3. Monitor application logs for errors
4. Keep old keys as backup until all tokens expire

### Phase 4: Cleanup

1. Remove old keys after token expiration period
2. Update documentation with new rotation timestamp
3. Schedule next rotation

## Troubleshooting

### Common Issues

#### "Private key environment variable is empty or invalid"

- Verify environment variables are set correctly
- Check for extra whitespace or newlines
- Ensure base64 encoding is correct (no headers/footers)

#### "Private and public keys do not match"

- Regenerate key pair using rotation script
- Verify both keys are from the same generation
- Check for file corruption or encoding issues

#### "Authentication failures after key rotation"

- Verify all service instances have new keys
- Check if old tokens are still being validated
- Ensure proper rolling deployment was followed

### Validation Commands

```bash
# Verify private key
openssl rsa -in private_key.pem -check -noout

# Verify public key
openssl rsa -in public_key.pem -pubin -text -noout

# Check key pair match
openssl rsa -in private_key.pem -modulus -noout
openssl rsa -in public_key.pem -pubin -modulus -noout
```

### Monitoring

Monitor these metrics after key rotation:

- Authentication success/failure rates
- JWT validation errors
- Service availability
- Token generation latency

## Emergency Procedures

### Key Compromise

If a private key is compromised:

1. **Immediately** generate new keys
2. **Deploy** new keys to all instances
3. **Invalidate** all existing tokens (force re-authentication)
4. **Investigate** the compromise source
5. **Update** security procedures

### Service Recovery

If authentication service fails after key rotation:

1. **Rollback** to previous keys from backup
2. **Restart** affected services
3. **Investigate** the failure cause
4. **Re-plan** the rotation with fixes

## Automation

### Scheduled Rotation

Consider automating key rotation using:

- **Kubernetes CronJobs** for scheduled rotation
- **CI/CD pipelines** for automated deployment
- **HashiCorp Vault** for dynamic key generation
- **AWS Secrets Manager** or **Azure Key Vault** for cloud environments

### Example Kubernetes CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: jwt-key-rotation
spec:
  schedule: "0 2 1 */3 *"  # Every 3 months at 2 AM on the 1st
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: key-rotator
            image: hopngo/key-rotator:latest
            command: ["/scripts/rotate-and-deploy.sh"]
          restartPolicy: OnFailure
```

## Compliance and Auditing

### Audit Trail

Maintain records of:

- Key generation timestamps
- Rotation schedules and actual dates
- Personnel who performed rotations
- Any security incidents or compromises

### Compliance Requirements

- **SOC 2**: Regular key rotation and access controls
- **PCI DSS**: Cryptographic key management
- **GDPR**: Data protection through proper encryption
- **HIPAA**: Safeguarding of authentication mechanisms

---

**Last Updated**: $(date)
**Next Scheduled Review**: $(date -d "+3 months")
**Document Version**: 1.0