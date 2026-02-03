package com.devgate.config

import com.devgate.users.repositories.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

@Configuration
class AppConfig(
	private val userRepository: UserRepository
) {
	@Bean
	fun userDetailsService(): UserDetailsService {
		return UserDetailsService { username ->
			userRepository.findByEmail(username)
				?: throw UsernameNotFoundException("User not found")
		}
	}
}