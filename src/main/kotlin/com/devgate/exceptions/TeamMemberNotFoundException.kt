package com.devgate.exceptions

import org.springframework.http.HttpStatus

class TeamMemberNotFoundException :
	ApiException(
		message = "Team member not found",
		httpStatus = HttpStatus.NOT_FOUND
	)