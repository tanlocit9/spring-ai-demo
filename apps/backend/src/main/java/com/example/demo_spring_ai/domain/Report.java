package com.example.demo_spring_ai.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reports")
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Enumerated(EnumType.STRING)
	@Column(name = "report_type", nullable = false)
	private ReportType reportType;

	@Column(name = "report_date")
	private LocalDate reportDate;

	@Column(name = "period_start")
	private LocalDate periodStart;

	@Column(name = "period_end")
	private LocalDate periodEnd;

	@JdbcTypeCode(SqlTypes.JSON)
	@Convert(converter = JsonbListConverter.class)
	@Column(name = "completed_tasks", nullable = false, columnDefinition = "jsonb")
	private List<String> completedTasks = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Convert(converter = JsonbListConverter.class)
	@Column(name = "next_plans", nullable = false, columnDefinition = "jsonb")
	private List<String> nextPlans = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Convert(converter = JsonbListConverter.class)
	@Column(name = "blockers", nullable = false, columnDefinition = "jsonb")
	private List<String> blockers = new ArrayList<>();

	@Column(nullable = false)
	private String content = "";

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Long getId() { return id; }
	public Employee getEmployee() { return employee; }
	public void setEmployee(Employee employee) { this.employee = employee; }
	public ReportType getReportType() { return reportType; }
	public void setReportType(ReportType reportType) { this.reportType = reportType; }
	public LocalDate getReportDate() { return reportDate; }
	public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
	public LocalDate getPeriodStart() { return periodStart; }
	public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
	public LocalDate getPeriodEnd() { return periodEnd; }
	public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
	public List<String> getCompletedTasks() { return completedTasks; }
	public void setCompletedTasks(List<String> completedTasks) { this.completedTasks = completedTasks == null ? new ArrayList<>() : completedTasks; }
	public List<String> getNextPlans() { return nextPlans; }
	public void setNextPlans(List<String> nextPlans) { this.nextPlans = nextPlans == null ? new ArrayList<>() : nextPlans; }
	public List<String> getBlockers() { return blockers; }
	public void setBlockers(List<String> blockers) { this.blockers = blockers == null ? new ArrayList<>() : blockers; }
	public String getContent() { return content; }
	public void setContent(String content) { this.content = content == null ? "" : content; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
	public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
