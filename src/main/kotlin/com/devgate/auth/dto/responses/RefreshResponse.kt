package com.devgate.auth.dto.responses

import com.fasterxml.jackson.annotation.JsonProperty

data class RefreshResponse(
	@JsonProperty("access_token")
	val accessToken: String
)