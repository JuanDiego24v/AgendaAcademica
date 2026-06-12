import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

export default function WelcomeModal() {
  const { username, refreshPeriodo } = useAuth();
  const [form, setForm] = useState({ nombre: '', fechaInicio: '', semanas: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await client.post('/api/periodos', { ...form, semanas: Number(form.semanas) });
      await refreshPeriodo();
    } catch {
      setError('Error al guardar. Verificá los datos e intentá de nuevo.');
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0,
      background: 'rgba(0,0,0,0.85)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 2000, padding: '24px 16px',
    }}>
      <div style={{
        background: 'var(--black-card)',
        border: '1px solid var(--border)',
        borderRadius: 4,
        width: '100%', maxWidth: 480,
        position: 'relative',
        overflow: 'hidden',
        animation: 'fadeUp 0.4s ease both',
      }}>
        {/* top accent line */}
        <div style={{
          position: 'absolute', top: 0, left: 0, right: 0, height: 2,
          background: 'linear-gradient(90deg, var(--orange), transparent)',
        }} />

        <div style={{ padding: '40px 36px 36px' }}>
          {/* eyebrow */}
          <div className="page-eyebrow" style={{ opacity: 1, animation: 'none', marginBottom: 8 }}>
            Bienvenido
          </div>

          {/* title */}
          <div style={{
            fontFamily: "'Bebas Neue', sans-serif",
            fontSize: 'clamp(36px, 8vw, 52px)',
            lineHeight: 0.95, letterSpacing: 1,
            color: 'var(--white)', marginBottom: 8,
          }}>
            HOLA,<br />
            <span style={{ color: 'var(--orange)' }}>{username?.toUpperCase()}</span>
          </div>

          <div style={{
            fontFamily: "'Space Mono', monospace",
            fontSize: 11, letterSpacing: 2,
            color: 'var(--muted)', textTransform: 'uppercase',
            marginBottom: 32,
          }}>
            Configurá tu periodo estudiantil para comenzar
          </div>

          <div style={{ height: 1, background: 'var(--border)', marginBottom: 28 }} />

          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label">¿Cómo se llama tu periodo?</label>
              <input
                type="text"
                className="form-control"
                placeholder="Ej: Ciclo I 2026, Semestre 2026-1, Trimestre 3…"
                value={form.nombre}
                onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))}
                required
                autoFocus
              />
            </div>

            <div className="mb-3">
              <label className="form-label">¿Cuándo empieza?</label>
              <input
                type="date"
                className="form-control"
                value={form.fechaInicio}
                onChange={e => setForm(f => ({ ...f, fechaInicio: e.target.value }))}
                required
              />
            </div>

            <div className="mb-4">
              <label className="form-label">Duración en semanas</label>
              <input
                type="number"
                className="form-control"
                min={1} max={52}
                value={form.semanas}
                onChange={e => setForm(f => ({ ...f, semanas: e.target.value }))}
                required
              />
            </div>

            {error && <div className="alert-dark-danger mb-3">{error}</div>}

            <button type="submit" className="btn-submit" disabled={loading}>
              {loading ? 'Guardando...' : 'Comenzar →'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
