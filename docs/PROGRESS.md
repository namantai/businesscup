# AI Compass — фактический прогресс

**План:** `../CODEX_PROMPT.md`, версия 1.0, 14.11.2026.
**Текущее состояние:** локальный каркас P01 реализован и проверен; предметная логика не начата.
**Текущий этап:** P01 — DONE (закрытие по прямому поручению владельца).
**Следующее разрешённое действие:** остановиться после локального коммита P01; P02 требует отдельного разрешения.
**Источник состояния:** этот файл + фактический Git/code/test evidence, не обещания агента.

## 1. Маршрут

| Этап | Название | Статус | Приёмка владельца / доказательство |
|---|---|---|---|
| P00 | Read-only аудит | ACCEPTED | Владелец 14.11.2026: «P00 ПРИНЯТ. Переходим к P01» |
| P01 | Каркас и инфраструктура | DONE | Владелец 14.11.2026 предварительно принял отчёт и поручил закрытие; повторно 11 backend + 7 UI PASS, smoke 3 PASS |
| P02 | Типы, schema, fixtures | NOT_STARTED | — |
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
| Локальные изменения кода | Владелец 14.11.2026: только P01, docs и тесты; без перехода P02 |
| Первоначальный локальный commit и ветка | Владелец 14.11.2026: четыре исходных файла без изменений, затем codex/p01-scaffold; выполнено |
| Установка project dependencies / Docker images / browsers | Владелец 14.11.2026: инструменты/зависимости и изолированная инфраструктура P01; без sudo/global config/платных услуг |
| Системные установки / sudo / global config | Нет |
| Платные LLM/API | Нет |
| Реальные письма/уведомления внешним адресатам | Нет |
| Production платежи / тестовые списания | Нет |
| VPS/домен/хостинг/облако | Нет |
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

## 5. Протокол текущего этапа

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

P01 закрыт по поручению владельца. Ожидать отдельного разрешения на P02; P02 = NOT_STARTED.

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
