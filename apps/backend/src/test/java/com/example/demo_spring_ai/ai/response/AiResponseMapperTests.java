package com.example.demo_spring_ai.ai.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.example.demo_spring_ai.domain.ReportType;
import com.example.demo_spring_ai.service.AccessDeniedException;
import com.example.demo_spring_ai.service.ReportDtos.ReportView;
import com.example.demo_spring_ai.service.ReportDtos.WeeklyResult;

class AiResponseMapperTests {
	private final AiResponseMapper mapper = new AiResponseMapper();

	@Test
	void mapsDailyReportWithNoBlockersToSuccessStatus() {
		var response = mapper.fromReport(new ReportView(
			1L,
			2L,
			ReportType.DAILY,
			LocalDate.parse("2026-07-13"),
			null,
			null,
			List.of("Reviewed team delivery risks"),
			List.of("Prepare weekly summary"),
			List.of(),
			"All work is on track."
		));

		assertThat(response.messages()).extracting(RichTextResponse::type)
			.containsExactly(RichTextType.HEADING, RichTextType.SECTION, RichTextType.SECTION, RichTextType.STATUS, RichTextType.PARAGRAPH);
		assertThat(response.messages().get(0).metadata()).containsEntry("date", "2026-07-13");
		assertThat(response.messages().get(3).title()).isEqualTo("Blockers");
		assertThat(response.messages().get(3).content()).isEqualTo("None");
		assertThat(response.messages().get(3).metadata()).containsEntry("status", "success");
	}

	@Test
	void mapsDailyReportWithBlockersToWarningStatus() {
		var response = mapper.fromReport(new ReportView(
			1L,
			2L,
			ReportType.DAILY,
			LocalDate.parse("2026-07-13"),
			null,
			null,
			List.of("Completed API review"),
			List.of("Fix defects"),
			List.of("Waiting for test data"),
			null
		));

		var blockers = response.messages().get(3);
		assertThat(blockers.type()).isEqualTo(RichTextType.STATUS);
		assertThat(blockers.items()).containsExactly("Waiting for test data");
		assertThat(blockers.metadata()).containsEntry("status", "warning");
	}

	@Test
	void mapsWeeklyMissingWorkdaysToWarningStatus() {
		var response = mapper.fromWeeklyResult(new WeeklyResult(
			"MISSING_WORKDAY_DATA",
			null,
			List.of(LocalDate.parse("2026-07-13"), LocalDate.parse("2026-07-14"))
		));

		assertThat(response.messages()).hasSize(1);
		assertThat(response.messages().getFirst().type()).isEqualTo(RichTextType.STATUS);
		assertThat(response.messages().getFirst().title()).isEqualTo("Missing workday data");
		assertThat(response.messages().getFirst().items()).containsExactly("2026-07-13", "2026-07-14");
		assertThat(response.messages().getFirst().metadata()).containsEntry("status", "warning");
	}

	@Test
	void mapsWeeklySuccessToWeeklyReportBlocks() {
		var response = mapper.fromWeeklyResult(new WeeklyResult(
			"OK",
			new ReportView(
				10L,
				2L,
				ReportType.WEEKLY,
				null,
				LocalDate.parse("2026-07-13"),
				LocalDate.parse("2026-07-17"),
				List.of("Delivered backend API"),
				List.of("Prepare demo"),
				List.of(),
				"Weekly summary generated from 5 daily report(s)."
			),
			List.of()
		));

		assertThat(response.messages().getFirst().title()).isEqualTo("Weekly Report");
		assertThat(response.messages().getFirst().metadata())
			.containsEntry("periodStart", "2026-07-13")
			.containsEntry("periodEnd", "2026-07-17");
	}

	@Test
	void mapsKnownExceptionsToSafeResponses() {
		assertThat(mapper.fromException(new AccessDeniedException("internal employee id 99")).messages().getFirst().content())
			.isEqualTo("You do not have permission to access this data.");
		assertThat(mapper.fromException(new NoSuchElementException()).messages().getFirst().metadata())
			.containsEntry("status", "empty");
		assertThat(mapper.fromException(new IllegalArgumentException("reportDate must be ISO-8601")).messages().getFirst().content())
			.isEqualTo("reportDate must be ISO-8601");
	}

	@Test
	void mapsPlainTextToParagraphFallback() {
		var response = mapper.fromText("Here is your report summary.");

		assertThat(response.messages()).hasSize(1);
		assertThat(response.messages().getFirst().type()).isEqualTo(RichTextType.PARAGRAPH);
		assertThat(response.messages().getFirst().content()).isEqualTo("Here is your report summary.");
	}
}
