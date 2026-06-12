import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

interface PeriodoData {
  id: number;
  nombre: string;
  fechaInicio: string;
  semanas: number;
}

interface PerfilData {
  username: string;
  email: string;
  periodoActivo: PeriodoData | null;
  totalCursos: number;
  totalExamenes: number;
}

export default function Perfil() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const setupMode = params.get('setup') === 'true';

  const [perfil, setPerfil] = useState<PerfilData | null>(null);
  const [showNuevoPeriodo, setShowNuevoPeriodo] = useState(false);
  const [periodoForm, setPeriodoForm] = useState({ nombre: '', fechaInicio: '', semanas: 18 });
  const [periodoLoading, setPeriodoLoading] = useState(false);
  const [periodoError, setPeriodoError] = useState('');
  const [periodoSuccess, setPeriodoSuccess] = useState('');

  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwLoading, setPwLoading] = useState(false);
  const [pwError, setPwError] = useState('');

  useEffect(() => {
    client.get<PerfilData>('/api/perfil')
      .then(r => {
        setPerfil(r.data);
        if (setupMode || !r.data.periodoActivo) {
          setShowNuevoPeriodo(true);
        }
      })
      .catch(() => {
        if (setupMode) setShowNuevoPeriodo(true);
      });
  }, [setupMode]);

  const handlePeriodoSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPeriodoLoading(true);
    setPeriodoError('');
    setPeriodoSuccess('');
    try {
      await client.post('/api/periodos', periodoForm);
      const r = await client.get<PerfilData>('/api/perfil');
      setPerfil(r.data);
      setPeriodoSuccess('Periodo guardado correctamente.');
      setShowNuevoPeriodo(false);
      navigate('/home');
    } catch {
      setPeriodoError('Error al guardar el periodo. Verificá los datos.');
    } finally {
      setPeriodoLoading(false);
    }
  };

  const handlePwSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwError('');
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError('Las contraseñas no coinciden.');
      return;
    }
    setPwLoading(true);
    try {
      await client.post('/api/change-password', {
        currentPassword: pwForm.currentPassword,
        newPassword: pwForm.newPassword,
      });
      logout();
      navigate('/login?passwordChanged=true');
    } catch {
      setPwError('Contraseña actual incorrecta.');
    } finally {
      setPwLoading(false);
    }
  };

  return (
    <Layout>
      {/* ── Sección 1: Perfil ── */}
      <div className="page-eyebrow">Configuración</div>
      <h1 className="page-title">MI<br /><span>PERFIL</span></h1>
      <div className="header-line" />

      {/* Card de perfil */}
      <div className="stat-card mb-4" style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', padding: '1.5rem 2rem' }}>
        <div style={{ fontFamily: "'Bebas Neue', sans-serif", fontSize: '3rem', color: '#E87620', lineHeight: 1 }}>◉</div>
        <div>
          <div style={{ fontFamily: "'Bebas Neue', sans-serif", fontSize: '2rem', letterSpacing: '0.05em', color: '#f0f0f0' }}>
            {perfil?.username ?? '—'}
          </div>
          <div style={{ fontFamily: "'Space Mono', monospace", fontSize: '0.8rem', color: '#888', marginTop: '0.25rem' }}>
            {perfil?.email ?? '—'}
          </div>
        </div>
      </div>

      {/* Stat cards */}
      <div className="row g-3 mb-5">
        <div className="col-6 col-md-3">
          <div className="stat-card">
            <div className="stat-number" style={{ fontSize: '1.1rem' }}>
              {perfil?.periodoActivo?.nombre ?? 'Sin periodo'}
            </div>
            <div className="stat-label">Periodo activo</div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="stat-card">
            <div className="stat-number">{perfil?.periodoActivo?.semanas ?? '—'}</div>
            <div className="stat-label">Semanas</div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="stat-card">
            <div className="stat-number">{perfil?.totalCursos ?? '—'}</div>
            <div className="stat-label">Cursos</div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="stat-card">
            <div className="stat-number">{perfil?.totalExamenes ?? '—'}</div>
            <div className="stat-label">Exámenes</div>
          </div>
        </div>
      </div>

      {/* ── Sección 2: Seguridad ── */}
      <div className="section-title">Seguridad</div>

      <form onSubmit={handlePwSubmit} style={{ maxWidth: '480px' }} className="mb-5">
        {pwError && <div className="alert-dark-danger mb-3">{pwError}</div>}
        <div className="mb-3">
          <label className="form-label">Contraseña actual</label>
          <input
            type="password"
            className="form-control"
            value={pwForm.currentPassword}
            onChange={e => setPwForm(f => ({ ...f, currentPassword: e.target.value }))}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Nueva contraseña</label>
          <input
            type="password"
            className="form-control"
            value={pwForm.newPassword}
            onChange={e => setPwForm(f => ({ ...f, newPassword: e.target.value }))}
            required
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Confirmar contraseña</label>
          <input
            type="password"
            className="form-control"
            value={pwForm.confirmPassword}
            onChange={e => setPwForm(f => ({ ...f, confirmPassword: e.target.value }))}
            required
          />
        </div>
        <button type="submit" className="btn-submit" disabled={pwLoading}>
          {pwLoading ? 'Guardando...' : 'Cambiar contraseña'}
        </button>
      </form>

      {/* ── Sección 3: Periodo estudiantil ── */}
      <div className="section-title">Periodo estudiantil</div>

      {perfil?.periodoActivo && !showNuevoPeriodo ? (
        <div className="mb-4">
          <div style={{ fontFamily: "'Bebas Neue', sans-serif", fontSize: '2rem', color: '#f0f0f0', letterSpacing: '0.05em' }}>
            {perfil.periodoActivo.nombre}
          </div>
          <div style={{ fontFamily: "'Space Mono', monospace", fontSize: '0.8rem', color: '#888', marginTop: '0.4rem' }}>
            Inicio: {perfil.periodoActivo.fechaInicio} · {perfil.periodoActivo.semanas} semanas
          </div>
          <button
            className="btn-outline-action mt-3"
            onClick={() => { setShowNuevoPeriodo(true); setPeriodoError(''); setPeriodoSuccess(''); }}
          >
            Nuevo periodo
          </button>
        </div>
      ) : null}

      {showNuevoPeriodo && (
        <form onSubmit={handlePeriodoSubmit} style={{ maxWidth: '480px' }} className="mb-5">
          {!perfil?.periodoActivo && (
            <p style={{ fontFamily: "'Space Mono', monospace", fontSize: '0.85rem', color: '#888', marginBottom: '1.25rem' }}>
              Para comenzar, configura tu periodo estudiantil actual
            </p>
          )}
          {periodoError && <div className="alert-dark-danger mb-3">{periodoError}</div>}
          {periodoSuccess && <div className="alert-dark-success mb-3">{periodoSuccess}</div>}
          <div className="mb-3">
            <label className="form-label">Nombre del periodo</label>
            <input
              type="text"
              className="form-control"
              placeholder="Ej: Ciclo 2026 — I"
              value={periodoForm.nombre}
              onChange={e => setPeriodoForm(f => ({ ...f, nombre: e.target.value }))}
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Fecha de inicio</label>
            <input
              type="date"
              className="form-control"
              value={periodoForm.fechaInicio}
              onChange={e => setPeriodoForm(f => ({ ...f, fechaInicio: e.target.value }))}
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Duración (semanas)</label>
            <input
              type="number"
              className="form-control"
              min={1}
              max={52}
              value={periodoForm.semanas}
              onChange={e => setPeriodoForm(f => ({ ...f, semanas: Number(e.target.value) }))}
              required
            />
          </div>
          <div className="d-flex gap-3 flex-wrap">
            <button type="submit" className="btn-submit" disabled={periodoLoading}>
              {periodoLoading ? 'Guardando...' : 'Guardar periodo'}
            </button>
            {perfil?.periodoActivo && (
              <button type="button" className="btn-outline-action" onClick={() => setShowNuevoPeriodo(false)}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      )}
    </Layout>
  );
}
