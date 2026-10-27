package com.devgate.domains.organizations.dto

import jakarta.validation.constraints.Size

data class UpdateDepartmentRequest(
	@Size(max = 255)
	val name: String? = null,
	@Size(max = 1000)
	val description: String? = null
)