---
description: Create a git commit following project conventions.
model: sonnet
argument-hint: "[optional commit description]"
---

# Create commit

## Arguments (optional)
$ARGUMENTS
- No arguments: auto-detect type and description from the diff.
- Text: use it as the commit description.

## Steps
1. Run `git status`, `git diff --staged`, `git diff` — see what changed.
2. Determine the commit type: feat / fix / docs / refactor / test / ci / build / chore.
3. Run pre-commit checks: `./gradlew compileKotlin compileTestKotlin` (type-check). If anything is broken — stop and report.
4. Group changes into logically separate commits when they cover unrelated concerns.
5. Build the message: `<type>: <краткое описание на русском>`
   - Russian, lowercase first letter, no trailing period, ≤ 72 chars on the subject line.
   - Optional body with `-` bullets for non-trivial changes.

## Examples
feat: добавлен эндпоинт смены роли пользователя
fix: исправлена валидация email при регистрации
refactor: вынесена генерация cookie в отдельный сервис

## Rules
- Do not `git push` unless the user explicitly asks.
- Do NOT add Co-Authored-By / Signed-off-by / automatic footers (project preference).
- Never `git add -A` blindly — stage only the files belonging to each logical commit.
