package com.example.demo_spring_ai.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo_spring_ai.domain.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	List<Employee> findByFullNameContainingIgnoreCaseOrderByFullNameAscEmployeeCodeAsc(String fullName);

	@Query(value = """
		WITH RECURSIVE subtree AS (
		    SELECT * FROM employees WHERE id = :rootId
		    UNION ALL
		    SELECT e.* FROM employees e JOIN subtree s ON e.manager_id = s.id
		)
		SELECT * FROM subtree ORDER BY full_name, employee_code
		""", nativeQuery = true)
	List<Employee> findSubtree(@Param("rootId") Long rootId);

	@Query(value = """
		WITH RECURSIVE subtree AS (
		    SELECT id FROM employees WHERE id = :rootId
		    UNION ALL
		    SELECT e.id FROM employees e JOIN subtree s ON e.manager_id = s.id
		)
		SELECT EXISTS(SELECT 1 FROM subtree WHERE id = :employeeId)
		""", nativeQuery = true)
	boolean isInSubtree(@Param("rootId") Long rootId, @Param("employeeId") Long employeeId);

	List<Employee> findByIdInAndFullNameContainingIgnoreCaseOrderByFullNameAscEmployeeCodeAsc(Collection<Long> ids, String fullName);
}
