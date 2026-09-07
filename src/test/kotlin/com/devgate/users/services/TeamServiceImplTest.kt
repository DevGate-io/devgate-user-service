package com.devgate.users.services

import com.devgate.domains.teams.dto.AddMemberRequest
import com.devgate.domains.teams.dto.CreateTeamRequest
import com.devgate.domains.teams.dto.UpdateMemberRoleRequest
import com.devgate.domains.teams.dto.UpdateTeamRequest
import com.devgate.domains.teams.models.Team
import com.devgate.domains.teams.models.TeamMember
import com.devgate.domains.teams.models.enums.TeamRole
import com.devgate.domains.teams.repositories.TeamMemberRepository
import com.devgate.domains.teams.repositories.TeamRepository
import com.devgate.domains.teams.services.impl.TeamServiceImpl
import com.devgate.domains.users.models.User
import com.devgate.domains.users.models.enums.Role
import com.devgate.domains.users.repositories.UserRepository
import com.devgate.exceptions.TeamAlreadyExistsException
import com.devgate.exceptions.TeamMemberNotFoundException
import com.devgate.exceptions.TeamNotFoundException
import com.devgate.exceptions.UserNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class TeamServiceImplTest {
	private val fakeUser =
		User(
			id = UUID.randomUUID(),
			fullName = "Fake User 1",
			email = "test@test.com",
			role = Role.MEMBER,
			lastLogin = Instant.now(),
			hashedPassword = "hashed password"
		)

	private val fakeTeam =
		Team(
			id = UUID.randomUUID(),
			name = "Test Team",
			slug = "test-team",
			description = "A test team"
		)

	private fun createFakeMember(
		team: Team = fakeTeam,
		user: User = fakeUser,
		role: TeamRole = TeamRole.MEMBER
	): TeamMember =
		TeamMember(
			id = UUID.randomUUID(),
			team = team,
			user = user,
			role = role
		)

	private fun createTeam(
		id: UUID = UUID.randomUUID(),
		name: String = "Team",
		slug: String = "team",
		description: String = ""
	): Team =
		Team(
			id = id,
			name = name,
			slug = slug,
			description = description
		)

	@Mock
	private lateinit var teamRepository: TeamRepository

	@Mock
	private lateinit var teamMemberRepository: TeamMemberRepository

	@Mock
	private lateinit var userRepository: UserRepository

	@InjectMocks
	private lateinit var teamService: TeamServiceImpl

	@Test
	fun `getAllTeams - positive case without search`() {
		val team1 = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		team1.id = fakeTeam.id
		val team2 = createTeam(name = "Another Team", slug = "another-team")
		`when`(teamRepository.findAll()).thenReturn(listOf(team1, team2))

		val result = teamService.getAllTeams(null)

		assertEquals(2, result.size)
		assertEquals("Test Team", result[0].name)
		assertEquals("Another Team", result[1].name)
	}

	@Test
	fun `getAllTeams - positive case with search filter`() {
		val team1 = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		team1.id = fakeTeam.id
		val team2 = createTeam(name = "DevOps Squad", slug = "devops-squad")
		`when`(teamRepository.findAll()).thenReturn(listOf(team1, team2))

		val result = teamService.getAllTeams("devops")

		assertEquals(1, result.size)
		assertEquals("DevOps Squad", result[0].name)
	}

	@Test
	fun `getAllTeams - search by slug`() {
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		`when`(teamRepository.findAll()).thenReturn(listOf(team))

		val result = teamService.getAllTeams("test-team")

		assertEquals(1, result.size)
	}

	@Test
	fun `getAllTeams - search by description`() {
		val team = createTeam(name = "Backend", slug = "backend", description = "Backend infrastructure team")
		`when`(teamRepository.findAll()).thenReturn(listOf(team))

		val result = teamService.getAllTeams("infrastructure")

		assertEquals(1, result.size)
		assertEquals("Backend infrastructure team", result[0].description)
	}

	@Test
	fun `getAllTeams - empty result`() {
		`when`(teamRepository.findAll()).thenReturn(emptyList())

		val result = teamService.getAllTeams("nonexistent")

		assertEquals(0, result.size)
	}

	@Test
	fun `getAllTeams - search is case insensitive`() {
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		`when`(teamRepository.findAll()).thenReturn(listOf(team))

		val result = teamService.getAllTeams("TEST")

		assertEquals(1, result.size)
	}

	@Test
	fun `getAllTeams - search trims whitespace`() {
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		`when`(teamRepository.findAll()).thenReturn(listOf(team))

		val result = teamService.getAllTeams("  test-team  ")

		assertEquals(1, result.size)
	}

	@Test
	fun `getTeamById - positive case`() {
		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))

		val result = teamService.getTeamById(fakeTeam.id!!)

		assertEquals(fakeTeam.name, result.name)
		assertEquals(fakeTeam.slug, result.slug)
	}

	@Test
	fun `getTeamById - team not found`() {
		val teamId = UUID.randomUUID()
		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.getTeamById(teamId)
		}
	}

	@Test
	fun `createTeam - positive case`() {
		val request = CreateTeamRequest(name = "New Team", slug = "new-team", description = "desc")
		val savedTeam = createTeam(name = request.name, slug = request.slug, description = request.description ?: "")
		savedTeam.id = UUID.randomUUID()
		val owner = createFakeMember(team = savedTeam, role = TeamRole.OWNER)
		savedTeam.members.add(owner)

		val authentication =
			UsernamePasswordAuthenticationToken(fakeUser.email, null, fakeUser.authorities)
		SecurityContextHolder.getContext().authentication = authentication

		`when`(teamRepository.existsBySlug(request.slug)).thenReturn(false)
		`when`(userRepository.findByEmail(fakeUser.email)).thenReturn(fakeUser)
		`when`(teamRepository.save(any(Team::class.java))).thenReturn(savedTeam)

		val result = teamService.createTeam(request)

		assertEquals("New Team", result.name)
		assertEquals("new-team", result.slug)
		assertEquals(1, result.members.size)
		assertEquals("OWNER", result.members[0].role)

		SecurityContextHolder.clearContext()
	}

	@Test
	fun `createTeam - slug already exists`() {
		val request = CreateTeamRequest(name = "New Team", slug = "existing-slug")
		`when`(teamRepository.existsBySlug(request.slug)).thenReturn(true)

		assertThrows(TeamAlreadyExistsException::class.java) {
			teamService.createTeam(request)
		}
	}

	@Test
	fun `createTeam - not authenticated`() {
		val request = CreateTeamRequest(name = "New Team", slug = "new-team")
		`when`(teamRepository.existsBySlug(request.slug)).thenReturn(false)

		assertThrows(ResponseStatusException::class.java) {
			teamService.createTeam(request)
		}
	}

	@Test
	fun `updateTeam - positive case all fields`() {
		val request = UpdateTeamRequest(name = "Updated", slug = "updated-slug", description = "new desc")
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		team.id = fakeTeam.id
		val savedTeam = createTeam(name = "Updated", slug = "updated-slug", description = "new desc")
		savedTeam.id = fakeTeam.id

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(team))
		`when`(teamRepository.existsBySlug("updated-slug")).thenReturn(false)
		`when`(teamRepository.save(any(Team::class.java))).thenReturn(savedTeam)

		val result = teamService.updateTeam(fakeTeam.id!!, request)

		assertEquals("Updated", result.name)
		assertEquals("updated-slug", result.slug)
		assertEquals("new desc", result.description)
	}

	@Test
	fun `updateTeam - partial update only name`() {
		val request = UpdateTeamRequest(name = "Renamed")
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		team.id = fakeTeam.id
		val savedTeam = createTeam(name = "Renamed", slug = "test-team", description = "A test team")
		savedTeam.id = fakeTeam.id

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(team))
		`when`(teamRepository.save(any(Team::class.java))).thenReturn(savedTeam)

		val result = teamService.updateTeam(fakeTeam.id!!, request)

		assertEquals("Renamed", result.name)
		assertEquals("test-team", result.slug)
		assertEquals("A test team", result.description)
	}

	@Test
	fun `updateTeam - slug already exists`() {
		val request = UpdateTeamRequest(slug = "taken-slug")
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		team.id = fakeTeam.id

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(team))
		`when`(teamRepository.existsBySlug("taken-slug")).thenReturn(true)

		assertThrows(TeamAlreadyExistsException::class.java) {
			teamService.updateTeam(fakeTeam.id!!, request)
		}
	}

	@Test
	fun `updateTeam - same slug does not conflict`() {
		val request = UpdateTeamRequest(slug = fakeTeam.slug)
		val team = createTeam(name = "Test Team", slug = "test-team", description = "A test team")
		team.id = fakeTeam.id

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(team))
		`when`(teamRepository.save(any(Team::class.java))).thenReturn(team)

		val result = teamService.updateTeam(fakeTeam.id!!, request)

		assertEquals(fakeTeam.slug, result.slug)
		verify(teamRepository, never()).existsBySlug(fakeTeam.slug)
	}

	@Test
	fun `updateTeam - team not found`() {
		val teamId = UUID.randomUUID()
		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.updateTeam(teamId, UpdateTeamRequest(name = "X"))
		}
	}

	@Test
	fun `deleteTeam - positive case`() {
		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))

		teamService.deleteTeam(fakeTeam.id!!)

		verify(teamRepository).delete(fakeTeam)
	}

	@Test
	fun `deleteTeam - team not found`() {
		val teamId = UUID.randomUUID()
		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.deleteTeam(teamId)
		}
	}

	@Test
	fun `getTeamMembers - positive case`() {
		val member = createFakeMember()
		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findAllByTeamId(fakeTeam.id!!)).thenReturn(listOf(member))

		val result = teamService.getTeamMembers(fakeTeam.id!!)

		assertEquals(1, result.size)
		assertEquals(fakeUser.fullName, result[0].userName)
		assertEquals("MEMBER", result[0].role)
	}

	@Test
	fun `getTeamMembers - team not found`() {
		val teamId = UUID.randomUUID()
		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.getTeamMembers(teamId)
		}
	}

	@Test
	fun `getTeamMembers - empty list`() {
		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findAllByTeamId(fakeTeam.id!!)).thenReturn(emptyList())

		val result = teamService.getTeamMembers(fakeTeam.id!!)

		assertEquals(0, result.size)
	}

	@Test
	fun `addMember - positive case`() {
		val request = AddMemberRequest(userId = fakeUser.id!!, role = TeamRole.MEMBER)
		val member = createFakeMember()

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.existsByTeamIdAndUserId(fakeTeam.id!!, fakeUser.id!!)).thenReturn(false)
		`when`(userRepository.findById(fakeUser.id!!)).thenReturn(Optional.of(fakeUser))
		`when`(teamMemberRepository.save(any(TeamMember::class.java))).thenReturn(member)

		val result = teamService.addMember(fakeTeam.id!!, request)

		assertEquals(fakeUser.fullName, result.userName)
		assertEquals("MEMBER", result.role)
	}

	@Test
	fun `addMember - team not found`() {
		val teamId = UUID.randomUUID()
		val request = AddMemberRequest(userId = fakeUser.id!!)

		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.addMember(teamId, request)
		}
	}

	@Test
	fun `addMember - user already member`() {
		val request = AddMemberRequest(userId = fakeUser.id!!)

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.existsByTeamIdAndUserId(fakeTeam.id!!, fakeUser.id!!)).thenReturn(true)

		assertThrows(TeamAlreadyExistsException::class.java) {
			teamService.addMember(fakeTeam.id!!, request)
		}
	}

	@Test
	fun `addMember - user not found`() {
		val request = AddMemberRequest(userId = fakeUser.id!!)

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.existsByTeamIdAndUserId(fakeTeam.id!!, fakeUser.id!!)).thenReturn(false)
		`when`(userRepository.findById(fakeUser.id!!)).thenReturn(Optional.empty())

		assertThrows(UserNotFoundException::class.java) {
			teamService.addMember(fakeTeam.id!!, request)
		}
	}

	@Test
	fun `updateMemberRole - positive case`() {
		val member = createFakeMember(role = TeamRole.MEMBER)
		val request = UpdateMemberRoleRequest(role = TeamRole.MAINTAINER)
		val updatedMember = createFakeMember(role = TeamRole.MAINTAINER)

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findById(member.id!!)).thenReturn(Optional.of(member))
		`when`(teamMemberRepository.save(any(TeamMember::class.java))).thenReturn(updatedMember)

		val result = teamService.updateMemberRole(fakeTeam.id!!, member.id!!, request)

		assertEquals("MAINTAINER", result.role)
	}

	@Test
	fun `updateMemberRole - team not found`() {
		val teamId = UUID.randomUUID()
		val memberId = UUID.randomUUID()
		val request = UpdateMemberRoleRequest(role = TeamRole.MAINTAINER)

		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.updateMemberRole(teamId, memberId, request)
		}
	}

	@Test
	fun `updateMemberRole - member not found`() {
		val memberId = UUID.randomUUID()
		val request = UpdateMemberRoleRequest(role = TeamRole.MAINTAINER)

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findById(memberId)).thenReturn(Optional.empty())

		assertThrows(TeamMemberNotFoundException::class.java) {
			teamService.updateMemberRole(fakeTeam.id!!, memberId, request)
		}
	}

	@Test
	fun `updateMemberRole - member belongs to different team`() {
		val otherTeam = createTeam(name = "Other", slug = "other")
		val member = createFakeMember(team = otherTeam)
		val request = UpdateMemberRoleRequest(role = TeamRole.MAINTAINER)

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findById(member.id!!)).thenReturn(Optional.of(member))

		assertThrows(TeamMemberNotFoundException::class.java) {
			teamService.updateMemberRole(fakeTeam.id!!, member.id!!, request)
		}
	}

	@Test
	fun `removeMember - positive case`() {
		val member = createFakeMember()

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findById(member.id!!)).thenReturn(Optional.of(member))

		teamService.removeMember(fakeTeam.id!!, member.id!!)

		verify(teamMemberRepository).delete(member)
	}

	@Test
	fun `removeMember - team not found`() {
		val teamId = UUID.randomUUID()
		val memberId = UUID.randomUUID()

		`when`(teamRepository.findById(teamId)).thenReturn(Optional.empty())

		assertThrows(TeamNotFoundException::class.java) {
			teamService.removeMember(teamId, memberId)
		}
	}

	@Test
	fun `removeMember - member not found`() {
		val memberId = UUID.randomUUID()

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findById(memberId)).thenReturn(Optional.empty())

		assertThrows(TeamMemberNotFoundException::class.java) {
			teamService.removeMember(fakeTeam.id!!, memberId)
		}
	}

	@Test
	fun `removeMember - member belongs to different team`() {
		val otherTeam = createTeam(name = "Other", slug = "other")
		val member = createFakeMember(team = otherTeam)

		`when`(teamRepository.findById(fakeTeam.id!!)).thenReturn(Optional.of(fakeTeam))
		`when`(teamMemberRepository.findById(member.id!!)).thenReturn(Optional.of(member))

		assertThrows(TeamMemberNotFoundException::class.java) {
			teamService.removeMember(fakeTeam.id!!, member.id!!)
		}
	}
}