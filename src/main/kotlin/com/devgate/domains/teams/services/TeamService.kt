package com.devgate.domains.teams.services

import com.devgate.domains.teams.dto.*
import java.util.UUID

interface TeamService {
	fun getAllTeams(search: String? = null): List<TeamResponse>

	fun getTeamById(id: UUID): TeamResponse

	fun createTeam(request: CreateTeamRequest): TeamResponse

	fun updateTeam(
		id: UUID,
		request: UpdateTeamRequest
	): TeamResponse

	fun deleteTeam(id: UUID)

	fun getTeamMembers(teamId: UUID): List<TeamMemberResponse>

	fun addMember(
		teamId: UUID,
		request: AddMemberRequest
	): TeamMemberResponse

	fun updateMemberRole(
		teamId: UUID,
		memberId: UUID,
		request: UpdateMemberRoleRequest
	): TeamMemberResponse

	fun removeMember(
		teamId: UUID,
		memberId: UUID
	)
}