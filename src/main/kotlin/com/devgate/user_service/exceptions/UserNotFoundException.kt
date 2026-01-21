package com.devgate.user_service.exceptions

import org.springframework.http.HttpStatus

class UserNotFoundException : ApiException(
	message = "User not found",
	httpStatus = HttpStatus.NOT_FOUND
)