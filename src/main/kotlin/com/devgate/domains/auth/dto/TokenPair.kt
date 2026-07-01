package com.devgate.domains.auth.dto

data class TokenPair(
	val accessToken: String,
	val refreshToken: String
)