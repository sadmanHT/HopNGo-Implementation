# Security Policy

## Supported Versions

We release patches for security vulnerabilities. Which versions are eligible for receiving such patches depends on the CVSS v3.0 Rating:

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of HopNGo seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### Where to Report

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to: **security@hopngo.com**

If you prefer to encrypt your report, you can use our PGP key (contact us for the public key).

### What to Include

Please include the following information in your report:

- Type of issue (e.g. buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit the issue

### Response Timeline

We will acknowledge receipt of your vulnerability report within 48 hours and will send you regular updates about our progress. If you have not received a response to your email within 48 hours, please follow up to ensure we received your original message.

### Disclosure Policy

We follow the principle of coordinated disclosure:

1. **Report received**: We will acknowledge receipt and begin investigation
2. **Investigation**: We will investigate and develop a fix
3. **Fix development**: We will develop and test a security patch
4. **Release**: We will release the fix and publish a security advisory
5. **Public disclosure**: After users have had time to update, we will publicly disclose the vulnerability

### Bug Bounty

Currently, we do not offer a paid bug bounty program. However, we will acknowledge security researchers who responsibly disclose vulnerabilities to us.

### Security Best Practices

When contributing to HopNGo, please follow these security best practices:

- Never commit secrets, API keys, or passwords to the repository
- Use environment variables for sensitive configuration
- Validate and sanitize all user inputs
- Use parameterized queries to prevent SQL injection
- Implement proper authentication and authorization
- Keep dependencies up to date
- Follow the principle of least privilege
- Use HTTPS for all communications
- Implement proper error handling without exposing sensitive information

### Security Tools

We use various security tools to help identify vulnerabilities:

- Dependency scanning for known vulnerabilities
- Static code analysis
- Dynamic application security testing
- Container security scanning

### Contact

For any questions about this security policy, please contact us at security@hopngo.com.

---

**Note**: This security policy is subject to change. Please check back regularly for updates.