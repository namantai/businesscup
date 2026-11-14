# AI Compass — архитектура P01

## Граница

Модульный монолит: React/TypeScript → same-origin Vite proxy → Java/Spring Boot → PostgreSQL. В P01 Docker Compose управляет локальной БД; frontend и backend работают на хосте с закреплёнными инструментами. Production images/Caddy, предметные модули, каталог, аккаунты и Recommendation Engine не создавались заранее. Их границы остаются по CODEX_PROMPT.md.

Единственная миграция V1 создаёт пустую схему `compass`. Служебная таблица Flyway ведёт историю миграций. Hibernate использует `validate`; `open-in-view` отключён. Пустая БД не получает демонстрационные тарифы.

Readiness включает DB health, но наружу отдаёт только `status`. `/actuator/env` закрыт. CSRF не отключён; HTTP-ошибки API используют Problem Details. Только чтение scaffold API и health разрешено анонимно; аккаунты пока отсутствуют.

## Закреплённые версии

| Компонент | Версия |
|---|---|
| Temurin JDK | 21.0.13+9 |
| Maven / Wrapper | 3.10.0 / 3.3.4 |
| Spring Boot | 4.1.2 |
| Flyway / PostgreSQL JDBC | 12.4.0 / 42.7.14, управляет Boot BOM |
| JUnit Jupiter / Testcontainers | 6.0.3 / 2.0.5, управляет Boot BOM |
| PostgreSQL | 17.12-bookworm, digest в Compose и интеграционном тесте |
| Node / npm | 24.22.3 / 11.19.1 |
| React / React DOM | 19.3.1 |
| TypeScript | 6.0.3 |
| Vite / React plugin | 8.3.8 / 6.2.1 |
| Vitest / Testing Library React | 5.1.0 / 16.3.3 |
| ESLint / @eslint/js / typescript-eslint | 10.15.0 / 10.0.1 / 8.72.2 |
| Prettier | 3.10.1 |
| Playwright | 1.64.1; Chromium фиксированной ревизии в project-local cache |

Все прямые frontend-версии фиксированы без диапазонов, транзитивные версии и integrity — в `package-lock.json`. Maven-зависимости управляются фиксированным Boot parent. Maven Wrapper проверяет SHA-256 дистрибутива. Локальный Maven settings направляет репозитории зависимостей только в Maven Central и не меняет глобальные настройки.

TypeScript 7 из последнего релиза не выбран: [typescript-eslint](https://typescript-eslint.io/users/dependency-versions/) декларирует поддержку `<6.1.0`. React plugin допускает Vite 8; Vitest 5 допускает Vite 8 и Node 24. `@eslint/js` имеет собственную версию 10.0.1 с peer `eslint ^10.0.0`; предположение о совпадении версии с ESLint было исправлено после отказа npm.

Официальные источники и реестры, проверенные на P00/P01:

- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html), [managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html), Maven Central: `https://repo.maven.apache.org/maven2`.
- [Temurin 21.0.13+9](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13+9), [Node 24.22.3](https://nodejs.org/en/download/archive/v24.22.3).
- npm registry `https://registry.npmjs.org`: версии и peer dependencies сверены до успешного install/build.
- [PostgreSQL versioning](https://www.postgresql.org/support/versioning/), официальный образ `postgres`.
- [Playwright Docker](https://playwright.dev/docs/docker): первоначально выбранный образ `v1.64.1-noble` (manifest digest `sha256:2f05f8a734509a7b3f26a912639488ee08f58a7b292d14d62271bcff9038768f`) не удалось скачать: две одинаковые ошибки сохранения слоя containerd. Без вмешательства в Docker использован разрешённый project-local Chromium из того же Playwright. Arch официально не поддерживается; CI-файл использует Ubuntu 24.04. Manifest подтверждён, работоспособность Docker-образа не заявляется.

Springdoc/OpenAPI и генерация DTO будут подключены при появлении предметного API; рекомендуемая линия P00 — springdoc 3.2.0. React Router, TanStack Query и формы появятся по необходимости соответствующего этапа. Их отсутствие в пустом shell не означает изменение целевой архитектуры.

## Локальная изоляция

Compose project и volume принадлежат AI Compass. Привязки портов — только loopback. `.env` создаётся с правами 0600, случайным паролем, не коммитится и не выводится командами проверки. `compose config` вызывается с `--quiet`.

Интеграционные тесты используют настоящий PostgreSQL Testcontainers с тем же digest и отдельной БД; H2 отсутствует. В Testcontainers адрес образа записан как `postgres@sha256:...`: сочетание tag+digest не прошло внутреннюю проверку имени. Это тот же официальный образ, а не замена БД или ослабление теста.

Ryuk не запускается, так как требует Docker socket в контейнере. Очистка собственного тестового контейнера выполняется Java-кодом и shutdown hook. Принудительный kill требует ручной проверки оставшихся только проектных ресурсов. Существующие контейнеры машины не затрагиваются.

## Проверка владельцем

После `bash scripts/dev-up.sh` открыть frontend, дождаться «Сервис доступен», выполнить `bash scripts/test-persistence.sh`, затем `bash scripts/dev-down.sh`. Приёмка P01 остаётся за владельцем; P02 не разрешён автоматически.
