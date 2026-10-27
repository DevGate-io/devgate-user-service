package com.devgate.domains.organizations.repositories

import com.devgate.domains.organizations.models.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DepartmentRepository : JpaRepository<Department, UUID> {
	fun findByIdAndOrganizationId(
		id: UUID,
		organizationId: UUID
	): Department?
}