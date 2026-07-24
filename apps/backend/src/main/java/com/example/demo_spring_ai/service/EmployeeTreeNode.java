package com.example.demo_spring_ai.service;

import java.util.List;

public record EmployeeTreeNode(
		Long id,
		String employeeCode,
		String fullName,
		String department,
		Long managerId,
		List<EmployeeTreeNode> children) {

	public EmployeeTreeNode {
		children = List.copyOf(children);
	}
}
