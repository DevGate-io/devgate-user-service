package com.devgate.core.models.enums

enum class Role(private val roleName: String) {
	MEMBER("member"),
	ADMIN("admin"),
	MANAGER("manager"),
	DEVOPS("devops"),
	QA("qa")
}