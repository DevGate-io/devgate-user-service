#!/usr/bin/env python3
"""Permission Guard Hook для Claude Code (событие PreToolUse).

Блокирует опасные команды и операции вне проекта. Во всех остальных случаях
завершается с кодом 0 БЕЗ вывода — это передаёт решение штатному механизму
разрешений (списки allow/ask в settings.json и defaultMode). Таким образом
guard работает как чёрный список, а не как авто-аппрув всего подряд.
"""

import json
import os
import re
import sys

PROJECT_DIR = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())

DANGEROUS_GIT_PATTERNS = [
    r"git\s+push\s+.*--force",
    r"git\s+push\s+.*-f\b",
    r"git\s+reset\s+--hard",
    r"git\s+rebase",
    r"git\s+commit\s+--amend",
    r"git\s+branch\s+-[dD]\b",
    r"git\s+clean\s+-[a-z]*f",
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
    r":\(\)\s*\{",
]

SENSITIVE_PATTERNS = [
    r"cat\s+.*\.env\b",
    r"grep.*password",
    r"grep.*secret",
    r"grep.*token",
    r"\bprintenv\b",
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


def deny(reason: str) -> None:
    output = {
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }
    print(json.dumps(output))
    sys.exit(0)


def check_bash_command(command: str) -> None:
    for pattern in DANGEROUS_GIT_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            deny("Опасная git-команда заблокирована permission-guard")
    for pattern in DANGEROUS_SYSTEM_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            deny("Опасная системная команда заблокирована permission-guard")
    for pattern in SENSITIVE_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            deny("Доступ к чувствительным данным заблокирован permission-guard")


def main() -> None:
    try:
        input_data = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        sys.exit(0)

    tool_name = input_data.get("tool_name", "")
    tool_input = input_data.get("tool_input", {}) or {}

    if tool_name == "Bash":
        check_bash_command(tool_input.get("command", ""))
    elif tool_name in ("Edit", "Write", "NotebookEdit"):
        file_path = tool_input.get("file_path", "") or tool_input.get("notebook_path", "")
        if not is_path_in_project(file_path):
            deny(f"Операция с файлом вне проекта заблокирована: {file_path}")

    # Решения нет — передаём управление штатному механизму разрешений.
    sys.exit(0)


if __name__ == "__main__":
    main()
