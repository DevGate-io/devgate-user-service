---
description: Run a full bug audit of the codebase.
model: sonnet
---

Запусти полный аудит ошибок в `src/`.

1. Launch an `Agent` (subagent_type: "general-purpose") with this brief:
   "Ты bug-hunter для Kotlin/Spring Boot проекта. Просканируй src/main/kotlin, найди реальные
   баги: null-safety проблемы (несмотря на -Xjsr305=strict), неверная обработка ошибок,
   утечки ресурсов, логические ошибки в auth/JWT/refresh-логике, проблемы транзакций JPA,
   небезопасные приведения типов. Для каждого: файл:строка, severity (critical/high/medium/low),
   описание, предлагаемый фикс. Запиши отчёт в `.claude/logs/YYYY-MM-DD-bug-hunting-report.md`.
   НИЧЕГО не меняй в коде."
2. Покажи пользователю сводку: количество багов по severity.
3. Спроси: "Исправить сейчас?" → если да:
   запусти `Agent` (subagent_type: "general-purpose") с инструкцией прочитать отчёт и
   исправить баги по убыванию severity, следуя `.claude/rules/code-generation.md`, затем
   прогнать `./gradlew compileKotlin test`.
