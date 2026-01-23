package com.devgate.users.services

import com.devgate.users.dto.UserDto
import com.devgate.users.exceptions.UserAlreadyExistsException
import com.devgate.users.exceptions.UserNotFoundException
import com.devgate.users.models.User
import com.devgate.users.models.enums.Role
import com.devgate.users.repositories.UserRepository
import com.devgate.users.services.impl.UserServiceImpl
import com.devgate.users.utils.PasswordEncoder
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
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class UserServiceTest {
	private val userDto = UserDto(
		fullName = "Fake User 1",
		email = "test@test.com",
		role = Role.MEMBER,
		password = "this is password",
	)

	private val fakeUser = User(
		id = UUID.randomUUID(),
		fullName = "Fake User 1",
		email = "test@test.com",
		role = Role.MEMBER,
		lastLogin = Instant.now(),
		hashedPassword = "this is hashed password",
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

		`when`(passwordEncoder.encodePassword(anyString()))
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
}