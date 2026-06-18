import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import client from '../api/client';
import { applyTheme } from '../themes';

export interface PeriodoActivo {
  id: number;
  nombre: string;
  fechaInicio: string;
  semanas: number;
}

interface AuthState {
  token: string | null;
  username: string | null;
  tema: string;
}

interface AuthContextType extends AuthState {
  login: (token: string, username: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
  periodoActivo: PeriodoActivo | null;
  cargandoPeriodo: boolean;
  periodoFetchError: boolean;
  refreshPeriodo: () => Promise<void>;
  setTemaUsuario: (tema: string) => Promise<void>;
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
    tema: localStorage.getItem('tema') ?? 'dark',
  });
  const [periodoActivo, setPeriodoActivo] = useState<PeriodoActivo | null>(null);
  const [cargandoPeriodo, setCargandoPeriodo] = useState(!!localStorage.getItem('token'));
  const [periodoFetchError, setPeriodoFetchError] = useState(false);

  useEffect(() => {
    applyTheme(auth.tema);
  }, []);

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
    setAuth(prev => ({ ...prev, token, username }));
    client.get<{ tema: string }>('/api/perfil').then(r => {
      const t = r.data.tema ?? 'dark';
      localStorage.setItem('tema', t);
      applyTheme(t);
      setAuth(prev => ({ ...prev, tema: t }));
    }).catch(() => {});
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('tema');
    applyTheme('dark');
    setAuth({ token: null, username: null, tema: 'dark' });
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

  const setTemaUsuario = async (nuevoTema: string) => {
    await client.put('/api/perfil/tema', { tema: nuevoTema });
    localStorage.setItem('tema', nuevoTema);
    applyTheme(nuevoTema);
    setAuth(prev => ({ ...prev, tema: nuevoTema }));
  };

  return (
    <AuthContext.Provider value={{
      ...auth, login, logout,
      isAuthenticated: !!auth.token,
      periodoActivo, cargandoPeriodo, periodoFetchError, refreshPeriodo,
      setTemaUsuario,
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
