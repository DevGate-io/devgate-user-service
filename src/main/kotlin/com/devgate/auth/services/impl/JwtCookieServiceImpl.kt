package com.devgate.auth.services.impl

import com.devgate.Constants
import com.devgate.auth.services.JwtCookieService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.web.util.WebUtils
import java.time.Duration

@Service
class JwtCookieServiceImpl(
	@Value($$"${jwt.cookie.name}")
	private val cookieName: String,
	@Value($$"${jwt.cookie.secure:false}")
	private val cookieSecure: Boolean,
	@Value($$"${jwt.cookie.same-site:Lax}")
	private val cookieSameSite: String
) : JwtCookieService {
	override fun generateRefreshCookie(token: String): ResponseCookie = buildCookie(token, Duration.ofDays(30))

	private fun buildCookie(
		value: String,
		maxAge: Duration
	): ResponseCookie =
		ResponseCookie
			.from(cookieName, value)
			.httpOnly(true)
			.secure(cookieSecure)
			.sameSite(cookieSameSite)
			.path(Constants.REFRESH_PATH)
			.maxAge(maxAge)
			.build()

	override fun generateCleanCookie(): ResponseCookie =
		ResponseCookie
			.from(cookieName, "")
			.httpOnly(true)
			.secure(cookieSecure)
			.sameSite(cookieSameSite)
			.path(Constants.REFRESH_PATH)
			.maxAge(Duration.ZERO)
			.build()

	override fun getRefreshTokenFromCookie(request: HttpServletRequest): String? {
		val cookie = WebUtils.getCookie(request, cookieName) ?: return null

		return cookie.value
	}
}