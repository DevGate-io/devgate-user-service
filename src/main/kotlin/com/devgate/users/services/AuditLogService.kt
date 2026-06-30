package com.devgate.users.services

import com.devgate.users.models.Action
import com.devgate.users.models.Target

interface AuditLogService {
	fun sendMessage(action: Action, target: Target)
}