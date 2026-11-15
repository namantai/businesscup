import { useState } from 'react';

// Owned by App, so route changes preserve this tab's draft without browser storage.
export function useQuickMatchDraft() {
  const [role, setRole] = useState('student-dev');
  const [customized, setCustomized] = useState(false);
  const [goals, setGoals] = useState<string[]>([
    'study',
    'development',
    'presentations',
    'research',
  ]);
  const [country, setCountry] = useState('KZ');
  const [budget, setBudget] = useState('20');
  const [currency, setCurrency] = useState('USD');
  return {
    role,
    setRole,
    customized,
    setCustomized,
    goals,
    setGoals,
    country,
    setCountry,
    budget,
    setBudget,
    currency,
    setCurrency,
  };
}
