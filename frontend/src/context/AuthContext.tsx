import { createContext, useContext, useState, ReactNode } from 'react';

interface AuthState {
  token: string | null;
  username: string | null;
}

interface AuthContextType extends AuthState {
  login: (token: string, username: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
  });

  const login = (token: string, username: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    setAuth({ token, username });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setAuth({ token: null, username: null });
  };

  return (
    <AuthContext.Provider value={{ ...auth, login, logout, isAuthenticated: !!auth.token }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
