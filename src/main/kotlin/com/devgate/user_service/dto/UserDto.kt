package com.devgate.user_service.dto

import com.devgate.core.models.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UserDto(
	@NotBlank
	@NotNull
	var fullName: String,
	var role: Role = Role.MEMBER,

	@Email
	var email: String,

	@NotNull
	@NotBlank
	var password: String
)