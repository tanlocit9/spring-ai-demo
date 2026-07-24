package com.example.demo_spring_ai.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo_spring_ai.service.EmployeeTreeNode;
import com.example.demo_spring_ai.service.EmployeeTreeService;

class EmployeeControllerTests {
	private final EmployeeTreeService employeeTreeService = mock(EmployeeTreeService.class);
	private final EmployeeController controller = new EmployeeController(employeeTreeService);

	@Test
	void returnsNestedActorRootedTree() throws Exception {
		var child = new EmployeeTreeNode(2L, "E002", "Bob Tran", "Engineering", 1L, List.of());
		var root = new EmployeeTreeNode(1L, "E001", "Alice Nguyen", "Executive", null, List.of(child));
		when(employeeTreeService.treeForActor(1L)).thenReturn(root);

		MockMvcBuilders.standaloneSetup(controller).build()
			.perform(get("/api/employees/tree").header("X-Actor-Id", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.managerId").isEmpty())
			.andExpect(jsonPath("$.children[0].id").value(2))
			.andExpect(jsonPath("$.children[0].managerId").value(1))
			.andExpect(jsonPath("$.createdAt").doesNotExist())
			.andExpect(jsonPath("$.updatedAt").doesNotExist())
			.andExpect(jsonPath("$.manager").doesNotExist());
	}

	@Test
	void missingActorHeaderReturnsBadRequest() throws Exception {
		MockMvcBuilders.standaloneSetup(controller).build()
			.perform(get("/api/employees/tree"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void malformedActorHeaderReturnsBadRequest() throws Exception {
		MockMvcBuilders.standaloneSetup(controller).build()
			.perform(get("/api/employees/tree").header("X-Actor-Id", "not-a-number"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unknownActorReturnsNotFound() throws Exception {
		when(employeeTreeService.treeForActor(99L))
			.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: 99"));

		MockMvcBuilders.standaloneSetup(controller).build()
			.perform(get("/api/employees/tree").header("X-Actor-Id", "99"))
			.andExpect(status().isNotFound());
	}
}
