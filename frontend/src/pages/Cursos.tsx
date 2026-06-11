import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import client from '../api/client';
import type { Curso } from '../types';

interface ModalState {
  open: boolean;
  id: number | null;
  nombre: string;
}

export default function Cursos() {
  const [cursos, setCursos] = useState<Curso[]>([]);
  const [modal, setModal] = useState<ModalState>({ open: false, id: null, nombre: '' });

  const load = () => client.get<Curso[]>('/api/cursos').then(r => setCursos(r.data));

  useEffect(() => { load(); }, []);

  const openAdd = () => setModal({ open: true, id: null, nombre: '' });
  const openEdit = (c: Curso) => setModal({ open: true, id: c.id, nombre: c.nombre });
  const closeModal = () => setModal({ open: false, id: null, nombre: '' });

  const handleSave = async () => {
    if (!modal.nombre.trim()) return;
    if (modal.id) {
      await client.put(`/api/cursos/${modal.id}`, { nombre: modal.nombre });
    } else {
      await client.post('/api/cursos', { nombre: modal.nombre });
    }
    closeModal();
    load();
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Eliminar este curso?')) return;
    await client.delete(`/api/cursos/${id}`);
    load();
  };

  return (
    <Layout>
      <div className="page-eyebrow">Gestión</div>
      <h1 className="page-title">MIS<br /><span>CURSOS</span></h1>
      <div className="header-line" />

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="section-title" style={{ flex: 1, marginBottom: 0 }}>Lista de cursos</div>
        <button className="btn-add ms-3" onClick={openAdd}>+ Añadir Curso</button>
      </div>

      <table className="dark-table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th style={{ textAlign: 'right' }}>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {cursos.length === 0 ? (
            <tr className="empty-row"><td colSpan={2}>Aún no tienes cursos registrados</td></tr>
          ) : cursos.map(c => (
            <tr key={c.id}>
              <td><span className="course-name">{c.nombre}</span></td>
              <td style={{ textAlign: 'right' }}>
                <button className="btn-action btn-action-edit me-2" onClick={() => openEdit(c)}>Editar</button>
                <button className="btn-action btn-action-delete" onClick={() => handleDelete(c.id)}>Eliminar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {modal.open && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-dialog" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">{modal.id ? 'Editar curso' : 'Añadir curso'}</span>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <div className="modal-body">
              <label className="form-label">Nombre del curso</label>
              <input
                type="text"
                className="form-control"
                value={modal.nombre}
                onChange={e => setModal(m => ({ ...m, nombre: e.target.value }))}
                onKeyDown={e => e.key === 'Enter' && handleSave()}
                autoFocus
              />
            </div>
            <div className="modal-footer">
              <button className="btn-save" onClick={handleSave}>Guardar</button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
