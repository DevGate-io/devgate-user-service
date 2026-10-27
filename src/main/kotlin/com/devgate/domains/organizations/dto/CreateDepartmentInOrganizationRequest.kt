package com.devgate.domains.organizations.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateDepartmentInOrganizationRequest(
	@NotBlank
	@Size(max = 255)
	val name: String,
	@Size(max = 1000)
	val description: String? = null,
	val parentId: UUID? = null
)