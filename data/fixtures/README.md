# Синтетический каталог P02

`catalog-demo.json` — только тестовые данные, не сведения о существующих сервисах. Все продукты, тарифы, источники и FX вымышлены; статус версии DEMO, источники SYNTHETIC и `.invalid`. Реальные ISO-коды валют/стран служат проверке формата, не подтверждению цен или доступности.

Файл проверяется JSON Schema через `bash scripts/check-catalog.sh`, Java validator и настоящим PostgreSQL импортом внутри `bash scripts/check.sh`. Runtime auto-seed отсутствует; CLI публикации относится к P03. Никакие ссылки из fixture не открываются.

| Сценарий / id suffix | Что проверяется |
|---|---|
| monthly | Точные decimal 12.34, обычная месячная оплата |
| annual | Полные 120.01 upfront, эквивалент не проходит upfront limit 30 |
| free | Настоящий recurring ноль, отдельно от отсутствия данных |
| unknown | amount/upfront null, UNKNOWN evidence |
| student | PROVIDER_VERIFICATION как условие; self-declaration не подтверждение |
| trial | 7 дней, one-time credits с expiry, нет recurring allowance |
| metered | 0.000123 за единицу, без объёма итог неизвестен; API не цена приложения |
| regional / monthly-us | Валюты/регионы и несколько предложений одного плана без вымышленной конвертации |
| tax-unknown | Базовая цена известна, all-in отсутствует |
| fee-unknown | Неизвестная комиссия не равна 0 |
| shared | Image/video расходуют один pool, единицы не суммируются дважды |
| included-agent | Агент входит в plan; отдельной покупки за связь нет |
| conflicting-tier | Группа только одного продукта; два продукта provider не запрещены |
| stale | Устаревшая цена/возможность остаются явно STALE |
| assertion-slides / local-only | UNKNOWN поддержки/локальной обработки не выдаётся за отсутствие или подтверждение |
| assertion-study | PARTIAL + CONDITIONAL, не произвольный quality score |
| model-v1 / model-v2 | M:N модели/продукты; benchmark v1 не наследуется v2 |
| need-study / need-coding / need-slides | Несколько пользовательских нужд, отдельно от функций продукта |
| demo-fx | Только синтетический курс для exact arithmetic; никогда реальный FX |

Это сценарии **модели данных**, не готовые рекомендации. Проверка победителей, hard budgets, бесплатного предпочтения и portfolio optimization относится к P04/P07. По умолчанию demo запрещён; тестовый профиль и явный флаг включены только в integration context. Проверки отклоняют demo в prod и REVIEWED.
