package com.devgate.auth.dto.responses

data class RefreshResponse(
	val accessToken: String,
	val refreshToken: String
)