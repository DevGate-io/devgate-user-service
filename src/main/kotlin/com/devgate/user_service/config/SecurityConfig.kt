package com.devgate.user_service.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration

@EnableWebSecurity
@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http.cors { it.configurationSource { corsConfiguration() } }
			.csrf { it.disable()}
			.authorizeHttpRequests {
				it.anyRequest().permitAll()
			}

		return http.build()
	}

	fun corsConfiguration(): CorsConfiguration {
		val corsConfiguration = CorsConfiguration()

		corsConfiguration.allowedOriginPatterns = listOf("*")
		corsConfiguration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
		corsConfiguration.allowedHeaders = listOf("*")
		corsConfiguration.allowCredentials = true

		return corsConfiguration
	}
}