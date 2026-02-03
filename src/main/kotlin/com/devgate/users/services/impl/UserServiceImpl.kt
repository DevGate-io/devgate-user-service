package com.devgate.users.services.impl

import com.devgate.exceptions.UserAlreadyExistsException
import com.devgate.exceptions.UserNotFoundException
import com.devgate.users.dto.UserDto
import com.devgate.users.dto.toUser
import com.devgate.users.models.User
import com.devgate.users.repositories.UserRepository
import com.devgate.users.services.UserService
import com.devgate.utils.PasswordEncoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@Service
class UserServiceImpl(
	@Autowired
	private val userRepository: UserRepository,

	@Autowired
	private val passwordEncoder: PasswordEncoder
) : UserService {

	private fun getHashedPassword(rawPassword: CharSequence): String {
		return passwordEncoder.encode(rawPassword) ?: throw ResponseStatusException(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"Failed to encode password"
		)
	}

	override fun updateLastLogin(userId: UUID?): User {
		val user: User = getUserById(userId)
		user.lastLogin = Instant.now()

		return userRepository.save(user)
	}

	override fun getAllUsers(): List<User> {
		return userRepository.findAll()
	}

	override fun getUserById(id: UUID?): User {
		if (id == null) {
			throw UserNotFoundException()
		}

		return userRepository.findById(id)
			.orElseThrow { UserNotFoundException() }
	}

	override fun deleteUserById(id: UUID?) {
		if (id == null || !userRepository.existsById(id)) {
			throw UserNotFoundException()
		}

		return userRepository.deleteById(id)
	}

	override fun createUser(request: UserDto): User {
		if (userRepository.existsByEmail(request.email)) {
			throw UserAlreadyExistsException()
		}

		val hashedPassword = getHashedPassword(request.password)

		val user = User(
			fullName = request.fullName,
			role = request.role,
			hashedPassword = hashedPassword,
			email = request.email,
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

	override fun updateUser(dto: UserDto): User {
		val oldUser: User = userRepository.findByEmail(dto.email) ?: throw UserNotFoundException()

		val updatedUser = dto.toUser().apply {
			hashedPassword = getHashedPassword(dto.password)
			id = oldUser.id
			lastLogin = oldUser.lastLogin
		}

		return userRepository.save(updatedUser)
	}
}