package com.example.demo_spring_ai.service;

import java.time.LocalDate;
import java.util.List;

import com.example.demo_spring_ai.domain.PtoType;
import com.example.demo_spring_ai.domain.ReportType;

public final class ReportDtos {
	private ReportDtos() {}

	public record EmployeeCandidate(Long id, String employeeCode, String fullName, String department) {}
	public record ReportView(Long id, Long employeeId, ReportType reportType, LocalDate reportDate, LocalDate periodStart, LocalDate periodEnd, List<String> completedTasks, List<String> nextPlans, List<String> blockers, String content) {}
	public record DailyReportRequest(LocalDate reportDate, List<String> completedTasks, List<String> nextPlans, List<String> blockers, String content) {}
	public record PtoRequest(Long employeeId, LocalDate ptoDate, PtoType ptoType, String reason, boolean approved) {}
	public record WeeklyResult(String status, ReportView report, List<LocalDate> missingWorkdays) {}
}
