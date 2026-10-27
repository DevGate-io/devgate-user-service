package com.devgate.domains.organizations.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateDepartmentRequest(
	@NotBlank
	@Size(max = 255)
	val name: String,
	@Size(max = 1000)
	val description: String? = null,
	@Valid
	val children: List<CreateDepartmentRequest> = emptyList()
)