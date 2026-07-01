package com.devgate.config

import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig
	@Autowired
	constructor(
		@Value($$"${rabbitmq.queue}")
		private val queueName: String
	) {
		@Bean
		fun auditQueue(): Queue = Queue(queueName)

		@Bean
		fun converter(): MessageConverter = JacksonJsonMessageConverter()
	}