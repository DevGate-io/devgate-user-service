package com.devgate.auth.dto.responses

import com.devgate.auth.dto.AuthPayload
import com.devgate.users.models.User

data class AuthenticatedResponse(
	override var user: User,
	override var accessToken: String,
) : AuthPayload
