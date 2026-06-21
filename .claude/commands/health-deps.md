---
description: Audit Gradle dependencies for outdated or vulnerable packages.
model: sonnet
---

Аудит зависимостей (`build.gradle.kts`).

1. Launch an `Agent` (subagent_type: "general-purpose") with this brief:
   "Ты dependency-auditor для Gradle/Kotlin проекта. Проанализируй build.gradle.kts и версии
   зависимостей (Spring Boot, Kotlin, JJWT, PostgreSQL driver, Liquibase, Mockito, detekt, ktlint).
   Найди: устаревшие версии (с указанием актуальной), известные CVE, конфликты версий, лишние
   зависимости. Можно использовать `./gradlew dependencies` и web-поиск CVE. Для каждого:
   зависимость, текущая → рекомендуемая версия, причина, риск обновления.
   Запиши отчёт в `.claude/logs/YYYY-MM-DD-dependency-report.md`. НИЧЕГО не меняй."
2. Покажи устаревшие и уязвимые зависимости.
3. Спроси: "Обновить?" → если да: обнови версии в build.gradle.kts по согласованию, затем
   `./gradlew build` для проверки совместимости.
