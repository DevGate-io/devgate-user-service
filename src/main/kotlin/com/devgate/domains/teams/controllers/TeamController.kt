package com.devgate.domains.teams.controllers

import com.devgate.domains.teams.dto.*
import com.devgate.domains.teams.services.TeamService
import com.devgate.domains.users.models.Action
import com.devgate.domains.users.models.toTarget
import com.devgate.domains.users.services.AuditLogService
import com.devgate.domains.users.services.UserService
import com.devgate.utils.Logger
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/teams")
class TeamController
	@Autowired
	constructor(
		private val auditLogService: AuditLogService,
		private val teamService: TeamService,
		private val userService: UserService
	) {
	@GetMapping
		fun getAll(
			@RequestParam(required = false) search: String?
		): ResponseEntity<List<TeamResponse>> = ResponseEntity.ok(teamService.getAllTeams(search))

		@GetMapping("/{id}")
		fun getById(
			@PathVariable id: String
		): ResponseEntity<TeamResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(teamService.getTeamById(uuid))
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun create(
			@RequestBody @Valid body: CreateTeamRequest
		): ResponseEntity<TeamResponse> {
			val response = teamService.createTeam(body)
			auditLogService.sendMessage(Action.TEAM_CREATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@PatchMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun update(
			@PathVariable id: String,
			@RequestBody @Valid body: UpdateTeamRequest
		): ResponseEntity<TeamResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val response = teamService.updateTeam(uuid, body)

			auditLogService.sendMessage(Action.TEAM_UPDATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@DeleteMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun delete(
			@PathVariable id: String
		): ResponseEntity<Nothing> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val team = teamService.getTeamById(uuid)

			teamService.deleteTeam(uuid)
			auditLogService.sendMessage(Action.TEAM_DELETED, team.toTarget())

			return ResponseEntity.ok().build()
		}

		@GetMapping("/{id}/members")
		fun getMembers(
			@PathVariable id: String
		): ResponseEntity<List<TeamMemberResponse>> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(teamService.getTeamMembers(uuid))
		}

		@PostMapping("/{id}/members")
		@PreAuthorize("hasRole('ADMIN')")
		fun addMember(
			@PathVariable id: String,
			@RequestBody @Valid body: AddMemberRequest
		): ResponseEntity<TeamMemberResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()

			try {
				val member = userService.getUserById(body.userId)
				auditLogService.sendMessage(Action.TEAM_MEMBER_ADDED, member.toTarget())
			} catch (e: Exception) {
				Logger.error("Failed to log action: $e", this)
			}

			return ResponseEntity.ok(teamService.addMember(uuid, body))
		}

		@PatchMapping("/{id}/members/{memberId}/role")
		@PreAuthorize("hasRole('ADMIN')")
		fun updateMemberRole(
			@PathVariable id: String,
			@PathVariable memberId: String,
			@RequestBody @Valid body: UpdateMemberRoleRequest
		): ResponseEntity<TeamMemberResponse> {
			val teamUuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val memberUuid = parseUuid(memberId) ?: return ResponseEntity.badRequest().build()

			try {
				val member = userService.getUserById(memberUuid)
				auditLogService.sendMessage(Action.TEAM_MEMBER_UPDATED, member.toTarget())
			} catch (e: Exception){
				Logger.error("Failed to log action: $e", this)
			}

			return ResponseEntity.ok(teamService.updateMemberRole(teamUuid, memberUuid, body))
		}

		@DeleteMapping("/{id}/members/{memberId}")
		@PreAuthorize("hasRole('ADMIN')")
		fun removeMember(
			@PathVariable id: String,
			@PathVariable memberId: String
		): ResponseEntity<Nothing> {
			val teamUuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val memberUuid = parseUuid(memberId) ?: return ResponseEntity.badRequest().build()

			val member = userService.getUserById(memberUuid)
			auditLogService.sendMessage(Action.TEAM_MEMBER_REMOVED, member.toTarget())

			teamService.removeMember(teamUuid, memberUuid)

			return ResponseEntity.ok().build()
		}

		private fun parseUuid(id: String): UUID? =
			runCatching { UUID.fromString(id) }
				.onFailure { Logger.error("Invalid UUID: $id", this) }
				.getOrNull()
	}