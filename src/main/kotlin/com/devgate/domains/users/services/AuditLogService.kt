package com.devgate.domains.users.services

import com.devgate.domains.users.models.Action
import com.devgate.domains.users.models.Target

interface AuditLogService {
	fun sendMessage(action: Action, target: Target)
}