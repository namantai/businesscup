# AI Compass — фактический прогресс

**План:** `../CODEX_PROMPT.md`, версия 1.0, 14.11.2026.
**Текущее состояние:** P01–P07 = DONE; P08 и P10.a приняты владельцем для локальной beta, локальные QA P11 также приняты (15.11.2026). Публичный запуск не разрешён.
**Текущий этап:** финальное локальное закрытие. P08 = ACCEPTED (local beta); P09 = DEFERRED; P10.a = ACCEPTED (local beta); P10.b = DEFERRED; P11 local QA = ACCEPTED. Общий локальный статус — ACCEPTED. Independent-review, coverage и production gates остаются OPEN / NOT_RUN / NOT_VERIFIED, не объявлены DONE.
**Следующее действие:** локальная демонстрация по `docs/DEMO_GUIDE.md`. Проверки closeout завершены; кандидат подготовлен к единственному разрешённому commit `feat: complete ai compass local beta candidate`. После фиксации работа остановлена. Дальнейшие изменения только по отдельному поручению; push/PR/merge/deployment и P12–P14 запрещены. Принятый локальный scope отделён от открытых production gates.
**Источник состояния:** этот файл + фактический Git/code/test evidence, не обещания агента.

## 1. Маршрут

| Этап | Название | Статус | Приёмка владельца / доказательство |
|---|---|---|---|
| P00 | Read-only аудит | ACCEPTED | Владелец 14.11.2026: «P00 ПРИНЯТ. Переходим к P01» |
| P01 | Каркас и инфраструктура | DONE | Закрыт commit d895c04; затем владелец явно сообщил «P01 ПРИНЯТ» и разрешил P02 |
| P02 | Типы, schema, fixtures | DONE | Владелец явно сообщил «P02 ПРИНЯТ» и разрешил P03; закрыт commit 8fc3304 |
| P03 | Проверенный каталог и API | DONE | Владелец поручил финальную проверку 5 утверждений, все проверки и один commit; выполнено 14.11.2026, итог в разделе закрытия P03 |
| P04 | Recommendation Engine | DONE | Владелец явно поручил закрытие; 10 инвариантов проверены, 185 backend +18 UI +14 E2E, p95=61.19 ms; исправлена граница horizon, engine1.0.1 |
| P05 | Гостевой frontend / R1 | DONE | По явному поручению владельца от 15.11.2026: 10 групп инвариантов проверены; повторно 185 backend +42 UI +27 E2E PASS, все обязательные команды exit 0; итог в разделе закрытия P05 |
| P06 | Аккаунты и сохранение | DONE | Окончательное закрытие по поручению владельца 15.11.2026: исправлены 2 дефекта rate limiting, 118 unit +93 integration +53 UI +34 E2E PASS; все обязательные команды exit 0; P07 не разрешён |
| P07 | Портфель и расходы | DONE | Владелец явно принял отчёт и поручил закрытие без commit; проверены сохранённые 146 unit +100 PostgreSQL IT +60 UI +38 E2E PASS, повторно catalog/smoke/secrets/diff PASS |
| P08 | Admin, мониторинг, уведомления | ACCEPTED (local beta, owner 15.11.2026) | 155 unit +105 PostgreSQL IT +65 UI; 39 E2E полный запуск и 1/1 финальный целевой повтор; остальные обязательные scripts PASS |
| P09 | Optional LLM explanations | DEFERRED | Детерминированные объяснения достаточны; LLM/SDK/платные API не подключены, OFF |
| P10.a | Локальные Free/Premium entitlements и beta | ACCEPTED (local beta, owner 15.11.2026) | 155 unit +109 PostgreSQL IT +71 UI +40 E2E PASS; без платежей |
| P10.b | Payment provider | DEFERRED | Нет выбранного PSP, merchant eligibility и разрешения на SDK/сети/checkout |
| P11 | Hardening и beta / R2 | ACCEPTED — local QA only; remaining gates OPEN | 156 unit +112 PostgreSQL IT +74 UI +40 E2E PASS; local ops PASS; independent review/coverage NOT_RUN |
| P12 | Staging / deployment / ops | NOT_STARTED | — |
| P13 | Paid pilot / R3 | NOT_STARTED | — |
| P14 | Данные пилота и бизнес-решение | NOT_STARTED | — |

Допустимые статусы: NOT_STARTED, IN_PROGRESS, BLOCKED, AWAITING_REVIEW, IMPLEMENTED_PENDING_OWNER_REVIEW, ACCEPTED, DEFERRED. IMPLEMENTED_PENDING_OWNER_REVIEW разрешён новым пакетным планом P08–P11, без самостоятельной приёмки. Агент не назначает ACCEPTED без явной приёмки владельца. Отложенная часть не считается выполненной. Для закрытия P01 владелец отдельным сообщением 14.11.2026 явно предписал статус DONE; это исключение для данного этапа, без разрешения на P02.

## 2. Зафиксированные продуктовые решения

| ID | Решение | Основание |
|---|---|---|
| D01 | Подбор наборов AI-подписок/агентов под несколько потребностей | Согласованная концепция владельца |
| D02 | Нет обязательного задания, промпта, кода или документа | Согласованное ограничение владельца |
| D03 | Сравнение качества отдельно от функционального покрытия | Правило достоверности плана |
| D04 | Free/студенческие/альтернативные продукты рассматриваются без paid bias | Правило независимого подбора |
| D05 | Модульный монолит; версии P00 подтверждаются установкой P01 | P00 принят 14.11.2026; фактические версии в ARCHITECTURE.md |
| D06 | Весь код пишет Codex; человек принимает этапы и разрешает внешние действия | Рабочий процесс |
| D07 | Premium 4.99 USD/месяц — гипотеза, не подтверждённый спрос | Бизнес-гипотеза |
| D08 | Никаких платных/live интеграций по умолчанию | Безопасная начальная конфигурация |

## 3. Разрешения

| Действие | Текущее разрешение |
|---|---|
| FINAL LOCAL BETA CLOSEOUT | Владелец 15.11.2026 принял P08/P10.a и локальный QA P11; разрешил один локальный commit всего кандидата P07–P11 после проверок. Без новых зависимостей, изменения применённых миграций, push/PR/merge/deployment. Прежние запреты commit ниже исторические и заменены только этим разрешением; production/review/coverage gates открыты |
| P00 read-only аудит в рабочем репозитории | Выполнен и принят 14.11.2026 |
| Локальные изменения кода | Владелец 15.11.2026 подтвердил P07 DONE и разрешил P08 → P09 → P10.a → P11 по доступному AI_COMPASS_BATCH_P08_P11.md без промежуточных остановок. Новые фазы получают IMPLEMENTED_PENDING_OWNER_REVIEW/AWAITING_REVIEW, не DONE. P09 допускает обоснованный DEFERRED; P10 только локальные права/beta. Новые прямые зависимости требуют отдельного разрешения. Commit до итоговой приёмки, push/PR/deployment, платные API, реальные платежи/email, неразрешённый scraping, global installs и P12 запрещены |
| Первоначальный локальный commit и ветка | Владелец 14.11.2026: четыре исходных файла без изменений, затем codex/p01-scaffold; выполнено |
| Установка project dependencies / Docker images / browsers | P01 — ранее разрешённая локальная инфраструктура. P03 — владелец отдельно разрешил springdoc 3.2.0, react-router 8.5.1, @tanstack/react-query 5.107.1, @hey-api/openapi-ts 0.99.0 и их транзитивные зависимости; без других прямых зависимостей, sudo/global config/платных услуг |
| Системные установки / sudo / global config | Нет |
| Новая прямая зависимость P06 | Владелец 15.11.2026 отдельно разрешил `org.springframework.boot:spring-boot-starter-session-jdbc:4.1.2` и необходимые транзитивные зависимости, включая Spring Session JDBC/Core 4.1.1. Только backend/pom.xml, версии через Boot BOM, без sudo/global installs и других прямых зависимостей; установка и сборка выполнены |
| Локальный commit P06 | Владелец 15.11.2026 предварительно принял P06 и разрешил окончательное закрытие: повторный security review/checks, DONE только после PASS, один локальный commit `feat: implement authentication and saved ai profiles`; без push/PR/P07 |
| Платные LLM/API | Нет |
| Реальные письма/уведомления внешним адресатам | Нет |
| Production платежи / тестовые списания | Нет |
| VPS/домен/хостинг/облако | Нет |
| Локальный commit P05 | Владелец 15.11.2026 разрешил один commit после успешных обязательных проверок: `feat: implement personalized ai stack builder ui`; без push/PR/P06 |
| Локальный commit P04 | Владелец 14.11.2026 разрешил один commit после проверки инвариантов и обязательных checks: `feat: implement personalized ai recommendation engine`; без push/PR/P05 |
| Локальный commit P03 | Владелец 14.11.2026 разрешил один commit после проверки 5 источников и обязательных checks: `feat: implement verified ai catalog and public api`; без push/PR/P04 |
| Локальный commit P02 | Владелец 14.11.2026 разрешил один commit после архитектурной проверки и успешных тестов: `feat: implement versioned ai catalog data model`; без push/PR |
| Локальный commit закрытия P01 | Владелец 14.11.2026: один commit только P01, сообщение `feat: bootstrap ai compass infrastructure`; без push/PR |
| Git push / merge / deploy | Нет |
| Удаление volumes/данных/чужих файлов | Нет |

Новые разрешения записывать точно: дата, кто дал, область, лимит расходов, срок/условие. Сам факт наличия ключа не является разрешением использовать его.

## 4. Блокеры и решения до соответствующего этапа

| ID | Неизвестность | Блокирует | Что требуется |
|---|---|---|---|
| B01 | Репозиторий и окружение проверены | Снят | Исходный commit ff7f556, Docker доступен, порты назначены |
| B02 | Основные зависимости установлены и собраны | Снят для каркаса | Maven verify и frontend checks прошли; браузерная проверка отдельно |
| B03 | Reviewed рыночные данные | R1 market-ready | P03 provenance/review |
| B04 | Платёжный провайдер для реального продавца | P10.b / P13 | Eligibility + sandbox account + ADR-PAYMENTS |
| B05 | Домен/платформа/регион/бюджет | P12 | Решение владельца |
| B06 | Правовые, налоговые и privacy условия | P13 | Отдельная проверка владельца |
| B07 | Готовность пользователей платить повторно | Business validation | Реальный пилот и полный период |

Эти записи — известные будущие decision gates, не сообщение о провале ещё не начатой разработки.

## 5. Исторический протокол P01

- Этап: P01 — Infrastructure & Project Scaffold.
- Владелец 14.11.2026 явно принял P00 и разрешил только P01, локальные установки/контейнеры/тесты/миграции/docs. Расходы на платные услуги: не разрешены. Системные настройки, push, deploy, внешние письма/платежи и P02 запрещены.
- Исходный commit: `ff7f556` — ровно четыре исходных файла без изменения содержимого. После него создана `codex/p01-scaffold`.
- P00: один worktree, до начала не было коммитов; Java 21, Docker daemon доступен; 8080 занят; выбраны 5173/18080/15432. Версии и ограничения проверены по официальным источникам и затем реальной установкой.
- Созданы backend/frontend, первая техническая Flyway migration, loopback Compose PostgreSQL, безопасная локальная конфигурация, Wrapper/lockfile, скрипты, тесты и ручной CI workflow. Предметные таблицы/каталог/рекомендации не реализованы.
- Группы файлов: `backend/`, `frontend/`, `infra/`, `scripts/`, `.github/workflows/ci.yml`, корневые настройки, README, ARCHITECTURE и этот журнал. AGENTS, CODEX_PROMPT и HTML-референс не менялись.
- Ручной review: безопасность настроек, CSRF, фильтр requestId/Problem Details, отсутствие внешних адаптеров, loopback binds, project-only lifecycle, сохранность volume, игнорирование приватных файлов; скриншоты 360/1440 px просмотрены. Арифметика цены, hard constraints и UNKNOWN в engine: NOT_APPLICABLE для P01, ядра ещё нет.
- Владелец 14.11.2026 сообщил: «P01 предварительно принят по предоставленному отчёту» и поручил только закрытие. Оно оформляется одним локальным коммитом `feat: bootstrap ai compass infrastructure` после повторных проверок; статус DONE назначается по прямому поручению владельца, P02 запрещён.

## 6. Фактические проверки

Дата: 14.11.2026. Команды выполнялись из корня, кроме явно указанных. `maven` — helper из `scripts/common.sh`: Maven Wrapper с локальными settings и `.cache/m2`.

| Команда | Статус / exit | Фактический результат |
|---|---|---|
| `bash scripts/setup.sh` | PASS / 0 | Temurin 21.0.13+9 и Node 24.22.3 скачаны в .tools; SHA-256 проверены; .env создан с 0600, пароль не выведен |
| `mvn -B -ntp -Dmaven.repo.local=/home/mrturtle/AITU/businesscup/.cache/m2 org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.10.0 -Dtype=only-script` в backend | PASS / 0 | Создан Maven Wrapper 3.3.4; дистрибутив Maven дополнительно проверен по SHA-512, SHA-256 закреплён |
| `npm install --no-audit --no-fund` в frontend с project-local PATH/cache | PASS / 0 после исправления | Установлено 230 пакетов; создан lockfile |
| `bash scripts/check.sh` | PASS / 0 | clean verify: 5 unit + 6 PostgreSQL integration; npm ci (230 packages), format/lint/typecheck, 7 UI tests, build |
| `source scripts/common.sh; maven clean verify` после финального review Problem Details | PASS / 0 | 5 unit + 6 integration, failures/errors/skipped = 0; проверены requestId и instance для 403/404 |
| `bash scripts/dev-down.sh && bash scripts/dev-up.sh && bash scripts/test-persistence.sh && bash scripts/test-e2e.sh` | PASS / 0 | Проверен обычный запуск с пересборкой, повторная сохранность данных и 3 браузерных сценария на финальном backend. В dev-up tests явно skipped; доказательство тестов — отдельный verify |
| `docker pull postgres:17.12-bookworm` | PASS / 0 | Официальный образ и digest закреплены в Compose/Testcontainers |
| `compose config --quiet` через common.sh | PASS / 0 | Конфигурация валидна; секреты не печатались |
| `bash scripts/dev-up.sh --no-build` | PASS / 0 | PostgreSQL healthy, backend и frontend доступны на loopback |
| `bash scripts/smoke.sh` | PASS / 0 | 3 проверки: прямой readiness, readiness через proxy, HTML frontend |
| `bash scripts/test-persistence.sh` | PASS / 0 после исправления | UUID сохранён после stop/start; проверочная таблица удалена; volume сохранён |
| `bash scripts/test-e2e.sh` | PASS / 0 | 3 Chromium smoke: 360/768/1440, настоящая readiness, повтор запроса, нет console/page errors или горизонтального overflow |
| PostgreSQL SQL: version, flyway history, schema tables | PASS / 0 | PostgreSQL 17.12; миграция V1 success; 0 предметных таблиц в compass после очистки probe |
| `bash scripts/check-secrets.sh` | PASS / 0 | Нет совпадений шаблонов или фактического DB-пароля в tracked/untracked nonignored files |
| Проверка фактического DB-пароля в локальных log/XML-файлах | PASS / 0 | 0 совпадений; значения не выводились; финальный повтор по текущим файлам |
| `git diff --check`, проверка новых файлов через `git diff --no-index --check` | PASS / 0 после исправления Markdown whitespace | Нет ошибок пробелов; начальные документы и референс сохранены |

Артефакты (локальные, игнорируются Git): `.runtime/check.log`, `.runtime/backend-final.log`, `backend/target/surefire-reports/`, `backend/target/failsafe-reports/`, `frontend/test-results/scaffold-{360,768,1440}.png`. Реальные версии — ARCHITECTURE.md и manifests; npm использует один package-lock.json.

### Ошибки, исправления и ограничения

- API Adoptium metadata вернул HTTP 403; официальный GitHub-релиз с checksum был доступен и использован.
- Первое npm install: FAIL / 1, `@eslint/js@10.15.0` отсутствует. У пакета собственная версия 10.0.1 с peer `eslint ^10.0.0`; исправлено без `--force` и без отключения peer checks.
- Первый backend verify: unit 5 passed; failsafe 6 errors (5 сценариев и lifecycle), 0 skipped. Testcontainers отверг tag+digest. Использован тот же проверенный PostgreSQL через `postgres@sha256:...`; повторный verify прошёл. Число IT затем увеличено до 6 отдельной проверкой CSRF-фильтра.
- Maven обнаруживал private repositories из транзитивных POM и получал 401. Project-local mirror направляет обращения только в Maven Central; глобальные настройки не менялись.
- Первый persistence script: FAIL / 1 при перезапуске из-за TIME_WAIT, не активного listener. Проверка bind исправлена через SO_REUSEADDR; сохранённая строка подтверждена SQL, повторный полный сценарий прошёл. Проверки сборки и перезапуска финальной версии выполняются последовательно.
- Playwright Docker pull: FAIL / 1 два раза, одна и та же ошибка rename отсутствующего ingest-файла containerd. Docker/system configuration не менялась, посторонние ресурсы не удалялись. Manifest digest прочитан, запуск этого образа — NOT_RUN. Вместо него скачан project-local Chromium 156.0.8078.4 (revision 1248) и FFmpeg 1013; на Arch это Ubuntu fallback, официальная поддержка Arch не заявляется. Реальные браузерные тесты прошли.
- Mockito из Boot test starter выдаёт предупреждение self-attach на Java 21. Тесты проходят; предупреждение не подавлялось и не является разрешением отключать проверки.
- Backend health/Problem Details проверяются локально. Аккаунты, domain API/OpenAPI, каталог и engine не входят в P01. Безопасность с реальными пользователями и production не заявляется.
- Remote CI, production images/deploy, платежи, email, LLM и collectors: NOT_RUN, вне разрешённого P01. Сам файл CI создан с ручным workflow_dispatch, push не выполнялся.
- Владелец предварительно принял P01 по предоставленному отчёту и поручил закрытие. Самостоятельный запуск владельцем не подтверждён и не приписывается ему; переход к P02 по-прежнему запрещён без отдельного поручения.

## 7. Каталог и внешние условия

- Published catalog version: отсутствует.
- Verified market offers: не импортированы.
- Источники из HTML: только кандидаты на перепроверку.
- LLM mode: OFF; включение блокируется конфигурацией P01.
- Payment mode: OFF; платёжного адаптера нет.
- Email mode: LOCAL; почтового адаптера/Mailpit пока нет, внешних писем нет.
- Live collectors: не разрешены.
- Billing provider: не выбран.
- Deployment: не выполнен.

## 8. Журнал изменений плана

| Дата | Что изменено | Причина | Кто согласовал | Какие этапы/тесты затронуты |
|---|---|---|---|---|
| 14.11.2026 | Создан план v1.0 | Подготовка последовательной разработки | Владелец принял P00 и поручил P01 | P00–P14 |
| 14.11.2026 | P01 выполнен без предметных функций; уточнены фактические версии и browser fallback | Реальные registry/build/runtime проверки | В рамках разрешённого P01; затем предварительно принят владельцем по отчёту | P01 |
| 14.11.2026 | Закрытие P01 со статусом DONE и одним локальным коммитом | Повторные проверки и аудит состава коммита | Прямое поручение владельца; P02 не разрешён | P01 |

## 9. Release readiness

- [ ] R1: P00–P05 приняты; правдивые данные и работающий гостевой подбор.
- [ ] R2: сохранение, портфель, мониторинг, права и beta hardening; отложенные интеграции явно обозначены.
- [ ] R3: реальный PSP, production, backup restore, условия оплаты и поддержка.
- [ ] P14: реальные показатели пользы/повторной оплаты и решение о продолжении.

## 10. Следующий запуск Codex

P02 = DONE. P03 = NOT_STARTED. После локального коммита P02 остановиться; не начинать P03 без отдельного разрешения.

## 11. Итог P01 и критерии

- [x] Исходные четыре файла сохранены commit ff7f556, затем создана отдельная ветка.
- [x] Backend/frontend с закреплёнными версиями, Maven Wrapper и package-lock.
- [x] Editor/ignore/env и formatter/linter; .env 0600, случайный пароль вне Git.
- [x] Compose project ai-compass-p01, отдельные сеть/volume, PostgreSQL healthcheck; все published ports loopback.
- [x] Backend readiness с проверкой БД; frontend получает его через proxy; базовые Problem Details с requestId.
- [x] Flyway V1 на реальном PostgreSQL; schema validate; demo seed отсутствует.
- [x] 5 unit + 6 integration + 7 UI + 3 browser tests: **21 passed, 0 failed/errors, 0 skipped** в тестовых прогонах.
- [x] Clean build, lint, typecheck, format; smoke; сохранность UUID после stop/start.
- [x] CI-файл создан локально, remote CI не запускался.
- [x] README, ARCHITECTURE, скрипты запуска/остановки/проверок, этот журнал.
- [x] Ручной diff/security/scope review, скриншоты, whitespace и ограниченное сканирование секретов.
- [x] Владелец предварительно принял P01 по отчёту и явно поручил закрытие со статусом DONE. Самостоятельный запуск владельцем не подтверждён.

Git закрытия: `codex/p01-scaffold`, исходный commit `ff7f556`. Для одного локального коммита P01 отобраны 49 новых файлов и docs/PROGRESS.md (50 файлов). Сообщение: `feat: bootstrap ai compass infrastructure`. Хеш и фактическое состояние после коммита выводятся в итоговом отчёте. Push/PR не выполняются. AGENTS.md, CODEX_PROMPT.md и reference HTML совпадают с исходным commit.

На момент отчёта приложение оставлено работающим: frontend 127.0.0.1:5173, backend 127.0.0.1:18080, PostgreSQL 127.0.0.1:15432 healthy. Остановка: `bash scripts/dev-down.sh`; volume остаётся. Последний SQL-контроль: V1 success, таблиц предметной области в compass нет, persistence probe удалён. Docker socket в контейнеры не монтировался; privileged mode не использовался.

Статус: **DONE** по прямому поручению владельца. **P02 = NOT_STARTED**. После закрытия — остановиться; следующий этап требует отдельного разрешения.

## 12. Повторная проверка закрытия P01 — 14.11.2026

Scope: только аудит файлов, повторные проверки, обновление этого журнала и один локальный commit. Код приложения и зависимости при закрытии не изменялись. Ветка проверена: codex/p01-scaffold.

| Команда | Результат | Фактические числа / ограничение |
|---|---|---|
| `bash scripts/check.sh` | PASS / exit 0 | Maven clean verify: 5 unit + 6 PostgreSQL integration; npm ci: 230 packages; format/lint/typecheck/build PASS; UI: 7 passed; failures/errors/skipped = 0 |
| `bash scripts/smoke.sh` | PASS / exit 0 | 3 проверки: backend readiness, readiness через frontend proxy, HTML |
| `bash scripts/check-secrets.sh` | PASS / exit 0 | Совпадений шаблонов и локального DB-пароля не найдено; ограниченный сканер, не гарантия полного security-аудита |
| `git diff --check` | PASS / exit 0 | Нет ошибок пробелов |
| Аудит списка будущего коммита | PASS / exit 0 | 50 файлов; нет symlinks, .env, кешей, node_modules, target/dist, логов, браузерных отчётов и архивов; .env.example содержит только безопасный образец |
| `git check-ignore .env .cache .tools .runtime backend/target frontend/node_modules frontend/dist frontend/test-results` | PASS / exit 0 | Все 8 локальных путей игнорируются |

Повторный прогон закрытия: **18 тестов passed, 0 failed/errors, 0 skipped**, отдельно 3 smoke-проверки. Лог: `.runtime/p01-close-check.log` (не коммитится); backend XML — target/surefire-reports и target/failsafe-reports (не коммитятся).

Playwright/E2E и persistence при закрытии повторно не запускались: NOT_RUN, их успешные результаты относятся к предыдущей реализации P01 и сохранены выше. Известный Ubuntu fallback Chromium на Arch и две ошибки Docker pull сохранены; remote CI остаётся NOT_RUN. Никаких новых заявлений об официальной поддержке Arch, production или remote CI нет.

При ручном просмотре состава коммита случайных файлов не обнаружено. Исходные AGENTS.md, CODEX_PROMPT.md и HTML-референс не входят в изменения закрытия. Разрешён только локальный commit; push, PR и P02 не выполняются.

## 13. P02 — Data Model & Database Design

P01 явно принят владельцем в поручении P02. Прочитаны полностью AGENTS.md, CODEX_PROMPT.md, этот журнал и ARCHITECTURE.md. Исходное дерево чистое, ветка codex/p01-scaffold, HEAD d895c04. Работа остаётся в этой ветке; commit до приёмки P02 запрещён. V1 сохраняется; используются существующие зависимости и локальный PostgreSQL/Testcontainers. Фактические результаты ниже. P02 завершён в статусе AWAITING_REVIEW; P03 не начат.


### Выполнено в P02

- V1 сохранена побайтно. V2 создаёт 23 таблицы каталога, version-scoped PK/FK, UNIQUE/CHECK, индексы и точные numeric domains; V3 защищает неизменяемость, соответствие evidence полю и UNKNOWN.
- Разделены provider/product/model family/exact model/agent/plan/offer/region/need/capability. ER-модель и назначение каждой таблицы — ARCHITECTURE.md.
- `CatalogManifest`, typed enums, строгий JSON contract, Bean Validation и `CatalogImportValidator`; понятные ошибки полей/ссылок, без вывода исходного содержимого в parse errors.
- Атомарный внутренний `CatalogImportService`: validate/hash/DRAFT/rows/PUBLISHED; повторный и конкурентный импорт без дублей, откат поздней DB-ошибки, история старых версий.
- `Money`, `FxConversion`, `OfferCosts`, UTC Clock и календарный `RenewalSchedule`; никаких float/double для денег, никаких вымышленных реальных FX.
- JSON Schema и синтетический manifest: 14 сценарных тарифов, 15 региональных предложений, более 12 различных границ (перечень в data/fixtures/README.md).
- DEMO выключен по умолчанию, разрешается только явным dev/test flag/profile; нет auto-seed/HTTP import/URL fetch. Fixtures отсутствуют в runtime JAR. В dev БД после миграции 0 catalog versions.
- В check.sh включена проверка schema; новые зависимости/версии не добавлялись. Maven POM изменён только для test resources; frontend source и lockfile не изменены.
- README уточнён, чтобы больше не утверждать, что существует только P01. AGENTS.md, CODEX_PROMPT.md и HTML-референс не изменены.

### Фактические проверки P02 — 14.11.2026

Команды из корня; `maven` — helper после `source scripts/common.sh`. Docker/network sandbox expansion применялась только к разрешённым локальным сборкам, контейнерам и loopback-проверкам.

| Команда / проверка | Результат / exit | Факты |
|---|---|---|
| `maven -o test -DskipTests` | PASS / 0 | Начальная compilation; tests SKIPPED явно, не доказательство тестов |
| `bash scripts/check.sh` | PASS / 0 | clean compilation/package/verify: 60 unit + 41 PostgreSQL IT; npm ci 230 packages; format/lint/typecheck/build PASS; UI 7 passed; schema 1 valid + 12 invalid cases |
| `maven -o verify` после финального review Money JSON и cleanup конкурентного теста | PASS / 0 | **61 unit + 41 PostgreSQL IT**, failures/errors/skipped = 0; packaged final JAR |
| `bash scripts/check-catalog.sh` | PASS / 0 | 1 synthetic manifest accepted, 12 invalid shape cases rejected; Ajv 6.15.0 уже был в lockfile |
| `bash scripts/dev-down.sh`, `bash scripts/dev-up.sh --no-build` | PASS / 0 | Существующая dev БД обновлена с V1 до V3; финальный JAR повторно запущен; Flyway validated 3 migrations, schema up to date |
| `bash scripts/smoke.sh` | PASS / 0 | 3 read-only проверки: direct readiness, proxy readiness, frontend HTML; финальный повтор PASS |
| `bash scripts/test-persistence.sh` | PASS / 0 | Контрольный UUID сохранился после stop/start; только собственная probe-таблица удалена; volume сохранён |
| `bash scripts/test-e2e.sh` | PASS / 0 | 3 Chromium сценария 360/768/1440, health/retry/console/overflow; прежний Ubuntu fallback на Arch |
| Read-only SQL dev DB | PASS / 0 | V1/V2/V3 success; 23 таблицы compass; 0 catalog versions, 0 probe tables |
| Runtime JAR inspection | PASS / 0 | Нет catalog-demo.json и fixture resources |
| `bash scripts/check-secrets.sh` | PASS / 0 | Нет совпадений ограниченного сканера или фактического локального DB-пароля |
| `git diff --check`, whitespace review новых файлов | PASS / 0 | После удаления лишней пустой строки в V2; --no-index возвращает 1 для нового содержимого, без whitespace diagnostics |
| Аудит состава изменений | PASS / 0 | 6 изменённых tracked + 23 новых файла; нет .env, caches, build artifacts, node_modules, symlinks |

Последние результаты по слоям: **61 unit + 41 IT + 7 UI + 3 E2E = 112 passed, 0 failed/errors, 0 skipped**. Это сумма последних прогонов соответствующих слоёв, не утверждение об одном совмещённом прогоне. Отдельно 13 schema-проверок и 3 smoke-проверки. P01 regression сохранена: 5 IntegrationSettings unit, 6 ScaffoldIT, 7 UI, 3 E2E.

Локальные игнорируемые артефакты: `.runtime/p02-check.log`, `.runtime/p02-final-backend.log`, `.runtime/p02-runtime.log`, `.runtime/p02-final-runtime.log`, backend/target/{surefire,failsafe}-reports, frontend/test-results. Отчёты не добавлены в Git.

### Найденные и исправленные проблемы

- Первые unit-прогоны: **FAIL / 1**, 58 tests, 1 failure: Jackson преобразовывал числовую цену в String несмотря на общий scalar guard. Добавлен явный запрет Integer/Float/Boolean → Textual; regression test сохранён. Повторный verify до этого исправления также завершился на той же unit-ошибке, IT в нём не запускались.
- Дополнительный контрактный прогон: **FAIL / 1**, 59 tests, 1 failure: Jackson принимал числовой epoch как Instant. Добавлен строгий UTC string deserializer; regression test сохранён.
- Self-review: денежная сериализация закреплена decimal string, без scientific notation; отдельный финальный unit test. Безуспешные тесты не удалялись и ожидания не ослаблялись.
- SQL CHECK использует `IS TRUE` для условных nullable сумм; чрезмерная точность отклоняется, а не округляется до нуля. Измерения benchmark отделены от денег и могут быть знаковыми; кредиты имеют свой numeric domain.
- Защита snapshot ставится только на перечисленные таблицы P02, не на временную persistence probe. Конкурентная публикация и повторный импорт проверены настоящими параллельными DB-транзакциями.
- Убрана лишняя пустая строка в конце V2; финальный запуск подтвердил прежний Flyway checksum/validate на уже обновлённой dev БД. V1 не редактировалась.

### Ограничения и ручной review

- P03 **NOT_STARTED**: реальных reviewed данных, публичного catalog API, CLI review/activation нет. Recommendation Engine, ранжирование, учёт личного портфеля и профили пользователей не реализованы.
- Cost helpers возвращают компоненты, не подтверждённую пригодность: evidence freshness/eligibility/annual opt-in/hard budget проверит P04. Доплаты P02 относятся к одному billing period; более сложный график add-ons — P04/P07.
- JSON Schema и Java validation — два проверяемых представления контракта; при изменениях поддерживать оба. Ajv используется из существующей транзитивной зависимости ESLint и закреплён lockfile; npm upgrade должен сохранять эту проверку.
- Remote CI **NOT_RUN**, не было push/PR; production deployment, внешние AI/payment/mail/source integrations **NOT_RUN**, вне этапа. Mockito self-attach warning сохранён.
- Playwright: 3 tests PASS; Arch остаётся официально неподдерживаемой ОС с Ubuntu fallback. Docker Playwright image из P01 по-прежнему не проверен; прежние ошибки containerd не устранялись глобальными настройками.
- Измерение процентов code/branch coverage **NOT_RUN**: инструмент покрытия в существующем стеке не настроен; проценты не заявляются. Поведенческие negative/integration tests и точные числа приведены выше.
- Ручной self-review выполнен: scope/связи/PK-FK-CHECK, nullable суммы, источники и отсутствие рейтингов, межверсионные ссылки, publication locks, rollback, default OFF/demo isolation, состав Git и целостность V1. Независимая приёмка владельцем ещё не выполнена; screenshots не выдаются за доказательство денежных правил.

### Критерии P02

- [x] Money/currency/billing/evidence/eligibility/capability types.
- [x] Flyway schema + immutable catalog versions; V1 сохранена.
- [x] JSON Schema + synthetic fixtures, >=12 разных граничных случаев.
- [x] CatalogImportValidator, broken references, UNKNOWN != zero, provenance fields.
- [x] Recurring/upfront helpers, FX/rounding, UTC/asOf и календарное продление.
- [x] Product-tier exclusivity, included agents, shared quota metadata.
- [x] Fresh PostgreSQL migrations + validate/no-op migrate; V1 upgrade в dev.
- [x] Repeat/concurrent import, snapshot immutability и failed-import rollback.
- [x] Demo/prod guard и отсутствие auto-seed; frontend/P01 regression.
- [x] Документация, whitespace/secret/file review.
- [x] Владелец предварительно принял отчёт P02 и поручил окончательное закрытие после проверок (см. §14).

Исторический статус до поручения о закрытии: **P02 = AWAITING_REVIEW**. **P03 = NOT_STARTED**. Ветка `codex/p01-scaffold`, HEAD `d895c04c76fe6d0da5dd96d1450dc66a35f343f3`. Изменения P02 не staged и не committed; push/PR не выполнялись. Приложение оставлено на 127.0.0.1:5173/18080, PostgreSQL на 127.0.0.1:15432. Остановка: `bash scripts/dev-down.sh`, без удаления volume.

В исходном отчёте commit был запрещён до приёмки; новое разрешение и окончательное сообщение коммита записаны в §14.

### Полный список файлов P02

```text
M  README.md
M  backend/pom.xml
M  backend/src/main/resources/application.yml
M  docs/ARCHITECTURE.md
M  docs/PROGRESS.md
M  scripts/check.sh
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogImportService.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogImportValidator.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogJson.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogManifest.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogPolicy.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogTypes.java
?? backend/src/main/java/com/aicompass/compass/catalog/OfferCosts.java
?? backend/src/main/java/com/aicompass/compass/shared/money/FxConversion.java
?? backend/src/main/java/com/aicompass/compass/shared/money/Money.java
?? backend/src/main/java/com/aicompass/compass/shared/time/RenewalSchedule.java
?? backend/src/main/java/com/aicompass/compass/shared/time/TimeConfiguration.java
?? backend/src/main/resources/db/migration/V2__create_versioned_catalog.sql
?? backend/src/main/resources/db/migration/V3__protect_catalog_snapshots.sql
?? backend/src/test/java/com/aicompass/compass/catalog/CatalogIT.java
?? backend/src/test/java/com/aicompass/compass/catalog/CatalogImportValidatorTest.java
?? backend/src/test/java/com/aicompass/compass/catalog/CatalogTestData.java
?? backend/src/test/java/com/aicompass/compass/shared/MoneyTest.java
?? backend/src/test/java/com/aicompass/compass/shared/RenewalScheduleTest.java
?? data/fixtures/README.md
?? data/fixtures/catalog-demo.json
?? data/schema/catalog.schema.json
?? scripts/check-catalog.mjs
?? scripts/check-catalog.sh
```


## 14. Закрытие P02 — 14.11.2026

Владелец предварительно принял P02 по отчёту, поручил короткую архитектурную проверку, повторные проверки, статус DONE и один локальный commit. DONE — явное поручение владельца, как при закрытии P01, без разрешения на следующий этап.

Результаты трёх групп архитектурных проверок:

1. **Версии / конкурентный импорт.** P02 выбирает опубликованный снимок только по явно переданному UUID (`readPublishedArtifact`), а не «последний по дате». Единственной активной версии пока нет; atomic activation прямо относится к P03. Разные versionId импортируются независимо, оба могут стать PUBLISHED; значения и строки не смешиваются благодаря составным ключам и отдельным транзакциям. Новый тест запускает разные версии одновременно и проверяет обе целиком; прежние тесты same-id retry/immutability сохранены.
2. **Неизвестность / доказательность.** CapabilityState и EvidenceStatus независимы. UNSUPPORTED с VERIFIED_SOURCE означает подтверждённое источником отсутствие; UNKNOWN/отсутствующая assertion — недостаток данных. CONDITIONAL/CONFLICTED/STALE не становятся VERIFIED автоматически; условия сохраняются. UNKNOWN evidence запрещён для SUPPORTED и UNSUPPORTED как приложением, так и БД. Добавлены отрицательные unit/DB проверки именно неподтверждённого отсутствия.
3. **Связи / модульность.** Одна модель доступна через планы нескольких продуктов. Один тариф включает несколько моделей и агентов через M:N; новый PostgreSQL-тест подтверждает 2 модели + 2 агента в одном плане, без новых price offers. Все 23 таблицы — каталог, внешних FK в identity/billing/portfolio нет; catalog зависит только от shared.money, обратной зависимости нет. Таблицы не превращены в сервисы. Производительность будущих API-запросов не заявляется.

Реальных ошибок в проверенных архитектурных пунктах не выявлено. Production-код, V1–V3 и зависимости при закрытии не менялись; структурный рефакторинг не выполнялся. Добавлены 2 integration methods, 1 отрицательный DB case и 1 unit case; обновлены архитектурные пояснения и статус документации. Выбор active version не реализовывался.

| Команда | Статус / exit | Фактический результат |
|---|---|---|
| `bash scripts/check.sh` | PASS / 0 | clean compile/package/verify: 62 unit + 44 PostgreSQL IT; npm ci, format/lint/typecheck/build PASS; 7 UI tests PASS; schema cases PASS |
| `bash scripts/check-catalog.sh` | PASS / 0 | 1 valid fixture + 12 invalid schema cases |
| `bash scripts/smoke.sh` | PASS / 0 | 3 проверки: direct readiness, proxy readiness, HTML; production-код совпадает с ранее запущенным P02 |
| `bash scripts/check-secrets.sh` | PASS / 0 | Шаблонов секретов/фактического DB-пароля в Git-кандидатах не найдено |
| `git diff --check` | PASS / 0 | Нет whitespace errors; новые файлы проверены отдельно |

Повторный прогон: **113 tests passed, 0 failures/errors, 0 skipped**; отдельно 13 schema cases и 3 smoke checks. Лог `.runtime/p02-close-check.log`, XML target/surefire-reports и target/failsafe-reports — локальные игнорируемые артефакты.

E2E/Playwright и persistence в этом закрытии повторно **NOT_RUN**: их прежние результаты сохранены в §13. Ограничения Arch/Ubuntu fallback и Docker Playwright image остаются. Remote CI и coverage percentage **NOT_RUN**, внешние интеграции/production не запускались. Все пять запрошенных проверок доступны и выполнены.

Состав коммита проверен: **29 файлов**, 6 изменённых tracked + 23 новых, полный список в §13. Нет .env, настоящих секретов, кешей, логов, target/dist, node_modules, symlinks и случайных файлов. AGENTS.md, CODEX_PROMPT.md, HTML reference и V1 совпадают с исходным HEAD. Индекс перед staging пустой; включаются только явно перечисленные пути P02.

Ветка: `codex/p01-scaffold`. Один локальный commit: `feat: implement versioned ai catalog data model`; итоговый hash и git status выводятся после выполнения в отчёте. Push/PR не выполняются. **P02 = DONE; P03 = NOT_STARTED.** После коммита остановиться.


## P03 — исторический промежуточный отчёт до разрешения зависимостей

Поручение владельца: P02 принят, P03 разрешён, frontend каталога включён явно; P04, commit до приёмки, push и PR запрещены. Полностью перечитаны AGENTS.md, CODEX_PROMPT.md, ARCHITECTURE.md и PROGRESS.md. Исходное дерево было чистым, ветка codex/p01-scaffold, HEAD 8fc33048e6c62286fa07f4f1d7d0aafd9790e0c6.

### Выполненная независимая часть

- V4 (V1–V3 не менялись): review по sealed hash, append-only publication audit, singleton active pointer; 26 таблиц вместо 23.
- CatalogPublicationService: FOR UPDATE, expected revision, атомарная смена, защита ABA, rollback только ранее активированного reviewed UUID. Импорт остаётся приватным до активации.
- CatalogReadService / CatalogController / CatalogViews / CatalogResource: read-only typed REST, нормализованные таблицы, version-pinned reads, REPEATABLE_READ, pagination/search/filter/sort, Problem Details, отдельные public evidence/source DTO и read-time freshness. Endpoint list и параметры — README.
- CatalogCommand + scripts/catalog.sh: validate/import/review/publish/rollback/current без HTTP listener. CatalogPolicy выполняется до CLI, web SecurityConfig остаётся с CSRF; default OFF режимы не изменены.
- Реальные manifest: ChatGPT, Claude, GitHub Copilot; 7 plans, 11 offers, Codex/Claude Code как included agents, 2 именованных релиза моделей, 8 official sources, 72 field-level evidence records на snapshot. Страницы открыты вручную web tool, OpenAI проверялся через OpenAI Docs. Нет paid calls, автоматического crawler, аккаунтов или вымышленных FX/benchmark.
- Известная US monthly base price Claude Pro: 20 USD; annual charge 200 USD с отдельными условиями. Другие USD list observations CONDITIONAL по региону. 3 KZ offers UNKNOWN. Все полные checkout totals UNKNOWN из-за неподтверждённых taxes/fees. Trial не придуман. Student eligibility требует provider verification. Copilot credits — один shared pool с условным flex.
- Ручная проверка выявила слишком широкий capability id у отрицательного утверждения про Codex Free. Создан r2 с отдельным codex-image-generation и regression test; r1 не редактировался после импорта. Проверенные даты источников не обновлялись задним числом.
- Локально импортированы/reviewed/published r1→r2. Активен 03000000-0000-4000-8000-000000000002, revision=2, canonical SHA-256 e7def5914eee6e3ebd66ec8ce0506c55a00434ffc75b427c7dc7fed84704c702. r1 сохранён, explicit-version read работает. Это локальная активация, не deploy и не приёмка P03 владельцем.

### Фактически выполненные проверки 14.11.2026

| Команда / проверка | Результат |
|---|---|
| `bash scripts/check.sh` (финальный прогон текущих изменений) | PASS, exit 0; backend **64 unit + 60 PostgreSQL IT**, failures/errors/skipped=0; frontend **7** прежних тестов shell; npm ci, format, lint, typecheck и build PASS |
| `bash scripts/check-catalog.sh` | PASS, exit 0; 1 synthetic + 2 reviewed manifests, 12 отрицательных schema cases |
| `bash scripts/catalog.sh validate --file=data/catalog/catalog-2026-11-14.json` | PASS, exit 0; Java semantic validator; r2 также валидирован при import и unit test |
| `bash scripts/dev-down.sh` → `bash scripts/dev-up.sh --no-build` | PASS, exit 0; только собственные процессы/контейнер, volume сохранён, PostgreSQL healthy, frontend/backend loopback |
| `bash scripts/smoke.sh` | PASS, exit 0; 3 readiness/proxy/HTML checks |
| `bash scripts/test-e2e.sh` | PASS, exit 0; **3 прежних scaffold E2E** (360/768/1440), не E2E каталога |
| Локальные GET API после r2 publication (Python urllib) | PASS, exit 0; **12** проверок: meta, 7 resources/counts/version, annual amount/upfront, KZ UNKNOWN, r1 history, scoped negative claim |
| `bash scripts/check-secrets.sh` | PASS, exit 0; limited pattern/local-password scan, не полный security audit |
| `bash -n scripts/catalog.sh`, `git diff --check` | PASS, exit 0 |

16 новых PostgreSQL publication/API IT проверяют empty state, private import/review, correct hash, switch/rollback/history, concurrency/ABA, injected pointer failure rollback, version reads, pagination/filter/sort, public resources и отсутствие внутренних полей, UNKNOWN/free/region, запрещённые POST/PUT/PATCH/DELETE, demo isolation, append-only history. Остальные PostgreSQL IT P01/P02 сохранены. Два reviewed manifest tests проверяют semantic validity, отсутствие вымышленных KZ totals, benchmarks/FX и уточнённый scope r2.

Логи и build artifacts только в ignored .runtime/backend/target/frontend/dist/test-results. .env, caches и node_modules не входят в Git. V1–V3 unchanged; изменений чужих файлов/ресурсов, новых коммитов, push и PR нет. Runtime оставлен доступным на 127.0.0.1:5173 / 18080 / 15432.

### Исторический блокер и незавершённые на тот момент критерии

AGENTS.md требует explicit permission для project dependency installation/downloads. Отправлен отдельный запрос на необходимые P03 dependencies; ответа на момент отчёта нет. Ничего нового из этого списка не установлено. Метаданные официальных npm/Maven Central и документация проверены:

| Предлагаемая зависимость | Версия / результат read-only проверки |
|---|---|
| springdoc-openapi-starter-webmvc-api | 3.2.0; POM существует в Maven Central, [официальная документация](https://springdoc.org/) |
| react-router | 8.5.1; registry engines Node >=22.22.0, peers React >=19.2.7 совместимы с установленными Node24/React19.3 |
| @tanstack/react-query | 5.107.1; registry peers React18/19 |
| @hey-api/openapi-ts | 0.99.0; registry Node >=22.18.0 и TypeScript >=6 поддержаны |

Это проверка метаданных, **не** доказательство успешной сборки с библиотеками. openapi-typescript 7.13.0 не выбран: peer TypeScript ^5.x не соответствует TS6; обход peer checks не выполнялся.

**NOT_RUN / не реализовано:** OpenAPI generation/contract check, generated TS DTO, React Router/TanStack Query integration, frontend catalog search/filter/detail/states/accessibility tests, isolated import→publish→API→UI→new version→rollback E2E. Старые UI/E2E успехи этого не доказывают. Эти части требуют продолжения P03 после ответа на permission request; P03 нельзя выдавать за завершённый или AWAITING_REVIEW.

Сохраняются ограничения: Playwright на Arch использует fallback Ubuntu24.04 browser build и предупреждает об отсутствии официальной поддержки; remote CI не запускался. Production DB role separation и authenticated admin отсутствуют (вне текущей локальной границы). Catalog coverage ограничен 3 продуктами; country eligibility, tax/fees, часть функций/лимитов UNKNOWN. Public detail пока фильтрует связанные таблицы snapshot в памяти; performance production-каталога не измерена.

**Статус: P03 BLOCKED, P04 NOT_STARTED.** Следующий шаг: разрешение перечисленных project-local зависимостей, затем завершение и полные проверки P03; после этого AWAITING_REVIEW без commit.


Продолжение P03: владелец явно разрешил springdoc 3.2.0, react-router 8.5.1, @tanstack/react-query 5.107.1, @hey-api/openapi-ts 0.99.0 и их необходимые транзитивные зависимости; без других direct dependencies/sudo/global changes. Предыдущий dependency blocker снят.


## P03 — исторический отчёт перед финальной приёмочной проверкой, 14.11.2026

**P03 = AWAITING_REVIEW. P04 = NOT_STARTED.** Статусы IN_PROGRESS/BLOCKED в предыдущем промежуточном отчёте исторические: разрешение зависимостей получено, перечисленные там незавершённые части реализованы и проверены. Самостоятельная приёмка/commit не выполнялись.

### Изменения и установленные версии

- Установлены ровно разрешённые новые direct dependencies: `springdoc-openapi-starter-webmvc-api:3.2.0`, `react-router:8.5.1`, `@tanstack/react-query:5.107.1`, dev `@hey-api/openapi-ts:0.99.0`. Frontend manifest/lockfile содержат точные версии; Maven dependency также закреплена. Не добавлялись другие прямые зависимости.
- До установки сверены официальные registry/POM, Node/React/TypeScript peers и springdoc Boot 4 compatibility. Подтверждено реальной сборкой с Boot 4.1.2/Java 21.0.13+9, React 19.3.1, TS 6.0.3, Vite 8.3.8, Node 24.22.3/npm 11.19.1. Глобальное окружение не менялось.
- Springdoc получает OpenAPI 3.0.1 из реальных контроллеров по `GET /api/v1/openapi`: 21 публичный GET catalog path, конкретные 200 DTO, Problem Details 400/404, явные required/nullable. Money остаётся string|null; UNKNOWN не становится FREE. Internal publication/review/manifest/actor не включены в публичную схему.
- Сохранена проверенная схема `docs/openapi.json`, сгенерированы TS DTO/SDK и bundled fetch client. `generate-api.sh` получает схему от настоящего тестового backend с PostgreSQL; `check-api.sh` сравнивает её и повторно сгенерированный клиент. Ручных frontend DTO-копий нет.
- React Router + TanStack Query: реальный API, список/детальная карточка, поиск, фильтры по типу и возможности, сортировка, пагинация, URL-параметры, отдельные модели/агенты, версия snapshot в запросах и ссылках. Тарифы/регион/валюта/upfront/annual equivalent, налоги/сборы, лимиты/shared pools/eligibility, evidence/source/date/UNKNOWN/STALE/CONDITIONAL. Loading/empty/no-results/error/retry. Адаптивность и keyboard skip link.
- Meta дополнен справочниками/счётчиками охвата из одного snapshot. Исправлено пропущенное соответствие capability-фильтра assertions тарифов; существующий API regression test дополнен проверкой. Не добавлялись подбор, ranking, анкета, account/admin HTTP или функции P04.
- E2E запускает собственный PostgreSQL в tmpfs и приложение на случайных loopback-портах, прогоняет настоящий CLI import/review/publish и API/UI. Production/dev каталог не изменяется; постоянные volumes не создаются и не удаляются. CI workflow подготовлен для отдельной загрузки закреплённого Chromium; удалённо не запускался.
- README/ARCHITECTURE/DATA_POLICY описывают запуск, воспроизводимую генерацию, публикацию/rollback, источники, ограничения, права использования benchmark data и реальные тесты.

### Фактические проверки финальной реализации

| Команда / проверка | Статус / exit | Фактический результат |
|---|---|---|
| `bash scripts/check.sh` | PASS / 0 | clean Maven compile/package/verify: **64 unit + 61 PostgreSQL IT**, 0 failures/errors/skipped; `npm ci` 279 packages, OpenAPI/client drift check, format/lint/typecheck, **18 UI tests**, frontend build и schema checks PASS |
| `bash scripts/check-catalog.sh` | PASS / 0 | 1 synthetic + 2 reviewed manifests, 12 negative shape cases |
| `bash scripts/smoke.sh` | PASS / 0 | 3 проверки после перезапуска на финальном JAR: direct readiness, proxy readiness, frontend HTML |
| `bash scripts/test-e2e.sh` | PASS / 0 | **11 E2E** (8 catalog + 3 scaffold), 0 failures/skipped, 31.2 s; реальные CLI/backend/PostgreSQL/browser |
| `bash scripts/check-secrets.sh` | PASS / 0 | Нет совпадений секретных шаблонов/локального DB-пароля в Git-кандидатах; ограниченный scan, не полный security audit |
| `git diff --check` | PASS / 0 | Нет whitespace errors; новые файлы дополнительно проверены отдельно |
| `bash scripts/generate-api.sh` | PASS / 0 | Настоящий backend: 5 unit + 7 ScaffoldIT, экспорт schema и генерация клиента; этот отдельный повтор не прибавляется к итоговому числу тестов |
| `bash scripts/check-api.sh` | PASS / 0 | Backend schema совпадает с docs; повторная генерация не меняет ни один generated файл |
| `bash scripts/dev-down.sh` → `bash scripts/dev-up.sh --no-build` | PASS / 0 | Только процессы AI Compass, PostgreSQL healthy, volume сохранён, всё на 127.0.0.1 |
| Дополнительная проверка живого каталога через Node/Playwright | PASS / 0 | Сохранён r2/revision=2, 3 продукта; 21 GET в OpenAPI; реальные monthly/annual суммы Claude разделены; KZ показывает UNKNOWN и не показывает US price; 360 px без overflow/page errors |
| `bash -n` для новых shell scripts, `python3 -m py_compile scripts/e2e.py` | PASS / 0 | Синтаксис локальных скриптов |
| Проверка frontend production bundle | PASS / 0 | Synthetic test fixture не попал в скомпилированный JS |

Итог уникальных тестов финального набора: **154 passed = 64 unit + 61 integration + 18 UI + 11 E2E; 0 failures/errors, 0 skipped**. Отдельно 15 schema cases и 3 smoke checks. XML backend подтверждают 64/61; логи `.runtime/p03-final-check.log`, `.runtime/p03-ui.log`, `.runtime/p03-e2e.log` и отчёты target/test-results игнорируются Git.

В процессе реализации были реальные неуспешные прогоны; они не скрыты: первоначальная OpenAPI-аннотация не давала конкретную application/json 200 schema (исправлена, добавлена проверка всех GET), TypeScript потребовал явного union типа списков, два UI expectations неверно учитывали вложенный текст даты и страну synthetic KZT offer (скорректированы по неизменённому fixture), генерация Prettier отличалась завершающей запятой generic без filepath (исправлен единый путь форматирования). После исправлений все обязательные проверки выше успешны; версии зависимостей для обхода ошибок не менялись.

### Проверенные сценарии и безопасность

- E2E: import и review не делают snapshot публичным; публикация открывает его API и UI. Новая версия меняет UI/цену, старые ссылки читаются воспроизводимо, rollback переключает pointer с новой ревизией.
- PostgreSQL IT: конкуренция/ABA, атомарность event+pointer при сбое, immutability, запрет чужой/неопубликованной версии, nullable money, UNKNOWN/FREE, freshness и demo guard. V1–V3 не менялись.
- Публичные POST/PUT/PATCH/DELETE запрещены; import/review/publish/rollback не имеют HTTP handlers. E2E проверяет POST catalog/admin → 403 и GET псевдоопераций → 404; OpenAPI содержит только GET. CSRF не отключался. CLI actor — аудиторская метка локального доверенного оператора, не пользовательская аутентификация.
- Локальный reviewed r2 остался активным после stop/start, revision=2. E2E не изменяет его. Нет платных AI API, реальных платежей/писем, source-fetch, deploy, push/PR/commit. Режимы OFF/OFF/LOCAL/false сохранены.
- Self-review: nullable DTO и generated types, отсутствие static production catalog, отсутствие float для цены, независимость валюты/страны, upfront/UNKNOWN, sources/условия, security diff, только необходимые четыре direct dependencies, состав Git и нетронутые V1–V3. Визуально просмотрены catalog 360/1440 и реальный Claude detail 360; автоматические проверки также прошли на 768.

### Критерии P03

| Критерий | Статус / доказательство |
|---|---|
| Минимальный перечень и честное рыночное покрытие | DONE: DATA_POLICY, 3 продукта / 7 планов / 11 предложений, ограничения видны в UI |
| Реальный reviewed manifest, цена/функции/лимиты/регион проверяются отдельно | DONE: r1/r2, 8 official sources, 72 field-level claims; семантическая/schema проверка |
| Model / product / agent / plan / regional offer разделены | DONE: БД/API/отдельные UI views; reference-only неизвестность сохраняется |
| CLI import/review/publish, atomic activation, audit, update/rollback/history | DONE: реальные PostgreSQL IT + сквозной E2E |
| Публичные list/detail/meta, фильтры, поиск, пагинация, стабильные ошибки | DONE: backend API tests + UI/E2E |
| Источники/даты/условия/UNKNOWN/STALE; FREE отдельно от неизвестного | DONE: evidence DTO/UI, freshness IT, UI/E2E денежных/региональных сценариев |
| OpenAPI из endpoints, generated client/DTO и воспроизводимость | DONE: 21 GET path, nullable contract test, generate/check-api |
| Frontend работает с backend; карточки, модели/агенты, планы/регионы | DONE: React Router/Query + настоящие API/браузерные проверки |
| Loading/empty/error, адаптивность, базовая клавиатурная доступность | DONE: 18 UI + 11 E2E, 360/768/1440 |
| Закрытые внутренние операции и внешние интеграции OFF | DONE: HTTP deny tests, CLI only, конфигурация без внешних адаптеров |
| DATA_POLICY и инструкции добавления/публикации продукта | DONE: README, ARCHITECTURE, DATA_POLICY |
| Все шесть обязательных команд | DONE: таблица фактических результатов выше |
| Приёмка владельцем, 5 выбранных утверждений | AWAITING_REVIEW: выполняется владельцем после этого отчёта; не заявлена как уже пройденная |

Невыполненных обязательных **критериев реализации P03** не осталось. Приёмка владельцем остаётся открытой; market-ready/R1 или production-ready не заявляются.

### Ограничения / NOT_RUN

- Playwright на Arch остаётся официально неподдерживаемым Ubuntu24.04 fallback; прежний Docker Playwright image не проверен. Удалённый CI **NOT_RUN**, workflow только подготовлен, push запрещён.
- Нет полного рынка: 3 продукта, неизвестны country eligibility, KZ цены, налоги/сборы и часть функций. Реальные checkout totals UNKNOWN. Статус verified source означает чтение заявления провайдера, не независимую оценку качества. Цены меняются; read-time STALE не выполняет автоматическую перепроверку.
- Полный нагрузочный/security/accessibility аудит, проценты code/branch coverage и production DB role separation **NOT_RUN / вне P03**. Детальная карточка пока фильтрует связанные таблицы snapshot в памяти; производительность большого каталога не измерена.
- Authenticated admin UI, crawler, фоновые уведомления, Recommendation Engine и анкета не реализованы по границам этапов. Платные/external/live integration checks **NOT_RUN**, запрещены. Отдельный `test-persistence.sh` в этом продолжении не повторялся: сохранность active catalog проверена реальным dev stop/start, прежний полный persistence результат сохранён в P01/P02.

### Git и состав изменений

Ветка **codex/p01-scaffold**, HEAD **8fc33048e6c62286fa07f4f1d7d0aafd9790e0c6** (P02). Индекс пустой. Новых commits, staging, push/PR нет. Все изменения — P03. AGENTS.md, CODEX_PROMPT.md, reference HTML, V1–V3 и инфраструктура dev Compose не изменены. .env, caches, logs, target/dist, node_modules, screenshots, test-results и __pycache__ игнорируются и не входят в список Git-кандидатов.

Итого `git status --short --untracked-files=all`: **21 modified + 47 untracked = 68 файлов**, staged = 0. После E2E контейнеров с `com.aicompass.scope=p03-e2e` не осталось; dev PostgreSQL healthy на 127.0.0.1:15432.

Полный текущий список изменённых и новых файлов (M — изменённый, ?? — новый; generated client включён намеренно):

```text
 M .github/workflows/ci.yml
 M .gitignore
 M README.md
 M backend/pom.xml
 M backend/src/main/java/com/aicompass/compass/CompassApplication.java
 M backend/src/main/java/com/aicompass/compass/catalog/CatalogManifest.java
 M backend/src/main/java/com/aicompass/compass/catalog/CatalogPolicy.java
 M backend/src/main/java/com/aicompass/compass/config/SecurityConfig.java
 M backend/src/main/resources/application.yml
 M backend/src/test/java/com/aicompass/compass/ScaffoldIT.java
 M backend/src/test/java/com/aicompass/compass/catalog/CatalogIT.java
 M docs/ARCHITECTURE.md
 M docs/PROGRESS.md
 M frontend/eslint.config.js
 M frontend/package-lock.json
 M frontend/package.json
 M frontend/playwright.config.ts
 M frontend/src/app/App.tsx
 M scripts/check-catalog.mjs
 M scripts/check.sh
 M scripts/test-e2e.sh
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogCommand.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogController.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogPublicationService.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogReadService.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogResource.java
?? backend/src/main/java/com/aicompass/compass/catalog/CatalogViews.java
?? backend/src/main/java/com/aicompass/compass/config/OpenApiConfiguration.java
?? backend/src/main/java/com/aicompass/compass/shared/web/ApiProblem.java
?? backend/src/main/resources/db/migration/V4__catalog_publication.sql
?? backend/src/test/java/com/aicompass/compass/catalog/CatalogPublicationIT.java
?? backend/src/test/java/com/aicompass/compass/catalog/ReviewedManifestTest.java
?? data/catalog/README.md
?? data/catalog/catalog-2026-11-14-r2.json
?? data/catalog/catalog-2026-11-14.json
?? docs/DATA_POLICY.md
?? docs/openapi.json
?? frontend/openapi-ts.config.ts
?? frontend/scripts/generate-client.mjs
?? frontend/src/api/catalog.ts
?? frontend/src/api/generated/client.gen.ts
?? frontend/src/api/generated/client/client.gen.ts
?? frontend/src/api/generated/client/index.ts
?? frontend/src/api/generated/client/types.gen.ts
?? frontend/src/api/generated/client/utils.gen.ts
?? frontend/src/api/generated/core/auth.gen.ts
?? frontend/src/api/generated/core/bodySerializer.gen.ts
?? frontend/src/api/generated/core/params.gen.ts
?? frontend/src/api/generated/core/pathSerializer.gen.ts
?? frontend/src/api/generated/core/queryKeySerializer.gen.ts
?? frontend/src/api/generated/core/serverSentEvents.gen.ts
?? frontend/src/api/generated/core/types.gen.ts
?? frontend/src/api/generated/core/utils.gen.ts
?? frontend/src/api/generated/index.ts
?? frontend/src/api/generated/sdk.gen.ts
?? frontend/src/api/generated/types.gen.ts
?? frontend/src/app/Home.tsx
?? frontend/src/catalog/Catalog.module.css
?? frontend/src/catalog/Catalog.test.tsx
?? frontend/src/catalog/CatalogLayout.tsx
?? frontend/src/catalog/CatalogList.tsx
?? frontend/src/catalog/ProductDetail.tsx
?? frontend/src/catalog/test-data.ts
?? frontend/tests/e2e/catalog.spec.ts
?? scripts/catalog.sh
?? scripts/check-api.sh
?? scripts/e2e.py
?? scripts/generate-api.sh
```

Следующий шаг — приёмка P03 владельцем. **Остановиться. Commit, push, PR и P04 запрещены без нового разрешения.**


## Финальная приёмочная проверка и закрытие P03 — 14.11.2026

Владелец отдельным поручением делегировал выбор/проверку 5 утверждений по реальным первоисточникам, повторные checks и один локальный commit при выполнении критериев. **P03 = DONE** установлен по этому прямому поручению, как исключение из обычного AWAITING_REVIEW; самостоятельного разрешения на следующий этап нет. **P04 = NOT_STARTED.** Предыдущие запреты commit и ожидание проверки владельцем в историческом отчёте заменены этим поручением только для закрытия P03.

Повторно прочитаны AGENTS.md, общие инварианты/правила, разделы источников, тестов и P03 в CODEX_PROMPT.md, текущий статус/результаты PROGRESS.md. Вложенных/родительских AGENTS.md не найдено. Исходные 68 dirty files совпали с зафиксированным списком P03; ветка codex/p01-scaffold, индекс пустой, HEAD до закрытия 8fc33048e6c62286fa07f4f1d7d0aafd9790e0c6.

### Проверка реальных источников

Проверен активный API: UUID `03000000-0000-4000-8000-000000000002`, revision **2**. Пять evidence records получены из опубликованного `/api/v1/catalog/evidence`, их текст/статус/даты/условия совпадают с r2 manifest. Выборка охватывает цену, включённого агента, eligibility, переменный shared quota и подтверждённое отсутствие конкретной возможности — это не повторная проверка всех 72 claims.

Источники открыты и прочитаны вручную через web tool **14.11.2026**; завершение сверки **17:25:41 UTC**. Для OpenAI применён OpenAI Docs с поиском и чтением официальной страницы. Ни login, ни paid API, ни scraping job не запускались.

| Evidence ID / продукт, тариф или агент | Проверенное утверждение | Первоисточник | Дата повторной проверки | Итоговый статус |
|---|---|---|---|---|
| `claude-pro-us-month-price` / Claude Pro | Базовая месячная цена в US — 20 USD; это не подтверждённый итог checkout и не цена KZ | [Claude: What is the Pro plan?](https://support.claude.com/en/articles/8325606-what-is-the-pro-plan), раздел стоимости | 2026-11-14 | VERIFIED_SOURCE, без изменения |
| `claude-pro-claude-code-access` / Claude Code в Pro | Доступ включён в подписку Pro; отдельная покупка агента не нужна, лимиты не удваиваются | [Pro benefits](https://support.claude.com/en/articles/8325606-what-is-the-pro-plan); дополнительно [Claude Code with Pro](https://support.claude.com/en/articles/11145838-use-claude-code-with-your-pro-max-or-ultra-plan), единая подписка и общие usage limits | 2026-11-14 | VERIFIED_SOURCE, без изменения; API key billing отдельно |
| `copilot-student-eligibility` / GitHub Copilot Student | Нужна верификация студенческого статуса GitHub, самоописания недостаточно | [Copilot plans](https://docs.github.com/en/copilot/get-started/plans); дополнительно [Student access](https://docs.github.com/en/copilot/how-tos/copilot-on-github/set-up-copilot/enable-copilot/set-up-for-students) | 2026-11-14 | VERIFIED_SOURCE для требования; entitlement пользователя не подтверждался |
| `copilot-pro-credit-allowance` / GitHub Copilot Pro | 1 000 base + 500 flex = 1 500 общих AI credits в месяц; flex меняется, переноса нет, сброс первого числа UTC | [GitHub usage billing](https://docs.github.com/en/copilot/concepts/billing-and-usage/individuals/billing), allowance table и reset rules | 2026-11-14 | CONDITIONAL сохранён: переменный flex нельзя обещать как постоянный лимит |
| `chatgpt-free-image-generation-evidence` / Codex в Free | В Codex Free генерация изображений недоступна; это не утверждение о ChatGPT Chat | [OpenAI pricing](https://learn.chatgpt.com/docs/pricing), FAQ image-generation usage | 2026-11-14 | VERIFIED_SOURCE; capability `codex-image-generation` = UNSUPPORTED |

Результат: **5/5 утверждений подтверждены в записанных границах; 0 неподтверждённых/устаревших в этой выборке**. По факту чтения страницы не гарантируются региональная покупка, качество продукта или пользовательская eligibility. Правки статусов, новые данные и регрессионные тесты не потребовались. Неизменяемые r1/r2, их исходный `verifiedAt=2026-11-14T12:58:14Z` и история публикации не переписывались; дата этой повторной проверки зафиксирована отдельно здесь. Новый snapshot ради изменения даты не создавался.

### Повтор обязательных проверок

| Команда | Статус / exit | Фактический результат именно этого прогона |
|---|---|---|
| `bash scripts/check.sh` | PASS / 0 | clean compile/package/verify: **64 unit + 61 PostgreSQL integration**, 0 failures/errors/skipped; npm ci, OpenAPI/client reproducibility, format/lint/typecheck/build; **18 UI tests PASS** |
| `bash scripts/check-catalog.sh` | PASS / 0 | 3 valid manifests (1 synthetic, 2 reviewed), 12 negative schema cases |
| `bash scripts/smoke.sh` | PASS / 0 | 3 проверки: backend readiness, frontend proxy readiness, HTML |
| `bash scripts/test-e2e.sh` | PASS / 0 | **11 passed**, 0 failed/skipped, 35.0 s; import/review/publication → API → UI, update/rollback, UNKNOWN/FREE/regions, public mutation denial, 360/768/1440 |
| `bash scripts/check-secrets.sh` | PASS / 0 | Нет шаблонных секретов/фактического локального пароля в Git-кандидатах |
| `git diff --check` | PASS / 0 | Whitespace ошибок нет; новые файлы также проверены отдельно |

**154 tests passed, 0 failed/errors, 0 skipped**; отдельно 15 schema cases и 3 smoke checks. Backend XML пересчитаны после текущего verify. Логи `.runtime/p03-accept-check.log` и `.runtime/p03-accept-e2e.log` игнорируются Git. Скриншоты не заменяют backend/денежные проверки. Все обязательные критерии P03 выполнены, блокеров закрытия нет.

Сохранены ограничения: Playwright на Arch использует Ubuntu fallback, официальной поддержки Arch нет; удалённый CI **NOT_RUN**, только локальные проверки. Полный market coverage, production/load/accessibility/security audit и coverage percentages не заявлены. Каталог по-прежнему ограничен 3 продуктами; KZ prices и checkout totals UNKNOWN. P04/engine, live integrations, реальные платежи/письма, deploy **NOT_RUN / запрещены**. Эти ограничения не скрыты и не сняты при закрытии.

### Финальный Git review

Состав P03: **68 файлов — 21 изменённый tracked и 47 новых**, полный перечень выше. Повторный review подтвердил только файлы этапа: backend/API/V4, источники, frontend/generated client, тесты/скрипты/CI и документация. При этом закрытии изменены только README, ARCHITECTURE и PROGRESS; код и manifest исправлений не потребовали.

Не включены .env, секреты, caches, logs, node_modules, target/dist, screenshots/test-results, __pycache__ и локальные tools. Нет symlinks. AGENTS.md, CODEX_PROMPT.md, HTML reference, V1–V3 и dev Compose не изменены. Индекс до staging был пустой; staging выполняется по проверенному явному списку этих 68 путей.

Разрешён один локальный commit: **`feat: implement verified ai catalog and public api`** в `codex/p01-scaffold`; фактический hash и итоговый git status проверяются после создания и выводятся в отчёте. Push, PR, новые ветки и P04 не выполняются. После коммита остановиться.

## P04 — Recommendation Engine, 14.11.2026 (исторический отчёт перед закрытием)

**Основание:** владелец явно принял P03 и разрешил P04 в приложенном поручении. Ветка сохранена `codex/p01-scaffold`; исходный HEAD `9f3e48f68d41ae40f32281f553a32349de4b4429`. Коммит, push, PR, P05 и новые зависимости не разрешены и не выполнялись. Существующие версии manifests/lockfile не менялись. После обязательных проверок итоговый статус приведён в конце этого раздела.

### Реализация и ручное ревью

Чистое Java-ядро отделено от HTTP/DB: validation → one active immutable snapshot → eligibility → BigDecimal costs → capability coverage → bounded combinations/shared quota allocation → три стратегии → причины/условные альтернативы. Controller делегирует service; snapshot снимается в REPEATABLE READ. Public profile/DTO/SDK code-first; endpoints POST preview и GET CSRF. Сохранения профилей, LLM и новых таблиц нет. Подробные правила входа/ранжирования/денег/лимитов зафиксированы в ARCHITECTURE.md, запуск и запрос — в README.md.

При ручном ревью исправлены и покрыты регрессиями: валюта Compass fee и отсутствие FX; исходный календарный anchor после февраля; окончание offer раньше billing horizon; квота без подтверждённой функции; явный searchComplete=false при превышении лимита распределения квот; nullable/ошибочные типы JSON, включая null внутри языкового списка; CSRF query parameter и неверные error response schemas в автоматически полученной OpenAPI. Ручной diff review проверяет регион/налог/FX/UNKNOWN, retained costs, отсутствие paid/brand fields в objective, product-scoped conflicts, публичность internal writes и отсутствие изменения рыночных данных. Старый алгоритм HTML не использован.

До финального прогона были промежуточные ошибки: неверный порядок аргументов activate в новом IT, неправильное имя State accessor при уточнении usage, неправильное имя exactVersion в тестовой мутации. Они исправлены; промежуточные неуспешные команды не считаются PASS. Проверки не отключались и ожидаемый победитель не менялся ради зелёного результата.

### Матрица сценариев плана

| Проверки | Доказательство / граница |
|---|---|
| T01 multi-needs | studentAndEditorHaveDifferentBundlesAndFreeWins: study/coding/pptx присутствуют в coverage; реальные HTTP/E2E |
| T02, T03 free/unknown/trial | Бесплатный learner+slides побеждает; unknownPriceAndTrialNeverBecomeFreeRecurring; seeded budgets включают 0 |
| T04 student | SELF_DECLARED и USER_REPORTED_PROVIDER_VERIFIED остаются CONDITIONAL; NONE — INELIGIBLE |
| T05 annual | annualRequiresOptInAndEnoughUpfront: 120/год не помещается в upfront=30; при 120 проходит с recurring=10 |
| T06 mustKeep | mustKeepCountsCostAndCoverageExactlyOnce: retained 12 при budget=10 даёт конфликт, сумма не теряется |
| T07, T08 agent/vendor | Included agent не отдельная покупка, разные продукты одного provider разрешены |
| T09 conflicts | mandatoryCountAndTwoProductTiersCannotBeSilentlyDropped + uniqueness invariant |
| T10 platform | Обязательные Linux/web подтверждаются отдельно; creator без Linux исключается |
| T11, T12 credits | One-time reset NEVER не доказывает recurring usage; 5 image ×10 +2 video ×25=100, третий video превышает общий pool |
| T13 tax/FX | Неизвестный налог/прямой FX не даёт confirmed fit; fresh USD→KZT только из synthetic FX; currency fee также конвертируется |
| T14, T15 language/privacy | outputLanguage отдельно от interface; неизвестный Japanese/LOCAL_ONLY не становится ELIGIBLE |
| T16 stale | STALE/CONFLICTED/UNKNOWN support не превращается в UNSUPPORTED или подтверждённого победителя |
| T17 unmet | Нет опубликованного каталога/функции/совместимого набора; PARTIAL нужен explicit accepted evidence |
| T18 paid bias | Бесплатный набор выигрывает; optionalFeaturesSeparateStrategiesWithoutPaidBonus проверяет раздельные стратегии |
| T19 dedup | Три совпавшие стратегии → один bundle с тремя labels и NO_DISTINCT_ALTERNATIVE |
| T20 model version | Смена exactVersion не переносит benchmark quality: engine вообще не принимает benchmark observations, UNKNOWN_QUALITY сохраняется |
| T21 API vs app | apiOnlyPricesCannotSubstituteForApplicationSubscriptions |
| T22 replay | Полное равенство результата pure engine для input/snapshot/asOf/policy. Public preview намеренно не принимает старый UUID, только active snapshot |
| T23 concurrent catalog | Реальная PostgreSQL: 6 переключений active pointer +16 snapshot reads, цена 15 или 18 строго по UUID; rollback восстанавливает 15 |
| T24 affiliate | Affiliate flag в текущей schema отсутствует: переключение флага NOT_APPLICABLE. Структурный контроль objective + тест изменения brand/provider names: ranking не зависит от бренда/affiliate текста |
| T25 Compass fee | Fee=4.99 USD при retained 12 → total16.99 и net savings=-4.99; missing FX не даёт fee=0 |
| T26 annual cancel | Уже оплаченная годовая подписка не создаёт refund; только CANCEL_AT_RENEWAL и будущая регулярная разница |
| T33 maxTools | Два locked при maxTools=1 → RETAINED_CONFLICT; ни один не удаляется молча |
| T34 limited catalog | LIMITED_CATALOG всегда; integration с реальным r2: 3 продукта, video UNKNOWN/no-match, не иллюзия полного рынка |
| T35 notifications | NOT_APPLICABLE: monitoring/change notifications относятся к P08; P04 их не создаёт. Preview не добавляет publication events |
| T36 LLM OFF | Все unit/HTTP/E2E выполняются без LLM/API adapter; factual и policy explanations возвращаются локальным кодом |

Дополнительно: 160 сгенерированных профилей seed 4042026 с бюджетом, upfront, annual и maxTools проверяют hard requirements, детерминированность, уникальность и отсутствие бесплатного student baseline; строгий JSON/enum/duplicate/currency/size validation; 40-candidate cap и 10 000 quota-allocation cap не скрываются; CSRF/429/413, no-store, private import/publish/profile endpoints. Большая fixture проверяет полный перебор до четырёх инструментов.

### Критерии P04

| Критерий | Реализация и проверка |
|---|---|
| Profile validation и taxonomy | Typed v1, веса/required/интенсивность; taxonomy одного snapshot, неизвестный need явно UNKNOWN |
| Нормализация offers/horizon/existing | Money, tax/fee, calendar anchor, FX snapshot, mustKeep/AUGMENT/REVIEW |
| ELIGIBLE/CONDITIONAL/UNKNOWN/INELIGIBLE | Раздельные assessments и conditionalAlternatives; регион/канал/язык/platform/privacy/student |
| Hard requirements/maxTools/exclusivity/shared pools | Unit, 160 generated cases, отдельная квотная regression; отсутствуют vendor-wide bans |
| Три стратегии без LLM и bias | ECONOMY/BALANCED/EXPANDED, 0.05 policy tolerance, stable tie-breaking/dedup |
| Typed costs/upfront/recurring/renewal/confidence | Nullable decimal strings, Item.nextRenewal, usageFit, negative net savings при fee |
| Reason codes/unmet/evidence/alternatives | Публичные DTO с factual/policy reasons, источниками/conditions, limited-market предупреждениями |
| Preview, rate limit, version metadata | Real HTTP + OpenAPI/generated SDK + E2E; без persistence и public import |
| Производительность/детерминированность | Отдельный benchmark 40×4; показатели и итог ниже |
| Студент и видеомонтажёр отличаются | Синтетические HTTP/E2E: free learner+slides против creator15; реальные рыночные данные не подменены |

### Сохранённые ограничения

Каталог real r2: только ChatGPT/Claude/GitHub Copilot; taxonomy содержит development/research, image/video потребности не покрыты, KZ checkout totals и часть ограничений неизвестны. NO_MATCH здесь честен. Новых рыночных утверждений и source fetching в P04 нет. Фиктивные тарифы существуют только в DEMO fixture и изолированных тестах.

Нет сопоставимого независимого quality ranking; coverage — функции, не процент качества. Частота без количества не доказывает достаточный лимит. Поддержка usage ограничена mode-qualified monthly actions; daily allowances, split одной потребности между пулами, неизвестные top-up prices не выдумываются. Горизонт — проекция текущих условий, не гарантия будущей цены. Fee в публичной policy равен 0 USD; платный вариант проверяется только синтетическим unit test, не платежом.

Rate limiter — один процесс и remote IP; same-origin proxy может объединять клиентов. Production capacity/security/accessibility audit, профили/анкета P05, аккаунты, monitoring и payment flow не реализуются в P04. Playwright на Arch по-прежнему использует Ubuntu fallback; remote CI **NOT_RUN**. Проценты line/branch coverage **NOT_RUN**: инструмент не настроен, новый dependency не устанавливался в соответствии с запретом владельца; 85%/70% не заявляются, сценарная матрица не подменяется процентами. Отдельное включение инструмента покрытия требует согласования. Production deploy и платные/внешние интеграции **NOT_RUN / запрещены**.

### Изменённые файлы P04

39 изменённых/новых файлов на момент отчёта; manifests/lockfile и исходные рыночные manifests не изменены. Изменения не staged, commit запрещён. Полный список:

```text
README.md
backend/src/main/java/com/aicompass/compass/catalog/CatalogReadService.java
backend/src/main/java/com/aicompass/compass/catalog/PublishedCatalogSnapshot.java
backend/src/main/java/com/aicompass/compass/config/OpenApiConfiguration.java
backend/src/main/java/com/aicompass/compass/config/SecurityConfig.java
backend/src/main/java/com/aicompass/compass/recommendation/BundleSearch.java
backend/src/main/java/com/aicompass/compass/recommendation/Candidate.java
backend/src/main/java/com/aicompass/compass/recommendation/CandidateResolver.java
backend/src/main/java/com/aicompass/compass/recommendation/CapabilityCoverage.java
backend/src/main/java/com/aicompass/compass/recommendation/CostCalculator.java
backend/src/main/java/com/aicompass/compass/recommendation/EligibilityAssessment.java
backend/src/main/java/com/aicompass/compass/recommendation/EvidenceAssessment.java
backend/src/main/java/com/aicompass/compass/recommendation/PreviewLoadGuard.java
backend/src/main/java/com/aicompass/compass/recommendation/ProfileValidator.java
backend/src/main/java/com/aicompass/compass/recommendation/RecommendationController.java
backend/src/main/java/com/aicompass/compass/recommendation/RecommendationEngine.java
backend/src/main/java/com/aicompass/compass/recommendation/RecommendationProfile.java
backend/src/main/java/com/aicompass/compass/recommendation/RecommendationResult.java
backend/src/main/java/com/aicompass/compass/recommendation/RecommendationService.java
backend/src/main/java/com/aicompass/compass/recommendation/UsageAssessment.java
backend/src/main/resources/application.yml
backend/src/test/java/com/aicompass/compass/ScaffoldIT.java
backend/src/test/java/com/aicompass/compass/recommendation/EngineFixtures.java
backend/src/test/java/com/aicompass/compass/recommendation/ProfileAndInvariantTest.java
backend/src/test/java/com/aicompass/compass/recommendation/RecommendationBenchmark.java
backend/src/test/java/com/aicompass/compass/recommendation/RecommendationEngineTest.java
backend/src/test/java/com/aicompass/compass/recommendation/RecommendationIT.java
data/fixtures/profiles/editor.json
data/fixtures/profiles/student.json
data/fixtures/recommendation-demo.json
docs/ARCHITECTURE.md
docs/PROGRESS.md
docs/openapi.json
frontend/src/api/generated/index.ts
frontend/src/api/generated/sdk.gen.ts
frontend/src/api/generated/types.gen.ts
frontend/tests/e2e/recommendation.spec.ts
scripts/benchmark-recommendation.sh
scripts/check-catalog.mjs
```

### Фактические финальные команды и результаты

Все обязательные проверки P04 выполнены на окончательной реализации. Никакие падающие проверки не пропущены.

| Команда | Результат | Exit / фактические числа |
|---|---|---|
| `bash scripts/generate-api.sh` | PASS | 0; actual HTTP schema экспортирована, nullable request/response и ApiProblem/CSRF schema проверены, TS SDK/DTO обновлены |
| `bash scripts/check.sh` | PASS | 0; 114 unit +69 integration = **183 backend**, failures/errors/skipped **0/0/0**; npm ci, API drift, format/lint/typecheck PASS; **18 UI**, 0 fail; production frontend build PASS |
| `bash scripts/check-catalog.sh` | PASS | 0; 2 synthetic +2 reviewed manifests +12 negative cases = **16 schema checks** |
| `bash scripts/smoke.sh` | PASS | 0; **3/3**: backend readiness, proxy readiness, HTML |
| `bash scripts/test-e2e.sh` | PASS | 0; **14/14**, 0 failed/skipped, итог 1.2 min; 3 новых recommendation tests, 11 прежних; реальный browser/backend/PostgreSQL |
| `bash scripts/benchmark-recommendation.sh` | PASS | 0; **1/1**, 0 failed/skipped; 40 candidates, maxTools4, 5 warmup +20 measured |
| `bash scripts/check-secrets.sh` | PASS | 0; tracked/untracked nonignored + local password scan без совпадений; не полный коммерческий secret scanner |
| `git diff --check` | PASS | 0; whitespace ошибок нет |
| `bash scripts/dev-down.sh` + `bash scripts/dev-up.sh --no-build` | PASS | 0; остановлены/запущены только собственные процессы и Compose postgres; volume сохранён |
| Read-only HTTP preview через frontend proxy | PASS | 0; CSRF + HttpOnly cookie, no-store, HTTP200/NO_MATCH/UNKNOWN video; r2 UUID и revision2 не изменились |
| Команда preview из README через `fish --no-config` | PASS | 0; выполнена без редактирования, ответ реального dev-каталога проверен, token не печатался |

Unit-часть P04: **29 RecommendationEngineTest +21 ProfileAndInvariantTest =50** тестовых случаев (в одном property/invariant test дополнительно 160 генераций, seed 4042026). Новых IT **8**, существующих IT61. Всего уникальных исполняемых тестовых случаев основного backend/frontend/E2E **215** (183+18+14), плюс **1** отдельный benchmark; schema checks и smoke считаются отдельно, повторные прогоны не суммируются. Результаты взяты из Maven XML, Vitest и Playwright, не из наличия файлов тестов.

Benchmark: **p50=63.383349 ms, p95=77.538516 ms**, 102 091 посещённая комбинация, searchComplete=true. Среда: Linux x86_64, Intel Core i5-12450H, 12 logical CPUs, Java21.0.13; 5 прогревов и 20 последовательных измерений pure engine. JVM startup/DB/HTTP не входят; это не universal production SLA. Первичный замер до финальных регрессий был p95≈77.90 ms; итоговый отчёт использует только повторный финальный результат. Raw report: ignored `backend/target/recommendation-benchmark.txt`.

Игнорируемые логи финальных прогонов: `.runtime/p04-check-final.log`, `p04-generate-final.log`, `p04-e2e-final.log`, `p04-benchmark-final.log`, `p04-dev-start.log`. Они не включаются в Git. PostgreSQL17.12 healthy, Flyway V1–V4 валидны, P04 не добавлял миграций. Временных P04 integration/E2E контейнеров после завершения нет; dev PostgreSQL сохранён. Сервисы слушают loopback: 5173/18080/15432. `LLM_MODE=OFF`, `PAYMENTS_MODE=OFF`, `EMAIL_MODE=LOCAL`, `SOURCE_FETCH_ENABLED=false`.

### Реальные ответы HTTP на тестовые сценарии

Ниже выбранные поля из ответов RecommendationIT, а не вручную придуманные результаты. DEMO не является рыночным каталогом; полный response содержит evidence, sources, assessments, причины и версии. Input примеры — `data/fixtures/profiles/`; ответы сохраняются только в ignored target.

student:

```json
{
  "status": "MATCH",
  "catalogVersion": "04000000-0000-4000-8000-000000000001",
  "datasetKind": "DEMO",
  "engineVersion": "1.0.0",
  "policyVersion": "p04-1-fee-USD-0",
  "asOf": "2026-11-14T12:00:00Z",
  "searchComplete": true,
  "bundles": [
    {
      "strategies": [
        "BALANCED",
        "ECONOMY",
        "EXPANDED"
      ],
      "products": [
        "learner",
        "slides"
      ],
      "costs": {
        "currency": "USD",
        "cashDueNow": "0",
        "upfront": "0",
        "recurringMonthly": "0",
        "horizonTotal": "0",
        "futureMonthly": "0",
        "comparableBaselineMonthly": null,
        "potentialNetMonthlySavings": null,
        "compassFeeMonthly": "0",
        "confidence": "CONFIRMED_COMPONENTS",
        "confirmedBudgetFit": true
      },
      "usageFit": "UNKNOWN",
      "coverage": [
        {
          "capabilityId": "coding",
          "state": "SUPPORTED"
        },
        {
          "capabilityId": "pptx",
          "state": "SUPPORTED"
        },
        {
          "capabilityId": "study",
          "state": "SUPPORTED"
        }
      ]
    }
  ],
  "unmetRequirements": []
}
```

editor:

```json
{
  "status": "MATCH",
  "catalogVersion": "04000000-0000-4000-8000-000000000001",
  "datasetKind": "DEMO",
  "engineVersion": "1.0.0",
  "policyVersion": "p04-1-fee-USD-0",
  "asOf": "2026-11-14T12:00:00Z",
  "searchComplete": true,
  "bundles": [
    {
      "strategies": [
        "BALANCED",
        "ECONOMY",
        "EXPANDED"
      ],
      "products": [
        "creator"
      ],
      "costs": {
        "currency": "USD",
        "cashDueNow": "15",
        "upfront": "15",
        "recurringMonthly": "15",
        "horizonTotal": "180",
        "futureMonthly": "15",
        "comparableBaselineMonthly": null,
        "potentialNetMonthlySavings": null,
        "compassFeeMonthly": "0",
        "confidence": "CONFIRMED_COMPONENTS",
        "confirmedBudgetFit": true
      },
      "usageFit": "UNKNOWN",
      "coverage": [
        {
          "capabilityId": "editing",
          "state": "SUPPORTED"
        },
        {
          "capabilityId": "image",
          "state": "SUPPORTED"
        },
        {
          "capabilityId": "video",
          "state": "SUPPORTED"
        }
      ]
    }
  ],
  "unmetRequirements": []
}
```

real-catalog:

```json
{
  "status": "NO_MATCH",
  "catalogVersion": "03000000-0000-4000-8000-000000000002",
  "datasetKind": "REVIEWED",
  "engineVersion": "1.0.0",
  "policyVersion": "p04-1-fee-USD-0",
  "asOf": "2026-11-14T16:00:00Z",
  "searchComplete": true,
  "bundles": [],
  "unmetRequirements": [
    {
      "capabilityId": "need:images",
      "state": "UNKNOWN",
      "reason": "UNKNOWN_CAPABILITY"
    },
    {
      "capabilityId": "need:video",
      "state": "UNKNOWN",
      "reason": "UNKNOWN_CAPABILITY"
    }
  ]
}
```

Студент получил learner+slides за 0 USD, покрытие study/coding/pptx; это не условная студенческая скидка. Видеомонтажёр получил creator за 15 USD сейчас/месяц, 180 USD за 12 месяцев; image/video/editing подтверждены в DEMO. У обоих нет количественной заявки, поэтому usageFit=UNKNOWN. При отсутствии существующей базы savings=null. Три стратегии в этих двух кейсах совпали и объединены в один результат; отдельный unit scenario с необязательными images разводит ECONOMY и BALANCED/EXPANDED. Реальный r2 возвращает пустые bundles и UNKNOWN по отсутствующим images/video needs, с предупреждением об ограниченном каталоге из 3 продуктов.

### Итог P04

Все 10 функциональных критериев таблицы P04 выше — **PASS**. Применимая матрица сценариев выполнена; T24 именно переключение несуществующего affiliate flag и T35 notifications — **NOT_APPLICABLE** с причинами выше. Не заявляются: remote CI, проценты покрытия, production SLA/полный market coverage, независимый рейтинг качества и функциональность P05.

**P04 = AWAITING_REVIEW. P05 = NOT_STARTED.** Работа остановлена до приёмки владельцем. Ветка `codex/p01-scaffold`, HEAD остаётся `9f3e48f68d41ae40f32281f553a32349de4b4429`; **новых commit/push/PR нет**, index пуст. 39 изменённых/новых файлов перечислены выше; .env, secrets, caches, logs, target/dist, node_modules и browser artifacts остаются ignored и не входят в набор Git. Следующее действие — приёмка P04 владельцем и отдельное разрешение на commit/следующий этап.

## Финальная приёмка и закрытие P04 — 14.11.2026

Владелец предварительно принял P04 и явно разрешил финальную проверку, исправление реальных ошибок с регрессиями, статус DONE при отсутствии блокеров и один локальный commit `feat: implement personalized ai recommendation engine`. P05, push и PR по-прежнему запрещены. Предыдущий раздел сохраняет исторический отчёт первоначальной реализации и его реальные результаты; актуальное закрытие описано здесь.

### Проверка 10 архитектурных инвариантов

| № | Инвариант | Проверенное основание |
|---|---|---|
| 1 | Чистое детерминированное ядро и границы модулей | Controller вызывает Service; только Service получает Clock и CatalogReadService. Engine не обращается к БД, HTTP, LLM, random или системному времени; catalog не зависит от recommendation. Без анкеты P05 и новых dependencies |
| 2 | Только один опубликованный immutable snapshot | currentSnapshot разрешает active UUID один раз; class-level REPEATABLE_READ и version_id во всех выборках. PublishedCatalogSnapshot копирует списки; транзакция завершается до перебора. RecommendationIT повторно проверяет concurrent activation, чтение и rollback |
| 3 | Воспроизводимость и версии | Явные asOf/catalog/engine/policy/FX, стабильные сортировки и сравнения без бренда. Чистый результат воспроизводим; HTTP requestId — отдельная транспортная метка. Изменение денежного расчёта в этом закрытии обозначено engineVersion=1.0.1 |
| 4 | Строгий ввод и неизменность границы продукта | Typed multi-needs profile; денежные строки, ISO currency, даты, duplicate/null/range/unknown-field validation. Нет обязательного задания, prompt/upload или сохранения профиля; mass assignment premium отклоняется |
| 5 | Неопределённость, доказательства и hard requirements | EvidenceAssessment вычисляет свежесть на asOf; UNKNOWN/STALE/CONFLICTED не дают confirmed coverage. PARTIAL не получает 0.5; explicit accepted condition не делает его полным. Region/platform/languages/privacy/student не обходят ограничения |
| 6 | Деньги, календарь, FX и Compass fee | BigDecimal/Money, явная валюта, неизвестные tax/fee/FX не становятся нулём. Annual upfront отдельно от monthly equivalent; fee включена в итог/savings. Найдена и исправлена ошибка правой границы календарного горизонта existing subscriptions — описание ниже |
| 7 | MustKeep и честные действия с портфелем | Mandatory никогда не удаляются; их costs/capabilities учитываются один раз, over-budget/maxTools/conflict дают отказ. REVIEW не выполняет отмену; CANCEL_AT_RENEWAL не обещает refund. Savings только с известной базой |
| 8 | Раздельные сущности, конфликты и shared pools | Model/product/plan/offer/agent различаются. Included agent не дополнительный платёж; exact model access не означает quality score. Конфликты scoped к продукту, не vendor. UsageAssessment распределяет mode-qualified потребление по pool ID без двойного счёта |
| 9 | Стратегии, безопасный поиск и честные метрики | Hard constraints до ranking; ECONOMY/BALANCED/EXPANDED, tolerance0.05, stable tie-break/dedup, без paid/affiliate/brand бонуса. Удаляется только доказуемо избыточная необязательная новая покупка. Cap40/usage cap дают searchComplete=false. Пустой корень счётчика разобран и проверен отдельно |
| 10 | Read-only публичный API и безопасность | POST preview с session CSRF; strict body limit64KiB, per-IP/global/concurrency limits, Problem Details и no-store. Import/publish/profile writes недоступны. Реальные HTTP/E2E тесты; LLM/payments/source-fetch OFF, внешняя почта отсутствует |

### Найденная ошибка и регрессия

`CostCalculator.existing` раньше задавал конец горизонта как `asOf + horizonMonths` с часами расчёта, но дни продлений учитывал с полуночи. При продлении сегодня и расчёте в полдень это захватывало ещё одно продление на конечной календарной дате. Воспроизведение: 12 USD/MONTH, nextRenewal=2026-11-14, asOf=2026-11-14T12:00Z, horizon=12 → **156 вместо 144 USD**.

До исправления целевой тест `existingRenewalTodayDoesNotIncludeExtraPaymentAtHorizonEnd` действительно упал: **1 run /1 failure /0 errors /0 skipped**, Maven exit1, `.runtime/p04-close-regression-before.log`. Это не зачтено как PASS.

Исправление: правая граница — начало даты `today + horizonMonths` в **billingZone**, исключительная, как и дневная точность nextRenewal/cashDueNow. Это сохраняет исходный anchor и не меняет правила upfront/annual opt-in. Регрессия проверяет MONTH/YEAR, UTC/Asia-Almaty, полночь/полдень/поздний вечер: 12×12=144 и 1×120=120. Годовой тест использует upfront=120, чтобы проверять календарь, а не отказ по unrelated budget. Версия движка повышена до **1.0.1**, policy и schema не менялись. После исправления отдельный RecommendationEngineTest: **31/31 PASS**, exit0; `.runtime/p04-close-regression-after.log`.

### Почему 102091, а не 102090

Для 40 кандидатов и размеров 1–4: `C(40,1)+C(40,2)+C(40,3)+C(40,4) =40+780+9880+91390 =102090` непустых сочетаний. `BundleSearch.enumerate` увеличивает счётчик при каждом вызове, включая **начальный пустой корень**. Поэтому `visitedCombinations=102091` означает 102090 непустых сочетаний +1 корень, а не лишнюю рекомендацию. Пустой набор явно исключается из feasible/recommendations. Старое описание «102091 комбинация» было неточным.

Поле API сохранено для совместимости; Java comment и ARCHITECTURE уточняют семантику. Benchmark теперь печатает `visited_search_nodes`, `non_empty_combinations`, `empty_root_nodes` отдельно и проверяет **точное** равенство 102091/102090 вместо слабого `>=100000`. Regression `searchCountsTheEmptyRootButNeverRecommendsIt`: 4 кандидата →16 узлов, 15 feasible непустых сочетаний, в ответах нет пустого набора. Для поиска с mandatory/отсечениями метрика не преобразуется простым вычитанием единицы.

### Повторные обязательные проверки закрытия

| Команда / проверка | Статус | Фактический результат |
|---|---|---|
| `bash scripts/check.sh` | PASS, exit0 | **116 unit +69 integration =185 backend**, 0 failed/errors/skipped; **18 frontend UI**, 0 failed; install/build/format/lint/typecheck, OpenAPI drift и generated client PASS |
| `bash scripts/check-catalog.sh` | PASS, exit0 | **16 schema checks** (2 synthetic, 2 reviewed manifests, 12 negative cases) |
| `bash scripts/smoke.sh` | PASS, exit0 | **3/3**: backend readiness, proxy readiness, frontend HTML |
| `bash scripts/test-e2e.sh` | PASS, exit0 | **14/14**, 0 failed/skipped; 1.1 min, isolated PostgreSQL/backend/Chromium |
| `bash scripts/benchmark-recommendation.sh` | PASS, exit0 | **1/1**; p50=57.539629 ms, **p95=61.192790 ms**; exact 102091 nodes /102090 nonempty /1 empty root |
| `bash scripts/check-secrets.sh` | PASS, exit0 | tracked +untracked nonignored, pattern/local password scan; совпадений нет |
| `git diff --check` | PASS, exit0 | whitespace ошибок нет; новые файлы также проверены отдельно |
| `bash scripts/dev-down.sh` + `bash scripts/dev-up.sh --no-build` | PASS, exit0 | Только собственные процессы/Compose PostgreSQL; volume сохранён |
| Actual POST preview через frontend proxy | PASS, exit0 | CSRF session; engine1.0.1; real r2 UUID `03000000-0000-4000-8000-000000000002`, revision2 неизменны; video NO_MATCH/UNKNOWN |
| 10 архитектурных инвариантов выше | **10/10 PASS после исправления** | Код, ограничения, регрессии и действующие PostgreSQL/API/E2E checks; блокеров закрытия не осталось |

Новые регрессии: два JUnit случая (календарная граница с 12 сочетаниями period/zone/hour и пустой корень поиска). Property/invariant test по-прежнему исполняет 160 сгенерированных профилей. Все финальные прогоны имеют 0 failed/errors/skipped; ранее воспроизведённая ошибка не скрыта. 185+18+14=**217** основных тестовых случаев, отдельно 1 benchmark; повторные запуски не суммируются. Новые зависимости не устанавливались, manifests/lockfile не менялись. Benchmark среда прежняя: Linux x86_64, i5-12450H, 12 logical CPUs, Java21.0.13, 5 warmup +20 measured; измеряется pure engine, не HTTP/DB и не production SLA.

Логи закрытия остаются ignored: `.runtime/p04-close-{check,e2e,benchmark,dev,regression-before,regression-after}.log`. Перед staging проверены ровно **39 файлов P04** из списка предыдущего раздела, пустой исходный index, отсутствие .env/секретов/logs/cache/build/node_modules/browser artifacts/symlinks и посторонних изменений. Статические примеры предыдущего раздела относятся к его запуску engine1.0.0; актуальные повторно выполненные HTTP examples в `backend/target/p04-*-response.json` имеют engine1.0.1.

Ограничения сохранены: remote CI и line/branch coverage percentages **NOT_RUN**; новый инструмент покрытия не разрешён. Playwright на Arch использует Ubuntu fallback. Ограниченный каталог/UNKNOWN, отсутствие независимого quality ranking, ограниченная модель usage и локальный rate limiter не выдаются за production readiness. T24 переключение отсутствующего affiliate flag и T35 notifications остаются NOT_APPLICABLE с прежними основаниями. P05, платные API/платежи, внешняя почта, deployment, push и PR не запускались.

**P04 = DONE, P05 = NOT_STARTED** — статус DONE явно предписан владельцем для этого закрытия при успешных проверках. Разрешён ровно один локальный commit `feat: implement personalized ai recommendation engine` на существующей `codex/p01-scaffold`, только с проверенными файлами P04; hash выводится в финальном отчёте и доступен через `git log -1`. После коммита работа прекращается, начало P05 требует отдельного разрешения.

## P05 — AI Stack Builder UI (14–15.11.2026, Asia/Almaty)

**Разрешение:** владелец явно принял P04 и разрешил только P05. Начальное рабочее дерево чистое; ветка `codex/p01-scaffold`, HEAD `67516620e1dcf20ca46feea69f02dd5b33700001`. Commit/push/PR/P06 запрещены. P01–P04 отмечены DONE по прямому поручению владельца.

### Реализовано

- Главная с переходом к подбору; `/onboarding` с тремя шагами; `/results`; `/methodology`; сохранены `/catalog` и `/catalog/:slug`. Общая навигация, адаптивная сетка и видимый focus.
- Multi-select taxonomy из backend, приоритет 1–5, обязательность и частота; дополнительные функции, страна/языки/платформа/privacy, явно заданный объём, бюджет/предоплата/годовая оплата/горизонт/число инструментов/студент.
- Existing subscriptions: опубликованный offer или custom, собственная фактическая цена либо UNKNOWN, календарь продления, AUGMENT/REVIEW/mustKeep. Они передаются в P04 и участвуют в стоимости, а не только отображаются.
- Generated SDK: pinned paginated Catalog API → CSRF → POST preview. AbortController, блокировка повторной отправки, номер запроса, отмена, HTTP 400/403/413/429/500/network, Retry-After и новый CSRF при повторе. Версия результата сверяется с версией формы.
- Результаты: фактическое количество наборов и стратегии сервера, условные варианты отдельно; названия тарифов/продуктов, модели/агенты, покрытие/unmet, причины, ограничения, источники/даты/статусы, денежные компоненты без frontend-арифметики. Условный вариант без strategy label получает заголовок из состава.
- Черновик/шаг/результат только в React memory; переходы их сохраняют, изменение профиля инвалидирует результат, «Очистить анкету» сбрасывает контекст. Нет localStorage/sessionStorage/profile DB и обязательного задания.
- Никаких новых зависимостей, backend-алгоритмов, DTO вручную, миграций, изменений реальных manifest или внешних интеграций.

### Проверки и найденные случаи

Первый `bash scripts/check.sh`: exit 0; backend 116 unit +69 PostgreSQL IT =185 PASS, 0 failures/errors/skipped; OpenAPI/client consistency PASS; frontend format/lint/typecheck/build PASS; UI 37 PASS; catalog schemas PASS. Отдельные check-catalog/check-secrets/diff и smoke (3 проверки localhost) выполнены с exit 0. После расширения UI suite отдельный `npm test`: 41 PASS, 0 FAIL.

Промежуточные ошибки не скрыты: UI-тест обнаружил задержку кнопки при Retry-After=0, исправлена синхронизация времени ответа. Ручной реальный preview выявил пустой заголовок условного варианта (strategies=[]), добавлен заголовок по составу и регрессионный тест. Базовая цена неполного предложения теперь явно отделена от полной стоимости. Первые два E2E-прогона: по 14 PASS /1 FAIL /9 NOT_RUN из-за точного label selector и неоднозначного H1/H3 в тесте; селекторы исправлены. Третий прогон: 21 PASS /1 FAIL /5 NOT_RUN — тест запросил 200 кредитов при общем pool=100, сервер правильно вернул NO_MATCH. Сценарий сохранён как отрицательная проверка, затем пользователь снижает потребление до ровно 100 и ожидает подтверждённый набор. Лимиты/engine/источники ради теста не изменялись. Следующий прогон: 14 PASS /1 setup FAIL /12 NOT_RUN — стандартный beforeAll timeout 30 секунд короче обязательной паузы rate window; для этого setup-hook установлен timeout 120 секунд.

Следующая полная E2E-серия: 27 PASS /0 FAIL /0 skipped (3.7m). Визуальный просмотр реальных скриншотов обнаружил отдельный UX-дефект: переиспользованный DOM button менял type с button на submit при переходе к бюджету и показывал преждевременные ошибки. Исправлены key/preventDefault, ошибки снимаются при редактировании, label отделён от сообщения. Добавлены UI и E2E-assertions отсутствия alert до отправки. После исправления полная check.sh повторена: 185 backend +42 UI PASS, build/lint/typecheck/OpenAPI/catalog PASS. Дополнительная проверка фокуса выявила перенос на H2 при очистке ошибки: поле оставалось со значением «2» вместо «20». Тест сначала воспроизвёл FAIL, эффекты фокуса разделены по смене шага и ошибкам; затем frontend lint/typecheck/42 tests/build повторены. После исправления фокуса E2E: 27 PASS /0 FAIL /0 skipped (3.6m), в том числе новые assertions отсутствия преждевременных ошибок. Визуально устранено наследование общих стилей каталога для checkbox row и primary button; frontend lint/typecheck/build повторены, контрольный E2E со свежими скриншотами выполняется. Статус пока IN_PROGRESS.

### Ручной прогон и ограничения

Через in-app Browser проверены главная, настоящая анкета, preview по реальному r2 (US/en/USD, данные введены явно), NO_MATCH и условные варианты с неизвестной полной ценой, каталог, ChatGPT detail, возврат с сохранённым бюджетом 20, методология. На viewport 360 измерено clientWidth=scrollWidth=345 (полоса прокрутки учтена), на 768 — 753=753; ошибки приложения в console log отсутствовали. Встроенный screenshot/click API иногда возвращал CDP timeout; DOM/CUA-взаимодействие работало. Review-скриншоты снимаются настоящим Chromium в E2E и проверяются отдельно.

Реальный опубликованный dev-каталог остался `03000000-0000-4000-8000-000000000002`, revision 2, три продукта. Только две сферы: «Разработка программ» и «Поиск информации». Отсутствующая обязательная сфера вызывает явное ограничение, не создаёт frontend need ID. Для полноценных положительных сценариев используются только изолированные DEMO fixtures, в том числе дополнительная terminal-agent capability внутри E2E manifest. Все такие экраны обозначены DEMO.

Годовой итог берётся из backend horizonTotal при 12 месяцах; для другого горизонта предлагается изменить период, без умножения monthly на 12. Неизвестный all-in/FX/регион/льгота не становится гарантией доступности. Временные 500/429 для UI recovery вводятся HTTP route fixture; повтор обращается к настоящему серверу. Реальная CSRF/413/429-защита покрыта PostgreSQL RecommendationIT. Между предыдущей API suite и Builder suite выдерживается окно 60 секунд, лимитер не отключается.

Playwright на Arch по-прежнему использует ранее разрешённый Ubuntu 24.04 Chromium fallback. Удалённый CI, полный accessibility/security audit, процент coverage и production load testing — NOT_RUN, не являются результатом локального запуска. Новых установок браузера и системных зависимостей нет. Платные AI API, реальные платежи, deployment — NOT_APPLICABLE для P05 и не выполнялись.

### Изменённые файлы P05

```text
README.md
docs/ARCHITECTURE.md
docs/PROGRESS.md
frontend/src/api/builder.ts
frontend/src/app/App.module.css
frontend/src/app/App.tsx
frontend/src/app/Home.tsx
frontend/src/app/Methodology.tsx
frontend/src/app/Navigation.tsx
frontend/src/builder/Builder.module.css
frontend/src/builder/Builder.test.tsx
frontend/src/builder/BuilderPage.tsx
frontend/src/builder/BuilderSession.tsx
frontend/src/builder/ExistingSubscriptions.tsx
frontend/src/builder/ResultsPage.tsx
frontend/src/builder/draft.ts
frontend/src/builder/test-data.ts
frontend/src/catalog/Catalog.module.css
frontend/src/catalog/CatalogLayout.tsx
frontend/tests/e2e/stack-builder.spec.ts
```

`.env`, `.cache`, `.tools`, `.runtime`, `frontend/node_modules`, `frontend/dist`, `frontend/test-results`, `backend/target` игнорируются Git. Index пуст; чужих файлов/правок в начальном дереве не было. Commit не создавался. P06 = NOT_STARTED.


### Финальное выполнение P05 — 15.11.2026 (UTC 14.11.2026)

**P05 = AWAITING_REVIEW. P06 = NOT_STARTED.** Все обязательные технические критерии выполнены; владелец ещё не принимал P05. Никакого commit/push/PR/deploy.

| Проверка | Результат | Фактические данные |
|---|---|---|
| `bash scripts/check.sh` | PASS, exit 0 | 116 unit +69 PostgreSQL IT =185 backend, 0 failures/errors/skipped; 42 UI; npm ci, format/lint/typecheck/build, OpenAPI/client consistency, catalog schemas |
| Frontend после последней правки фокуса/стилей | PASS, exit 0 | format:check/lint/typecheck/build повторены; итоговый `npm test`: 42 passed, 0 failed/skipped (7 Home +11 Catalog +24 Builder) |
| `bash scripts/check-catalog.sh` | PASS, exit 0 | Synthetic manifest, 12 negative cases, оба reviewed manifest, recommendation DEMO schema |
| `bash scripts/smoke.sh` | PASS, exit 0 | 3 проверки: backend readiness 18080, frontend 5173, HTML; только loopback |
| `bash scripts/test-e2e.sh` | PASS, exit 0 | Последний прогон 27 passed, 0 failed/skipped, 3.6m; 14 существующих +13 P05; отдельный PostgreSQL tmpfs и локальные app processes, штатная очистка только своих ресурсов |
| `bash scripts/check-secrets.sh` | PASS, exit 0 | Совпадений с шаблонами секретов и локальным паролем нет; проверены tracked + untracked кандидаты |
| `git diff --check` | PASS, exit 0 | Нет whitespace errors |
| Git/artifacts review | PASS | 20 файлов P05 (8 modified +12 new), index пуст; .env/cache/logs/node_modules/build artifacts не входят в изменения |

Логи находятся в ignored `.runtime/p05-check-final.log`, `.runtime/p05-ui-review.log`, `.runtime/p05-e2e-release-review.log`, `.runtime/p05-format-final.log`. Промежуточные failures и их исправления описаны выше, не заменены обещанием PASS.

### Матрица технических критериев приёмки

| ID | Критерий | Результат / доказательство |
|---|---|---|
| C01 | Landing/onboarding/results/catalog/product/methodology и работающая навигация | PASS: маршруты, existing catalog tests, ручной browser review |
| C02 | Multi-select из опубликованной taxonomy, отсутствие выдуманных категорий | PASS: UI + реальный r2 E2E; missing mandatory need блокирует фиктивный подбор |
| C03 | Важность/required/frequency, функции, страна, язык, платформа, privacy и объём | PASS: generated profile assertions; E2E A/C/D |
| C04 | Monthly/upfront/annual, существующие подписки и mustKeep участвуют в расчёте | PASS: UI поля и реальные API-проверки zero budget/retained charge once |
| C05 | Generated SDK, CSRF, строгий профиль, повторные/устаревшие запросы | PASS: UI transport/race/cancel/snapshot tests, API security IT/E2E |
| C06 | Реальные 1/2/3 набора, стратегии, NO_MATCH и условные варианты отдельно | PASS: UI counts, реальные E2E, заголовок при пустом strategies |
| C07 | Money/UNKNOWN/FREE/условия, источники/даты, модели и агенты отдельно | PASS: UI nullable cost rendering, региональный r2 E2E H, Linux included agent C; нет frontend money/ranking arithmetic |
| C08 | Loading/empty/errors 400/403/413/429/500/network, retry, local draft/reset | PASS: 24 Builder UI tests, E2E recovery/edit, Storage.setItem не вызывается |
| C09 | Keyboard/focus/labels, 360/768/1440, отсутствие overflow/console errors | PASS: E2E assert + просмотр screenshots; focus correction regression сначала воспроизведён, затем исправлен |
| C10 | Персоны A/C/D/E/H, mustKeep, недостаток видео-данных, нет фиктивных рекомендаций | PASS: 13 новых E2E с настоящим сервером, синтетика только в изолированной БД |
| C11 | Нет prompt/task requirement, нового алгоритма, лишних dependencies или внешних действий | PASS: diff/manual review; backend/manifests/lockfile/real catalog неизменны, внешние режимы OFF |
| C12 | README/ARCHITECTURE/PROGRESS, ручная инструкция, проверки и Git review | PASS: 2 сценария R1 в README; все обязательные команды выполнены; commit не создавался |

### Review-артефакты и Git

Актуальные 13 PNG скопированы без изменения из последнего E2E в ignored `.runtime/p05-review/`: `p05-home-{360,768,1440}.png`, `p05-form-{360,768,1440}.png`, `p05-results-{360,768,1440}.png`, `p05-catalog-{360,768,1440}.png`, `p05-student-results.png`. Визуально просмотрены desktop home/results, формы 360/768 и mobile home; новые формы без преждевременных ошибок, с checkbox row и выделенной primary button. В формах/результатах screenshots — изолированные DEMO-данные, не реальная рыночная рекомендация.

Git: `codex/p01-scaffold`; HEAD остался `67516620e1dcf20ca46feea69f02dd5b33700001` (P04). Все 20 перечисленных файлов P05 остаются unstaged. Приёмка владельца — следующий шаг; commit и P06 требуют отдельного разрешения.

## Окончательное закрытие P05 — 15.11.2026

**Основание:** владелец сообщил «P05 предварительно принят. Разрешаю выполнить его окончательное закрытие», поручил повторные проверки, статус DONE при успехе и один локальный commit `feat: implement personalized ai stack builder ui`. P06 не разрешён. Повторно прочитаны AGENTS.md, общие правила/инварианты и раздел P05 в CODEX_PROMPT.md, актуальные записи PROGRESS.md; проверены код, тесты и Git. Ветка `codex/p01-scaffold`, исходный HEAD `67516620e1dcf20ca46feea69f02dd5b33700001`.

### Десять групп инвариантов

Отдельного прежнего нумерованного списка «10 инвариантов P05» в доступном поручении и документах нет. Ниже явно сгруппированы требования исходного задания P05, AGENTS.md и матрицы C01–C12, без изменения требований.

| № | Инвариант | Итог и проверенное доказательство |
|---|---|---|
| 1 | Подбор по постоянным потребностям, без задания/промпта/регистрации и второго алгоритма | PASS: маршруты App, BuilderPage и generated preview SDK; расчёт/ранжирование остаются в P04, E2E проходит гостевой путь |
| 2 | Только backend taxonomy и согласованная версия каталога | PASS: readBuilderCatalog загружает страницы с одним UUID и сверяет versionId; неизвестная обязательная сфера блокирует подбор; UI snapshot mismatch test и реальный r2 E2E |
| 3 | Выбор потребностей и ограничений действительно отправляется в профиль | PASS: веса, required, частота, страна/языки/платформы/privacy, функции и объём проверены UI; E2E Linux/terminal agent и shared quota; частота не превращается в выдуманный объём |
| 4 | Деньги, UNKNOWN, FREE, предоплата, годовой период и FX не подменяются | PASS: decimal strings/null, без денежной арифметики на frontend; FREE требует подтверждённых нулевых компонентов; годовая сумма только при horizon=12; fee/baseline/net savings — серверные поля; zero budget и региональный UNKNOWN E2E |
| 5 | Текущие подписки и включённые возможности учитываются без удвоения | PASS: existing subscriptions/mustKeep отправлены в backend; E2E сохраняет creator-plan ровно один раз с расходом 15 USD; модели/агенты отдельно, общий pool=100 проверен на превышении 200 и ровно 100 |
| 6 | Количество, порядок и стратегии серверных наборов сохранены; NO_MATCH честный | PASS: ResultsPage отображает bundles без сортировки/добавления, UI проверяет 1/2/3; условные варианты отдельно, без придуманной стратегии; NO_MATCH → изменение бюджета → новый реальный requestId |
| 7 | Утверждения имеют видимые основания и ограничения | PASS: evidence/source/status/date/conditions, unmet и причины; старые product links закреплены UUID; eligibility/студенческие/региональные условия не превращаются в гарантию; внешние ссылки ограничены http(s) |
| 8 | Контракт и безопасность API сохранены | PASS: generated SDK, same-origin session, CSRF перед preview; 400/403/413/429/500/network состояния; failed-CSRF UI test не отправляет профиль; backend IT/E2E проверяют CSRF и недоступность внутренних операций |
| 9 | Черновик только в памяти; reset, навигация, повторы и запоздалые ответы корректны | PASS: BuilderSession React state, AbortController + sequence/ref, invalidation при редактировании; UI tests cancel/race/reset/no storage/Retry-After, E2E recovery; профили не сохраняются на сервере |
| 10 | Доступность, адаптивность и существующая навигация сохранены | PASS: labels/semantic headings/focus/error summary/disabled, регрессия ввода после ошибки; E2E 360/768/1440 без overflow/console errors, старые catalog routes; повторно просмотрены свежие mobile form и desktop home screenshots |

Новых ошибок в коде при окончательной проверке не обнаружено; дополнительные исправления и регрессионные тесты не потребовались. Ранее найденные и исправленные ошибки P05 и их регрессии сохранены выше. Backend, миграции, реальные manifest, manifests/lockfile зависимостей и generated API не изменены. Тестовые fixtures не импортируются производственным frontend. Режимы внешних интеграций по умолчанию OFF / local sink, проверки IntegrationSettingsTest пройдены.

### Повторные обязательные проверки

| Команда | Фактический результат |
|---|---|
| `bash scripts/check.sh` | PASS, exit 0: 116 unit +69 PostgreSQL integration =185 backend tests, 0 failures/errors/skipped; npm ci, OpenAPI/client consistency, format/lint/typecheck/build PASS; 42 UI tests, 0 failed/skipped (24 Builder +11 Catalog +7 Home) |
| `bash scripts/check-catalog.sh` | Финальный последовательный запуск PASS, exit 0: 1 synthetic manifest +12 negative cases, 2 reviewed manifests и recommendation DEMO schema |
| `bash scripts/smoke.sh` | PASS, exit 0: backend readiness 18080, frontend readiness 5173, frontend HTML — 3 проверки |
| `bash scripts/test-e2e.sh` | PASS, exit 0: 27 passed, 0 failed/skipped, 3.0m; отдельные PostgreSQL tmpfs/backend/frontend, штатное завершение стенда |
| `bash scripts/check-secrets.sh` | PASS, exit 0: известные secret patterns и локальный пароль не найдены; это не всеобъемлющий аудит секретов |
| `git diff --check` | PASS, exit 0 |

Один промежуточный отдельный `check-catalog.sh` завершился exit 1 (`Cannot find module 'ajv'`): он был ошибочно запущен одновременно с `npm ci` внутри check.sh, которое временно заменяло node_modules. После завершения check.sh команда повторена последовательно и прошла. Это ошибка порядка запуска проверки, не скрытая ошибка каталога; код/зависимости ради неё не менялись. Все остальные перечисленные финальные запуски прошли с первого раза.

Итого финальная серия: **254 теста PASS (185 backend +42 UI +27 E2E), 0 FAIL, 0 skipped**, дополнительно 3 smoke и schema/contract/static checks. Логи ignored: `.runtime/p05-close-check.log`, `.runtime/p05-close-e2e.log`. Новые 13 screenshots: `.runtime/e2e-80ca7c2e28db/p05-*.png`; визуально просмотрены `p05-form-360.png` и `p05-home-1440.png`. Предыдущий ручной браузерный прогон всех основных маршрутов описан выше; E2E повторяет реальные переходы.

### Ограничения и Git

- Сохранены ограничения Playwright: Arch использует ранее установленный Ubuntu 24.04 Chromium fallback. Удалённый CI, полный accessibility/security audit, процент coverage и production load testing — NOT_RUN; локальные проверки не объявляются их заменой.
- Реальный каталог ограничен тремя продуктами и двумя потребностями; недостаток данных явно показан. Synthetic E2E публикации происходят только в отдельной БД. Платные API/реальные платежи/deployment — NOT_APPLICABLE, не запускались.
- Просмотрены все 20 кандидатов P05: 3 документа, frontend UI/adapter/styles и UI/E2E tests. Чужих изменений нет. `.env`, `.runtime`, `.cache`, `.tools`, node_modules, target/dist, логи и screenshots исключены; tracked/staged артефактов нет. Начальный index пуст. Перед commit повторяются secret scan и staged diff check.
- Разрешён ровно один локальный commit с указанным сообщением; push/PR не выполняются. Исторические записи AWAITING_REVIEW выше описывают предыдущую передачу, итоговый статус этого закрытия — **P05 = DONE; P06 = NOT_STARTED**. Следующий этап только по отдельному разрешению владельца.

## P06 — начальная проверка и блокер зависимости (15.11.2026)

Владелец явно сообщил «P05 ПРИНЯТ. Разрешаю начать P06 — User Accounts, Authentication & Saved Profiles». P06 начат с проверки требований и окружения; реализация остановлена согласно прямому пункту 17 поручения: «Если необходима новая прямая зависимость, остановись, сообщи название, версию, назначение и причину установки».

Прочитаны AGENTS.md, актуальное состояние PROGRESS.md, README.md, ARCHITECTURE.md и применимые разделы CODEX_PROMPT.md: архитектура, пользовательские данные, API, Free-профиль, безопасность/приватность и P06. Исходный Git чистый, ветка `codex/p01-scaffold`, HEAD `8ec96c51f3b4316f7b4e06d72dd2ec0e57f1cce1` (закрытие P05).

### Факты и конкретное предложение для согласования

- `backend/pom.xml`: Boot parent 4.1.2, Java 21; Spring Security/JDBC через существующие starters уже доступны. Spring Session JDBC/Core и session starter отсутствуют; в локальном Maven cache есть только их BOM, не реализация. Текущая конфигурация использует servlet session и не содержит PostgreSQL session repository.
- Требование P06: серверные сессии в PostgreSQL, переживающие restart, без самодельного session manager. Официальный [Spring Boot Session JDBC starter](https://docs.spring.io/spring-boot/reference/web/spring-session.html) предоставляет интеграцию стандартного Spring Session с servlet-приложением.
- Предложена **одна новая прямая зависимость**: `org.springframework.boot:spring-boot-starter-session-jdbc:4.1.2`. Она использует `spring-boot-session-jdbc:4.1.2` и транзитивные `org.springframework.session:spring-session-jdbc:4.1.1` / `spring-session-core:4.1.1`. Необходимые транзитивные зависимости входят в запрос разрешения. Новые frontend/UI зависимости не предлагаются.
- Проверены существующий локальный Boot BOM 4.1.2 (`spring-session.version=4.1.1`, `spring-security.version=7.1.2`, `spring-framework.version=7.0.10`) и Spring Session BOM 4.1.1. Версии согласованы с текущим управлением зависимостями; менять Boot, Java, React или TypeScript не требуется. Успешная сборка с новым starter **ещё не проверена**, поскольку его установка не разрешена.
- Read-only чтение официальных Maven POM дало HTTP 200 для [starter 4.1.2](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-session-jdbc/4.1.2/spring-boot-starter-session-jdbc-4.1.2.pom), [Boot session JDBC 4.1.2](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-session-jdbc/4.1.2/spring-boot-session-jdbc-4.1.2.pom) и [Session JDBC 4.1.1](https://repo.maven.apache.org/maven2/org/springframework/session/spring-session-jdbc/4.1.1/spring-session-jdbc-4.1.1.pom). Web-инструмент не смог открыть Maven XML, поэтому metadata прочитаны urllib непосредственно из Maven Central в память. JAR/зависимости не устанавливались, manifests не менялись.
- V1–V4 содержат схему каталога; `user_need` — taxonomy, не личный профиль. Таблиц аккаунтов/личных результатов/сессий пока нет. Будущие миграции только после V4; существующие миграции сохраняются.
- При реализации необходимо заменить нынешнее общее разрешение `GET /api/v1/**` в SecurityConfig на явные публичные маршруты и защищённые личные маршруты. Это необходимая часть P06, не ослабление текущей CSRF-защиты.

### Граница продолжения и проверок

После отдельного разрешения: подключить закреплённый starter; реализовать identity/ownership, Flyway-схему сессий и личных данных, локальный email sink, auth/verification/reset, один Free-профиль и исторические snapshots, UI и generated API. Guest → account сохраняется только после явного подтверждения, без browser token storage. До приёмки нужны реальные PostgreSQL A/B ownership/security tests, restart persistence, UI/E2E и все обязательные scripts. Это план продолжения, а не заявление о реализованной функциональности.

**Историческая матрица до разрешения зависимости:** проверка исходного состояния и dependency metadata — PASS; auth/роли/verification/reset, PostgreSQL sessions/restart, profiles/snapshots/stacks, guest transfer, UI/OpenAPI, ownership/лимиты, export/delete, security/UI/E2E — NOT_IMPLEMENTED / NOT_RUN. `check.sh`, `check-catalog.sh`, `smoke.sh`, `test-e2e.sh` в этой начальной проверке не запускались: код не менялся, этап остановлен перед установкой. Результаты P05 выше не выдаются за тесты P06. Для единственного изменения документации выполнены `bash scripts/check-secrets.sh` и `git diff --check` (exit 0).

Изменён только `docs/PROGRESS.md`: актуальный статус, разрешения и этот блокер. Commit/push/PR, миграции, установка зависимостей, реальная почта, платные API и платежи не выполнялись. **P06 = BLOCKED; P07 = NOT_STARTED.** Следующее действие — отдельное разрешение владельца на указанную зависимость и её необходимые транзитивные зависимости.


## P06 — продолжение после разрешения зависимости, 15.11.2026

Предыдущая запись BLOCKED описывает исходную остановку до разрешения; этот блок содержит последующую реализацию. P01–P05 приняты владельцем. Ветка `codex/p01-scaffold`, исходный HEAD `8ec96c51f3b4316f7b4e06d72dd2ec0e57f1cce1`. Commit/staging/push/PR не выполнялись; P07 не начат.

### Зависимость и изменения

- В `backend/pom.xml` добавлен только `spring-boot-starter-session-jdbc`, без собственной версии: Boot parent/BOM 4.1.2 разрешает starter 4.1.2 и Session JDBC/Core 4.1.1. `maven dependency:tree` и последующая загрузка/compile exit 0, дерево `.runtime/p06-dependencies.log`, установка `.runtime/p06-install.log`. Транзитивные версии вручную не закреплялись, frontend manifests/lockfile и глобальное окружение не менялись.
- V5 — стандартная PostgreSQL schema Spring Session 4.1.1 (две таблицы); V6 — user_account, identity_token, auth_rate, saved_profile, recommendation_snapshot, saved_stack. V1–V4 неизменны, `initialize-schema=never`, та же PostgreSQL. Роли USER/ADMIN в constrained column. Новые Redis/БД/сервисы не создавались.
- `identity`: Spring Security PasswordEncoder/AuthenticationManager, normalize/unique email, generic responses, verify/resend/reset, одноразовые hashed tokens с TTL, PostgreSQL rate limits, локальный приватный файловый sink, безопасный первый ADMIN через локальный CLI. Стандартный Spring Session JDBC обеспечивает restart/expiration/logout/fixation, CSRF обновляется при login/logout. Reset/delete отзывают все сессии; credential version закрывает гонку позднего сохранения сессии.
- `portfolio`: Free один профиль (UNIQUE + account lock + optimistic revision), immutable server-derived snapshots и выбранные наборы; все SQL ограничены owner из Principal. Результат сохраняется с версиями, временем, параметрами, UNKNOWN/FX/предупреждениями; новые публикации старые snapshots не меняют. Export без секретов, delete с паролем/подтверждением и cascade личных данных/сессий/локальных писем. P07 оптимизация расходов не реализована.
- Frontend: register/login/verify/forgot/reset/account/dashboard, guest→account без потери памяти SPA и без автозаписи, отдельное согласие, load/rerun/history, logout/export/delete, session expiration/account switch и errors. Только generated DTO/SDK; OpenAPI получен от реального backend и повторная генерация проверяется. Нет JWT/browser token storage, новых UI dependencies, share links или Premium из клиента.
- Скрипты: dev-профиль для локального sink, отдельный sink E2E, `dev-mail.py` (без печати кодов), `admin-bootstrap.sh` (интерактивный GRANT, без запуска на рабочей БД). README/ARCHITECTURE документируют flow, schema, API, ограничения и использование.

### Фактические финальные проверки P06

- `bash scripts/check.sh`: PASS, exit 0. Backend 116 unit +88 integration =204, failures/errors/skipped 0; frontend 53 UI tests, 0 failed/skipped; install, generated OpenAPI/SDK comparison, format/lint/typecheck/build и JSON Schema PASS. Лог `.runtime/p06-check.log`, XML в backend/target.
- `bash scripts/check-catalog.sh`: PASS, exit 0 — synthetic manifest, 12 negative cases, оба reviewed manifests и recommendation fixture.
- `bash scripts/dev-down.sh` + `bash scripts/dev-up.sh --no-build`: PASS, exit 0, только собственные процессы/Compose, volume сохранён; локальная PostgreSQL healthy, V5/V6 применены.
- `bash scripts/smoke.sh`: PASS, exit 0 — 3/3 (backend readiness, proxy readiness, frontend HTML); `.runtime/p06-smoke.log`.
- `maven test-compile failsafe:integration-test failsafe:verify -Dit.test=IdentityIT`: дополнительный PASS, exit 0, 18/18 PostgreSQL security tests после усиления replay старой cookie и старого CSRF после logout. Не заменяет полный check.sh; production JAR при этом не пересобирался. Лог `.runtime/p06-security.log`.
- `python3 -m py_compile` для локальных Python scripts и `bash -n scripts/admin-bootstrap.sh`: PASS. Две изолированные проверки dev-mail viewer с mocked desktop handler: успешный/неуспешный запуск, отсутствие token в output/launcher argv — PASS. Реальное открытие системного браузера этим скриптом не заявляется.
- `bash scripts/test-e2e.sh`: PASS, exit 0, **34/34**, 0 failed, 0 skipped, 7.1 минуты. Из них 7 новых комплексных P06 scenarios и 27 прежних. Реальные изолированные PostgreSQL, backend, Vite/Chromium, local sink; test resources остановлены в finally. Лог `.runtime/p06-e2e.log`, screenshots `.runtime/e2e-060325ade6a4/`.
- `bash scripts/check-secrets.sh` и `git diff --check`: PASS, exit 0, повторены после документации. Git candidate audit: 0 секретных/случайных/почтовых/build/cache файлов, 0 staged; .env/node_modules/target/dist/.runtime/.cache исключены. Это ограниченный scanner + ручной review, не полный независимый security audit.

### Исправления, найденные проверками

Первый тестовый запуск выявил ошибочную инициализацию временного mail directory в fixture: исправлен lifecycle @TempDir; созданные этим тестом `.mail` удалены из untracked backend/null, без удаления чужих файлов. Snapshot POST/GET расходились по наносекундам savedAt: время нормализовано до точности PostgreSQL (микросекунды), историческое равенство проверяется регрессионным тестом. Сценарии IdentityIT изолированы реальным lifecycle приложения, чтобы их отдельные запросы не делили preview rate window; сами production лимиты и отдельные rate-limit проверки сохранены. Старый тест запрета всех personal routes обновлён на новый контракт: private 401, catalog write 403. Первый E2E остановился до тестов из-за test.use(trace/video) внутри describe; настройка перенесена на уровень файла. Следующий прогон: 28 PASS, 1 FAIL, 5 NOT_RUN из-за неоднозначного тестового status-selector (статус подбора и сохранения на одной странице); селектор уточнён, независимые P06 тесты больше не объединены serial-зависимостью. OpenAPI дополнен cookie security и typed ApiProblem responses. Финальный просмотр выявил ещё одну реальную ошибку контракта: nullable Account внутри SessionStatus терял null при $ref-генерации Springdoc. Добавлена явная nullable schema-обёртка и регрессионная проверка guest response/schema; generated SDK пересоздан с `account: Account | null`. Диагностика security assertions не выводит значения password/session/CSRF даже при падении. Ни одна из этих неудач не выдаётся за PASS; итоговые результаты относятся к повторным запускам.

### Ограничения и NOT_RUN

- Реальная отправка email, платные API/платежи, production deployment, push/PR/commit — NOT_RUN, запрещены. Интеграции OFF, email только local sink.
- ADMIN CLI на рабочей БД — NOT_RUN, чтобы не менять рабочие роли; механизм grant/one-time/revocation проверен на Testcontainers.
- Удалённый CI — NOT_RUN; локальный Chromium Playwright на Arch использует прежний Ubuntu fallback (ограничение P01 сохранено). E2E не зависит от реального рынка/платных API.
- Production HTTPS/proxy, независимый pentest, правовая/privacy-проверка и доставка/retention production-писем — не заявляются выполненными. Локальные письма содержат одноразовые секреты и хранятся приватно до удаления аккаунта/ручной очистки. Истёкшие token rows не действуют, удаляются при замене/удалении аккаунта.
- Гостевой черновик сохраняется при SPA переходе/входе, но не после reload/закрытия вкладки (privacy правило P05). Для verify через новое окно следует вернуться в исходную вкладку. Защитные ограничения: 100 snapshots/account, один Free-профиль, воспроизведение старого engine/policy возможно только пока версия поддерживается; иначе явный 409.


### Файлы P06 перед приёмкой

Ручной review: manifests, default-deny/CSRF/ownership, schema/FK/cascade, UNKNOWN и server-derived pricing, guest consent, локальные mail permissions, generated DTO и тестовые изоляции. Изменения AGENTS/CODEX_PROMPT, V1–V4, frontend manifests/lockfile отсутствуют. В index ничего не добавлено. Полный рабочий список (M — изменён, ?? — новый):

```text
 M README.md
 M backend/pom.xml
 M backend/src/main/java/com/aicompass/compass/CompassApplication.java
 M backend/src/main/java/com/aicompass/compass/catalog/CatalogReadService.java
 M backend/src/main/java/com/aicompass/compass/config/OpenApiConfiguration.java
 M backend/src/main/java/com/aicompass/compass/config/SecurityConfig.java
 M backend/src/main/java/com/aicompass/compass/shared/web/ProblemDetailsAdvice.java
 M backend/src/main/resources/application.yml
 M backend/src/test/java/com/aicompass/compass/ScaffoldIT.java
 M backend/src/test/java/com/aicompass/compass/catalog/CatalogIT.java
 M backend/src/test/java/com/aicompass/compass/recommendation/RecommendationIT.java
 M docs/ARCHITECTURE.md
 M docs/PROGRESS.md
 M docs/openapi.json
 M frontend/src/api/generated/index.ts
 M frontend/src/api/generated/sdk.gen.ts
 M frontend/src/api/generated/types.gen.ts
 M frontend/src/app/App.tsx
 M frontend/src/app/Navigation.tsx
 M frontend/src/builder/Builder.module.css
 M frontend/src/builder/BuilderSession.tsx
 M frontend/src/builder/ResultsPage.tsx
 M scripts/e2e.py
 M scripts/local_processes.py
?? backend/src/main/java/com/aicompass/compass/identity/AdminBootstrap.java
?? backend/src/main/java/com/aicompass/compass/identity/AuthController.java
?? backend/src/main/java/com/aicompass/compass/identity/AuthDtos.java
?? backend/src/main/java/com/aicompass/compass/identity/AuthRateLimiter.java
?? backend/src/main/java/com/aicompass/compass/identity/IdentityGuard.java
?? backend/src/main/java/com/aicompass/compass/identity/IdentityJson.java
?? backend/src/main/java/com/aicompass/compass/identity/IdentityService.java
?? backend/src/main/java/com/aicompass/compass/identity/LocalMailSink.java
?? backend/src/main/java/com/aicompass/compass/portfolio/PortfolioController.java
?? backend/src/main/java/com/aicompass/compass/portfolio/PortfolioDtos.java
?? backend/src/main/java/com/aicompass/compass/portfolio/PortfolioService.java
?? backend/src/main/resources/db/migration/V5__persistent_sessions.sql
?? backend/src/main/resources/db/migration/V6__accounts_and_saved_profiles.sql
?? backend/src/test/java/com/aicompass/compass/identity/IdentityIT.java
?? frontend/src/account/Account.test.tsx
?? frontend/src/account/AccountPage.tsx
?? frontend/src/account/AuthPage.tsx
?? frontend/src/account/AuthSession.tsx
?? frontend/src/account/Dashboard.tsx
?? frontend/src/account/PersonalGate.tsx
?? frontend/src/account/SavePanel.tsx
?? frontend/src/api/account.ts
?? frontend/tests/e2e/zz-accounts.spec.ts
?? scripts/admin-bootstrap.sh
?? scripts/dev-mail.py
```


### Матрица приёмки P06

| Критерий | Результат и доказательство |
|---|---|
| Разрешённая зависимость/BOM/actual versions | PASS: только starter 4.1.2 в backend/pom.xml; JDBC/Core 4.1.1 через Boot BOM, dependency tree/compile/verify |
| PostgreSQL sessions/Flyway | PASS: V5/V6 на реальной PostgreSQL, initialize-schema=never, V1–V4 неизменны, Redis/новой постоянной БД нет |
| Сессии после restart и сохранность профиля | PASS: IdentityIT закрывает Spring-приложение и запускает заново с той же PostgreSQL; прежняя cookie авторизована и профиль читается |
| Logout/expiry/fixation/concurrent sessions/CSRF | PASS: HTTP + PostgreSQL IT; старые cookie/CSRF отвергаются, новые действуют; E2E logout/DB expiry; Secure/HttpOnly/Lax default проверен отдельно |
| Register/verify/resend/reset/expiry/reuse | PASS: 18 IdentityIT сценариев, UI/E2E; hashed token rows, local sink, reset отзывает обе concurrent sessions |
| Enumeration/rate limiting/USER–ADMIN/bootstrap | PASS: одинаковые ошибки/202, persisted counters, strict DTO, USER denied admin HTTP; verified/one-time local bootstrap в Testcontainers |
| Ownership/IDOR/mass assignment/Free one profile | PASS: реальные users A/B HTTP + E2E; foreign GET/PATCH/DELETE 404; race create 201/409; owner/role/Premium не принимаются от клиента |
| Сохранение профиля/результата/набора | PASS: реальный RecommendationProfile, server-derived immutable snapshot с versions/asOf/FX/UNKNOWN; catalog update не меняет историю; выбранный bundle проверен сервером |
| Guest → account и повторный подбор | PASS: 0 writes до согласия, SPA draft сохраняется через register/verify/login, явное сохранение; load/revisit/rerun; UI очищает previous account state |
| Frontend/API/OpenAPI | PASS: 7 account/auth страниц, loading/empty/errors, typed generated client, nullable Account исправлен и проверен, private cookie security + typed Problem Details |
| Export/delete/privacy | PASS: только свои данные, без hashes/tokens/sessions; password+confirm; cascade, session revoke, mail cleanup; общий каталог и guest preview сохраняются |
| Responsive/доступность | PASS в пределах локальных проверок: labels/focus, 360/768/1440 без overflow, 53 UI tests + E2E; независимый a11y audit не заявляется |
| Обязательные scripts и Git hygiene | PASS: check/catalog/smoke/E2E/secrets/diff; 204 backend +53 UI +34 E2E =291 тест, 0 failed/skipped в финальных suite |
| Внешние действия и граница этапа | PASS по scope review: LLM/payments/source-fetch OFF, email только sink; no commit/push/PR/deploy; P07 NOT_STARTED |

Ручной visual review: просмотрены реальные E2E `p06-account-360.png`, `p06-account-1440.png`, `p06-dashboard-360.png`: читаемые формы и тексты, перенос длинного синтетического email, disabled delete до подтверждения, корректный empty state, без обрезки/горизонтального overflow. Дополнительно автоматизированы ширины 768 и 1440 для dashboard. Скриншоты содержат только synthetic @example.invalid, без секретов:

- [Account 360](../.runtime/e2e-060325ade6a4/p06-account-360.png), [768](../.runtime/e2e-060325ade6a4/p06-account-768.png), [1440](../.runtime/e2e-060325ade6a4/p06-account-1440.png).
- [Dashboard 360](../.runtime/e2e-060325ade6a4/p06-dashboard-360.png), [768](../.runtime/e2e-060325ade6a4/p06-dashboard-768.png), [1440](../.runtime/e2e-060325ade6a4/p06-dashboard-1440.png).

Финальная сборка запущена в локальных процессах; повторный smoke 3/3 PASS. Compose `ai-compass-p01-postgres-1` running/healthy, PostgreSQL 17.12, bind 127.0.0.1:15432; frontend/backend 127.0.0.1:5173/18080. Только собственные ресурсы проекта, volume сохранён. Ветка `codex/p01-scaffold`; HEAD остаётся `8ec96c51f3b4316f7b4e06d72dd2ec0e57f1cce1`; 24 modified +25 untracked файлов P06, staged 0. Полный список выше. **P06 = AWAITING_REVIEW; P07 = NOT_STARTED. Обязательных незавершённых критериев P06 нет; приёмка владельца ещё не получена. Работа остановлена без commit/push/PR.**


## Окончательное закрытие P06 — 15.11.2026

Владелец предварительно принял этап и явно поручил только финальную проверку, исправление обнаруженных ошибок, статус DONE при успешных проверках и один локальный commit `feat: implement authentication and saved ai profiles`. P07 не разрешён. Исторические AWAITING_REVIEW/NOT_RUN commit выше относятся к предыдущей передаче.

Повторно прочитаны AGENTS.md, применимые workflow/security/privacy/P06 разделы CODEX_PROMPT.md, фактический PROGRESS и архитектура P06. Исходный HEAD `8ec96c51f3b4316f7b4e06d72dd2ec0e57f1cce1`, ветка `codex/p01-scaffold`. Рабочие изменения — ранее реализованный P06, посторонних изменений не выявлено.

### Результат security review и исправления

- Подтверждены две ошибки в IdentityGuard: HTTP 429 общего IP limiter терял Retry-After из-за записи заголовка после завершения response; GET/DELETE отдельного `/recommendations/{id}` не попадали в общий IP limiter. Два regression tests до исправления дали FAIL (2/2, exit 1): missing header и 404 вместо 429 при исчерпанном лимите. Исправлены порядок записи заголовка и распознавание personal recommendation routes; preview сохраняет прежний отдельный limiter. Ownership не обходился.
- Добавлены проверки конкурентного reset одним token (ровно 200+400, прежняя сессия отозвана), атомарного limiter (из 12 параллельных попыток ровно 5 разрешены, 7 получают 429; counter=12), абсолютного session lifetime 12h при свежей активности и idle timeout 30m.
- Проверка чистой PostgreSQL явно подтверждает отсутствие compass schema перед первым запуском, затем историю Flyway ровно 1–6, success и validate checksums. V1–V6 не редактировались при закрытии.
- Добавлены unit checks локального sink: default/prod/staging и смешанные dev+prod/test+staging не создают писем; symlink каталога не перенаправляет запись. Реальной сети/SMTP нет. Код создаёт каталог 0700 и файлы 0600; имеющиеся IT проверяют права файлов 0600, отсутствие секретов в output/export и удаление писем аккаунта.
- Перепроверены session persistence/logout/reset/fixation/CSRF, SQL owner predicates, Free UNIQUE+lock/revision, snapshot/stack ownership, password+confirm delete, cascade/revoke, server role и bootstrap, cookie defaults и локальный HTTP override. Новых прямых зависимостей, миграций или рефакторинга структуры нет.
- При подготовке дополнительного теста первая testCompile остановилась из-за package-private JdbcSession. Тест переведён на публичный SessionRepository; это ошибка тестового кода, не ошибка runtime. Ни неудачный compile, ни regression-before не считаются успешными проверками.

### Финальные результаты закрытия

| Команда / проверка | Результат |
|---|---|
| `bash scripts/check.sh` | PASS, exit 0: 118 unit +93 integration =211 backend; 53 UI; 0 failures/errors/skipped; install, format, lint, typecheck, build, actual OpenAPI/generated SDK comparison и schema checks PASS |
| `bash scripts/check-catalog.sh` | PASS, exit 0: synthetic/12 negative cases/reviewed/recommendation manifests |
| `bash scripts/smoke.sh` | PASS, exit 0: 3/3, после запуска окончательной сборки |
| `bash scripts/test-e2e.sh` | PASS, exit 0: 34/34, 0 failed/skipped, 6.8 минуты; isolated PostgreSQL/backend/browser/sink |
| `bash scripts/check-secrets.sh` | PASS, exit 0: secret patterns и фактический local password не найдены в кандидатах Git |
| `git diff --check` | PASS, exit 0 |
| Чистая PostgreSQL + Flyway | PASS: compass отсутствует перед первым стартом; применены V1–V6, checksums validate; initialize-schema=never |
| Cookie | PASS: реальный dev response HttpOnly=true/SameSite=Lax/Secure=false; ScaffoldIT проверяет Secure=true/HttpOnly/Lax по умолчанию; значения cookie не выводились |
| Email sink permissions | PASS: отдельная проверка фактического E2E каталога 0700 и всех писем 0600, без чтения содержимого в отчёт |

Всего **298 тестов**, 0 ошибок/пропусков в финальных suites. Добавлены 7 тестов: 5 PostgreSQL IT и 2 unit; IdentityIT теперь 23/23 PASS. Regression-before 2/2 FAIL зафиксирован отдельно, итог относится к повторному прогону после исправления.

Логи (ignored): `.runtime/p06-close-check.log`, `p06-close-catalog.log`, `p06-close-smoke.log`, `p06-close-e2e.log`, `p06-close-secrets.log`, `p06-close-regression-before.log`; E2E artifacts — `.runtime/e2e-c42159029dfe/`. Рабочие аккаунты/каталог E2E не изменяет; собственный временный контейнер и процессы завершены finally. Локальные frontend/backend оставлены работающими на loopback с окончательной сборкой.

### Итог безопасности и Git

Session restart/logout/reset/expiry/fixation и CSRF — PASS; concurrent reset/rate limiter/Free-profile creation — PASS; A/B ownership и mass assignment — PASS; local sink isolation/profiles/permissions — PASS; export/delete/cascade/revoke — PASS. Проверены backend owner predicates и отсутствие browser token storage. Новых блокеров нет.

Ручной review охватил код auth/filter/portfolio, схемы/FK/cascade, cookie defaults/dev override, местные mail scripts, весь набор Git. Для одного локального commit подготовлены ровно **50 файлов P06**: полный список предыдущей передачи выше плюс `backend/src/test/java/com/aicompass/compass/identity/LocalMailSinkTest.java`. При закрытии изменены только IdentityGuard (два исправления), IdentityIT (пять дополнительных проверок и clean DB assertion), новый LocalMailSinkTest и этот журнал. Секреты, .env, mail-файлы, caches, node_modules, логи и build artifacts не входят. AGENTS/CODEX_PROMPT, frontend manifests/lockfile и миграции не менялись при закрытии.

Разрешённое сообщение коммита: `feat: implement authentication and saved ai profiles`. Ветка `codex/p01-scaffold`. Hash и фактический результат Git сообщаются после выполнения коммита, без записи предполагаемого hash в этот файл.

NOT_RUN/ограничения сохраняются: удалённый CI, production HTTPS/proxy/pentest и юридическая проверка не выполнялись; Arch Playwright использует прежний Ubuntu fallback. Рабочий ADMIN bootstrap не запускался, механизм проверен на Testcontainers. Реальные email/платежи/платные API/deployment/push/PR запрещены и не выполнялись. Гостевой черновик остаётся в памяти вкладки; локальные письма приватны и требуют предусмотренного удаления/очистки.

**P06 = DONE. P07 = NOT_STARTED. Работа ограничена закрытием P06; после локального коммита остановиться.**


## P07 — начало реализации, 15.11.2026

Владелец явно принял P06 и разрешил P07 по вложенному поручению. Полностью перечитаны пять обязательных документов. Исходное дерево чистое, ветка codex/p01-scaffold, HEAD ffcde80860a3da71f28faa0bde29f056ab4dd348. Разрешены изменения backend/frontend/миграций после V6, документации и локальные проверки; commit до приёмки, новые прямые зависимости, push/PR/P08 и внешние действия запрещены. Исторические ограничения P06 выше не отменяют новое разрешение P07.

План: один user_subscription ledger с ownership/revision и явными неизвестными компонентами; повторное использование CostCalculator/RenewalSchedule/RecommendationEngine/coverage/evidence; CURRENT/OPTIMIZED с будущим графиком и без refund; typed API/generated SDK, страница подписок, export/delete, unit/PostgreSQL/UI/E2E. V1–V6 сохраняются. Проверки P07 пока NOT_RUN; прежние результаты не являются доказательством нового этапа.

### Промежуточные проверки P07

Реализованы реестр/V7, typed API, финансовый timeline на CostCalculator, композиция прежнего engine, frontend/generated SDK и тесты. Новых прямых зависимостей нет. Первые compile/check прогоны выявили и устранили: сравнение строкового Evidence.status с enum; устаревшее ожидаемое число миграций; ESLint запрет совместного экспорта фабрики формы и компонента (фабрика вынесена); несоответствие SubscriptionPlan.name вместо tier; форматирование новых TS файлов. Эти неуспешные команды не считаются PASS.

Первый денежный тест неверно ожидал неизвестную будущую цену уже отменённой без обязательств подписки: будущих платежей в таком сценарии действительно нет. Исправлена постановка: UNKNOWN amount/tax проверяются в CURRENT, неизвестные условия/расходы отмены — в CANCEL. UNKNOWN baseline по-прежнему даёт UNKNOWN savings, а не вымышленный ноль.

Общий PostgreSQL regression воспроизвёл прежнюю гонку CatalogImportService: identical concurrent import мог конфликтовать по unique content_hash, тогда как ON CONFLICT покрывал только id. Теперь обрабатываются оба unique conflicts, после чего обязательно сверяются exact id/hash и sealed state. Существующая конкурентная проверка усилена до пяти повторений; версии/публикация/immutable данные не изменяются.

Ручной review P07 нашёл две ошибки CURRENT: budget и maxTools/exclusivity не входили в итог requirementsMet. Новые два теста до исправления дали 2 FAIL /exit1 (`p07-current-regression-before.log`); после повторного использования BundleSearch.incompatible и проверки budget/maxTools все 23 SubscriptionOptimizerTest PASS /exit0 (`p07-current-regression-after.log`). Обычный гостевой engine не изменил правила/версию.

Промежуточный полный `check.sh` (`p07-check-final.log`) PASS /exit0: 139 unit +100 PostgreSQL integration, 59 UI, OpenAPI/SDK, format/lint/types/build/catalog. После последующих регрессий и улучшений графика выполняется повторный check; этот результат не выдаётся за окончательную проверку. E2E P07 пока NOT_RUN. Статус остаётся IN_PROGRESS; P08 NOT_STARTED; commit/staging/push/PR не выполняются.

### Проверенная сборка P07 и запуск перед E2E

`bash scripts/check.sh` (`.runtime/p07-check-acceptance.log`) — PASS /exit0: **143 unit +100 PostgreSQL integration +59 UI**, failures/errors/skipped=0. Проверены clean compilation/package, npm ci существующего lockfile, OpenAPI/client drift, format/lint/typecheck/build и schema. Из новых unit cases 25 относятся к оптимизатору; дополнительно 80 seeded итераций внутри одного теста, не отдельные JUnit cases. В 100 IT входят 26 IdentityIT и 42 CatalogIT (включая пять повторений concurrent import), прежние suites сохранены.

Дополнительный review выявил предоплаченный доступ с autoRenew=NO: прежняя новая проверка P07 отвергала такой CURRENT даже при paidUntil на конец горизонта. Регрессия сначала FAIL 1/1 (`p07-prepaid-regression-before.log`), после исправления входит в 25/25 PASS. Неизвестные переходные расходы теперь также не позволяют утверждать requirementsMet; сравнение может показывать условный вариант с UNKNOWN. Детерминированность проверена при разном порядке map entries. Одна промежуточная проверка lint обнаружила неиспользованный словарь подписи действий, он подключён к UI; финальный check PASS.

После сборки `python3 scripts/local_processes.py stop/start` через common.sh — PASS /exit0: только собственные процессы, backend/frontend loopback. `bash scripts/smoke.sh` — PASS /exit0, 3/3. Read-only SQL: Flyway V1–V7 success, 35 таблиц compass, user_subscription пуст (0), рабочий каталог r2 UUID 03000000-0000-4000-8000-000000000002/revision2/3 products неизменён. PostgreSQL healthy на 127.0.0.1:15432; frontend/backend 5173/18080.

Отдельно `bash scripts/check-catalog.sh`, `bash scripts/check-secrets.sh`, `git diff --check` — PASS /exit0. Git candidate audit: 37 файлов, 0 staged, нет .env, symlinks, кешей, логов, node_modules, target/dist и screenshots. Сравнение с HEAD подтверждает неизменность AGENTS/CODEX_PROMPT, manifests/lockfile и V1–V6. HEAD ffcde80860a3da71f28faa0bde29f056ab4dd348, ветка прежняя.

E2E выполняется отдельным обязательным прогоном (`.runtime/p07-e2e.log`, 38 сценариев в suite). До завершения этого прогона и просмотра артефактов статус P07 остаётся IN_PROGRESS.

### Дополнительные регрессии перед передачей P07

Первый E2E завершился exit 1: 37 PASS, 1 FAIL (mustKeep checkbox). Контролируемый checkbox возвращался к прежнему значению до ответа API. Отдельный UI test с задержанным PATCH воспроизвёл ошибку (6 PASS /1 FAIL, `.runtime/p07-toggle-before.log`). Исправлено немедленное отображение выбора с блокировкой повторного запроса и откатом при HTTP 409/ошибке. До завершения запроса также блокируются профиль/режим сравнения и поля сохраняемой формы, чтобы ответ не относился к уже изменённым настройкам. После исправления 7/7 UI PASS (`p07-toggle-after.log`). E2E дополнительно ждёт завершения сохранения после обратного переключения.

Проверка некорректного календаря воспроизвела две ошибки: неизвестная IANA zone не переводилась в HTTP 400, год за пределами четырёхзначного ISO проходил валидацию перед V7. Два параметризованных теста сначала FAIL 2/2 (`p07-calendar-before.log`), после исправления все 27 SubscriptionOptimizerTest PASS (`p07-calendar-after.log`). При первом отдельном запуске Maven был ошибочно вызван без проектного helper и остановился на недоступном default cache; повтор выполнен через `maven` из common.sh с `.cache/m2`, глобальная конфигурация не изменялась.

Просмотрены реальные desktop/mobile screenshots первого E2E. Layout без overflow; длинные названия переносятся, CURRENT/OPTIMIZED складываются в колонку на 360px. Найденные технические идентификаторы в названиях инструментов заменены отображаемыми названиями; денежная арифметика остаётся только на сервере. Финальный полный check и повторный E2E выполняются после этих изменений. До их завершения P07 остаётся IN_PROGRESS.

Повторный `check.sh` (`p07-check-release.log`) PASS: 145 unit +100 IT +60 UI. Smoke после перезапуска — 3/3 PASS. При окончательном review найдено ошибочное отнесение всех прогнозируемых продлений к неизбежным платежам при неизвестной доступной дате отмены. Это не меняло baseline horizon total, но завышало достоверность mandatoryTotal. Регрессия сначала 1 FAIL (`p07-mandatory-before.log`); исправлено на UNKNOWN для обязательной части и отдельное пояснение. Возвраты и возможность отмены по-прежнему не предполагаются. Уже запущенный повтор E2E (`p07-e2e-final.log`) штатно остановлен через SIGTERM собственного runner (exit 130, не PASS), чтобы полный финальный прогон проверял эту исправленную сборку. Собственные временные процессы/контейнер завершены finally; постоянные volumes не удалялись.

### Состав изменений P07 для review

Всего 37 файлов; ничего не staged. Новых зависимостей или lockfile изменений нет.

```text
README.md
backend/src/main/java/com/aicompass/compass/catalog/CatalogImportService.java
backend/src/main/java/com/aicompass/compass/config/OpenApiConfiguration.java
backend/src/main/java/com/aicompass/compass/config/SecurityConfig.java
backend/src/main/java/com/aicompass/compass/identity/IdentityGuard.java
backend/src/main/java/com/aicompass/compass/identity/IdentityJson.java
backend/src/main/java/com/aicompass/compass/identity/IdentityService.java
backend/src/main/java/com/aicompass/compass/portfolio/PortfolioController.java
backend/src/main/java/com/aicompass/compass/portfolio/PortfolioDtos.java
backend/src/main/java/com/aicompass/compass/portfolio/SubscriptionController.java
backend/src/main/java/com/aicompass/compass/portfolio/SubscriptionDtos.java
backend/src/main/java/com/aicompass/compass/portfolio/SubscriptionService.java
backend/src/main/java/com/aicompass/compass/recommendation/CandidateResolver.java
backend/src/main/java/com/aicompass/compass/recommendation/CostCalculator.java
backend/src/main/java/com/aicompass/compass/recommendation/OptimizationResult.java
backend/src/main/java/com/aicompass/compass/recommendation/PreviewLoadGuard.java
backend/src/main/java/com/aicompass/compass/recommendation/RecommendationEngine.java
backend/src/main/java/com/aicompass/compass/recommendation/SubscriptionOptimizer.java
backend/src/main/java/com/aicompass/compass/recommendation/SubscriptionTerms.java
backend/src/main/resources/application.yml
backend/src/main/resources/db/migration/V7__subscription_ledger.sql
backend/src/test/java/com/aicompass/compass/catalog/CatalogIT.java
backend/src/test/java/com/aicompass/compass/identity/IdentityIT.java
backend/src/test/java/com/aicompass/compass/recommendation/SubscriptionOptimizerTest.java
docs/ARCHITECTURE.md
docs/PROGRESS.md
docs/openapi.json
frontend/src/api/account.ts
frontend/src/api/generated/index.ts
frontend/src/api/generated/sdk.gen.ts
frontend/src/api/generated/types.gen.ts
frontend/src/app/App.tsx
frontend/src/app/Navigation.tsx
frontend/src/subscriptions/Subscription.test.tsx
frontend/src/subscriptions/SubscriptionPage.tsx
frontend/src/subscriptions/form.ts
frontend/tests/e2e/zzz-subscriptions.spec.ts
```

### Контрольные примеры P07 — только synthetic fixtures

1. Три личные подписки по 12, 8 и 5 USD/месяц, 12 месяцев, известные даты, отмена доступна сейчас, без обязательств/дополнительных/переходных расходов: CURRENT 300 USD будущих списаний. Synthetic free learner+slides покрывают выбранные потребности за 0 USD; потенциальная разница 300 USD, ESTIMATED_USER_REPORTED, confirmedSavings=null. Функции личных продуктов остаются UNKNOWN; взаимозаменяемость не выдумывается. В браузерном сценарии первая цена редактируется с 12 до 10: проверяется 276 USD, а не 300.
2. Synthetic Creator с личной ценой 1 USD/месяц покрывает требования редактора: CURRENT 12 USD за год, решение KEEP_CURRENT. Публичная цена каталога не подменяет фактическую цену владельца.
3. Неизвестная цена/FX или условия отмены не позволяют подтвердить соответствующий расход/экономию: null + UNKNOWN, без фиктивного нуля или диапазона. Уже оплаченные 120 USD показываются отдельно и никогда не прибавляются к будущей экономии.

### Матрица реализации P07 (итог проверок — ниже)

| Требование | Реализация и доказательство |
|---|---|
| Архитектура / reuse | SubscriptionOptimizer композирует engine, resolver, coverage, usage, evidence, Money/FX/RenewalSchedule; CostCalculator расширен; новых прямых зависимостей нет |
| Личный ledger / custom | Авторизованный CRUD, статус, actual price USER_REPORTED/CATALOG/UNKNOWN, currency, MONTH/YEAR, anchor/renewal/timezone, paid/trial/autoRenew/commitment/mustKeep/extras; custom функции UNKNOWN |
| Database | V7, одна user_subscription; owner cascade, plan/offer FK, unique, numeric money/checks, generated dates/costs, индекс owner, revision/lock; V1–V6 неизменны |
| Финансовые события | Paid отдельно; future/cash now/monthly/mandatory/transition/next payment; полуоткрытый календарный горизонт, годовая предоплата, leap/31st/timezone, нет refund, extras/FX UNKNOWN; 28 optimizer unit cases и 80 seeded итераций |
| CURRENT / OPTIMIZED | REVIEW/AUGMENT, mustKeep, budget/upfront/maxTools/eligibility/shared quotas, current cheaper → KEEP_CURRENT; неполные варианты не подтверждают requirementsMet |
| Overlap / потери | Подтверждённые capability claims и evidence IDs; INSUFFICIENT_DATA/BLOCKED_BY_REQUIREMENTS; альтернативы показаны отдельно; потери по всем известным функциям каталога, без процента качества/дублирования |
| Экономия | Разница future totals одной валюты/горизонта, учитывает обязательства и переход; неизвестное остаётся UNKNOWN; known projection ESTIMATED_USER_REPORTED, confirmedSavings=null |
| API / client | GET/POST collection, GET/PATCH/DELETE item, POST analyze с обоими сценариями и объяснениями; typed OpenAPI/generated SDK; 400/401/403/404/409/429/500, no-store |
| Dashboard | /subscriptions: CRUD/toggle, prefill/manual override, profile reuse, optimize/compare/details/evidence, loading/empty/error/retry, responsive 360/768/1440 |
| UX / provenance | Цена из каталога не повышает личную цену до VERIFIED_SOURCE; поздний ответ не подменяет новые параметры; ошибка toggle откатывает состояние; удаление записи не означает provider cancel |
| Business scope | Compass fee прежней server policy учитывается, сейчас 0 USD; нет Premium billing, мониторинга, уведомлений, auto-cancel или гарантии экономии |
| Privacy / security | Session/CSRF/owner predicates, A/B HTTP, strict DTO, body/account/IP/load limits, concurrent revisions 200/409, export/cascade/delete/revoke, без browser token storage и bank/provider credentials |
| Unit / PostgreSQL / UI | Сохранены P01–P06 suites; новые денежные/calendar/unknown/no-change/shared/agent/ownership/concurrency tests; реальная PostgreSQL Testcontainers, не H2 |
| Browser | Четыре P07 E2E cases объединяют все 12 запрошенных сценариев; собственные tmpfs DB/catalog/accounts/sink; окончательный результат фиксируется после завершения |
| Обязательные команды | check/catalog/smoke/E2E/secrets/diff выполняются; прерванные/неуспешные прогоны выше не считаются PASS |
| Documentation | README: запуск/user flow; ARCHITECTURE: model/rules/API/security/limits; PROGRESS: команды, факты, файлы, примеры и матрица |
| Git / ограничения | Исходная ветка; staged 0; нет commit/push/PR/deploy/real email/payments/paid APIs, V1–V6/manifests/lockfile сохранены; P08 не начат |

Ограничения: рабочий каталог r2 содержит только 3 продукта и неполные региональные цены/налоги/лимиты; tests используют явно synthetic данные. Реальные рыночные источники в P07 не обновлялись. Поиск ограничен каталогом и прежними стратегиями engine, глобальный минимум не обещается. Новые покупки начинаются сейчас; автоматический оптимальный отложенный старт не реализован. Actual costs, даты и условия — сведения пользователя, не банковское подтверждение. Usage/top-ups вводятся суммой за период или UNKNOWN. История optimizer автоматически не записывается: ответ содержит входы/versions/FX для replay, существующие профили P06 переиспользованы. Максимум 8 записей. При конфликте редактирования нужно обновить данные и заново открыть запись, черновик не перезаписывается молча.

NOT_RUN / NOT_APPLICABLE: remote CI, production HTTPS/proxy/pentest, независимый a11y/privacy/legal audit и процент coverage не выполнялись; локальные regression tests не являются такими аудитами. Сохраняется ранее разрешённый Playwright Ubuntu fallback на Arch. Реальные платежи/email/API/deploy, P08 monitoring и внешние подписочные действия не входят в P07 и не выполнялись.

### Финальная сборка после исправления обязательных платежей

`bash scripts/check.sh` (`.runtime/p07-check-final-suite.log`) — PASS, exit 0: **146 unit +100 PostgreSQL integration +60 UI**, 0 failures/errors/skipped. Включены 28 cases SubscriptionOptimizerTest и прежние 80 seeded итераций внутри одного case. Backend clean compilation/package, existing-lock npm ci, live OpenAPI/generated SDK drift, format, lint, typecheck, frontend build и catalog schema PASS. Дополнительных зависимостей нет.

В IT логе имеются сообщения Spring Session cleanup о недоступной БД после остановки контейнера предыдущего cached test context. Они относятся к фоновому завершению тестового окружения, не к работающей PostgreSQL приложения; JUnit IT 100/100 PASS. Ошибки тестов не скрывались, лог сохранён. Новый полный E2E использует эту финальную сборку (`p07-e2e-acceptance.log`); до его завершения передача AWAITING_REVIEW не объявляется.

`python3 scripts/local_processes.py stop/start` после финальной сборки — PASS; `bash scripts/smoke.sh` (`p07-smoke-acceptance.log`) — PASS, exit 0, 3/3. PostgreSQL running/healthy, 127.0.0.1:15432; frontend/backend loopback 5173/18080. Read-only SQL подтвердил Flyway V1–V7 success и 0 user_subscription rows в рабочей БД. Первая диагностическая SQL-команда ошибочно указала compass.flyway_schema_history и получила relation-not-found; исправлен только запрос на public.flyway_schema_history, схема/данные не менялись.

Отдельные `bash scripts/check-catalog.sh`, `bash scripts/check-secrets.sh`, `git diff --check` после исправления — PASS, exit 0 (`p07-catalog-acceptance.log`, `p07-secrets-acceptance.log`). Git audit: 37 candidates, 0 forbidden files, 0 staged; исходные AGENTS/CODEX_PROMPT, manifests/lockfile, V1–V6 неизменны. HEAD остаётся ffcde80860a3da71f28faa0bde29f056ab4dd348, ветка codex/p01-scaffold.


### Итоговая передача P07 — AWAITING_REVIEW

Все обязательные критерии матрицы P07 выше выполнены; незавершённых обязательных критериев нет. Это передача на review, не самостоятельная приёмка этапа.

| Финальная команда / проверка | Фактический результат |
|---|---|
| `bash scripts/check.sh` | PASS, exit 0: 146 unit +100 PostgreSQL integration +60 UI, 0 failures/errors/skipped; compilation/package/install/format/lint/typecheck/build/OpenAPI+SDK/catalog PASS |
| `bash scripts/check-catalog.sh` | PASS, exit 0: synthetic schema, 12 negative cases, оба reviewed manifests и recommendation fixture |
| `bash scripts/smoke.sh` | PASS, exit 0: 3/3, финальная сборка работает на loopback |
| `bash scripts/test-e2e.sh` | PASS, exit 0: 38/38, 0 failed/skipped, 10.9 min; все 12 сценариев P07 внутри четырёх cases |
| `bash scripts/check-secrets.sh` | PASS, exit 0; secret-pattern/local-password scan, значения не выводились |
| `git diff --check` | PASS, exit 0 |
| Flyway / PostgreSQL | PASS: V1–V7 на чистых Testcontainers и локальной БД; V1–V6 неизменны, одна новая таблица/35 всего в compass; own PostgreSQL healthy |
| Security / snapshots | PASS: P06 regression + subscription owner A/B, CSRF, optimistic conflict, stale catalog/revisions 409, rate limit 429, export/cascade/delete, private no-store |
| Visual / responsive | PASS в пределах локальной проверки: фактические screenshots 360/768/1440 просмотрены, длинные названия переносятся, UNKNOWN/estimated savings видимы, overflow/pageerror отсутствуют |

Всего **344 теста**, 0 failures/errors/skipped в финальных suites. Smoke 3/3 и schema checks дополнительно, не прибавлены к числу тестов. Числа unit/IT независимо сверены с Surefire/Failsafe XML. Неуспешный первый E2E и прерванный промежуточный прогон сохранены выше и не выданы за PASS.

Итоговые логи (ignored): `.runtime/p07-check-final-suite.log`, `p07-catalog-acceptance.log`, `p07-smoke-acceptance.log`, `p07-e2e-acceptance.log`, `p07-secrets-acceptance.log`. Окончательные screenshots: [360](../.runtime/e2e-9473658498bd/p07-subscriptions-360.png), [768](../.runtime/e2e-9473658498bd/p07-subscriptions-768.png), [1440](../.runtime/e2e-9473658498bd/p07-subscriptions-1440.png). Только synthetic данные, auth trace/video OFF; артефакты не входят в Git.

Git: `codex/p01-scaffold`, HEAD `ffcde80860a3da71f28faa0bde29f056ab4dd348` (последний P06); 25 modified +12 new files P07, staged 0. Секреты/.env/cache/logs/node_modules/build artifacts не входят. Манифесты/lockfile, AGENTS/CODEX_PROMPT и V1–V6 не изменены. Commit, push, PR и deployment не выполнялись. Финальная локальная сборка оставлена работающей; изолированные E2E процессы и собственный tmpfs контейнер завершены finally. В рабочем backend логе ERROR=0; тестовый cleanup warning описан отдельно выше.

**P07 = AWAITING_REVIEW. P08 = NOT_STARTED. Работа остановлена до решения владельца.**


## Закрытие P07 и начало разрешённого пакета P08–P11 — 15.11.2026

Владелец явно сообщил «P07 ПРИНЯТ по предоставленному отчёту», поручил закрыть P07 без отдельного commit и последовательно выполнить пакет до P11. Для этого поручения отменены промежуточные остановки ради приёмки успешно проверенных фаз: после обязательных проверок можно ставить DONE и переходить дальше. Это явное разрешение владельца, а не самостоятельная смена workflow. Общий AWAITING_REVIEW ставится только после выполнения пакета; P12 не разрешён.

Прочитаны AGENTS.md, общие/product/security/testing инварианты и P07–P11 в CODEX_PROMPT.md, текущие статусы и весь протокол P07 в PROGRESS.md, ARCHITECTURE.md. Проверены текущие код ledger/cost calculator, Git и сохранённые фактические отчёты. Исторические AWAITING_REVIEW/запрет P08 выше относятся к предыдущим поручениям и не отменяют новую авторизацию.

### P07 = DONE

- Сохранённые Surefire XML: 146 unit, failures/errors/skipped=0; Failsafe XML: 100 PostgreSQL integration, failures/errors/skipped=0. Лог окончательного check.sh подтверждает BUILD SUCCESS, 60 UI PASS, live OpenAPI/client drift и frontend checks/build. Лог окончательного E2E подтверждает 38 PASS за 10.9 min. Итого 344 теста прежнего принятого прогона; это проверка артефактов, не новый запуск этих suites.
- Повторно выполнены сейчас `bash scripts/check-catalog.sh` — PASS/exit0; `bash scripts/smoke.sh` — PASS/exit0, 3/3; `bash scripts/check-secrets.sh` — PASS/exit0; `git diff --check` — PASS/exit0.
- Полные check.sh/test-e2e.sh при этом документальном закрытии не перезапускались: application code/тесты не редактируются. Новых исправлений или миграций нет. Предыдущие исправления, failed-before evidence, финансовые примеры, security results, screenshots, ограничения Playwright/CI/данных и test-cleanup logging сохранены выше.
- Git: codex/p01-scaffold, HEAD ffcde80860a3da71f28faa0bde29f056ab4dd348; 37 изменённых/новых файлов P07, staged 0. Candidate audit: forbidden files 0. AGENTS/CODEX_PROMPT, manifests/lockfile и V1–V6 неизменны. При этом закрытии меняется только этот PROGRESS.md. Commit не создан.

### Блокер пакетного плана

`AI_COMPASS_BATCH_P07_P11.md` отсутствует в корне репозитория. Поиск точного имени по доступному /home/mrturtle, поиск имён в AITU/attachments/tmp и поиск ссылки в доступных .md/.txt репозитория и вложений файл не обнаружили. Нельзя утверждать, что он прочитан, или подменять назначенный владельцем основной план прежним общим описанием этапов. Пользователю запрошен доступный путь либо содержание документа.

P08 = BLOCKED (подготовка; реализация не начата). P09–P11 = NOT_STARTED: причины для конкретного P09 DEFERRED ещё не оценены по отсутствующему плану. Готовность закрытой beta не подтверждена; новых admin/monitoring/LLM/entitlement/analytics функций в этой части работы нет. Обязательные проверки будущих фаз NOT_RUN, потому что фазы не реализованы. Новых внешних интеграций, прямых зависимостей, commit/push/PR/deployment и P12 нет.

**Общий статус пакета: BLOCKED до получения основного файла. Разрешение продолжать P08–P11 уже получено и не требует повторного запроса приёмки P07.**

## Продолжение пакета P08–P11 — 15.11.2026

Полностью прочитаны AGENTS, CODEX_PROMPT, новый AI_COMPASS_BATCH_P08_P11, PROGRESS и ARCHITECTURE. Предыдущий блокер снят. P07 DONE сохраняется без повторной реализации; исходные 37 изменений и добавленный владельцем план сохранены. Ветка codex/p01-scaffold, HEAD ffcde80860a3da71f28faa0bde29f056ab4dd348, индекс пуст. Новые фазы до review не объявляются DONE. План: защищённые draft/review/publish → семантические изменения и личные уведомления → локальные права beta → QA/docs. Новых прямых зависимостей не требуется. Проверки новых фаз пока NOT_RUN.

### P08 — первые вертикальные срезы

Добавлены V8 (catalog_draft/admin_audit), V9 (watched_stack/monitor_job/change_notice/notification_outbox), защищённые admin draft→validate→review→publish и личные monitoring endpoints. Существующие catalog_review/publication/active повторно используются, V1–V7 неизменны. Изменение черновика сбрасывает review; публикация проверяет active revision. Необходима текущая server ADMIN роль; источники не загружаются.

Очередь использует append-only publication как event source; UNIQUE(watch,revision), PostgreSQL claim/lease/token, atomic notice+completion, 5 попыток, backoff, восстановление истёкшей lease. Сначала сравниваются семантические факты нужных планов/страны, движок запускается только при изменениях. Один job/minute/instance, повторный расчёт одного watch не чаще минуты. Письма только LOCAL_ONLY outbox с explicit opt-in; подтверждённая отписка отменяет outbox. Применение — только self-reported.

Фактические промежуточные проверки: compile PASS; admin PostgreSQL HTTP 2/2 PASS после исправления порядка role/body-size (первый 1 PASS/1 FAIL); semantic unit 5/5 и monitoring PostgreSQL 2/2 PASS (`p08-monitor-retry.log`), UI 5/5 + lint/typecheck PASS (`p08-ui-tests-retry.log`). Первый semantic run 4 PASS/1 FAIL: неизменённое неизвестное предложение мешало подавлению малого изменения цены; исправлен отбор только изменённых offers. Первый monitoring IT 1 PASS/1 FAIL: fixtures предыдущего сценария оставляли активный watch; setup теперь явно отключает наблюдение предыдущего сценария, production limits не менялись. UI test typecheck сначала выявил неподдерживаемый Testing Library option exact, исправлен selector.

OpenAPI/generated SDK получены из реального ScaffoldIT; admin/inbox UI используют SDK, отдельного HTTP/fake data слоя нет. Полный check.sh ещё выполняется: обнаружено старое ожидаемое число миграций (7 вместо новых 9), исправлено; этот неуспешный запуск не считается PASS. P08 остаётся IN_PROGRESS до полного check/E2E/smoke/security. Никаких новых прямых dependencies, commits/staging/push/PR/внешней почты.

P08 полный повторный `check.sh` PASS/exit0 (`p08-check-retry.log`): 151 unit +104 PostgreSQL IT +65 UI, failures/errors/skipped=0; build/install/format/lint/typecheck/OpenAPI/schema PASS. Первый полный check FAIL только на оставшемся assertion 7 миграций вместо 9; остальные 103 IT прошли. После исправления проверены 41 таблица compass, V1–V9 и отсутствие auto-seed. Runtime перезапущен только собственными local_processes: smoke 3/3, check-catalog, check-secrets, diff --check PASS/exit0.

Дополнительный `CatalogChangesTest` теперь 7/7 PASS (`p08-fx-tests.log`): регион не выводится из валюты, fresh direct FX позволяет применить порог, stale FX не выдаётся за известную оценку. Это два новых случая сверх полного check, а не повторное суммирование семи прежних. Дополненные monitoring IT 2/2 PASS (`p08-monitor-privacy.log`): экспорт watches/notices, delete каскадно удаляет jobs/outbox, после пятой неуспешной попытки FAILED. Production JAR не пересобирался во время E2E; эти дополнительные проверки меняли только tests. Полный E2E с 39 cases ещё выполняется; AWAITING_REVIEW пока не заявляется.

Полный P08 E2E (`p08-e2e.log`) PASS: 39/39 за 13.0 min. Scheduler работал штатно; диагностическое чтение до очередного минутного tick показало пустую очередь, последующий tick доставил ровно 1 notice нужному owner, другой получил 0. Попытка read-only jcmd пришлась уже на штатное завершение E2E процесса и вернула non-existent PID; это не сбой приложения.

Последующий review: новый regression fee-before действительно FAIL 1/1 — малая known комиссия обходила финансовый порог. Исправлено: semantic diff отделён от notification significance; все price/tax/fee/upfront компоненты проверяются по одному порогу, а изменение пригодности/решения engine не скрывается порогом. Budget 15 → цена 15.10 даёт предупреждение о потере соответствия. 9 CatalogChangesTest PASS (`p08-financial-policy.log`). Дополнительно исправлены nullable Active.versionId, UTF-8 byte limit (413 до DB constraint), экспорт без inbox LIMIT100 (регрессия 101 notice), указание горизонта и DEMO в UI. V8/V9 уже применены и с этого момента не редактируются.

Повторный backend в `p08-check-final.log` PASS: 155 unit +105 IT (0 failures/errors/skipped), OpenAPI PASS. Общий script остановился на frontend typecheck: визуальная правка временно оставила неиспользуемый Link import; link implementation завершена, запускаются frontend checks и полный check повторно. Это не выдаётся за успешный check.sh. Визуально найден checkbox, растянутый общим catalog CSS: добавлены scoped overrides, подписи цен заменены читаемыми названиями, product links pinned к snapshot. Для точечной проверки E2E runner теперь передаёт аргументы Playwright; вызов без аргументов по-прежнему выполняет всю suite.

Cached Spring test contexts теперь явно закрываются AFTER_CLASS (DirtiesContext) вместе со своими ресурсами: прежние фоновые cleanup ошибки после остановки PostgreSQL не маскируются, lifecycle будет перепроверен полным check. Никаких проверок не отключено.

### P08: итог фазы A (15.11.2026)

`bash scripts/check.sh` (`.runtime/p08-check-release.log`) exit 0: 155 unit, 105 PostgreSQL integration, 65 UI; 0 failures/errors/skips, lint/typecheck/build/API drift PASS. После @DirtiesContext нет ERROR сообщений закрытых test DB. `bash scripts/test-e2e.sh` ранее 39/39 PASS; финальный `bash scripts/test-e2e.sh tests/e2e/zzzz-monitoring.spec.ts` 1/1 PASS, 2.0 min (не дополнительные 40 уникальных тестов). Реальная публикация → worker → ровно один impacted inbox, без уведомления незатронутого пользователя. Скриншоты `.runtime/e2e-146d0d47e38c/p08-{admin,inbox}-{360,768,1440}.png`; 360 inbox просмотрен: нет overflow, DEMO и период видны. Названия тарифов загружаются отдельно по pinned version, при отсутствии ответа явное «пока недоступно».

Локальные процессы перезапущены с собранным P08 JAR; smoke 3/3, check-catalog, check-secrets и diff --check exit 0. Политика значимости: semantic diff сначала отсекает нерелевантные/оформительские изменения, затем engine пересчитывает затронутое; порог цены применяется после проверки изменения допустимости/решений. Малое повышение, нарушающее budget, не скрывается. Полные counts включают regression для small fees, budget crossing, UTF-8 size и export 101 уведомления при UI limit 100.

P08 = IMPLEMENTED_PENDING_OWNER_REVIEW. P09 = DEFERRED: существующие structured deterministic explanations достаточны, внешние LLM не нужны и OFF. P10.b = DEFERRED до отдельного выбора PSP и разрешения. Начата только P10.a: Free 1 профиль/current result, Premium beta 5 профилей/history/5 watches; fee 0, без оплаты. P07 сохранён без повторной реализации, commit/staging/push не выполнялись.

### P10.a: вертикальный срез прав

Добавлены V10 `beta_grant`/`beta_grant_audit`, серверный сервис/контроллер, ownership/expiry/revocation/idempotency/CAS/owner locks, Free profile/history и Premium monitoring gates, export/cascade, `/pricing` и `/account/billing` с ADMIN issue/revoke. Прямые зависимости/manifests/lockfile не менялись. V1–V9 сохранены.

Первый targeted command с ошибочным `-Dtest=IdentitySettingsTest` остановился до IT: такого класса нет. Исправлено на `IntegrationSettingsTest`. Второй запуск: 5 unit PASS, 35 IdentityIT — 34 PASS / 1 ERROR (старый bootstrap test увидел ADMIN из предыдущего нового сценария). Это изоляция fixture, не обход bootstrap: BeforeEach теперь сбрасывает роли только в принадлежащей тесту PostgreSQL, где каждый сценарий сам создаёт ADMIN. Production bootstrap не изменён. Повтор и общие проверки ещё ожидаются.

P10.a промежуточные проверки: `.runtime/p10-check.log` backend 155 unit +109 PostgreSQL IT PASS (0 failures/errors/skips), frontend typecheck FAIL выявил коллизию simple-name `Audit` между каталогом и beta, и ссылку на catalog `plans` вместо billing operation. Исправлено на `BetaGrantAudit`/`BetaPlan`/`betaPlans`; в ScaffoldIT добавлены assertions обеих схем, operationId, nullable expiry. `generate-api.sh` после исправления exit 0 (5 unit +8 ScaffoldIT). Также удалён неподдерживаемый Testing Library `exact` (строковое name уже точное). `.runtime/p10-ui-retry.log`: typecheck/lint PASS, 22/22 целевых UI tests PASS. Premium SavePanel теперь позволяет явно выбрать существующий либо отдельный профиль; regression подтверждает POST revision0 вместо перезаписи. Финальный полный check ещё выполняется.

P10.a `.runtime/p10-check-release.log`: полный `bash scripts/check.sh` exit 0, 155 unit +109 PostgreSQL IT +71 UI, 0 failures/errors/skips; lint/typecheck/format/build/live OpenAPI drift/catalog/secrets PASS. V10 применяется к локальной DB при restart; после этого V10 неизменяема. Запущен полный `bash scripts/test-e2e.sh` (40 scenarios), результат ещё не объявляется. Код/JAR заморожены на время E2E.

P10.a дополнительные обязательные команды после restart: smoke 3/3, check-catalog (schema +12 negative +reviewed/synthetic manifests), check-secrets и git diff --check — exit 0. V10 успешно применена локально; менять её содержимое далее нельзя. Ветка codex/p01-scaffold, индекс пуст; manifests/lockfile и V1–V6 без diff. Ручной P10 security review: backend strict JSON/DB role/owner lock/CSRF, owner export/delete и worker expiry recheck просмотрены; это внутренний review, не независимый pentest. Full E2E ещё выполняется.

### Фаза B завершена локально; начало P11

Полный `bash scripts/test-e2e.sh` (`.runtime/p10-e2e.log`) exit 0: **40/40 PASS, 14.1 min**. Все P01–P08 сценарии плюс настоящий admin issue → Premium → revoke → Free, fake success denial, USER admin denial, CSRF/consent, собственный audit/export и logout. P10 screenshots `.runtime/e2e-4f4d0735c915/p10-billing-{360,768,1440}.png`; 360 просмотрен. Portfolio 1440 также просмотрен: расходы/prepaid/UNKNOWN/self-reported savings разделены. Все обязательные команды фазы B завершены успешно. P10.a = IMPLEMENTED_PENDING_OWNER_REVIEW, P09/P10.b = DEFERRED.

P11 начат: минимальные opt-in daily product events без профилей/email/IP, export/delete/withdrawal, local supply-chain/status/persistence checks, T01–T38, RUNBOOK/DATA_POLICY/RELEASE_CHECKLIST. Запрошено отдельное разрешение на независимого read-only агента; ответа пока нет, агент не запускается. Собственный review не будет выдаваться за независимый. Процент coverage, внешний CVE feed, remote CI, pentest/privacy/legal/production review пока NOT_RUN/NOT_VERIFIED.

### P11: implementation and targeted verification

V11 adds consent and three daily event types only; no profile/email/IP/free-form/payment fields. Configure/collect/delete share the owner lock, daily uniqueness deduplicates, 30-day read window plus daily cleanup, withdrawal removes stored events. Server save/read acknowledgement emits events only after consent; account revisit is opt-in. `/account` exposes consent OFF by default; export includes own preferences/events. Explicit «Прочитано» does not mark a recommendation applied.

`.runtime/p11-identity.log` exit 0: 5 IntegrationSettings unit +38 IdentityIT (0 failures/errors/skips). Tests cover no collection before consent, CSRF/strict fields, no arbitrary payment event, ownership, daily dedup, concurrent withdrawal, retention, server-confirmed saves and account cascade. Existing notice test confirms CHANGE_NOTICE_VIEWED after consent. `generate-api.sh` exit 0: 5 unit +8 ScaffoldIT, actual OpenAPI/client regenerated. `.runtime/p11-ui.log`: typecheck/lint PASS and 31/31 targeted UI tests, including consent/withdrawal and source HTML rendered as text. Full engine test for source instructions is included in the pending full check.

`bash scripts/check-supply-chain.sh` exit 0: 311 npm registry SHA512 lock entries, exact manifests/root lock, installed npm tree, Maven stable versions/parent/wrapper hash, pinned CI actions; cached Maven dependency plugin 3.10.0 ran offline. Resolved springdoc 3.2.0 and Spring Session JDBC/Core 4.1.1 confirmed. No downloads/new direct dependencies. External CVE/signature/provenance/license audit NOT_RUN. Added status script and stronger candidate-artifact secret scan; bash -n and diff --check PASS. Documentation RUNBOOK/DATA_POLICY/ARCHITECTURE/README and T01–T38 checklist updated. Full P11 check/E2E/persistence/benchmark still pending at this entry.

P11 полный `bash scripts/check.sh` (`.runtime/p11-check.log`) exit 0: **156 unit +112 PostgreSQL IT +74 UI**, failures/errors/skips 0. Fresh V1–V11, 45 таблиц, повторная migrate без новых шагов; lint/typecheck/format/build/API drift/schema PASS. `benchmark-recommendation.sh` exit0, 1 benchmark: p50 43.18 ms, p95 51.66 ms, 102091 visited nodes =102090 непустых комбинаций +1 пустой корень; последний не рекомендуется.

Smoke 3/3, check-catalog, check-secrets, status.sh и test-persistence.sh exit0. Рабочая PostgreSQL17.12 healthy; V1–V11 successful; catalog revision2 сохранён. Frontend127.0.0.1:5173, backendIPv4-mapped loopback18080, DB127.0.0.1:15432. Marker пережил stop/start; удалена только собственная probe table, volume сохранён. Рабочие ADMIN/grants/notice payloads не менялись. Финальный полный E2E запущен, результат пока pending.

CI не запускался. В workflow обновлено только название: offline dependency-tree script требует ранее разрешённого cached Maven plugin и не добавлен как заведомо неработающий шаг на чистом remote runner. Локальный supply-chain script проверен, новая установка plugin/remote CI в этом проходе не выполнялась.


### Итог пакета P08–P11 — AWAITING_REVIEW, 15.11.2026

Финальный `bash scripts/test-e2e.sh` завершён **exit 0, 40/40 PASS, 14.1 min**, `.runtime/p11-e2e.log`, screenshots `.runtime/e2e-f25ab8dd505b/`. Проверены реальные изолированные publish → worker → API → inbox, отсутствие уведомления незатронутому владельцу, отдельное подтверждение прочтения, выдача/отзыв beta-прав, consent/export/delete и прежние guest/catalog/account/optimizer сценарии. Финальный код/JAR не менялся во время прогона. Просмотрены финальные account360, inbox1440, admin1440 и billing360; responsive тесты 360/768/1440 без overflow. Снимки используют synthetic fixture data; UUID аккаунта не session identifier.

Итоговые уникальные счётчики последнего полного запуска: **156 backend unit, 112 PostgreSQL integration, 74 frontend UI, 40 E2E; failures/errors/skips 0**. Дополнительно 1 benchmark PASS (не суммируется с unit), smoke 3/3 и schema 12 negative cases PASS. Все обязательные команды `check.sh`, `check-catalog.sh`, `smoke.sh`, `test-e2e.sh`, `check-secrets.sh`, `git diff --check` exit 0. Supply-chain/status/persistence exit 0. После последних изменений документации secrets/diff повторно PASS /exit 0; сборки повторно не требуются для Markdown-only правок.

Новые миграции пакета V8 draft/audit, V9 watches/jobs/notices/local outbox, V10 beta grants/audit/profile limits, V11 consent/daily events. Принятая V7 P07 сохранена; V1–V6 не изменены. Fresh PostgreSQL V1–V11 /45 tables PASS, повторная migrate no-op; рабочая PostgreSQL healthy, catalog revision2 и данные сохранены после stop/start. Новых direct dependencies нет. Реально разрешены Java21.0.13+9, Maven3.10.0, Boot4.1.2, Spring Session4.1.1, springdoc3.2.0; Node24.22.3/npm11.19.1, React19.3.1, TypeScript6.0.3, Vite8.3.8, Router8.5.1, Query5.107.1, openapi-ts0.99.0, Playwright1.64.1, Vitest5.1.0, PostgreSQL17.12.

Полные возможности, T01–T38, исправленные регрессии, финансовые примеры, security/ops evidence и список файлов: `docs/RELEASE_CHECKLIST.md`; эксплуатация: `docs/RUNBOOK.md`. Собственный review границ доступа, денежных расчётов, UNKNOWN, опубликованных версий и diff выполнен. **Независимое read-only review NOT_RUN**: отдельное разрешение на агента запрошено, ответа нет; собственная проверка не подменяет независимую. Coverage процентов NOT_RUN (инструментирование отсутствует), цели 85%/70% не объявлены достигнутыми. External CVE/provenance/license audit, remote CI, cross-browser, backup/restore, pentest/privacy/legal/tax/production review NOT_RUN/NOT_VERIFIED. Каталог ограничен 3 продуктами/7 тарифами/11 regional observations; неизвестные данные не заполнены выдумками. Это локальный кандидат, не разрешение на публичную beta/deployment.

**P07 DONE; P08 IMPLEMENTED_PENDING_OWNER_REVIEW; P09 DEFERRED; P10.a IMPLEMENTED_PENDING_OWNER_REVIEW; P10.b DEFERRED; P11 AWAITING_REVIEW. Общий статус AWAITING_REVIEW. P12–P14 NOT_STARTED.** Git: `codex/p01-scaffold`, HEAD `ffcde80860a3da71f28faa0bde29f056ab4dd348`, 42 modified +42 untracked paths (84, включая принятый P07 и план владельца), staged0/deleted0. Секреты/.env/caches/builds/logs/browser artifacts остаются ignored; commit/stage/push/PR не выполнялись. Внешние интеграции OFF; платных вызовов, платежей, отправки писем, новых прямых зависимостей и deployment нет. Работа завершена до решения владельца.


## FINAL LOCAL BETA CLOSEOUT — 15.11.2026

Владелец явно принял P08 и P10.a в пределах локальной beta, а также локальные QA-результаты P11; разрешён **один** локальный commit `feat: complete ai compass local beta candidate`. Это заменяет прежний запрет commit, но не закрывает independent-review, coverage и production gates. P09/P10.b DEFERRED; P12–P14 NOT_STARTED.

Проверены исходные 84 modified/new paths (42+42), все обычные UTF-8 файлы, без binary/symlink/удалений; индекс был пуст. Список 37 файлов принятого P07 из собственного раздела журнала полностью присутствует. Зафиксированы SHA256 исходного набора в ignored `.runtime/closeout-initial-hashes.json`; при closeout код, тесты, manifests/lockfiles, инфраструктура и миграции не менялись. V1–V6 без diff к HEAD; V7–V11 сохранены побайтно относительно начала closeout. Сохранён весь согласованный кандидат P07–P11, включая план владельца.

Последний полный P11 evidence переиспользован по явному разрешению: `.runtime/p11-check.log` содержит 156 unit +112 PostgreSQL IT +74 UI PASS, `.runtime/p11-e2e.log` 40/40 PASS. Ни один application source/test не новее полного прогона; позже check менялось только название CI workflow, до E2E, без изменения исполняемых шагов. Новых установок или сборок нет. Повторно `bash scripts/status.sh` exit0: healthy PostgreSQL, V1–V11 success, active catalog revision2; `bash scripts/smoke.sh` exit0: 3/3. Frontend127.0.0.1:5173, backend127.0.0.1:18080, PostgreSQL127.0.0.1:15432.

Read-only браузерная проверка рабочей БД (`.runtime/closeout-live-final.log`) exit0: главная/connection, 3 реальных продукта, 2 выбранные потребности без промпта, настоящий preview HTTP200 → CONDITIONAL_ONLY/0 confirmed/3 conditional/REVIEWED; отдельный запрос с неизвестной обязательной потребностью → HTTP200/NO_MATCH, UNKNOWN и ноль подтверждённых наборов; явный data gap для отсутствующей потребности, pricing beta. Первые два запуска временного скрипта завершались на неверном позиционном selector/неуказанной частоте и неточном имени кнопки; исправлен только временный скрипт, не приложение. Эти попытки не объявлены PASS. Рабочие аккаунты/каталог не изменялись.

Добавлен `docs/DEMO_GUIDE.md`: запуск подготовленной машины без загрузок, страницы/регистрация/private mail sink/сохранение/портфель/права, изолированный admin+notifications replay и пределы реального каталога. Обновлены только PROGRESS/RELEASE_CHECKLIST и новый DEMO_GUIDE. Для commit ожидаются 85 файлов (исходные 84 + руководство). Все `.env`, письма, caches/tools, логи, browser/test/build artifacts остаются ignored. Сохранённые тестовые пароли в synthetic fixtures не являются реальными учётными данными.

Приёмка LOCAL: P01–P07 DONE, P08/P10.a ACCEPTED, P11 local QA ACCEPTED. OPEN: independent review, coverage targets, external CVE/provenance/license audit, remote CI, cross-browser, backup/restore, pentest/privacy/legal/tax/production review. Прежние записи AWAITING_REVIEW выше — история передачи, не текущий статус. Никакого публичного deployment, paid API, реальной почты, оплаты, push/PR/merge или P12–P14.


Финальный closeout replay: `bash scripts/test-e2e.sh --grep 'import stays private|student with several needs|no match is honest|limited real catalog|guest result survives|UI add, actual price|admin publishes semantic|beta grants are server'` → **exit0, 8/8 PASS, 8.1 min**, `.runtime/closeout-demo-e2e.log`, artifacts `.runtime/e2e-a14693ac3b28/`. Это целевой повтор восьми существующих сценариев, не восемь новых тестов и не замена сохранённой полной suite40. Реально проверены все восемь запрошенных групп demo: live главная/каталог, несколько потребностей, backend preview, NO_MATCH/UNKNOWN, регистрация/сохранение, подписки/расходы, изолированные admin/notifications, Free/Premium без оплаты. Новых ошибок приложения не найдено, исходный код не менялся; обязательные сборки не перезапускались согласно разрешению владельца.

Перед фиксацией повторные `check-secrets.sh`, `git diff --check` PASS/exit0. В commit включается точный набор85 путей; служебный manifest списка и SHA256 остаётся ignored, применённые миграции неизменны. Сообщение commit: `feat: complete ai compass local beta candidate`. Hash и итоговый чистый Git status фиксируются в финальном ответе и истории Git, без дополнительного commit ради собственного hash. Работа на этом завершена; принятие относится только к локальному MVP.
