package com.devgate.auth.dto.requests

import com.devgate.auth.models.Principal
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class LoginRequest(
	@Email
	override var email: String,

	@NotBlank
	@NotNull
	@Size(min = 8)
	override var password: String
) : Principal