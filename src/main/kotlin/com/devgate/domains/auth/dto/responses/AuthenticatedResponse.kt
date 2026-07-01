package com.devgate.domains.auth.dto.responses

import com.devgate.domains.auth.dto.AuthPayload
import com.devgate.domains.users.models.User

data class AuthenticatedResponse(
	override var user: User,
	override var accessToken: String,
	val refreshToken: String
) : AuthPayload