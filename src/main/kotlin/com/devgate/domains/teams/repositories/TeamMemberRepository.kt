package com.devgate.domains.teams.repositories

import com.devgate.domains.teams.models.TeamMember
import com.devgate.domains.teams.models.enums.TeamRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TeamMemberRepository : JpaRepository<TeamMember, UUID> {
	fun findAllByTeamId(teamId: UUID): List<TeamMember>

	fun findByTeamIdAndUserId(
		teamId: UUID,
		userId: UUID
	): TeamMember?

	fun existsByTeamIdAndUserId(
		teamId: UUID,
		userId: UUID
	): Boolean

	fun countByTeamIdAndRole(
		teamId: UUID,
		role: TeamRole
	): Long
}