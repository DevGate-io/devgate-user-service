package com.devgate.domains.users.services.impl

import com.devgate.domains.users.dto.UserDto
import com.devgate.domains.users.dto.toUser
import com.devgate.domains.users.models.User
import com.devgate.domains.users.models.enums.Role
import com.devgate.domains.users.repositories.UserRepository
import com.devgate.domains.users.services.UserService
import com.devgate.exceptions.UserAlreadyExistsException
import com.devgate.exceptions.UserNotFoundException
import com.devgate.utils.PasswordEncoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@Service
class UserServiceImpl
	@Autowired
	constructor(
		private val userRepository: UserRepository,
		private val passwordEncoder: PasswordEncoder
	) : UserService {
	private fun getHashedPassword(rawPassword: CharSequence): String =
		passwordEncoder.encode(rawPassword) ?: throw ResponseStatusException(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"Failed to encode password"
		)

	@Transactional
	override fun updateLastLogin(userId: UUID?): User {
		val user: User = getUserById(userId)
		user.lastLogin = Instant.now()

		return userRepository.save(user)
	}

	override fun getAllUsers(search: String?): List<User> {
		val all = userRepository.findAll()
		val query = search?.trim()?.lowercase().orEmpty()

		if (query.isEmpty()) return all

		return all.filter { user ->
			user.fullName.lowercase().contains(query) ||
				user.email.lowercase().contains(query)
		}
	}

	@Transactional
	override fun updateUserRole(
		id: UUID,
		role: Role
	): User {
		val user = userRepository.findById(id).orElseThrow { UserNotFoundException() }
		user.role = role
		return userRepository.save(user)
	}

	override fun getUserById(id: UUID?): User {
		if (id == null) {
			throw UserNotFoundException()
		}

		return userRepository
			.findById(id)
			.orElseThrow { UserNotFoundException() }
	}

	@Transactional
	override fun deleteUserById(id: UUID?) {
		if (id == null || !userRepository.existsById(id)) {
			throw UserNotFoundException()
		}

		return userRepository.deleteById(id)
	}

	@Transactional
	override fun createUser(request: UserDto): User {
		if (userRepository.existsByEmail(request.email)) {
			throw UserAlreadyExistsException()
		}

		val hashedPassword = getHashedPassword(request.password)

		val user =
			User(
				fullName = request.fullName,
				role = request.role,
				hashedPassword = hashedPassword,
				email = request.email
			)

		return userRepository.save(user)
	}

	override fun getCurrentUser(): User {
		val securityContext = SecurityContextHolder.getContext()
		val authentication =
			securityContext.authentication
				?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized")

		if (!authentication.isAuthenticated) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized")
		}

		val email = authentication.name

		return userRepository.findByEmail(email)
			?: throw UserNotFoundException()
	}

	@Transactional
	override fun updateUser(dto: UserDto): User {
		val oldUser: User = userRepository.findByEmail(dto.email) ?: throw UserNotFoundException()

		val updatedUser =
			dto.toUser().apply {
				hashedPassword = getHashedPassword(dto.password)
				id = oldUser.id
				lastLogin = oldUser.lastLogin
			}

		return userRepository.save(updatedUser)
	}
}