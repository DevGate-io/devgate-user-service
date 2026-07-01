package com.devgate.domains.auth.dto.responses

data class RefreshResponse(
	val accessToken: String,
	val refreshToken: String
)