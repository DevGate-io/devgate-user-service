package com.devgate.utils

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.security.crypto.password.PasswordEncoder as IPasswordEncoder

@Component
class PasswordEncoder : IPasswordEncoder {
	private val bcrypt = BCryptPasswordEncoder()

	override fun encode(raw: CharSequence?): String? = bcrypt.encode(raw)

	override fun matches(
		rawPassword: CharSequence?,
		encodedPassword: String?
	): Boolean = bcrypt.matches(rawPassword, encodedPassword)
}