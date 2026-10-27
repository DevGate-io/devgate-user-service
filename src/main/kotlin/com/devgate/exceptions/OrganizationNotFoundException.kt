package com.devgate.exceptions

import org.springframework.http.HttpStatus

class OrganizationNotFoundException :
	ApiException(
		message = "Organization not found",
		httpStatus = HttpStatus.NOT_FOUND
	)