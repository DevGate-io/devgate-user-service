package com.devgate.domains.teams.dto

import jakarta.validation.constraints.NotBlank

data class CreateTeamRequest(
	@NotBlank
	val name: String,
	@NotBlank
	val description: String,
	@NotBlank
	val slug: String
)