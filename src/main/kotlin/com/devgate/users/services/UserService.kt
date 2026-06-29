package com.devgate.users.services

import com.devgate.users.dto.UserDto
import com.devgate.users.models.User
import com.devgate.users.models.enums.Role
import java.util.*

interface UserService {
	fun updateLastLogin(userId: UUID?): User

	fun getAllUsers(search: String? = null): List<User>

	fun getUserById(id: UUID?): User

	fun deleteUserById(id: UUID?)

	fun createUser(request: UserDto): User

	fun getCurrentUser(): User

	fun updateUser(dto: UserDto): User

	fun updateUserRole(
		id: UUID,
		role: Role
	): User
}