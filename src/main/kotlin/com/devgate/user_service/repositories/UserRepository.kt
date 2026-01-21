package com.devgate.user_service.repositories

import com.devgate.core.models.enums.Role
import com.devgate.user_service.models.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository: JpaRepository<User, UUID> {
	fun findByEmail(email: String): User?
	fun findByFullName(fullName: String): User?
	fun findByRole(role: Role): List<User>
	fun existsByEmail(email: String): Boolean
}