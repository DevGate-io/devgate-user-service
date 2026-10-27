package com.devgate.domains.organizations.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

// Top-level departments have depth 1.
const val MAX_DEPARTMENT_DEPTH = 3

@Entity
@Table(name = "departments")
class Department(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	var organization: Organization,
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	var parent: Department? = null,
	@Column(nullable = false)
	var name: String,
	@Column(nullable = false, length = 1000)
	var description: String = ""
) {
	@Column(nullable = false)
	var createdAt: Instant = Instant.now()
}