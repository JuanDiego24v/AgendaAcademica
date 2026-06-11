package proyecto.personal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import proyecto.personal.ia.ChatbotService;
import proyecto.personal.ia.PdfExtractorService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ia")
public class IaController {

    private final ChatbotService chatbotService;
    private final PdfExtractorService pdfExtractorService;

    public IaController(ChatbotService chatbotService, PdfExtractorService pdfExtractorService) {
        this.chatbotService = chatbotService;
        this.pdfExtractorService = pdfExtractorService;
    }

    @PostMapping("/chatbot/ask")
    @ResponseBody
    public Map<String, String> preguntarChatbot(@RequestParam("mensaje") String mensaje) {
        String respuesta = chatbotService.responderDuda(mensaje, List.of(), List.of(), List.of()).respuesta();
        return Map.of("respuesta", respuesta);
    }

    @PostMapping("/silabo/subir")
    public String subirSilabo(@RequestParam("file") MultipartFile file, 
                              @RequestParam("cursoId") Long cursoId) {
        try {
            if (!file.isEmpty()) {
                pdfExtractorService.extraerYGuardarExamenes(file, cursoId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Podrías añadir un RedirectAttributes para mostrar mensaje de error en la vista
        }
        return "redirect:/examenes?cursoId=" + cursoId;
    }
}
