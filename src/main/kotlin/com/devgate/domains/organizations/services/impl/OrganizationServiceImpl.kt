package com.devgate.domains.organizations.services.impl

import com.devgate.domains.organizations.dto.CreateDepartmentRequest
import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.dto.OrganizationResponse
import com.devgate.domains.organizations.dto.toResponse
import com.devgate.domains.organizations.models.Department
import com.devgate.domains.organizations.models.Organization
import com.devgate.domains.organizations.repositories.OrganizationRepository
import com.devgate.domains.organizations.services.OrganizationService
import com.devgate.exceptions.OrganizationAlreadyExistsException
import com.devgate.exceptions.OrganizationNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizationServiceImpl
	@Autowired
	constructor(
		private val organizationRepository: OrganizationRepository
	) : OrganizationService {
		@Transactional(readOnly = true)
		override fun getAllOrganizations(): List<OrganizationResponse> =
			organizationRepository.findAll().map { it.toResponse() }

		@Transactional(readOnly = true)
		override fun getOrganizationById(id: UUID): OrganizationResponse =
			organizationRepository
				.findById(id)
				.orElseThrow { OrganizationNotFoundException() }
				.toResponse()

		@Transactional
		override fun createOrganization(request: CreateOrganizationRequest): OrganizationResponse {
			val name = request.name.trim()

			if (organizationRepository.existsByNameIgnoreCase(name)) {
				throw OrganizationAlreadyExistsException()
			}

			val organization =
				Organization(
					name = name,
					description = request.description?.trim().orEmpty()
				)

			addDepartments(organization, null, request.departments)

			return organizationRepository.save(organization).toResponse()
		}

		// Pre-order walk: a parent is added (and therefore persisted) before its children.
		private fun addDepartments(
			organization: Organization,
			parent: Department?,
			requests: List<CreateDepartmentRequest>
		) {
			requests.forEach { request ->
				val department =
					Department(
						organization = organization,
						parent = parent,
						name = request.name.trim(),
						description = request.description?.trim().orEmpty()
					)

				organization.departments.add(department)
				addDepartments(organization, department, request.children)
			}
		}
	}