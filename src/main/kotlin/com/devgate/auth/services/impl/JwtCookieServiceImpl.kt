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
	private val cookieName: String
) : JwtCookieService {
	override fun generateCookie(token: String): ResponseCookie {
		return ResponseCookie.from(
			cookieName,
			token
		)
			.httpOnly(true)
			.path(Constants.REFRESH_PATH)
			.maxAge(Duration.ofDays(30))
			.build()

	}

	override fun generateCleanCookie(): ResponseCookie {
		return ResponseCookie.from(cookieName, "")
			.path(Constants.REFRESH_PATH)
			.build()
	}

	override fun getRefreshTokenFromCookie(request: HttpServletRequest): String? {
		val cookie = WebUtils.getCookie(request, cookieName) ?: return null

		return cookie.value
	}
}