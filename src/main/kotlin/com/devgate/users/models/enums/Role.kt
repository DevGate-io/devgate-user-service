package com.devgate.users.models.enums

import org.springframework.security.core.GrantedAuthority

enum class Role(
	private val roleName: String
) : GrantedAuthority {
	MEMBER("ROLE_MEMBER"),
	ADMIN("ROLE_ADMIN"),
	MANAGER("ROLE_MANAGER"),
	DEVOPS("ROLE_DEVOPS"),
	QA("ROLE_QA");

	override fun getAuthority(): String = this.roleName
}