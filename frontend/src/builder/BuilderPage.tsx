import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { readMeta } from '../api/catalog';
import {
  errorMessage,
  readBuilderCatalog,
  type BuilderCatalog,
} from '../api/builder';
import { CatalogLayout, ErrorState, Loading } from '../catalog/CatalogLayout';
import { useBuilder } from './BuilderSession';
import { validateDraft, type Draft, type FieldError } from './draft';
import { ExistingSubscriptions } from './ExistingSubscriptions';
import styles from './Builder.module.css';
export function BuilderPage() {
  const meta = useQuery({
    queryKey: ['builder-meta'],
    queryFn: ({ signal }) => readMeta(signal),
    staleTime: 0,
  });
  const version = meta.data?.versionId;
  const catalog = useQuery({
    queryKey: ['builder-catalog', version],
    queryFn: ({ signal }) => readBuilderCatalog(version!, signal),
    enabled: !!version,
  });
  return (
    <CatalogLayout>
      <div className={styles.heading}>
        <p className={styles.eyebrow}>AI STACK BUILDER · ЛИЧНЫЙ ПОДБОР</p>
        <h1>Соберём ваш набор AI</h1>
        <p className={styles.muted}>
          Несколько потребностей, один понятный бюджет. Без регистрации, задания
          и промпта.
        </p>
      </div>
      {meta.isError ? (
        <ErrorState error={meta.error} retry={() => void meta.refetch()} />
      ) : meta.isPending ? (
        <Loading />
      ) : !version ? (
        <p role="status">
          Каталог пока не опубликован. Подбор будет доступен после проверки
          данных.
        </p>
      ) : catalog.isError ? (
        <ErrorState
          error={catalog.error}
          retry={() => void catalog.refetch()}
        />
      ) : !catalog.data ? (
        <Loading />
      ) : (
        <BuilderForm
          catalog={catalog.data}
          refresh={() => void meta.refetch()}
          refreshing={meta.isFetching}
        />
      )}
    </CatalogLayout>
  );
}
function BuilderForm({
  catalog,
  refresh,
  refreshing,
}: {
  catalog: BuilderCatalog;
  refresh: () => void;
  refreshing: boolean;
}) {
  const {
    draft,
    update,
    step,
    setStep,
    submit,
    pending,
    error,
    retryAt,
    cancel,
    reset,
  } = useBuilder();
  const navigate = useNavigate();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const summary = useRef<HTMLDivElement>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const [clock, setClock] = useState(Date.now);
  const [outputText, setOutputText] = useState(
    draft.outputLanguages.join(', '),
  );
  useEffect(() => () => cancel(), [cancel]);
  useEffect(() => {
    heading.current?.focus();
  }, [step]);
  useEffect(() => {
    if (errors.length) summary.current?.focus();
  }, [errors, step]);
  useEffect(() => {
    if (!retryAt) return;
    const timer = window.setInterval(() => setClock(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [retryAt]);
  const seconds = Math.max(0, Math.ceil((retryAt - clock) / 1000));
  const set = <K extends keyof Draft>(key: K, value: Draft[K]) =>
    update((d) => ({ ...d, [key]: value }));
  function check(targetStep?: number) {
    const found = validateDraft(draft, catalog).filter(
      (e) => targetStep === undefined || e.step === targetStep,
    );
    setErrors(found);
    if (found.length) {
      setStep(found[0].step);
      return false;
    }
    return true;
  }
  const field = (id: string, title: string, control: ReactNode) => (
    <div className={styles.field}>
      <label htmlFor={id}>{title}</label>
      {control}
      {errors
        .filter((e) => e.field === id)
        .map((e) => (
          <span key={e.message} className={styles.error}>
            {e.message}
          </span>
        ))}
    </div>
  );
  const invalid = (id: string) => errors.some((e) => e.field === id);
  const titles = [
    'Ваши потребности',
    'Условия и возможности',
    'Бюджет и подписки',
  ];
  return (
    <>
      <ol className={styles.steps} aria-label="Шаги анкеты">
        {titles.map((title, index) => (
          <li key={title}>
            <button
              type="button"
              aria-current={step === index ? 'step' : undefined}
              onClick={() => {
                setErrors([]);
                setStep(index);
              }}
            >
              {index + 1}. {title}
            </button>
          </li>
        ))}
      </ol>
      <form
        className={styles.panel}
        noValidate
        onChange={() => {
          if (errors.length) setErrors([]);
        }}
        onSubmit={async (e) => {
          e.preventDefault();
          if (pending || seconds > 0 || !check()) return;
          const succeeded = await submit(catalog);
          setClock(Date.now());
          if (succeeded) navigate('/results');
        }}
      >
        <h2 ref={heading} tabIndex={-1}>
          {titles[step]}
        </h2>
        <p className={styles.muted}>
          Шаг {step + 1} из 3. Черновик хранится в памяти вкладки и исчезнет
          после перезагрузки.
        </p>
        {!!errors.length && (
          <div
            className={styles.error}
            role="alert"
            ref={summary}
            tabIndex={-1}
          >
            <strong>Проверьте анкету</strong>
            <ul>
              {errors.map((e, i) => (
                <li key={i}>
                  <a href={`#${e.field}`} onClick={() => setStep(e.step)}>
                    {e.message}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
        <fieldset
          disabled={pending}
          style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
        >
          {step === 0 && (
            <>
              <p id="needs" tabIndex={-1}>
                Выберите несколько направлений из опубликованного каталога.
                Приоритет 1 — низкий, 5 — высокий.
              </p>
              <div className={styles.grid}>
                {catalog.needs.map((need) => {
                  const selected = draft.needs.find((n) => n.id === need.id);
                  return (
                    <div className={styles.option} key={need.id}>
                      <label className={styles.check}>
                        <input
                          type="checkbox"
                          checked={!!selected}
                          onChange={(e) =>
                            set(
                              'needs',
                              e.target.checked
                                ? [
                                    ...draft.needs,
                                    {
                                      id: need.id,
                                      weight: 3,
                                      required: true,
                                      intensity: '',
                                    },
                                  ]
                                : draft.needs.filter((n) => n.id !== need.id),
                            )
                          }
                        />
                        {need.name}
                      </label>
                      {selected && (
                        <>
                          <label className={styles.field}>
                            Приоритет: {need.name}
                            <select
                              value={selected.weight}
                              onChange={(e) =>
                                set(
                                  'needs',
                                  draft.needs.map((n) =>
                                    n.id === need.id
                                      ? { ...n, weight: Number(e.target.value) }
                                      : n,
                                  ),
                                )
                              }
                            >
                              {[1, 2, 3, 4, 5].map((n) => (
                                <option value={n} key={n}>
                                  {n}
                                </option>
                              ))}
                            </select>
                          </label>
                          <label className={styles.check}>
                            <input
                              type="checkbox"
                              checked={selected.required}
                              onChange={(e) =>
                                set(
                                  'needs',
                                  draft.needs.map((n) =>
                                    n.id === need.id
                                      ? { ...n, required: e.target.checked }
                                      : n,
                                  ),
                                )
                              }
                            />
                            Обязательно: {need.name}
                          </label>
                          {field(
                            `frequency-${need.id}`,
                            `Частота: ${need.name}`,
                            <select
                              id={`frequency-${need.id}`}
                              aria-invalid={invalid(`frequency-${need.id}`)}
                              value={selected.intensity}
                              onChange={(e) =>
                                set(
                                  'needs',
                                  draft.needs.map((n) =>
                                    n.id === need.id
                                      ? {
                                          ...n,
                                          intensity: e.target
                                            .value as Draft['needs'][number]['intensity'],
                                        }
                                      : n,
                                  ),
                                )
                              }
                            >
                              <option value="">Выберите частоту</option>
                              <option value="OCCASIONAL">
                                Время от времени
                              </option>
                              <option value="WEEKLY">Каждую неделю</option>
                              <option value="DAILY">Каждый день</option>
                            </select>,
                          )}
                        </>
                      )}
                    </div>
                  );
                })}
              </div>
              <aside className={styles.notice}>
                Здесь показаны только направления, описанные в текущем каталоге.
                Если вам нужны учёба, презентации, документы, тексты, дизайн,
                изображения, видео, AI-агенты или анализ данных, а
                соответствующего направления нет в списке, данных для такого
                подбора пока недостаточно. Наличие отдельной возможности не
                означает покрытие всей сферы.
                <label className={styles.check}>
                  <input
                    id="missingNeed"
                    type="checkbox"
                    checked={draft.missingNeed}
                    onChange={(e) => set('missingNeed', e.target.checked)}
                  />
                  В каталоге нет одной из моих обязательных потребностей
                </label>
              </aside>
            </>
          )}
          {step === 1 && (
            <>
              <div className={styles.grid}>
                {field(
                  'country',
                  'Страна покупки (код)',
                  <input
                    id="country"
                    autoComplete="country"
                    placeholder="KZ"
                    maxLength={2}
                    value={draft.country}
                    aria-invalid={invalid('country')}
                    onChange={(e) =>
                      set('country', e.target.value.toUpperCase())
                    }
                  />,
                )}
                {field(
                  'interfaceLanguage',
                  'Язык интерфейса (код)',
                  <input
                    id="interfaceLanguage"
                    placeholder="ru"
                    value={draft.interfaceLanguage}
                    aria-invalid={invalid('interfaceLanguage')}
                    onChange={(e) =>
                      set('interfaceLanguage', e.target.value.trim())
                    }
                    list="language-codes"
                  />,
                )}
                {field(
                  'outputLanguages',
                  'Языки ответов (необязательно)',
                  <input
                    id="outputLanguages"
                    placeholder="ru, en"
                    value={outputText}
                    onChange={(e) => {
                      setOutputText(e.target.value);
                      set(
                        'outputLanguages',
                        e.target.value
                          .split(',')
                          .map((x) => x.trim())
                          .filter(Boolean),
                      );
                    }}
                  />,
                )}
                {field(
                  'purchaseChannel',
                  'Способ покупки',
                  <input
                    id="purchaseChannel"
                    list="purchase-channels"
                    placeholder="Выберите или укажите канал"
                    value={draft.purchaseChannel}
                    onChange={(e) => set('purchaseChannel', e.target.value)}
                  />,
                )}
              </div>
              <datalist id="language-codes">
                <option value="ru">Русский</option>
                <option value="en">Английский</option>
                <option value="kk">Казахский</option>
              </datalist>
              <datalist id="purchase-channels">
                {[...new Set(catalog.offers.map((o) => o.channel))].map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
              <p className={styles.muted}>
                Коды: Казахстан — KZ, США — US; русский — ru, английский — en,
                казахский — kk. Способ покупки берётся из предложений каталога.
                Неподтверждённая доступность в вашей стране будет показана
                отдельно.
              </p>
              <fieldset>
                <legend>Где будете использовать AI (необязательно)</legend>
                <div className={styles.grid}>
                  {[
                    ['WEB', 'Браузер'],
                    ['WINDOWS', 'Windows'],
                    ['MACOS', 'macOS'],
                    ['LINUX', 'Linux'],
                    ['ANDROID', 'Android'],
                    ['IOS', 'iOS'],
                    ['IDE', 'Редактор кода'],
                    ['TERMINAL', 'Терминал'],
                  ].map(([code, label]) => (
                    <label className={styles.check} key={code}>
                      <input
                        type="checkbox"
                        checked={draft.requirements.platforms.includes(code)}
                        onChange={(e) =>
                          set('requirements', {
                            ...draft.requirements,
                            platforms: e.target.checked
                              ? [...draft.requirements.platforms, code]
                              : draft.requirements.platforms.filter(
                                  (x) => x !== code,
                                ),
                          })
                        }
                      />
                      {label}
                    </label>
                  ))}
                </div>
              </fieldset>
              <label className={styles.field}>
                Конфиденциальность
                <select
                  value={draft.requirements.privacy}
                  onChange={(e) =>
                    set('requirements', {
                      ...draft.requirements,
                      privacy: e.target
                        .value as Draft['requirements']['privacy'],
                    })
                  }
                >
                  <option value="NO_STRICT_REQUIREMENT">
                    Без требования локальной обработки
                  </option>
                  <option value="LOCAL_ONLY">
                    Только локальная обработка данных
                  </option>
                </select>
              </label>
              <h3 id="features" tabIndex={-1}>
                Дополнительные возможности
              </h3>
              <div className={styles.grid}>
                {catalog.capabilities
                  .filter(
                    (c) =>
                      c.category === 'FUNCTION' || c.category === 'LICENSE',
                  )
                  .map((c) => {
                    const f = draft.requirements.features.find(
                      (x) => x.capabilityId === c.id,
                    );
                    return (
                      <div className={styles.option} key={c.id}>
                        <label className={styles.check}>
                          <input
                            type="checkbox"
                            checked={!!f}
                            onChange={(e) =>
                              set('requirements', {
                                ...draft.requirements,
                                features: e.target.checked
                                  ? [
                                      ...draft.requirements.features,
                                      {
                                        capabilityId: c.id,
                                        weight: 3,
                                        required: true,
                                      },
                                    ]
                                  : draft.requirements.features.filter(
                                      (x) => x.capabilityId !== c.id,
                                    ),
                              })
                            }
                          />
                          {c.name}
                        </label>
                        {f && (
                          <>
                            <label className={styles.check}>
                              <input
                                type="checkbox"
                                checked={f.required}
                                onChange={(e) =>
                                  set('requirements', {
                                    ...draft.requirements,
                                    features: draft.requirements.features.map(
                                      (x) =>
                                        x.capabilityId === c.id
                                          ? { ...x, required: e.target.checked }
                                          : x,
                                    ),
                                  })
                                }
                              />
                              Обязательна: {c.name}
                            </label>
                            <label className={styles.field}>
                              Важность: {c.name}
                              <select
                                value={f.weight}
                                onChange={(e) =>
                                  set('requirements', {
                                    ...draft.requirements,
                                    features: draft.requirements.features.map(
                                      (x) =>
                                        x.capabilityId === c.id
                                          ? {
                                              ...x,
                                              weight: Number(e.target.value),
                                            }
                                          : x,
                                    ),
                                  })
                                }
                              >
                                {[1, 2, 3, 4, 5].map((n) => (
                                  <option key={n}>{n}</option>
                                ))}
                              </select>
                            </label>
                          </>
                        )}
                      </div>
                    );
                  })}
              </div>
              <p className={styles.notice}>
                Частота использования не задаёт количество запросов
                автоматически. Если лимиты важны, укажите объём ниже; иначе их
                достаточность может остаться неизвестной.
              </p>
              <details>
                <summary>Уточнить месячный объём действий</summary>
                {[
                  ...new Map(
                    catalog.limits.map((l) => [
                      `${l.capabilityId}:${l.mode}`,
                      l,
                    ]),
                  ).values(),
                ].map((l) => {
                  const selected = draft.requirements.usage.find(
                    (u) =>
                      u.capabilityId === l.capabilityId && u.mode === l.mode,
                  );
                  const label = `${catalog.capabilities.find((c) => c.id === l.capabilityId)?.name ?? l.capabilityId} · ${l.mode}`;
                  return (
                    <div key={label}>
                      <label className={styles.check}>
                        <input
                          type="checkbox"
                          checked={!!selected}
                          onChange={(e) =>
                            set('requirements', {
                              ...draft.requirements,
                              usage: e.target.checked
                                ? [
                                    ...draft.requirements.usage,
                                    {
                                      capabilityId: l.capabilityId,
                                      mode: l.mode,
                                      unit: 'actions',
                                      monthlyQuantity: '',
                                      required: true,
                                    },
                                  ]
                                : draft.requirements.usage.filter(
                                    (u) => u !== selected,
                                  ),
                            })
                          }
                        />
                        {label}
                      </label>
                      {selected && (
                        <label className={styles.field}>
                          Действий за месяц: {label}
                          <input
                            id={`usage-${draft.requirements.usage.indexOf(selected)}`}
                            inputMode="decimal"
                            value={selected.monthlyQuantity}
                            onChange={(e) =>
                              set('requirements', {
                                ...draft.requirements,
                                usage: draft.requirements.usage.map((u) =>
                                  u === selected
                                    ? { ...u, monthlyQuantity: e.target.value }
                                    : u,
                                ),
                              })
                            }
                          />
                        </label>
                      )}
                    </div>
                  );
                })}
                {!catalog.limits.length && (
                  <p>В каталоге нет описанных режимов использования.</p>
                )}
              </details>
            </>
          )}
          {step === 2 && (
            <>
              <div className={styles.grid}>
                {field(
                  'currency',
                  'Валюта бюджета',
                  <input
                    id="currency"
                    placeholder="USD или KZT"
                    maxLength={3}
                    value={draft.budget.currency}
                    aria-invalid={invalid('currency')}
                    onChange={(e) =>
                      set('budget', {
                        ...draft.budget,
                        currency: e.target.value.toUpperCase(),
                      })
                    }
                  />,
                )}
                {field(
                  'monthlyMax',
                  'Месячный бюджет на весь AI',
                  <input
                    id="monthlyMax"
                    inputMode="decimal"
                    value={draft.budget.monthlyMax}
                    aria-invalid={invalid('monthlyMax')}
                    onChange={(e) =>
                      set('budget', {
                        ...draft.budget,
                        monthlyMax: e.target.value,
                      })
                    }
                  />,
                )}
                {field(
                  'upfrontMax',
                  'Доступная предоплата',
                  <input
                    id="upfrontMax"
                    inputMode="decimal"
                    value={draft.budget.upfrontMax}
                    aria-invalid={invalid('upfrontMax')}
                    onChange={(e) =>
                      set('budget', {
                        ...draft.budget,
                        upfrontMax: e.target.value,
                      })
                    }
                  />,
                )}
              </div>
              <p className={styles.muted}>
                Укажите 0 для поиска бесплатных вариантов. Бюджет включает
                сохраняемые подписки и применимую плату Compass. Разные валюты
                сравниваются только при наличии подтверждённого курса.
              </p>
              <label className={styles.check}>
                <input
                  type="checkbox"
                  checked={draft.allowAnnualBilling}
                  onChange={(e) => set('allowAnnualBilling', e.target.checked)}
                />
                Разрешить годовую оплату в пределах предоплаты
              </label>
              <div className={styles.grid}>
                <label className={styles.field}>
                  Период планирования
                  <select
                    value={draft.horizonMonths}
                    onChange={(e) =>
                      set('horizonMonths', Number(e.target.value))
                    }
                  >
                    {[1, 3, 6, 12, 24].map((n) => (
                      <option value={n} key={n}>
                        {n} мес.
                      </option>
                    ))}
                  </select>
                </label>
                <label className={styles.field}>
                  Максимум инструментов
                  <select
                    value={draft.maxTools}
                    onChange={(e) => set('maxTools', Number(e.target.value))}
                  >
                    {[1, 2, 3, 4].map((n) => (
                      <option key={n}>{n}</option>
                    ))}
                  </select>
                </label>
                <label className={styles.field}>
                  Главный приоритет
                  <select
                    value={draft.priority}
                    onChange={(e) =>
                      set('priority', e.target.value as Draft['priority'])
                    }
                  >
                    <option value="BALANCE">Баланс цены и возможностей</option>
                    <option value="ECONOMY">Экономия</option>
                    <option value="CAPABILITIES">Больше возможностей</option>
                  </select>
                </label>
                <label className={styles.field}>
                  Студенческий статус
                  <select
                    value={draft.studentStatus}
                    onChange={(e) =>
                      set(
                        'studentStatus',
                        e.target.value as Draft['studentStatus'],
                      )
                    }
                  >
                    <option value="NONE">Не заявляю льготу</option>
                    <option value="SELF_DECLARED">
                      Я студент, проверку провайдера не проходил
                    </option>
                    <option value="USER_REPORTED_PROVIDER_VERIFIED">
                      Сообщаю, что прошёл проверку провайдера
                    </option>
                  </select>
                </label>
              </div>
              <p className={styles.notice}>
                Даже заявленная проверка не подтверждает льготу автоматически.
                Предложения с условиями отображаются отдельно и требуют проверки
                у провайдера.
              </p>
              <ExistingSubscriptions catalog={catalog} />
            </>
          )}
        </fieldset>
        {error !== null && (
          <div role="alert" className={styles.error}>
            {errorMessage(error)}
          </div>
        )}
        {pending && (
          <p role="status">Подбираем набор по опубликованному каталогу…</p>
        )}
        {seconds > 0 && (
          <p role="status">Повторный запрос доступен через {seconds} сек.</p>
        )}
        <div className={styles.actions}>
          {step > 0 && (
            <button
              type="button"
              className={styles.secondary}
              disabled={pending}
              onClick={() => {
                setErrors([]);
                setStep(step - 1);
              }}
            >
              Назад
            </button>
          )}
          {step < 2 ? (
            <button
              key="continue"
              type="button"
              className={styles.primary}
              onClick={(event) => {
                // A step transition must never become the new submit button's default action.
                event.preventDefault();
                if (check(step)) setStep(step + 1);
              }}
            >
              Продолжить
            </button>
          ) : (
            <button
              key="preview"
              type="submit"
              className={styles.primary}
              disabled={pending || seconds > 0}
            >
              Подобрать мой AI-набор
            </button>
          )}
          {pending && (
            <button type="button" className={styles.secondary} onClick={cancel}>
              Отменить запрос
            </button>
          )}
        </div>
      </form>
      <div className={styles.actions}>
        <button
          type="button"
          className={styles.secondary}
          disabled={pending}
          onClick={() => {
            reset();
            setErrors([]);
            setOutputText('');
          }}
        >
          Очистить анкету
        </button>
      </div>
      <p className={styles.muted}>Версия каталога: {catalog.version}</p>
      <button
        className={styles.secondary}
        type="button"
        disabled={pending || refreshing}
        onClick={refresh}
      >
        Обновить каталог
      </button>
    </>
  );
}
