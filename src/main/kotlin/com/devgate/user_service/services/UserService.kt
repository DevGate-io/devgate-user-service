package com.devgate.user_service.services

import com.devgate.user_service.dto.UserDto
import com.devgate.user_service.models.User
import java.util.*

interface UserService {
	fun updateLastLogin(userId: UUID?): User
	fun getAllUsers(): List<User>
	fun getUserById(id: UUID?): User
	fun deleteUserById(id: UUID?)
	fun createUser(request: UserDto): User
}