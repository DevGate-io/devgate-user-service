package com.devgate.auth.services.impl

import com.devgate.auth.dto.TokenPair
import com.devgate.auth.models.RefreshToken
import com.devgate.auth.repositories.RefreshTokenRepository
import com.devgate.auth.security.TokenGenerator
import com.devgate.auth.services.TokenManager
import com.devgate.users.models.User
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class TokenManagerImpl(
	@Autowired
	private val refreshTokenRepository: RefreshTokenRepository,

	@Autowired
	private val tokenGenerator: TokenGenerator
) : TokenManager {
	@Transactional
	override fun refreshToken(user: User, refreshToken: String): TokenPair {
		if (!tokenGenerator.validateRefreshToken(refreshToken)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
		}

		removeRefreshToken(user, refreshToken)

		val newTokenPair = createTokenPair(user)

		saveRefreshToken(user, newTokenPair.refreshToken)

		return newTokenPair
	}

	override fun generateTokenPair(user: User): TokenPair {
		val tokenPair = createTokenPair(user)
		saveRefreshToken(user, tokenPair.refreshToken)

		return tokenPair
	}

	private fun createTokenPair(details: UserDetails): TokenPair {
		val newRefreshToken: String = tokenGenerator.generateRefreshToken(details)
		val newAccessToken: String = tokenGenerator.generateAccessToken(details)

		return TokenPair(
			accessToken = newAccessToken,
			refreshToken = newRefreshToken
		)
	}

	@Transactional
	override fun removeRefreshToken(user: User, refreshToken: String) {
		val hashedToken = hashToken(refreshToken)

		refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashedToken)?.let { token ->
			refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, token.hashedToken)
		} ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found")
	}

	@Transactional
	override fun removeRefreshToken(refreshToken: String?) {
		if (refreshToken == null) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found")
		}

		val hashedToken = hashToken(refreshToken)

		refreshTokenRepository.findByHashedToken(hashedToken)?.let { token ->
			refreshTokenRepository.removeRefreshTokenByHashedToken(token.hashedToken)
		} ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found")
	}

	private fun saveRefreshToken(user: User, token: String) {
		val expiresAt = Instant.now().plusMillis(tokenGenerator.refreshTokenExpirationInMs)

		refreshTokenRepository.save(
			RefreshToken(
				hashedToken = hashToken(token),
				user = user,
				expiresAt = expiresAt,
			)
		)
	}

	private fun hashToken(token: String): String {
		val digest = MessageDigest.getInstance("SHA-256")
		val hashedToken = digest.digest(token.encodeToByteArray())

		return Base64.getEncoder().encodeToString(hashedToken)
	}
}