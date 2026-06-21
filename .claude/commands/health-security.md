---
description: Run a security audit of the codebase.
model: sonnet
---

Запусти аудит безопасности.

1. Launch an `Agent` (subagent_type: "general-purpose") with this brief:
   "Ты security-scanner для Kotlin/Spring Boot сервиса аутентификации. Проверь: конфигурацию
   Spring Security и CORS, обработку JWT (подпись HS256, валидация exp/type), хранение и
   ротацию refresh-токенов, хеширование паролей, утечки секретов в логи/ответы, IDOR в
   эндпоинтах /users, корректность @PreAuthorize, SQL/инъекции, небезопасные настройки cookie
   (secure/sameSite/httpOnly). Для каждого: файл:строка, severity, описание, рекомендация.
   Запиши отчёт в `.claude/logs/YYYY-MM-DD-security-scan-report.md`. НИЧЕГО не меняй."
2. Покажи критичные уязвимости.
3. Спроси: "Исправить?" → если да: запусти `Agent` (general-purpose) для фиксов по убыванию
   severity с последующим `./gradlew compileKotlin test`.
