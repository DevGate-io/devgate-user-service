package com.devgate.utils

import org.slf4j.LoggerFactory

fun Any.info(message: String) {
	LoggerFactory.getLogger(javaClass).info(message)
}

fun Any.error(message: String) {
	LoggerFactory.getLogger(javaClass).error(message)
}

fun Any.debug(message: String) {
	LoggerFactory.getLogger(javaClass).debug(message)
}