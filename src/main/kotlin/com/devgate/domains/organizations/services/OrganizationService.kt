package com.devgate.domains.organizations.services

import com.devgate.domains.organizations.dto.CreateDepartmentInOrganizationRequest
import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.dto.DepartmentChangeResult
import com.devgate.domains.organizations.dto.OrganizationResponse
import com.devgate.domains.organizations.dto.UpdateDepartmentRequest
import com.devgate.domains.users.models.Target
import java.util.UUID

interface OrganizationService {
	fun getAllOrganizations(): List<OrganizationResponse>

	fun getOrganizationById(id: UUID): OrganizationResponse

	fun createOrganization(request: CreateOrganizationRequest): OrganizationResponse

	fun addDepartment(
		organizationId: UUID,
		request: CreateDepartmentInOrganizationRequest
	): DepartmentChangeResult

	fun updateDepartment(
		organizationId: UUID,
		departmentId: UUID,
		request: UpdateDepartmentRequest
	): DepartmentChangeResult

	fun deleteDepartment(
		organizationId: UUID,
		departmentId: UUID
	): Target
}