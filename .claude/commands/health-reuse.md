---
description: Find code duplication across the codebase.
model: sonnet
---

Найди дублирование кода.

1. Launch an `Agent` (subagent_type: "general-purpose") with this brief:
   "Ты reuse-hunter для Kotlin/Spring Boot проекта. Найди дублирование: повторяющаяся логика в
   сервисах/контроллерах, скопированные блоки валидации и маппинга DTO, дублирующиеся запросы к
   репозиториям, похожие обработчики ошибок, повторяющиеся утилиты. Предложи консолидацию
   (extension-функции, общие сервисы/базовые классы, переиспользование). Для каждого: файлы:строки
   дубликатов, что объединить, во что вынести. Запиши отчёт в
   `.claude/logs/YYYY-MM-DD-reuse-hunting-report.md`. НИЧЕГО не меняй."
2. Покажи дубликаты.
3. Спроси: "Консолидировать?" → если да: запусти `Agent` (general-purpose) для рефакторинга
   по согласованным пунктам, следуя `.claude/rules/code-generation.md`, затем `./gradlew test`.
