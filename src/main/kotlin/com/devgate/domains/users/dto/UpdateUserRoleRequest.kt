package com.devgate.domains.users.dto

import com.devgate.users.models.enums.Role
import jakarta.validation.constraints.NotNull

data class UpdateUserRoleRequest(
	@NotNull
	var role: Role
)