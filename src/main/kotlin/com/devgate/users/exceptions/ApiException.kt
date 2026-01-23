package com.devgate.users.exceptions

import org.springframework.http.HttpStatus

open class ApiException(
	override val message: String,
	val httpStatus: HttpStatus
) : RuntimeException(message)