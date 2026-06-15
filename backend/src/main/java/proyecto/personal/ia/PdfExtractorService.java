package proyecto.personal.ia;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Periodo;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;
import proyecto.personal.service.PeriodoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PdfExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractorService.class);

    private final GroqClient groqClient;
    private final ExamenService examenService;
    private final CursoService cursoService;
    private final PeriodoService periodoService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT_EXAMENES =
        "Eres un asistente especializado en extraer información estructurada de sílabos universitarios. " +
        "Tu tarea es analizar el texto extraído de un PDF e identificar todos los exámenes, prácticas, parciales o evaluaciones mencionadas. " +
        "Devuelve ÚNICAMENTE un arreglo JSON válido sin texto adicional (ni siquiera backticks de markdown). " +
        "El formato debe ser estrictamente: " +
        "[{ \"nombre\": \"Nombre del Examen\", \"fecha\": \"YYYY-MM-DD\", \"porcentaje\": 20.0 }] " +
        "Si no puedes encontrar la fecha exacta, trata de inferirla o usa la fecha de hoy. Si el porcentaje no está, asume un estimado o 0.0. " +
        "La fecha DEBE estar en formato YYYY-MM-DD. " +
        "Asegúrate de que la suma de porcentajes tenga sentido (no más del 100%).";

    private static final String SYSTEM_PROMPT_SILABO =
        "Eres un asistente especializado en extraer información de sílabos universitarios latinoamericanos. " +
        "Analiza el texto y extrae: el nombre del curso/materia y todos los exámenes/evaluaciones/prácticas calificadas. " +
        "Devuelve ÚNICAMENTE un objeto JSON válido sin texto adicional ni backticks de markdown. " +
        "El formato EXACTO debe ser: " +
        "{ \"curso\": { \"nombre\": \"Nombre del curso\" }, " +
        "\"examenes\": [{ \"nombre\": \"...\", \"fecha\": \"YYYY-MM-DD\", \"semana\": null, \"porcentaje\": 20.0 }] } " +
        "REGLAS PARA FECHAS (MUY IMPORTANTE — NO INVENTES NI INFERIR): " +
        "- Si el sílabo tiene una fecha exacta ESCRITA EXPLÍCITAMENTE (ej: '15/03/2026', '15 de marzo de 2026'), ponla en 'fecha' como YYYY-MM-DD y deja 'semana' en null. " +
        "- Si el sílabo indica explícitamente el número de semana junto al examen (ej: 'Semana 8', 'Week 8', 'Sem. 12'), pon ese número entero en 'semana' y deja 'fecha' en null. " +
        "- Si NO encuentras una fecha o semana EXPLÍCITAMENTE asociada a ese examen en el texto, pon AMBOS campos en null. NUNCA inventes, estimes ni calcules semanas o fechas. " +
        "REGLAS PARA PORCENTAJES: " +
        "- Extrae los porcentajes exactos del documento. " +
        "- Si no están especificados, distribúyelos uniformemente entre los exámenes (suma total = 100).";

    public record ImportResult(String nombreCurso, int examenesCreados, int sinFecha) {}

    public PdfExtractorService(GroqClient groqClient, ExamenService examenService,
                               CursoService cursoService, PeriodoService periodoService) {
        this.groqClient = groqClient;
        this.examenService = examenService;
        this.cursoService = cursoService;
        this.periodoService = periodoService;
        this.objectMapper = new ObjectMapper();
    }

    public ImportResult extraerCursoYExamenes(MultipartFile file) throws Exception {
        String textoPdf = extraerTextoPdf(file);

        java.util.Optional<proyecto.personal.model.Periodo> periodoOpt = periodoService.obtenerActivo();
        LocalDate fechaInicioPeriodo = periodoOpt.map(p -> p.getFechaInicio()).orElse(null);

        log.info("[PDF] Texto extraído ({} chars): {}", textoPdf.length(), textoPdf.substring(0, Math.min(500, textoPdf.length())));
        log.info("[PDF] fechaInicioPeriodo={}", fechaInicioPeriodo);

        String prompt = "Extrae el nombre del curso y los exámenes del siguiente sílabo:\n\n" + textoPdf;
        String respuesta = groqClient.callGroqApi(SYSTEM_PROMPT_SILABO, prompt);
        respuesta = respuesta.replaceAll("```json", "").replaceAll("```", "").trim();

        log.info("[PDF] Respuesta Groq: {}", respuesta);

        JsonNode root = objectMapper.readTree(respuesta);

        String nombreCurso = root.path("curso").path("nombre").asText("Curso importado");
        Curso curso = new Curso();
        curso.setNombre(nombreCurso);
        Curso cursoGuardado = cursoService.guardar(curso);

        JsonNode examenesNode = root.path("examenes");
        int creados = 0;
        int sinFecha = 0;
        for (JsonNode examenNode : examenesNode) {
            String nombre = examenNode.path("nombre").asText("Examen");
            String fechaStr = examenNode.path("fecha").asText(null);
            int semana = examenNode.path("semana").asInt(0);
            double porcentaje = examenNode.path("porcentaje").asDouble(0.0);

            LocalDate fecha = null;
            if (fechaStr != null && !fechaStr.equalsIgnoreCase("null") && !fechaStr.isBlank()) {
                try { fecha = LocalDate.parse(fechaStr); } catch (Exception ignored) {}
            }
            if (fecha == null && semana > 0 && fechaInicioPeriodo != null) {
                fecha = fechaInicioPeriodo.plusDays((long)(semana - 1) * 7);
            }

            if (fecha == null) {
                sinFecha++;
                fecha = LocalDate.now().plusYears(1);
            }

            try {
                examenService.guardar(nombre, fecha, null, porcentaje, cursoGuardado.getId(), "PENDIENTE");
                creados++;
            } catch (Exception e) {
                System.err.println("Error guardando examen '" + nombre + "': " + e.getMessage());
            }
        }

        return new ImportResult(nombreCurso, creados, sinFecha);
    }

    public void extraerYGuardarExamenes(MultipartFile file, Long cursoId) throws Exception {
        String textoPdf = extraerTextoPdf(file);
        String promptUsuario = "Extrae los exámenes del siguiente texto de un sílabo:\n\n" + textoPdf;
        String respuestaGrok = groqClient.callGroqApi(SYSTEM_PROMPT_EXAMENES, promptUsuario);
        respuestaGrok = respuestaGrok.replaceAll("```json", "").replaceAll("```", "").trim();

        List<Map<String, Object>> examenesExtraidos = objectMapper.readValue(respuestaGrok, new TypeReference<>() {});

        for (Map<String, Object> examenData : examenesExtraidos) {
            String nombre = (String) examenData.get("nombre");
            String fechaStr = (String) examenData.get("fecha");
            Object porcentajeObj = examenData.get("porcentaje");

            Double porcentaje = 0.0;
            if (porcentajeObj instanceof Number n) {
                porcentaje = n.doubleValue();
            } else if (porcentajeObj instanceof String s) {
                try { porcentaje = Double.parseDouble(s); } catch (Exception ignored) {}
            }

            LocalDate fecha = LocalDate.now();
            if (fechaStr != null && !fechaStr.equalsIgnoreCase("null")) {
                try { fecha = LocalDate.parse(fechaStr); } catch (Exception ignored) {}
            }

            try {
                examenService.guardar(nombre, fecha, null, porcentaje, cursoId, "PENDIENTE");
            } catch (Exception e) {
                System.err.println("Error guardando examen '" + nombre + "': " + e.getMessage());
            }
        }
    }

    private String extraerTextoPdf(MultipartFile file) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}
