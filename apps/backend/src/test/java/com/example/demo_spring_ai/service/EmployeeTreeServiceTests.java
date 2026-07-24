package com.example.demo_spring_ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo_spring_ai.domain.Employee;
import com.example.demo_spring_ai.repository.EmployeeRepository;

class EmployeeTreeServiceTests {
	private final EmployeeRepository employees = mock(EmployeeRepository.class);
	private final EmployeeTreeService service = new EmployeeTreeService(employees);

	@Test
	void buildsMultiLevelTreeAndSortsDuplicateNamesByEmployeeCode() {
		Employee root = employee(1L, "E001", "Alice Nguyen", "Executive", null);
		Employee manager = employee(2L, "E002", "Bob Tran", "Engineering", root);
		Employee secondDavid = employee(5L, "E005", "David Kim", "Engineering", manager);
		Employee firstDavid = employee(4L, "E004", "David Kim", "Engineering", manager);
		when(employees.findSubtree(1L)).thenReturn(List.of(secondDavid, manager, root, firstDavid));

		EmployeeTreeNode tree = service.treeForActor(1L);

		assertThat(tree.id()).isEqualTo(1L);
		assertThat(tree.managerId()).isNull();
		assertThat(tree.children()).extracting(EmployeeTreeNode::id).containsExactly(2L);
		assertThat(tree.children().getFirst().children())
			.extracting(EmployeeTreeNode::employeeCode)
			.containsExactly("E004", "E005");
		assertThat(tree.children().getFirst().managerId()).isEqualTo(1L);
	}

	@Test
	void returnsLeafActorWithEmptyChildren() {
		Employee manager = employee(1L, "E001", "Alice Nguyen", "Executive", null);
		Employee leaf = employee(2L, "E002", "Bob Tran", "Engineering", manager);
		when(employees.findSubtree(2L)).thenReturn(List.of(leaf));

		EmployeeTreeNode tree = service.treeForActor(2L);

		assertThat(tree.managerId()).isEqualTo(1L);
		assertThat(tree.children()).isEmpty();
	}

	@Test
	void returnsNotFoundForUnknownActor() {
		when(employees.findSubtree(99L)).thenReturn(List.of());

		assertThatThrownBy(() -> service.treeForActor(99L))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("404 NOT_FOUND");
	}

	@Test
	void rejectsRowsDisconnectedFromRoot() {
		Employee root = employee(1L, "E001", "Alice Nguyen", "Executive", null);
		Employee outsideManager = employee(9L, "E009", "Outside", "Other", null);
		Employee disconnected = employee(2L, "E002", "Bob Tran", "Engineering", outsideManager);
		when(employees.findSubtree(1L)).thenReturn(List.of(root, disconnected));

		assertThatThrownBy(() -> service.treeForActor(1L))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("disconnected");
	}

	private Employee employee(Long id, String code, String name, String department, Employee manager) {
		Employee employee = mock(Employee.class);
		when(employee.getId()).thenReturn(id);
		when(employee.getEmployeeCode()).thenReturn(code);
		when(employee.getFullName()).thenReturn(name);
		when(employee.getDepartment()).thenReturn(department);
		when(employee.getManager()).thenReturn(manager);
		return employee;
	}
}
