package com.example.demo_spring_ai.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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

	private final ChatClient chatClient;
	private final AiResponseMapper responseMapper;

	public ChatController(ChatClient chatClient, AiResponseMapper responseMapper) {
		this.chatClient = chatClient;
		this.responseMapper = responseMapper;
	}

	@PostMapping
	public ChatResponse chat(@RequestHeader(ACTOR_ID_HEADER) Long actorId, @RequestBody ChatRequest request) {
		try {
			AiResponse response = chatClient.prompt()
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

	public record ChatRequest(String message) {}

	public record ChatResponse(List<RichTextResponse> messages) {}
}
