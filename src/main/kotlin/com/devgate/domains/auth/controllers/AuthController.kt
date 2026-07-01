package com.devgate.domains.auth.controllers

import com.devgate.domains.auth.dto.requests.LoginRequest
import com.devgate.domains.auth.dto.responses.AuthenticatedResponse
import com.devgate.domains.auth.dto.responses.RefreshResponse
import com.devgate.domains.auth.dto.toAuthenticatedResponse
import com.devgate.domains.auth.dto.toRefreshResponse
import com.devgate.domains.auth.services.AuthService
import com.devgate.domains.users.dto.UserDto
import com.devgate.domains.users.models.User
import com.devgate.domains.users.services.UserService
import com.devgate.utils.extensions.response_entity.withCookies
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController @Autowired constructor(
	private val authService: AuthService,
	private val userService: UserService
) {
	@PostMapping("login")
	fun login(
		@RequestBody @Valid request: LoginRequest
	): ResponseEntity<AuthenticatedResponse> {
		val response = authService.login(request)

		return ResponseEntity
			.ok()
			.withCookies(response.cookie)
			.body(response.toAuthenticatedResponse())
	}

	@PostMapping("register")
	fun register(
		@RequestBody @Valid request: UserDto
	): ResponseEntity<AuthenticatedResponse> {
		val response = authService.register(request)

		return ResponseEntity
			.ok()
			.withCookies(response.cookie)
			.body(response.toAuthenticatedResponse())
	}

	@PostMapping("logout")
	fun logout(request: HttpServletRequest): ResponseEntity<Void> {
		val cookie = authService.logout(request)

		return ResponseEntity
			.noContent()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.build()
	}

	@PostMapping("refresh")
	fun refresh(request: HttpServletRequest): ResponseEntity<RefreshResponse> {
		val response = authService.refresh(request)

		return ResponseEntity
			.ok()
			.header(HttpHeaders.SET_COOKIE, response.cookie.toString())
			.body(response.toRefreshResponse())
	}

	@GetMapping("me")
	fun me(): ResponseEntity<User> = ResponseEntity.ok(userService.getCurrentUser())
}