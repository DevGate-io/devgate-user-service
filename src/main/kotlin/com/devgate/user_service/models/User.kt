package com.devgate.user_service.models

import com.devgate.core.models.enums.Role
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.UuidGenerator
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.InstantSerializer
import java.time.Instant
import java.util.*

@Table(name = "users")
@Entity
data class User(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,

	@Column(name = "full_name")
	@NotBlank
	@NotNull
	var fullName: String,

	@Column(name = "last_login")
	@JsonSerialize(using = InstantSerializer::class)
	var lastLogin: Instant? = null,

	@Enumerated(EnumType.STRING)
	@NotNull
	var role: Role = Role.MEMBER,

	@Email
	val email: String,

	@NotNull
	@NotBlank
	@Size(min = 8)
	@Column("hashed_password")
	var hashedPassword: String
)