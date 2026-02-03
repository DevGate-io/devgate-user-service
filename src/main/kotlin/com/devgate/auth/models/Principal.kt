package com.devgate.auth.models

interface Principal {
	var email: String
	var password: String
}