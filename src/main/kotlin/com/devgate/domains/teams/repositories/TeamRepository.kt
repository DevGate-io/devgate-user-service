package com.devgate.domains.teams.repositories

import com.devgate.domains.teams.models.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TeamRepository : JpaRepository<Team, UUID> {
	fun findBySlug(slug: String): Team?

	fun existsBySlug(slug: String): Boolean
}