package com.example.demo_spring_ai.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo_spring_ai.domain.Employee;
import com.example.demo_spring_ai.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class EmployeeTreeService {
	private static final Comparator<Employee> EMPLOYEE_ORDER = Comparator
		.comparing(Employee::getFullName, String.CASE_INSENSITIVE_ORDER)
		.thenComparing(Employee::getEmployeeCode, String.CASE_INSENSITIVE_ORDER)
		.thenComparing(Employee::getId);

	private final EmployeeRepository employees;

	public EmployeeTreeService(EmployeeRepository employees) {
		this.employees = employees;
	}

	public EmployeeTreeNode treeForActor(Long actorId) {
		List<Employee> subtree = employees.findSubtree(actorId);
		if (subtree.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + actorId);
		}

		Map<Long, Employee> employeesById = new HashMap<>();
		Map<Long, List<Employee>> childrenByManagerId = new HashMap<>();
		for (Employee employee : subtree) {
			if (employeesById.put(employee.getId(), employee) != null) {
				throw malformedHierarchy("Duplicate employee in subtree: " + employee.getId());
			}
			Employee manager = employee.getManager();
			if (manager != null) {
				childrenByManagerId.computeIfAbsent(manager.getId(), ignored -> new ArrayList<>()).add(employee);
			}
		}

		Employee root = employeesById.get(actorId);
		if (root == null) {
			throw malformedHierarchy("Subtree does not contain requested root: " + actorId);
		}
		childrenByManagerId.values().forEach(children -> children.sort(EMPLOYEE_ORDER));

		Set<Long> visiting = new HashSet<>();
		Set<Long> assembled = new HashSet<>();
		EmployeeTreeNode tree = assemble(root, childrenByManagerId, visiting, assembled);
		if (assembled.size() != employeesById.size()) {
			throw malformedHierarchy("Subtree contains employees disconnected from root: " + actorId);
		}
		return tree;
	}

	private EmployeeTreeNode assemble(
			Employee employee,
			Map<Long, List<Employee>> childrenByManagerId,
			Set<Long> visiting,
			Set<Long> assembled) {
		if (!visiting.add(employee.getId())) {
			throw malformedHierarchy("Cycle detected at employee: " + employee.getId());
		}
		if (assembled.contains(employee.getId())) {
			throw malformedHierarchy("Employee appears more than once in hierarchy: " + employee.getId());
		}

		List<EmployeeTreeNode> children = childrenByManagerId
			.getOrDefault(employee.getId(), List.of())
			.stream()
			.map(child -> assemble(child, childrenByManagerId, visiting, assembled))
			.toList();
		visiting.remove(employee.getId());
		assembled.add(employee.getId());

		Employee manager = employee.getManager();
		return new EmployeeTreeNode(
			employee.getId(),
			employee.getEmployeeCode(),
			employee.getFullName(),
			employee.getDepartment(),
			manager == null ? null : manager.getId(),
			children);
	}

	private IllegalStateException malformedHierarchy(String message) {
		return new IllegalStateException("Invalid employee hierarchy. " + message);
	}
}
