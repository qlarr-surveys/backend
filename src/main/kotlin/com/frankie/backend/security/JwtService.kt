package com.frankie.backend.security

import com.frankie.backend.api.user.AccessToken
import com.frankie.backend.common.nowUtc
import com.frankie.backend.persistence.entities.UserEntity
import com.frankie.backend.properties.JwtProperties
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*
import java.util.function.Function

@Component
class JwtService(private val props: JwtProperties) {
    fun extractSubject(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }

    fun <T> extractClaim(token: String, claimsResolver: Function<Claims, T>): T {
        val claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    fun extractClaimBypassExpiry(token: String): Claims {
        return try {
            extractAllClaims(token)
        } catch (e: ExpiredJwtException) {
            e.claims
        }
    }


    fun getJwtUserDetails(token: String): JwtUserDetails {
        return extractAllClaims(token).toJwtUserDetails()
    }

    fun getJwtUserDetailsBypassExpiry(token: String): JwtUserDetails {
        return extractClaimBypassExpiry(token).toJwtUserDetails()
    }

    fun getResetPasswordDetails(token: String): JwtResetPasswordData {
        return extractAllClaims(token).let {
            JwtResetPasswordData(
                    it.subject,
                    it[RESET_PASSWORD] as Boolean,
                    it[NEW_USER] as Boolean,
            )
        }
    }

    fun getJwtUserDetailsFromExpired(token: String): JwtUserDetails {
        return try {
            extractAllClaims(token).toJwtUserDetails()
        } catch (e: ExpiredJwtException) {
            e.claims.toJwtUserDetails()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Claims.toJwtUserDetails(): JwtUserDetails {
        val authorities = (this[AUTHORITIES] as List<String>).map { SimpleGrantedAuthority(it) }
        val sessionId = (this[SESSION_ID] as? String)?.let {
            UUID.fromString(this[SESSION_ID] as? String)
        }
        val userId = this[USER_ID] as String
        return JwtUserDetails(userId, sessionId, authorities)
    }


    fun generateAccessToken(user: UserEntity): AccessToken {
        val tokenExpiration = Date(System.currentTimeMillis() + props.activeExpiration)
        val sessionId = UUID.randomUUID()
        val refreshTokenExpiration = nowUtc().plusSeconds(props.refreshExpiration / 1000)
        val claims = buildAccessTokenClaims(user, sessionId.toString())
        val token = setClaimsAndBuildToken(claims, tokenExpiration, user.email)
        val refreshToken = UUID.randomUUID()
        return AccessToken(
                sessionId = sessionId,
                token = token,
                refreshToken = refreshToken,
                refreshTokenExpiry = refreshTokenExpiration
        )
    }

    fun generatePasswordResetToken(user: UserEntity, newUser: Boolean): String {
        val tokenExpiration = Date(System.currentTimeMillis() + if (newUser) {
            props.resetExpirationForNewUsersMs
        } else {
            props.resetExpiration
        })
        return setClaimsAndBuildToken(
                buildResetTokenClaims(newUser),
                tokenExpiration,
                user.email
        )
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            val expiration = claims.expiration
            claims.toJwtUserDetails()
            expiration.after(Date())
        } catch (ex: JwtException) {
            ex.printStackTrace()
            false
        }
    }

    private fun buildAccessTokenClaims(user: UserEntity, sessionId: String): MutableMap<String, Any> {
        val claims: MutableMap<String, Any> = hashMapOf()
        claims[USER_ID] = user.id!!
        claims[AUTHORITIES] = user.roles.map { it.name.lowercase() }
        claims[SESSION_ID] = sessionId
        return claims
    }

    private fun buildResetTokenClaims(newUser: Boolean): MutableMap<String, Any> {
        val claims: MutableMap<String, Any> = hashMapOf()
        claims[RESET_PASSWORD] = true
        claims[NEW_USER] = newUser
        return claims
    }

    private fun setClaimsAndBuildToken(claims: MutableMap<String, Any>, expiration: Date, email: String): String {
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expiration)
                .setIssuedAt(Date(System.currentTimeMillis()))
                .setSubject(email)
                .signWith(generateKey(), SignatureAlgorithm.HS256)
                .compact()
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts
                .parserBuilder()
                .setSigningKey(generateKey())
                .build()
                .parseClaimsJws(token)
                .body
    }

    private fun generateKey(): Key {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret))
    }

    companion object {
        private const val RESET_PASSWORD = "reset_password"
        private const val NEW_USER = "new_user"
        private const val USER_ID = "user_id"
        private const val AUTHORITIES = "authorities"
        private const val SESSION_ID = "session_id"
    }

    data class JwtUserDetails(
            val userId: String,
            val sessionId: UUID?,
            val authorities: List<GrantedAuthority>,
    )

    data class JwtResetPasswordData(
            val email: String,
            val resetPassword: Boolean,
            val newUser: Boolean,
    )
}
