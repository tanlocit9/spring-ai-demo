package com.example.demo_spring_ai.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo_spring_ai.domain.Employee;
import com.example.demo_spring_ai.domain.PtoRecord;
import com.example.demo_spring_ai.domain.PtoStatus;
import com.example.demo_spring_ai.domain.Report;
import com.example.demo_spring_ai.domain.ReportType;
import com.example.demo_spring_ai.repository.EmployeeRepository;
import com.example.demo_spring_ai.repository.PtoRecordRepository;
import com.example.demo_spring_ai.repository.ReportRepository;
import com.example.demo_spring_ai.security.ActorContext;
import com.example.demo_spring_ai.service.ReportDtos.DailyReportRequest;
import com.example.demo_spring_ai.service.ReportDtos.PtoRequest;
import com.example.demo_spring_ai.service.ReportDtos.ReportView;
import com.example.demo_spring_ai.service.ReportDtos.WeeklyResult;

@Service
@Transactional
public class ReportService {
	private final EmployeeRepository employees;
	private final ReportRepository reports;
	private final PtoRecordRepository ptoRecords;
	private final EmployeeAccessService access;

	public ReportService(EmployeeRepository employees, ReportRepository reports, PtoRecordRepository ptoRecords, EmployeeAccessService access) {
		this.employees = employees;
		this.reports = reports;
		this.ptoRecords = ptoRecords;
		this.access = access;
	}

	public ReportView createDailyReport(ActorContext actor, DailyReportRequest request) {
		return saveDaily(actor, actor.actorId(), request);
	}

	public ReportView updateMyReport(ActorContext actor, DailyReportRequest request) {
		return saveDaily(actor, actor.actorId(), request);
	}

	public ReportView getMyReport(ActorContext actor, LocalDate date) {
		return view(reports.findByEmployeeIdAndReportTypeAndReportDate(actor.actorId(), ReportType.DAILY, date).orElseThrow());
	}

	public ReportView getEmployeeReport(ActorContext actor, Long employeeId, LocalDate date) {
		access.assertCanViewEmployee(actor, employeeId);
		return view(reports.findByEmployeeIdAndReportTypeAndReportDate(employeeId, ReportType.DAILY, date).orElseThrow());
	}

	public PtoRecord createPtoRecord(ActorContext actor, PtoRequest request) {
		access.assertCanViewEmployee(actor, request.employeeId());
		var employee = employees.findById(request.employeeId()).orElseThrow();
		var record = ptoRecords.findByEmployeeIdAndPtoDateAndPtoType(request.employeeId(), request.ptoDate(), request.ptoType()).orElseGet(PtoRecord::new);
		record.setEmployee(employee);
		record.setPtoDate(request.ptoDate());
		record.setPtoType(request.ptoType());
		record.setReason(request.reason());
		record.setStatus(request.approved() ? PtoStatus.APPROVED : PtoStatus.PENDING);
		return ptoRecords.save(record);
	}

	public WeeklyResult generateWeeklyReport(ActorContext actor, Long employeeId, LocalDate start, LocalDate end) {
		access.assertCanViewEmployee(actor, employeeId);
		var missing = workingDays(start, end).stream()
			.filter(day -> reports.findByEmployeeIdAndReportTypeAndReportDate(employeeId, ReportType.DAILY, day).isEmpty())
			.filter(day -> !ptoRecords.existsByEmployeeIdAndPtoDateAndStatus(employeeId, day, PtoStatus.APPROVED))
			.toList();
		if (!missing.isEmpty()) {
			return new WeeklyResult("MISSING_WORKDAY_DATA", null, missing);
		}
		var employee = employees.findById(employeeId).orElseThrow();
		var daily = reports.findByEmployeeIdAndReportTypeAndReportDateBetweenOrderByReportDateAsc(employeeId, ReportType.DAILY, start, end);
		var weekly = reports.findByEmployeeIdAndReportTypeAndPeriodStartAndPeriodEnd(employeeId, ReportType.WEEKLY, start, end).orElseGet(Report::new);
		weekly.setEmployee(employee);
		weekly.setReportType(ReportType.WEEKLY);
		weekly.setPeriodStart(start);
		weekly.setPeriodEnd(end);
		weekly.setCompletedTasks(daily.stream().flatMap(r -> r.getCompletedTasks().stream()).toList());
		weekly.setNextPlans(daily.stream().flatMap(r -> r.getNextPlans().stream()).toList());
		weekly.setBlockers(daily.stream().flatMap(r -> r.getBlockers().stream()).toList());
		weekly.setContent("Weekly summary generated from " + daily.size() + " daily report(s).");
		return new WeeklyResult("OK", view(reports.save(weekly)), List.of());
	}

	private ReportView saveDaily(ActorContext actor, Long employeeId, DailyReportRequest request) {
		access.assertCanEditOwnReport(actor, employeeId);
		Employee employee = employees.findById(employeeId).orElseThrow();
		Report report = reports.findByEmployeeIdAndReportTypeAndReportDate(employeeId, ReportType.DAILY, request.reportDate()).orElseGet(Report::new);
		report.setEmployee(employee);
		report.setReportType(ReportType.DAILY);
		report.setReportDate(request.reportDate());
		report.setCompletedTasks(request.completedTasks());
		report.setNextPlans(request.nextPlans());
		report.setBlockers(request.blockers());
		report.setContent(request.content());
		return view(reports.save(report));
	}

	private static List<LocalDate> workingDays(LocalDate start, LocalDate end) {
		var days = new ArrayList<LocalDate>();
		for (var day = start; !day.isAfter(end); day = day.plusDays(1)) {
			if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) days.add(day);
		}
		return days;
	}

	private static ReportView view(Report r) {
		return new ReportView(r.getId(), r.getEmployee().getId(), r.getReportType(), r.getReportDate(), r.getPeriodStart(), r.getPeriodEnd(), r.getCompletedTasks(), r.getNextPlans(), r.getBlockers(), r.getContent());
	}
}
