package com.devgate.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
	@Bean
	fun openApi(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("DevGate User Service API")
					.description("User management and authentication (JWT access token + refresh-token cookie)")
					.version("1.0.0")
			).components(
				Components().addSecuritySchemes(
					BEARER_SCHEME,
					SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
				)
			).addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

	private companion object {
		const val BEARER_SCHEME = "bearer-jwt"
	}
}