package com.devgate.user_service.models

import com.devgate.core.models.enums.Role
import org.hibernate.annotations.UuidGenerator
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.*

@Table(name = "users")
@Entity
data class User(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private val id: UUID,

	@Column(name = "full_name")
	@NotBlank
	@NotNull
	private val fullName: String,

	@Column(name = "last_login")
	@JsonSerialize(using = LocalDateTimeSerializer::class)
	private val lastLogin: LocalDateTime,

	@Enumerated(EnumType.STRING)
	@NotNull
	private val role: Role = Role.MEMBER,

	@NotNull
	@NotBlank
	@Email
	private val email: String,

	@NotNull
	@NotBlank
	@Size(min = 8)
	private val password: String
)