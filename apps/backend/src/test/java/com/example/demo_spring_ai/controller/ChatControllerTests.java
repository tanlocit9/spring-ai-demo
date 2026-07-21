package com.example.demo_spring_ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo_spring_ai.ai.response.AiResponse;
import com.example.demo_spring_ai.ai.response.AiResponseMapper;
import com.example.demo_spring_ai.ai.response.RichTextResponse;
import com.example.demo_spring_ai.ai.response.RichTextType;
import com.example.demo_spring_ai.repository.EmployeeRepository;

class ChatControllerTests {
	@Test
	void chatReturnsDynamicStructuredJsonMessagesAndNoTopLevelMessage() throws Exception {
		var chatOnlyClient = mock(ChatClient.class);
		var reportToolClient = mock(ChatClient.class);
		var requestSpec = mock(ChatClientRequestSpec.class);
		var callSpec = mock(CallResponseSpec.class);
		var employeeRepository = mock(EmployeeRepository.class);
		when(reportToolClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callSpec);
		when(callSpec.entity(AiResponse.class)).thenReturn(new AiResponse(List.of(
			new RichTextResponse(RichTextType.HEADING, "Daily Report", null, List.of(), java.util.Map.of("date", "2026-07-13")),
			new RichTextResponse(RichTextType.STATUS, "Blockers", "None", List.of(), java.util.Map.of("status", "success"))
		)));

		var mvc = MockMvcBuilders.standaloneSetup(new ChatController(chatOnlyClient, reportToolClient, new AiResponseMapper(), employeeRepository)).build();

		mvc.perform(post("/api/chat")
				.header("X-Actor-Id", "1")
				.contentType("application/json")
				.content("{\"message\":\"show my report\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.messages").isArray())
			.andExpect(jsonPath("$.messages[0].type").value("HEADING"))
			.andExpect(jsonPath("$.messages[0].title").value("Daily Report"))
			.andExpect(jsonPath("$.messages[1].type").value("STATUS"))
			.andExpect(jsonPath("$.messages[1].metadata.status").value("success"))
			.andExpect(jsonPath("$.message").doesNotExist());
	}

	@Test
	void chatOnlyMessageUsesChatOnlyClient() throws Exception {
		var chatOnlyClient = mock(ChatClient.class);
		var reportToolClient = mock(ChatClient.class);
		var requestSpec = mock(ChatClientRequestSpec.class);
		var callSpec = mock(CallResponseSpec.class);
		var employeeRepository = mock(EmployeeRepository.class);
		when(chatOnlyClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callSpec);
		when(callSpec.entity(AiResponse.class)).thenReturn(new AiResponse(List.of(
			new RichTextResponse(RichTextType.PARAGRAPH, null, "I can help with reports.", List.of(), java.util.Map.of())
		)));

		var mvc = MockMvcBuilders.standaloneSetup(new ChatController(chatOnlyClient, reportToolClient, new AiResponseMapper(), employeeRepository)).build();

		mvc.perform(post("/api/chat")
				.header("X-Actor-Id", "1")
				.contentType("application/json")
				.content("{\"message\":\"hello, what can you do?\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.messages[0].type").value("PARAGRAPH"));

		verify(chatOnlyClient).prompt();
		verify(reportToolClient, never()).prompt();
	}

	@Test
	void reportMessageUsesReportToolClient() throws Exception {
		var chatOnlyClient = mock(ChatClient.class);
		var reportToolClient = mock(ChatClient.class);
		var requestSpec = mock(ChatClientRequestSpec.class);
		var callSpec = mock(CallResponseSpec.class);
		var employeeRepository = mock(EmployeeRepository.class);
		when(reportToolClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callSpec);
		when(callSpec.entity(AiResponse.class)).thenReturn(new AiResponse(List.of(
			new RichTextResponse(RichTextType.HEADING, "Daily Report", null, List.of(), java.util.Map.of("date", "2026-07-13"))
		)));

		var mvc = MockMvcBuilders.standaloneSetup(new ChatController(chatOnlyClient, reportToolClient, new AiResponseMapper(), employeeRepository)).build();

		mvc.perform(post("/api/chat")
				.header("X-Actor-Id", "1")
				.contentType("application/json")
				.content("{\"message\":\"show my report\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.messages[0].type").value("HEADING"));

		verify(reportToolClient).prompt();
		verify(chatOnlyClient, never()).prompt();
	}

	@Test
	void ptoMessageUsesReportToolClient() throws Exception {
		var chatOnlyClient = mock(ChatClient.class);
		var reportToolClient = mock(ChatClient.class);
		var requestSpec = mock(ChatClientRequestSpec.class);
		var callSpec = mock(CallResponseSpec.class);
		var employeeRepository = mock(EmployeeRepository.class);
		when(reportToolClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callSpec);
		when(callSpec.entity(AiResponse.class)).thenReturn(new AiResponse(List.of(
			new RichTextResponse(RichTextType.STATUS, "PTO", "Recorded", List.of(), java.util.Map.of("status", "success"))
		)));

		var mvc = MockMvcBuilders.standaloneSetup(new ChatController(chatOnlyClient, reportToolClient, new AiResponseMapper(), employeeRepository)).build();

		mvc.perform(post("/api/chat")
				.header("X-Actor-Id", "1")
				.contentType("application/json")
				.content("{\"message\":\"create sick leave for today\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.messages[0].type").value("STATUS"));

		verify(reportToolClient).prompt();
		verify(chatOnlyClient, never()).prompt();
	}
}
