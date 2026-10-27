package com.devgate.domains.organizations.services.impl

import com.devgate.domains.organizations.dto.CreateDepartmentInOrganizationRequest
import com.devgate.domains.organizations.dto.CreateDepartmentRequest
import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.dto.DepartmentChangeResult
import com.devgate.domains.organizations.dto.OrganizationResponse
import com.devgate.domains.organizations.dto.UpdateDepartmentRequest
import com.devgate.domains.organizations.dto.toResponse
import com.devgate.domains.organizations.dto.toTarget
import com.devgate.domains.organizations.models.Department
import com.devgate.domains.organizations.models.MAX_DEPARTMENT_DEPTH
import com.devgate.domains.organizations.models.Organization
import com.devgate.domains.organizations.repositories.DepartmentRepository
import com.devgate.domains.organizations.repositories.OrganizationRepository
import com.devgate.domains.organizations.services.OrganizationService
import com.devgate.domains.users.models.Target
import com.devgate.exceptions.ApiException
import com.devgate.exceptions.DepartmentDepthExceededException
import com.devgate.exceptions.DepartmentNotFoundException
import com.devgate.exceptions.OrganizationAlreadyExistsException
import com.devgate.exceptions.OrganizationNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizationServiceImpl
	@Autowired
	constructor(
		private val organizationRepository: OrganizationRepository,
		private val departmentRepository: DepartmentRepository
	) : OrganizationService {
		@Transactional(readOnly = true)
		override fun getAllOrganizations(): List<OrganizationResponse> =
			organizationRepository.findAll().map { it.toResponse() }

		@Transactional(readOnly = true)
		override fun getOrganizationById(id: UUID): OrganizationResponse = findOrganization(id).toResponse()

		@Transactional
		override fun createOrganization(request: CreateOrganizationRequest): OrganizationResponse {
			if (request.departments.any { it.depth() > MAX_DEPARTMENT_DEPTH }) {
				throw DepartmentDepthExceededException()
			}

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

		@Transactional
		override fun addDepartment(
			organizationId: UUID,
			request: CreateDepartmentInOrganizationRequest
		): DepartmentChangeResult {
			val organization = findOrganization(organizationId)
			val parent = request.parentId?.let { findDepartment(organizationId, it) }

			if (parent != null && parent.depth() >= MAX_DEPARTMENT_DEPTH) {
				throw DepartmentDepthExceededException()
			}

			val department =
				Department(
					organization = organization,
					parent = parent,
					name = request.name.trim(),
					description = request.description?.trim().orEmpty()
				)

			// Persisted directly (not merged through the organization) so the id lands on this
			// instance; the flush makes the still-lazy departments collection load it below.
			departmentRepository.saveAndFlush(department)

			return DepartmentChangeResult(
				organization = organization.toResponse(),
				department = department.toTarget()
			)
		}

		@Transactional
		override fun updateDepartment(
			organizationId: UUID,
			departmentId: UUID,
			request: UpdateDepartmentRequest
		): DepartmentChangeResult {
			val organization = findOrganization(organizationId)
			val department = findDepartment(organizationId, departmentId)

			request.name?.let { name ->
				if (name.isBlank()) {
					throw ApiException(message = "Department name must not be blank", httpStatus = HttpStatus.BAD_REQUEST)
				}
				department.name = name.trim()
			}
			request.description?.let { department.description = it.trim() }

			return DepartmentChangeResult(
				organization = organization.toResponse(),
				department = department.toTarget()
			)
		}

		@Transactional
		override fun deleteDepartment(
			organizationId: UUID,
			departmentId: UUID
		): Target {
			val department = findDepartment(organizationId, departmentId)
			val target = department.toTarget()

			// The subtree is removed by the departments.parent_id FK (ON DELETE CASCADE). The
			// organization departments collection stays uninitialized here, so no stale
			// descendants are left in the persistence context for Hibernate to delete again.
			departmentRepository.delete(department)

			return target
		}

		private fun findOrganization(id: UUID): Organization =
			organizationRepository
				.findById(id)
				.orElseThrow { OrganizationNotFoundException() }

		private fun findDepartment(
			organizationId: UUID,
			departmentId: UUID
		): Department =
			departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
				?: throw DepartmentNotFoundException()

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

		private fun CreateDepartmentRequest.depth(): Int = 1 + (children.maxOfOrNull { it.depth() } ?: 0)

		private fun Department.depth(): Int = generateSequence(this) { it.parent }.count()
	}