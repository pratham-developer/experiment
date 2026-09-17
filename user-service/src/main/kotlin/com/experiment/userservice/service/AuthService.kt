package com.experiment.userservice.service

import com.experiment.userservice.dto.LoginRequest
import com.experiment.userservice.dto.LoginResponse
import com.experiment.userservice.dto.RegisterRequest
import com.experiment.userservice.entity.Role
import com.experiment.userservice.entity.User
import com.experiment.userservice.exception.EmailAlreadyExistsException
import com.experiment.userservice.exception.InvalidCredentialsException
import com.experiment.userservice.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun registerUser(request: RegisterRequest) {

        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException()
        }

        val user = User(
            name = request.name,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Password encoding failed"),
            role = Role.from(request.role),
            phone = request.phone,
            address = request.address
        )

        userRepository.save(user)
    }

    fun loginUser(request: LoginRequest): LoginResponse {

        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(
                request.password,
                user.passwordHash
            )
        ) {
            throw InvalidCredentialsException()
        }

        val userId = user.id
            ?: throw IllegalStateException("User ID is missing")

        return LoginResponse(
            accessToken = jwtService.generateAccessToken(user),
            userId = userId,
            email = user.email,
            role = user.role.name
        )
    }
}