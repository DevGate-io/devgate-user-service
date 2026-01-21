package com.devgate.user_service.controllers

import com.devgate.user_service.Endpoints
import com.devgate.user_service.models.User
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(Endpoints.USERS)
class UserController {
	@GetMapping
	fun getAll(): ResponseEntity<List<User>> {
		return ResponseEntity.ok(emptyList())
	}

	@GetMapping("/{id}")
	fun getUserById(@PathVariable id: UUID): ResponseEntity<User> {
		return ResponseEntity.ok(null)
	}
}