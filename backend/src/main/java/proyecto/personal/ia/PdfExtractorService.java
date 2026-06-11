package proyecto.personal.ia;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import proyecto.personal.service.ExamenService;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PdfExtractorService {

    private final GroqClient groqClient;
    private final ExamenService examenService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = 
        "Eres un asistente especializado en extraer información estructurada de sílabos universitarios. " +
        "Tu tarea es analizar el texto extraído de un PDF e identificar todos los exámenes, prácticas, parciales o evaluaciones mencionadas. " +
        "Devuelve ÚNICAMENTE un arreglo JSON válido sin texto adicional (ni siquiera backticks de markdown). " +
        "El formato debe ser estrictamente: " +
        "[" +
        "  { \"nombre\": \"Nombre del Examen\", \"fecha\": \"YYYY-MM-DD\", \"porcentaje\": 20.0 }" +
        "] " +
        "Si no puedes encontrar la fecha exacta, trata de inferirla o usa 'null'. Si el porcentaje no está, asume un estimado o 0.0. " +
        "La fecha DEBE estar en formato 'YYYY-MM-DD'. " +
        "Asegúrate de que la suma de porcentajes tenga sentido (no más del 100%).";

    public PdfExtractorService(GroqClient groqClient, ExamenService examenService) {
        this.groqClient = groqClient;
        this.examenService = examenService;
        this.objectMapper = new ObjectMapper();
    }

    public void extraerYGuardarExamenes(MultipartFile file, Long cursoId) throws Exception {
        
        String textoPdf = extraerTextoPdf(file);
        
        String promptUsuario = "Extrae los exámenes del siguiente texto de un sílabo:\n\n" + textoPdf;
        String respuestaGrok = groqClient.callGroqApi(SYSTEM_PROMPT, promptUsuario);
        
        // Limpiar posible markdown o texto basura de la respuesta JSON
        respuestaGrok = respuestaGrok.replaceAll("```json", "").replaceAll("```", "").trim();

        // Parsear el JSON
        List<Map<String, Object>> examenesExtraidos = objectMapper.readValue(respuestaGrok, new TypeReference<List<Map<String, Object>>>() {});

        for (Map<String, Object> examenData : examenesExtraidos) {
            String nombre = (String) examenData.get("nombre");
            String fechaStr = (String) examenData.get("fecha");
            Object porcentajeObj = examenData.get("porcentaje");
            
            Double porcentaje = 0.0;
            if (porcentajeObj instanceof Number) {
                porcentaje = ((Number) porcentajeObj).doubleValue();
            } else if (porcentajeObj instanceof String) {
                try {
                    porcentaje = Double.parseDouble((String) porcentajeObj);
                } catch (Exception e) {
                    porcentaje = 0.0;
                }
            }

            LocalDate fecha = LocalDate.now(); // default
            if (fechaStr != null && !fechaStr.equalsIgnoreCase("null")) {
                try {
                    fecha = LocalDate.parse(fechaStr);
                } catch (Exception e) {
                    // ignorar
                }
            }

            // Guardar como PENDIENTE — el alumno carga la nota cuando rinde
            try {
                examenService.guardar(nombre, fecha, null, porcentaje, cursoId, "PENDIENTE");
            } catch (Exception e) {
                // Si algo falla al guardar un examen, lo ignoramos y seguimos con el siguiente (por ejemplo si excede 100%)
                System.err.println("Error guardando examen " + nombre + ": " + e.getMessage());
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
