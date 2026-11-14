import type {
  ExistingSubscription,
  Need,
  RecommendationProfile,
} from '../api/generated/types.gen';
import type { BuilderCatalog } from '../api/builder';
export type Draft = Omit<
  RecommendationProfile,
  'needs' | 'existingSubscriptions'
> & {
  needs: Array<Omit<Need, 'intensity'> & { intensity: Need['intensity'] | '' }>;
  existingSubscriptions: Array<
    Omit<ExistingSubscription, 'billingPeriod'> & {
      billingPeriod: ExistingSubscription['billingPeriod'] | '';
    }
  >;
  missingNeed: boolean;
};
export const emptyDraft = (): Draft => ({
  profileSchemaVersion: 1,
  country: '',
  interfaceLanguage: '',
  outputLanguages: [],
  needs: [],
  budget: { currency: '', monthlyMax: '', upfrontMax: '', scope: 'ALL_AI' },
  allowAnnualBilling: false,
  maxTools: 4,
  horizonMonths: 12,
  priority: 'BALANCE',
  studentStatus: 'NONE',
  portfolioMode: 'AUGMENT',
  purchaseChannel: '',
  requirements: {
    platforms: [],
    privacy: 'NO_STRICT_REQUIREMENT',
    features: [],
    usage: [],
  },
  existingSubscriptions: [],
  acceptedConditionEvidenceIds: [],
  asOf: null,
  missingNeed: false,
});
export type FieldError = { field: string; step: number; message: string };
const decimal = /^(0|[1-9][0-9]{0,8})(\.[0-9]{1,6})?$/;
const language = /^[a-z]{2,3}(-[A-Z]{2})?$/;
export const normalizeAmount = (s: string) => s.trim().replace(',', '.');
const currencies = new Set(Intl.supportedValuesOf('currency'));
function isoDate(value: string) {
  return (
    /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    !Number.isNaN(Date.parse(value)) &&
    new Date(value).toISOString().slice(0, 10) === value
  );
}
export function validateDraft(d: Draft, catalog: BuilderCatalog): FieldError[] {
  const errors: FieldError[] = [];
  const add = (field: string, step: number, message: string) =>
    errors.push({ field, step, message });
  if (!d.needs.length || d.needs.length > 12)
    add('needs', 0, 'Выберите от одной до 12 потребностей.');
  if (d.missingNeed)
    add(
      'missingNeed',
      0,
      'В текущем каталоге недостаточно подтверждённых данных, чтобы подобрать инструменты для этой потребности.',
    );
  for (const n of d.needs) {
    if (!catalog.needs.some((x) => x.id === n.id))
      add(
        'needs',
        0,
        'Выбранная потребность отсутствует в этой версии каталога. Обновите выбор.',
      );
    if (!n.intensity)
      add(
        `frequency-${n.id}`,
        0,
        'Укажите частоту для каждой выбранной потребности.',
      );
  }
  if (!/^[A-Z]{2}$/.test(d.country))
    add('country', 1, 'Укажите страну двухбуквенным ISO-кодом, например KZ.');
  if (!language.test(d.interfaceLanguage))
    add(
      'interfaceLanguage',
      1,
      'Укажите язык интерфейса: например ru, en или kk.',
    );
  if (
    d.outputLanguages.length > 5 ||
    d.outputLanguages.some((l) => !language.test(l)) ||
    new Set(d.outputLanguages).size !== d.outputLanguages.length
  )
    add(
      'outputLanguages',
      1,
      'Укажите до пяти разных кодов языков через запятую.',
    );
  if (!d.purchaseChannel.trim() || d.purchaseChannel.length > 40)
    add('purchaseChannel', 1, 'Укажите способ покупки.');
  if (d.requirements.features.length > 32)
    add('features', 1, 'Можно выбрать до 32 дополнительных возможностей.');
  if (d.requirements.usage.length > 16)
    add('features', 1, 'Можно указать до 16 режимов использования.');
  for (const f of d.requirements.features)
    if (!catalog.capabilities.some((c) => c.id === f.capabilityId))
      add(
        'features',
        1,
        'Дополнительная возможность отсутствует в текущем каталоге.',
      );
  if (!currencies.has(d.budget.currency))
    add('currency', 2, 'Укажите действующий код валюты, например USD или KZT.');
  if (!decimal.test(normalizeAmount(d.budget.monthlyMax)))
    add(
      'monthlyMax',
      2,
      'Укажите месячный бюджет: 0 или положительную сумму, до 6 знаков после запятой.',
    );
  if (!decimal.test(normalizeAmount(d.budget.upfrontMax)))
    add(
      'upfrontMax',
      2,
      'Укажите доступную предоплату: 0 или положительную сумму.',
    );
  if (d.existingSubscriptions.length > 8)
    add('subscriptions', 2, 'Можно указать до восьми текущих подписок.');
  const planIds = d.existingSubscriptions.map((s) => s.planId).filter(Boolean);
  if (new Set(planIds).size !== planIds.length)
    add('subscriptions', 2, 'Один тариф нельзя указать дважды.');
  for (const [index, s] of d.existingSubscriptions.entries()) {
    const prefix = `Подписка ${index + 1}: `;
    if (!currencies.has(s.currency))
      add(`sub-${s.id}-currency`, 2, prefix + 'укажите валюту.');
    if (s.amount !== null && !decimal.test(normalizeAmount(s.amount)))
      add(
        `sub-${s.id}-amount`,
        2,
        prefix + 'укажите сумму или отметьте «Цена неизвестна».',
      );
    if (!s.billingPeriod)
      add(`sub-${s.id}-period`, 2, prefix + 'выберите период оплаты.');
    if (
      !isoDate(s.renewalAnchor) ||
      !isoDate(s.nextRenewal) ||
      s.renewalAnchor > s.nextRenewal
    )
      add(
        `sub-${s.id}-anchor`,
        2,
        prefix + 'проверьте исходную дату и следующее продление.',
      );
    try {
      if (!s.billingZone) throw new Error();
      new Intl.DateTimeFormat('en', { timeZone: s.billingZone }).format();
    } catch {
      add(
        `sub-${s.id}-zone`,
        2,
        prefix + 'укажите часовой пояс, например Asia/Almaty.',
      );
    }
    if (
      s.offerId &&
      !catalog.offers.some((o) => o.id === s.offerId && o.planId === s.planId)
    )
      add(
        `sub-${s.id}-offer`,
        2,
        prefix + 'предложение больше не представлено в этой версии.',
      );
  }
  for (const [i, u] of d.requirements.usage.entries())
    if (!decimal.test(normalizeAmount(u.monthlyQuantity)))
      add(`usage-${i}`, 1, 'Укажите ожидаемое число действий за месяц.');
  return errors;
}
export function toProfile(d: Draft): RecommendationProfile {
  const { missingNeed: omitted, ...profile } = d;
  void omitted;
  return {
    ...profile,
    asOf: null,
    budget: {
      ...d.budget,
      monthlyMax: normalizeAmount(d.budget.monthlyMax),
      upfrontMax: normalizeAmount(d.budget.upfrontMax),
    },
    needs: d.needs.map((n) => {
      if (!n.intensity) throw new Error('Frequency required');
      return { ...n, intensity: n.intensity };
    }),
    existingSubscriptions: d.existingSubscriptions.map((s) => {
      if (!s.billingPeriod) throw new Error('Billing period required');
      return {
        ...s,
        billingPeriod: s.billingPeriod,
        amount: s.amount === null ? null : normalizeAmount(s.amount),
      };
    }),
    requirements: {
      ...d.requirements,
      usage: d.requirements.usage.map((u) => ({
        ...u,
        monthlyQuantity: normalizeAmount(u.monthlyQuantity),
      })),
    },
  };
}
