package com.devgate.domains.teams.dto

data class UpdateTeamRequest(
	val name: String? = null,
	val description: String? = null,
	val slug: String? = null
)