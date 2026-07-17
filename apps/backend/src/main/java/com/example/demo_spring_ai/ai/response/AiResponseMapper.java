package com.example.demo_spring_ai.ai.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Component;

import com.example.demo_spring_ai.domain.ReportType;
import com.example.demo_spring_ai.service.AccessDeniedException;
import com.example.demo_spring_ai.service.ReportDtos.ReportView;
import com.example.demo_spring_ai.service.ReportDtos.WeeklyResult;

@Component
public class AiResponseMapper {
	public AiResponse fromText(String content) {
		if (content == null || content.isBlank()) {
			return new AiResponse(List.of(RichTextResponses.status("No response", "No content was returned.", "empty")));
		}
		return new AiResponse(List.of(RichTextResponses.paragraph(content)));
	}

	public AiResponse fromReport(ReportView report) {
		if (report == null) {
			return new AiResponse(List.of(RichTextResponses.status("Report not found", "No report data was returned.", "empty")));
		}

		var messages = new ArrayList<RichTextResponse>();
		var title = report.reportType() == ReportType.WEEKLY ? "Weekly Report" : "Daily Report";
		messages.add(RichTextResponses.heading(title, reportMetadata(report)));
		addSectionIfPresent(messages, "Completed Tasks", report.completedTasks());
		addSectionIfPresent(messages, "Next Plans", report.nextPlans());
		messages.add(blockersStatus(report.blockers()));
		if (report.content() != null && !report.content().isBlank()) {
			messages.add(new RichTextResponse(RichTextType.PARAGRAPH, "Summary", report.content(), List.of(), Map.of()));
		}
		return new AiResponse(messages);
	}

	public AiResponse fromWeeklyResult(WeeklyResult result) {
		if (result == null) {
			return new AiResponse(List.of(RichTextResponses.status("Weekly report unavailable", "No weekly report result was returned.", "empty")));
		}
		if (!result.missingWorkdays().isEmpty()) {
			var missingDays = result.missingWorkdays().stream().map(String::valueOf).toList();
			return new AiResponse(List.of(
				RichTextResponses.status("Missing workday data", "Weekly report cannot be generated until all workdays have report or approved PTO data.", missingDays, "warning")
			));
		}
		return fromReport(result.report());
	}

	public AiResponse fromException(RuntimeException exception) {
		if (exception instanceof AccessDeniedException) {
			return new AiResponse(List.of(RichTextResponses.status("Access denied", "You do not have permission to access this data.", "denied")));
		}
		if (exception instanceof NoSuchElementException) {
			return new AiResponse(List.of(RichTextResponses.status("Report not found", "No matching report data was found.", "empty")));
		}
		if (exception instanceof IllegalArgumentException) {
			return new AiResponse(List.of(RichTextResponses.status("Invalid request", safeMessage(exception, "The request contains invalid input."), "warning")));
		}
		return new AiResponse(List.of(RichTextResponses.error("The request could not be completed. Please try again later.")));
	}

	private static Map<String, Object> reportMetadata(ReportView report) {
		var metadata = new java.util.LinkedHashMap<String, Object>();
		metadata.put("employeeId", report.employeeId());
		metadata.put("reportType", report.reportType().name());
		if (report.reportDate() != null) {
			metadata.put("date", report.reportDate().toString());
		}
		if (report.periodStart() != null) {
			metadata.put("periodStart", report.periodStart().toString());
		}
		if (report.periodEnd() != null) {
			metadata.put("periodEnd", report.periodEnd().toString());
		}
		return metadata;
	}

	private static void addSectionIfPresent(List<RichTextResponse> messages, String title, List<String> items) {
		if (items != null && !items.isEmpty()) {
			messages.add(RichTextResponses.section(title, items));
		}
	}

	private static RichTextResponse blockersStatus(List<String> blockers) {
		if (blockers == null || blockers.isEmpty()) {
			return RichTextResponses.status("Blockers", "None", "success");
		}
		return RichTextResponses.status("Blockers", null, blockers, "warning");
	}

	private static String safeMessage(RuntimeException exception, String fallback) {
		return exception.getMessage() == null || exception.getMessage().isBlank() ? fallback : exception.getMessage();
	}
}
