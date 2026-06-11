export interface Curso {
  id: number;
  nombre: string;
}

export interface Examen {
  id: number;
  nombre: string;
  fecha: string;
  porcentaje: number;
  estado: 'PENDIENTE' | 'COMPLETADO';
  nota: number | null;
  curso: Curso;
}

export interface DashboardData {
  proximosExamenes: Examen[];
}

export interface ExamenesPageData {
  examenes: Examen[];
  cursos: Curso[];
  cursoSeleccionado: number;
  notaMaxima: number;
  notaMinimaAprobatoria: number;
  promedioCurso: number;
  porcentajeEvaluado: number;
  porcentajePendiente: number;
}

export interface CalendarioData {
  todosExamenes: Examen[];
}

export interface CalendarEvent {
  id: string;
  title: string;
  start: string;
  extendedProps: {
    curso: string;
    porcentaje: number;
  };
}

export interface AuthResponse {
  token: string;
  username: string;
}
