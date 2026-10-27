package com.devgate.exceptions

import com.devgate.domains.organizations.models.MAX_DEPARTMENT_DEPTH
import org.springframework.http.HttpStatus

class DepartmentDepthExceededException :
	ApiException(
		message = "Department nesting depth must not exceed $MAX_DEPARTMENT_DEPTH levels",
		httpStatus = HttpStatus.BAD_REQUEST
	)