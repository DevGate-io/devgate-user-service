# Workflow: Action Plan & Task Management

## CRITICAL RULES

- **NEVER start writing code without an approved plan** — even if the task seems obvious.
- **NEVER ignore user questions during execution.** If the user asks something — STOP, answer, then continue.
- **NEVER skip the report.** Every completed task gets a log file.
- **Plan is NOT optional** for any non-trivial task — it is a BLOCKING requirement.
- **ALWAYS use `EnterPlanMode`** to propose the plan. Do NOT write plans as plain text in chat.
- **ALWAYS execute the ENTIRE approved plan,** including the report step.
- **ALWAYS verify the result matches the request** before reporting completion — re-read the request.

## Action cycle

### Step 1: Plan

Use `EnterPlanMode`. Structure:
- **Goal:** what we are doing.
- **Steps:** atomic list of actions.
- **Verification Checklist:** technical checks to run after completion (test / detekt / ktlint / build).

WAIT for explicit approval before starting.

### Step 2: Execution & Reporting

- If the user sends a message — STOP and respond first.
- If you deviate from the plan — explain why before continuing.

After completing the task, create `.claude/logs/YYYY-MM-DD-task-name.md`:
- Status of each step.
- Verification checklist results (which Gradle checks ran, pass/fail).
- Issues encountered.
- `## Summary` section (3–5 sentences) — mandatory for context restoration.

### Step 3: "Продолжаем" protocol

When the user writes "продолжаем" (or "continue"):
1. Find the most recent file in `.claude/logs/`.
2. Read its `## Summary` and last `## Verification Checklist`.
3. Resume without asking for context.
4. Output: "Вижу отчёт [filename]. Последний шаг: [step], статус: [status]. Продолжаю с [next step]."

## Security

When modifying user-facing strings, exception messages, or any templated/dynamic content:
- Review for injection risks.
- Ensure no internal/system details leak into user-visible content.
- Validate that dynamic content is properly escaped.
