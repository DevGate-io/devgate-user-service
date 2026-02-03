package com.devgate.auth.services

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie

interface JwtCookieService {
	fun generateCookie(token: String): ResponseCookie
	fun generateCleanCookie(): ResponseCookie
	fun getRefreshTokenFromCookie(request: HttpServletRequest): String?
}