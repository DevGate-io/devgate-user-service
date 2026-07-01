package com.devgate.domains.auth.dto

import com.devgate.domains.auth.dto.responses.AuthenticatedResponse
import com.devgate.domains.users.models.User
import org.springframework.http.ResponseCookie

data class AuthenticatedDto(
	override var user: User,
	override var accessToken: String,
	var refreshToken: String,
	var cookie: Map<String, ResponseCookie>
) : AuthPayload

fun AuthenticatedDto.toAuthenticatedResponse(): AuthenticatedResponse =
	AuthenticatedResponse(
		user = this.user,
		accessToken = this.accessToken,
		refreshToken = this.refreshToken
	)