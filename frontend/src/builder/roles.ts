import type { Draft } from './draft';

// Presets refer to catalog need IDs, never capabilities or invented task categories.
// The active snapshot is authoritative: absent IDs are never sent to the engine.
export const roles = [
  {
    id: 'student',
    label: 'Студент',
    symbol: '01',
    description: 'Учёба и новые знания',
    needs: ['study', 'presentations', 'research'],
  },
  {
    id: 'developer',
    label: 'Разработчик',
    symbol: '02',
    description: 'Код и рабочие проекты',
    needs: ['development', 'agents'],
  },
  {
    id: 'student-developer',
    label: 'Студент + код',
    symbol: '03',
    description: 'Учиться и создавать',
    needs: ['study', 'development', 'presentations', 'research'],
  },
  {
    id: 'designer',
    label: 'Дизайнер',
    symbol: '04',
    description: 'Визуальные идеи',
    needs: ['design', 'images'],
  },
  {
    id: 'creator',
    label: 'Контент-креатор',
    symbol: '05',
    description: 'Изображения, видео и звук',
    needs: ['images', 'video', 'editing', 'voice', 'music'],
  },
  {
    id: 'marketer',
    label: 'Маркетолог',
    symbol: '06',
    description: 'Тексты и исследования',
    needs: ['writing', 'research', 'images'],
  },
  {
    id: 'custom',
    label: 'Своё',
    symbol: '↗',
    description: 'Свои цели и настройки',
    needs: [],
  },
];
export function presetNeeds(
  role: (typeof roles)[number],
  available: { id: string }[],
  current: Draft['needs'],
): Draft['needs'] {
  return role.needs
    .filter((id) => available.some((n) => n.id === id))
    .map(
      (id) =>
        current.find((n) => n.id === id) ?? {
          id,
          weight: 3,
          required: true,
          intensity: '',
        },
    );
}
