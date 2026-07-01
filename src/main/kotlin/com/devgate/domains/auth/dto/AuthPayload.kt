package com.devgate.domains.auth.dto

import com.devgate.users.models.User

interface AuthPayload {
	var user: User
	var accessToken: String
}