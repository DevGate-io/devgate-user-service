package com.devgate.users.controllers

import com.devgate.users.Endpoints
import com.devgate.users.dto.UserDto
import com.devgate.users.models.User
import com.devgate.users.services.UserService
import jakarta.validation.Valid
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
	fun createUser(@RequestBody @Valid body: UserDto): ResponseEntity<User> {
		return ResponseEntity.ok(userService.createUser(body))
	}
}