import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

interface PerfilData {
  username: string;
  email: string;
  periodoActivo: { id: number; nombre: string; fechaInicio: string; semanas: number } | null;
  totalCursos: number;
  totalExamenes: number;
}

export default function Perfil() {
  const { logout, periodoActivo, refreshPeriodo } = useAuth();
  const navigate = useNavigate();

  const [perfil, setPerfil] = useState<PerfilData | null>(null);
  const [showNuevoPeriodo, setShowNuevoPeriodo] = useState(false);
  const [periodoForm, setPeriodoForm] = useState({ nombre: '', fechaInicio: '', semanas: 18 });
  const [periodoLoading, setPeriodoLoading] = useState(false);
  const [periodoError, setPeriodoError] = useState('');
  const [periodoSuccess, setPeriodoSuccess] = useState('');

  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwLoading, setPwLoading] = useState(false);
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState('');

  useEffect(() => {
    client.get<PerfilData>('/api/perfil').then(r => setPerfil(r.data)).catch(() => {});
  }, [periodoActivo]);

  const handlePeriodoSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPeriodoLoading(true);
    setPeriodoError('');
    setPeriodoSuccess('');
    try {
      await client.post('/api/periodos', periodoForm);
      await refreshPeriodo();
      setPeriodoSuccess('Periodo guardado correctamente.');
      setShowNuevoPeriodo(false);
      setPeriodoForm({ nombre: '', fechaInicio: '', semanas: 18 });
    } catch {
      setPeriodoError('Error al guardar el periodo. Verificá los datos.');
    } finally {
      setPeriodoLoading(false);
    }
  };

  const handlePwSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwError('');
    setPwSuccess('');
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
      <div className="page-eyebrow">Cuenta</div>
      <h1 className="page-title">CONFIGURACIÓN<br /><span>DE PERFIL</span></h1>
      <div className="header-line" />

      {/* ── PERFIL ── */}
      <div className="section-title">Perfil</div>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 24,
        background: 'var(--black-card)', border: '1px solid var(--border)',
        borderRadius: 4, padding: '28px 32px', marginBottom: 40,
      }}>
        <div style={{
          width: 64, height: 64, borderRadius: '50%',
          background: 'var(--orange-pale)', border: '2px solid var(--orange-line)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: "'Bebas Neue', sans-serif", fontSize: 28, color: 'var(--orange)',
          flexShrink: 0,
        }}>
          {perfil?.username?.[0]?.toUpperCase() ?? '?'}
        </div>
        <div>
          <div style={{ fontFamily: "'Bebas Neue', sans-serif", fontSize: 32, letterSpacing: 1, color: 'var(--white)', lineHeight: 1 }}>
            {perfil?.username ?? '—'}
          </div>
          <div style={{ fontFamily: "'Space Mono', monospace", fontSize: 11, color: 'var(--muted)', marginTop: 6, letterSpacing: 1 }}>
            {perfil?.email ?? '—'}
          </div>
        </div>
      </div>

      {/* STATS */}
      <div className="row g-3 mb-5">
        {[
          { label: 'Periodo activo', value: perfil?.periodoActivo?.nombre ?? '—', small: true },
          { label: 'Semanas', value: String(perfil?.periodoActivo?.semanas ?? '—') },
          { label: 'Cursos', value: String(perfil?.totalCursos ?? '—') },
          { label: 'Exámenes', value: String(perfil?.totalExamenes ?? '—') },
        ].map((s, i) => (
          <div key={i} className="col-6 col-md-3">
            <div className="stat-card" style={{ animationDelay: `${0.1 * i}s` }}>
              <div className="stat-number" style={s.small ? { fontSize: 18, lineHeight: 1.3, marginTop: 4 } : undefined}>
                {s.value}
              </div>
              <div className="stat-label">{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* ── PERIODO ESTUDIANTIL ── */}
      <div className="section-title">Periodo estudiantil</div>

      {perfil?.periodoActivo && !showNuevoPeriodo ? (
        <div style={{
          background: 'var(--black-card)', border: '1px solid var(--border)',
          borderLeft: '3px solid var(--orange)', borderRadius: 4,
          padding: '24px 28px', marginBottom: 40,
        }}>
          <div style={{ fontFamily: "'Bebas Neue', sans-serif", fontSize: 28, color: 'var(--white)', letterSpacing: 1, marginBottom: 6 }}>
            {perfil.periodoActivo.nombre}
          </div>
          <div style={{ fontFamily: "'Space Mono', monospace", fontSize: 10, color: 'var(--muted)', letterSpacing: 1.5, textTransform: 'uppercase', marginBottom: 20 }}>
            Inicio: {perfil.periodoActivo.fechaInicio} &nbsp;·&nbsp; {perfil.periodoActivo.semanas} semanas
          </div>
          <button className="btn-outline-action" onClick={() => { setShowNuevoPeriodo(true); setPeriodoError(''); setPeriodoSuccess(''); }}>
            Iniciar nuevo periodo
          </button>
        </div>
      ) : null}

      {showNuevoPeriodo && (
        <div style={{
          background: 'var(--black-card)', border: '1px solid var(--border)',
          borderRadius: 4, padding: '28px 32px', marginBottom: 40, maxWidth: 480,
        }}>
          <div style={{ fontFamily: "'Space Mono', monospace", fontSize: 10, color: 'var(--muted)', letterSpacing: 2, textTransform: 'uppercase', marginBottom: 20 }}>
            Esto archivará el periodo actual y creará uno nuevo
          </div>
          {periodoError && <div className="alert-dark-danger mb-3">{periodoError}</div>}
          {periodoSuccess && <div className="alert-dark-success mb-3">{periodoSuccess}</div>}
          <form onSubmit={handlePeriodoSubmit}>
            <div className="mb-3">
              <label className="form-label">Nombre del periodo</label>
              <input type="text" className="form-control" placeholder="Ej: Ciclo II 2026"
                value={periodoForm.nombre} onChange={e => setPeriodoForm(f => ({ ...f, nombre: e.target.value }))} required />
            </div>
            <div className="mb-3">
              <label className="form-label">Fecha de inicio</label>
              <input type="date" className="form-control"
                value={periodoForm.fechaInicio} onChange={e => setPeriodoForm(f => ({ ...f, fechaInicio: e.target.value }))} required />
            </div>
            <div className="mb-4">
              <label className="form-label">Duración (semanas)</label>
              <input type="number" className="form-control" min={1} max={52}
                value={periodoForm.semanas} onChange={e => setPeriodoForm(f => ({ ...f, semanas: Number(e.target.value) }))} required />
            </div>
            <div className="d-flex gap-3 flex-wrap">
              <button type="submit" className="btn-add" disabled={periodoLoading}>
                {periodoLoading ? 'Guardando...' : 'Guardar periodo'}
              </button>
              <button type="button" className="btn-outline-action" onClick={() => setShowNuevoPeriodo(false)}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── SEGURIDAD ── */}
      <div className="section-title">Seguridad</div>
      <div style={{
        background: 'var(--black-card)', border: '1px solid var(--border)',
        borderRadius: 4, padding: '28px 32px', maxWidth: 480, marginBottom: 40,
      }}>
        {pwError && <div className="alert-dark-danger mb-3">{pwError}</div>}
        {pwSuccess && <div className="alert-dark-success mb-3">{pwSuccess}</div>}
        <form onSubmit={handlePwSubmit}>
          <div className="mb-3">
            <label className="form-label">Contraseña actual</label>
            <input type="password" className="form-control"
              value={pwForm.currentPassword} onChange={e => setPwForm(f => ({ ...f, currentPassword: e.target.value }))} required />
          </div>
          <div className="mb-3">
            <label className="form-label">Nueva contraseña</label>
            <input type="password" className="form-control"
              value={pwForm.newPassword} onChange={e => setPwForm(f => ({ ...f, newPassword: e.target.value }))} required />
          </div>
          <div className="mb-4">
            <label className="form-label">Confirmar nueva contraseña</label>
            <input type="password" className="form-control"
              value={pwForm.confirmPassword} onChange={e => setPwForm(f => ({ ...f, confirmPassword: e.target.value }))} required />
          </div>
          <button type="submit" className="btn-submit" disabled={pwLoading}>
            {pwLoading ? 'Guardando...' : 'Cambiar contraseña'}
          </button>
        </form>
      </div>
    </Layout>
  );
}
