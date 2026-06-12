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
  refreshPeriodo: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

async function fetchPeriodo(): Promise<PeriodoActivo | null> {
  try {
    const r = await client.get<PeriodoActivo>('/api/periodos/activo');
    return r.data;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
  });
  const [periodoActivo, setPeriodoActivo] = useState<PeriodoActivo | null>(null);
  const [cargandoPeriodo, setCargandoPeriodo] = useState(!!localStorage.getItem('token'));

  useEffect(() => {
    if (auth.token) {
      setCargandoPeriodo(true);
      fetchPeriodo().then(p => {
        setPeriodoActivo(p);
        setCargandoPeriodo(false);
      });
    } else {
      setPeriodoActivo(null);
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
    const p = await fetchPeriodo();
    setPeriodoActivo(p);
  };

  return (
    <AuthContext.Provider value={{
      ...auth, login, logout,
      isAuthenticated: !!auth.token,
      periodoActivo, cargandoPeriodo, refreshPeriodo,
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
