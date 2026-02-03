package com.devgate.exceptions.handlers

import com.devgate.exceptions.ApiException
import com.devgate.utils.ApiError
import com.devgate.utils.ApiErrorParser
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalErrorHandler {
	@ExceptionHandler(ApiException::class)
	fun handleUserNotFoundException(exception: ApiException): ProblemDetail {
		return ProblemDetail.forStatusAndDetail(exception.httpStatus, exception.message)
	}

	@ExceptionHandler(ResponseStatusException::class)
	fun handleException(exception: ResponseStatusException): ProblemDetail {
		return ProblemDetail.forStatusAndDetail(exception.statusCode, exception.reason)
	}

	@ExceptionHandler(HttpClientErrorException::class)
	fun handleException(exception: HttpClientErrorException): ProblemDetail {
		val body = exception.responseBodyAsString
		val apiError: ApiError? = ApiErrorParser.parse(body)

		return ProblemDetail.forStatusAndDetail(
			exception.statusCode,
			apiError?.detail ?: exception.message
		)
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleException(exception: HttpMessageNotReadableException): ProblemDetail {
		return ProblemDetail.forStatusAndDetail(
			HttpStatus.UNPROCESSABLE_ENTITY,
			exception.message
		)
	}
}