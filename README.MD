[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Liquidbase](https://img.shields.io/badge/Liquidbase-5.0-blue)
![Gradle](https://img.shields.io/badge/Gradle-8.0.2-blue)
![Docker](https://img.shields.io/badge/Docker-blue)

# DevGate User Service

Сервис пользователей DevGate

## 🛠️ Технологический стек

### Основные технологии

- Kotlin 2.2.21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Liquidbase

### Вспомогательные технологии

- Docker Compose
- Gradle

## 🚀 Функциональность

- Feature 1
- Feature 2
- Feature 3
- Feature 4

## 🔗 API Reference

#### Get all items

```http
  GET /api/items
```

| Parameter | Type     | Description                |
|:----------|:---------|:---------------------------|
| `api_key` | `string` | **Required**. Your API key |

#### Get item

```http
  GET /api/items/${id}
```

| Parameter | Type     | Description                       |
|:----------|:---------|:----------------------------------|
| `id`      | `string` | **Required**. Id of item to fetch |

## ⚙️ Переменные окружения

Чтобы запустить этот проект, вам нужно добавить следующие переменные окружения в ваш `.env` файл

| Переменная        | Принимаемый тип | Описание        |
|:------------------|:----------------|:----------------|
| `API_KEY`         | `string`        | API-ключ        |
| `ANOTHER_API_KEY` | `string`        | Другой API-ключ |

## 🚀 Локальный запуск

Склонируйте проект

```bash
  git clone https://github.com/Pr0gger1/spring-boot-kotlin-template
```

Перейдите в директорию проекта

```bash
  cd spring-boot-kotlin-template
```

Установите зависимости в `build.gradle.kts`, используя Intellij IDEA

Запустите сервер

```bash
  ./gradlew bootRun
```

## 📦 Запуск в Docker

Есть два окружения проекта: **dev** и **prod**. Они разбиты по папкам в каталоге `docker`

- В окружении dev приложение запускается локально, а база данных - в контейнере

- В окружении prod все элементы системы запускаются в собственных сервисах-контейнерах

Пример запуска полноценного докеризированного приложения:

```bash
  docker compose up -f docker/prod/compose.yaml
```

## 👨‍💼 Авторы

- [@pr0gger1](https://www.github.com/Pr0gger1)

## 📜 Документация

[Documentation](https://linktodocumentation)

## 📨 Обратная связь

Если у вас есть какие-то вопросы или предложения, пожалуйста, напишите на почту <a href="mailto:pr0gger.dev@gmail.com">
pr0gger.dev@gmail.com</a>

## 🪪 Лицензия

[MIT](https://choosealicense.com/licenses/mit/)
