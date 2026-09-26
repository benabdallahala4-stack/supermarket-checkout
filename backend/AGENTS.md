# Backend conventions

- Target Java 21 and use the repository Gradle Wrapper.
- Use constructor injection for collaborators.
- Keep catalog and checkout responsibilities in separate feature packages; catalog must not depend on checkout.
- Domain code uses only JDK and domain types. Controllers translate HTTP and delegate use cases.
- Construct money from decimal strings, normalize exact cents with UNNECESSARY and compare numerically with compareTo. Never use float/double for money.
- Separate HTTP input validation from domain invariants; reject invalid configuration at startup.
- Prefer real objects in domain tests and JUnit 5/AssertJ. Mock only meaningful external boundaries.
- Run `./gradlew :backend:spotlessApply` to format Java and `./scripts/verify.sh backend` from the root to verify.
- Spotless checks handwritten source under src; generated code will remain outside that target.
- Use braces for conditional blocks and blank lines between validation, lookup, calculation and assignment steps. Keep related statements together.
