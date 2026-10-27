package com.devgate.domains.organizations.repositories

import com.devgate.domains.organizations.models.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationRepository : JpaRepository<Organization, UUID> {
	fun existsByNameIgnoreCase(name: String): Boolean
}