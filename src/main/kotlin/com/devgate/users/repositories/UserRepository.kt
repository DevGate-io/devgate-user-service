package com.devgate.users.repositories

import com.devgate.users.models.User
import com.devgate.users.models.enums.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {
	fun findByEmail(email: String): User?
	fun findByFullName(fullName: String): User?
	fun findByRole(role: Role): List<User>
	fun existsByEmail(email: String): Boolean
}