package com.example.demo_spring_ai.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo_spring_ai.domain.Report;
import com.example.demo_spring_ai.domain.ReportType;

public interface ReportRepository extends JpaRepository<Report, Long> {

	Optional<Report> findByEmployeeIdAndReportTypeAndReportDate(Long employeeId, ReportType reportType, LocalDate reportDate);

	Optional<Report> findByEmployeeIdAndReportTypeAndPeriodStartAndPeriodEnd(Long employeeId, ReportType reportType, LocalDate periodStart, LocalDate periodEnd);

	List<Report> findByEmployeeIdAndReportTypeAndReportDateBetweenOrderByReportDateAsc(Long employeeId, ReportType reportType, LocalDate start, LocalDate end);
}
