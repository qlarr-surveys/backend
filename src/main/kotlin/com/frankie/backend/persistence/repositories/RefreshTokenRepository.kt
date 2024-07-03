package com.frankie.backend.persistence.repositories

import com.frankie.backend.persistence.entities.RefreshTokenEntity
import com.frankie.backend.persistence.entities.UserEntity
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*


interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {


    @Query(
        "SELECT u as user, r as refreshToken " +
                "FROM RefreshTokenEntity r JOIN UserEntity u " +
                "ON r.id = :token AND u.id = r.userId "
    )
    fun getUserByRefreshToken(token: UUID): UserRefresh?

    @Transactional
    fun deleteBySessionId(sessionId: UUID)
    fun deleteByUserId(userId: UUID)
}


interface UserRefresh {
    val user: UserEntity
    val refreshToken: RefreshTokenEntity
}