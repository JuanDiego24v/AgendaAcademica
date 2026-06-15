package proyecto.personal.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import proyecto.personal.dto.ChatMessageDto;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Examen;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private final GroqClient groqClient;
    private final ExamenService examenService;
    private final CursoService cursoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ChatbotResponse(String respuesta, boolean updated) {}

    private static final String SYSTEM_PROMPT =
        "Eres un asistente virtual de la aplicación 'Agenda Académica'. Tu propósito es explicar cómo usar la app y gestionar exámenes y cursos cuando el usuario lo pida.\n\n" +

        "SECCIONES QUE EXISTEN (exactamente estas cuatro):\n\n" +

        "1. HOME: Resumen del ciclo académico. 4 tarjetas estadísticas (total de exámenes próximos, exámenes en los próximos 7 días, días al próximo examen, urgentes), mini calendario y sección 'Más próximos' con los 3 exámenes más cercanos.\n\n" +

        "2. CURSOS: Lista de cursos. Se pueden agregar, editar y eliminar. Cada curso solo tiene nombre.\n\n" +

        "3. EXÁMENES: Sección principal. Contiene sistema de evaluación (nota máxima y mínima aprobatoria), promedio real del curso, tabla de exámenes (nombre, fecha, estado, nota, porcentaje), botón para añadir examen o subir PDF de sílabo, botones editar/eliminar por examen, y simulación de notas con sliders.\n\n" +

        "4. CALENDARIO: Vista de calendario con los exámenes marcados por fecha.\n\n" +

        "ACCIONES QUE PUEDES REALIZAR (usa las funciones disponibles):\n" +
        "- Crear, editar o eliminar exámenes\n" +
        "- Registrar o cambiar la nota de un examen (al hacerlo queda marcado como Revisado)\n" +
        "- Crear, editar o eliminar cursos\n\n" +

        "IMPORTANTE al editar exámenes: si el usuario solo pide cambiar un campo (ej: la nota), usa los valores existentes del examen para los demás campos.\n\n" +

        "ESTADOS DE EXÁMENES:\n" +
        "Solo existen dos estados posibles. El usuario los ve con estos nombres en la app:\n" +
        "- 'Falta revisar' → internamente es PENDIENTE. El examen todavía no fue rendido o no tiene nota registrada.\n" +
        "- 'Examen revisado' → internamente es COMPLETADO. El examen fue rendido y tiene una nota asignada.\n" +
        "Cuando el usuario diga 'ponle falta revisar', 'marcalo como falta revisar' o similar → usá estado=PENDIENTE.\n" +
        "Cuando el usuario diga 'ponle examen revisado', 'marcalo como revisado' o similar → usá estado=COMPLETADO.\n" +
        "NUNCA asignes nota=0 a menos que el usuario haya dicho explícitamente que la nota es 0. Si el usuario solo pide cambiar el estado, no toques la nota.\n\n" +

        "LO QUE NO EXISTE (nunca lo menciones):\n" +
        "- No hay sección de notas separada, reportes, estadísticas avanzadas, notificaciones, alertas por email, perfil editable, tareas, trabajos ni asistencia.\n\n" +

        "REGLA ABSOLUTA: NUNCA inventes funcionalidades ni secciones fuera de la lista. Si no sabes si algo existe, di que no está disponible.\n\n" +
        "REGLA DE PRESENTACIÓN: NUNCA muestres IDs (de exámenes ni de cursos) al usuario en tus respuestas. Los IDs son internos y solo los usas para llamar a las funciones.\n\n" +

        "FLUJO MANDATORIO PARA TODA ACCIÓN DE MODIFICACIÓN:\n" +
        "Cuando el usuario pida crear, editar o eliminar un examen o curso, seguí SIEMPRE estos tres pasos en orden. No podés saltear ninguno.\n\n" +

        "PASO A — RESOLVER AMBIGÜEDAD\n" +
        "Buscá en la lista de CURSOS Y EXÁMENES DEL USUARIO los que coincidan con el nombre mencionado (parcial o completo, sin importar mayúsculas).\n" +
        "• Si coincide MÁS DE UNO: DETENTE. No llames ninguna función. Listá todas las coincidencias (nombre + curso + fecha) y preguntá cuál es el que el usuario quiere modificar. Esperá su respuesta antes de continuar.\n" +
        "• Si coincide exactamente uno: identificalo internamente y avanzá al PASO B.\n" +
        "NUNCA le pidas al usuario que te dé el ID. Los IDs son internos.\n\n" +

        "PASO B — PEDIR CONFIRMACIÓN\n" +
        "DETENTE. No llames ninguna función todavía. Mostrá un resumen claro de lo que vas a hacer y preguntá si el usuario confirma.\n" +
        "Formato del resumen según la acción:\n" +
        "• Editar examen: nombre del examen, curso, y qué campo(s) cambian (valor anterior → valor nuevo).\n" +
        "• Crear examen: nombre, curso, fecha, porcentaje y nota (si aplica).\n" +
        "• Eliminar examen: nombre, curso, fecha y porcentaje.\n" +
        "• Crear curso: solo el nombre. NUNCA menciones eliminación de exámenes al confirmar creación.\n" +
        "• Editar curso: nombre anterior → nombre nuevo.\n" +
        "• Eliminar curso: nombre y advertencia de que se eliminarán todos sus exámenes.\n" +
        "Para operaciones en lote, mostrá la lista completa de cambios de una sola vez.\n\n" +

        "PASO C — EJECUTAR\n" +
        "Solo llegás acá si el mensaje ACTUAL del usuario es una confirmación explícita ('sí', 'confirmo', 'dale', 'adelante', etc.) Y tu mensaje ANTERIOR en el historial fue el resumen del PASO B para esa acción específica.\n" +
        "Recién entonces llamá la función correspondiente.\n" +
        "ANTI-LOOP: Si el usuario ya confirmó, ejecutá de una vez. NUNCA vuelvas a mostrar el resumen de PASO B ni a pedir confirmación de nuevo.\n\n" +

        "PROHIBICIÓN ABSOLUTA: Nunca llames una función de modificación (crear/editar/eliminar) en el mismo turno en que identificás el examen, pedís datos o mostrás el resumen. SIEMPRE hay que esperar confirmación explícita en el turno siguiente.\n\n" +

        "Si el usuario pregunta algo ajeno a esta app, niégate educadamente.\n\n" +

        "CÁLCULO DE FECHAS POR DÍA DE SEMANA — REGLA CRÍTICA:\n" +
        "Cuando el usuario pida mover exámenes a un día de la semana (ej: 'pasalos al martes', 'cambialos al lunes más próximo'):\n" +
        "PASO 1 — Llamá a `calcular_fecha_dia_semana` para CADA examen afectado (una llamada por examen). JAMÁS calcules fechas de cabeza: los LLMs cometen errores graves de aritmética de fechas.\n" +
        "PASO 2 — Con los resultados del tool, mostrá el resumen en formato 'NOMBRE: FECHA_ORIGINAL → FECHA_NUEVA' y pedí confirmación.\n" +
        "PASO 3 — Cuando el usuario confirme: buscá en tu mensaje ANTERIOR del historial los valores FECHA_NUEVA (los que están después de '→'). Usá esas fechas EXACTAS para llamar `editar_examen`. PROHIBIDO volver a llamar `calcular_fecha_dia_semana`. PROHIBIDO hacer cualquier aritmética de fechas. Las fechas ya están calculadas en el resumen.\n" +
        "SI UN `editar_examen` FALLA: reportá el error directamente. No repitas el resumen ni pidas confirmación de nuevo.";

    public ChatbotService(GroqClient groqClient, ExamenService examenService, CursoService cursoService) {
        this.groqClient = groqClient;
        this.examenService = examenService;
        this.cursoService = cursoService;
    }

    public ChatbotResponse responderDuda(String mensaje, List<ChatMessageDto> historial, List<Examen> examenes, List<Curso> cursos) {
        String systemPrompt = buildSystemPrompt(examenes, cursos);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        int start = Math.max(0, historial.size() - 6);
        for (int i = start; i < historial.size(); i++) {
            ChatMessageDto msg = historial.get(i);
            String role = "bot".equals(msg.sender()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", msg.text()));
        }

        messages.add(Map.of("role", "user", "content", mensaje));

        try {
            List<Map<String, Object>> tools = buildTools();
            GroqClient.GroqResponse response = groqClient.callWithTools(messages, tools);
            boolean updated = false;
            int iterations = 0;

            while (response.isToolCall() && iterations < 20) {
                iterations++;

                List<Map<String, Object>> toolCallsList = new ArrayList<>();
                List<Map<String, Object>> toolResultMsgs = new ArrayList<>();

                for (GroqClient.ToolCall tc : response.toolCalls()) {
                    JsonNode args = objectMapper.readTree(tc.toolArguments());
                    String toolResult;
                    try {
                        toolResult = ejecutarTool(tc.toolName(), args);
                        if (isDataModifyingTool(tc.toolName())) updated = true;
                    } catch (Exception e) {
                        toolResult = "Error: " + e.getMessage();
                    }

                    toolCallsList.add(Map.of(
                        "id", tc.toolCallId(),
                        "type", "function",
                        "function", Map.of("name", tc.toolName(), "arguments", tc.toolArguments())
                    ));

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", tc.toolCallId());
                    toolMsg.put("content", toolResult);
                    toolResultMsgs.add(toolMsg);
                }

                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", null);
                assistantMsg.put("tool_calls", toolCallsList);
                messages.add(assistantMsg);
                messages.addAll(toolResultMsgs);

                response = groqClient.callWithTools(messages, tools);
            }

            return new ChatbotResponse(response.text(), updated);

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null && e.getMessage().contains("429")
                ? "Estoy recibiendo demasiadas consultas en este momento. Espera unos segundos e intenta de nuevo."
                : "Ocurrió un error al procesar tu consulta. Intenta de nuevo en un momento.";
            return new ChatbotResponse(msg, false);
        }
    }

    private String ejecutarTool(String toolName, JsonNode args) throws Exception {
        return switch (toolName) {
            case "crear_examen" -> {
                String nombre = args.path("nombre").asText();
                String fecha = args.path("fecha").asText();
                double porcentaje = args.path("porcentaje").asDouble();
                long cursoId = args.path("curso_id").asLong();
                String estado = args.has("estado") ? args.path("estado").asText() : "PENDIENTE";
                Double nota = args.has("nota") && !args.path("nota").isNull() ? args.path("nota").asDouble() : null;
                examenService.guardar(nombre, LocalDate.parse(fecha), nota, porcentaje, cursoId, estado);
                yield "Examen '" + nombre + "' creado correctamente.";
            }
            case "editar_examen" -> {
                long id = args.path("examen_id").asLong();
                Examen existente = examenService.buscarExamenDelUsuarioActual(id);
                String nombre = args.has("nombre") ? args.path("nombre").asText() : existente.getNombre();
                String fecha = args.has("fecha") ? args.path("fecha").asText() : existente.getFecha().toString();
                double porcentaje = args.has("porcentaje") ? args.path("porcentaje").asDouble() : existente.getPorcentaje();
                String estado = args.has("estado") ? args.path("estado").asText() : existente.getEstado();
                boolean notaExplicita = args.has("nota") && !args.path("nota").isNull();
                Double nota = notaExplicita ? args.path("nota").asDouble() : existente.getNota();
                if (notaExplicita && !args.has("estado")) estado = "COMPLETADO";
                examenService.editarDatosExamen(id, nombre, LocalDate.parse(fecha), porcentaje, nota, estado);
                yield "Examen '" + nombre + "' actualizado correctamente.";
            }
            case "eliminar_examen" -> {
                long id = args.path("examen_id").asLong();
                Examen ex = examenService.buscarExamenDelUsuarioActual(id);
                examenService.eliminar(id);
                yield "Examen '" + ex.getNombre() + "' eliminado.";
            }
            case "crear_curso" -> {
                String nombre = args.path("nombre").asText();
                Curso nuevo = new Curso();
                nuevo.setNombre(nombre);
                cursoService.guardar(nuevo);
                yield "Curso '" + nombre + "' creado correctamente.";
            }
            case "editar_curso" -> {
                long id = args.path("curso_id").asLong();
                String nombre = args.path("nombre").asText();
                cursoService.actualizar(id, nombre);
                yield "Curso actualizado a '" + nombre + "'.";
            }
            case "eliminar_curso" -> {
                long id = args.path("curso_id").asLong();
                Curso curso = cursoService.buscarCursoDelUsuarioActual(id);
                cursoService.eliminar(id);
                yield "Curso '" + curso.getNombre() + "' eliminado.";
            }
            case "calcular_fecha_dia_semana" -> {
                String fechaStr = args.path("fecha").asText();
                String diaSemana = args.path("dia_semana").asText().toUpperCase()
                        .replace("É", "E").replace("Á", "A");
                LocalDate fecha = LocalDate.parse(fechaStr);
                DayOfWeek target = switch (diaSemana) {
                    case "LUNES"     -> DayOfWeek.MONDAY;
                    case "MARTES"    -> DayOfWeek.TUESDAY;
                    case "MIERCOLES" -> DayOfWeek.WEDNESDAY;
                    case "JUEVES"    -> DayOfWeek.THURSDAY;
                    case "VIERNES"   -> DayOfWeek.FRIDAY;
                    case "SABADO"    -> DayOfWeek.SATURDAY;
                    case "DOMINGO"   -> DayOfWeek.SUNDAY;
                    default -> throw new IllegalArgumentException("Día no reconocido: " + diaSemana);
                };
                LocalDate next = fecha.with(TemporalAdjusters.nextOrSame(target));
                LocalDate prev = fecha.with(TemporalAdjusters.previousOrSame(target));
                long dNext = ChronoUnit.DAYS.between(fecha, next);
                long dPrev = ChronoUnit.DAYS.between(prev, fecha);
                LocalDate result = dNext <= dPrev ? next : prev;
                yield "El " + diaSemana.toLowerCase() + " más próximo a " + fechaStr + " es: " + result;
            }
            default -> throw new IllegalArgumentException("Función desconocida: " + toolName);
        };
    }

    private boolean isDataModifyingTool(String toolName) {
        return switch (toolName) {
            case "crear_examen", "editar_examen", "eliminar_examen",
                 "crear_curso", "editar_curso", "eliminar_curso" -> true;
            default -> false;
        };
    }

    private String buildSystemPrompt(List<Examen> examenes, List<Curso> cursos) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        if (!cursos.isEmpty()) {
            sb.append("\n\nCURSOS DEL USUARIO:\n");
            for (Curso c : cursos) {
                sb.append(String.format("- ID: %d | Nombre: %s%n", c.getId(), c.getNombre()));
            }
        }

        if (!examenes.isEmpty()) {
            sb.append("\nEXÁMENES DEL USUARIO:\n");
            for (Examen e : examenes) {
                sb.append(String.format("- ID: %d | Nombre: %s | Curso: %s (ID: %d) | Fecha: %s | Estado: %s | Nota: %s | Porcentaje: %.1f%%%n",
                    e.getId(), e.getNombre(), e.getCurso().getNombre(), e.getCurso().getId(),
                    e.getFecha(), e.getEstado(),
                    e.getNota() != null ? e.getNota() : "sin nota",
                    e.getPorcentaje()));
            }
        }

        return sb.toString();
    }

    private List<Map<String, Object>> buildTools() {
        return List.of(
            tool("crear_examen", "Crea un nuevo examen para un curso.",
                Map.of(
                    "nombre", prop("string", "Nombre del examen"),
                    "fecha", prop("string", "Fecha en formato YYYY-MM-DD"),
                    "porcentaje", prop("number", "Porcentaje que vale sobre el total del curso"),
                    "curso_id", prop("integer", "ID del curso al que pertenece"),
                    "nota", prop("number", "Nota obtenida (opcional, solo si ya fue rendido)"),
                    "estado", prop("string", "PENDIENTE o COMPLETADO (por defecto PENDIENTE)")
                ),
                List.of("nombre", "fecha", "porcentaje", "curso_id")
            ),
            tool("editar_examen", "Edita uno o más campos de un examen existente. Usa los valores actuales para los campos que no cambian. Si se provee nota, el estado se actualiza a COMPLETADO automáticamente.",
                Map.of(
                    "examen_id", prop("integer", "ID del examen a editar"),
                    "nombre", prop("string", "Nuevo nombre"),
                    "fecha", prop("string", "Nueva fecha en formato YYYY-MM-DD"),
                    "porcentaje", prop("number", "Nuevo porcentaje"),
                    "nota", prop("number", "Nueva nota"),
                    "estado", prop("string", "PENDIENTE o COMPLETADO")
                ),
                List.of("examen_id")
            ),
            tool("eliminar_examen", "Elimina un examen.",
                Map.of("examen_id", prop("integer", "ID del examen a eliminar")),
                List.of("examen_id")
            ),
            tool("crear_curso", "Crea un nuevo curso.",
                Map.of("nombre", prop("string", "Nombre del curso")),
                List.of("nombre")
            ),
            tool("editar_curso", "Cambia el nombre de un curso existente.",
                Map.of(
                    "curso_id", prop("integer", "ID del curso a editar"),
                    "nombre", prop("string", "Nuevo nombre del curso")
                ),
                List.of("curso_id", "nombre")
            ),
            tool("eliminar_curso", "Elimina un curso y todos sus exámenes.",
                Map.of("curso_id", prop("integer", "ID del curso a eliminar")),
                List.of("curso_id")
            ),
            tool("calcular_fecha_dia_semana",
                "Calcula el día de la semana más próximo (antes o después) a una fecha dada. " +
                "Úsala SIEMPRE antes de editar un examen cuando el usuario pida mover fechas a un día de la semana específico.",
                Map.of(
                    "fecha", prop("string", "Fecha base en formato YYYY-MM-DD"),
                    "dia_semana", prop("string", "Día en español en mayúsculas: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO")
                ),
                List.of("fecha", "dia_semana")
            )
        );
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        return Map.of("type", "function", "function", function);
    }

    private Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
