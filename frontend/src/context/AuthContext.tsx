import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import client from '../api/client';

export interface PeriodoActivo {
  id: number;
  nombre: string;
  fechaInicio: string;
  semanas: number;
}

interface AuthState {
  token: string | null;
  username: string | null;
}

interface AuthContextType extends AuthState {
  login: (token: string, username: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
  periodoActivo: PeriodoActivo | null;
  cargandoPeriodo: boolean;
  periodoFetchError: boolean;
  refreshPeriodo: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

type FetchPeriodoResult =
  | { ok: true; data: PeriodoActivo }
  | { ok: false; notFound: boolean };

async function fetchPeriodo(): Promise<FetchPeriodoResult> {
  try {
    const r = await client.get<PeriodoActivo>('/api/periodos/activo');
    return { ok: true, data: r.data };
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status;
    return { ok: false, notFound: status === 404 };
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
  });
  const [periodoActivo, setPeriodoActivo] = useState<PeriodoActivo | null>(null);
  const [cargandoPeriodo, setCargandoPeriodo] = useState(!!localStorage.getItem('token'));
  const [periodoFetchError, setPeriodoFetchError] = useState(false);

  useEffect(() => {
    if (auth.token) {
      setCargandoPeriodo(true);
      setPeriodoFetchError(false);
      fetchPeriodo().then(result => {
        if (result.ok) {
          setPeriodoActivo(result.data);
        } else {
          setPeriodoActivo(null);
          setPeriodoFetchError(!result.notFound);
        }
        setCargandoPeriodo(false);
      });
    } else {
      setPeriodoActivo(null);
      setPeriodoFetchError(false);
      setCargandoPeriodo(false);
    }
  }, [auth.token]);

  const login = (token: string, username: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    setAuth({ token, username });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setAuth({ token: null, username: null });
    setPeriodoActivo(null);
  };

  const refreshPeriodo = async () => {
    const result = await fetchPeriodo();
    if (result.ok) {
      setPeriodoActivo(result.data);
      setPeriodoFetchError(false);
    } else {
      setPeriodoActivo(null);
      setPeriodoFetchError(!result.notFound);
    }
  };

  return (
    <AuthContext.Provider value={{
      ...auth, login, logout,
      isAuthenticated: !!auth.token,
      periodoActivo, cargandoPeriodo, periodoFetchError, refreshPeriodo,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
