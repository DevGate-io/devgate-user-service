package com.devgate.auth.controllers

import com.devgate.auth.dto.requests.LoginRequest
import com.devgate.auth.dto.responses.AuthenticatedResponse
import com.devgate.auth.dto.responses.RefreshResponse
import com.devgate.auth.dto.toAuthenticatedResponse
import com.devgate.auth.dto.toRefreshResponse
import com.devgate.auth.services.AuthService
import com.devgate.users.dto.UserDto
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
	@Autowired
	private val authService: AuthService,
) {
	@PostMapping("login")
	fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<AuthenticatedResponse> {
		val response = authService.login(request)

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, response.cookie.toString())
			.body(response.toAuthenticatedResponse())
	}

	@PostMapping("register")
	fun register(
		@RequestBody @Valid request: UserDto,
	): ResponseEntity<AuthenticatedResponse> {
		val registerResponse = authService.register(request)

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, registerResponse.cookie.toString())
			.body(registerResponse.toAuthenticatedResponse())
	}

	@PostMapping("logout")
	fun logout(request: HttpServletRequest): ResponseEntity<Void> {
		val cookie = authService.logout(request)

		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.build()
	}

	@PostMapping("refresh")
	fun refresh(request: HttpServletRequest): ResponseEntity<RefreshResponse> {
		val response = authService.refresh(request)

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, response.cookie.toString())
			.body(response.toRefreshResponse())
	}
}