package com.devgate.domains.users.services.impl

import com.devgate.users.dto.LogMessagePayload
import com.devgate.users.models.Action
import com.devgate.users.models.Target
import com.devgate.users.services.AuditLogService
import com.devgate.users.services.UserService
import com.devgate.utils.error
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuditLogServiceImpl @Autowired constructor(
	private val userService: UserService,
	private val rabbitTemplate: RabbitTemplate,

	@Value($$"${rabbitmq.queue}")
	private val queue: String
): AuditLogService {
	override fun sendMessage(action: Action, target: Target) {
		val user = userService.getCurrentUser()

		val payload = LogMessagePayload(
			actorId = user.id,
			action = action,
			target = target
		)

		try {
			rabbitTemplate.convertAndSend(queue, payload)
		} catch (e: AmqpException) {
			error(e.stackTraceToString())
		}
	}
}