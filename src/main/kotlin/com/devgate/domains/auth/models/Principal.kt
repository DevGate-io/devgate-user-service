package com.devgate.domains.auth.models

interface Principal {
	var email: String
	var password: String
}