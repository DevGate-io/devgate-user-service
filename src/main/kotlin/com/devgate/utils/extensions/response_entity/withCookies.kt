package com.devgate.utils.extensions.response_entity

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity

fun ResponseEntity.BodyBuilder.withCookies(cookie: Map<String, ResponseCookie>): ResponseEntity.BodyBuilder {
	cookie.values.forEach { value ->
		this.header(HttpHeaders.SET_COOKIE, value.toString())
	}

	return this
}