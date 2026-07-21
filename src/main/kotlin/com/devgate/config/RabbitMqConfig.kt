package com.devgate.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
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
		private val queueName: String,
		@Value($$"${rabbitmq.exchange}")
		private val exchangeName: String,
		@Value($$"${rabbitmq.routingKey}")
		private val routingKey: String
	) {
		@Bean
		fun auditQueue(): Queue = Queue(queueName)

		@Bean
		fun auditExchange(): DirectExchange = DirectExchange(exchangeName)

		// Declared here as well so that the topology exists even when devgate-audit-service is down.
		@Bean
		fun auditBinding(): Binding = BindingBuilder.bind(auditQueue()).to(auditExchange()).with(routingKey)

		@Bean
		fun converter(): MessageConverter = JacksonJsonMessageConverter()
	}