import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { setRefresher } from '../api/client';
import * as api from '../api/endpoints';
import type { LoginResult } from '../api/types';

interface AuthState {
  user: LoginResult | null;
  /** true, пока при старте страницы пробуем восстановить сессию по cookie. */
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<LoginResult | null>(null);
  const [loading, setLoading] = useState(true);

  // Любой 401 в клиенте сначала пробует обновить сессию через cookie.
  useEffect(() => {
    setRefresher(async () => {
      const session = await api.refreshSession();
      setUser(session);
      return session !== null;
    });
    return () => setRefresher(null);
  }, []);

  // При старте страницы сессии в памяти нет, но cookie может быть жива.
  useEffect(() => {
    api.refreshSession().then((session) => {
      setUser(session);
      setLoading(false);
    });
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const session = await api.login(email, password);
    setUser(session);
  }, []);

  const signOut = useCallback(async () => {
    await api.logout();
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading, signIn, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}
