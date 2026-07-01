package com.devgate.domains.users.controllers

import com.devgate.users.dto.UpdateUserRoleRequest
import com.devgate.users.dto.UserDto
import com.devgate.users.models.Action
import com.devgate.users.models.User
import com.devgate.users.models.toTarget
import com.devgate.users.services.AuditLogService
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
class UserController
	@Autowired
	constructor(
		private val auditLogService: AuditLogService,
		private val userService: UserService
	) {
		private val logger: Logger = LoggerFactory.getLogger(this::class.java)

		@GetMapping
		fun getAll(
			@RequestParam(required = false) search: String?
		): ResponseEntity<List<User>> = ResponseEntity.ok(userService.getAllUsers(search))

		@GetMapping("/{id}")
		fun getUserById(
			@PathVariable id: String
		): ResponseEntity<User> {
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
		fun deleteUserById(
			@PathVariable id: String,
		): ResponseEntity<Nothing> {
			try {
				val uuid = runCatching { UUID.fromString(id) }.getOrElse {
					return ResponseEntity.badRequest().build()
				}

				val user = userService.getUserById(uuid)

				auditLogService.sendMessage(Action.USER_DELETED, user.toTarget())
				userService.deleteUserById(UUID.fromString(id))

				return ResponseEntity.ok().build()
			} catch (e: IllegalArgumentException) {
				logger.error(e.message)
				return ResponseEntity.badRequest().build()
			}
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun createUser(
			@RequestBody @Valid body: UserDto
		): ResponseEntity<User> {
			val user = userService.createUser(body)
			auditLogService.sendMessage(Action.USER_CREATED, user.toTarget())

			return ResponseEntity.ok(user)
		}

		@PutMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun updateUser(
			@RequestBody @Valid body: UserDto
		): ResponseEntity<User> {
			val user = userService.updateUser(body)
			auditLogService.sendMessage(Action.USER_UPDATED, user.toTarget())

			return ResponseEntity.ok(user)
		}

		@PatchMapping("/{id}/role")
		@PreAuthorize("hasRole('ADMIN')")
		fun updateUserRole(
			@PathVariable id: String,
			@RequestBody @Valid body: UpdateUserRoleRequest
		): ResponseEntity<User> {
			try {
				val updated = userService.updateUserRole(UUID.fromString(id), body.role)
				auditLogService.sendMessage(Action.ROLE_CHANGED, updated.toTarget())

				return ResponseEntity.ok(updated)
			} catch (e: IllegalArgumentException) {
				logger.error(e.message)
				return ResponseEntity.badRequest().build()
			}
		}

		@GetMapping("/current")
		fun getCurrentUser(): ResponseEntity<User> = ResponseEntity.ok(userService.getCurrentUser())
	}