package com.devgate.domains.teams.services.impl

import com.devgate.domains.teams.dto.*
import com.devgate.domains.teams.models.Team
import com.devgate.domains.teams.models.TeamMember
import com.devgate.domains.teams.models.enums.TeamRole
import com.devgate.domains.teams.repositories.TeamMemberRepository
import com.devgate.domains.teams.repositories.TeamRepository
import com.devgate.domains.teams.services.TeamService
import com.devgate.domains.users.repositories.UserRepository
import com.devgate.exceptions.TeamAlreadyExistsException
import com.devgate.exceptions.TeamMemberNotFoundException
import com.devgate.exceptions.TeamNotFoundException
import com.devgate.exceptions.UserNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class TeamServiceImpl
	@Autowired
	constructor(
		private val teamRepository: TeamRepository,
		private val teamMemberRepository: TeamMemberRepository,
		private val userRepository: UserRepository
	) : TeamService {
		@Transactional(readOnly = true)
		override fun getAllTeams(search: String?): List<TeamResponse> {
			val all = teamRepository.findAll()
			val query = search?.trim()?.lowercase().orEmpty()

			if (query.isEmpty()) return all.map { it.toResponse() }

			return all
				.filter {
					it.name.lowercase().contains(query) ||
						it.slug.lowercase().contains(query) ||
						it.description.lowercase().contains(query)
				}
				.map { it.toResponse() }
		}

		@Transactional(readOnly = true)
		override fun getTeamById(id: UUID): TeamResponse {
			val team = findTeam(id)
			return team.toResponse()
		}

		@Transactional
		override fun createTeam(request: CreateTeamRequest): TeamResponse {
			if (teamRepository.existsBySlug(request.slug)) {
				throw TeamAlreadyExistsException()
			}

			val user = getCurrentUser()

			val team =
				Team(
					name = request.name,
					slug = request.slug,
					description = request.description ?: ""
				)

			val owner =
				TeamMember(
					team = team,
					user = user,
					role = TeamRole.OWNER
				)
			team.members.add(owner)

			val saved = teamRepository.save(team)
			return saved.toResponse()
		}

		@Transactional
		override fun updateTeam(
			id: UUID,
			request: UpdateTeamRequest
		): TeamResponse {
			val team = findTeam(id)

			request.slug?.let { slug ->
				if (slug != team.slug && teamRepository.existsBySlug(slug)) {
					throw TeamAlreadyExistsException()
				}
				team.slug = slug
			}
			request.name?.let { team.name = it }
			request.description?.let { team.description = it }

			val saved = teamRepository.save(team)
			return saved.toResponse()
		}

		@Transactional
		override fun deleteTeam(id: UUID) {
			val team = findTeam(id)
			teamRepository.delete(team)
		}

		@Transactional(readOnly = true)
		override fun getTeamMembers(teamId: UUID): List<TeamMemberResponse> {
			findTeam(teamId)
			return teamMemberRepository.findAllByTeamId(teamId).map { it.toResponse() }
		}

		@Transactional
		override fun addMember(
			teamId: UUID,
			request: AddMemberRequest
		): TeamMemberResponse {
			val team = findTeam(teamId)

			if (teamMemberRepository.existsByTeamIdAndUserId(teamId, request.userId)) {
				throw TeamAlreadyExistsException()
			}

			val user = userRepository.findById(request.userId).orElseThrow { UserNotFoundException() }

			val member =
				TeamMember(
					team = team,
					user = user,
					role = request.role
				)
			team.members.add(member)

			val saved = teamMemberRepository.save(member)
			return saved.toResponse()
		}

		@Transactional
		override fun updateMemberRole(
			teamId: UUID,
			memberId: UUID,
			request: UpdateMemberRoleRequest
		): TeamMemberResponse {
			findTeam(teamId)

			val member = findTeamMember(memberId)

			if (member.team.id != teamId) {
				throw TeamMemberNotFoundException()
			}

			member.role = request.role
			val saved = teamMemberRepository.save(member)
			return saved.toResponse()
		}

		@Transactional
		override fun removeMember(
			teamId: UUID,
			memberId: UUID
		) {
			findTeam(teamId)

			val member = findTeamMember(memberId)

			if (member.team.id != teamId) {
				throw TeamMemberNotFoundException()
			}

			teamMemberRepository.delete(member)
		}

		private fun findTeam(id: UUID): Team = teamRepository.findById(id).orElseThrow { TeamNotFoundException() }

		private fun findTeamMember(id: UUID): TeamMember =
			teamMemberRepository.findById(id).orElseThrow { TeamMemberNotFoundException() }

		private fun getCurrentUser(): com.devgate.domains.users.models.User {
			val authentication =
				SecurityContextHolder.getContext().authentication
					?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized")

			val email = authentication.name

			return userRepository.findByEmail(email)
				?: throw UserNotFoundException()
		}
	}