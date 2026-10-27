package com.devgate.exceptions

import org.springframework.http.HttpStatus

class DepartmentNotFoundException :
	ApiException(
		message = "Department not found",
		httpStatus = HttpStatus.NOT_FOUND
	)