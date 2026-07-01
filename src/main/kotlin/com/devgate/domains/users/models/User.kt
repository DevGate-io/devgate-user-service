package com.devgate.domains.users.models

import com.devgate.users.models.enums.Role
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.UuidGenerator
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.InstantSerializer
import java.io.Serializable
import java.time.Instant
import java.util.*

@Table(name = "users")
@Entity
class User(
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
	@NotBlank
	@NotNull
	@Column(nullable = false, unique = true)
	var email: String,
	@NotNull
	@NotBlank
	@Size(min = 8)
	@Column("hashed_password")
	var hashedPassword: String
) : UserDetails,
	Serializable {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as User

		if (id != other.id) return false
		if (fullName != other.fullName) return false
		if (lastLogin != other.lastLogin) return false
		if (role != other.role) return false
		if (email != other.email) return false
		if (hashedPassword != other.hashedPassword) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id?.hashCode() ?: 0
		result = 31 * result + fullName.hashCode()
		result = 31 * result + (lastLogin?.hashCode() ?: 0)
		result = 31 * result + role.hashCode()
		result = 31 * result + email.hashCode()
		result = 31 * result + hashedPassword.hashCode()
		return result
	}

	override fun toString(): String =
		"User(id=$id, fullName='$fullName', lastLogin=$lastLogin, role=$role, email='$email', hashedPassword='$hashedPassword')"

	override fun getAuthorities(): Collection<GrantedAuthority> = listOf(this.role)

	@JsonIgnore
	override fun getPassword(): String = this.hashedPassword

	@JsonIgnore
	override fun getUsername(): String = this.email
}

fun User.copy(
	id: UUID? = this.id,
	fullName: String = this.fullName,
	lastLogin: Instant? = this.lastLogin,
	role: Role = this.role,
	email: String = this.email,
	hashedPassword: String = this.hashedPassword
): User =
	User(
		id = id,
		fullName = fullName,
		lastLogin = lastLogin,
		role = role,
		email = email,
		hashedPassword = hashedPassword
	)