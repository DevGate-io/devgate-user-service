---
description: Find and remove dead code.
model: sonnet
---

Найди и удали мёртвый код.

1. Launch an `Agent` (subagent_type: "general-purpose") with this brief:
   "Ты dead-code-hunter для Kotlin проекта. Найди: неиспользуемые классы/функции/свойства,
   недостижимый код, неиспользуемые import'ы, закомментированный код, незадействованные DTO и
   методы сервисов. Учитывай, что точки входа — Spring-бины, контроллеры, JPA-репозитории и
   тесты (их публичные методы могут вызываться рефлексией/фреймворком — помечай как сомнительные,
   не удаляй вслепую). Для каждого: файл:строка, уверенность (high/medium/low), обоснование.
   Запиши отчёт в `.claude/logs/YYYY-MM-DD-dead-code-report.md`. НИЧЕГО не меняй."
2. Покажи найденное, сгруппированное по уверенности.
3. Спроси: "Удалить (только high-confidence)?" → если да: запусти `Agent` (general-purpose)
   для удаления только high-confidence находок, затем `./gradlew compileKotlin test`.
