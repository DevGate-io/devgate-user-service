package com.devgate.auth.dto

data class TokenPair(
	val accessToken: String,
	val refreshToken: String
)