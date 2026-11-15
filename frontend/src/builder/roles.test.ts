import { expect, test } from 'vitest';
import { presetNeeds, roles } from './roles';

test('student-developer spans study, coding, presentations and research when present', () => {
  const r = roles.find((r) => r.id === 'student-developer')!;
  const available = r.needs.map((id) => ({ id }));
  expect(presetNeeds(r, available, []).map((n) => n.id)).toEqual([
    'study',
    'development',
    'presentations',
    'research',
  ]);
});
test('limited snapshot never receives invented goals or inferred frequency', () => {
  const r = roles.find((r) => r.id === 'student-developer')!;
  expect(
    presetNeeds(r, [{ id: 'development' }, { id: 'research' }], []),
  ).toEqual([
    { id: 'development', weight: 3, required: true, intensity: '' },
    { id: 'research', weight: 3, required: true, intensity: '' },
  ]);
});
test('presets preserve an existing goal’s explicit settings', () => {
  const r = roles.find((r) => r.id === 'student')!;
  const existing = {
    id: 'study',
    weight: 5,
    required: false,
    intensity: 'DAILY' as const,
  };
  expect(presetNeeds(r, [{ id: 'study' }], [existing])).toEqual([existing]);
});
