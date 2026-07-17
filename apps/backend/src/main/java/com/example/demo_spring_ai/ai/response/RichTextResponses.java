package com.example.demo_spring_ai.ai.response;

import java.util.List;
import java.util.Map;

public final class RichTextResponses {
	private RichTextResponses() {
	}

	public static RichTextResponse heading(String title, Map<String, Object> metadata) {
		return new RichTextResponse(RichTextType.HEADING, title, null, List.of(), metadata);
	}

	public static RichTextResponse section(String title, List<String> items) {
		return new RichTextResponse(RichTextType.SECTION, title, null, items, Map.of());
	}

	public static RichTextResponse checklist(String title, List<String> items) {
		return new RichTextResponse(RichTextType.CHECKLIST, title, null, items, Map.of());
	}

	public static RichTextResponse paragraph(String content) {
		return new RichTextResponse(RichTextType.PARAGRAPH, null, content, List.of(), Map.of());
	}

	public static RichTextResponse status(String title, String content, String status) {
		return new RichTextResponse(RichTextType.STATUS, title, content, List.of(), Map.of("status", status));
	}

	public static RichTextResponse status(String title, String content, List<String> items, String status) {
		return new RichTextResponse(RichTextType.STATUS, title, content, items, Map.of("status", status));
	}

	public static RichTextResponse employeeOptions(String title, List<?> options) {
		return new RichTextResponse(RichTextType.EMPLOYEE_OPTIONS, title, null, List.of(), Map.of("options", options));
	}

	public static RichTextResponse error(String content) {
		return new RichTextResponse(RichTextType.ERROR, "Unable to complete request", content, List.of(), Map.of());
	}
}
