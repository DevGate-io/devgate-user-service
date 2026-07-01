package com.devgate.domains.users.models

data class Target (
	var type: String? = null,
	var id: String? = null,
	var label: String? = null,
)

fun User.toTarget(): Target = Target(
	type = "user",
	id = id.toString(),
	label = fullName
)