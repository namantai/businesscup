# AI Compass — фактический прогресс

**План:** `../CODEX_PROMPT.md`, версия 1.0, 14.11.2026.
**Текущее состояние:** P01–P05 = DONE. Владелец предварительно принял P05 и явно разрешил окончательное закрытие; повторные обязательные проверки выполнены 15.11.2026.
**Текущий этап:** P05 — DONE.
**Следующее действие:** остановиться после одного разрешённого локального commit P05. P06 = NOT_STARTED; начало P06 требует отдельного разрешения. Push/PR/deploy запрещены.
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
| Локальные изменения кода | Владелец 15.11.2026 разрешил окончательное закрытие P05: проверка инвариантов, исправления реальных ошибок с регрессиями, обязательные проверки и документация; P06 запрещён |
| Первоначальный локальный commit и ветка | Владелец 14.11.2026: четыре исходных файла без изменений, затем codex/p01-scaffold; выполнено |
| Установка project dependencies / Docker images / browsers | P01 — ранее разрешённая локальная инфраструктура. P03 — владелец отдельно разрешил springdoc 3.2.0, react-router 8.5.1, @tanstack/react-query 5.107.1, @hey-api/openapi-ts 0.99.0 и их транзитивные зависимости; без других прямых зависимостей, sudo/global config/платных услуг |
| Системные установки / sudo / global config | Нет |
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
