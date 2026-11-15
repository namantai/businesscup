import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import type {
  RecommendationProfile,
  RecommendationResult,
} from '../api/generated/types.gen';
import {
  PreviewError,
  requestPreview,
  type BuilderCatalog,
} from '../api/builder';
import { emptyDraft, toProfile, type Draft } from './draft';
export type CompletedPreview = {
  response: RecommendationResult;
  profile: RecommendationProfile;
  catalog: BuilderCatalog;
};
function useSessionState() {
  const [draft, setDraft] = useState(emptyDraft);
  const [step, setStep] = useState(0);
  const [completed, setCompleted] = useState<CompletedPreview | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [retryAt, setRetryAt] = useState(0);
  const sequence = useRef(0);
  const request = useRef<AbortController | null>(null);
  const cancel = useCallback(() => {
    sequence.current++;
    request.current?.abort();
    request.current = null;
    setPending(false);
  }, []);
  useEffect(
    () => () => {
      request.current?.abort();
    },
    [],
  );
  const update = useCallback(
    (apply: (draft: Draft) => Draft) => {
      cancel();
      setDraft(apply);
      setCompleted(null);
      setError(null);
    },
    [cancel],
  );
  const reset = useCallback(() => {
    cancel();
    setDraft(emptyDraft());
    setCompleted(null);
    setError(null);
    setStep(0);
  }, [cancel]);
  async function submit(catalog: BuilderCatalog) {
    if (request.current || Date.now() < retryAt) return false;
    const controller = new AbortController();
    request.current = controller;
    const id = ++sequence.current;
    setPending(true);
    setError(null);
    setCompleted(null);
    const profile = toProfile(draft);
    try {
      const response = await requestPreview(profile, controller.signal);
      if (id !== sequence.current || controller.signal.aborted) return false;
      if (response.catalogVersion !== catalog.version)
        throw new PreviewError(409);
      setCompleted({ response, profile, catalog });
      return true;
    } catch (failure) {
      if (id === sequence.current && !controller.signal.aborted) {
        setError(failure);
        if (failure instanceof PreviewError && failure.status === 429)
          setRetryAt(Date.now() + failure.retryAfter * 1000);
      }
      return false;
    } finally {
      if (id === sequence.current) {
        setPending(false);
        request.current = null;
      }
    }
  }
  function load(profile: RecommendationProfile) {
    cancel();
    setDraft({ ...profile, asOf: null, missingNeed: false });
    setCompleted(null);
    setError(null);
    setStep(1);
  }
  function restore(value: CompletedPreview) {
    cancel();
    setDraft({ ...value.profile, asOf: null, missingNeed: false });
    setCompleted(value);
    setError(null);
    setStep(1);
  }
  return {
    load,
    restore,
    draft,
    update,
    reset,
    step,
    setStep,
    completed,
    pending,
    error,
    retryAt,
    submit,
    cancel,
  };
}
const Session = createContext<ReturnType<typeof useSessionState> | null>(null);
export function BuilderSession({ children }: { children: ReactNode }) {
  return (
    <Session.Provider value={useSessionState()}>{children}</Session.Provider>
  );
}
// The draft and CSRF are never persisted to browser storage or the server.
// eslint-disable-next-line react-refresh/only-export-components
export function useBuilder() {
  const session = useContext(Session);
  if (!session) throw new Error('BuilderSession required');
  return session;
}
