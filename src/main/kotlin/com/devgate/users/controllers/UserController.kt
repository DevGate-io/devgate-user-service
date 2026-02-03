package com.devgate.users.controllers

import com.devgate.users.dto.UserDto
import com.devgate.users.models.User
import com.devgate.users.services.UserService
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/users")
class UserController(
	@Autowired
	private val userService: UserService
) {
	private val logger: Logger = LoggerFactory.getLogger(this::class.java)

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
			logger.error(e.message)
			return ResponseEntity.badRequest().build()
		}
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	fun deleteUserById(@PathVariable id: String): ResponseEntity<Nothing> {
		try {
			userService.deleteUserById(UUID.fromString(id))

			return ResponseEntity.ok().build()
		} catch (e: IllegalArgumentException) {
			logger.error(e.message)
			return ResponseEntity.badRequest().build()
		}
	}

	@PostMapping
	fun createUser(@RequestBody @Valid body: UserDto): ResponseEntity<User> {
		return ResponseEntity.ok(userService.createUser(body))
	}

	@PutMapping
	fun updateUser(@RequestBody @Valid body: UserDto): ResponseEntity<User> {
		return ResponseEntity.ok(userService.updateUser(body))
	}

	@GetMapping("/current")
	fun getCurrentUser(): ResponseEntity<User> {
		return ResponseEntity.ok(userService.getCurrentUser())
	}
}