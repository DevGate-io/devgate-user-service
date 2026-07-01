package com.devgate.domains.auth.repositories

import com.devgate.auth.models.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
	@Query(
		"""
			DELETE FROM RefreshToken rt
			WHERE rt.user.id = :userId
			AND rt.hashedToken = :hashedToken
		"""
	)
	@Modifying
	fun deleteByUserIdAndHashedToken(
		userId: UUID?,
		hashedToken: String
	)

	@Query(
		"""
			SELECT rt	FROM RefreshToken rt
			WHERE rt.user.id= :userId
			AND rt.hashedToken = :hashedToken
		"""
	)
	fun findByUserIdAndHashedToken(
		userId: UUID?,
		hashedToken: String
	): RefreshToken?

	fun findByHashedToken(hashedToken: String): RefreshToken?

	fun removeRefreshTokenByHashedToken(hashedToken: String)
}