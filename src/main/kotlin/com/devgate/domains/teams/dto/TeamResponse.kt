package com.devgate.domains.teams.dto

import com.devgate.domains.teams.models.Team
import com.devgate.domains.teams.models.TeamMember
import com.devgate.domains.users.models.Target
import java.time.Instant
import java.util.UUID

data class TeamResponse(
	val id: UUID?,
	val name: String,
	val description: String,
	val slug: String,
	val createdAt: Instant,
	val members: List<TeamMemberResponse>
)

data class TeamMemberResponse(
	val id: UUID?,
	val userId: UUID,
	val userName: String,
	val userEmail: String,
	val role: String,
	val joinedAt: Instant
)

fun TeamResponse.toTarget() = Target(
	id = id.toString(),
	label = name,
	type = "team"
)

fun Team.toResponse(): TeamResponse =
	TeamResponse(
		id = id,
		name = name,
		description = description,
		slug = slug,
		createdAt = createdAt,
		members = members.map { it.toResponse() }
	)

fun TeamMember.toResponse(): TeamMemberResponse =
	TeamMemberResponse(
		id = id,
		userId = user.id!!,
		userName = user.fullName,
		userEmail = user.email,
		role = role.name,
		joinedAt = joinedAt
	)