package com.devgate.domains.auth.dto.requests

import com.devgate.domains.auth.models.Principal
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class LoginRequest(
	@Email
	@NotBlank
	@NotNull
	override var email: String,
	@NotBlank
	@NotNull
	override var password: String
) : Principal