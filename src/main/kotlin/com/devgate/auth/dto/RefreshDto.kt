package com.devgate.auth.dto

import com.devgate.auth.dto.responses.RefreshResponse
import org.springframework.http.ResponseCookie

data class RefreshDto(
	val accessToken: String,
	val refreshToken: String,
	val cookie: ResponseCookie
)

fun RefreshDto.toRefreshResponse(): RefreshResponse =
	RefreshResponse(
		accessToken = this.accessToken,
		refreshToken = this.refreshToken
	)