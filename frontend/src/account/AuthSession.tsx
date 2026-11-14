import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useLocation } from 'react-router';
import { useQueryClient } from '@tanstack/react-query';
import type { Account } from '../api/generated/types.gen';
import { checked, protection, logout, session } from '../api/account';
import { useBuilder } from '../builder/BuilderSession';

function useAuthState() {
  const { pathname } = useLocation();
  const [account, setAccount] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true);
  const [failure, setFailure] = useState<unknown>(null);
  const previous = useRef<string | null>(null);
  const sequence = useRef(0);
  const { reset } = useBuilder();
  const cache = useQueryClient();
  const accept = useCallback(
    (next: Account | null) => {
      sequence.current++;
      if (previous.current && previous.current !== next?.id) {
        reset();
        cache.removeQueries({ queryKey: ['personal'] });
      }
      previous.current = next?.id ?? null;
      setAccount(next);
      setLoading(false);
      setFailure(null);
    },
    [cache, reset],
  );
  const refresh = useCallback(async () => {
    const id = ++sequence.current;
    try {
      const value = await checked(session());
      if (id === sequence.current) accept(value.account);
    } catch (error) {
      if (id === sequence.current) {
        setFailure(error);
        setLoading(false);
      }
    }
  }, [accept]);
  const invalidate = useCallback(() => {
    sequence.current++;
  }, []);
  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (pathname !== '/') void refresh();
    }, 0);
    const focus = () => void refresh();
    window.addEventListener('focus', focus);
    return () => {
      window.clearTimeout(timer);
      invalidate();
      window.removeEventListener('focus', focus);
    };
  }, [refresh, pathname, invalidate]);
  async function signOut() {
    await checked(logout(await protection(account?.id)));
    reset();
    accept(null);
  }
  return { account, loading, failure, accept, refresh, signOut };
}
const Context = createContext<ReturnType<typeof useAuthState> | null>(null);
export function AuthSession({ children }: { children: ReactNode }) {
  return <Context.Provider value={useAuthState()}>{children}</Context.Provider>;
}
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error('AuthSession required');
  return value;
}
