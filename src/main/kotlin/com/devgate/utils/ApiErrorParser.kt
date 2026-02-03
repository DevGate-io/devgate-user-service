package com.devgate.utils

import tools.jackson.databind.ObjectMapper

data class ApiError(
	val detail: String,
	val instance: String,
	val status: Int,
	val title: String
)

class ApiErrorParser {
	companion object {
		fun parse(response: String): ApiError? {
			return try {
				val mapper = ObjectMapper()

				return mapper.readValue(response, ApiError::class.java)
			} catch (e: Exception) {
				null
			}
		}
	}
}