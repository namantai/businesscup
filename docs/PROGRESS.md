# AI Compass — фактический прогресс

**План:** `../CODEX_PROMPT.md`, версия 1.0, 14.11.2026.
**Текущее состояние:** P02 предварительно принят владельцем и закрыт после архитектурной проверки и повторных тестов.
**Текущий этап:** P02 — DONE (по прямому поручению владельца).
**Следующее разрешённое действие:** остановиться после одного локального коммита P02. P03, push и PR не разрешены.
**Источник состояния:** этот файл + фактический Git/code/test evidence, не обещания агента.

## 1. Маршрут

| Этап | Название | Статус | Приёмка владельца / доказательство |
|---|---|---|---|
| P00 | Read-only аудит | ACCEPTED | Владелец 14.11.2026: «P00 ПРИНЯТ. Переходим к P01» |
| P01 | Каркас и инфраструктура | ACCEPTED | Закрыт commit d895c04; затем владелец явно сообщил «P01 ПРИНЯТ» и разрешил P02 |
| P02 | Типы, schema, fixtures | DONE | Владелец предварительно принял отчёт и поручил закрытие; повторно 62 unit + 44 PostgreSQL IT + 7 UI PASS |
| P03 | Проверенный каталог и API | NOT_STARTED | — |
| P04 | Recommendation Engine | NOT_STARTED | — |
| P05 | Гостевой frontend / R1 | NOT_STARTED | — |
| P06 | Аккаунты и сохранение | NOT_STARTED | — |
| P07 | Портфель и расходы | NOT_STARTED | — |
| P08 | Admin, мониторинг, уведомления | NOT_STARTED | — |
| P09 | Optional LLM explanations | NOT_STARTED | Можно DEFERRED только по решению владельца |
| P10 | Entitlements / sandbox billing | NOT_STARTED | P10.a права; P10.b sandbox; live требует обе части |
| P11 | Hardening и beta / R2 | NOT_STARTED | — |
| P12 | Staging / deployment / ops | NOT_STARTED | — |
| P13 | Paid pilot / R3 | NOT_STARTED | — |
| P14 | Данные пилота и бизнес-решение | NOT_STARTED | — |

Допустимые статусы: NOT_STARTED, IN_PROGRESS, BLOCKED, AWAITING_REVIEW, ACCEPTED, DEFERRED. Агент не назначает ACCEPTED без явной приёмки владельца. Отложенная часть не считается выполненной. Для закрытия P01 владелец отдельным сообщением 14.11.2026 явно предписал статус DONE; это исключение для данного этапа, без разрешения на P02.

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
| P00 read-only аудит в рабочем репозитории | Выполнен и принят 14.11.2026 |
| Локальные изменения кода | Владелец явно принял P01 и разрешил P02: схема, типы, fixtures, тесты и документация; P03 запрещён |
| Первоначальный локальный commit и ветка | Владелец 14.11.2026: четыре исходных файла без изменений, затем codex/p01-scaffold; выполнено |
| Установка project dependencies / Docker images / browsers | Владелец 14.11.2026: инструменты/зависимости и изолированная инфраструктура P01; без sudo/global config/платных услуг |
| Системные установки / sudo / global config | Нет |
| Платные LLM/API | Нет |
| Реальные письма/уведомления внешним адресатам | Нет |
| Production платежи / тестовые списания | Нет |
| VPS/домен/хостинг/облако | Нет |
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
