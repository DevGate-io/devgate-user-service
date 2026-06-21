# devgate-user-service

Сервис пользователей DevGate: REST API для управления пользователями + аутентификация
(JWT access-токен + refresh-токен в HttpOnly cookie). Часть монорепозитория DevGate
(`devgate-auth-service`, `devgate-core`, `devgate-frontend`).

## Working with me

- **Language:** все ответы и объяснения — на русском. Код, имена, комментарии — только English.
- **Commits:** на русском, conventional-style (`feat:`, `fix:`, `refactor:`, ...) — как в истории репозитория.
- **Plans first:** перед написанием кода используй `EnterPlanMode` и дождись одобрения. См. `.claude/rules/workflow.md`.
- **After code changes:** запускай проверки фоновыми агентами параллельно (tests + detekt + ktlint). См. `.claude/rules/checks.md`.
- **Logs:** после каждой задачи создавай `.claude/logs/YYYY-MM-DD-task-name.md` с обязательной секцией `## Summary`.
- **Git push:** всегда спрашивай перед push. Никогда не force-push (заблокировано `permission-guard.py`).
- **Questions mid-task:** если задаю вопрос во время выполнения — остановись, ответь, потом продолжай.
- **"Продолжаем":** найди свежий файл в `.claude/logs/`, прочитай `## Summary` и продолжай без запроса контекста.

## Stack

- **Язык/рантайм:** Kotlin 2.2.21, JVM toolchain 21 (Amazon Corretto), Gradle 9.2.1 (wrapper)
- **Framework:** Spring Boot 4.0.1 (Web MVC, Data JPA, Data REST, Security, Actuator, Validation, RestClient)
- **БД:** PostgreSQL, миграции — Liquibase 5.0 (YAML changelog)
- **Auth:** JJWT 0.12.6 (HS256), access-токен 15 мин + refresh-токен 30 дней в HttpOnly cookie `jwt-refresh-cookie`
- **Тесты:** JUnit 5 + Mockito 5, H2 для тестов
- **Качество:** ktlint (стиль, по `.editorconfig`); detekt отложен — нет стабильного релиза под Kotlin 2.2.x

## Project structure

```
src/main/kotlin/com/devgate/
├── auth/        # аутентификация: controllers, dto, security (JWT filter, token gen), services
├── users/       # пользователи: CRUD-контроллер, сервис, модель User (UserDetails), Role enum
├── config/      # SecurityConfig, AppConfig
├── exceptions/  # ApiException + GlobalErrorHandler
└── utils/       # PasswordEncoder, extensions
src/main/resources/db/changelog/   # Liquibase миграции
docker/{dev,prod}/                  # compose + Makefile, prod: nginx reverse-proxy
```

## Development commands

| Задача | Команда |
|--------|---------|
| Запуск | `./gradlew bootRun` |
| Тесты | `./gradlew test` |
| Полная сборка | `./gradlew build` |
| Type-check / статанализ | `./gradlew compileKotlin compileTestKotlin` |
| Стиль (проверка) | `./gradlew ktlintCheck` |
| Стиль (автофикс) | `./gradlew ktlintFormat` |
| Docker dev (БД) | `make -C docker/dev up` / `down` |
| Docker prod (всё) | `make -C docker/prod up` / `down` |

Pre-push git-хук (`.githooks/pre-push`) уже гоняет полный `./gradlew build` — push не пройдёт при ошибке сборки.

## Orchestration & Agents

- Делегирование задач: `.claude/rules/orchestration.md` (GATHER → DELEGATE → VERIFY → REVIEW).
- Проверки после изменений: `.claude/rules/checks.md`.
- Check-агенты (`.claude/agents/`): `run-tests`, `static-analysis`, `detekt`/`ktlint` через `code-style` (модель haiku, только чтение).

@.claude/rules/general.md
@.claude/rules/workflow.md
@.claude/rules/orchestration.md
@.claude/rules/checks.md
@.claude/rules/naming.md
@.claude/rules/code-generation.md
