# Contributing to HopNGo

We welcome contributions to HopNGo! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/hopngo.git`
3. Install dependencies: `pnpm install`
4. Set up the development environment: `pnpm run setup`

## Development Workflow

1. Create a new branch: `git checkout -b feature/your-feature-name`
2. Make your changes
3. Run tests: `pnpm test`
4. Run linting: `pnpm lint`
5. Format code: `pnpm format`
6. Commit your changes using conventional commits
7. Push to your fork and create a pull request

## Commit Message Format

We use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:

```
type(scope): description

[optional body]

[optional footer]
```

### Types
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to the build process or auxiliary tools

### Examples
```
feat(auth): add OAuth2 authentication
fix(booking): resolve date validation issue
docs(readme): update installation instructions
```

## Code Style

- Use Prettier for code formatting
- Follow ESLint rules for JavaScript/TypeScript
- Use meaningful variable and function names
- Write clear comments for complex logic
- Follow existing patterns in the codebase

## Testing

- Write unit tests for new features
- Ensure all tests pass before submitting PR
- Aim for good test coverage
- Test both happy path and edge cases

## Pull Request Process

1. Update documentation if needed
2. Add tests for new functionality
3. Ensure CI/CD pipeline passes
4. Request review from maintainers
5. Address feedback and make necessary changes

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Questions?

If you have questions, please:
1. Check existing issues and discussions
2. Create a new issue with the "question" label
3. Join our community discussions

Thank you for contributing to HopNGo! 🚀