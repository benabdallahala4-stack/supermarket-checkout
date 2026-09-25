# Project instructions

- Keep changes scoped to the current milestone. Add behavior tests before production behavior changes; commit tests with implementation.
- Organize handwritten backend code by catalog and checkout feature.
- Keep domain code independent of Spring, HTTP and generated API models.
- The backend owns pricing. Never calculate offers or totals in the frontend.
- Use BigDecimal for monetary arithmetic and exact two-decimal strings at the REST boundary.
- When API generation is introduced, `api/openapi.yaml` is its source of truth. Never edit generated sources manually.
- Use MapStruct for structural REST/domain conversion when those adapters are introduced, never for pricing logic.
- Run `./scripts/verify.sh backend` before completing a backend change. Introduce frontend and contract verification modes with their implementations.
- Update documentation alongside changes. Keep durable status concise in CONTEXT.md.
- Commit related tests and implementation together using sequential `[SC-XX] type: description` messages.
- Inspect effective Git author and committer identities before each commit. Never alter identity or add attribution/co-author trailers.
- Keep credentials, build output and private coordination material out of commits. Do not publish or rewrite history without explicit authorization.
