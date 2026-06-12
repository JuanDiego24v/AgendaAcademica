package proyecto.personal.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import proyecto.personal.dto.ChatMessageDto;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Examen;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;

import java.time.LocalDate;
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

        "LO QUE NO EXISTE (nunca lo menciones):\n" +
        "- No hay sección de notas separada, reportes, estadísticas avanzadas, notificaciones, alertas por email, perfil editable, tareas, trabajos ni asistencia.\n\n" +

        "REGLA ABSOLUTA: NUNCA inventes funcionalidades ni secciones fuera de la lista. Si no sabes si algo existe, di que no está disponible.\n\n" +
        "REGLA DE PRESENTACIÓN: NUNCA muestres IDs (de exámenes ni de cursos) al usuario en tus respuestas. Los IDs son internos y solo los usas para llamar a las funciones.\n\n" +

        "CONFIRMACIÓN OBLIGATORIA ANTES DE CUALQUIER ACCIÓN:\n" +
        "Antes de llamar a CUALQUIER función (crear, editar o eliminar exámenes o cursos), SIEMPRE debes pedir confirmación en el turno anterior. Nunca ejecutes una acción directamente.\n" +
        "Muestra siempre un resumen claro de lo que vas a hacer:\n" +
        "- Crear examen: nombre, curso, fecha, porcentaje y nota (si aplica).\n" +
        "- Editar examen: nombre del examen, curso, y qué campo(s) cambian (valor anterior → valor nuevo).\n" +
        "- Eliminar examen: nombre, curso, fecha y porcentaje.\n" +
        "- Crear curso: solo el nombre. NUNCA menciones eliminación de exámenes al confirmar creación de un curso. Crear un curso es siempre una adición nueva y no afecta nada existente.\n" +
        "- Editar curso: nombre anterior → nombre nuevo.\n" +
        "- Eliminar curso: nombre y advertencia de que se eliminarán todos sus exámenes.\n" +
        "Solo ejecuta la acción si el mensaje actual del usuario confirma explícitamente (dice 'sí', 'confirmo', 'adelante', etc.) Y tu mensaje anterior en el historial fue una pregunta de confirmación para esa acción específica.\n" +
        "Si hay ambigüedad (varios exámenes o cursos con nombres similares), listalos con su curso y pide al usuario que especifique cuál.\n\n" +

        "Si el usuario pregunta algo ajeno a esta app, niégate educadamente.";

    public ChatbotService(GroqClient groqClient, ExamenService examenService, CursoService cursoService) {
        this.groqClient = groqClient;
        this.examenService = examenService;
        this.cursoService = cursoService;
    }

    public ChatbotResponse responderDuda(String mensaje, List<ChatMessageDto> historial, List<Examen> examenes, List<Curso> cursos) {
        String systemPrompt = buildSystemPrompt(examenes, cursos);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        int start = Math.max(0, historial.size() - 10);
        for (int i = start; i < historial.size(); i++) {
            ChatMessageDto msg = historial.get(i);
            String role = "bot".equals(msg.sender()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", msg.text()));
        }

        messages.add(Map.of("role", "user", "content", mensaje));

        try {
            GroqClient.GroqResponse response = groqClient.callWithTools(messages, buildTools());

            if (response.isToolCall()) {
                JsonNode args = objectMapper.readTree(response.toolArguments());
                String toolResult;
                boolean success = false;

                try {
                    toolResult = ejecutarTool(response.toolName(), args);
                    success = true;
                } catch (Exception e) {
                    toolResult = "Error: " + e.getMessage();
                }

                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("tool_calls", List.of(Map.of(
                    "id", response.toolCallId(),
                    "type", "function",
                    "function", Map.of("name", response.toolName(), "arguments", response.toolArguments())
                )));
                messages.add(assistantMsg);

                String finalText = groqClient.sendToolResult(messages, response.toolCallId(), toolResult);
                return new ChatbotResponse(finalText, success);
            }

            return new ChatbotResponse(response.text(), false);

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null && e.getMessage().contains("429")
                ? "Estoy recibiendo demasiadas consultas en este momento. Esperá unos segundos e intentá de nuevo."
                : "Ocurrió un error al procesar tu consulta. Intentá de nuevo en un momento.";
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
                Double nota = args.has("nota") && !args.path("nota").isNull() ? args.path("nota").asDouble() : existente.getNota();
                if (args.has("nota") && !args.path("nota").isNull()) estado = "COMPLETADO";
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
            default -> throw new IllegalArgumentException("Función desconocida: " + toolName);
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
            tool("editar_examen", "Edita uno o más campos de un examen existente. Usá los valores actuales para los campos que no cambian. Si se provee nota, el estado se actualiza a COMPLETADO automáticamente.",
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
