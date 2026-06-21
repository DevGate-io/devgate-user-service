# Code checks after changes

## CRITICAL RULE

After writing or modifying Kotlin code, **ALWAYS** run all three check agents **in parallel** as background tasks:

1. **run-tests** — `./gradlew test` (JUnit 5 + Mockito).
2. **static-analysis** — type-check (`./gradlew compileKotlin compileTestKotlin`). detekt is deferred until a stable release supports Kotlin 2.2.x.
3. **code-style** — `./gradlew ktlintCheck` (style per `.editorconfig`).

Run all three or none. Never run only one or two.

## When to run

- After creating new files.
- After editing existing files.
- Before reporting task completion.
- Before committing.

## How to run

Single message with three parallel `Agent` calls (`run-tests`, `static-analysis`, `code-style`),
all with `run_in_background: true`. Continue working while they run — review results when notified.

## If a check fails

- ktlint style violations → try `./gradlew ktlintFormat` first, then re-check.
- Fix the issue, then re-run **only the failed check** to confirm the fix.

## Note on hooks

There are NO blocking compile/commit hooks in this project (by design). The only enforced gate
is the `pre-push` git hook running a full `./gradlew build`. Checks are driven by these agents.
