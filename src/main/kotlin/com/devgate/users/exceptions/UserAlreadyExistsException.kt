package com.devgate.users.exceptions

import org.springframework.http.HttpStatus

class UserAlreadyExistsException : ApiException(
	message = "User already exists",
	httpStatus = HttpStatus.CONFLICT
)