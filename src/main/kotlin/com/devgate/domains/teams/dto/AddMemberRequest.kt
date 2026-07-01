package com.devgate.domains.teams.dto

import com.devgate.domains.teams.models.enums.TeamRole
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AddMemberRequest(
	@NotNull
	val userId: UUID,
	val role: TeamRole = TeamRole.MEMBER
)