package com.example.demo_spring_ai.ai.response;

import java.util.List;
import java.util.Map;

public record RichTextResponse(
	RichTextType type,
	String title,
	String content,
	List<String> items,
	Map<String, Object> metadata
) {
	public RichTextResponse {
		items = items == null ? List.of() : List.copyOf(items);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
