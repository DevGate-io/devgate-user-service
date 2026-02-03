package com.devgate.users.models.enums

import org.springframework.security.core.GrantedAuthority

enum class Role(private val roleName: String) : GrantedAuthority {
	MEMBER("member"),
	ADMIN("admin"),
	MANAGER("manager"),
	DEVOPS("devops"),
	QA("qa");

	override fun getAuthority(): String = this.roleName
}