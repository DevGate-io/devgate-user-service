package com.devgate.user_service.services.impl

import com.devgate.user_service.dto.UserDto
import com.devgate.user_service.exceptions.UserAlreadyExistsException
import com.devgate.user_service.exceptions.UserNotFoundException
import com.devgate.user_service.models.User
import com.devgate.user_service.repositories.UserRepository
import com.devgate.user_service.services.UserService
import com.devgate.user_service.utils.PasswordEncoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserServiceImpl(
	@Autowired
	private val userRepository: UserRepository,

	@Autowired
	private val passwordEncoder: PasswordEncoder
) : UserService {

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

		return userRepository.findById(id).orElseThrow { UserNotFoundException() }
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

		val hashedPassword = passwordEncoder.encodePassword(request.password)

		val user = User(
			fullName = request.fullName,
			role = request.role,
			hashedPassword = hashedPassword,
			email = request.email,
		)

		return userRepository.save(user)
	}
}