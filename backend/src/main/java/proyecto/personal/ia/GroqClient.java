package proyecto.personal.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqClient {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String apiModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record GroqResponse(String text, String toolName, String toolArguments, String toolCallId) {
        public boolean isToolCall() { return toolName != null; }
    }

    public String callGroqApi(String systemPrompt, String userMessage) {
        try {
            List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            );
            GroqResponse response = execute(messages, null);
            return response.text() != null ? response.text() : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al comunicarse con Groq: " + e.getMessage();
        }
    }

    public GroqResponse callWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) throws Exception {
        return execute(messages, tools);
    }

    public String sendToolResult(List<Map<String, Object>> messages, String toolCallId, String result) throws Exception {
        List<Map<String, Object>> updated = new ArrayList<>(messages);
        Map<String, Object> toolMsg = new HashMap<>();
        toolMsg.put("role", "tool");
        toolMsg.put("tool_call_id", toolCallId);
        toolMsg.put("content", result);
        updated.add(toolMsg);
        GroqResponse response = execute(updated, null);
        return response.text() != null ? response.text() : "";
    }

    private GroqResponse execute(List<Map<String, Object>> messages, List<Map<String, Object>> tools) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", apiModel);
        body.put("messages", messages);
        body.put("temperature", 0.3);

        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choice = root.path("choices").get(0);
        JsonNode message = choice.path("message");
        String finishReason = choice.path("finish_reason").asText();

        if ("tool_calls".equals(finishReason)) {
            JsonNode toolCall = message.path("tool_calls").get(0);
            return new GroqResponse(
                null,
                toolCall.path("function").path("name").asText(),
                toolCall.path("function").path("arguments").asText(),
                toolCall.path("id").asText()
            );
        }

        return new GroqResponse(message.path("content").asText(), null, null, null);
    }
}
