package proyecto.personal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import proyecto.personal.dto.ChatMessageDto;
import proyecto.personal.dto.ChatRequest;
import proyecto.personal.ia.ChatbotService;
import proyecto.personal.ia.PdfExtractorService;
import proyecto.personal.model.Curso;
import proyecto.personal.model.Examen;
import proyecto.personal.service.CursoService;
import proyecto.personal.service.ExamenService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ia")
public class ApiIaController {

    private final ChatbotService chatbotService;
    private final PdfExtractorService pdfExtractorService;
    private final ExamenService examenService;
    private final CursoService cursoService;

    public ApiIaController(ChatbotService chatbotService, PdfExtractorService pdfExtractorService,
                           ExamenService examenService, CursoService cursoService) {
        this.chatbotService = chatbotService;
        this.pdfExtractorService = pdfExtractorService;
        this.examenService = examenService;
        this.cursoService = cursoService;
    }

    @PostMapping("/chatbot/ask")
    public Map<String, Object> preguntar(@RequestBody ChatRequest req) {
        List<Examen> examenes = examenService.listarExamenesDelUsuarioActual();
        List<Curso> cursos = cursoService.listarCursosDelUsuarioActual();
        List<ChatMessageDto> historial = req.historial() != null ? req.historial() : List.of();
        ChatbotService.ChatbotResponse response = chatbotService.responderDuda(req.mensaje(), historial, examenes, cursos);
        return Map.of("respuesta", response.respuesta(), "updated", response.updated());
    }

    @PostMapping("/silabo/importar")
    public ResponseEntity<?> importarSilabo(@RequestParam("file") MultipartFile file) {
        try {
            PdfExtractorService.ImportResult result = pdfExtractorService.extraerCursoYExamenes(file);
            String msg = "Curso \"" + result.nombreCurso() + "\" creado con " + result.examenesCreados() + " examen" + (result.examenesCreados() != 1 ? "es" : "") + ".";
            if (result.sinFecha() > 0) {
                msg += " " + result.sinFecha() + " examen" + (result.sinFecha() != 1 ? "es" : "") + " no tenían fecha clara — revisalos en la sección Exámenes.";
            }
            return ResponseEntity.ok(Map.of("message", msg, "updated", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error al procesar el sílabo: " + e.getMessage(), "updated", false));
        }
    }

    @PostMapping("/silabo/subir")
    public ResponseEntity<?> subirSilabo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("cursoId") Long cursoId) {
        try {
            if (!file.isEmpty()) {
                pdfExtractorService.extraerYGuardarExamenes(file, cursoId);
            }
            return ResponseEntity.ok(Map.of("message", "Sílabo procesado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error al procesar el sílabo"));
        }
    }
}
