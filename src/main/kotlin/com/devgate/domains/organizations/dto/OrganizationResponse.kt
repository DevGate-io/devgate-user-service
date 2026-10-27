package com.devgate.domains.organizations.dto

import com.devgate.domains.organizations.models.Department
import com.devgate.domains.organizations.models.Organization
import com.devgate.domains.users.models.Target
import java.time.Instant
import java.util.UUID

data class OrganizationResponse(
	val id: UUID?,
	val name: String,
	val description: String,
	val createdAt: Instant,
	val departments: List<DepartmentResponse>
)

data class DepartmentResponse(
	val id: UUID?,
	val parentId: UUID?,
	val name: String,
	val description: String,
	val children: List<DepartmentResponse>
)

fun OrganizationResponse.toTarget() =
	Target(
		id = id.toString(),
		label = name,
		type = "organization"
	)

fun Organization.toResponse(): OrganizationResponse {
	val childrenByParentId = departments.groupBy { it.parent?.id }

	return OrganizationResponse(
		id = id,
		name = name,
		description = description,
		createdAt = createdAt,
		departments = buildDepartmentTree(null, childrenByParentId)
	)
}

private fun buildDepartmentTree(
	parentId: UUID?,
	childrenByParentId: Map<UUID?, List<Department>>
): List<DepartmentResponse> = childrenByParentId[parentId].orEmpty().map { it.toResponse(childrenByParentId) }

private fun Department.toResponse(childrenByParentId: Map<UUID?, List<Department>>): DepartmentResponse =
	DepartmentResponse(
		id = id,
		parentId = parent?.id,
		name = name,
		description = description,
		// A null id would resolve to the root bucket and recurse forever.
		children = id?.let { buildDepartmentTree(it, childrenByParentId) }.orEmpty()
	)