package com.devgate.utils

import org.slf4j.LoggerFactory

object Logger {
	fun info(message: String, obj: Any) {
		LoggerFactory.getLogger(obj.javaClass).info(message)
	}

	fun error(message: String, obj: Any) {
		LoggerFactory.getLogger(obj.javaClass).error(message)
	}

	fun debug(message: String, obj: Any) {
		LoggerFactory.getLogger(obj.javaClass).debug(message)
	}
}
