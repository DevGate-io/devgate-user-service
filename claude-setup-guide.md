# Claude Code Setup Guide

Полный гайд по настройке Claude Code в новом проекте. Составлен на основе реальных конфигов
бэкенда (PHP/Symfony) и фронтенда (Next.js/TypeScript). Убери лишнее под свой стек.

---

## Структура `.claude/`

```
.claude/
├── settings.json              # Разрешения и хуки
├── settings.local.json        # Локальные переопределения (в .gitignore)
├── hooks/
│   └── permission-guard.py    # Python-хук: блокирует опасные команды
├── rules/                     # Правила, загружаемые автоматически в каждый контекст
│   ├── general.md             # Стиль общения и ответов
│   ├── workflow.md            # Цикл план → выполнение → отчёт
│   ├── orchestration.md       # GATHER → DELEGATE → VERIFY → REVIEW pipeline
│   ├── checks.md              # Проверки после изменений кода
│   ├── naming.md              # Язык, именование, комментарии
│   └── code-generation.md     # Правила генерации кода (под стек проекта)
├── agents/                    # Субагенты на haiku (только читают, не меняют файлы)
│   ├── code-style.md
│   ├── run-tests.md
│   └── static-analysis.md
├── docs/
│   └── conventions/           # Детальные примеры — не грузятся автоматически,
│       └── ...                # агенты читают их по запросу
├── commands/                  # Slash-команды /commit, /health-*, /create-* etc.
│   ├── commit.md
│   ├── health-bugs.md
│   ├── health-security.md
│   ├── health-cleanup.md
│   ├── health-deps.md
│   └── health-reuse.md
└── logs/                      # Отчёты о выполненных задачах (создаёт Claude)
```

---

## CLAUDE.md — корень проекта

Главный файл. Claude читает его первым в каждой сессии.

```markdown
# Project Name

## Working with me

- **Language:** All responses and explanations in Russian. Code, commits, comments — English only.
- **Plans first:** Before writing any code — use `EnterPlanMode` to propose a plan. Wait for approval.
- **After code changes:** Always run tests, static analysis, and linter in parallel as background agents.
- **Logs:** After every task create `.claude/logs/YYYY-MM-DD-task-name.md` with `## Summary` section.
- **Git push:** Always ask before pushing. Never force push.
- **Questions mid-task:** If I ask something — stop, answer, then continue.
- **"Продолжаем":** Find latest `.claude/logs/` file, read `## Summary`, resume without asking for context.

## Stack
[Описание стека проекта]

## Project structure
[Краткая карта директорий]

## Development commands
[Команды для запуска, тестов, линтера]

## Orchestration & Agents
See `.claude/rules/orchestration.md` for delegation pipeline.
See `.claude/rules/checks.md` for code check rules.
```

---

## settings.json

```json
{
  "permissions": {
    "allow": [
      "Bash(git status*)",
      "Bash(git diff*)",
      "Bash(git log*)",
      "Bash(git branch*)",
      "Bash(git add*)",
      "Bash(git commit*)",
      "Bash(git checkout*)",
      "Bash(git stash*)",
      "Bash(git fetch*)",
      "Bash(git pull*)",
      "Bash(git show*)",
      "Bash(git rev-parse*)",
      "Bash(ls*)",
      "Bash(mkdir*)",
      "Bash(grep*)",
      "Bash(find*)",
      "Bash(cat*)",
      "Bash(echo*)"
    ],
    "ask": [
      "Bash(git push*)"
    ],
    "defaultMode": "acceptEdits"
  },
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "",
        "hooks": [
          {
            "type": "command",
            "command": "python3 .claude/hooks/permission-guard.py"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "REPLACE_WITH_LINT_COMMAND",
            "timeout": 30,
            "statusMessage": "Checking code style..."
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash(git commit*)",
        "hooks": [
          {
            "type": "command",
            "command": "REPLACE_WITH_STATIC_ANALYSIS_COMMAND",
            "timeout": 120,
            "statusMessage": "Running static analysis before commit..."
          }
        ]
      }
    ]
  },
  "plansDirectory": "./docs/plans"
}
```

**`defaultMode: acceptEdits`** — Claude применяет Edit/Write без подтверждения.
Опасные операции блокирует `permission-guard.py`.

### Примеры LINT_COMMAND для hooks

**TypeScript (PostToolUse)**
```bash
FILE=$(echo $TOOL_INPUT | jq -r '.file_path // empty'); if [ -n "$FILE" ] && echo "$FILE" | grep -qE '\.(ts|tsx|js|jsx)$'; then npx eslint "$FILE" 2>&1 || echo 'ESLint: found issues'; fi
```

**TypeScript (PreToolUse — tsc перед коммитом)**
```bash
npx tsc --noEmit 2>&1 || echo 'TypeScript: found errors'
```

**PHP Symfony (PostToolUse)**
```bash
FILE=$(echo $TOOL_INPUT | jq -r '.file_path // empty'); if [ -n "$FILE" ] && echo "$FILE" | grep -q '.php$'; then REL="${FILE#$PWD/}"; docker-compose -f docker/dev/docker-compose.yml exec -T app ./vendor/bin/phpcs --report=summary "$REL" 2>&1 || echo 'PHPCS: found issues'; fi
```

**PHP Symfony (PreToolUse — phpstan перед коммитом)**
```bash
docker-compose -f docker/dev/docker-compose.yml exec -T app ./vendor/bin/phpstan analyse --memory-limit=-1 --no-progress 2>&1 || echo 'PHPStan: found errors'
```

---

## permission-guard.py

Создай `.claude/hooks/permission-guard.py`. Автоматически блокирует опасные команды.

```python
#!/usr/bin/env python3
"""Permission Guard Hook для Claude Code."""

import json
import os
import re
import sys

PROJECT_DIR = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())

DANGEROUS_GIT_PATTERNS = [
    r"git\s+push\s+.*--force",
    r"git\s+reset\s+--hard",
    r"git\s+rebase",
    r"git\s+commit\s+--amend",
    r"git\s+branch\s+-[dD]",
    r"git\s+clean\s+-fd",
    r"git\s+stash\s+drop",
    r"git\s+stash\s+clear",
]

DANGEROUS_SYSTEM_PATTERNS = [
    r"\brm\s+-rf?\s+/",
    r"\brm\s+-rf?\s+~",
    r"\bsudo\b",
    r"\bchmod\s+777",
    r"\bdd\s+.*of=/dev",
    r"\bkill\s+-9\s+-1",
    r"\breboot\b",
    r"\bshutdown\b",
]

SENSITIVE_PATTERNS = [
    r"cat\s+.*\.env",
    r"grep.*password",
    r"grep.*secret",
    r"grep.*token",
    r"printenv",
    r"docker.*config",
    r"docker.*inspect",
]


def is_path_in_project(path: str) -> bool:
    if not path:
        return True
    expanded = os.path.expanduser(os.path.expandvars(path))
    if not os.path.isabs(expanded):
        return True
    try:
        real_path = os.path.realpath(expanded)
        real_project = os.path.realpath(PROJECT_DIR)
        return real_path.startswith(real_project)
    except Exception:
        return False


def check_bash_command(command: str) -> tuple[bool, str]:
    for pattern in DANGEROUS_GIT_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            return False, "Опасная git команда заблокирована"
    for pattern in DANGEROUS_SYSTEM_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            return False, "Опасная системная команда заблокирована"
    for pattern in SENSITIVE_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            return False, "Доступ к чувствительным данным заблокирован"
    return True, ""


def main():
    try:
        input_data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    tool_name = input_data.get("tool_name", "")
    tool_input = input_data.get("tool_input", {})

    ALWAYS_SAFE_TOOLS = {
        "Read", "Glob", "Grep", "WebSearch", "WebFetch",
        "Agent", "TaskCreate", "TaskGet", "TaskList", "TaskOutput",
        "TaskUpdate", "TaskStop", "EnterPlanMode", "ExitPlanMode",
        "EnterWorktree", "ExitWorktree", "AskUserQuestion", "Skill", "ToolSearch",
        "CronCreate", "CronDelete", "CronList",
    }

    MCP_SAFE_PREFIXES = ("mcp__claude-in-chrome__", "mcp__ide__")

    is_safe = True
    reason = ""

    if tool_name == "Bash":
        command = tool_input.get("command", "")
        is_safe, reason = check_bash_command(command)
    elif tool_name in ALWAYS_SAFE_TOOLS:
        is_safe = True
    elif any(tool_name.startswith(p) for p in MCP_SAFE_PREFIXES):
        is_safe = True
    elif tool_name in ("Edit", "Write"):
        file_path = tool_input.get("file_path", "")
        if not is_path_in_project(file_path):
            is_safe, reason = False, f"Операция вне проекта: {file_path}"
    else:
        is_safe = True

    if is_safe:
        output = {"hookSpecificOutput": {"hookEventName": "PermissionRequest",
                                         "decision": {"behavior": "allow"}}}
    else:
        output = {"hookSpecificOutput": {"hookEventName": "PermissionRequest",
                                         "decision": {"behavior": "deny",
                                                       "message": reason,
                                                       "interrupt": False}}}

    print(json.dumps(output))
    sys.exit(0)


if __name__ == "__main__":
    main()
```

---

## rules/general.md

```markdown
# General Behavior Rules

- Provide concrete code, not "high-level" approach
- Brief answer first → explanations after if needed
- Suggest solutions the user might not have thought of
- Treat user as an expert
- Arguments matter more than authority
- Speculation is fine, but label it
- No moralizing
- When correcting code — show only changes with context, not the entire file
- Always respond in Russian
- Batch independent tool calls in a single message — never chain them sequentially
```

---

## rules/workflow.md

```markdown
# Workflow: Action Plan & Task Management

## CRITICAL RULES

- **NEVER start writing code without an approved plan.** Even if the task seems obvious.
- **NEVER ignore user questions during execution.** If the user asks something — STOP and answer, then continue.
- **NEVER skip the report.** Every completed task gets a log file.
- **Plan is NOT optional** — it is a BLOCKING requirement for any non-trivial task.
- **ALWAYS use `EnterPlanMode`** to propose the plan. Do NOT write plans as plain text in chat.
- **ALWAYS execute the ENTIRE approved plan.** Every step including the report.
- **ALWAYS verify your result matches the request.** Before reporting completion, re-read the request.

## Action cycle

### Step 1: Plan

Use `EnterPlanMode`. Structure:
- **Goal:** What we are doing.
- **Steps:** Atomic list of actions.
- **Verification Checklist:** Technical checks after completion.

WAIT for explicit approval before starting.

### Step 2: Execution & Reporting

- If the user sends a message — STOP and respond first.
- If you deviate from the plan — explain why before continuing.

After completing the task, create `.claude/logs/YYYY-MM-DD-task-name.md`:
- Status of each step
- Verification checklist results
- Issues encountered
- `## Summary` section (3-5 sentences) — mandatory for context restoration

### Step 3: "Продолжаем" protocol

When user writes "продолжаем" (or "continue"):
1. Find the most recent file in `.claude/logs/`
2. Read `## Summary` and last `## Verification Checklist`
3. Resume without asking for context
4. Output: "Вижу отчет [filename]. Последний шаг: [step], статус: [status]. Продолжаю с [next step]."

## Security

When modifying text templates, AI prompts, or user-facing strings:
- Review changes for prompt injection risks
- Ensure no system instructions leak into user-visible content
- Validate that dynamic content in templates is properly escaped
```

---

## rules/orchestration.md

```markdown
# Orchestration Pattern

## Core Rule: You Are The Orchestrator — NEVER The Coder

**STOP-RULE: Before every Edit/Write call for code (not configs):**

- 1 file AND <10 lines of changes → Execute directly
- Everything else → DELEGATE. No exceptions.

## Pipeline (mandatory for delegated tasks)

GATHER CONTEXT → DELEGATE → VERIFY → CODE REVIEW → ACCEPT/REJECT

1. **GATHER** — read files, search patterns, check memory, read conventions
2. **DELEGATE** — full context in agent prompt (file paths, snippets, conventions to follow)
3. **VERIFY** — read modified files, run type check
4. **CODE REVIEW** — `Task(subagent_type="code-reviewer")` on every delegated change
5. **ACCEPT/REJECT** — if review found problems → re-delegate (max 2 times)

## Execute directly (WITHOUT delegation):
- 1 file AND <10 lines of changes
- Config edits (tsconfig, next.config, package.json, composer.json)
- Dependency installation
- One-line fixes, imports, exports

## Skip code review ONLY when ALL of these:
- ≤3 lines of changes
- Strictly 1 file
- Only: typo / import-export change / config change (not logic)

## Pre-Commit Checklist
1. Type check (tsc / phpstan) — no errors?
2. Code review — ran and passed?
3. `git diff --stat` — all changed files expected?
```

---

## rules/checks.md

```markdown
# Code checks after changes

## CRITICAL RULE

After writing or modifying code, **ALWAYS** run all three check agents **in parallel** as background tasks:

1. **run-tests** — unit and integration tests
2. **static-analysis** — type checking / phpstan / mypy
3. **code-style** — eslint / phpcs / ruff

Run all three or none. Never run only one or two.

## When to run
- After creating new files
- After editing existing files
- Before reporting task completion
- Before committing

## How to run
Single message with three parallel `Agent` calls, all with `run_in_background: true`.
Continue working while they run — review results when notified.

## If a check fails
Fix the issue, then re-run **only the failed check** to confirm the fix.
```

---

## rules/naming.md

```markdown
# Naming conventions

## Language
- All generated code must be in English: names, variables, constants
- All text in code must be in English: comments, exception messages, log messages
- Git commits in English (or Russian — decide per project, be consistent)

## Code comments
- Code should be self-documenting
- Do not add obvious comments ("// Get user", "// Return result")
- Comments only for:
  - Complex business logic that cannot be simplified
  - Non-obvious decisions with "why" explanation
  - TODO/FIXME with task reference
```

---

## Агенты (`.claude/agents/`)

Агенты запускаются на модели `haiku`. Только читают — никогда не меняют файлы.

### `agents/run-tests.md`

```markdown
---
name: run-tests
description: Run tests and return results. Use PROACTIVELY after writing or modifying code. Returns structured report with analysis of failures.
tools: Read, Bash, Grep
model: haiku
---

You are a test runner. Run tests, analyze results, return structured report. NEVER modify files.

## Commands
# Replace with your project's test commands:
# npm test / npx vitest run / npx jest
# ./vendor/bin/phpunit
# pytest

## Report format

## Test Results
**Status:** PASS / FAIL
**Tests:** X total, Y passed, Z failed, W skipped
**Time:** Xs

### Failures (if any)
- **Test:** name
- **File:** path:line
- **Error:** what failed
- **Likely cause:** brief analysis

### Summary
1-2 sentences: what passed, what failed, next steps.

## Rules
- NEVER modify files
- If tests timeout (> 5 min), report partial results
- Always include the full command you ran
```

### `agents/static-analysis.md`

```markdown
---
name: static-analysis
description: Run static analysis and return results. Use PROACTIVELY after writing or modifying code. Returns structured report with issues grouped by severity.
tools: Read, Bash, Grep
model: haiku
---

You are a static analysis checker. NEVER modify files.

## Commands
# Replace with your project's commands:
# npx tsc --noEmit
# ./vendor/bin/phpstan analyse --memory-limit=-1
# mypy .

## Report format

## Static Analysis Report
**Status:** PASS / X errors

### Critical (must fix before commit)
- **File:** path:line — Error description

### Warnings (should fix)
- **File:** path:line — Warning description

### Summary
1-2 sentences: overall quality, what needs attention.

## Rules
- NEVER modify files
- Group issues by file when many errors in one file
```

### `agents/code-style.md`

```markdown
---
name: code-style
description: Run code style checks and return results. Use PROACTIVELY after writing or modifying code. Returns structured report with auto-fixable issues marked.
tools: Read, Bash, Grep
model: haiku
---

You are a code style checker. NEVER modify files.

## Commands
# Replace with your project's commands:
# npx eslint . / npx prettier --check .
# ./vendor/bin/phpcs
# ruff check .

## Report format

## Code Style Report
**Status:** PASS / X violations

### Errors (must fix)
- **File:** path:line — Violation description

### Style (auto-fixable)
- **File:** path:line — Style violation

### Summary
1-2 sentences: overall quality, whether auto-fix can resolve issues.

## Rules
- NEVER modify files
- Distinguish between errors and auto-fixable style issues
```

---

## Скиллы (`.claude/commands/`)

### `commands/commit.md`

```markdown
---
description: Create a git commit following project conventions.
model: sonnet
---

# Create commit

## Arguments (optional)
$ARGUMENTS
- No arguments: auto-detect type and description
- Text: use as commit description

## Steps
1. `git status`, `git diff --staged`, `git diff` — что изменилось
2. Extract task ID from branch (pattern: `[A-Z]+-\d+`, e.g. PROJ-123)
3. Determine commit type: feat / fix / docs / refactor / test / ci / build
4. Run pre-commit checks (lint + type check)
5. Build message: `<type>: <description>`
   - English (or Russian — по договорённости с командой)
   - Lowercase first letter, no period, 50-72 chars

## Examples
feat: add image generation endpoint
fix: correct file upload for files larger than 10MB
refactor: extract validation logic to separate service

## Rules
- If task ID not found in branch — ask user
- Do not `git push` unless user explicitly asks
- Do NOT add Co-Authored-By, Signed-off-by or automatic footers
```

### `commands/health-bugs.md`

```markdown
---
description: Run full bug audit of the codebase.
model: sonnet
---

Запусти полный аудит ошибок:

1. Task(subagent_type="bug-hunter") — сканирует весь src/, генерирует bug-hunting-report.md
2. Покажи пользователю отчёт: количество багов по приоритетам
3. Спроси: "Исправить сейчас?" → если да:
   Task(subagent_type="bug-fixer") — читает отчёт и фиксит по приоритету
```

### `commands/health-security.md`

```markdown
---
description: Run security audit of the codebase.
model: sonnet
---

Запусти аудит безопасности:

1. Task(subagent_type="security-scanner") — генерирует security-scan-report.md
2. Покажи критичные уязвимости
3. Спроси: "Исправить?" → Task(subagent_type="vulnerability-fixer")
```

### `commands/health-cleanup.md`

```markdown
---
description: Find and remove dead code.
model: sonnet
---

Найди и удали мёртвый код:

1. Task(subagent_type="dead-code-hunter") — генерирует dead-code-report.md
2. Покажи найденное
3. Спроси: "Удалить?" → Task(subagent_type="dead-code-remover")
```

### `commands/health-deps.md`

```markdown
---
description: Audit project dependencies for outdated or vulnerable packages.
model: sonnet
---

Аудит зависимостей:

1. Task(subagent_type="dependency-auditor") — генерирует dependency-report.md
2. Покажи устаревшие и уязвимые зависимости
3. Спроси: "Обновить?" → Task(subagent_type="dependency-updater")
```

### `commands/health-reuse.md`

```markdown
---
description: Find code duplication across the codebase.
model: sonnet
---

Найди дублирование кода:

1. Task(subagent_type="reuse-hunter") — генерирует reuse-hunting-report.md
2. Покажи дубликаты
3. Спроси: "Консолидировать?" → Task(subagent_type="reuse-fixer")
```

---

## Память между сессиями

Claude сохраняет контекст в `~/.claude/projects/<project-path>/memory/`.

**Типы памяти:**

| Тип | Когда сохранять |
|-----|----------------|
| `user` | Роль, опыт, предпочтения пользователя |
| `feedback` | Что делать / не делать (с `Why:` и `How to apply:`) |
| `project` | Решения, дедлайны, контекст задач (с `Why:`) |
| `reference` | Где искать информацию (Linear, Jira, Grafana, Slack) |

**MEMORY.md** — индекс всех файлов памяти, загружается в каждую сессию автоматически.

**Когда сохранять в MCP Memory (теги в описании):**
- Исправлен баг → `[bugfix]` с root cause
- Архитектурное решение → `[decision]` с обоснованием
- Найден подводный камень → `[gotcha]` с workaround
- Новый паттерн → `[pattern]` с путём к файлу

**Что НЕ сохранять:**
- Код, паттерны, пути файлов — читать из кода
- Git историю — `git log` авторитетен
- Контекст текущей задачи — он в плане/логах

---

## Superpowers — ключевые скилы

Superpowers — система скилов, загружаемых через `Skill` tool. Claude обязан проверять
применимость скила перед любым действием.

| Скил | Когда использовать |
|------|--------------------|
| `superpowers:writing-plans` | Есть спека/требования, нужен план |
| `superpowers:executing-plans` | Есть готовый план, начинаем реализацию |
| `superpowers:brainstorming` | Создание фичи, компонента, новой функциональности |
| `superpowers:systematic-debugging` | Любой баг, тест падает, неожиданное поведение |
| `superpowers:verification-before-completion` | Перед тем как сказать "готово" |
| `superpowers:finishing-a-development-branch` | Реализация завершена, все тесты прошли |
| `superpowers:requesting-code-review` | После реализации фичи |
| `superpowers:receiving-code-review` | Получен code review |
| `superpowers:dispatching-parallel-agents` | 2+ независимых задачи одновременно |
| `superpowers:subagent-driven-development` | Сложный план с независимыми задачами |

---

## MCP-интеграции

### Claude in Chrome (браузерная автоматизация)

Установи расширение Claude in Chrome. Используй для тестирования UI.
**Не используй Playwright** — он создаёт изолированный браузер без сессии пользователя.

```
mcp__claude-in-chrome__tabs_context_mcp  — список вкладок (вызывай первым)
mcp__claude-in-chrome__navigate          — перейти на страницу
mcp__claude-in-chrome__resize_window     — изменить размер окна (1440x900 для Figma)
mcp__claude-in-chrome__computer          — скриншот, клик
mcp__claude-in-chrome__read_page         — прочитать DOM
mcp__claude-in-chrome__javascript_tool   — выполнить JS
mcp__claude-in-chrome__read_console_messages — читать консоль
```

Перед использованием загрузи схему инструмента:
```
ToolSearch("select:mcp__claude-in-chrome__navigate")
```

---

## Чеклист настройки нового проекта

- [ ] `CLAUDE.md` в корне — описание + блок "Working with me"
- [ ] `.claude/settings.json` — разрешения, `defaultMode: acceptEdits`, hooks
- [ ] `.claude/hooks/permission-guard.py` — скопировать из этого гайда
- [ ] `.claude/rules/general.md` — стиль общения
- [ ] `.claude/rules/workflow.md` — план → выполнение → отчёт
- [ ] `.claude/rules/orchestration.md` — GATHER → DELEGATE → VERIFY → REVIEW
- [ ] `.claude/rules/checks.md` — параллельные агенты после изменений
- [ ] `.claude/rules/naming.md` — язык, комментарии
- [ ] `.claude/rules/code-generation.md` — правила генерации под стек
- [ ] `.claude/agents/run-tests.md` — с командами проекта (haiku)
- [ ] `.claude/agents/static-analysis.md` — с командами проекта (haiku)
- [ ] `.claude/agents/code-style.md` — с командами проекта (haiku)
- [ ] `.claude/commands/commit.md`
- [ ] `.claude/commands/health-*.md` — пять health-команд
- [ ] `.claude/logs/.gitkeep` — пустая директория для логов
- [ ] `.claude/settings.local.json` добавить в `.gitignore`
- [ ] Установить плагин Superpowers (глобальные скилы: планирование, дебаггинг, ревью)
- [ ] Установить расширение Claude in Chrome (для UI-тестирования)
