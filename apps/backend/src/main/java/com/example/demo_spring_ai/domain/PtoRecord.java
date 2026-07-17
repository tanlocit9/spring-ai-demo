package com.example.demo_spring_ai.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
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
@Table(name = "pto_records")
public class PtoRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "pto_date", nullable = false)
	private LocalDate ptoDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "pto_type", nullable = false, columnDefinition = "pto_type")
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	private PtoType ptoType;

	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "pto_status")
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	private PtoStatus status = PtoStatus.PENDING;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Long getId() { return id; }
	public Employee getEmployee() { return employee; }
	public void setEmployee(Employee employee) { this.employee = employee; }
	public LocalDate getPtoDate() { return ptoDate; }
	public void setPtoDate(LocalDate ptoDate) { this.ptoDate = ptoDate; }
	public PtoType getPtoType() { return ptoType; }
	public void setPtoType(PtoType ptoType) { this.ptoType = ptoType; }
	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
	public PtoStatus getStatus() { return status; }
	public void setStatus(PtoStatus status) { this.status = status; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
	public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
