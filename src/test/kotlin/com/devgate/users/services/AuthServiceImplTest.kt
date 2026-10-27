package com.devgate.users.services

import com.devgate.domains.auth.dto.requests.LoginRequest
import com.devgate.domains.auth.dto.TokenPair
import com.devgate.domains.auth.security.TokenGenerator
import com.devgate.domains.auth.services.JwtCookieService
import com.devgate.domains.auth.services.TokenManager
import com.devgate.domains.auth.services.impl.AuthServiceImpl
import com.devgate.domains.users.dto.UserDto
import com.devgate.domains.users.models.User
import com.devgate.domains.users.models.copy
import com.devgate.domains.users.models.enums.Role
import com.devgate.domains.users.repositories.UserRepository
import com.devgate.domains.users.services.UserService
import com.devgate.exceptions.ApiException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class AuthServiceImplTest {
	private val fakeUser =
		User(
			id = UUID.randomUUID(),
			fullName = "Fake User",
			email = "test@test.com",
			role = Role.MEMBER,
			lastLogin = Instant.now(),
			hashedPassword = "hashed password"
		)

	private val fakeTokenPair =
		TokenPair(
			accessToken = "access-token",
			refreshToken = "refresh-token"
		)

	@Mock
	private lateinit var userService: UserService

	@Mock
	private lateinit var userRepository: UserRepository

	@Mock
	private lateinit var tokenGenerator: TokenGenerator

	@Mock
	private lateinit var tokenManager: TokenManager

	@Mock
	private lateinit var jwtCookieService: JwtCookieService

	@Mock
	private lateinit var authenticationManager: AuthenticationManager

	@InjectMocks
	private lateinit var authService: AuthServiceImpl

	@Test
	fun `register - positive case`() {
		val request = UserDto(fullName = "New User", email = "new@test.com", role = Role.ADMIN, password = "password123")
		val user = fakeUser.copy(email = "new@test.com", fullName = "New User")
		val refreshCookie = ResponseCookie.from("jwt-refresh-cookie", "refresh-token").build()

		`when`(userService.getAllUsers()).thenReturn(emptyList())
		`when`(userService.createUser(request)).thenReturn(user)
		`when`(tokenManager.generateTokenPair(user)).thenReturn(fakeTokenPair)
		`when`(jwtCookieService.generateRefreshCookie("refresh-token")).thenReturn(refreshCookie)
		`when`(userRepository.save(user)).thenReturn(user)

		val result = authService.register(request)

		assertEquals(user, result.user)
		assertEquals("access-token", result.accessToken)
		assertEquals("refresh-token", result.refreshToken)
		assertTrue(result.cookie.containsKey("refresh"))
		verify(userRepository).save(user)
	}

	@Test
	fun `register - ignores requested role and creates ADMIN`() {
		val request = UserDto(fullName = "New User", email = "new@test.com", role = Role.MEMBER, password = "password123")
		val adminRequest = request.copy(role = Role.ADMIN)
		val user = fakeUser.copy(email = "new@test.com", fullName = "New User", role = Role.ADMIN)
		val refreshCookie = ResponseCookie.from("jwt-refresh-cookie", "refresh-token").build()

		`when`(userService.getAllUsers()).thenReturn(emptyList())
		`when`(userService.createUser(adminRequest)).thenReturn(user)
		`when`(tokenManager.generateTokenPair(user)).thenReturn(fakeTokenPair)
		`when`(jwtCookieService.generateRefreshCookie("refresh-token")).thenReturn(refreshCookie)
		`when`(userRepository.save(user)).thenReturn(user)

		authService.register(request)

		verify(userService).createUser(adminRequest)
		verify(userService, never()).createUser(request)
	}

	@Test
	fun `register - closed when users exist`() {
		val request = UserDto(fullName = "New User", email = "new@test.com", role = Role.ADMIN, password = "password123")

		`when`(userService.getAllUsers()).thenReturn(listOf(fakeUser))

		val exception =
			assertThrows(ApiException::class.java) {
				authService.register(request)
			}

		assertEquals(HttpStatus.FORBIDDEN, exception.httpStatus)
		verify(userService, never()).createUser(request)
	}

	@Test
	fun `register - user creation fails`() {
		val request = UserDto(fullName = "New User", email = "new@test.com", role = Role.ADMIN, password = "password123")

		`when`(userService.getAllUsers()).thenReturn(emptyList())
		`when`(userService.createUser(request)).thenThrow(ResponseStatusException(HttpStatus.CONFLICT))

		assertThrows(ResponseStatusException::class.java) {
			authService.register(request)
		}
	}

	@Test
	fun `login - positive case`() {
		val request = LoginRequest(email = "test@test.com", password = "password123")
		val refreshCookie = ResponseCookie.from("jwt-refresh-cookie", "refresh-token").build()

		`when`(userRepository.findByEmail("test@test.com")).thenReturn(fakeUser)
		`when`(tokenManager.generateTokenPair(fakeUser)).thenReturn(fakeTokenPair)
		`when`(jwtCookieService.generateRefreshCookie("refresh-token")).thenReturn(refreshCookie)
		`when`(userRepository.save(fakeUser)).thenReturn(fakeUser)

		val result = authService.login(request)

		assertEquals(fakeUser, result.user)
		assertEquals("access-token", result.accessToken)
		verify(authenticationManager).authenticate(any<UsernamePasswordAuthenticationToken>())
	}

	@Test
	fun `login - email not found`() {
		val request = LoginRequest(email = "notfound@test.com", password = "password123")

		`when`(userRepository.findByEmail("notfound@test.com")).thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			authService.login(request)
		}
	}

	@Test
	fun `login - bad credentials`() {
		val request = LoginRequest(email = "test@test.com", password = "wrong")

		`when`(userRepository.findByEmail("test@test.com")).thenReturn(fakeUser)
		`when`(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
			.thenThrow(BadCredentialsException("Bad credentials"))

		assertThrows(ResponseStatusException::class.java) {
			authService.login(request)
		}
	}

	@Test
	fun `login - updates lastLogin`() {
		val request = LoginRequest(email = "test@test.com", password = "password123")
		val refreshCookie = ResponseCookie.from("jwt-refresh-cookie", "refresh-token").build()

		`when`(userRepository.findByEmail("test@test.com")).thenReturn(fakeUser)
		`when`(tokenManager.generateTokenPair(fakeUser)).thenReturn(fakeTokenPair)
		`when`(jwtCookieService.generateRefreshCookie("refresh-token")).thenReturn(refreshCookie)
		`when`(userRepository.save(fakeUser)).thenReturn(fakeUser)

		authService.login(request)

		verify(userRepository).save(fakeUser)
	}

	@Test
	fun `logout - positive case with refresh token`() {
		val request = mockHttpRequestWithCookie("jwt-refresh-cookie", "refresh-token")
		val cleanCookie = ResponseCookie.from("jwt-refresh-cookie", "").build()

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn("refresh-token")
		`when`(jwtCookieService.generateCleanCookie()).thenReturn(cleanCookie)

		val result = authService.logout(request)

		assertEquals(cleanCookie, result)
		verify(tokenManager).removeRefreshToken("refresh-token")
	}

	@Test
	fun `logout - no refresh token in cookie`() {
		val request = mockHttpRequestWithoutCookie()
		val cleanCookie = ResponseCookie.from("jwt-refresh-cookie", "").build()

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn(null)
		`when`(jwtCookieService.generateCleanCookie()).thenReturn(cleanCookie)

		val result = authService.logout(request)

		assertEquals(cleanCookie, result)
		verify(tokenManager).removeRefreshToken(null)
	}

	@Test
	fun `logout - token already removed`() {
		val request = mockHttpRequestWithCookie("jwt-refresh-cookie", "refresh-token")
		val cleanCookie = ResponseCookie.from("jwt-refresh-cookie", "").build()

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn("refresh-token")
		`when`(jwtCookieService.generateCleanCookie()).thenReturn(cleanCookie)
		`when`(tokenManager.removeRefreshToken("refresh-token"))
			.thenThrow(ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found"))

		val result = authService.logout(request)

		assertEquals(cleanCookie, result)
	}

	@Test
	fun `refresh - positive case`() {
		val request = mockHttpRequestWithCookie("jwt-refresh-cookie", "old-refresh-token")
		val refreshCookie = ResponseCookie.from("jwt-refresh-cookie", "new-refresh-token").build()
		val newTokenPair = TokenPair(accessToken = "new-access", refreshToken = "new-refresh-token")

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn("old-refresh-token")
		`when`(tokenGenerator.getUsernameFromToken("old-refresh-token")).thenReturn(fakeUser.email)
		`when`(userRepository.findByEmail(fakeUser.email)).thenReturn(fakeUser)
		`when`(tokenManager.refreshToken(fakeUser, "old-refresh-token")).thenReturn(newTokenPair)
		`when`(jwtCookieService.generateRefreshCookie("new-refresh-token")).thenReturn(refreshCookie)

		val result = authService.refresh(request)

		assertEquals("new-access", result.accessToken)
		assertEquals("new-refresh-token", result.refreshToken)
		assertEquals(refreshCookie, result.cookie)
	}

	@Test
	fun `refresh - no refresh token`() {
		val request = mockHttpRequestWithoutCookie()

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			authService.refresh(request)
		}
	}

	@Test
	fun `refresh - invalid token`() {
		val request = mockHttpRequestWithCookie("jwt-refresh-cookie", "invalid-token")

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn("invalid-token")
		`when`(tokenGenerator.getUsernameFromToken("invalid-token")).thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			authService.refresh(request)
		}
	}

	@Test
	fun `refresh - user not found`() {
		val request = mockHttpRequestWithCookie("jwt-refresh-cookie", "valid-token")

		`when`(jwtCookieService.getRefreshTokenFromCookie(request)).thenReturn("valid-token")
		`when`(tokenGenerator.getUsernameFromToken("valid-token")).thenReturn("unknown@test.com")
		`when`(userRepository.findByEmail("unknown@test.com")).thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			authService.refresh(request)
		}
	}

	@Test
	fun `isNeedRegister - true when no users`() {
		`when`(userService.getAllUsers()).thenReturn(emptyList())

		assertTrue(authService.isNeedRegister())
	}

	@Test
	fun `isNeedRegister - false when users exist`() {
		`when`(userService.getAllUsers()).thenReturn(listOf(fakeUser))

		assertFalse(authService.isNeedRegister())
	}

	private fun mockHttpRequestWithCookie(
		cookieName: String,
		cookieValue: String
	): HttpServletRequest {
		val request = Mockito.mock(HttpServletRequest::class.java)
		`when`(request.getCookies()).thenReturn(arrayOf(Cookie(cookieName, cookieValue)))
		return request
	}

	private fun mockHttpRequestWithoutCookie(): HttpServletRequest {
		val request = Mockito.mock(HttpServletRequest::class.java)
		`when`(request.getCookies()).thenReturn(null)
		return request
	}
}