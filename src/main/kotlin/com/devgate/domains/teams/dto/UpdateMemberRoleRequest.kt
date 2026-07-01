package com.devgate.domains.teams.dto

import com.devgate.domains.teams.models.enums.TeamRole
import jakarta.validation.constraints.NotNull

data class UpdateMemberRoleRequest(
	@NotNull
	val role: TeamRole
)