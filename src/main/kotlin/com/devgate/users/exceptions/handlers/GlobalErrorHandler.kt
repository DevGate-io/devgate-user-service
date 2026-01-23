package com.devgate.users.exceptions.handlers

import com.devgate.users.exceptions.ApiException
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalErrorHandler {
	@ExceptionHandler(ApiException::class)
	fun handleUserNotFoundException(exception: ApiException): ProblemDetail {
		return ProblemDetail.forStatusAndDetail(exception.httpStatus, exception.message)
	}
}