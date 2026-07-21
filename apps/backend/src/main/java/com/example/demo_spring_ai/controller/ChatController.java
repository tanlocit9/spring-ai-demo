package com.example.demo_spring_ai.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo_spring_ai.ai.response.AiResponse;
import com.example.demo_spring_ai.ai.response.AiResponseMapper;
import com.example.demo_spring_ai.ai.response.RichTextResponse;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
	private static final String ACTOR_ID_HEADER = "X-Actor-Id";
	private static final List<String> TOOL_INTENT_KEYWORDS = List.of(
		"report",
		"daily",
		"weekly",
		"pto",
		"leave",
		"vacation",
		"sick",
		"employee",
		"manager",
		"blocker",
		"completed",
		"next plan",
		"summary",
		"create",
		"update",
		"show my",
		"my report",
		"nghỉ",
		"phép"
	);

	private final ChatClient chatOnlyClient;
	private final ChatClient reportToolClient;
	private final AiResponseMapper responseMapper;

	public ChatController(
			@Qualifier("chatOnlyClient") ChatClient chatOnlyClient,
			@Qualifier("reportToolClient") ChatClient reportToolClient,
			AiResponseMapper responseMapper) {
		this.chatOnlyClient = chatOnlyClient;
		this.reportToolClient = reportToolClient;
		this.responseMapper = responseMapper;
	}

	@PostMapping
	public ChatResponse chat(@RequestHeader(ACTOR_ID_HEADER) Long actorId, @RequestBody ChatRequest request) {
		try {
			AiResponse response = chatClientFor(request.message()).prompt()
				.user(user -> user.text("""
						authenticatedActorId: {actorId}
						Use this ID as the target employee ID unless the user explicitly requests
						data for another employee.

						message: {message}
						""")
				.param("actorId", actorId)
				.param("message", request.message()))
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, "actor-" + actorId))
				.call()
				.entity(AiResponse.class);

			return new ChatResponse(response.messages());
		}
		catch (RuntimeException ex) {
			return new ChatResponse(responseMapper.fromException(ex).messages());
		}
	}

	private ChatClient chatClientFor(String message) {
		return needsReportTools(message) ? reportToolClient : chatOnlyClient;
	}

	private boolean needsReportTools(String message) {
		if (message == null || message.isBlank()) {
			return false;
		}

		String normalizedMessage = message.toLowerCase(Locale.ROOT);
		return TOOL_INTENT_KEYWORDS.stream().anyMatch(normalizedMessage::contains);
	}

	public record ChatRequest(String message) {}

	public record ChatResponse(List<RichTextResponse> messages) {}
}
