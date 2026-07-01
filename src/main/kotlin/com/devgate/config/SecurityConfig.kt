package com.devgate.config

import com.devgate.domains.auth.security.JwtAccessDeniedHandler
import com.devgate.domains.auth.security.JwtAuthenticationEntryPoint
import com.devgate.domains.auth.security.RequestFilter
import com.devgate.utils.PasswordEncoder
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
	private val authenticationEntryPoint: JwtAuthenticationEntryPoint,
	private val accessDeniedHandler: JwtAccessDeniedHandler
) {
	@Bean
	fun authenticationProvider(): AuthenticationProvider {
		val provider = DaoAuthenticationProvider(userDetailsService)
		provider.setPasswordEncoder(passwordEncoder)
		return provider
	}

	@Bean
	fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager =
		configuration.authenticationManager

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.cors { it.configurationSource { corsConfiguration() } }
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.addFilterBefore(requestFilter, UsernamePasswordAuthenticationFilter::class.java)
			.authenticationProvider(authenticationProvider())
			.exceptionHandling {
				it.authenticationEntryPoint(authenticationEntryPoint)
				it.accessDeniedHandler(accessDeniedHandler)
			}.authorizeHttpRequests {
				it
					.requestMatchers(
						"/auth/login",
						"/auth/register",
						"/auth/refresh",
						"/auth/logout"
					).permitAll()
				it
					.requestMatchers(
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

	fun corsConfiguration(): CorsConfiguration =
		CorsConfiguration().apply {
			allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			allowedHeaders = listOf("*")
			exposedHeaders = listOf("Set-Cookie")
			allowCredentials = true
			maxAge = 3600L
		}
}