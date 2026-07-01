package com.devgate.domains.auth.security

import com.devgate.Constants
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

enum class JwtType {
	ACCESS,
	REFRESH
}

@Service
class TokenGenerator(
	@Value($$"${jwt.secret}")
	private val jwtSecret: String
) {
	private val logger = LoggerFactory.getLogger(this::class.java)

	val accessTokenExpirationInMs: Long = Duration.ofMinutes(15).toMillis()
	val refreshTokenExpirationInMs: Long = Duration.ofDays(30).toMillis()

	private val base64Decoder = Base64.getDecoder()
	private val decodedJwtSecret: SecretKey = Keys.hmacShaKeyFor(base64Decoder.decode(jwtSecret))

	private fun generateToken(
		details: UserDetails,
		type: JwtType,
		expiration: Long
	): String {
		val now = Date()
		val expiryDate = Instant.now().plusMillis(expiration)

		return Jwts
			.builder()
			.subject(details.username)
			.claim("type", type.name)
			.issuedAt(now)
			.expiration(Date.from(expiryDate))
			.signWith(decodedJwtSecret, Jwts.SIG.HS256)
			.compact()
	}

	fun generateAccessToken(details: UserDetails): String =
		generateToken(
			details,
			JwtType.ACCESS,
			accessTokenExpirationInMs
		)

	fun generateRefreshToken(details: UserDetails): String =
		generateToken(
			details,
			JwtType.REFRESH,
			refreshTokenExpirationInMs
		)

	private fun validateToken(
		token: String,
		type: JwtType
	): Boolean {
		val claims = getClaims(token) ?: return false
		val areTypesEqual = claims["type"] == type.name

		logger.debug("TokenGenerator[validateToken]: are types equal -> $areTypesEqual")

		try {
			Jwts
				.parser()
				.verifyWith(decodedJwtSecret)
				.build()
				.parseSignedClaims(token)
		} catch (e: ExpiredJwtException) {
			logger.error("JwtService[validateToken]: expired token", e)
		} catch (e: UnsupportedJwtException) {
			logger.error("JwtService[validateToken]: unsupported token", e)
		} catch (e: MalformedJwtException) {
			logger.error("JwtService[validateToken]: malformed token", e)
		} catch (e: SignatureException) {
			logger.error("JwtService[validateToken]: signature exception", e)
		} catch (e: java.lang.Exception) {
			logger.error("JwtService[validateToken]: invalid token", e)
		}

		return areTypesEqual && claims.expiration.time > Date().time
	}

	fun validateAccessToken(token: String): Boolean =
		validateToken(token.replace(Constants.AUTH_PREFIX, ""), JwtType.ACCESS)

	fun validateRefreshToken(token: String): Boolean = validateToken(token, JwtType.REFRESH)

	fun getUsernameFromToken(token: String): String? {
		val claims: Claims = getClaims(token) ?: throw IllegalArgumentException("Invalid token")

		return claims.subject
	}

	fun getClaims(token: String): Claims? {
		val processedToken = token.replace(Constants.AUTH_PREFIX, "")

		return try {
			Jwts
				.parser()
				.verifyWith(decodedJwtSecret)
				.build()
				.parseSignedClaims(processedToken)
				.payload
		} catch (e: Exception) {
			logger.error("JwtService[getClaims]: invalid token", e)
			null
		}
	}
}