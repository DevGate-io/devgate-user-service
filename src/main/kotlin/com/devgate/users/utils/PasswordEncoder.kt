package com.devgate.users.utils

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncoder {
	private val bcrypt = BCryptPasswordEncoder()

	fun encodePassword(raw: String): String = bcrypt.encode(raw)!!

	fun matches(raw: String, hashed: String) = bcrypt.matches(raw, hashed)
}