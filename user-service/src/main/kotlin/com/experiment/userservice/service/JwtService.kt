package com.experiment.userservice.service

import com.experiment.userservice.entity.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(

    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,

    @Value("\${jwt.access-token-secret}")
    private val accessSecret: String

) {

    private fun getAccessKey(): SecretKey {
        return Keys.hmacShaKeyFor(accessSecret.toByteArray())
    }

    fun generateAccessToken(user: User): String {

        val userId = user.id
            ?: throw IllegalStateException("Cannot generate JWT for unsaved user")

        val now = Instant.now()

        return Jwts.builder()
            .subject(userId)
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(
                Date.from(
                    now.plusMillis(accessTokenExpiration)
                )
            )
            .signWith(getAccessKey())
            .compact()
    }
}