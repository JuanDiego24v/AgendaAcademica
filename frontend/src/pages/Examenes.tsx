import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import client from '../api/client';
import type { Curso, Examen } from '../types';

interface PageData {
  examenes: Examen[];
  cursos: Curso[];
  cursoSeleccionado: number;
  notaMaxima: number;
  notaMinimaAprobatoria: number;
  promedioCurso: number;
  porcentajeEvaluado: number;
  porcentajePendiente: number;
}

interface ExamenForm {
  nombre: string;
  fecha: string;
  porcentaje: string;
  estado: 'PENDIENTE' | 'COMPLETADO';
  nota: string;
}

const emptyForm: ExamenForm = { nombre: '', fecha: '', porcentaje: '', estado: 'PENDIENTE', nota: '' };

const formatDateInput = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
};

const toISO = (dmy: string) => {
  const [d, m, y] = dmy.split('/');
  return `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`;
};

const fromISO = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

export default function Examenes() {
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState<PageData | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showSistemaModal, setShowSistemaModal] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<ExamenForm>(emptyForm);
  const [sistemaForm, setSistemaForm] = useState({ notaMaxima: '', notaMinimaAprobatoria: '' });
  const [sliderValues, setSliderValues] = useState<Record<number, number>>({});
  const silaboPending = params.get('openModal') === 'true';

  const cursoId = Number(params.get('cursoId') || 0);

  const load = async (cId?: number) => {
    const q = cId ? `?cursoId=${cId}` : cursoId ? `?cursoId=${cursoId}` : '';
    const r = await client.get<PageData>(`/api/examenes${q}`);
    setData(r.data);
    const initial: Record<number, number> = {};
    r.data.examenes.forEach(e => { initial[e.id] = e.nota ?? 0; });
    setSliderValues(initial);
    setSistemaForm({ notaMaxima: String(r.data.notaMaxima), notaMinimaAprobatoria: String(r.data.notaMinimaAprobatoria) });
  };

  useEffect(() => {
    load();
    if (silaboPending) setShowAddModal(true);
  }, []);

  const changeCurso = (id: number) => {
    setParams({ cursoId: String(id) });
    load(id);
  };

  const handleSaveExamen = async () => {
    const payload = {
      nombre: form.nombre, fecha: toISO(form.fecha),
      porcentaje: Number(form.porcentaje), estado: form.estado,
      nota: form.estado === 'COMPLETADO' && form.nota ? Number(form.nota) : null,
      cursoId: data?.cursoSeleccionado,
    };
    await client.post('/api/examenes', payload);
    setShowAddModal(false);
    setForm(emptyForm);
    load();
  };

  const handleEditExamen = async () => {
    if (!editId) return;
    const payload = {
      nombre: form.nombre, fecha: toISO(form.fecha),
      porcentaje: Number(form.porcentaje), estado: form.estado,
      nota: form.estado === 'COMPLETADO' && form.nota ? Number(form.nota) : null,
    };
    await client.put(`/api/examenes/${editId}`, payload);
    setShowEditModal(false);
    setForm(emptyForm);
    setEditId(null);
    load();
  };

  const openEdit = (e: Examen) => {
    setEditId(e.id);
    setForm({ nombre: e.nombre, fecha: fromISO(e.fecha), porcentaje: String(e.porcentaje), estado: e.estado, nota: e.nota != null ? String(e.nota) : '' });
    setShowEditModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Eliminar examen?')) return;
    await client.delete(`/api/examenes/${id}`);
    load();
  };

  const handleSaveSistema = async () => {
    await client.post('/api/examenes/usuario/actualizar-sistema', {
      notaMaxima: Number(sistemaForm.notaMaxima),
      notaMinimaAprobatoria: Number(sistemaForm.notaMinimaAprobatoria),
    });
    setShowSistemaModal(false);
    load();
  };

  const handleSilabo = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    fd.append('cursoId', String(data?.cursoSeleccionado));
    await client.post('/api/ia/silabo/subir', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
    setShowAddModal(false);
    load();
  };

  const sliderBg = (e: Examen) => {
    const val = sliderValues[e.id] ?? 0;
    const pct = (val / (data?.notaMaxima ?? 20)) * 100;
    if (e.estado === 'COMPLETADO') {
      return { background: `linear-gradient(to right, var(--orange-dim) ${pct}%, rgba(232,118,32,0.08) ${pct}%)` };
    }
    return { background: `linear-gradient(to right, var(--blue) ${pct}%, rgba(61,123,255,0.15) ${pct}%)` };
  };

  // Proyección
  const proyPromedio = data ? data.examenes.reduce((acc, e) => acc + (sliderValues[e.id] ?? 0) * (e.porcentaje / 100), 0) : 0;
  const proyPct = data ? data.examenes.reduce((acc, e) => acc + e.porcentaje, 0) : 0;
  const notaMin = data?.notaMinimaAprobatoria ?? 11;
  const proyEstado = proyPct === 0 ? '—' : proyPromedio >= notaMin ? '✓ Aprobado' : '✗ No aprueba';
  const proyEstadoCls = proyPct === 0 ? '' : proyPromedio >= notaMin ? ' passing' : ' failing';

  if (!data) return <Layout><div style={{ color: 'var(--muted)', fontFamily: "'Space Mono',monospace", fontSize: 12 }}>Cargando...</div></Layout>;

  const examenFormFields = (
    <>
      <div className="mb-3">
        <label className="form-label">Nombre</label>
        <input type="text" className="form-control" value={form.nombre} onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))} required />
      </div>
      <div className="mb-3">
        <label className="form-label">Fecha</label>
        <input type="text" className="form-control" placeholder="DD/MM/AAAA" maxLength={10}
          value={form.fecha}
          onChange={e => setForm(f => ({ ...f, fecha: formatDateInput(e.target.value) }))}
          required />
      </div>
      <div className="mb-3">
        <label className="form-label">Porcentaje</label>
        <input type="number" step="0.1" min="0" max="100" className="form-control" value={form.porcentaje} onChange={e => setForm(f => ({ ...f, porcentaje: e.target.value }))} required />
      </div>
      <div className="mb-3">
        <label className="form-label">Estado</label>
        <select className="form-select" value={form.estado} onChange={e => setForm(f => ({ ...f, estado: e.target.value as 'PENDIENTE' | 'COMPLETADO', nota: '' }))}>
          <option value="PENDIENTE">Falta revisar</option>
          <option value="COMPLETADO">Examen revisado</option>
        </select>
      </div>
      <div className="mb-3">
        <label className="form-label">Nota</label>
        <input type="number" step="0.1" min="0" max={data?.notaMaxima} className="form-control"
          value={form.nota} onChange={e => setForm(f => ({ ...f, nota: e.target.value }))}
          disabled={form.estado !== 'COMPLETADO'} />
      </div>
    </>
  );

  return (
    <Layout>
      <div className="page-eyebrow">Gestión</div>
      <h1 className="page-title">MIS<br /><span>EXÁMENES</span></h1>
      <div className="header-line" />

      {/* SISTEMA + PROMEDIO */}
      <div className="row g-3 mb-4">
        <div className="col-md-6">
          <div className="info-card h-100">
            <div className="info-label">Sistema de evaluación</div>
            <div className="info-row"><span className="info-key">Nota máxima</span><span className="info-val">{data.notaMaxima}</span></div>
            <div className="info-row"><span className="info-key">Mínimo aprobatorio</span><span className="info-val">{data.notaMinimaAprobatoria}</span></div>
            <button className="btn-outline-action mt-4" onClick={() => setShowSistemaModal(true)}>Editar sistema</button>
          </div>
        </div>
        <div className="col-md-6">
          <div className="promedio-card h-100 d-flex" style={{ flexDirection: 'column', justifyContent: 'center' }}>
            <div className="promedio-label">Promedio real del curso</div>
            <div className="promedio-value">{data.promedioCurso.toFixed(2)}</div>
            <div style={{ fontFamily: "'Space Mono',monospace", fontSize: 10, color: 'var(--muted)', marginTop: 8, letterSpacing: 1 }}>
              Evaluado: {data.porcentajeEvaluado.toFixed(1)}% &nbsp;·&nbsp; Pendiente: {data.porcentajePendiente.toFixed(1)}%
            </div>
          </div>
        </div>
      </div>

      {/* SELECTOR + BOTÓN */}
      <div className="d-flex align-items-center gap-3 mb-4 flex-wrap">
        <select className="form-select" style={{ maxWidth: 280 }} value={data.cursoSeleccionado}
          onChange={e => changeCurso(Number(e.target.value))}>
          {data.cursos.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <button className="btn-add" onClick={() => { setForm(emptyForm); setShowAddModal(true); }}>+ Añadir examen</button>
      </div>

      {/* TABLA */}
      <div className="section-title">Exámenes del curso</div>
      <div className="table-responsive mb-5">
        <table className="dark-table">
          <thead>
            <tr>
              <th>Nombre</th><th>Fecha</th><th>Estado</th><th>Nota</th><th>%</th>
              <th style={{ textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {data.examenes.map(e => (
              <tr key={e.id}>
                <td><span className="exam-name">{e.nombre}</span></td>
                <td><span style={{ fontFamily: "'Space Mono',monospace", fontSize: 11, color: 'var(--muted)' }}>{e.fecha}</span></td>
                <td><span className={`badge-pill ${e.estado === 'COMPLETADO' ? 'badge-completado' : 'badge-pendiente'}`}>{e.estado === 'COMPLETADO' ? 'Revisado' : 'Pendiente'}</span></td>
                <td>{e.nota != null ? <span className="nota-valor">{e.nota}</span> : <span className="nota-pendiente">—</span>}</td>
                <td><span style={{ fontFamily: "'Space Mono',monospace", fontSize: 11, color: 'var(--orange)' }}>{e.porcentaje}</span><span style={{ fontFamily: "'Space Mono',monospace", fontSize: 11, color: 'var(--muted)' }}>%</span></td>
                <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                  <button className="btn-action btn-action-edit me-2" onClick={() => openEdit(e)}>Editar</button>
                  <button className="btn-action btn-action-delete" onClick={() => handleDelete(e.id)}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* PROYECCIÓN */}
      {data.examenes.length > 0 && (
        <>
          <div className="section-title">Simulación de notas</div>
          <div className="proy-wrap">
            <div className="proy-eyebrow">Modo simulación</div>
            <div className="proy-title">Proyección de aprobación</div>
            <div className="proy-subtitle">Mové los sliders para simular distintos escenarios. Los exámenes revisados arrancan con su nota real.</div>
            <div className="proy-metrics">
              <div className="proy-metric"><div className="proy-metric-label">Promedio proyectado</div><div className={`proy-metric-value${proyEstadoCls}`}>{proyPromedio.toFixed(2)}</div></div>
              <div className="proy-metric"><div className="proy-metric-label">% cubierto</div><div className="proy-metric-value">{proyPct.toFixed(0)}%</div></div>
              <div className="proy-metric"><div className="proy-metric-label">Estado</div><div className={`proy-metric-value${proyEstadoCls}`}>{proyEstado}</div></div>
            </div>
            <div className="proy-divider" />
            {data.examenes.map(e => (
              <div key={e.id} className="proy-exam-row">
                <div className="proy-exam-info">
                  <span className="proy-exam-name">{e.nombre}</span>
                  <span className="proy-exam-meta">{e.porcentaje}% del total</span>
                </div>
                <div className="proy-slider-wrap">
                  <input
                    type="range" className="proy-slider"
                    min={0} max={data.notaMaxima} step={0.1}
                    value={sliderValues[e.id] ?? 0}
                    disabled={e.estado === 'COMPLETADO'}
                    style={sliderBg(e)}
                    onChange={ev => setSliderValues(s => ({ ...s, [e.id]: Number(ev.target.value) }))}
                  />
                  <span className="proy-slider-value" style={e.estado === 'COMPLETADO' ? { color: 'var(--orange)' } : undefined}>
                    {(sliderValues[e.id] ?? 0).toFixed(1)}
                  </span>
                </div>
                <span className={`badge-pill ${e.estado === 'COMPLETADO' ? 'badge-completado' : 'badge-pendiente'}`}>
                  {e.estado === 'COMPLETADO' ? 'Revisado' : 'Pendiente'}
                </span>
              </div>
            ))}
          </div>
        </>
      )}

      {/* MODAL NUEVO EXAMEN */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-dialog" onClick={e => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="modal-header">
              <span className="modal-title">Nuevo examen</span>
              <button className="modal-close" onClick={() => setShowAddModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              {examenFormFields}
              <div style={{ textAlign: 'right' }}>
                <button className="btn-save" onClick={handleSaveExamen}>Guardar</button>
              </div>
              <hr className="modal-divider" />
              <div className="ia-section-title">Lector de sílabo (IA)</div>
              <form onSubmit={handleSilabo}>
                <div className="drop-zone mb-3">
                  <div className="drop-zone-label">Arrastra tu sílabo (PDF) o haz clic</div>
                  <input className="form-control" type="file" name="file" accept="application/pdf" required />
                </div>
                <div style={{ textAlign: 'right' }}>
                  <button className="btn-modal-secondary" type="submit">Extraer y guardar</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* MODAL EDITAR EXAMEN */}
      {showEditModal && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-dialog" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">Editar examen</span>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              {examenFormFields}
            </div>
            <div className="modal-footer">
              <button className="btn-save" onClick={handleEditExamen}>Guardar cambios</button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL SISTEMA */}
      {showSistemaModal && (
        <div className="modal-overlay" onClick={() => setShowSistemaModal(false)}>
          <div className="modal-dialog" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">Editar sistema</span>
              <button className="modal-close" onClick={() => setShowSistemaModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="mb-3">
                <label className="form-label">Nota máxima</label>
                <input type="number" step="0.1" min="0" className="form-control"
                  value={sistemaForm.notaMaxima} onChange={e => setSistemaForm(s => ({ ...s, notaMaxima: e.target.value }))} />
              </div>
              <div className="mb-3">
                <label className="form-label">Nota mínima aprobatoria</label>
                <input type="number" step="0.1" min="0" className="form-control"
                  value={sistemaForm.notaMinimaAprobatoria} onChange={e => setSistemaForm(s => ({ ...s, notaMinimaAprobatoria: e.target.value }))} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-save" onClick={handleSaveSistema}>Guardar cambios</button>
            </div>
          </div>
        </div>
      )}

    </Layout>
  );
}
