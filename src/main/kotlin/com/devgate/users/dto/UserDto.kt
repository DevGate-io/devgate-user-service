package com.devgate.users.dto

import com.devgate.auth.models.Principal
import com.devgate.users.models.User
import com.devgate.users.models.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UserDto(
	@NotBlank
	@NotNull
	var fullName: String,
	var role: Role = Role.MEMBER,
	@Email
	@NotBlank
	@NotNull
	override var email: String,
	@NotNull
	@NotBlank
	@Size(min = 8)
	override var password: String
) : Principal

fun UserDto.toUser(): User =
	User(
		fullName = this.fullName,
		role = this.role,
		email = this.email,
		hashedPassword = this.password
	)