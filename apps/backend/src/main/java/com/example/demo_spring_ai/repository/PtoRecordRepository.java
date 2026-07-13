package com.example.demo_spring_ai.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo_spring_ai.domain.PtoRecord;
import com.example.demo_spring_ai.domain.PtoStatus;
import com.example.demo_spring_ai.domain.PtoType;

public interface PtoRecordRepository extends JpaRepository<PtoRecord, Long> {

	Optional<PtoRecord> findByEmployeeIdAndPtoDateAndPtoType(Long employeeId, LocalDate ptoDate, PtoType ptoType);

	List<PtoRecord> findByEmployeeIdAndStatusAndPtoDateBetween(Long employeeId, PtoStatus status, LocalDate start, LocalDate end);

	boolean existsByEmployeeIdAndPtoDateAndStatus(Long employeeId, LocalDate ptoDate, PtoStatus status);
}
