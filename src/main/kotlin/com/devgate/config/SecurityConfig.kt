package com.devgate.config

import com.devgate.auth.security.RequestFilter
import com.devgate.utils.PasswordEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration

@EnableWebSecurity
@Configuration
class SecurityConfig(
	private val requestFilter: RequestFilter,
	private val passwordEncoder: PasswordEncoder,
	private val userDetailsService: UserDetailsService
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
				it.requestMatchers("/auth/**").permitAll()
				it.anyRequest().authenticated()
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