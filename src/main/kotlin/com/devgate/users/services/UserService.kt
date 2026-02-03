package com.devgate.users.services

import com.devgate.users.dto.UserDto
import com.devgate.users.models.User
import java.util.*

interface UserService {
	fun updateLastLogin(userId: UUID?): User
	fun getAllUsers(): List<User>
	fun getUserById(id: UUID?): User
	fun deleteUserById(id: UUID?)
	fun createUser(request: UserDto): User
	fun getCurrentUser(): User
	fun updateUser(dto: UserDto): User
}