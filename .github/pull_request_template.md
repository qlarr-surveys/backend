
<!-- markdownlint-disable line-length -->
# Description

<!-- What does this PR fix or add? -->

## Type

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor
- [ ] Database migration
- [ ] Documentation update

## How to Test

1. Build the project:

   ```bash
   ./gradlew build
   ```

2. Start PostgreSQL (required):

   ```bash
   docker-compose up -d postgres-db
   ```

3. Run the application:

   ```bash
   ./gradlew bootRun
   ```

4. Test your changes:
   - [ ] Describe how to verify the fix works

## Testing

- [ ] Unit tests pass (`./gradlew test`)
- [ ] Integration tests pass
- [ ] E2E tests pass (if applicable)
- [ ] Manual API testing completed

## Checklist

- [ ] PR title follows [conventional commits](https://www.conventionalcommits.org/) format
- [ ] Code follows layered architecture (Controllers → Services → Repositories)
- [ ] Database changes include Liquibase migration
- [ ] No new security vulnerabilities
- [ ] Documentation updated (if API changes)

## Related Issue

Closes #

## Additional Notes

<!-- Any other context about the PR -->