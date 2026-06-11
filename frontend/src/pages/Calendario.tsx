import { useEffect, useRef, useState } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import Layout from '../components/Layout';
import client from '../api/client';
import type { Examen, CalendarEvent } from '../types';

function daysLeft(fecha: string): number {
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const d = new Date(fecha); d.setHours(0, 0, 0, 0);
  return Math.round((d.getTime() - today.getTime()) / 86400000);
}

function dotClass(days: number) {
  if (days < 7) return 'dot-urgent';
  if (days < 14) return 'dot-soon';
  if (days < 30) return 'dot-ok';
  return 'dot-future';
}
function daysClass(days: number) {
  if (days < 7) return 'days-urgent';
  if (days < 14) return 'days-soon';
  return 'days-ok';
}

export default function Calendario() {
  const [examenes, setExamenes] = useState<Examen[]>([]);
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [highlighted, setHighlighted] = useState<string | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    client.get<Examen[]>('/api/calendario').then(r => {
      setExamenes(r.data);
      setEvents(r.data.map(e => ({
        id: String(e.id),
        title: e.nombre,
        start: e.fecha,
        extendedProps: { curso: e.curso.nombre, porcentaje: e.porcentaje },
      })));
    });
  }, []);

  const highlightRow = (nombre: string) => {
    setHighlighted(nombre);
    const el = panelRef.current?.querySelector(`[data-nombre="${nombre}"]`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    setTimeout(() => setHighlighted(null), 2000);
  };

  const urgentes = examenes.filter(e => daysLeft(e.fecha) <= 7).length;
  const proximos = examenes.filter(e => { const d = daysLeft(e.fecha); return d > 7 && d <= 14; }).length;
  const resto    = examenes.filter(e => daysLeft(e.fecha) > 14).length;

  let prevMonth: number | null = null;

  return (
    <Layout>
      <div className="page-eyebrow">Vista completa</div>
      <h1 className="page-title">CALENDARIO<br /><span>ACADÉMICO</span></h1>
      <div className="header-line" />

      <div className="row g-4" style={{ alignItems: 'flex-start' }}>
        {/* FULLCALENDAR */}
        <div className="col-12 col-lg-8">
          <div className="section-title">Agenda</div>
          <div className="calendar-container">
            <FullCalendar
              plugins={[dayGridPlugin, listPlugin]}
              initialView="dayGridMonth"
              locale="es"
              height={660}
              headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,dayGridWeek,listMonth' }}
              buttonText={{ today: 'Hoy', month: 'Mes', week: 'Semana', list: 'Lista' }}
              events={events}
              eventContent={(arg) => {
                const curso = arg.event.extendedProps.curso ?? '';
                const pct = arg.event.extendedProps.porcentaje ?? 0;
                const days = Math.ceil((new Date(arg.event.startStr).getTime() - Date.now()) / 86400000);
                const urgent = days <= 7;
                return {
                  html: `<div style="background:${urgent ? 'linear-gradient(135deg,#8a1a10,#c03020)' : 'linear-gradient(135deg,#b85c14,#E87620)'};border-radius:3px;padding:4px 8px;overflow:hidden"><div style="font-family:'Bebas Neue',sans-serif;font-size:13px;letter-spacing:0.5px;line-height:1.1;color:#fff">${arg.event.title}</div><div style="font-size:9px;letter-spacing:1px;text-transform:uppercase;color:rgba(255,255,255,0.7);font-family:'Space Mono',monospace;margin-top:1px">${curso} · ${pct}%</div></div>`,
                };
              }}
              eventClick={(info) => highlightRow(info.event.title)}
            />
          </div>
        </div>

        {/* RIGHT PANEL */}
        <div className="col-12 col-lg-4">
          <div className="section-title">Próximos exámenes</div>
          <div className="panel" style={{ maxHeight: 700 }}>
            <div className="panel-header">
              <div className="summary-row">
                <span className="summary-chip chip-total">{examenes.length} total</span>
                {urgentes > 0 && <span className="summary-chip chip-urgent">{urgentes} urgente{urgentes > 1 ? 's' : ''}</span>}
                {proximos > 0 && <span className="summary-chip chip-soon">{proximos} en 2 sem</span>}
                {resto > 0 && <span className="summary-chip chip-ok">{resto} planificado{resto > 1 ? 's' : ''}</span>}
              </div>
            </div>
            <div className="panel-body" ref={panelRef}>
              {examenes.length === 0 ? (
                <div className="empty-panel">Sin exámenes próximos</div>
              ) : examenes.map(ex => {
                const days = daysLeft(ex.fecha);
                const monthVal = new Date(ex.fecha).getMonth();
                const showChip = prevMonth === null || prevMonth !== monthVal;
                prevMonth = monthVal;
                const monthStr = new Date(ex.fecha).toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
                const dateStr = new Date(ex.fecha).toLocaleDateString('es-PE', { day: '2-digit', month: 'short' });
                return (
                  <div key={ex.id}>
                    {showChip && <div className="month-chip">{monthStr}</div>}
                    <div
                      className="exam-row"
                      data-nombre={ex.nombre}
                      style={highlighted === ex.nombre ? { background: 'var(--black-hover)' } : undefined}
                    >
                      <div className={`exam-row-dot ${dotClass(days)}`} />
                      <div className="exam-row-body">
                        <div className="exam-row-name">{ex.nombre}</div>
                        <div className="exam-row-course">{ex.curso.nombre}</div>
                        <div className="exam-row-pct">Peso: <span>{ex.porcentaje}%</span></div>
                      </div>
                      <div className="exam-row-right">
                        <div className="exam-row-date">{dateStr}</div>
                        <div className={`exam-row-days ${daysClass(days)}`}>{days}d</div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
