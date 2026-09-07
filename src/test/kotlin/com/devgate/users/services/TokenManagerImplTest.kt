package com.devgate.users.services

import com.devgate.domains.auth.models.RefreshToken
import com.devgate.domains.auth.repositories.RefreshTokenRepository
import com.devgate.domains.auth.security.TokenGenerator
import com.devgate.domains.auth.services.impl.TokenManagerImpl
import com.devgate.domains.users.models.User
import com.devgate.domains.users.models.enums.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class TokenManagerImplTest {
	private val fakeUser =
		User(
			id = UUID.randomUUID(),
			fullName = "Fake User",
			email = "test@test.com",
			role = Role.MEMBER,
			lastLogin = Instant.now(),
			hashedPassword = "hashed password"
		)

	@Mock
	private lateinit var refreshTokenRepository: RefreshTokenRepository

	@Mock
	private lateinit var tokenGenerator: TokenGenerator

	@InjectMocks
	private lateinit var tokenManager: TokenManagerImpl

	private fun hashToken(token: String): String {
		val digest = MessageDigest.getInstance("SHA-256")
		val hashedToken = digest.digest(token.encodeToByteArray())
		return Base64.getEncoder().encodeToString(hashedToken)
	}

	@Test
	fun `generateTokenPair - positive case`() {
		`when`(tokenGenerator.generateAccessToken(fakeUser)).thenReturn("access-token")
		`when`(tokenGenerator.generateRefreshToken(fakeUser)).thenReturn("refresh-token")
		`when`(tokenGenerator.refreshTokenExpirationInMs).thenReturn(2592000000L)

		val result = tokenManager.generateTokenPair(fakeUser)

		assertEquals("access-token", result.accessToken)
		assertEquals("refresh-token", result.refreshToken)
		verify(refreshTokenRepository).save(any(RefreshToken::class.java))
	}

	@Test
	fun `generateTokenPair - saves token with correct hash`() {
		`when`(tokenGenerator.generateAccessToken(fakeUser)).thenReturn("access-token")
		`when`(tokenGenerator.generateRefreshToken(fakeUser)).thenReturn("refresh-token")
		`when`(tokenGenerator.refreshTokenExpirationInMs).thenReturn(2592000000L)

		tokenManager.generateTokenPair(fakeUser)

		val savedToken = ArgumentCaptor.forClass(RefreshToken::class.java)
		verify(refreshTokenRepository).save(savedToken.capture())

		assertEquals(hashToken("refresh-token"), savedToken.value.hashedToken)
		assertEquals(fakeUser, savedToken.value.user)
		assertNotNull(savedToken.value.expiresAt)
	}

	@Test
	fun `refreshToken - positive case`() {
		`when`(tokenGenerator.validateRefreshToken("old-refresh-token")).thenReturn(true)
		`when`(tokenGenerator.generateAccessToken(fakeUser)).thenReturn("new-access-token")
		`when`(tokenGenerator.generateRefreshToken(fakeUser)).thenReturn("new-refresh-token")
		`when`(tokenGenerator.refreshTokenExpirationInMs).thenReturn(2592000000L)

		val existingToken = RefreshToken(
			id = UUID.randomUUID(),
			hashedToken = hashToken("old-refresh-token"),
			user = fakeUser,
			expiresAt = Instant.now().plusSeconds(3600)
		)
		`when`(refreshTokenRepository.findByUserIdAndHashedToken(fakeUser.id, hashToken("old-refresh-token")))
			.thenReturn(existingToken)

		val result = tokenManager.refreshToken(fakeUser, "old-refresh-token")

		assertEquals("new-access-token", result.accessToken)
		assertEquals("new-refresh-token", result.refreshToken)
		verify(refreshTokenRepository).deleteByUserIdAndHashedToken(fakeUser.id, existingToken.hashedToken)
		verify(refreshTokenRepository).save(any(RefreshToken::class.java))
	}

	@Test
	fun `refreshToken - invalid token`() {
		`when`(tokenGenerator.validateRefreshToken("invalid-token")).thenReturn(false)

		assertThrows(ResponseStatusException::class.java) {
			tokenManager.refreshToken(fakeUser, "invalid-token")
		}
	}

	@Test
	fun `refreshToken - old token not found in db`() {
		`when`(tokenGenerator.validateRefreshToken("old-refresh-token")).thenReturn(true)
		`when`(refreshTokenRepository.findByUserIdAndHashedToken(fakeUser.id, hashToken("old-refresh-token")))
			.thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			tokenManager.refreshToken(fakeUser, "old-refresh-token")
		}
	}

	@Test
	fun `removeRefreshToken with user - positive case`() {
		val existingToken = RefreshToken(
			id = UUID.randomUUID(),
			hashedToken = hashToken("refresh-token"),
			user = fakeUser,
			expiresAt = Instant.now().plusSeconds(3600)
		)
		`when`(refreshTokenRepository.findByUserIdAndHashedToken(fakeUser.id, hashToken("refresh-token")))
			.thenReturn(existingToken)

		tokenManager.removeRefreshToken(fakeUser, "refresh-token")

		verify(refreshTokenRepository).deleteByUserIdAndHashedToken(fakeUser.id, existingToken.hashedToken)
	}

	@Test
	fun `removeRefreshToken with user - token not found`() {
		`when`(refreshTokenRepository.findByUserIdAndHashedToken(fakeUser.id, hashToken("refresh-token")))
			.thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			tokenManager.removeRefreshToken(fakeUser, "refresh-token")
		}
	}

	@Test
	fun `removeRefreshToken without user - positive case`() {
		val existingToken = RefreshToken(
			id = UUID.randomUUID(),
			hashedToken = hashToken("refresh-token"),
			user = fakeUser,
			expiresAt = Instant.now().plusSeconds(3600)
		)
		`when`(refreshTokenRepository.findByHashedToken(hashToken("refresh-token")))
			.thenReturn(existingToken)

		tokenManager.removeRefreshToken("refresh-token" as String?)

		verify(refreshTokenRepository).removeRefreshTokenByHashedToken(existingToken.hashedToken)
	}

	@Test
	fun `removeRefreshToken without user - null token`() {
		assertThrows(ResponseStatusException::class.java) {
			tokenManager.removeRefreshToken(null)
		}
	}

	@Test
	fun `removeRefreshToken without user - token not found`() {
		`when`(refreshTokenRepository.findByHashedToken(hashToken("refresh-token")))
			.thenReturn(null)

		assertThrows(ResponseStatusException::class.java) {
			tokenManager.removeRefreshToken("refresh-token" as String?)
		}
	}
}