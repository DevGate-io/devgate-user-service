package com.devgate.auth.models

import com.devgate.users.models.User
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.jetbrains.annotations.NotNull
import java.time.Instant
import java.util.*

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@Column(name = "expires_at")
	@NotNull
	var expiresAt: Instant,
	@Column(name = "token", unique = true)
	var hashedToken: String,
	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	var user: User
)