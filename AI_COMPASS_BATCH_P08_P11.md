# AI Compass — пакетное задание Codex: P08–P11

> **Назначение:** продолжить реализацию **после завершённого P07**, не повторяя и не переписывая Subscription Optimizer; закончить P08 (админ-публикация + in-app уведомления), при возможности P09 (опциональный), P10.a (Free/Premium права без оплаты), P11 (локальный QA и подготовка закрытой beta). Не выполнять P10.b/P12–P14 без отдельного разрешения.
>
> **Актуальное состояние по последнему отчёту Codex:** P01–P07 = DONE; P08 = BLOCKED только из-за отсутствующего пакетного файла; P09–P11 = NOT_STARTED. Ветка `codex/p01-scaffold`; последняя указанная вершина `ffcde80860a3da71f28faa0bde29f056ab4dd348`. Изменения P07 **ещё не закоммичены** (37 modified/new), `docs/PROGRESS.md` дополнительно обновлён. Перед работой проверить фактический `git status` и **сохранить ВСЕ изменения P07**.
>
> **Источник правил:** `AGENTS.md`, `CODEX_PROMPT.md`, `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`. При конфликте не нарушай более строгие запреты на внешние действия и запрашивай уточнение только при настоящем блокере.
>
> **Не делай:** git commit/push/PR, production deployment, live payment, реальные письма, платные AI API, автоматический scraping, глобальные установки, изменение посторонних ресурсов Docker. Новые прямые зависимости или новые Docker/browser images требуют отдельного явного разрешения.

## 0. Принятое пакетное разрешение и ограничения

Владелец **уже принял P07** по отчёту. Повторная приёмка P07 не требуется, повторно реализовывать P07 запрещено. Можно дополнять его только точечными исправлениями регрессий с тестами, если зависимые функции P08–P11 выявят дефекты. **Не откатывай, не очищай и не перезаписывай незакоммиченные изменения P07.** Не создавай отдельный commit до общего review.

Разрешено **одним проходом** последовательно выполнить P08 → P09 (только опциональная безсетевая часть или DEFERRED) → P10.a → P11 с проверками после каждой внутренней фазы. Не останавливайся только ради промежуточной приёмки успешно выполненной фазы. Требующие отдельного разрешения действия блокируют соответствующую зависимую часть; независимую работу можно продолжать без обхода запретов.

Статусы внутренних реализаций до приёмки владельцем: `AWAITING_REVIEW` или `IMPLEMENTED_PENDING_OWNER_REVIEW` (не объявлять самостоятельно `DONE` для новых фаз). Общий статус пакета `AWAITING_REVIEW`, когда локальная работа завершена; P10.b = DEFERRED, P12–P14 = NOT_STARTED.

### Неподлежащие нарушению инварианты

1. AI-модель, приложение, агент, тариф и региональное предложение — разные сущности.
2. UNKNOWN / STALE / CONDITIONAL не становятся VERIFIED / FREE / 0.
3. Денежные расчёты — `BigDecimal`, подтверждённые FX, отдельные upfront/monthly/horizon суммы.
4. Уже оплаченные невозвратные периоды не превращаются в экономию; удаление записи в Compass не отменяет стороннюю подписку.
5. MustKeep, shared quotas и included agents учитываются без двойного счёта.
6. Бесплатные предложения не штрафуются искусственно; партнёрские интересы не влияют на ranking.
7. Никаких выдуманных рейтингов качества, цен, источников или оплат.
8. Backend ownership, CSRF, session protection, rate limits и admin authorization обязательны.
9. Ни секретов, ни персональных данных в логах и отчётах.
10. `LLM_MODE=OFF`, `PAYMENTS_MODE=OFF`, внешние source collectors OFF; email только локальный dev sink.
11. Гостевой подбор работает без регистрации; не собирать данные банковских карт и пароли чужих сервисов.
12. Исторические рекомендации воспроизводимы по snapshot и версиям; каталог меняется только через проверенную публикацию.

## 2. Фаза A — P08: Catalog Administration & Recurring Value (обязательно, offline-first)

**Admin / Data**
- Реализуй защищённый admin workflow `draft → validate → review → publish` с immutable snapshot, audit событий и неизменённой моделью публикации из P03.
- Очередь stale/CONFLICTED/UNKNOWN, ручная правка и reviewer approval; администратор не может незаметно опубликовать невалидный каталог.
- Сначала **ручное/синтетическое изменение каталога**: old/new сравнение, выделение действительно существенных изменений цены, возможностей, лимитов и условий.
- Таблицы для catalog change events, watched user stacks, impacted users, in-app notifications, scheduler leases/jobs (если реально нужны и не дублируют существующее). Проверяй idempotency, dedup, job retry, crash recovery и никаких дубликатов уведомлений.
- Recompute только затронутых пользователей, ограниченно по частоте и нагрузке; не заявляй экономию при неполных данных.
- Источники берутся из уже проверенных данных. **Не включай автоматический web scraping** и свободный URL-fetch. Source adapters OFF; не запрашивай внешние данные, если нет разрешения и проверки условий.
- Email notifications в P08 только `OUTBOX/DEV SINK` и явный opt-in, подтверждённая отписка. **Никаких реальных писем.**

**Frontend**
- Административный экран (доступ только ADMIN) с draft/review/publish, conflicts, stale queue, audit; просмотр без авторизации недоступен.
- Пользовательский inbox «что изменилось и почему» — old/new, затронутые подписки, actions, стоимость и источники; если изменился незначимый факт, не посылай уведомление.
- Пользователь может отметить, что рекомендация применена, но это self-reported, не подтверждённая покупка/экономия.

**Тесты P08**
- Publish conflict, userA/userB ownership, USER→ADMIN denial, replay event/dedup, scheduler crash/retry, change not affecting profile, stale/no evidence, cancellation/unsubscribe, no email sent, all external collectors OFF; regression of P01–P07.
- Реальный user flow: **админ вручную публикует тестовое изменение в отдельной БД → только затронутый пользователь получает одно in-app уведомление → видит old/new с объяснением**.

## 3. Фаза B — P09 / P10.a (условная, без внешних платежей)

### P09 — Explanation Provider (опциональный)

- Для быстрого beta-пилота **предпочтительно оформить P09=DEFERRED**, зафиксировав причину: детерминированные объяснения уже существуют, платные LLM пока не нужны.
- Если templates недостаточно, допускается реализовать `ExplanationProvider` + deterministic templates **без внешнего LLM**. Не ставь новые SDK и не отправляй запросы.
- Если потребуется облачный LLM, stop/block соответствующую часть, запросить отдельное разрешение; базовый ranking должен работать при OFF.

### P10.a — Free/Premium entitlements (обязательно для beta, без платёжного провайдера)

- Серверные права Free и Premium; user-owned state и audit; Free limits на backend, а не CSS; beta grant с expiration, revocation и admin-only issuing.
- UI: Free/Premium comparison и account billing **честно маркирован как «beta / оплата недоступна»**. Нельзя выдавать фиктивные транзакции за реальные.
- Не скрывать доказательства, цену, неизвестность и ограничения за paywall.
- Испытать privilege escalation, смену роли/плана через JSON, expired grant, logout, concurrent grants, direct API bypass, users A/B, CSRF.
- **P10.b = BLOCKED / DEFERRED:** не выбирать PSP за владельца, не ставить SDK, не подключать webhook, не запускать sandbox checkout. Сначала требуется реальный выбор PSP, подтверждение его merchant eligibility и отдельное разрешение на новые зависимости/сети.

## 4. Фаза C — P11: Beta Hardening & Release Candidate (обязательно локально)

**QA / Security**
- Полная матрица T01–T38 из CODEX_PROMPT.md и traceability к реализации/тестам. Реальные PASS/FAIL/NOT_RUN/DEFERRED с причиной. Нельзя считать несуществующий тест пройденным.
- Проверка точных денежных расчётов, eligibility, unknown/stale, права Free/Premium, ownership, CSRF, IDOR, session persistence, rate limits, admin auth, dev sink isolation, source adapters OFF, no false payments.
- Browser E2E на desktop/mobile 360/768/1440, keyboard/focus/labels, no horizontal overflow, loading/empty/error.
- Изолированные test fixtures, синтетические prices; реальные catalog assertions не меняй ради E2E.
- Check frontend/backend/generated SDK drift, Flyway migrations clean DB, Testcontainers; добавь локальные dependency/supply-chain checks на имеющихся инструментах. Не объявляй security audit/pen-test, если он не выполнялся.
- Проверь, что P09 OFF/DEFERRED, P10.b DEFERRED ясно показаны в release notes. Никакой кнопки «Оплатить», которая имитирует успешный платёж.
- Создай/актуализируй `docs/DATA_POLICY.md`, `docs/RUNBOOK.md`, `docs/RELEASE_CHECKLIST.md`, README и ARCHITECTURE; security/privacy/legal NOT_VERIFIED как ограничение.

**Infrastructure / Ops**
- Воспроизводимый локальный запуск через существующие scripts и Docker Compose; loopback only, `.env.example`, никакого секрета в Git.
- CI файл можно улучшить локально, **не выполняй remote CI** и не публикуй репозиторий. Без нового браузерного образа без разрешения.
- Не выполнять deployment, реальные SMTP, hosting, DNS, реальные платежи, domain/VPS или внешние мониторинговые сервисы.
- P12 подготовь только как список требований к staging, никаких сетевых действий.

## 5. Обязательный порядок работы и контроль качества

1. **Сначала** проверь рабочее дерево, существующие migrations, API contracts, фактическое состояние P07. Не дублируй уже реализованное.
2. Составь короткий план и объяви список разрешённых затрагиваемых модулей. Работай **вертикальными срезами**, а не огромным незапускаемым кодом.
3. После каждого вертикального среза запускай релевантные unit + PostgreSQL integration + UI tests, устраняй ошибки с regression tests.
4. После фаз A, B, C запускай по применимости:
   - `bash scripts/check.sh`
   - `bash scripts/check-catalog.sh`
   - `bash scripts/smoke.sh`
   - `bash scripts/test-e2e.sh`
   - `bash scripts/check-secrets.sh`
   - `git diff --check`
   - дополнительные scripts/security/monitoring tests, если созданы.
5. Не скрывай ошибки, не отключай/skip тесты для зелёного вывода, не меняй expected на фактическую ошибку.
6. Если этап требует нового прямого Maven/npm dependency или новых downloads, **остановись перед установкой** и запроси конкретную версию, источник, цель, альтернативы. Не переносить разрешение на иные зависимости.
7. При блокере внешней зависимости (PSP, LLM, approved collector, real email) **отложи** его, не имитируй реализацию; выполняй независимые части.
8. Веди подробный `docs/PROGRESS.md` по каждой фазе: реальные изменения, тесты, ошибки и исправления, ограничения, статус `IMPLEMENTED_PENDING_OWNER_REVIEW`/`AWAITING_REVIEW`. Не объявляй фазы ACCEPTED или DONE от имени владельца.
9. **Не создавать commits, не делать Git push/PR, не выкладывать staging** до итогового review. Рабочее дерево остаётся под контролем владельца.
10. Финальный результат — один понятный отчёт по всему пакету. Дай таблицу P07 (уже DONE)/P08/P09/P10.a/P10.b/P11/P12–P14 и честные статусы каждой части.

## 6. Definition of Done для пакетной закрытой beta

- Пользователь добавляет существующие подписки, видит будущие расходы и ограничения; нет обещания fake savings.
- Администратор вручную публикует валидное изменение каталога, и только затронутые пользователи получают одно in-app уведомление с понятным сравнением.
- Гостевой подбор + аккаунты + сохранённые профили продолжают работать.
- Free/Premium права проверяются сервером; fake payments не отображаются как настоящие.
- Все основные security/ownership/CSRF/financial regression проверки проходят.
- Пользовательский UX объясняет UNKNOWN/STale, датy источника и ограничения маленького каталога.
- Нет live payments, emails, LLM, collectors и deployment; это **локальная beta, не production**.
- README, ARCHITECTURE, PROGRESS, DATA_POLICY, RUNBOOK и RELEASE_CHECKLIST актуальны и воспроизводимы.

## 7. Финальный отчёт Codex (обязательный)

Для каждой фазы: `IMPLEMENTED_PENDING_OWNER_REVIEW / BLOCKED / DEFERRED / NOT_STARTED`, изменения по файлам, endpoints, migrations, данные, тесты с фактическими числами и exit codes, screenshots в пределах разрешений, найденные и исправленные дефекты, NOT_RUN, блокеры, обновлённый PROGRESS.

Дополнительно:
- Демонстрация пользователя с 3 текущими подписками, оптимизация с обоснованной стоимостью, «лучше ничего не менять» и «недостаточно данных».
- Демонстрация одного проверенного in-app уведомления после изменения синтетического snapshot в изолированной БД.
- Проверка Free/Premium entitlement bypass и экспорт/удаление персональных подписок.
- Архитектура и воспроизводимость локального запуска; итоговый Git status и список изменённых файлов.
- Явный список операций, **не выполнявшихся**: remote CI, production payment/deployment, paid API, real email, external web collectors, pentest, реальные клиенты/продления.

**Остановись после единого отчёта. Не начинай P12–P14 и не делай ни одного внешнего действия без отдельного согласования владельца.**
