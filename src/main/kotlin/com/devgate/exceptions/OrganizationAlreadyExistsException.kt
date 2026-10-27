package com.devgate.exceptions

import org.springframework.http.HttpStatus

class OrganizationAlreadyExistsException :
	ApiException(
		message = "Organization already exists",
		httpStatus = HttpStatus.CONFLICT
	)