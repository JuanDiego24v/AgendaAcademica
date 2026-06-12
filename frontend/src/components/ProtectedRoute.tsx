import { useState, useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';
import { ReactNode } from 'react';

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const [checking, setChecking] = useState(true);
  const [hasPeriodo, setHasPeriodo] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) { setChecking(false); return; }
    client.get('/api/periodos/activo')
      .then(() => setHasPeriodo(true))
      .catch((err: { response?: { status?: number } }) => {
        if (err.response?.status === 404) setHasPeriodo(false);
        else setHasPeriodo(true);
      })
      .finally(() => setChecking(false));
  }, [isAuthenticated]);

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (checking) return null;
  if (!hasPeriodo && location.pathname !== '/perfil') {
    return <Navigate to="/perfil?setup=true" replace />;
  }
  return <>{children}</>;
}
