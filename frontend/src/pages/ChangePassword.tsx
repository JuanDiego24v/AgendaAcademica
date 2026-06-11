import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function ChangePassword() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (newPassword !== confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }
    try {
      await client.post('/api/change-password', { currentPassword, newPassword });
      logout();
      navigate('/login?passwordChanged=true');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || 'Error al cambiar la contraseña');
    }
  };

  return (
    <Layout>
      <div className="page-eyebrow">Cuenta</div>
      <h1 className="page-title">CAMBIAR<br /><span>CONTRASEÑA</span></h1>
      <div className="header-line" />

      <div style={{ maxWidth: 400 }}>
        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="form-label">Contraseña actual</label>
            <input type="password" className="form-control" value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)} required />
          </div>
          <div className="mb-4">
            <label className="form-label">Nueva contraseña</label>
            <input type="password" className="form-control" value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)} required />
          </div>
          <div className="mb-4">
            <label className="form-label">Confirmar nueva contraseña</label>
            <input type="password" className="form-control" value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)} required />
          </div>
          <button type="submit" className="btn-submit">Guardar cambios</button>
        </form>
        {error && <div className="alert-dark-danger" style={{ marginTop: 16 }}>{error}</div>}
      </div>
    </Layout>
  );
}
