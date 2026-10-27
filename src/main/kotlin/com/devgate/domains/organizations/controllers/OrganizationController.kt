package com.devgate.domains.organizations.controllers

import com.devgate.domains.organizations.dto.CreateOrganizationRequest
import com.devgate.domains.organizations.dto.OrganizationResponse
import com.devgate.domains.organizations.dto.toTarget
import com.devgate.domains.organizations.services.OrganizationService
import com.devgate.domains.users.models.Action
import com.devgate.domains.users.services.AuditLogService
import com.devgate.utils.Logger
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/organizations")
class OrganizationController
	@Autowired
	constructor(
		private val auditLogService: AuditLogService,
		private val organizationService: OrganizationService
	) {
		@GetMapping
		fun getAll(): ResponseEntity<List<OrganizationResponse>> =
			ResponseEntity.ok(organizationService.getAllOrganizations())

		@GetMapping("/{id}")
		fun getById(
			@PathVariable id: String
		): ResponseEntity<OrganizationResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(organizationService.getOrganizationById(uuid))
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun create(
			@RequestBody @Valid body: CreateOrganizationRequest
		): ResponseEntity<OrganizationResponse> {
			val response = organizationService.createOrganization(body)
			auditLogService.sendMessage(Action.ORGANIZATION_CREATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		private fun parseUuid(id: String): UUID? =
			runCatching { UUID.fromString(id) }
				.onFailure { Logger.error("Invalid UUID: $id", this) }
				.getOrNull()
	}