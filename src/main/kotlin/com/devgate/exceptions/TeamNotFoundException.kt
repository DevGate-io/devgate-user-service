package com.devgate.exceptions

import org.springframework.http.HttpStatus

class TeamNotFoundException :
	ApiException(
		message = "Team not found",
		httpStatus = HttpStatus.NOT_FOUND
	)