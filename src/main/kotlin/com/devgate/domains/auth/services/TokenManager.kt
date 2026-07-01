package com.devgate.domains.auth.services

import com.devgate.auth.dto.TokenPair
import com.devgate.users.models.User

interface TokenManager {
	fun refreshToken(
		user: User,
		refreshToken: String
	): TokenPair

	fun removeRefreshToken(
		user: User,
		refreshToken: String
	)

	fun removeRefreshToken(refreshToken: String?)

	fun generateTokenPair(user: User): TokenPair
}