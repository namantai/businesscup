# AI Compass

Локальный каркас B2C-сервиса подбора сочетаний AI-подписок и агентов по нескольким потребностям, бюджету и ограничениям — без ввода конкретного задания.

Реализованы P01/P02 и каталог P03: публичный API чтения, локальные import/review/publish/rollback, небольшой manifest с официальными источниками, React-каталог и генерируемый OpenAPI-клиент. P03 закрыт после финальной проверки источников и тестов по поручению владельца. Анкета, Recommendation Engine, аккаунты и платежи ещё не реализованы. HTML в `reference/` — только референс, не проверенная база цен.

## Требования

Linux x86_64, Bash, Python 3.12+, curl или wget, unzip, Git, работающий Docker Engine с Compose и доступом текущего пользователя к daemon. Системные установки скрипты не выполняют. Точные версии и решения: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Первый запуск

Команды из корня работают также при вызове из fish:

```sh
bash scripts/setup.sh
bash scripts/check.sh
bash scripts/dev-up.sh --no-build
```

`setup.sh` скачивает закреплённые Node и JDK в `.tools/`, проверяет SHA-256 и создаёт `.env` с новым случайным паролем PostgreSQL и правами 0600. Существующий `.env` не перезаписывается. Кеши зависимостей находятся в `.cache/`. Нужен сетевой доступ к официальным реестрам; платные ключи не нужны.

`check.sh` выполняет clean backend verify, включая отдельный PostgreSQL Testcontainers, затем `npm ci`, сравнение OpenAPI работающего тестового backend с `docs/openapi.json`, повторную генерацию и сравнение TypeScript-клиента, format/lint/typecheck, компонентные тесты, frontend build и JSON Schema checks синтетического и reviewed каталогов. Недоступный Docker означает ошибку, а не пропуск интеграционных тестов.

Открыть **http://127.0.0.1:5173**. На странице должно появиться «Сервис доступен» после настоящего запроса через Vite proxy к backend readiness, включающему проверку БД. Каталог: **http://127.0.0.1:5173/catalog**. В новой БД он пуст до явных import/review/publish; порядок приведён ниже.

Для последующих запусков с пересборкой:

```sh
bash scripts/dev-up.sh
bash scripts/dev-down.sh
```

`dev-up.sh` собирает приложение с `-DskipTests` только для запуска; это **не** замена `check.sh`. Вариант `--no-build` требует уже собранный JAR и установленные npm-зависимости. Для перезапуска сначала использовать `dev-down.sh`.

## Порты и конфигурация

Все сервисы слушают только `127.0.0.1`:

| Сервис | Порт по умолчанию |
|---|---:|
| Frontend / Vite | 5173 |
| Backend | 18080 |
| PostgreSQL | 15432 |

Настройки в `.env`; образец — `.env.example`. Синтаксис: `KEY=value`, без кавычек, подстановок и shell-команд. Для занятого порта изменить соответствующий `*_PORT`, затем перезапустить проект. Чужие процессы скрипты не останавливают. После инициализации БД смена `DB_PASSWORD` сама по себе не меняет пароль внутри сохранённого volume.

Compose project: `ai-compass-p01`, volume: `ai-compass-p01_postgres_data`. Остановка сохраняет контейнер и volume. Не использовать `down -v`. Логи приложения и сведения о собственных PID находятся в `.runtime/` и не входят в Git.

`LLM_MODE=OFF`, `PAYMENTS_MODE=OFF`, `EMAIL_MODE=LOCAL`, `SOURCE_FETCH_ENABLED=false`. Попытка включить интеграцию в текущем каркасе блокирует запуск. Почтового адаптера пока нет: `LOCAL` означает отсутствие внешней отправки, а не уже работающий Mailpit.

## Проверки

```sh
bash scripts/check.sh
bash scripts/smoke.sh
bash scripts/test-e2e.sh
bash scripts/test-persistence.sh
bash scripts/check-secrets.sh
bash scripts/check-catalog.sh
```

`smoke.sh` и `test-persistence.sh` требуют работающего локального приложения. `smoke.sh` проверяет backend readiness, proxy readiness и frontend HTML.

`test-e2e.sh` требует успешного `check.sh`, Docker и уже установленного Chromium для закреплённого Playwright. Он сам запускает изолированные PostgreSQL в tmpfs, backend и frontend на случайных loopback-портах. Через настоящий CLI и API проверяет import → review → publish → frontend, новый snapshot, rollback, исторические ссылки, UNKNOWN/FREE, региональные цены, ошибки, запрет публичной записи и ширины 360/768/1440 px. Синтетические данные не попадают в dev-каталог. Собственные процессы и временный контейнер останавливаются в finally; постоянные volumes не создаются и не удаляются. Скриншоты — `frontend/test-results/`, логи — `.runtime/e2e-*`.

Если браузер ещё не установлен, после отдельного разрешения на его скачивание выполнить из корня (на этой машине уже установлен):

```sh
bash -c 'source scripts/common.sh; export PLAYWRIGHT_BROWSERS_PATH="$COMPASS_ROOT/.cache/playwright"; cd frontend; npm exec -- playwright install chromium'
```

Эта команда не устанавливает системные пакеты. В ручном CI загрузка браузера выделена в отдельный шаг перед E2E.

На Arch Playwright использует fallback-бинарник Ubuntu 24.04; официальной поддержки Arch нет. Загрузка первоначально предложенного Docker-образа дважды завершилась ошибкой сохранения слоя containerd. Поэтому проверяемый путь использует локальный браузер. Для CI выбрана поддерживаемая Ubuntu 24.04; сам удалённый CI ещё не запускался. Если браузеру не хватает системной библиотеки, скрипт завершится ошибкой, а не установит её через sudo.

`test-persistence.sh` создаёт собственную временную таблицу с UUID, выполняет stop/start, проверяет сохранённое значение и удаляет только эту проверочную таблицу. При ошибке она может остаться для диагностики; volumes не удаляются. Во время проверки приложение временно недоступно.

Backend-отчёты: `backend/target/surefire-reports/` и `backend/target/failsafe-reports/`. Проверка секретов — ограниченный поиск шаблонов и фактического локального пароля в отслеживаемых/неигнорируемых файлах, не гарантия полного security-аудита.

CI-файл создан только с ручным `workflow_dispatch`; удалённый CI не запускался. Публикация и production-развёртывание в P01 не входят.

## Ограничения разработки

Локальный HTTP использует session cookie без `Secure` только в запуске через dev-скрипт; конфигурация backend по умолчанию требует `Secure`. HttpOnly, SameSite и CSRF сохраняются. Аккаунтов нет, генерируемого admin-пароля нет, административные и прочие закрытые маршруты недоступны.

Testcontainers использует отдельную временную БД и случайный loopback-порт. Ryuk отключён, чтобы не монтировать Docker socket в контейнер; тесты останавливают свой контейнер через `AfterAll` и shutdown hook. При принудительном завершении JVM контейнер может остаться с меткой `com.aicompass.scope=p01-integration` или `p02-integration`; удаление чужих ресурсов запрещено.

Текущее состояние и фактические результаты: [docs/PROGRESS.md](docs/PROGRESS.md). P02 принят владельцем, P03 закрыт (DONE). P04 не начат.

Миграции V2/V3 создают пустой каталог: демотарифы при запуске не импортируются. Синтетические сценарии и ограничения: [data/fixtures/README.md](data/fixtures/README.md); модель и правила импорта: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).


## Каталог P03: локальный оператор

Все команды требуют запущенной локальной PostgreSQL и собранного JAR. CLI запускает Spring без HTTP-сервера, применяет Flyway и завершает процесс. Команда validate проверяет manifest, но не импортирует его. Startup ничего не импортирует автоматически.

```sh
bash scripts/catalog.sh validate --file=data/catalog/catalog-2026-11-14-r2.json
bash scripts/catalog.sh import --file=data/catalog/catalog-2026-11-14-r2.json
bash scripts/catalog.sh current
```

Import выводит version UUID и SHA-256 нормализованного manifest. После проверки фактов и этого хеша:

```sh
bash scripts/catalog.sh review --version=03000000-0000-4000-8000-000000000002 --hash='HASH_FROM_IMPORT' --actor='local-reviewer'
bash scripts/catalog.sh publish --version=03000000-0000-4000-8000-000000000002 --expected-revision=0 --actor='local-reviewer' --reason='Reviewed official sources'
```

`HASH_FROM_IMPORT` заменить фактическим хешем; expected-revision брать из current, а не всегда использовать 0. Review фиксирует именно этот sealed content. Повторный import того же UUID/content идемпотентен; изменённые данные требуют нового UUID. Импорт и review сами по себе не открывают snapshot через API.

Для rollback использовать ранее активированный UUID и актуальную ревизию:

```sh
bash scripts/catalog.sh rollback --version='PREVIOUS_UUID' --expected-revision=2 --actor='local-reviewer' --reason='Correction under review'
```

Конкурирующая команда с устаревшей ревизией завершается ошибкой; сначала проверить current. История и старые снимки сохраняются. Actor — локальная аудиторская метка, не аутентифицированный аккаунт. Публичных write endpoints нет.

## Public API P03

Префикс `/api/v1/catalog`, только GET:

- `/meta`: активные versionId/revision, datasetKind, asOf, регионы/потребности/возможности, число продуктов и UNKNOWN/STALE claims; без публикации status=EMPTY.
- `/products`, `/products/{slug}`: список и подробности продукта с тарифами, функциями, включёнными моделями/агентами, лимитами и evidence.
- `/providers`, `/regions`, `/models`, `/model-families`, `/agents`, `/plans`, `/offers`.
- `/needs`, `/capabilities`, `/need-capabilities`, `/assertions`, `/model-access`, `/agent-access`, `/quota-pools`, `/usage-limits`, `/eligibility-rules`.
- `/sources`, `/evidence`: публичные сведения; reviewer/method/internal manifest/hash не выдаются.

Списки возвращают `{versionId, items, total, page, size}`. `page` начинается с 0, `size` 1–100; `sort=id|-id|name|-name` (name только для сущностей с name), одинаковые имена упорядочены по id. `q` — поиск подстроки по name. Products: `kind=APPLICATION|AUTOMATION_TOOL`, `productId`, `capabilityId`; plans/agents: productId; offers: regionId/currency. Unsupported filter/resource combinations возвращают 400. Sources/evidence поддерживают page/size, порядок по id.

`version=UUID` закрепляет ранее активированный snapshot; импортированные/только reviewed версии дают 404. Без version выбирается активный snapshot, один на весь запрос. Для нескольких запросов клиент должен переносить возвращённый versionId. Пустой каталог даёт пустые списки, неизвестная детальная запись — 404. Ошибки используют Problem Details с requestId. Деньги — строки; null означает неизвестность. Monthly equivalent не заменяет upfront; неизвестные налоги/сборы исключают заявление об известной полной цене.

Для добавления продукта создать новый manifest UUID, сохранить stable ids существующих сущностей, добавить provider/product, планы и явно scoped evidence. Проверить JSON Schema и Java validator, импортировать, проверить хеш, review и publish. Правила источников, свежести и границы стартовых данных: [DATA_POLICY.md](docs/DATA_POLICY.md). Интерфейс использует этот API через сгенерированный клиент; статических рыночных массивов в приложении нет.


## OpenAPI и TypeScript-клиент

Схема доступна через backend и Vite proxy: `GET /api/v1/openapi`. Springdoc строит OpenAPI 3.0.1 из реальных catalog endpoints и Java DTO; в контракте только публичное чтение. Nullable-поля остаются явно присутствующим `null` (например, неизвестная цена), суммы — строки. Ошибки 400/404 описаны как Problem Details.

После изменения API/DTO:

```sh
bash scripts/generate-api.sh
bash scripts/check.sh
```

`generate-api.sh` запускает настоящий servlet backend с отдельной PostgreSQL через ScaffoldIT, сохраняет проверенную схему в `docs/openapi.json`, затем вызывает `npm run api:generate`. Генератор `@hey-api/openapi-ts` создаёт DTO, SDK и встроенный fetch-клиент в `frontend/src/api/generated/`. Ручные изменения этих файлов не нужны. В `check.sh` схема независимо экспортируется заново и `check-api.sh` сравнивает её и повторно сгенерированный клиент с файлами репозитория; расхождение завершает проверку ошибкой.

Frontend использует React Router для списка/деталей и TanStack Query для загрузки, кеша по versionId и повторов после ошибки. Поиск, тип продукта, возможность, сортировка и пагинация передаются API и сохраняются в URL; модели и агенты показаны отдельно. Детальные ссылки закрепляют snapshot. Кнопка обновления получает новую активную версию; предыдущие активированные снимки остаются доступными.

Покрытие стартового каталога ограничено 3 продуктами, 7 тарифами и 11 предложениями; это не полный рынок. UNKNOWN, устаревшие и условные утверждения видимы вместе с источником и датой. Региональная цена не означает подтверждённое право покупки; годовой эквивалент не заменяет платёж за год. Рекомендации и анкета относятся к следующим этапам.
