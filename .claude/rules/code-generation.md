# Code Generation Rules — Kotlin / Spring Boot

Follow the conventions already established in this codebase. Match surrounding code.

## Formatting

- **Indentation: tabs** (`indent_style = tab`, `tab_width = 2`), per `.editorconfig`. Never use spaces for indentation.
- Max line length: 120.
- `end_of_line = lf`, UTF-8, no trailing final newline (`insert_final_newline = false`).
- Run `./gradlew ktlintFormat` to normalize style.

## Architecture & layering

- Domain modules under `com.devgate` (e.g. `users`, `auth`), each split into:
  `controllers/`, `dto/` (with `requests/`, `responses/`), `models/` (+ `enums/`),
  `repositories/`, `services/` + `services/impl/`.
- **Services:** define an `interface` in `services/`, implementation in `services/impl/` (`...ServiceImpl`).
- **Dependency injection:** constructor injection only. Prefer omitting `@Autowired` on a single constructor.
- Controllers stay thin: validate input, delegate to a service, wrap in `ResponseEntity`. No business logic.

## DTO & validation

- Use `data class` for DTOs.
- Validate request bodies with Jakarta annotations (`@NotNull`, `@NotBlank`, `@Email`, `@Size`) and `@Valid` on the controller param.
- Map between DTO ↔ entity with extension functions (e.g. `fun UserDto.toUser()`), matching existing style.

## Persistence

- JPA entities are `@Entity` classes; entity classes are opened automatically (allOpen plugin) — do not add `open` manually.
- Schema changes go through a new Liquibase changeset in `src/main/resources/db/changelog/changes/`
  (next sequential prefix, e.g. `005-...yaml`) and must be referenced from the master changelog.
- `ddl-auto` is `validate` — the schema is owned by migrations, not Hibernate. Never rely on auto-DDL.

## Security & auth

- Access via `Authorization: Bearer <token>`; refresh token lives in the HttpOnly cookie `jwt-refresh-cookie`.
- Endpoint authorization: public auth endpoints are explicitly listed in `SecurityConfig`; everything else is authenticated.
- Method-level authorization with `@PreAuthorize("hasRole('ADMIN')")` (method security is enabled).
- Never log secrets, raw tokens, or passwords. Store only hashes (passwords, refresh tokens).

## Kotlin idioms

- Honor null-safety; the compiler runs with `-Xjsr305=strict`. Avoid `!!` — prefer `?:`, `requireNotNull`, or explicit handling.
- Prefer immutability (`val`) and expression bodies where they read cleanly.
- Use `ResponseStatusException` / the project's `ApiException` hierarchy for HTTP error mapping; let `GlobalErrorHandler` format responses.
