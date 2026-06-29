package com.devgate.config

import com.devgate.auth.security.RequestFilter
import com.devgate.utils.PasswordEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
class SecurityConfig(
	private val requestFilter: RequestFilter,
	private val passwordEncoder: PasswordEncoder,
	private val userDetailsService: UserDetailsService,

	@Value($$"${cors.allowed-origins:http://localhost:3000}")
	private val allowedOrigins: List<String>,
) {

	@Bean
	fun authenticationProvider(): AuthenticationProvider {
		val provider = DaoAuthenticationProvider(userDetailsService)
		provider.setPasswordEncoder(passwordEncoder)
		return provider
	}

	@Bean
	fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager {
		return configuration.authenticationManager
	}

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http.cors { it.configurationSource { corsConfiguration() } }
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.addFilterBefore(requestFilter, UsernamePasswordAuthenticationFilter::class.java)
			.authenticationProvider(authenticationProvider())
			.authorizeHttpRequests {
				it.requestMatchers(
					"/auth/login",
					"/auth/register",
					"/auth/refresh",
					"/auth/logout"
				).permitAll()
				it.requestMatchers(
					"/swagger-ui.html",
					"/swagger-ui/**",
					"/v3/api-docs",
					"/v3/api-docs/**",
					"/v3/api-docs.yaml",
					"/error"
				).permitAll()
				it.anyRequest().authenticated()
			}

		return http.build()
	}

	fun corsConfiguration(): CorsConfiguration {
		return CorsConfiguration().apply {
			allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			allowedHeaders = listOf("*")
			exposedHeaders = listOf("Set-Cookie")
			allowCredentials = true
			maxAge = 3600L
		}
	}
}