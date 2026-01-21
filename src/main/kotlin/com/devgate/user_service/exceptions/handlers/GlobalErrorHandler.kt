package com.devgate.user_service.exceptions.handlers

import com.devgate.user_service.exceptions.ApiException
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