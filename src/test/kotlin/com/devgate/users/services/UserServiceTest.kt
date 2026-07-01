package com.devgate.domains.users.services

import com.devgate.domains.users.dto.UserDto
import com.devgate.domains.users.models.User
import com.devgate.domains.users.models.copy
import com.devgate.domains.users.models.enums.Role
import com.devgate.domains.users.repositories.UserRepository
import com.devgate.domains.users.services.impl.UserServiceImpl
import com.devgate.exceptions.UserAlreadyExistsException
import com.devgate.exceptions.UserNotFoundException
import com.devgate.utils.PasswordEncoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class UserServiceTest {
	private val userDto =
		UserDto(
			fullName = "Fake User 1",
			email = "test@test.com",
			role = Role.MEMBER,
			password = "this is password"
		)

	private val fakeUser =
		User(
			id = UUID.randomUUID(),
			fullName = "Fake User 1",
			email = "test@test.com",
			role = Role.MEMBER,
			lastLogin = Instant.now(),
			hashedPassword = "this is hashed password"
		)

	@Mock
	private lateinit var userRepository: UserRepository

	@Spy
	private lateinit var passwordEncoder: PasswordEncoder

	@InjectMocks
	private lateinit var userService: UserServiceImpl

	@Test
	fun `updateLastLogin - positive case`() {
		`when`(userRepository.findById(fakeUser.id!!)).thenReturn(Optional.of(fakeUser))
		`when`(userRepository.save(fakeUser)).thenReturn(fakeUser)

		val result = userService.updateLastLogin(fakeUser.id)

		assertEquals(fakeUser, result)
		verify(userRepository).save(fakeUser)
	}

	@Test
	fun `getAllUsers - positive case`() {
		val users = listOf(fakeUser, fakeUser.copy(id = UUID.randomUUID(), fullName = "Fake User 2"))
		`when`(userRepository.findAll()).thenReturn(users)

		val result = userService.getAllUsers()

		assertEquals(users, result)
	}

	@Test
	fun `getUserById - positive case`() {
		`when`(userRepository.findById(fakeUser.id!!)).thenReturn(Optional.of(fakeUser))

		val result = userService.getUserById(fakeUser.id)

		assertEquals(fakeUser, result)
	}

	@Test
	fun `updateLastLogin - negative case`() {
		val userId = UUID.randomUUID()
		`when`(userRepository.findById(userId)).thenReturn(Optional.empty())

		assertThrows(UserNotFoundException::class.java) {
			userService.updateLastLogin(userId)
		}
	}

	@Test
	fun `getUserById - negative case`() {
		val userId = UUID.randomUUID()
		`when`(userRepository.findById(userId)).thenReturn(Optional.empty())

		assertThrows(UserNotFoundException::class.java) {
			userService.getUserById(userId)
		}
	}

	@Test
	fun `deleteUserById - positive case`() {
		val userId = UUID.randomUUID()
		`when`(userRepository.existsById(userId)).thenReturn(true)

		userService.deleteUserById(userId)

		verify(userRepository).deleteById(userId)
	}

	@Test
	fun `deleteUserById - negative case`() {
		val userId = UUID.randomUUID()
		`when`(userRepository.existsById(userId)).thenReturn(false)

		assertThrows(UserNotFoundException::class.java) {
			userService.deleteUserById(userId)
		}
	}

	@Test
	fun `createUser - positive case`() {
		`when`(userRepository.existsByEmail(userDto.email))
			.thenReturn(false)

		`when`(passwordEncoder.encode(anyString()))
			.thenReturn("this is hashed password")

		`when`(userRepository.save(any(User::class.java)))
			.thenAnswer { it.arguments[0] as User }

		val result = userService.createUser(userDto)

		assertEquals("Fake User 1", result.fullName)
		assertEquals("test@test.com", result.email)
		assertEquals("this is hashed password", result.hashedPassword)

		verify(userRepository).save(any(User::class.java))
	}

	@Test
	fun `createUser - already exists`() {
		`when`(userRepository.existsByEmail(userDto.email)).thenReturn(true)

		assertThrows(UserAlreadyExistsException::class.java) {
			userService.createUser(userDto)
		}
	}

	@Test
	@WithMockUser(username = "test@test.com")
	fun `getCurrentUser - positive`() {
		`when`(userRepository.findByEmail(fakeUser.email)).thenReturn(fakeUser)

		val authentication =
			UsernamePasswordAuthenticationToken(
				fakeUser.email,
				null,
				fakeUser.authorities
			)

		SecurityContextHolder.getContext().authentication = authentication
		val result = userService.getCurrentUser()

		assertEquals(fakeUser, result)
		SecurityContextHolder.clearContext()
	}

	@Test
	fun `getCurrentUser - negative`() {
		assertThrows(ResponseStatusException::class.java) {
			userService.getCurrentUser()
		}
	}

	@Test
	fun `getCurrentUser - not authenticated`() {
		val authentication =
			UsernamePasswordAuthenticationToken(
				"test@test.com",
				null,
				emptyList()
			).apply {
				isAuthenticated = false
			}

		SecurityContextHolder.getContext().authentication = authentication

		assertThrows(ResponseStatusException::class.java) {
			userService.getCurrentUser()
		}

		SecurityContextHolder.clearContext()
	}

	@Test
	fun `getCurrentUser - user not found`() {
		val authentication =
			UsernamePasswordAuthenticationToken(
				"test@test.com",
				null,
				emptyList()
			)

		SecurityContextHolder.getContext().authentication = authentication
		`when`(userRepository.findByEmail("test@test.com")).thenReturn(null)

		assertThrows(UserNotFoundException::class.java) {
			userService.getCurrentUser()
		}

		SecurityContextHolder.clearContext()
	}

	@Test
	fun `updateLastLogin - null id`() {
		assertThrows(UserNotFoundException::class.java) {
			userService.updateLastLogin(null)
		}
	}

	@Test
	fun `getUserById - null id`() {
		assertThrows(UserNotFoundException::class.java) {
			userService.getUserById(null)
		}
	}

	@Test
	fun `deleteUserById - null id`() {
		assertThrows(UserNotFoundException::class.java) {
			userService.deleteUserById(null)
		}
	}

	@Test
	fun `createUser - password encoding failed`() {
		`when`(userRepository.existsByEmail(userDto.email)).thenReturn(false)
		`when`(passwordEncoder.encode(anyString())).thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			userService.createUser(userDto)
		}
	}

	@Test
	fun `updateUser - positive case`() {
		val oldUser = fakeUser
		`when`(userRepository.findByEmail(userDto.email)).thenReturn(oldUser)
		`when`(passwordEncoder.encode(anyString())).thenReturn("new hashed password")
		`when`(userRepository.save(any(User::class.java)))
			.thenAnswer { it.arguments[0] as User }

		val result = userService.updateUser(userDto)

		assertEquals(oldUser.id, result.id)
		assertEquals(oldUser.lastLogin, result.lastLogin)
		assertEquals("new hashed password", result.hashedPassword)
		assertEquals(userDto.fullName, result.fullName)
		assertEquals(userDto.role, result.role)
	}

	@Test
	fun `updateUser - user not found`() {
		`when`(userRepository.findByEmail(userDto.email)).thenReturn(null)

		assertThrows(UserNotFoundException::class.java) {
			userService.updateUser(userDto)
		}
	}

	@Test
	fun `updateUser - password encoding failed`() {
		`when`(userRepository.findByEmail(userDto.email)).thenReturn(fakeUser)
		`when`(passwordEncoder.encode(anyString())).thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			userService.updateUser(userDto)
		}
	}
}