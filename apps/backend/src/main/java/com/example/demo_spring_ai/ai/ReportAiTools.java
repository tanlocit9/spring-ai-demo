package com.example.demo_spring_ai.ai;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo_spring_ai.domain.PtoRecord;
import com.example.demo_spring_ai.domain.PtoType;
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
	private static final Logger log = LoggerFactory.getLogger(ReportAiTools.class);

	private final ReportService reports;
	private final EmployeeAccessService employees;

	public ReportAiTools(ReportService reports, EmployeeAccessService employees) {
		this.reports = reports;
		this.employees = employees;
	}

	@Tool(description = "Create the current actor's daily report. Provide request.reportDate as ISO-8601 text, for example 2026-07-17. Permissions are enforced by the backend actor context.")
	public ReportView createDailyReport(Long actorId, ReportAiTools.AiDailyReportRequest request) {
		log.info("AI tool createDailyReport actorId={} requestDate={}", actorId, request == null ? null : request.reportDate());
		return reports.createDailyReport(new ActorContext(actorId), request.toServiceRequest());
	}

	@Tool(description = "Get the current actor's daily report for a date. Provide reportDate as ISO-8601 text, for example 2026-07-17.")
	public ReportView getMyReport(Long actorId, String reportDate) {
		log.info("AI tool getMyReport actorId={} reportDate={}", actorId, reportDate);
		return reports.getMyReport(new ActorContext(actorId), parseDate(reportDate, "reportDate"));
	}

	@Tool(description = "Update the current actor's daily report. Provide request.reportDate as ISO-8601 text, for example 2026-07-17. Managers cannot edit another employee's report.")
	public ReportView updateMyReport(Long actorId, ReportAiTools.AiDailyReportRequest request) {
		log.info("AI tool updateMyReport actorId={} requestDate={}", actorId, request == null ? null : request.reportDate());
		return reports.updateMyReport(new ActorContext(actorId), request.toServiceRequest());
	}

	@Tool(description = "Search employees visible to the current actor for duplicate-name resolution.")
	public List<EmployeeCandidate> searchVisibleEmployees(Long actorId, String query) {
		log.info("AI tool searchVisibleEmployees actorId={} query={}", actorId, query);
		return employees.searchVisibleEmployees(new ActorContext(actorId), query).stream()
			.map(e -> new EmployeeCandidate(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getDepartment()))
			.toList();
	}

	@Tool(description = "Get a visible employee's daily report after backend access checks. Provide reportDate as ISO-8601 text, for example 2026-07-17.")
	public ReportView getEmployeeReport(Long actorId, Long employeeId, String reportDate) {
		log.info("AI tool getEmployeeReport actorId={} employeeId={} reportDate={}", actorId, employeeId, reportDate);
		return reports.getEmployeeReport(new ActorContext(actorId), employeeId, parseDate(reportDate, "reportDate"));
	}

	@Tool(description = "Create a PTO record for weekly validation workflows. Provide request.ptoDate as ISO-8601 text, for example 2026-07-17.")
	public PtoRecord createPtoRecord(Long actorId, ReportAiTools.AiPtoRequest request) {
		log.info("AI tool createPtoRecord actorId={} employeeId={} ptoDate={} ptoType={}", actorId, request == null ? null : request.employeeId(), request == null ? null : request.ptoDate(), request == null ? null : request.ptoType());
		return reports.createPtoRecord(new ActorContext(actorId), request.toServiceRequest());
	}

	@Tool(description = "Generate a weekly report from daily reports and approved PTO, returning MISSING_WORKDAY_DATA when needed. Provide periodStart and periodEnd as ISO-8601 text, for example 2026-07-13.")
	public WeeklyResult generateWeeklyReport(Long actorId, Long employeeId, String periodStart, String periodEnd) {
		log.info("AI tool generateWeeklyReport actorId={} employeeId={} periodStart={} periodEnd={}", actorId, employeeId, periodStart, periodEnd);
		return reports.generateWeeklyReport(new ActorContext(actorId), employeeId, parseDate(periodStart, "periodStart"), parseDate(periodEnd, "periodEnd"));
	}

	public record AiDailyReportRequest(String reportDate, List<String> completedTasks, List<String> nextPlans, List<String> blockers, String content) {
		DailyReportRequest toServiceRequest() {
			return new DailyReportRequest(parseDate(reportDate, "reportDate"), completedTasks, nextPlans, blockers, content);
		}
	}

	public record AiPtoRequest(Long employeeId, String ptoDate, PtoType ptoType, String reason, boolean approved) {
		PtoRequest toServiceRequest() {
			return new PtoRequest(employeeId, parseDate(ptoDate, "ptoDate"), ptoType, reason, approved);
		}
	}

	private static LocalDate parseDate(String value, String fieldName) {
		try {
			return LocalDate.parse(value);
		}
		catch (RuntimeException ex) {
			throw new IllegalArgumentException(fieldName + " must be an ISO-8601 date string, for example 2026-07-17", ex);
		}
	}
}
