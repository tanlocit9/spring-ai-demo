package com.example.demo_spring_ai.ai.response;

import java.util.List;

public record AiResponse(List<RichTextResponse> messages) {
	public AiResponse {
		messages = messages == null ? List.of() : List.copyOf(messages);
	}
}
