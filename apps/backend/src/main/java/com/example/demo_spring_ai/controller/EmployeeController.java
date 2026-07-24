package com.example.demo_spring_ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo_spring_ai.service.EmployeeTreeNode;
import com.example.demo_spring_ai.service.EmployeeTreeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
	private static final String ACTOR_ID_HEADER = "X-Actor-Id";

	private final EmployeeTreeService employeeTreeService;

	public EmployeeController(EmployeeTreeService employeeTreeService) {
		this.employeeTreeService = employeeTreeService;
	}

	@GetMapping("/tree")
	public EmployeeTreeNode tree(@RequestHeader(ACTOR_ID_HEADER) Long actorId) {
		return employeeTreeService.treeForActor(actorId);
	}
}
