package com.devgate.auth.security

import com.devgate.Constants
import com.devgate.exceptions.UserNotFoundException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestFilter(
	private val tokenGenerator: TokenGenerator,
	private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain
	) {
		val authHeader = request.getHeader(Constants.AUTH_HEADER)

		authHeader?.let { authenticate(it, request) }

		filterChain.doFilter(request, response)
	}

	private fun authenticate(header: String, request: HttpServletRequest) {
		val token = header.replace(Constants.AUTH_PREFIX, "")

		if (header.startsWith(Constants.AUTH_PREFIX) && tokenGenerator.validateAccessToken(token)) {
			val email = tokenGenerator.getUsernameFromToken(token) ?: throw UserNotFoundException()
			val user = userDetailsService.loadUserByUsername(email)

			val authToken = UsernamePasswordAuthenticationToken(user.username, null, user.authorities)
			val securityContext = SecurityContextHolder.getContext()

			authToken.details = WebAuthenticationDetails(request)
			securityContext.authentication = authToken
		}
	}
}