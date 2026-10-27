package com.devgate.domains.organizations.models

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "organizations")
class Organization(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@Column(nullable = false, unique = true)
	var name: String,
	@Column(nullable = false, length = 1000)
	var description: String = ""
) {
	// Flat list of the whole department tree; the hierarchy lives in Department.parent.
	@OneToMany(mappedBy = "organization", cascade = [CascadeType.ALL], orphanRemoval = true)
	@OrderBy("createdAt ASC, name ASC")
	var departments: MutableList<Department> = mutableListOf()

	@Column(nullable = false)
	var createdAt: Instant = Instant.now()
}