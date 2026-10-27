package com.devgate.domains.auth.repositories

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class RegistrationLockRepository(
	private val jdbcTemplate: JdbcTemplate
) {
	// Serializes self-registration across concurrent requests (and service replicas): the
	// "no users yet" check and the user insert must not interleave, otherwise two parallel
	// first registrations would both become ADMIN. The transaction-scoped advisory lock is
	// released automatically on commit/rollback, so it must be acquired inside a transaction.
	fun acquire() {
		jdbcTemplate.execute("SELECT pg_advisory_xact_lock($REGISTRATION_LOCK_KEY)")
	}

	private companion object {
		const val REGISTRATION_LOCK_KEY = 7_303_001L
	}
}