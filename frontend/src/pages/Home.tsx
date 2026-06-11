import { useEffect, useState, useRef } from 'react';
import Layout from '../components/Layout';
import client from '../api/client';
import type { Examen } from '../types';

function daysLeft(fecha: string): number {
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const [y, m, day] = fecha.split('-').map(Number);
  const d = new Date(y, m - 1, day);
  return Math.round((d.getTime() - today.getTime()) / 86400000);
}

function badgeClass(days: number) {
  if (days < 7) return 'badge-pill badge-urgent';
  if (days < 14) return 'badge-pill badge-soon';
  if (days < 30) return 'badge-pill badge-ok';
  return 'badge-pill badge-done';
}
function badgeLabel(days: number) {
  if (days < 7) return 'Urgente';
  if (days < 14) return 'Próximo';
  if (days < 30) return 'Listo';
  return 'Planificado';
}

function MiniCalendar({ examenes }: { examenes: Examen[] }) {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth();
  const today = now.getDate();
  const monthNames = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
  const examDays = new Set(
    examenes.map(e => { const d = new Date(e.fecha); return d.getFullYear() === year && d.getMonth() === month ? d.getDate() : -1; }).filter(d => d > 0)
  );
  const firstDay = new Date(year, month, 1).getDay();
  const offset = firstDay === 0 ? 6 : firstDay - 1;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells: (number | null)[] = [...Array(offset).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)];

  return (
    <div className="calendar-card" style={{ opacity: 0, animation: 'fadeUp 0.5s ease 0.6s both' }}>
      <div className="cal-header">{monthNames[month]} {year}</div>
      <div className="cal-grid">
        {['L','M','X','J','V','S','D'].map(d => <div key={d} className="cal-day-name">{d}</div>)}
        {cells.map((d, i) =>
          d === null
            ? <div key={i} />
            : <div key={i} className={`cal-day${d === today ? ' today' : examDays.has(d) ? ' has-exam' : ''}`}>{d}</div>
        )}
      </div>
    </div>
  );
}

export default function Home() {
  const [examenes, setExamenes] = useState<Examen[]>([]);
  const progRefs = useRef<(HTMLDivElement | null)[]>([]);

  useEffect(() => {
    client.get<Examen[]>('/api/home/proximos').then(r => setExamenes(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    const t = setTimeout(() => {
      progRefs.current.forEach(el => {
        if (el) { const w = el.dataset.width ?? '0'; el.style.width = '0'; requestAnimationFrame(() => setTimeout(() => { el.style.width = w; }, 50)); }
      });
    }, 600);
    return () => clearTimeout(t);
  }, [examenes]);

  const total = examenes.length;
  const enSemana = examenes.filter(e => { const d = daysLeft(e.fecha); return d >= 0 && d <= 7; }).length;
  const urgentes = enSemana;
  const siguiente = examenes[0] ?? null;
  const siguienteDays = siguiente ? daysLeft(siguiente.fecha) : null;

  const tickerItems = [
    ...examenes.map(e => `<span class="ticker-item"><span>▶</span> PRÓXIMO: ${e.nombre} — ${e.fecha}</span>`),
    '<span class="ticker-item"><span>▶</span> AGENDA ACADÉMICA</span>',
    ...(urgentes > 0 ? [`<span class="ticker-item"><span>▶</span> ${urgentes} EXAMEN${urgentes > 1 ? 'ES' : ''} URGENTE${urgentes > 1 ? 'S' : ''}</span>`] : []),
  ];
  const tickerContent = [...tickerItems, ...tickerItems].join('');

  const progTotalPct = total > 0 ? Math.min(100, (total / 10) * 100) : 0;
  const progDiasPct = siguienteDays != null ? Math.max(5, Math.min(100, 100 - (siguienteDays / 30 * 100))) : 0;

  return (
    <Layout>
      <div className="page-eyebrow">Ciclo 2026 — I</div>
      <h1 className="page-title">AGENDA<br /><span>ACADÉMICA</span></h1>

      <div className="ticker-wrap mt-4">
        <div className="ticker-inner" dangerouslySetInnerHTML={{ __html: tickerContent }} />
      </div>

      <div className="header-line" />

      {/* STAT CARDS */}
      <div className="row g-3 mb-5">
        {[
          {
            id: 'total', delay: 'd1',
            number: String(total).padStart(2, '0'),
            label: 'Próximos exámenes',
            sub: total === 0 ? 'ninguno próximo' : `${total} examen${total > 1 ? 'es' : ''} próximo${total > 1 ? 's' : ''}`,
            prog: progTotalPct,
          },
          {
            id: 'semana', delay: 'd2',
            number: String(enSemana).padStart(2, '0'),
            label: 'Próximos 7 días',
            sub: enSemana > 0 ? '⚡ Alta carga' : '✓ Tranquilo',
            subColor: enSemana > 0 ? 'var(--orange)' : '#6dba7d',
          },
          {
            id: 'dias', delay: 'd3',
            number: siguienteDays != null ? String(siguienteDays).padStart(2, '0') : '—',
            label: 'Días al próximo',
            sub: siguiente ? siguiente.nombre : 'sin exámenes',
            prog: progDiasPct,
          },
          {
            id: 'urgentes', delay: 'd4',
            number: String(urgentes).padStart(2, '0'),
            label: 'Urgentes',
            sub: urgentes === 0 ? 'ninguno urgente' : `${urgentes} urgente${urgentes > 1 ? 's' : ''}`,
            subColor: urgentes === 0 ? '#6dba7d' : '#e05555',
          },
        ].map((c, i) => (
          <div key={c.id} className="col-6 col-md-3">
            <div className={`stat-card ${c.delay}`}>
              <div className="stat-number">{c.number}</div>
              <div className="stat-label">{c.label}</div>
              {'prog' in c && (
                <div className="prog-wrap mt-3">
                  <div
                    className="prog-bar"
                    ref={el => { progRefs.current[i] = el; }}
                    data-width={`${c.prog}%`}
                    style={{ width: 0 }}
                  />
                </div>
              )}
              <div className="stat-sub" style={c.subColor ? { color: c.subColor, marginTop: 10 } : undefined}>{c.sub}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="row g-4">
        {/* EXAM LIST */}
        <div className="col-12 col-lg-8">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <div className="section-title" style={{ flex: 1 }}>Exámenes programados</div>
            <a href="/examenes" className="btn-add ms-3">+ Agregar</a>
          </div>

          {examenes.length === 0 ? (
            <div className="exam-card d5" style={{ justifyContent: 'center', color: 'var(--muted)', fontFamily: "'Space Mono', monospace", fontSize: 12 }}>
              No hay exámenes próximos — ¡vas bien!
            </div>
          ) : examenes.map((ex, idx) => {
            const days = daysLeft(ex.fecha);
            return (
              <div key={ex.id} className={`exam-card d${idx + 5}`}>
                <div style={{ flex: 1 }}>
                  <div className="exam-subject">{ex.nombre}</div>
                  <div className="exam-days">{ex.curso.nombre}</div>
                </div>
                <div className="text-end">
                  <div className="exam-date">{new Date(ex.fecha).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }).toUpperCase()}</div>
                  <div className="exam-days mt-1">En {days} días</div>
                </div>
                <span className={badgeClass(days)}>{badgeLabel(days)}</span>
              </div>
            );
          })}
        </div>

        {/* RIGHT PANEL */}
        <div className="col-12 col-lg-4">
          <MiniCalendar examenes={examenes} />
          <div style={{ height: 12 }} />

          {examenes.length > 0 ? (
            <div className="calendar-card" style={{ opacity: 0, animation: 'fadeUp 0.5s ease 0.7s both', padding: 20 }}>
              <div className="section-title">Más próximos</div>
              {examenes.slice(0, 3).map(ex => {
                const days = daysLeft(ex.fecha);
                const color = days < 7 ? 'linear-gradient(90deg,#a03010,#e05030)' : days < 14 ? 'linear-gradient(90deg,var(--orange-dim),var(--orange))' : 'linear-gradient(90deg,#2a7a3a,#6dba7d)';
                const textColor = days < 7 ? '#e05555' : days < 14 ? 'var(--orange)' : '#6dba7d';
                const pct = days < 7 ? 85 : days < 14 ? 55 : 25;
                return (
                  <div key={ex.id} style={{ marginBottom: 14 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 5 }}>
                      <span style={{ fontFamily: "'Space Mono',monospace", fontSize: 11 }}>{ex.nombre}</span>
                      <span style={{ fontFamily: "'Space Mono',monospace", fontSize: 11, color: textColor }}>{days}d</span>
                    </div>
                    <div className="prog-wrap">
                      <div className="prog-bar" style={{ width: `${pct}%`, background: color }} />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="calendar-card" style={{ opacity: 0, animation: 'fadeUp 0.5s ease 0.7s both', padding: 20 }}>
              <div className="section-title">Estado</div>
              <div style={{ fontFamily: "'Space Mono',monospace", fontSize: 11, color: '#6dba7d', letterSpacing: 1 }}>Sin exámenes próximos</div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
