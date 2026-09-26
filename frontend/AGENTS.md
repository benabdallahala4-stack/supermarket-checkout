# Frontend conventions

- Use Angular 22 standalone components, strict TypeScript/templates, inject(), signals and computed() for local state, and OnPush change detection.
- Keep checkout UI under one feature, with separate page, product-list, cart, receipt and state folders as those behaviors are introduced.
- Use the generated API services directly. Configure the relative /api base path once; do not duplicate HttpClient wrappers or pricing rules.
- Never edit src/app/generated/api. Regenerate through the root Gradle task and verify contract drift.
- Keep monetary values as backend-provided strings. Frontend state owns quantities, not price calculations.
- Use semantic HTML, visible keyboard focus, accessible names and appropriate status/error announcements. Use @if/@for with stable tracking.
- Keep handwritten code free of any and avoid type assertions that bypass real constraints. Separate logical steps with whitespace.
- Add behavior tests before behavior changes. Use Angular HTTP testing with real generated services for request/response integration.
- Use the Node version in .nvmrc and the pinned npm version in package.json. npm ci is the reproducible installation command.
- Run ./scripts/verify.sh frontend from the repository root. Handwritten ESLint/Prettier rules exclude generated files; TypeScript compilation still includes them.
- Maintain the 80% line coverage floor over handwritten checkout-feature code. Generated sources and test files remain outside coverage.
