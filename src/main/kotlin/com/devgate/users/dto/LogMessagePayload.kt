package com.devgate.users.dto

import com.devgate.users.models.Action
import com.devgate.users.models.Target
import java.time.LocalDateTime
import java.util.UUID

data class LogMessagePayload(
	val action: Action? = null,
	val actorId: UUID? = null,
	val target: Target? = null,
	val payload: String? = null,
	val createdAt: LocalDateTime? = LocalDateTime.now()
)