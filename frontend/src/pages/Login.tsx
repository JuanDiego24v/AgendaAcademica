import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { AuthResponse } from '../types';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await client.post<AuthResponse>('/api/auth/login', { username, password });
      login(data.token, data.username);
      navigate('/home');
    } catch {
      setError('Usuario o contraseña incorrectos');
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">AA</div>
        <div className="auth-subtitle">Agenda Académica</div>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="form-label">Usuario</label>
            <input
              type="text"
              className="form-control"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="mb-4">
            <label className="form-label">Contraseña</label>
            <input
              type="password"
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn-submit">Entrar</button>
        </form>

        {error && <div className="alert-dark-danger">{error}</div>}
        {params.get('logout') && !error && (
          <div className="alert-dark-success">Sesión cerrada correctamente</div>
        )}
        {params.get('passwordChanged') && !error && (
          <div className="alert-dark-success">Contraseña actualizada. Iniciá sesión nuevamente.</div>
        )}

        <div className="auth-footer">
          ¿No tienes cuenta?{' '}
          <Link to="/register" className="auth-link ms-3">Registrarse</Link>
        </div>
      </div>
    </div>
  );
}
