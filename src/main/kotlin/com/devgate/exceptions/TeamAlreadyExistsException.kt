package com.devgate.exceptions

import org.springframework.http.HttpStatus

class TeamAlreadyExistsException :
	ApiException(
		message = "Team already exists",
		httpStatus = HttpStatus.CONFLICT
	)