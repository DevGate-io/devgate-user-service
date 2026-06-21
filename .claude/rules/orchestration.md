# Orchestration Pattern

## Core Rule: You Are The Orchestrator — NEVER The Coder

**STOP-RULE — before every Edit/Write call for *code* (not configs):**

- 1 file AND < 10 lines changed → execute directly.
- Everything else → DELEGATE. No exceptions.

## Pipeline (mandatory for delegated tasks)

GATHER CONTEXT → DELEGATE → VERIFY → CODE REVIEW → ACCEPT/REJECT

1. **GATHER** — read files, search patterns, check memory, read conventions (`.claude/rules/code-generation.md`).
2. **DELEGATE** — pass full context in the agent prompt (file paths, snippets, conventions to follow).
3. **VERIFY** — read the modified files, run type-check (`./gradlew compileKotlin`).
4. **CODE REVIEW** — run a review pass (`/code-review` or a review agent) on every delegated change.
5. **ACCEPT/REJECT** — if the review found problems → re-delegate (max 2 iterations).

## Execute directly (WITHOUT delegation)

- 1 file AND < 10 lines changed.
- Config edits (`build.gradle.kts`, `application.yml`, `settings.gradle.kts`, compose/Makefile).
- Dependency changes.
- One-line fixes, imports.

## Skip code review ONLY when ALL of these hold

- ≤ 3 lines changed.
- Strictly 1 file.
- Only: typo / import change / config change (not logic).

## Pre-commit checklist

1. Type-check (`./gradlew compileKotlin compileTestKotlin`) — no errors?
2. ktlint (`./gradlew ktlintCheck`) — pass? (detekt deferred until it supports Kotlin 2.2.x)
3. Tests (`./gradlew test`) — green?
4. `git diff --stat` — all changed files expected?
