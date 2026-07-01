package com.devgate.domains.teams.models

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID
import com.devgate.domains.users.models.Target

@Entity
@Table(name = "teams")
class Team(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,

	@Column(nullable = false)
	var name: String,

	var description: String = "",

	@Column(nullable = false, unique = true)
	var slug: String
) {
	@OneToMany(mappedBy = "team", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
	var members: MutableList<TeamMember> = mutableListOf()

	@Column(nullable = false)
	var createdAt: Instant = Instant.now()
}

fun Team.toTarget(): Target = Target(
		type = "team",
		id = id.toString(),
		label = name
	)