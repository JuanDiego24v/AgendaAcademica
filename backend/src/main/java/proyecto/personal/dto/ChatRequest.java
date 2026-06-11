package proyecto.personal.dto;

import java.util.List;

public record ChatRequest(String mensaje, List<ChatMessageDto> historial) {}
