package com.example.demo_spring_ai.ai;

import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo_spring_ai.domain.PtoRecord;
import com.example.demo_spring_ai.security.ActorContext;
import com.example.demo_spring_ai.service.EmployeeAccessService;
import com.example.demo_spring_ai.service.ReportDtos.DailyReportRequest;
import com.example.demo_spring_ai.service.ReportDtos.EmployeeCandidate;
import com.example.demo_spring_ai.service.ReportDtos.PtoRequest;
import com.example.demo_spring_ai.service.ReportDtos.ReportView;
import com.example.demo_spring_ai.service.ReportDtos.WeeklyResult;
import com.example.demo_spring_ai.service.ReportService;

@Component
public class ReportAiTools {
	private final ReportService reports;
	private final EmployeeAccessService employees;

	public ReportAiTools(ReportService reports, EmployeeAccessService employees) {
		this.reports = reports;
		this.employees = employees;
	}

	@Tool(description = "Create the current actor's daily report. Permissions are enforced by the backend actor context.")
	public ReportView createDailyReport(Long actorId, DailyReportRequest request) {
		return reports.createDailyReport(new ActorContext(actorId), request);
	}

	@Tool(description = "Get the current actor's daily report for a date.")
	public ReportView getMyReport(Long actorId, LocalDate reportDate) {
		return reports.getMyReport(new ActorContext(actorId), reportDate);
	}

	@Tool(description = "Update the current actor's daily report. Managers cannot edit another employee's report.")
	public ReportView updateMyReport(Long actorId, DailyReportRequest request) {
		return reports.updateMyReport(new ActorContext(actorId), request);
	}

	@Tool(description = "Search employees visible to the current actor for duplicate-name resolution.")
	public List<EmployeeCandidate> searchVisibleEmployees(Long actorId, String query) {
		return employees.searchVisibleEmployees(new ActorContext(actorId), query).stream()
			.map(e -> new EmployeeCandidate(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getDepartment()))
			.toList();
	}

	@Tool(description = "Get a visible employee's daily report after backend access checks.")
	public ReportView getEmployeeReport(Long actorId, Long employeeId, LocalDate reportDate) {
		return reports.getEmployeeReport(new ActorContext(actorId), employeeId, reportDate);
	}

	@Tool(description = "Create a PTO record for weekly validation workflows.")
	public PtoRecord createPtoRecord(Long actorId, PtoRequest request) {
		return reports.createPtoRecord(new ActorContext(actorId), request);
	}

	@Tool(description = "Generate a weekly report from daily reports and approved PTO, returning MISSING_WORKDAY_DATA when needed.")
	public WeeklyResult generateWeeklyReport(Long actorId, Long employeeId, LocalDate periodStart, LocalDate periodEnd) {
		return reports.generateWeeklyReport(new ActorContext(actorId), employeeId, periodStart, periodEnd);
	}
}
