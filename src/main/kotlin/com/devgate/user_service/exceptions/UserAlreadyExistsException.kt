package com.devgate.user_service.exceptions

import org.springframework.http.HttpStatus

class UserAlreadyExistsException : ApiException(
	message = "User already exists",
	httpStatus = HttpStatus.CONFLICT
)