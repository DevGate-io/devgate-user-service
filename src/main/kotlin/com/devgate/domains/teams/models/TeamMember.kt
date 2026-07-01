package com.devgate.domains.teams.models

import com.devgate.domains.teams.models.enums.TeamRole
import com.devgate.domains.users.models.User
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "team_members")
class TeamMember(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@ManyToOne
	@JoinColumn(name = "team_id", nullable = false)
	var team: Team,
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	var user: User,
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var role: TeamRole = TeamRole.MEMBER,
	@Column(name = "joined_at", nullable = false)
	var joinedAt: Instant = Instant.now()
)