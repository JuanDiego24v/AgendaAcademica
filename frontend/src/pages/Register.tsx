import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import client from '../api/client';

function getPasswordStrength(pass: string): { label: string; cls: string } {
  if (pass.length === 0) return { label: 'Mínimo 6 caracteres', cls: 'text-muted-mono' };
  if (pass.length < 6) return { label: 'Contraseña débil', cls: 'password-weak' };
  if (pass.match(/[A-Z]/) && pass.match(/[0-9]/)) return { label: 'Contraseña fuerte', cls: 'password-strong' };
  return { label: 'Aceptable — mejora con mayúscula y número', cls: 'password-medium' };
}

export default function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const strength = getPasswordStrength(password);
  const passwordValid = password.length >= 6;
  const confirmValid = password === confirm && confirm.length > 0;
  const canSubmit = passwordValid && confirmValid;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await client.post('/api/auth/register', { username, email, password });
      navigate('/login');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || 'Error al registrar el usuario');
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">AA</div>
        <div className="auth-subtitle">Registro de usuario</div>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="form-label">Usuario</label>
            <input type="text" className="form-control" value={username}
              onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="mb-4">
            <label className="form-label">Email</label>
            <input type="email" className="form-control" value={email}
              onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="mb-4">
            <label className="form-label">Contraseña</label>
            <input type="password" className="form-control" value={password}
              onChange={(e) => setPassword(e.target.value)} required />
            <div className="mt-2">
              <small className={strength.cls}>{strength.label}</small>
            </div>
          </div>
          <div className="mb-4">
            <label className="form-label">Confirmar contraseña</label>
            <input type="password" className="form-control" value={confirm}
              onChange={(e) => setConfirm(e.target.value)} required />
            <div className="mt-2">
              {confirm.length > 0 && (
                <small className={confirmValid ? 'password-strong' : 'password-weak'}>
                  {confirmValid ? 'Las contraseñas coinciden' : 'Las contraseñas no coinciden'}
                </small>
              )}
            </div>
          </div>
          <button type="submit" className="btn-submit" disabled={!canSubmit}>
            Registrarse
          </button>
        </form>

        {error && <div className="alert-dark-danger">{error}</div>}

        <div className="auth-footer">
          <Link to="/login" className="auth-link">← Volver al login</Link>
        </div>
      </div>
    </div>
  );
}
