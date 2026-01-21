package com.devgate.user_service.controllers

import com.devgate.user_service.Endpoints
import com.devgate.user_service.dto.UserDto
import com.devgate.user_service.models.User
import com.devgate.user_service.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(Endpoints.USERS)
class UserController(
	@Autowired
	private val userService: UserService
) {
	@GetMapping
	fun getAll(): ResponseEntity<List<User>> {
		return ResponseEntity.ok(userService.getAllUsers())
	}

	@GetMapping("/{id}")
	fun getUserById(@PathVariable id: String): ResponseEntity<User> {
		try {
			val user = userService.getUserById(UUID.fromString(id))
			return ResponseEntity.ok(user)
		} catch (e: IllegalArgumentException) {
			return ResponseEntity.badRequest().build()
		}
	}

	@DeleteMapping("/{id}")
	fun deleteUserById(@PathVariable id: String): ResponseEntity<Nothing> {
		try {
			userService.deleteUserById(UUID.fromString(id))

			return ResponseEntity.ok().build()
		} catch (e: IllegalArgumentException) {
			return ResponseEntity.badRequest().build()
		}
	}

	@PostMapping
	fun createUser(@RequestBody body: UserDto): ResponseEntity<User> {
		return ResponseEntity.ok(userService.createUser(body))
	}
}