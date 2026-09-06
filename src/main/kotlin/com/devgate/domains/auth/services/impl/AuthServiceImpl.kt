package com.devgate.domains.auth.services.impl

import com.devgate.domains.auth.dto.AuthenticatedDto
import com.devgate.domains.auth.dto.RefreshDto
import com.devgate.domains.auth.dto.requests.LoginRequest
import com.devgate.domains.auth.security.TokenGenerator
import com.devgate.domains.auth.services.AuthService
import com.devgate.domains.auth.services.JwtCookieService
import com.devgate.domains.auth.services.TokenManager
import com.devgate.domains.users.dto.UserDto
import com.devgate.domains.users.models.User
import com.devgate.domains.users.repositories.UserRepository
import com.devgate.domains.users.services.UserService
import com.devgate.utils.Logger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class AuthServiceImpl(
	private val userService: UserService,
	private val userRepository: UserRepository,
	private val tokenGenerator: TokenGenerator,
	private val tokenManager: TokenManager,
	private val jwtCookieService: JwtCookieService,
	private val authenticationManager: AuthenticationManager
) : AuthService {

	private fun authenticate(
		user: User,
		rawPassword: CharSequence
	): AuthenticatedDto {
		try {
			val token = UsernamePasswordAuthenticationToken(user.email, rawPassword, user.authorities)
			authenticationManager.authenticate(token)
		} catch (e: BadCredentialsException) {
			Logger.error(e.stackTraceToString(), this)
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials")
		}

		val tokenPair = tokenManager.generateTokenPair(user)
		val refreshCookie = jwtCookieService.generateRefreshCookie(tokenPair.refreshToken)

		Logger.info("AuthService[getAuthenticatedResponse]: user ${user.email} was successfully authenticated", this)

		user.lastLogin = Instant.now()
		userRepository.save(user)

		return AuthenticatedDto(
			user = user,
			accessToken = tokenPair.accessToken,
			refreshToken = tokenPair.refreshToken,
			cookie = mapOf("refresh" to refreshCookie)
		)
	}

	override fun register(request: UserDto): AuthenticatedDto {
		val user: User = userService.createUser(request)
		return authenticate(user, request.password)
	}

	override fun login(request: LoginRequest): AuthenticatedDto {
		val user: User =
			userRepository.findByEmail(request.email) ?: throw ResponseStatusException(
				HttpStatus.UNAUTHORIZED,
				"Invalid email or password"
			)

		return authenticate(user, request.password)
	}

	override fun logout(request: HttpServletRequest): ResponseCookie {
		val clearedCookie = jwtCookieService.generateCleanCookie()
		val refreshToken: String? = jwtCookieService.getRefreshTokenFromCookie(request)

		try {
			tokenManager.removeRefreshToken(refreshToken)
		} catch (e: ResponseStatusException) {
			Logger.info("AuthService[logout]: refresh token already absent (${e.reason})", this)
		}

		return clearedCookie
	}

	override fun refresh(request: HttpServletRequest): RefreshDto {
		val refreshToken: String =
			jwtCookieService.getRefreshTokenFromCookie(request) ?: throw ResponseStatusException(
				HttpStatus.UNAUTHORIZED,
				"Refresh token not found"
			)

		val email =
			tokenGenerator.getUsernameFromToken(refreshToken) ?: throw ResponseStatusException(
				HttpStatus.UNAUTHORIZED,
				"Invalid token"
			)

		val user: User =
			userRepository.findByEmail(email) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")

		val tokenPair = tokenManager.refreshToken(user, refreshToken)
		val refreshCookie = jwtCookieService.generateRefreshCookie(tokenPair.refreshToken)

		return RefreshDto(
			accessToken = tokenPair.accessToken,
			refreshToken = tokenPair.refreshToken,
			cookie = refreshCookie
		)
	}
}