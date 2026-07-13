package com.example.demo_spring_ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_spring_ai.domain.Employee;
import com.example.demo_spring_ai.repository.EmployeeRepository;
import com.example.demo_spring_ai.security.ActorContext;

@Service
@Transactional(readOnly = true)
public class EmployeeAccessService {
	private final EmployeeRepository employees;

	public EmployeeAccessService(EmployeeRepository employees) {
		this.employees = employees;
	}

	public Employee currentActor(ActorContext actor) {
		return employees.findById(actor.actorId()).orElseThrow(() -> new IllegalArgumentException("Unknown demo actor: " + actor.actorId()));
	}

	public boolean canViewEmployee(ActorContext actor, Long employeeId) {
		return employees.isInSubtree(actor.actorId(), employeeId);
	}

	public void assertCanViewEmployee(ActorContext actor, Long employeeId) {
		if (!canViewEmployee(actor, employeeId)) {
			throw new AccessDeniedException("Actor cannot view employee " + employeeId);
		}
	}

	public void assertCanEditOwnReport(ActorContext actor, Long employeeId) {
		if (!actor.actorId().equals(employeeId)) {
			throw new AccessDeniedException("Managers cannot edit another employee's report.");
		}
	}

	public List<Employee> visibleEmployees(ActorContext actor) {
		return employees.findSubtree(actor.actorId());
	}

	public List<Employee> searchVisibleEmployees(ActorContext actor, String query) {
		var visibleIds = visibleEmployees(actor).stream().map(Employee::getId).toList();
		return employees.findByIdInAndFullNameContainingIgnoreCaseOrderByFullNameAscEmployeeCodeAsc(visibleIds, query == null ? "" : query);
	}
}
