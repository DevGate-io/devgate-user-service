package com.devgate.user_service.exceptions

import org.springframework.http.HttpStatus

open class ApiException(
	override val message: String,
	val httpStatus: HttpStatus
) : RuntimeException(message)