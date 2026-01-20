package com.devgate.user_service.model

import com.devgate.core.models.enums.Role
import org.hibernate.annotations.UuidGenerator
import jakarta.persistence.*
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
	private val fullName: String,

	@Column(name = "last_login")
	@JsonSerialize(using = LocalDateTimeSerializer::class)
	private val lastLogin: LocalDateTime,

	@Enumerated(EnumType.STRING)
	private val role: Role,

	@NotNull
	private val email: String
)