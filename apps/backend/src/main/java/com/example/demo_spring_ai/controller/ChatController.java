package com.example.demo_spring_ai.controller;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.demo_spring_ai.domain.Employee;
import com.example.demo_spring_ai.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
	private static final Logger log = LoggerFactory.getLogger(ChatController.class);
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
	private final EmployeeRepository employeeRepository;

	public ChatController(
			@Qualifier("chatOnlyClient") ChatClient chatOnlyClient,
			@Qualifier("reportToolClient") ChatClient reportToolClient,
			AiResponseMapper responseMapper,
			EmployeeRepository employeeRepository) {
		this.chatOnlyClient = chatOnlyClient;
		this.reportToolClient = reportToolClient;
		this.responseMapper = responseMapper;
		this.employeeRepository = employeeRepository;
	}

	@PostMapping
	public ChatResponse chat(@RequestHeader(ACTOR_ID_HEADER) Long actorId, @RequestBody ChatRequest request) {
		try {
			String employeeContext = employeeContextFor(actorId);
			boolean usesReportTools = needsReportTools(request.message());
			log.info("chat request received actorId={} usesReportTools={} messageLength={}", actorId, usesReportTools, request.message() == null ? 0 : request.message().length());
			AiResponse response = (usesReportTools ? reportToolClient : chatOnlyClient).prompt()
				.user(user -> user.text("""
						authenticatedActorId: {actorId}
						Use this ID as the target employee ID unless the user explicitly requests
						data for another employee.

						Response language rule for this request:
						- Detect the primary language of the message below.
						- All user-facing JSON text fields must be written in that language.
						- This includes titles, content, item text, employee-context explanations,
						  status text, section labels, clarification questions, and summaries.
						- Do not translate IDs, employee codes, names, ISO dates, enum values,
						  reportType values, or metadata keys.

						authenticatedEmployeeContext:
						{employeeContext}

						message: {message}
						""")
				.param("actorId", actorId)
				.param("employeeContext", employeeContext)
				.param("message", request.message()))
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, "actor-" + actorId))
				.call()
				.entity(AiResponse.class);

			return new ChatResponse(response.messages());
		}
		catch (RuntimeException ex) {
			log.warn("chat request failed actorId={} exceptionType={} message={}", actorId, ex.getClass().getName(), ex.getMessage(), ex);
			return new ChatResponse(responseMapper.fromException(ex).messages());
		}
	}

	private String employeeContextFor(Long actorId) {
		Optional<Employee> employee = employeeRepository.findWithManagerById(actorId);
		if (employee.isEmpty()) {
			return "Employee not found for authenticated actor ID " + actorId + ".";
		}

		Employee actor = employee.get();
		Employee manager = actor.getManager();
		return """
				employeeId: %d
				employeeCode: %s
				fullName: %s
				department: %s
				managerId: %s
				managerName: %s
				""".formatted(
			actor.getId(),
			actor.getEmployeeCode(),
			actor.getFullName(),
			actor.getDepartment(),
			manager == null ? "none" : manager.getId(),
			manager == null ? "none" : manager.getFullName()
		).stripIndent().trim();
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
