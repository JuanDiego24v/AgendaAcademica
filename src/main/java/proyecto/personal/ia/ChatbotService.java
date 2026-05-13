package proyecto.personal.ia;

import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private final GrokClient grokClient;

    private static final String SYSTEM_PROMPT = 
        "Eres un asistente virtual para una aplicación web de Agenda Académica llamada 'Agenda Académica'. " +
        "Tu propósito es ayudar a los estudiantes con la gestión de sus exámenes, recordarles cómo funciona la aplicación, " +
        "cómo registrar notas, entender las proyecciones para aprobar y funcionalidades de la web.\n\n" +
        "REGLA MUY IMPORTANTE: Solo puedes resolver dudas sobre el entorno web y la gestión académica dentro de la app. " +
        "Si el estudiante te pregunta algo ajeno a esto (por ejemplo, resolver un problema de matemáticas, programación, historia, etc.), " +
        "DEBES negarte educadamente y advertir al usuario que tu función está estrictamente limitada a ayudar con el entorno web y la agenda académica.";

    public ChatbotService(GrokClient grokClient) {
        this.grokClient = grokClient;
    }

    public String responderDuda(String preguntaUsuario) {
        return grokClient.callGrokApi(SYSTEM_PROMPT, preguntaUsuario);
    }
}
