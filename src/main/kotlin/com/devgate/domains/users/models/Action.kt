package com.devgate.domains.users.models

import com.fasterxml.jackson.annotation.JsonValue

enum class Action(val value: String) {
	SERVICE_CREATED("service.created"),
	SERVICE_UPDATED("service.updated"),
	SERVICE_DELETED("service.deleted"),

	TEMPLATE_USED("template.used"),

	TEAM_MEMBER_ADDED("team.member_added"),
	TEAM_MEMBER_REMOVED("team.member_removed"),

	INTEGRATION_CONNECTED("integration.connected"),
	INTEGRATION_DISCONNECTED("integration.disconnected"),

	ROLE_CHANGED("user.role_changed"),
	USER_CREATED("user.created"),
	USER_UPDATED("user.updated"),
	USER_DELETED("user.deleted");

	@JsonValue
	fun toValue() = value
}
