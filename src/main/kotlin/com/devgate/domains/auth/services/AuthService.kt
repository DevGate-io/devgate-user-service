package com.devgate.domains.auth.services

import com.devgate.domains.auth.dto.AuthenticatedDto
import com.devgate.domains.auth.dto.RefreshDto
import com.devgate.domains.auth.dto.requests.LoginRequest
import com.devgate.domains.users.dto.UserDto
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie

interface AuthService {
	fun register(request: UserDto): AuthenticatedDto

	fun login(request: LoginRequest): AuthenticatedDto

	fun logout(request: HttpServletRequest): ResponseCookie

	fun refresh(request: HttpServletRequest): RefreshDto
}