package com.devgate.domains.organizations.services

import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.dto.OrganizationResponse
import java.util.UUID

interface OrganizationService {
	fun getAllOrganizations(): List<OrganizationResponse>

	fun getOrganizationById(id: UUID): OrganizationResponse

	fun createOrganization(request: CreateOrganizationRequest): OrganizationResponse
}